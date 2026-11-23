import json
import mmap
import re
import struct
import sys

import numpy as np

PATTERN = re.compile(
    rb"[^\x00]{0,300}(?:levelgen|OverworldGenerator|NetherGenerator|TheEndGenerator|WorldGenerator|"
    rb"BiomeSource|ChunkBlender|NoiseParameterDerivation|MultiNoise|TerrainShaper|BlockVolume|"
    rb"prepareHeights|SubChunk|Dimension::|Dimension\.cpp)[^\x00]{0,300}\x00"
)


def read_elf(buf):
    shoff = struct.unpack_from("<Q", buf, 0x28)[0]
    shentsize, shnum, shstrndx = struct.unpack_from("<HHH", buf, 0x3A)
    secs = []
    for i in range(shnum):
        o = shoff + i * shentsize
        name, typ, flags, addr, off, size = struct.unpack_from("<IIQQQQ", buf, o)
        secs.append((name, typ, flags, addr, off, size))
    stroff = secs[shstrndx][4]
    out = {}
    for name, typ, flags, addr, off, size in secs:
        end = buf.find(b"\x00", stroff + name)
        out[bytes(buf[stroff + name:end]).decode()] = (addr, off, size)
    return out


def function_table(buf, secs):
    hdr_addr, hdr_off, hdr_size = secs[".eh_frame_hdr"]
    count = struct.unpack_from("<I", buf, hdr_off + 8)[0]
    table = np.frombuffer(buf, dtype="<i4", count=count * 2, offset=hdr_off + 12).reshape(count, 2)
    starts = (table[:, 0].astype(np.int64) + hdr_addr)
    fdes = (table[:, 1].astype(np.int64) + hdr_addr)
    order = np.argsort(starts)
    starts = starts[order]
    fdes = fdes[order]
    ranges = np.empty(len(starts), dtype=np.int64)
    for i, f in enumerate(fdes):
        ranges[i] = struct.unpack_from("<i", buf, int(f) + 12)[0]
    return starts, ranges


def find_strings(buf, secs):
    addr, off, size = secs[".rodata"]
    data = bytes(buf[off:off + size])
    found = {}
    for m in PATTERN.finditer(data):
        s = m.group(0)[:-1]
        if len(s) < 8:
            continue
        found[addr + m.start()] = s.decode("utf-8", "replace")
    return found


def adrp_add_xrefs(buf, secs, targets):
    addr, off, size = secs[".text"]
    insn = np.frombuffer(buf, dtype="<u4", count=size // 4, offset=off)
    pcs = addr + np.arange(len(insn), dtype=np.int64) * 4
    is_adrp = (insn & 0x9F000000) == 0x90000000
    idx = np.nonzero(is_adrp[:-2])[0]
    a = insn[idx].astype(np.int64)
    rd = a & 31
    immlo = (a >> 29) & 3
    immhi = (a >> 5) & 0x7FFFF
    imm = (immhi << 2) | immlo
    imm = np.where(imm & (1 << 20), imm - (1 << 21), imm)
    page = (pcs[idx] & ~0xFFF) + (imm << 12)
    result = {}
    tset = np.array(sorted(targets), dtype=np.int64)
    for delta in (1, 2):
        b = insn[idx + delta].astype(np.int64)
        ok = ((b & 0xFFC00000) == 0x91000000) & (((b >> 5) & 31) == rd) & (((b) & 31) == rd)
        tgt = page + ((b >> 10) & 0xFFF)
        pos = np.searchsorted(tset, tgt)
        pos[pos >= len(tset)] = 0
        hit = ok & (tset[pos] == tgt)
        for pc, t in zip(pcs[idx][hit], tgt[hit]):
            result.setdefault(int(t), []).append(int(pc))
    return result


def main():
    path = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else "scan.json"
    with open(path, "rb") as f:
        buf = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
        secs = read_elf(buf)
        starts, ranges = function_table(buf, secs)
        strings = find_strings(buf, secs)
        xrefs = adrp_add_xrefs(buf, secs, strings.keys())
        funcs = {}
        for taddr, pcs in xrefs.items():
            for pc in pcs:
                i = int(np.searchsorted(starts, pc, side="right")) - 1
                if i < 0 or pc >= starts[i] + ranges[i]:
                    continue
                key = int(starts[i])
                entry = funcs.setdefault(key, {"size": int(ranges[i]), "strings": []})
                entry["strings"].append(strings[taddr])
        for entry in funcs.values():
            entry["strings"] = sorted(set(entry["strings"]))
        report = {
            "functions_total": int(len(starts)),
            "strings_matched": len(strings),
            "functions_with_hits": len(funcs),
            "functions": {hex(k): v for k, v in sorted(funcs.items())},
        }
        with open(out, "w") as o:
            json.dump(report, o, indent=1)
        print(report["functions_total"], report["strings_matched"], report["functions_with_hits"])


if __name__ == "__main__":
    main()
