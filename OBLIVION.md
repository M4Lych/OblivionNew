# Oblivion – feature layer

Everything new lives in `app/src/main/java/org/levimc/launcher/oblivion/` plus a few small hooks.
The launch pipeline (`MinecraftLauncher`, `MinecraftRuntimePreparer`, preloader, mods, auth) is untouched.

## Configure (edit `app/src/main/res/values/oblivion_config.xml`)
| Key | Purpose |
|---|---|
| `oblivion_dev_name`, `oblivion_dev_github` | Your card in **About** |
| `oblivion_discord_invite`, `oblivion_social_youtube`, `oblivion_social_instagram` | Onboarding + hub links |
| `oblivion_api_base`, `oblivion_server_pubkey` | Server for Discord roles, claim codes (see `server/`) |
| `oblivion_news_url` | URL of your `news.json` (OblivionNews). Empty = bundled news only |
| `oblivion_update_owner`, `oblivion_update_repo` | GitHub repo for update checks. Empty = update check off |

> Update checks were pointing at the LeviDroid-Modder releases (it would offer to replace Oblivion with that APK).
> They are now off until you set your own repo.

## What each request became
1 OblivionMod / 2 OblivionNews – renamed in all languages. 3/7/18 Discord link, roles, membership, bot – `OblivionSession` +
`server/` (needs hosting). 4 About – developer card. 5 Dungeons-style start screen – `DungeonHomeView`
(START GAME forwards to the normal launch button). 6 OblivionDLC – install a `theme.json`/zip in Hub → recolours Oblivion
screens. 8 Ave-Mujica-inspired dark/crimson/violet look (original artwork, no copied assets). 9 Loading background –
`LoadingBackground` (video from Hub, or `assets/oblivion/loading.mp4`, else animated embers). 11 Oblivion logo on the
loading screen. 12 Skin Changer – writes a skin pack into the selected instance. 13/15 Cosmetics + preview, role/claim
gated. 14 Claim codes – server-side atomic one-use redemption; packages stored AES-GCM encrypted (Android Keystore) in
app-private storage. 16 Client settings in Hub. 17 Changelog (`resources/launcher/changelog.json`). 19 First-run wizard,
re-shown when the app version changes. 20 Languages – 20 offered; Oblivion screens are translated in all 20, the rest of
the app uses the upstream translations (11 languages) and falls back to English for the other 9.

## Not done / honest limits
- 10 In-game UI redesign and custom player *geometry* models need game-side work (not attempted).
- Role/claim checks are enforced on the server; a patched APK can bypass client-side checks. Files Minecraft must read
  end up readable while the game uses them.
- The server is a reference implementation; deploy and test it before relying on it.
- Nothing here was compiled in the authoring environment – the first CI run may need small fixes.
