# Local native dependency required for Minecraft launch

The prebuilt `libgxcore.so` in this project declares `libdiscord_partner_sdk.so` as a direct ELF dependency. The app must package that dependency for Android's dynamic linker to load `gxcore`.

## Required file

The project accepts one of these formats:

- `app/libs/discord_partner_sdk.aar` containing `jni/arm64-v8a/libdiscord_partner_sdk.so` (upstream-style AAR); or
- `app/src/main/jniLibs/arm64-v8a/libdiscord_partner_sdk.so` (standalone native library).

Supply an authorized ARM64/AArch64 build of the Discord Partner SDK in exactly one of those locations. The source archive intentionally does not contain the SDK binary. The component is separately licensed and is not covered by the Apache-2.0 license of the LeviLauncher Android base project. Check the applicable Discord/SDK terms before using or distributing a build that contains it.

Gradle task `verifyDiscordPartnerSdk` runs before native-library merge tasks. It checks ELF64, little-endian encoding, and AArch64 (`e_machine = 183`). It stops the APK build with a direct error when the file is missing, duplicated, or has an incompatible ABI, rather than producing an APK that will fail later during launch.

## Diagnostics

`NativeBridgeHelper.ensureGxCoreLoaded()` now logs the actual `System.loadLibrary("gxcore")` exception under the `NativeBridgeHelper` tag. If loading still fails after the dependency is installed, inspect Android logcat for that tag; the original linker message should identify the next missing dependency or incompatibility.

## Install from a local ZIP (authorized copies only)

The project includes `scripts/install_discord_partner_sdk.py`. From Termux, for example:

```bash
python3 scripts/install_discord_partner_sdk.py ~/storage/downloads/LeviLib.zip
```

The helper extracts only the root-level `libdiscord_partner_sdk.so`, checks that it is ELF64/AArch64, and writes it to the standalone `.so` path above. This does not grant redistribution rights; do not upload the SDK to a public repository unless its license permits that.
