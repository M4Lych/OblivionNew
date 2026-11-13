# Oblivion Android — Physical Device Test Matrix

This document is the acceptance checklist for Phase 3. Tests below have **not** been run in this source-delivery environment. Record the device, Android build, Minecraft version, date, and evidence before marking a test as passed.

## Test environment

| Item | Record |
|---|---|
| Device model | ______________________________ |
| Android version / API | ______________________________ |
| ABI (`arm64-v8a`) | ______________________________ |
| Available RAM / free storage | ______________________________ |
| Minecraft version | ______________________________ |
| Minecraft install source | Google Play / Other: __________ |
| Oblivion build commit / artifact | ______________________________ |
| Test date | ______________________________ |

## Acceptance matrix

| ID | Area | Test | Expected result | Status / notes |
|---|---|---|---|---|
| DEV-01 | Install | Install the CI debug APK on a clean ARM64 device with official Minecraft already installed. | Package installs as `com.oblivion.client`; app opens without an immediate crash. | NOT RUN |
| DEV-02 | Install gate | Remove Minecraft or test with a non-Play-Store installation. | Oblivion explains the requirement and does not start the game runtime. | NOT RUN |
| DEV-03 | Install gate | Restore the official Google Play Minecraft installation. | Installer check passes and launch preconditions refresh. | NOT RUN |
| DEV-04 | Account | Sign in using the normal Microsoft/Xbox authentication flow. | Account is saved, gamertag/profile is visible, and no token or code is shown in logs/UI. | NOT RUN |
| DEV-05 | Account | Cancel sign-in, let a device code expire, then retry after network recovery. | Cancellation/expiry/network errors are reported cleanly; a new sign-in can start. | NOT RUN |
| DEV-06 | Account | Add a second account, switch active account, then remove one account. | Active account selection is consistent; removed credentials no longer launch Minecraft. | NOT RUN |
| DEV-07 | Launch | Launch the installed Minecraft version supported by the client. | Minecraft initializes through Oblivion's in-process Activity path; no external Minecraft launcher is opened. | NOT RUN |
| DEV-08 | Launch | Exit gameplay using the supported in-game/back flow and return to Oblivion. | Oblivion home is restored; app state and navigation remain usable. | NOT RUN |
| DEV-09 | Launch | Background/foreground the app during startup and during gameplay. | No duplicate runtime Activity, broken task stack, or unrecoverable black screen. | NOT RUN |
| DEV-10 | Launch | Verify touch controls, keyboard/mouse where available, audio, orientation, and display cutouts. | Input and display behavior are usable on the test device. | NOT RUN |
| DEV-11 | Worlds | Import a valid `.mcworld` through the Android document picker. | World imports once, appears in Minecraft, and can be opened. | NOT RUN |
| DEV-12 | Worlds | Export an imported world to a user-selected document destination. | Valid `.mcworld` is created and can be re-imported. | NOT RUN |
| DEV-13 | Resource packs | Import and export a valid resource `.mcpack`. | Pack is visible in the expected Minecraft pack list and can be activated. | NOT RUN |
| DEV-14 | Behavior packs | Import and export a valid behavior/script `.mcpack`. | Pack is visible and can be enabled in a test world where appropriate. | NOT RUN |
| DEV-15 | Skin packs | Import and export a valid skin-pack `.mcpack`. | Pack/skins appear in Minecraft's expected skin interface. | NOT RUN |
| DEV-16 | Add-ons | Import a valid `.mcaddon` containing nested `.mcpack` files. | Supported packs import; status/warnings accurately report skipped or unsupported content. | NOT RUN |
| DEV-17 | Storage | Repeat world/pack import and export using each supported internal/external storage option. | Correct storage profile is used; no silent cross-profile overwrite occurs. | NOT RUN |
| DEV-18 | Import safety | Try a malformed archive, a `../` traversal path, duplicate paths, an oversized entry, and deeply nested packages. | Import rejects unsafe input or skips over-depth items without writing outside the selected content directory or crashing the app. | NOT RUN |
| DEV-19 | Import recovery | Interrupt/cancel an import and check the content directory. | Temporary staging folders are cleaned or safely ignored; existing content remains intact. | NOT RUN |
| DEV-20 | OblivionMod | Browse the catalogue online and with network unavailable. | Browse works online; offline state is communicated without crashing. | NOT RUN |
| DEV-21 | OblivionMod | Install, update, and remove a compatible test package. | Package state is consistent and installed package files are correctly managed. | NOT RUN |
| DEV-22 | OblivionMod | Try an incompatible or malformed package. | Package manager reports incompatibility/failure and does not leave a falsely successful install state. | NOT RUN |
| DEV-23 | Stability | Run a sustained game session, then return to Oblivion. | No repeatable native crash, excessive thermal throttling, or unrecoverable memory failure. | NOT RUN |
| DEV-24 | Stability | Force-stop/relaunch Oblivion after a failed game startup. | App returns to a recoverable home state and logs preserve a useful failure reason. | NOT RUN |
| DEV-25 | Backup | Inspect Android backup behavior for app account state. | `files/xal/` and XAL crypto preferences are excluded from cloud backup and device transfer. | NOT RUN |

## Recommended device coverage

- One Android 12 ARM64 phone for the first end-to-end pass.
- At least one newer Android release and, where available, a device using 16 KB memory pages.
- One lower-memory ARM64 device to check runtime initialization and return-to-app behavior under memory pressure.

## Evidence to attach

- GitHub Actions run link and build/test summary.
- APK verifier output and the matching SHA-256 checksum.
- Android version, device model, ABI, Minecraft version, and install source.
- Logcat excerpt for any crash or failed runtime initialization. Remove access tokens, refresh tokens, device codes, email addresses, and other account identifiers before sharing logs.
- Screenshots/video of successful launch, content import/export, and any reproducible failure.

Do not mark a test passed based only on a source-code check or successful APK compilation. In particular, successful APK packaging does not prove that the runtime is compatible with every Minecraft release or OEM firmware.
