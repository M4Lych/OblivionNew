# Oblivion – feature layer

The Oblivion screens live in `app/src/main/java/org/levimc/launcher/oblivion/`. The launch pipeline
(`MinecraftLauncher`, `MinecraftRuntimePreparer`, preloader, mod loader, auth) is not changed; the new UI only
forwards taps to the existing launcher functions.

## New main screen (replaces the Levi home page)
Logo + "Oblivion" (tap = Oblivion Credits) · Party · Newsworthy ticker (tap = that news) · Friends (Xbox friends) ·
3D player model (tap = Cosmetics) · @Role + avatar + gamertag (tap = Profile) · version above LAUNCH · ▼ menu
(installed version / download from Google Play / import APK) · Resources · Settings.
Settings → "New Oblivion start screen" off = the classic Levi screen (fallback).

Other Levi screens (instances, content lists, mods, launcher settings, accounts, CurseForge, news catalogue,
Xbox friends) are kept and reachable from Resources / Settings / Profile; they use the Levi layouts with the
launcher colours, they were **not** re-laid-out.

## Intro and loading
Intro: `OblivionTiming.INTRO_MS` (5000 ms). Loading: `OblivionTiming.LOADING_MS` (10000 ms) – Minecraft is shown only
after that. Logs are behind a **Logs** button (text only); on failure you get Copy / Save log (Downloads/Oblivion) and
Back to Launcher; the X cancels. Videos: OblivionDLC `intro_video` / `loading_video`, or Profile-less shortcut:
`assets/oblivion/loading.mp4`.

## OblivionDLC (see docs/OblivionDLC.md)
Zip with `dlc.json`: theme colours, logo, home background, intro/loading video, custom player model, cosmetics,
timings. Data only (no code) by design.

## Configure `app/src/main/res/values/oblivion_config.xml`
`oblivion_dev_name` (M4LychMC), `oblivion_dev_github`, `oblivion_discord_invite`, `oblivion_social_*`,
`oblivion_api_base` + `oblivion_server_pubkey` (server), `oblivion_news_url`, `oblivion_update_owner/_repo`.

## Needs the server (`server/`)
Discord roles/membership, claim codes, DLC shop catalogue, Oblivion friends and **Party text chat**. Without it the
rest works.

## Honest limits
- **Party voice chat is not implemented** (needs a realtime media server); text chat only.
- Buying cosmetics/DLC is not implemented (no payment backend); locked items show how to unlock them.
- Minecraft's own Xbox achievements are not read; Profile shows client achievements.
- Friends = your Xbox friends (existing Levi screen). Party uses Oblivion friends (accepted on the server).
- Global resource packs: no separate Levi feature exists; the Resource packs tile opens the pack list.
- A DLC cannot change screen *structure* with code (security); it restyles via data.
- 3D model/UI code has not been compiled or run on a device in the authoring environment.
