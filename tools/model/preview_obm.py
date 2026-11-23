import json
import math
import os
import struct
import sys

import numpy as np
from PIL import Image


def read_obm(path):
    buf = open(path, "rb").read()
    assert buf[:4] == b"OBM1"
    vc, ic, sc, nc, sub, mc, _ = struct.unpack_from("<7I", buf, 4)
    pos = 32
    world = np.frombuffer(buf, "<f4", 16, pos).reshape(4, 4).T
    pos += 64
    def arr(dt, n, shape):
        nonlocal pos
        a = np.frombuffer(buf, dt, n, pos).reshape(shape)
        pos += a.nbytes
        return a
    P = arr("<f4", vc * 3, (vc, 3))
    N = arr("<f4", vc * 3, (vc, 3))
    U = arr("<f4", vc * 2, (vc, 2))
    BI = arr("<u2", vc * 4, (vc, 4))
    BW = arr("<f4", vc * 4, (vc, 4))
    I = arr("<u2", ic, (ic,))
    subs = []
    for _ in range(sub):
        subs.append(struct.unpack_from("<3I", buf, pos))
        pos += 12
    nodes = []
    for _ in range(nc):
        parent, nl, skin = struct.unpack_from("<iHi", buf, pos)
        pos += 10
        name = buf[pos:pos + nl].decode()
        pos += nl
        loc = np.frombuffer(buf, "<f4", 16, pos).reshape(4, 4).T
        pos += 64
        inv = np.frombuffer(buf, "<f4", 16, pos).reshape(4, 4).T
        pos += 64
        nodes.append((parent, name, skin, loc.copy(), inv.copy()))
    morphs = {}
    for _ in range(mc):
        nl = struct.unpack_from("<H", buf, pos)[0]
        pos += 2
        name = buf[pos:pos + nl].decode()
        pos += nl
        morphs[name] = arr("<f4", vc * 3, (vc, 3))
    return world, P, N, U, BI, BW, I, subs, nodes, morphs, sc


def skin_matrices(nodes, sc, deltas=None):
    g = [None] * len(nodes)
    for i, (parent, name, skin, loc, inv) in enumerate(nodes):
        l = loc if not deltas or name not in deltas else loc @ deltas[name]
        g[i] = l if parent < 0 else g[parent] @ l
    out = np.tile(np.eye(4), (sc, 1, 1))
    for i, (parent, name, skin, loc, inv) in enumerate(nodes):
        if skin >= 0:
            out[skin] = g[i] @ inv
    return out


