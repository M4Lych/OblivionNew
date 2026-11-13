<<<<<<< HEAD
# Oblivion for Android

**An ARM64 Minecraft Bedrock client shell that runs the official Minecraft Android runtime inside Oblivion.**

Oblivion is an Android application derived from the open-source LeviLauncher Android project. Its launcher identity, Android package namespace, application screens, and exposed feature set have been adapted for this project. The game itself is not bundled: Oblivion uses the licensed Minecraft Bedrock installation already installed from Google Play.

> Oblivion is an independent community project. It is not affiliated with, endorsed by, or supported by Mojang Studios or Microsoft.

## Product scope

The current client shell is intentionally limited to these user-facing areas:

- **Launch** — start Minecraft through Oblivion's in-process `MinecraftActivity`, not by opening the standalone Minecraft launcher Activity.
- **OblivionMod** — browse and manage supported native mod packages through the package manager.
- **Content** — import, export, inspect, and remove worlds, resource packs, behavior packs, and skin packs.
- **Microsoft account** — sign in to and manage Microsoft/Xbox accounts used by Minecraft. A valid active Minecraft account is required before Launch is enabled.

The launcher refuses to run unless the installed Minecraft package is `com.mojang.minecraftpe` and Android reports that it was installed by Google Play. Sideloaded or pirated Minecraft packages are not supported. The app does not download, redistribute, or bundle Minecraft APKs or game assets.

## Requirements

- Android 9.0 (API 28) or newer.
- ARM64 device (`arm64-v8a`).
- Official Minecraft Bedrock Edition installed from Google Play on the same device.
- A Microsoft account with Minecraft/Xbox access, signed in through Oblivion.
- Enough free storage for Minecraft and the user's worlds and packs.

The minimum Android and ABI requirements come from this Android build configuration. The target phone is responsible for real-device performance testing; native runtime compatibility can vary by Minecraft release and device firmware.

## Build with GitHub Actions

The repository includes `.github/workflows/android.yml`. A GitHub Actions runner installs Java 21, Android SDK, Android NDK r28c, and CMake, prepares the pinned native dependencies, runs Gradle unit tests, and assembles ARM64 debug and minified release APKs. It then checks APK package identity, ARM64-only native libraries, 16 KB ELF load alignment and file-offset congruence, and SHA-256 checksums. The release APK is unsigned.

1. Create a new GitHub repository for Oblivion and upload the contents of this project to its root. Do not upload `build/`, `.gradle/`, `.cxx/`, `local.properties`, or a personal `google-services.json`.
2. Open the repository's **Actions** tab and select **Oblivion Android CI**.
3. Choose **Run workflow**. The workflow also runs automatically for pushes and pull requests to `main`, `master`, or `develop`.
4. When the build job succeeds, open its run and download **Oblivion-Android-ARM64-Debug** for device testing. The **Oblivion-Android-ARM64-Release-Unsigned** artifact is for checking the minified release build, not production distribution.
5. Install the debug APK on an ARM64 Android device that already has the official Google Play Minecraft app installed. Complete Microsoft sign-in in Oblivion, then run the scenarios in `DEVICE_TEST_MATRIX.md`.

The workflow fetches the Android preloader and Microsoft's Android HTTP client at pinned commit IDs. It adapts the preloader's JNI entry-point names to Oblivion's Java namespace during the CI job. These dependencies are not copied into this source archive because they are fetched by the build script.

### Editing from Android

The source tree can be edited in Acode after extracting the ZIP. Use GitHub's web interface or Git from Termux to upload/push changes to your repository. GitHub Actions performs the Android SDK/NDK build in the cloud, so those large toolchains do not need to be installed on the phone.

## Important technical notes

- The Minecraft runtime integration is in `app/src/main/java/com/oblivion/client/core/minecraft/`. `MinecraftActivity` subclasses the Minecraft-provided `com.mojang.minecraftpe.MainActivity`; this is the core in-process launch mechanism.
- The Play Store gate is checked in both the launcher UI and `MinecraftLauncher` before the game Activity is started.
- Microsoft/Xbox authentication is prepared again during runtime initialization. The client also checks for an active account with a Minecraft profile before enabling Launch.
- Content import uses Android's Storage Access Framework. Export uses document/folder pickers; content is written to the selected destination.
- The app is configured for English resources and an English application locale.
- Internal file formats inherited from the upstream launcher may retain legacy extensions for compatibility. They are data-format identifiers, not the app name or Android package identity.

## Project verification status

Phase 3 adds account-data backup exclusions, a non-exported home Activity, bounded archive streaming and nested-archive limits, unit tests for byte-limited copies, plus CI APK verification for package ID, ARM64 ABI, and 16 KB native segment alignment and offset/address congruence. Conscrypt Android is updated to 2.8.0, and the assembled APK is checked directly. The offline source checks and APK-verifier self-tests pass in the current source delivery.

The Android Gradle build and physical-device runtime have not yet been verified here: the Gradle wrapper could not download its distribution in this environment. Run the GitHub Actions workflow, then complete `DEVICE_TEST_MATRIX.md`. A successful compile alone does not prove gameplay or import/export compatibility for every Minecraft version.

See `PHASE_3_REPORT.md` for the exact validation status and `RELEASE_CHECKLIST.md` before distributing a signed release.

## Upstream source and licensing

The Android application source is derived from [LiteLDev/LeviLauncher-Android](https://github.com/LiteLDev/LeviLauncher-Android), distributed under Apache License 2.0. The upstream license and applicable notices are retained in this repository. The Android controls compatibility module contains adapted PojavLauncher controls code under LGPL-3.0-or-later; its license and notice are retained under `oblivion_controls/`. `UPSTREAM_NOTICES.md` records the principal third-party components used by this fork.

Oblivion's use of the LeviLamina package ecosystem is a third-party integration for OblivionMod; LeviLamina is not the name or identity of this client.
=======
# OblivionNew
OblivienNew
>>>>>>> fe4e057d555b7df9eac19fc94615f908f835556e
