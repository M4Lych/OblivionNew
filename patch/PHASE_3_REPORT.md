# Oblivion Android — Phase 3 Delivery Report

**Delivery status: Phase 3 source hardening complete. Android compilation and physical-device runtime acceptance are still pending.**

## Delivered in the merged Phase 1 + Phase 2 + Phase 3 source

### Runtime and account hardening

- The main home Activity is now `android:exported="false"`. App launch remains through the exported Splash Activity; required Minecraft file/protocol and Microsoft/XAL callback handlers remain available.
- Microsoft/Xbox account state in `files/xal/` and the XAL crypto SharedPreferences file are excluded from both legacy full backup and Android 12+ cloud/device-transfer backup rules.
- The debug variant is now debuggable and unminified for diagnostic testing. The release variant remains minified and non-debuggable.

### Import safety

- Source archive file size is capped at 2 GiB, including files staged from Android document-provider URIs.
- Per-archive declared expanded data remains capped at 2 GiB; one ZIP entry is capped at 1 GiB; archive entry count is capped at 30,000; pack manifests are capped at 1 MiB.
- Nested `.mcpack` / `.mcaddon` archives are limited to three nested levels and a cumulative 4 GiB copied-byte budget across staged and extracted nested content.
- Stream copying enforces byte limits while copying, rather than trusting the archive's declared ZIP sizes alone.
- Added unit tests for normal bounded copies, over-limit rejection, and invalid negative limits.

### Build and packaging checks

- GitHub Actions executes the offline project preflight and APK verifier self-tests.
- CI runs `:app:testDebugUnitTest`, assembles the debug APK, and assembles the minified release APK.
- Conscrypt Android was updated from 2.5.3 to 2.8.0; CI verifies the native libraries in the assembled APK instead of trusting dependency version declarations alone.
- `scripts/verify_apk.py` validates ZIP integrity, the `com.oblivion.client` application ID, ARM64-only native library folders, ELF64/AArch64 architecture, 16 KB `PT_LOAD` alignment, and 16 KB file-offset/virtual-address congruence.
- CI publishes separate debug and unsigned release artifacts and a SHA-256 checksum file. Test reports are retained as a separate artifact.
- The release APK is unsigned by design; no private signing key is stored in the repository or source archive.

## Validation evidence completed here

| Check | Result |
|---|---|
| `python3 scripts/verify_oblivion_source.py` | PASS — 71 checks |
| XML resources | PASS — 468 parsed |
| JSON resources | PASS — 18 parsed |
| `python3 scripts/test_verify_apk.py` | PASS — 4 tests |
| Python `py_compile` | PASS |
| `bash -n scripts/prepare_oblivion_android_dependencies.sh` | PASS |
| GitHub Actions YAML parse | PASS |
| Bundled `libPlayFabMultiplayer.so`, `libgxcore.so`, `libinbuiltmods.so`, `libmaesdk.so` | PASS — ELF64/AArch64, 16 KB PT_LOAD alignment and congruence |
| Gradle Android build | NOT RUN — Gradle distribution download failed due network DNS resolution |
| Android physical-device runtime | NOT RUN — no connected Android test device in this environment |

## Build limitation

A local attempt to run `./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` stopped before Gradle could configure the project because `services.gradle.org` could not be resolved (`UnknownHostException`). Android SDK/NDK are not installed in this execution environment. This does not prove the Android build fails; it means the build result is currently unverified. The updated GitHub Actions workflow is configured to perform that build in a GitHub-hosted runner.

## Device acceptance still required

Use `DEVICE_TEST_MATRIX.md` to test the official Google Play Minecraft installation, Microsoft/Xbox sign-in, in-process launch, return-to-app behavior, all four content import/export categories, OblivionMod package operations, and resource/memory/crash behavior on the target ARM64 phone.

**Important:** This ZIP is the complete source project for the three phases, not an installable APK. The first debug APK becomes available only after the GitHub Actions build completes successfully. The unsigned release APK is for release-build verification, not production distribution.
