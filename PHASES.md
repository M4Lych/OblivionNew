# Oblivion Android — Three-Phase Delivery Plan and Status

This source tree is the merged result of Phase 1, Phase 2, and the Phase 3 source-hardening work. The Android build and real-device runtime acceptance checks remain explicitly open until GitHub Actions and a physical device have run them.

## Phase 1 — Project foundation

**Implemented and carried forward:**

- Android application identity is `Oblivion` / `com.oblivion.client`.
- The app hosts the installed official Minecraft Bedrock runtime through its in-process `MinecraftActivity` path; it does not intentionally dispatch gameplay to the standalone Minecraft launcher Activity.
- Launch checks for the official `com.mojang.minecraftpe` Google Play installation and an active Microsoft/Xbox account with a Minecraft profile.
- The home shell exposes Launch, OblivionMod, Microsoft accounts, worlds, resource packs, behavior packs, and skin packs.
- Native dependencies are fetched at pinned source revisions during CI; upstream and third-party license notices are retained.

## Phase 2 — Requested feature set

**Implemented and carried forward:**

- User-facing product areas are OblivionMod package management, Microsoft/Xbox accounts, and content import/export.
- Content categories are restricted to worlds, resource packs, behavior packs, and skin packs.
- Android document-picker import and create-document export flows are retained for supported Bedrock content archives.
- Old download, multi-instance, news, social, screenshots, server-list, and unrelated launcher surfaces are not exposed as normal app entry points.
- Import already rejects traversal paths and duplicate normalized names, stages extraction, and enforces entry-count, per-entry, manifest, and per-archive expanded-size limits.

## Phase 3 — Source hardening and CI validation

**Implemented in this delivery:**

- Imported archives are now limited to 2 GiB on disk; each archive remains limited to 30,000 entries, 2 GiB declared expanded content, 1 GiB per entry, and 1 MiB per manifest.
- Recursive `.mcpack` / `.mcaddon` processing is capped at three nested levels and a 4 GiB cumulative copied-byte budget across staged and extracted content. Streaming copies stop at their limit instead of relying only on ZIP metadata.
- Added unit tests for bounded streaming copies.
- Microsoft/Xbox account files and XAL crypto preferences are excluded from cloud backup and device-to-device transfer.
- `MainActivity` is no longer externally exported; the launcher Activity and required external protocol/file/auth callback handlers remain exported as needed.
- Debug builds are debuggable and unminified for testing. Release builds remain minified.
- Conscrypt Android is updated to 2.8.0; CI inspects the actual packaged native libraries rather than assuming alignment from dependency metadata.
- GitHub Actions now runs source checks and APK-verifier tests, executes Gradle unit tests, builds debug and unsigned release APKs, checks package identity/ARM64-only native payload/16 KB ELF load alignment plus file-offset congruence, and emits SHA-256 checksums.
- Added a physical-device test matrix and release checklist.

**Phase 3 validation performed in this environment:**

- Offline source preflight: **PASS — 71 checks**.
- Android XML: **468 documents parsed**.
- JSON resources: **18 documents parsed**.
- APK verifier self-tests: **PASS — 4 tests**.
- Python bytecode compilation: **PASS**.
- Native dependency preparation script Bash syntax: **PASS**.
- Four bundled ARM64 JNI libraries: **PASS — ELF64/AArch64, 16 KB PT_LOAD alignment, and file-offset/virtual-address congruence**.
- GitHub Actions workflow YAML: **parsed successfully**.

**Not completed / requires external execution:**

- The Gradle test/build command (`:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease`) could not start because Gradle 8.13 was not cached and this environment could not resolve `services.gradle.org` (`UnknownHostException`). Android SDK/NDK are not installed here.
- The GitHub Actions workflow has not yet been run in the user's repository. CI is configured to build and verify both APK variants there.
- Microsoft/Xbox authentication, in-process gameplay, return-to-Oblivion behavior, actual world/pack import-export, OblivionMod installation, device memory/thermal behavior, and crash recovery have not been tested on hardware.
- The CI release APK is intentionally **unsigned**. No release keystore or signing credentials are included. A production release still requires a user-controlled signing key and final device acceptance.

The matrix in `DEVICE_TEST_MATRIX.md` is the required acceptance record for physical-device validation. Do not treat static checks or a successful APK build as proof that Minecraft runtime behavior works on every version/device.

## Naming and upstream compatibility

Oblivion is an independent community client, not an official Mojang/Microsoft product. `com.mojang.minecraftpe` remains unchanged because it is the official Minecraft application package. LeviLamina, LeviMC package catalogue URLs, and upstream license/notice references may remain in compatibility or attribution code when required by the OblivionMod package ecosystem; they are not the Oblivion application identity.
