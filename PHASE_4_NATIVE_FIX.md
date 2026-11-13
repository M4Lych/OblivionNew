# Oblivion Android — Phase 4 Native Launch Repair

**Status: source diagnostics and dependency validation implemented. A fresh APK and on-device launch are still pending the separately licensed Discord Partner SDK dependency.**

## Root cause identified

Inspection of the ARM64 ELF metadata confirms that the bundled `libgxcore.so` declares a direct `DT_NEEDED` dependency on `libdiscord_partner_sdk.so`. The required library was absent from the previous Oblivion Debug APK. The `libgxcore.so` in the project and the one in the supplied Levi native-library archive have the same SHA-256 digest, confirming that this is the same prebuilt library.

The `libdiscord_partner_sdk.so` in the supplied archive is ELF64/AArch64 and has 16 KB `PT_LOAD` alignment with congruent file offsets and virtual addresses. Its direct dependencies are Android system libraries (`libOpenSLES.so`, `libandroid.so`, `liblog.so`, `libm.so`, `libdl.so`, and `libc.so`).

## Source changes

- `app/build.gradle` now requires the Discord Partner SDK before any native-library merge task. It accepts either the upstream-style `app/libs/discord_partner_sdk.aar` (containing `jni/arm64-v8a/libdiscord_partner_sdk.so`) or a standalone file under `app/src/main/jniLibs/arm64-v8a/`.
- `scripts/install_discord_partner_sdk.py` can install the root-level `.so` from a developer-supplied local ZIP after checking the file's ELF64/AArch64 header. The helper was tested against the supplied archive and installed into a temporary test directory; the SDK binary is not present in the source ZIP.
- The Gradle check rejects missing or duplicate SDK forms and validates ELF64, little-endian, AArch64 (`e_machine = 183`) before packaging.
- `NativeBridgeHelper.kt` now logs the underlying `System.loadLibrary("gxcore")` error under the `NativeBridgeHelper` tag and preserves the detail for callers.
- `NativeImageGuard.java` includes that original linker detail in its preparation error instead of returning only a generic message.
- `UPSTREAM_NOTICES.md`, `README_ID.md`, and `LOCAL_NATIVE_DEPENDENCIES.md` document the separately licensed dependency and its expected location.

## Why the SDK binary is not included in this source ZIP

The Discord Partner SDK is a separately licensed component and is not covered by the Apache-2.0 license of the LeviLauncher Android base. The supplied source archive therefore does not redistribute the binary. Upstream LeviLauncher documents it as a separate AAR dependency with a token-based CI acquisition path; use an authorized copy and check the applicable terms before packaging or distributing it.

## Verification performed

| Check | Result |
|---|---|
| `python3 scripts/verify_oblivion_source.py` | PASS — 76 checks; 468 XML and 18 JSON parsed |
| `python3 scripts/test_verify_apk.py` | PASS — 4 tests |
| Python bytecode compilation | PASS |
| `bash -n scripts/prepare_oblivion_android_dependencies.sh` | PASS |
| `scripts/install_discord_partner_sdk.py` on a fresh temporary output | PASS — extracted and validated 8,645,792-byte ELF64/AArch64 file |
| `readelf` inspection of `libgxcore.so` and supplied `libdiscord_partner_sdk.so` | PASS — ELF64/AArch64; 16 KB load alignment |
| Gradle configuration/build | NOT RUN — wrapper could not download Gradle 8.13 because `services.gradle.org` DNS was unavailable |
| Fresh APK / physical-device Minecraft launch | PENDING — requires an authorized SDK binary and a new Android build |

A successful source preflight is not a substitute for Gradle compilation or a device launch. After the dependency is supplied and a new Debug APK is built, test Minecraft launch first; then recheck account sign-in and import/export features.
