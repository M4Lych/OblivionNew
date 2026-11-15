# Oblivion

Oblivion is a Minecraft: Bedrock Edition launcher for Android (arm64-v8a). You import the game
you own (from Google Play or an APK/APKS) and it runs **inside the launcher** instead of installing
the Minecraft APK.

It is a fork of [LeviLauncher-Android](https://github.com/LiteLDev/LeviLauncher-Android) by LiteLDev
(Apache-2.0). **Credits for the launch engine, preloader and mod loading belong to the LeviLauncher authors.**
The Java package name `org.levimc.launcher` is intentionally kept because the native libraries bind to it.

## Features
- Import Minecraft Bedrock **26.x (1.26.x)** or newer from Google Play / APK / APKS; run it inside the launcher
- Microsoft account sign-in
- Import & export: worlds, resource packs, skin packs
- **OblivionMod** (native `.so` mods, formerly LeviMod)
- English by default; other languages via Settings

Removed from upstream: Discord, news/notifications, CurseForge, update checker, quick launch,
world/options editors, wallpaper/video background, built-in gameplay mods.
Some upstream code (package manager, instance backup/shortcuts) is kept dormant and has no UI entry.

## Build (GitHub Actions)
Workflow: `.github/workflows/build.yml`
- Push to any branch → **debug** + **beta** APKs (artifacts)
- Tag `v*` (e.g. `v1.0.0`) → debug + beta + **release**, published to GitHub Releases
- Manual run → choose `all | debug | beta | release`

Native sources are git submodules (`preloader`, `libHttpClient`); CI clones them automatically if missing.

Optional repository secrets:
| Secret | Purpose |
|---|---|
| `OBLIVION_KEYSTORE_BASE64`, `OBLIVION_KEYSTORE_PASSWORD`, `OBLIVION_KEY_ALIAS`, `OBLIVION_KEY_PASSWORD` | Sign beta/release (otherwise debug key) |
| `GOOGLE_SERVICES_JSON_BASE64` | Real Firebase config (otherwise a placeholder is generated) |

Local build: put a `google-services.json` in `app/`, install NDK r28c + JDK 21, run `./gradlew assembleDebug`.

## Legal
Unofficial; not affiliated with Mojang or Microsoft. You must own Minecraft. Licensed Apache-2.0 (see LICENSE).
