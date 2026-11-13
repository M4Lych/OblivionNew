#!/usr/bin/env python3
"""Validate the generated Oblivion APK's package identity and ARM64 native payload.

This is a packaging preflight. It does not install the APK or prove runtime compatibility.
"""
from __future__ import annotations

import argparse
import re
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

EXPECTED_PACKAGE = "com.oblivion.client"
EXPECTED_MACHINE_AARCH64 = 183
MIN_LOAD_ALIGNMENT = 16 * 1024


def fail(message: str) -> None:
    raise ValueError(message)


def verify_elf64_aarch64(name: str, content: bytes) -> None:
    if len(content) < 64 or content[:4] != b"\x7fELF":
        fail(f"Native payload is not a valid ELF file: {name}")
    if content[4] != 2:
        fail(f"Native library is not ELF64: {name}")
    if content[5] != 1:
        fail(f"Native library is not little-endian ELF: {name}")

    machine = struct.unpack_from("<H", content, 18)[0]
    if machine != EXPECTED_MACHINE_AARCH64:
        fail(f"Native library is not AArch64 (e_machine={machine}): {name}")

    phoff = struct.unpack_from("<Q", content, 32)[0]
    phentsize = struct.unpack_from("<H", content, 54)[0]
    phnum = struct.unpack_from("<H", content, 56)[0]
    if phentsize < 56 or phnum == 0 or phoff + phentsize * phnum > len(content):
        fail(f"Native library has an invalid ELF program-header table: {name}")

    load_count = 0
    for index in range(phnum):
        offset = phoff + index * phentsize
        p_type, _flags, _p_offset, _vaddr, _paddr, _filesz, _memsz, p_align = struct.unpack_from(
            "<IIQQQQQQ", content, offset
        )
        if p_type != 1:  # PT_LOAD
            continue
        load_count += 1
        if p_align < MIN_LOAD_ALIGNMENT:
            fail(
                f"Native PT_LOAD segment is not 16 KB aligned ({p_align:#x}): {name}"
            )
        if p_align & (p_align - 1):
            fail(f"Native PT_LOAD alignment is not a power of two: {name}")
        p_offset = struct.unpack_from("<Q", content, offset + 8)[0]
        p_vaddr = struct.unpack_from("<Q", content, offset + 16)[0]
        if (p_offset - p_vaddr) % MIN_LOAD_ALIGNMENT != 0:
            fail(f"Native PT_LOAD file offset and virtual address are not 16 KB congruent: {name}")

    if load_count == 0:
        fail(f"Native library has no PT_LOAD segments: {name}")


def verify_package_identity(apk: Path, aapt: str | None) -> None:
    if not aapt:
        return
    result = subprocess.run(
        [aapt, "dump", "badging", str(apk)],
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        fail(f"aapt could not read APK manifest: {result.stderr.strip()}")
    match = re.search(r"^package: name='([^']+)'", result.stdout, re.MULTILINE)
    if not match:
        fail("aapt output did not contain the APK package name")
    if match.group(1) != EXPECTED_PACKAGE:
        fail(f"Unexpected application ID: {match.group(1)}")


def verify_apk(apk: Path, aapt: str | None) -> tuple[int, list[str]]:
    if not apk.is_file() or apk.suffix.lower() != ".apk":
        fail(f"APK file does not exist: {apk}")

    with zipfile.ZipFile(apk) as archive:
        corrupt = archive.testzip()
        if corrupt is not None:
            fail(f"APK ZIP integrity check failed at entry: {corrupt}")

        names = archive.namelist()
        if "AndroidManifest.xml" not in names:
            fail("APK does not contain AndroidManifest.xml")
        if not any(name.startswith("classes") and name.endswith(".dex") for name in names):
            fail("APK does not contain a DEX payload")

        native_names = [name for name in names if name.startswith("lib/") and name.endswith(".so")]
        if not native_names:
            fail("APK does not contain native libraries")
        abis = sorted({name.split("/", 2)[1] for name in native_names if name.count("/") >= 2})
        if abis != ["arm64-v8a"]:
            fail(f"Expected ARM64-only native libraries; found ABI directories: {abis}")

        for name in native_names:
            with archive.open(name) as stream:
                content = stream.read()
            verify_elf64_aarch64(name, content)

    verify_package_identity(apk, aapt)
    return apk.stat().st_size, native_names


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--aapt", help="Path to Android SDK build-tools aapt binary")
    args = parser.parse_args()

    try:
        size, native_names = verify_apk(args.apk, args.aapt)
    except (OSError, ValueError, zipfile.BadZipFile, struct.error) as error:
        print(f"APK VERIFICATION FAILED: {error}", file=sys.stderr)
        return 1

    print(f"APK VERIFICATION PASSED: {args.apk.name}")
    print(f"Size: {size:,} bytes; package: {EXPECTED_PACKAGE}; ABI: arm64-v8a")
    print(f"Native libraries: {len(native_names)}; all ELF64/AArch64 with 16 KB PT_LOAD alignment")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
