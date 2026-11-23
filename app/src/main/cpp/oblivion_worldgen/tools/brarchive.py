import json
import os
import re
import struct
import sys

MAGIC = bytes.fromhex("7d2725b1a0527026")
ENTRY_SIZE = 256


def read_archive(path):
    data = open(path, "rb").read()
    if len(data) < 16 or data[:8] != MAGIC:
        return {}
    count = struct.unpack_from("<I", data, 8)[0]
    table = 16
    base = table + count * ENTRY_SIZE
    out = {}
    for i in range(count):
        ent = data[table + i * ENTRY_SIZE: table + (i + 1) * ENTRY_SIZE]
        name_len = ent[0]
        name = ent[1:1 + name_len].decode("utf-8")
        offset, size = struct.unpack_from("<II", ent, ENTRY_SIZE - 8)
        out[name] = data[base + offset: base + offset + size]
    return out


def version_key(folder):
    nums = re.findall(r"\d+", folder.replace("vanilla", ""))
    return tuple(int(n) for n in nums)


def merged(root, archive):
    folders = [d for d in os.listdir(root) if d.startswith("vanilla") and os.path.isdir(os.path.join(root, d))]
    folders.sort(key=lambda d: (version_key(d) == (), version_key(d)))
    folders.sort(key=lambda d: version_key(d))
    result = {}
    origin = {}
    for folder in folders:
        path = os.path.join(root, folder, "__brarchive", archive)
        if not os.path.exists(path):
            continue
        for name, body in read_archive(path).items():
            result[name] = body
            origin[name] = folder
    return result, origin


def main():
    root, archive, outdir = sys.argv[1], sys.argv[2], sys.argv[3]
    files, origin = merged(root, archive)
    os.makedirs(outdir, exist_ok=True)
    for name, body in files.items():
        with open(os.path.join(outdir, name), "wb") as f:
            f.write(body)
    print(len(files), "files from", len(set(origin.values())), "layers")


if __name__ == "__main__":
    main()
