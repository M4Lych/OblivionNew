import json
import mmap
import sys

import numpy as np

from mcpe_scan import read_elf

TARGETS = [
    ("OverworldGeneratorMultinoise_prepareHeightsForCells", "OverworldGeneratorMultinoise::_prepareHeightsForCells"),
    ("OverworldGeneratorMultinoise_prepareHeightsForColumn", "OverworldGeneratorMultinoise::prepareHeightsForColumn"),
    ("OverworldGenerator2d_prepareHeights", "OverworldGenerator2d::_prepareHeights"),
    ("ChunkBlender_attenuateDensity", "ChunkBlender::attenuateDensity"),
    ("NetherGenerator_decorationPostProcessChunk", "NetherGenerator::decorationPostProcessChunk"),
    ("TheEndGenerator_decorationPostProcessChunk", "TheEndGenerator::decorationPostProcessChunk"),
    ("TheEndGenerator_findSpawnPosition", "TheEndGenerator::findSpawnPosition"),
    ("BiomeSource3d_getBiomeArea", "BiomeSource3d::getBiomeArea"),
]

SIG_WORDS = 14


def wildcard_mask(words):
    mask = []
    prev_adrp = None
    for i, w in enumerate(words):
        m = 0xFFFFFFFF
        if (w & 0x9F000000) == 0x90000000 or (w & 0x9F000000) == 0x10000000:
            m = 0x0000001F
            prev_adrp = w & 31
        elif (w & 0x7C000000) == 0x14000000:
            m = 0xFC000000
        elif (w & 0x3B000000) == 0x18000000:
            m = 0xFF00001F
        elif (w & 0x7E000000) == 0x34000000 or (w & 0xFF000010) == 0x54000000:
            m = 0xFF00001F
        elif (w & 0x7E000000) == 0x36000000:
            m = 0xFFF8001F
        elif (w & 0xFFC00000) == 0x91000000 and prev_adrp is not None:
            m = 0xFFC003FF
            prev_adrp = None
        else:
            prev_adrp = None
        mask.append(m)
    return mask


def count_matches(text, words, mask):
    first = int(words[0] & mask[0])
    cand = np.nonzero((text & np.uint32(mask[0])) == np.uint32(first))[0]
    cand = cand[cand + len(words) < len(text)]
    ok = np.ones(len(cand), dtype=bool)
    for k in range(1, len(words)):
        ok &= (text[cand + k] & np.uint32(mask[k])) == np.uint32(int(words[k]) & mask[k])
        cand = cand[ok]
        ok = ok[ok]
    return cand


def main():
    path, scan_path, out = sys.argv[1], sys.argv[2], sys.argv[3]
    scan = json.load(open(scan_path))["functions"]
    with open(path, "rb") as f:
        buf = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
        secs = read_elf(buf)
        addr, off, size = secs[".text"]
        text = np.frombuffer(buf, dtype="<u4", count=size // 4, offset=off)
        results = []
        for ident, needle in TARGETS:
            hit = None
            for a, v in scan.items():
                if any(needle in s and "(" in s for s in v["strings"]):
                    hit = (int(a, 16), v["size"])
                    break
            if hit is None:
                print("missing", ident)
                continue
            rva, fsize = hit
            idx = (rva - addr) // 4
            words = text[idx:idx + SIG_WORDS].copy()
            mask = wildcard_mask([int(w) for w in words])
            matches = count_matches(text, [int(w) for w in words], mask)
            results.append({
                "name": ident,
                "rva": hex(rva),
                "size": fsize,
                "words": [int(w) for w in words],
                "mask": mask,
                "matches": int(len(matches)),
            })
            print(ident, hex(rva), fsize, "matches", len(matches))
        with open(out, "w") as o:
            json.dump(results, o, indent=1)


if __name__ == "__main__":
    main()
