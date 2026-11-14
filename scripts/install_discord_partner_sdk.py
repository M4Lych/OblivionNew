#!/usr/bin/env python3
"""Install the ARM64 Discord Partner SDK .so from a locally supplied ZIP.

The source archive does not contain the separately licensed SDK binary.
Only run this helper with a copy that you are authorized to use.
"""
from __future__ import annotations

import argparse
import hashlib
import os
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = ROOT / "app/src/main/jniLibs/arm64-v8a/libdiscord_partner_sdk.so"
ENTRY = "libdiscord_partner_sdk.so"
MAX_LIBRARY_SIZE = 64 * 1024 * 1024


def validate_elf(data: bytes) -> None:
    if len(data) < 20 or len(data) > MAX_LIBRARY_SIZE:
        raise ValueError(f"Unexpected SDK binary size: {len(data)} bytes")
    if data[:4] != b"\x7fELF" or data[4] != 2 or data[5] != 1:
        raise ValueError("SDK file is not little-endian ELF64")
    machine = int.from_bytes(data[18:20], "little")
    if machine != 183:
        raise ValueError(f"SDK architecture mismatch: expected AArch64 (183), found {machine}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="Local ZIP that contains libdiscord_partner_sdk.so at its root")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT,
                        help=f"Output path (default: {DEFAULT_OUTPUT.relative_to(ROOT)})")
    parser.add_argument("--force", action="store_true", help="Replace an existing output file")
    args = parser.parse_args()

    archive = args.archive.expanduser().resolve()
    output = args.output.expanduser().resolve()
    if not archive.is_file():
        print(f"ERROR: input ZIP not found: {archive}", file=sys.stderr)
        return 2

    try:
        with zipfile.ZipFile(archive) as zf:
            matches = [info for info in zf.infolist() if info.filename == ENTRY and not info.is_dir()]
            if len(matches) != 1:
                raise ValueError(f"Expected exactly one root entry named {ENTRY}; found {len(matches)}")
            info = matches[0]
            if info.file_size <= 0 or info.file_size > MAX_LIBRARY_SIZE:
                raise ValueError(f"Unexpected ZIP entry size: {info.file_size} bytes")
            data = zf.read(info)
    except (OSError, zipfile.BadZipFile, RuntimeError) as exc:
        print(f"ERROR: cannot read SDK archive: {exc}", file=sys.stderr)
        return 2
    except ValueError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    try:
        validate_elf(data)
    except ValueError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    digest = hashlib.sha256(data).hexdigest()
    if output.exists():
        current_digest = hashlib.sha256(output.read_bytes()).hexdigest()
        if current_digest == digest:
            print(f"Already installed: {output}")
            print(f"SHA-256: {digest}")
            return 0
        if not args.force:
            print(f"ERROR: output already exists and differs: {output} (use --force to replace)", file=sys.stderr)
            return 2

    output.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=output.name + ".", suffix=".tmp", dir=output.parent)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(data)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temp_name, output)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)

    print(f"Installed authorized ARM64 SDK library: {output}")
    print(f"Size: {len(data):,} bytes")
    print(f"SHA-256: {digest}")
    print("Next: rebuild the Debug APK; Gradle will verify this ELF library before packaging.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
