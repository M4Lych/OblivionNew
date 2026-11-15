# Oblivion milestone 1 — implementation notes

This ZIP is a conservative update to the supplied project. The existing Android package ID, namespace, Minecraft launcher classes, native libraries, launch-session flow, and upstream license files are intentionally retained. In particular, do not rename `org.levimc.launcher`: doing so can invalidate manifest/component references, JNI bindings, persisted data, or existing launch integration.

## Implemented in this milestone

- Visible mod catalog branding changed to **OblivionMod**; the catalog URL now comes only from `oblivion.properties` / Gradle property `oblivion.mod_catalog_url`. With no endpoint configured it safely shows the bundled/cache catalog rather than downloading LeviModHub content. Existing `.levipack`/archive handling is preserved for compatibility.
- News branding changed to **Oblivion News**; remote news now uses `news_url` / `oblivion.news_url`, and the Firebase topic constant is `oblivion-news`. With no feed configured, the app uses its bundled/cached news. The Firebase publisher must be updated separately to publish to this topic.
- Existing Discord OAuth account-linking and Rich Presence code remains in Settings. A configurable Oblivion Discord invite button is used in the mod catalog when `discord.server_invite` is supplied. No bot token or client secret is stored in the APK.
- About page adds **M4Lych — Oblivion Modder** and points the project source/issues links to `M4Lych/Oblivion`, while retaining upstream LeviLauncher author/modder credit and the Apache-2.0 license.
- Added an Ave Mujica-inspired charcoal / muted lavender / plum palette for light and dark modes. This is a color-theme treatment; no copyrighted show artwork, logo, or music is included.
- Added a compact home profile card showing the active Microsoft/Xbox gamertag and Xbox profile avatar. It opens account management, and its **Manage Skins** button opens the existing skin-pack manager. This is not yet a 3D player-model preview or a direct custom-skin uploader.
- The launcher loading overlay uses the Oblivion leaf logo. This changes the launcher loading overlay only; it does not replace Mojang/Minecraft assets inside the game binary.

## Still requires developer services or assets

1. **Discord server roles / membership / bot:** requires an Oblivion Discord application and bot, a continuously available HTTPS backend, server-side Discord guild membership/role verification, and secure storage. The client must never decide VIP/Admin/Staff/Creator/Partner status by trusting a local preference. Roles listed in the request are not granted by this client-only milestone.
2. **Oblivion+ payment, DLC entitlements, and role-gated cosmetics:** requires a real entitlement service and clear purchase/claim policy. No paywall or fake premium state has been added.
3. **One-time Claim Codes and private package delivery:** requires a backend database with atomic one-time redemption, signed entitlements, replay protection, expiry/revocation, and authenticated downloads. Android app-private storage can reduce casual browsing but cannot prevent a device owner from extracting files from a rooted/unlocked device or from a compromised build. Absolute anti-copy guarantees are not possible.
4. **Twenty-language support:** the supplied project currently includes English plus ten translated locales (11 total). The other nine languages need human-reviewed translations; this milestone does not fabricate translations.
5. **First-run/update onboarding:** existing EULA and changelog flows are retained. The requested multi-step welcome wizard (language, rules, license, credits, social links, Microsoft and Discord connection, skip/accept, and re-show-on-update versioning) has not yet been added.
6. **Dungeons-style animated camp/home screen and 3D character renderer:** the provided screenshot is used as design guidance, but a real-time 3D avatar needs a renderer/model pipeline and game-compatible skin/model data. The profile card added here shows the Microsoft/Xbox avatar instead.
7. **Custom loading video:** no video asset was supplied. Add a licensed MP4/WebM and define its placement, fallback behavior, and decode budget before integrating a video surface into Minecraft loading.
8. **In-game UI, custom skin injection, and custom player models:** these require separate Bedrock-compatible runtime/mod integration. They are not implemented by changing the Android launcher UI and should be delivered only after validating the existing in-client launch runtime.
9. **Oblivion News/Mod feeds:** host valid HTTPS JSON feeds using the existing schemas before filling `oblivion.properties`. Do not point these values to an upstream feed unless you have permission and intentionally want that content.

## Configuration

Copy `oblivion.properties.example` to `oblivion.properties` and fill only services you control. Copy `discord.properties.example` to `discord.properties` to configure the existing Discord OAuth/Rich Presence application. These local files are ignored by Git. Do not place a Discord bot token, OAuth client secret, payment secret, or claim-code signing private key in either file or in the APK.

The project updater now checks releases at `https://github.com/M4Lych/Oblivion`. If the repository is private, the unauthenticated release checker may not be able to read releases until a public release or authenticated update service is provided.

## Validation

XML files are parsed and resource IDs/references are checked as part of this archive preparation. A full Android Gradle build and device runtime test are not asserted here. The wrapper could not fetch the Gradle 8.13 distribution because DNS resolution for `services.gradle.org` failed, and no Android SDK path is configured in this environment. Build with the project wrapper, JDK 21, Android SDK 36, NDK `28.2.13676358`, and required CMake toolchain; then test launch on the target device before merging.
