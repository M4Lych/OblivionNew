# Patch-Rebuild package notes

## Contents

This package combines the full Oblivion Android client source with the latest JNI compatibility work and a separate hosted-runtime experiment.

### Main client

- Existing client modules and feature source are preserved.
- `NativeBridgeHelper` now delegates JNI calls to the original `org.levimc.launcher.util.NativeBridgeHelper` class name expected by the closed `libgxcore.so` binary.
- The app-side Kotlin helper preserves native linker error details for diagnostics.
- ProGuard keeps the compatibility class and its JNI entry points.
- The source preflight recognizes only the two explicitly permitted legacy JNI bridge source files; other legacy package references remain rejected.
- Duplicate Conscrypt dependency declaration removed.
- README conflict marker removed from the package copy.

### Hosted runtime experiment

`hosted-runtime/` is an independent proof-of-concept project. It is intentionally not merged into the main app module, and the main app still uses its existing launch architecture. The hosted runtime is not yet proven to start Minecraft on the target phone or current Minecraft versions.

## Verification performed during packaging

- Main project `python3 scripts/verify_oblivion_source.py`: PASS (85 checks; 468 XML and 18 JSON files parsed).
- Hosted-runtime `python3 scripts/verify_source.py`: PASS (manifest, Activity factory, native forwarder structure).
- Main project `python3 scripts/test_verify_apk.py`: PASS (4 tests).
- Python bytecode compilation and native dependency shell syntax check: PASS.
- Gradle Android builds and on-device Minecraft launch: not performed in this environment.

## Current baseline reference

The compatibility/source-preflight changes are aligned to the repository's inspected `main` HEAD commit `209e0cd2e576c3f5cf991465a7e444f96d9671fb`. The archive is a consolidated source bundle, not a byte-for-byte Git clone; it avoids carrying Git metadata and local/private build configuration.
