# Oblivion Android — Phase 1 Source Review

**Result: PARTIAL PASS — source preflight passed; Android compilation and device runtime remain unverified.**

## Checks completed in this environment

| Check | Result | Evidence |
|---|---|---|
| Project identity | PASS | Gradle root project, Android namespace, application ID, `OblivionApplication`, launcher label and launcher assets checked. |
| In-process runtime wiring | PASS (static) | `MinecraftActivity` extends Minecraft's `com.mojang.minecraftpe.MainActivity`; `MinecraftLoadingActivity` routes into the Oblivion Activity. |
| Official Minecraft restriction | PASS (static) | Launcher and `MinecraftLauncher` both check the Play Store installation; the expected package is `com.mojang.minecraftpe`. |
| Microsoft account gate | PASS (static) | Launch enablement and launch preflight require an active account with a Minecraft profile. |
| Content feature scope | PASS (static) | Main screen entries and accepted content types are worlds, resource packs, behavior packs and skin packs. |
| English app resources | PASS (static) | Translated resource directories are absent; default app label and shell strings identify Oblivion. |
| Native dependency bootstrap | PASS (static) | Preloader and libHttpClient source revisions are pinned; the preloader JNI bridge adaptation is present. |
| Android resources | PASS | Parsed 466 XML files and 18 JSON files; parsed the Gradle version catalog. |
| Workflow and scripts | PASS | `bash -n scripts/prepare_oblivion_android_dependencies.sh`; GitHub Actions workflow contains manual dispatch, source preflight, unit-test and ARM64 debug APK steps. |
| Manifest component declarations | PASS (static) | All app-local components declared in the manifest have source class declarations. |

The reusable offline check is `python3 scripts/verify_oblivion_source.py`. GitHub Actions runs it before downloading native dependencies or starting the Android build.

## Checks not completed

- `./gradlew --version` could not download Gradle 8.13 because this execution environment could not resolve `services.gradle.org` (`UnknownHostException`).
- No Android SDK/NDK build could be run locally.
- GitHub Actions has not been run against a repository owned by the user.
- No APK has been installed or tested on a physical Android device. In particular, in-process launch, Microsoft sign-in, import/export, and native runtime compatibility still need device verification.

## Review notes

- This delivery is a source ZIP, not a compiled APK.
- Native dependencies are fetched from pinned public upstream commits during CI; they are intentionally not vendored into the ZIP.
- Remaining LeviLamina/LeviMC references are limited to upstream package ecosystem compatibility, source/asset attribution, and the pinned preloader integration. The app identity and first-party Android namespace are Oblivion.
- The original upstream license and third-party notices are retained.
