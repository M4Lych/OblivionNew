import json
import math
import os
import re
import struct
import sys

import numpy as np
from PIL import Image

import fbx_parse

MAX_TEX = 1024
ARM_DROP = float(os.environ.get("OBM_ARM_DROP", "72"))
MAGIC = b"OBM1"


def mat(values):
    return np.array(values, dtype=np.float64).reshape(4, 4).T


def euler_matrix(deg, order=0):
    rx, ry, rz = [math.radians(a) for a in deg]
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    r = mz @ my @ mx
    m = np.eye(4)
    m[:3, :3] = r
    return m


def local_matrix(model):
    props = {}
    p70 = model.find("Properties70")
    if p70 is not None:
        for p in p70.children:
            props[p.props[0]] = p.props[4:]
    t = props.get("Lcl Translation", [0, 0, 0])
    r = props.get("Lcl Rotation", [0, 0, 0])
    s = props.get("Lcl Scaling", [1, 1, 1])
    pre = props.get("PreRotation", [0, 0, 0])
    tm = np.eye(4)
    tm[:3, 3] = t
    sm = np.diag([s[0], s[1], s[2], 1.0])
    return tm @ euler_matrix(pre) @ euler_matrix(r) @ sm


def lower_arms(nodes, node_parent, node_local, node_global, angle_deg):
    idx = {clean(n.props[1]): i for i, n in enumerate(nodes)}
    for side in ("L", "R"):
        a = idx.get("Arm_" + side)
        e = idx.get("Elbow_" + side)
        if a is None or e is None:
            continue
        d = node_global[e][:3, 3] - node_global[a][:3, 3]
        d = d / np.linalg.norm(d)
        horiz = np.array([d[0], 0.0, d[2]])
        if np.linalg.norm(horiz) < 1e-6:
            continue
        horiz = horiz / np.linalg.norm(horiz)
        ang = math.radians(angle_deg)
        t = horiz * math.cos(ang) + np.array([0.0, -1.0, 0.0]) * math.sin(ang)
        axis = np.cross(d, t)
        sn = np.linalg.norm(axis)
        cs = float(d @ t)
        if sn < 1e-6:
            continue
        axis = axis / sn
        k = np.array([[0, -axis[2], axis[1]], [axis[2], 0, -axis[0]], [-axis[1], axis[0], 0]])
        r = np.eye(3) + sn * k + (1 - cs) * (k @ k)
        new_g = node_global[a].copy()
        new_g[:3, :3] = r @ node_global[a][:3, :3]
        parent_g = node_global[node_parent[a]] if node_parent[a] >= 0 else np.eye(4)
        node_local[a] = np.linalg.inv(parent_g) @ new_g


def clean(name):
    return name.split("\x00")[0]


def load_fbx(path):
    version, root = fbx_parse.parse(path)
    objects = root.find("Objects")
    conns = root.find("Connections")
    by_id = {}
    for c in objects.children:
        if c.props and isinstance(c.props[0], int):
            by_id[c.props[0]] = c
    parents = {}
    children = {}
    for c in conns.children:
        if c.props[0] in ("OO", "OP"):
            child, parent = c.props[1], c.props[2]
            parents.setdefault(child, []).append(parent)
            children.setdefault(parent, []).append(child)
    return by_id, parents, children


def texture_for(material, files):
    base = re.sub(r"\.\d+$", "", clean(material))
    base = base.replace("mtl_", "")
    base = re.sub(r"_Alpha\d*$", "", base)
    tokens = [t for t in re.split(r"[_]", base) if t]
    if base.endswith("mayu"):
        tokens = [t for t in tokens if t != "mayu"] + ["face"]
    tokens = [t for t in tokens if t not in ("20", "00")]
    def best(kind):
        scored = []
        for f in files:
            low = f.lower()
            if kind not in low:
                continue
            score = sum(1 for t in tokens if t.lower() in low)
            scored.append((score, f))
        scored.sort(key=lambda x: (-x[0], x[1]))
        if scored and scored[0][0] > 0:
            return scored[0][1]
        return None
    diff = best("diff") or best("eye")
    shade = best("shad_c")
    emi = best("emi")
    if base.endswith("eye"):
        diff = best("eye")
    return diff, shade, emi