def render(model_dir, out_png, deltas=None, morph_w=None, yaw=0.0, size=(540, 900), skin_override=None, bob=0.0):
    world, P, N, U, BI, BW, I, subs, nodes, morphs, sc = read_obm(os.path.join(model_dir, "model.obm"))
    meta = json.load(open(os.path.join(model_dir, "model.json")))
    S = skin_override if skin_override is not None else skin_matrices(nodes, sc, deltas)
    Pm = P.copy()
    for k, w in (morph_w or {}).items():
        Pm += morphs[k] * w
    ph = np.concatenate([Pm, np.ones((len(Pm), 1))], axis=1)
    skinned = np.zeros((len(Pm), 4))
    skn = np.zeros((len(Pm), 3))
    for j in range(4):
        m = S[BI[:, j]]
        skinned += BW[:, j:j + 1] * np.einsum("vij,vj->vi", m, ph)
        skn += BW[:, j:j + 1] * np.einsum("vij,vj->vi", m[:, :3, :3], N)
    wp = (world @ skinned.T).T[:, :3]
    wn = (world[:3, :3] @ skn.T).T
    c, s_ = math.cos(yaw), math.sin(yaw)
    ry = np.array([[c, 0, s_], [0, 1, 0], [-s_, 0, c]])
    wp = wp @ ry.T
    wn = wn @ ry.T
    cam = meta["camera"]
    dist = cam["distance"]
    f = 1.0 / math.tan(math.radians(cam["fov"]) / 2)
    W, H = size
    aspect = W / H
    px = wp[:, 0] / (dist - wp[:, 2]) * f / aspect
    px += float(cam.get("offsetX", 0.0))
    py = (wp[:, 1] - cam["targetY"]) / (dist - wp[:, 2]) * f
    sx = (px * 0.5 + 0.5) * W
    sy = (1 - (py * 0.5 + 0.5)) * H
    depth = wp[:, 2]
    tex = {}
    for m in meta["materials"]:
        for k in ("diffuse", "shade", "emissive"):
            if m[k] and m[k] not in tex:
                tex[m[k]] = np.array(Image.open(os.path.join(model_dir, m[k])).convert("RGBA"))
    img = np.zeros((H, W, 4), dtype=np.float32)
    zbuf = np.full((H, W), -1e9, dtype=np.float32)
    light = np.array([0.4, 0.6, 0.7])
    light = light / np.linalg.norm(light)
    for si, (start, count, mi) in enumerate(subs):
        m = meta["materials"][mi]
        diff = tex.get(m["diffuse"])
        shade = tex.get(m["shade"])
        for t in range(start, start + count, 3):
            a, b, c3 = I[t], I[t + 1], I[t + 2]
            xs = np.array([sx[a], sx[b], sx[c3]])
            ys = np.array([sy[a], sy[b], sy[c3]])
            x0, x1 = int(max(0, math.floor(xs.min()))), int(min(W - 1, math.ceil(xs.max())))
            y0, y1 = int(max(0, math.floor(ys.min()))), int(min(H - 1, math.ceil(ys.max())))
            if x1 < x0 or y1 < y0:
                continue
            area = (xs[1] - xs[0]) * (ys[2] - ys[0]) - (xs[2] - xs[0]) * (ys[1] - ys[0])
            if abs(area) < 1e-9:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            w0 = ((xs[1] - gx) * (ys[2] - gy) - (xs[2] - gx) * (ys[1] - gy)) / area
            w1 = ((xs[2] - gx) * (ys[0] - gy) - (xs[0] - gx) * (ys[2] - gy)) / area
            w2 = 1 - w0 - w1
            inside = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
            if not inside.any():
                continue
            z = w0 * depth[a] + w1 * depth[b] + w2 * depth[c3]
            ok = inside & (z > zbuf[y0:y1 + 1, x0:x1 + 1])
            if not ok.any():
                continue
            u = w0 * U[a, 0] + w1 * U[b, 0] + w2 * U[c3, 0]
            v = w0 * U[a, 1] + w1 * U[b, 1] + w2 * U[c3, 1]
            n = w0[..., None] * wn[a] + w1[..., None] * wn[b] + w2[..., None] * wn[c3]
            n = n / (np.linalg.norm(n, axis=-1, keepdims=True) + 1e-9)
            ndl = (n @ light) * 0.5 + 0.5
            if diff is not None:
                th, tw = diff.shape[:2]
                ti = np.clip((v % 1.0) * th, 0, th - 1).astype(int)
                tj = np.clip((u % 1.0) * tw, 0, tw - 1).astype(int)
                col = diff[ti, tj].astype(np.float32) / 255.0
            else:
                col = np.ones(u.shape + (4,), dtype=np.float32) * 0.7
            keep = ok & (col[..., 3] > 0.5) if True else ok
            if not keep.any():
                continue
            lit = np.where(ndl > 0.5, 1.0, 0.62)[..., None]
            rgb = col[..., :3] * lit
            sub_img = img[y0:y1 + 1, x0:x1 + 1]
            sub_z = zbuf[y0:y1 + 1, x0:x1 + 1]
            sub_img[keep, :3] = rgb[keep]
            sub_img[keep, 3] = 1.0
            sub_z[keep] = z[keep]
    bg = np.zeros((H, W, 3), dtype=np.float32) + np.array([0.06, 0.05, 0.08])
    out = img[..., :3] * img[..., 3:4] + bg * (1 - img[..., 3:4])
    Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8)).save(out_png)


if __name__ == "__main__":
    render(sys.argv[1], sys.argv[2], yaw=float(sys.argv[3]) if len(sys.argv) > 3 else 0.0)
