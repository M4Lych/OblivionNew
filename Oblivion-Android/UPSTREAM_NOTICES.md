# Upstream and Third-Party Notices

This project is an independent derivative work. Renaming the application does not remove the licensing or attribution obligations of upstream components.

## Android launcher base

- Original project: https://github.com/LiteLDev/LeviLauncher-Android
- License: Apache License 2.0
- The original `LICENSE` file is retained at the repository root.
- Source-level attribution and third-party notices shipped with the upstream project are retained where applicable.

## Android controls compatibility module

- Upstream project: https://github.com/PojavLauncherTeam/PojavLauncher
- License: GNU Lesser General Public License v3.0 or later.
- The adapted module source is in `oblivion_controls/`.
- See `oblivion_controls/LICENSE` and `oblivion_controls/NOTICE`.

## Android native preloader

- Upstream project: https://github.com/LiteLDev/preloader-android
- Retrieved at pinned revision `cc08b40a31471d3e2dedb267f5f17b28da2b0604` by `scripts/prepare_oblivion_android_dependencies.sh`.
- It is downloaded during CI and is not redistributed in this source ZIP. Review its upstream repository for its current license and terms before making public binary distributions.

## Microsoft libHttpClient

- Upstream project: https://github.com/microsoft/libHttpClient
- Retrieved at pinned revision `f8ae1d9cf931d9d55542865eee395adca5bdbe41` with its declared Git submodules during CI.
- Its original license and notices remain part of the fetched dependency tree; it is not copied into this source ZIP.

## LeviLamina package ecosystem

- Upstream project: https://github.com/LiteLDev/LeviLamina
- Used as a third-party native-mod/package compatibility ecosystem through OblivionMod.
- Product/loader names and upstream identifiers are retained only where required for compatibility, attribution, package IDs, or external service URLs. They do not identify this application.


## Discord Partner SDK runtime dependency

- The prebuilt `app/src/main/jniLibs/arm64-v8a/libgxcore.so` has a direct ELF dependency on `libdiscord_partner_sdk.so` (`DT_NEEDED`).
- The Discord native Partner SDK is a separately licensed component; it is not covered by the Apache-2.0 license of LeviLauncher-Android. The project source archive does not redistribute this SDK binary.
- Before building, the developer must supply an authorized ARM64 copy either as the upstream-style `app/libs/discord_partner_sdk.aar` (containing `jni/arm64-v8a/libdiscord_partner_sdk.so`) or as `app/src/main/jniLibs/arm64-v8a/libdiscord_partner_sdk.so`. Gradle verifies the ELF64/AArch64 header and fails early if the file is missing, duplicated, or has the wrong ABI.
- The upstream LeviLauncher Android end-to-end setup documents the SDK as a separate AAR dependency and notes its CI/token-based acquisition path: https://github.com/LiteLDev/LeviLauncher-Android/blob/main/e2e/README.md
