#!/usr/bin/env python3
"""Oblivion: make libgxcore.so loadable without the (private) Discord Partner SDK.

The prebuilt libgxcore.so (from LeviLauncher) has DT_NEEDED=libdiscord_partner_sdk.so and imports
Discord_* symbols. Oblivion does not ship that SDK, so dlopen would fail. This script:
  1. repoints the NEEDED entry to an already-present library (libc.so, harmless duplicate)
  2. marks every undefined Discord_* import as WEAK (unresolved weak symbols bind to 0)
Idempotent. Usage: patch_gxcore.py path/to/libgxcore.so
"""
import struct, sys

def main(path):
    b = bytearray(open(path, 'rb').read())
    assert b[:4] == b'\x7fELF' and b[4] == 2 and b[5] == 1, 'expected ELF64 little-endian'
    shoff, = struct.unpack_from('<Q', b, 0x28)
    shentsize, shnum = struct.unpack_from('<HH', b, 0x3A)
    secs = []
    for i in range(shnum):
        o = shoff + i * shentsize
        name, typ, flags, addr, off, size, link, info, align, entsize = struct.unpack_from('<IIQQQQIIQQ', b, o)
        secs.append(dict(type=typ, off=off, size=size, link=link, entsize=entsize))
    dynsym = next(s for s in secs if s['type'] == 11)
    dynstr = secs[dynsym['link']]
    dyn = next(s for s in secs if s['type'] == 6)
    strtab = bytes(b[dynstr['off']:dynstr['off'] + dynstr['size']])

    def cstr(o):
        return strtab[o:strtab.index(b'\0', o)]

    # 1. NEEDED entry
    target = strtab.find(b'\0libc.so\0') + 1
    assert target > 0, 'libc.so string not found in dynstr'
    patched_needed = 0
    for o in range(dyn['off'], dyn['off'] + dyn['size'], 16):
        tag, val = struct.unpack_from('<qQ', b, o)
        if tag == 0:
            break
        if tag == 1 and cstr(val) == b'libdiscord_partner_sdk.so':
            struct.pack_into('<Q', b, o + 8, target)
            patched_needed += 1
    # 2. weak symbols
    weak = 0
    for o in range(dynsym['off'], dynsym['off'] + dynsym['size'], 24):
        name, info, other, shndx, value, size = struct.unpack_from('<IBBHQQ', b, o)
        if shndx == 0 and cstr(name).startswith(b'Discord_') and (info >> 4) == 1:
            b[o + 4] = (2 << 4) | (info & 0xF)
            weak += 1
    open(path, 'wb').write(b)
    print(f'NEEDED repointed: {patched_needed}, symbols made weak: {weak}')

if __name__ == '__main__':
    main(sys.argv[1])
