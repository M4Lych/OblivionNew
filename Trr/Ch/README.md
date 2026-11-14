<div align="center">

# Oblivion

**An Android launcher/client for Minecraft Bedrock Edition**

[![Android](https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white)](#system-requirements)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64-blue)](#system-requirements)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue)](LICENSE)

</div>

Oblivion is an independent downstream fork of **LiteLDev/LeviLauncher-Android**. It keeps the upstream native Bedrock runtime and Microsoft/Xbox authentication integration while replacing the launcher identity, home/navigation UI, and exposed feature set.

> **Important architecture note:** Oblivion does not bundle Minecraft into the Oblivion APK and does not repackage the Play Store game as part of the launcher. It runs the game through the retained LeviLauncher native runtime in a launcher-owned activity/process. The official Minecraft for Android package (`com.mojang.minecraftpe`) must be installed from Google Play, and the existing Play Store/license validation remains enabled. Imported APK/APKS files are stored as additional version profiles; they do not bypass the Play Store ownership check.

## Included features

- Microsoft account sign-in and Xbox account selection.
- Automatic detection of the official Google Play Minecraft installation.
- Import of Minecraft APK/APKS files as additional version profiles.
- Native in-launcher execution through the upstream Bedrock runtime.
- **OblivionMod** package/mod management, retained from the upstream package engine.
- World, resource-pack, and skin-pack management, including import/export flows.
- English as the first-run/default app language.

The Download tab, news feed, external mod catalog, CurseForge browser, Discord Rich Presence, general Settings tab, behavior-pack manager, screenshots, and server manager are removed from the primary Oblivion UI. Upstream implementation code may remain in the source tree where removal would risk breaking shared runtime dependencies; these surfaces are not exposed in the default launcher flow.

## System requirements

- Android 9.0 (API 28) or later.
- ARM64 (`arm64-v8a`) device.
- Minecraft Bedrock for Android v26.x or later (`1.26.0.0+`).
- A valid Google Play installation and license for Minecraft.

The app's Gradle configuration limits packaged native libraries to `arm64-v8a`. Devices with only 32-bit ABIs are not supported.

## Get an APK from GitHub Actions

The workflow at `.github/workflows/android.yml` builds three variants on every push, pull request, or manual workflow dispatch:

| Variant | Application ID | Signing | Intended use |
| --- | --- | --- | --- |
| Debug | `org.oblivionmc.launcher.debug` | Android debug key | Development and device debugging |
| Beta | `org.oblivionmc.launcher.beta` | Android debug key | Installable public testing build |
| Release | `org.oblivionmc.launcher` | Unsigned by default | Final build output; sign with your own release key before distribution |

Download the `Oblivion-debug-apk`, `Oblivion-beta-apk`, or `Oblivion-release-unsigned-apk` artifact from the **Actions** run. The Beta APK is debug-signed and can be installed directly for testing. The Release APK is deliberately unsigned because this source archive does not contain a private signing key. Do not commit a keystore or signing passwords to the repository. If you later configure release signing, store signing material in GitHub Actions Secrets and update the workflow to sign the release output.

Pushing a tag named `v*` also creates a GitHub Release containing the unsigned Release APK.

## Build locally

### Toolchain

- JDK 21
- Android SDK Platform 36 and Build Tools 35.0.0
- Android NDK r28c (`28.2.13676358`)
- CMake 3.22.1
- Xmake 2.9.9

### Native dependencies

The source archive does not include the upstream native Git submodule contents. Fetch them before building:

```bash
./scripts/prepare_native_deps.sh
```

The script checks out `LiteLDev/preloader-android` tag `0.2.3` and `microsoft/libHttpClient` branch `main` into the CMake paths expected by this project. GitHub Actions performs the same checkouts automatically.

### Gradle commands

```bash
chmod +x ./gradlew
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug :app:assembleBeta :app:assembleRelease
```

The default installable test artifact is `app/build/outputs/apk/beta/`. Release output is under `app/build/outputs/apk/release/` and is unsigned unless you configure signing.

## Project identity and upstream credits

- Product name: **Oblivion**
- Mod/package manager label: **OblivionMod**
- Downstream Android application IDs: `org.oblivionmc.launcher*`
- Internal Java/Kotlin namespace `org.levimc.launcher` is retained to preserve upstream runtime, XAL callback, and native integration contracts. This is an implementation namespace, not the displayed product identity.
- Upstream authorship and notices remain credited in [UPSTREAM_CREDITS.md](UPSTREAM_CREDITS.md). The upstream Apache 2.0 license is retained in [LICENSE](LICENSE).

Oblivion is not an official Mojang or Microsoft product and is not affiliated with Mojang or Microsoft.
