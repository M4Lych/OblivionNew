#!/usr/bin/env python3
"""Self-tests for the generated-APK verifier using the project's ARM64 JNI library."""
from __future__ import annotations

import struct
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from verify_apk import verify_apk  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
FIXTURE_SO = ROOT / "app/src/main/jniLibs/arm64-v8a/libgxcore.so"


class VerifyApkTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory(prefix="oblivion-apk-verify-")
        self.addCleanup(self.temp.cleanup)
        self.apk = Path(self.temp.name) / "fixture.apk"
        self.native = FIXTURE_SO.read_bytes()

    def write_apk(self, abi: str = "arm64-v8a", native: bytes | None = None) -> None:
        with zipfile.ZipFile(self.apk, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            archive.writestr("AndroidManifest.xml", b"manifest-placeholder")
            archive.writestr("classes.dex", b"dex-placeholder")
            archive.writestr(f"lib/{abi}/libfixture.so", self.native if native is None else native)

    def test_accepts_arm64_16kb_aligned_native_library(self) -> None:
        self.write_apk()
        size, libs = verify_apk(self.apk, None)
        self.assertGreater(size, 0)
        self.assertEqual(["lib/arm64-v8a/libfixture.so"], libs)

    def test_rejects_non_arm64_abi(self) -> None:
        self.write_apk(abi="x86_64")
        with self.assertRaisesRegex(ValueError, "ARM64-only"):
            verify_apk(self.apk, None)

    def test_rejects_native_library_with_4kb_load_alignment(self) -> None:
        patched = bytearray(self.native)
        phoff = struct.unpack_from("<Q", patched, 32)[0]
        phentsize = struct.unpack_from("<H", patched, 54)[0]
        phnum = struct.unpack_from("<H", patched, 56)[0]
        for index in range(phnum):
            offset = phoff + index * phentsize
            p_type = struct.unpack_from("<I", patched, offset)[0]
            if p_type == 1:
                struct.pack_into("<Q", patched, offset + 48, 4096)
        self.write_apk(native=bytes(patched))
        with self.assertRaisesRegex(ValueError, "not 16 KB aligned"):
            verify_apk(self.apk, None)

    def test_rejects_native_library_with_noncongruent_load_offset(self) -> None:
        patched = bytearray(self.native)
        phoff = struct.unpack_from("<Q", patched, 32)[0]
        phentsize = struct.unpack_from("<H", patched, 54)[0]
        phnum = struct.unpack_from("<H", patched, 56)[0]
        for index in range(phnum):
            offset = phoff + index * phentsize
            p_type = struct.unpack_from("<I", patched, offset)[0]
            if p_type == 1:
                p_offset = struct.unpack_from("<Q", patched, offset + 8)[0]
                struct.pack_into("<Q", patched, offset + 8, p_offset + 4096)
                break
        self.write_apk(native=bytes(patched))
        with self.assertRaisesRegex(ValueError, "not 16 KB congruent"):
            verify_apk(self.apk, None)


if __name__ == "__main__":
    unittest.main(verbosity=2)
