import glob
import json
import os
import shutil
import sys
import uuid
import zipfile

PRESETS = {
    "mountains": [0.45, 0.3],
    "extreme": [1.0, 0.5],
    "less_extreme": [0.2, 0.3],
    "stone_beach": [0.1, 0.8],
}

WATER_AND_UNDERGROUND = {"ocean", "deep_ocean", "river", "beach"}


def soften(variation, strength):
    if variation <= 0.2:
        return variation
    return round(0.2 + (variation - 0.2) * (1.0 - strength), 4)


def is_overworld_land(biome):
    tags = biome["components"].get("minecraft:tags", {}).get("tags", [])
    if "overworld" not in tags and "overworld_generation" not in tags:
        return False
    return not any(t in tags for t in ("ocean", "caves", "deep"))


def convert(biome, strength):
    comp = biome["components"]
    height = comp.get("minecraft:overworld_height")
    if height is None or not is_overworld_land(biome):
        return False
    if "noise_params" in height:
        base, var = height["noise_params"]
        new = [base, soften(var, strength)]
        if new == height["noise_params"]:
            return False
        height["noise_params"] = new
        return True
    kind = height.get("noise_type")
    if kind in PRESETS:
        base, var = PRESETS[kind]
        new = [base, soften(var, strength)]
        if new[1] == var:
            return False
        comp["minecraft:overworld_height"] = {"noise_params": new}
        return True
    return False


def main():
    src, out, icon = sys.argv[1], sys.argv[2], sys.argv[3]
    strength = float(sys.argv[4]) if len(sys.argv) > 4 else 0.5
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(os.path.join(out, "biomes"))
    changed = []
    for path in sorted(glob.glob(os.path.join(src, "*.json"))):
        doc = json.load(open(path, encoding="utf-8"))
        biome = doc["minecraft:biome"]
        if convert(biome, strength):
            name = os.path.basename(path)
            with open(os.path.join(out, "biomes", name), "w", encoding="utf-8") as f:
                json.dump(doc, f, indent=2)
                f.write("\n")
            changed.append(biome["description"]["identifier"])
    header_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, "oblivionmod.betterworldgen.bp.header"))
    module_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, "oblivionmod.betterworldgen.bp.module"))
    manifest = {
        "format_version": 2,
        "header": {
            "name": "OblivionMod Better World Generation",
            "description": "Smoother land terrain: reduces the height variation of overworld land biomes.",
            "uuid": header_uuid,
            "version": [0, 1, 0],
            "min_engine_version": [1, 26, 70],
        },
        "modules": [{"type": "data", "uuid": module_uuid, "version": [0, 1, 0]}],
    }
    with open(os.path.join(out, "manifest.json"), "w") as f:
        json.dump(manifest, f, indent=2)
        f.write("\n")
    if os.path.exists(icon):
        shutil.copy(icon, os.path.join(out, "pack_icon.png"))
    print(len(changed), "biomes overridden")
    for ident in changed:
        print(" ", ident)


if __name__ == "__main__":
    main()