def build(fbx_path, tex_dir, out_dir, model_id, model_name):
    by_id, parents, children = load_fbx(fbx_path)
    mesh_model = next(n for n in by_id.values() if n.name == "Model" and n.props[2] == "Mesh")
    geo = next(n for n in by_id.values() if n.name == "Geometry" and n.props[2] == "Mesh")
    skin = next((n for n in by_id.values() if n.name == "Deformer" and n.props[2] == "Skin"), None)

    cp = np.array(geo.find("Vertices").props[0], dtype=np.float64).reshape(-1, 3)
    pvi = geo.find("PolygonVertexIndex").props[0]

    def layer(name, uv_name=None):
        for c in geo.children:
            if c.name == name and (uv_name is None or (c.find("Name") and c.find("Name").props[0] == uv_name)):
                return c
        return None

    ln = layer("LayerElementNormal")
    lu = layer("LayerElementUV", "UVMap") or layer("LayerElementUV")
    lm = layer("LayerElementMaterial")
    normals = np.array(ln.find("Normals").props[0]).reshape(-1, 3)
    nidx = ln.find("NormalsIndex").props[0] if ln.find("NormalsIndex") else None
    uvs = np.array(lu.find("UV").props[0]).reshape(-1, 2)
    uidx = lu.find("UVIndex").props[0] if lu.find("UVIndex") else None
    mats = lm.find("Materials").props[0] if lm else [0]
    mat_mode = lm.find("MappingInformationType").props[0] if lm else "AllSame"

    clusters = []
    if skin is not None:
        for cid in children.get(skin.props[0], []):
            node = by_id.get(cid)
            if node is not None and node.name == "Deformer" and node.props[2] == "Cluster":
                clusters.append(node)
    def model_global(model_id, depth=0):
        node = by_id[model_id]
        m = local_matrix(node)
        for pid in parents.get(model_id, []):
            if pid in by_id and by_id[pid].name == "Model" and depth < 32:
                return model_global(pid, depth + 1) @ m
        return m

    mesh_bind = model_global(mesh_model.props[0])

    nodes = [n for n in by_id.values() if n.name == "Model" and n.props[2] != "Mesh"]
    node_index = {n.props[0]: i for i, n in enumerate(nodes)}
    node_parent = []
    for n in nodes:
        p = -1
        for pid in parents.get(n.props[0], []):
            if pid in node_index:
                p = node_index[pid]
                break
        node_parent.append(p)
    order = []
    seen = set()

    def visit(i):
        if i in seen:
            return
        if node_parent[i] >= 0:
            visit(node_parent[i])
        seen.add(i)
        order.append(i)

    for i in range(len(nodes)):
        visit(i)
    remap = {old: new for new, old in enumerate(order)}
    nodes = [nodes[i] for i in order]
    node_parent = [remap[node_parent[i]] if node_parent[i] >= 0 else -1 for i in order]
    node_index = {n.props[0]: i for i, n in enumerate(nodes)}
    node_local = [local_matrix(n) for n in nodes]
    node_global = []
    for i, m in enumerate(node_local):
        node_global.append(m if node_parent[i] < 0 else node_global[node_parent[i]] @ m)

    skin_nodes = []
    inv_bind = {}
    cluster_of_cp = [[] for _ in range(len(cp))]
    max_err = 0.0
    for k, cl in enumerate(clusters):
        bone_id = next((c for c in children.get(cl.props[0], []) if c in node_index), None)
        if bone_id is None:
            continue
        bi = node_index[bone_id]
        link = mat(cl.find("TransformLink").props[0])
        max_err = max(max_err, float(np.abs(link - node_global[bi]).max()))
        slot = len(skin_nodes)
        skin_nodes.append(bi)
        inv_bind[bi] = np.linalg.inv(link)
        ii = cl.find("Indexes")
        ww = cl.find("Weights")
        if ii is not None and ww is not None:
            for vi, w in zip(ii.props[0], ww.props[0]):
                cluster_of_cp[vi].append((slot, w))
    print("nodes", len(nodes), "skin bones", len(skin_nodes), "max |TransformLink - evaluated|", round(max_err, 5))

    if ARM_DROP > 0:
        lower_arms(nodes, node_parent, node_local, node_global, ARM_DROP)

    split = {}
    verts = []
    tri_index = []
    poly = []
    pos_in_poly = 0
    for pv, raw in enumerate(pvi):
        idx = raw if raw >= 0 else ~raw
        n_i = int(nidx[pv]) if nidx else pv
        u_i = int(uidx[pv]) if uidx else pv
        nv = normals[n_i]
        uvv = uvs[u_i]
        key = (idx, round(float(nv[0]), 3), round(float(nv[1]), 3), round(float(nv[2]), 3), round(float(uvv[0]), 5), round(float(uvv[1]), 5))
        v = split.get(key)
        if v is None:
            v = len(verts)
            split[key] = v
            verts.append((idx, n_i, u_i))
        poly.append(v)
        if raw < 0:
            for t in range(1, len(poly) - 1):
                tri_index.append((poly[0], poly[t], poly[t + 1]))
            poly = []
    tri_count = len(tri_index)
    poly_mat = []
    if mat_mode == "ByPolygon":
        poly_mat = list(mats)
    else:
        poly_mat = [mats[0]] * tri_count
    if len(poly_mat) != tri_count:
        poly_mat = (list(poly_mat) + [0] * tri_count)[:tri_count]

    vc = len(verts)
    if vc > 65535:
        raise SystemExit("too many vertices for 16 bit indices: %d" % vc)
    pos = np.zeros((vc, 3), dtype=np.float32)
    nor = np.zeros((vc, 3), dtype=np.float32)
    uv = np.zeros((vc, 2), dtype=np.float32)
    bone_idx = np.zeros((vc, 4), dtype=np.uint16)
    bone_w = np.zeros((vc, 4), dtype=np.float32)
    rot = mesh_bind[:3, :3]
    nrot = np.linalg.inv(rot).T
    for v, (ci, ni, ui) in enumerate(verts):
        p = mesh_bind @ np.array([cp[ci][0], cp[ci][1], cp[ci][2], 1.0])
        pos[v] = p[:3]
        nn = nrot @ normals[ni]
        ln_ = np.linalg.norm(nn)
        nor[v] = nn / ln_ if ln_ > 0 else (0, 1, 0)
        uv[v] = (uvs[ui][0], 1.0 - uvs[ui][1])
        infl = sorted(cluster_of_cp[ci], key=lambda x: -x[1])[:4]
        total = sum(w for _, w in infl)
        if total > 0:
            for j, (s, w) in enumerate(infl):
                bone_idx[v, j] = s
                bone_w[v, j] = w / total
        elif skin_nodes:
            bone_idx[v, 0] = 0
            bone_w[v, 0] = 1.0

    lo, hi = pos.min(axis=0), pos.max(axis=0)
    print("bounds", lo, hi)
    height = hi[1] - lo[1]
    scale = 1.7 / height if height > 0 else 1.0
    center_x = (lo[0] + hi[0]) / 2
    center_z = (lo[2] + hi[2]) / 2
    world = np.eye(4)
    world[0, 0] = world[1, 1] = world[2, 2] = scale
    world[0, 3] = -center_x * scale
    world[1, 3] = -lo[1] * scale
    world[2, 3] = -center_z * scale

    groups = {
        "blink": [r"^Eye_2_[LR]\(CloseA\)"],
        "smile": [r"^Mouth_4_0\(WaraiA\)"],
    }
    morph_arrays = {}
    for name, pats in groups.items():
        arr = np.zeros((vc, 3), dtype=np.float32)
        cp_delta = {}
        for ch in [n for n in by_id.values() if n.name == "Deformer" and n.props[2] == "BlendShapeChannel"]:
            if not any(re.search(p, clean(ch.props[1])) for p in pats):
                continue
            for sid in children.get(ch.props[0], []):
                sh = by_id.get(sid)
                if sh is None or sh.name != "Geometry":
                    continue
                ind = sh.find("Indexes").props[0] if sh.find("Indexes") else []
                dv = np.array(sh.find("Vertices").props[0]).reshape(-1, 3) if sh.find("Vertices") else []
                for a, d in zip(ind, dv):
                    cp_delta[a] = cp_delta.get(a, 0) + rot @ d
        for v, (ci, _, _) in enumerate(verts):
            if ci in cp_delta:
                arr[v] = cp_delta[ci]
        morph_arrays[name] = arr
        print("morph", name, int((np.abs(arr).sum(axis=1) > 0).sum()), "vertices")

    material_names = []
    for n in by_id.values():
        if n.name == "Material":
            material_names.append((n.props[0], clean(n.props[1])))
    mat_order = [nm for _, nm in material_names]
    linked = [by_id[c] for c in children.get(mesh_model.props[0], []) if c in by_id and by_id[c].name == "Material"]
    if linked:
        mat_order = [clean(m.props[1]) for m in linked]

    files = sorted(f for f in os.listdir(tex_dir) if f.lower().endswith((".png", ".jpg", ".jpeg")))
    os.makedirs(os.path.join(out_dir, "textures"), exist_ok=True)
    used = set()
    mat_json = []
    for nm in mat_order:
        d, s, e = texture_for(nm, files)
        for f in (d, s, e):
            if f:
                used.add(f)
        mat_json.append({
            "name": nm,
            "diffuse": ("textures/" + d) if d else None,
            "shade": ("textures/" + s) if s else None,
            "emissive": ("textures/" + e) if e else None,
            "alpha": "alpha" in nm.lower(),
            "outline": not any(k in nm.lower() for k in ("eye", "mayu", "alpha")),
        })
    for f in used:
        im = Image.open(os.path.join(tex_dir, f)).convert("RGBA")
        if max(im.size) > MAX_TEX:
            r = MAX_TEX / max(im.size)
            im = im.resize((max(1, int(im.size[0] * r)), max(1, int(im.size[1] * r))), Image.LANCZOS)
        im.save(os.path.join(out_dir, "textures", f), optimize=True)

    order_idx = sorted(range(tri_count), key=lambda t: (poly_mat[t], t))
    indices = []
    submeshes = []
    for t in order_idx:
        indices.extend(tri_index[t])
    cursor = 0
    for m in sorted(set(poly_mat)):
        cnt = sum(1 for x in poly_mat if x == m) * 3
        submeshes.append((cursor, cnt, int(m)))
        cursor += cnt

    with open(os.path.join(out_dir, "model.obm"), "wb") as f:
        f.write(MAGIC)
        f.write(struct.pack("<7I", vc, len(indices), len(skin_nodes), len(nodes), len(submeshes), len(morph_arrays), 0))
        f.write(world.T.astype("<f4").tobytes())
        f.write(pos.astype("<f4").tobytes())
        f.write(nor.astype("<f4").tobytes())
        f.write(uv.astype("<f4").tobytes())
        f.write(bone_idx.astype("<u2").tobytes())
        f.write(bone_w.astype("<f4").tobytes())
        f.write(np.array(indices, dtype="<u2").tobytes())
        for s in submeshes:
            f.write(struct.pack("<3I", *s))
        for i, n in enumerate(nodes):
            nm = clean(n.props[1]).encode("utf-8")
            f.write(struct.pack("<iHi", node_parent[i], len(nm), skin_nodes.index(i) if i in skin_nodes else -1))
            f.write(nm)
            f.write(node_local[i].T.astype("<f4").tobytes())
            f.write((inv_bind[i] if i in inv_bind else np.eye(4)).T.astype("<f4").tobytes())
        for name, arr in morph_arrays.items():
            nm = name.encode("utf-8")
            f.write(struct.pack("<H", len(nm)))
            f.write(nm)
            f.write(arr.astype("<f4").tobytes())

    manifest = {
        "id": model_id,
        "name": model_name,
        "format": "obm1",
        "mesh": "model.obm",
        "materials": mat_json,
        "camera": {"fov": 28.0, "targetY": 1.05, "distance": 4.2, "offsetX": 0.0},
        "animation": {
            "breath": 1.0,
            "sway": 1.0,
            "springs": 1.0,
            "blink": True,
            "lookAt": 1.0,
        },
        "bones": {
            "hip": "Hip",
            "spine": ["Spine", "Chest"],
            "neck": "Neck",
            "head": "Head",
            "springPrefix": "Sp_",
        },
        "morphs": {"blink": "blink", "smile": "smile"},
    }
    with open(os.path.join(out_dir, "model.json"), "w") as f:
        json.dump(manifest, f, indent=2)
    print("vertices", vc, "triangles", len(indices) // 3, "submeshes", submeshes)
    return manifest


def main():
    fbx, tex, out, mid, name = sys.argv[1:6]
    os.makedirs(out, exist_ok=True)
    build(fbx, tex, out, mid, name)


if __name__ == "__main__":
    main()
