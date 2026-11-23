# Ave Mujica theme — change notes
- Launcher: dark-first, crimson (#C2234A) + antique gold on near-black, flat cards with hairline borders, 14dp radius.
- Client (in-game mod menu): separate violet/silver "Mask" palettes (Mujica, Mask, Oblivion, Crucifix, Requiem, Silver, Vesper, Gilded, Ashen), sharp 4dp panel, squared FAB. Intentionally different from the launcher.
- Icon: generated from the supplied gear/crescent artwork (launcher icon, adaptive, monochrome, in-app logo, mod menu logo).
- Build: not compiled here (no Gradle/network). Run ./gradlew assembleDebug.

## UI structure following the reference video (v4)
- Navbar: left rail (56dp) — back, logo, then Launch / Download / Instances / About / Settings with the label under each icon, bell + avatar at the bottom. Active item has a crimson bar. Other positions (top/bottom/right) still work from Settings > Personalization.
- Home: header card (title, edition, tip, instance picker, Launch pill button) and three columns Mods | Content Management + Xbox Friends | Download content.
- Settings: title + subtitle on top, pill tabs (selected = filled crimson pill), content full width below.
- Splash: ringed logo, crimson wordmark, underline, "Preparing to launch" centred.
- Background: animated Wave in the accent colour is on by default.
- Cards 12dp radius with crimson hairline, pill buttons, plain accent-tinted icons.
- Palette stays Ave Mujica (crimson / gold on near-black); icon stays the Oblivion gear/crescent mark.

## Fix: Microsoft sign-in button cut off on short screens
- Rail tabs now sit in a scrollable area, with news / sign-in (avatar) / theme toggle pinned at the bottom, and sizes were made compact so they stay on screen in landscape phones.

## v5
- Navbar rail is icon-only again (hover or long-press shows the name).
- "LeviMC" mod page names are now "OblivionMod" in every language.
- News feed now reads resources/launcher/news.json from M4Lych/OblivionNew (branch Oblivion-Development); the bundled file was rewritten for Oblivion.
- New native module oblivion_worldgen plus a Settings card (see docs/OBLIVIONMOD_WORLDGEN.md).

## v6
- 3D model stage on the home screen (see docs/OBLIVION_MODEL_STAGE.md): skinned model, procedural idle animation, touch reactions, hide-cards button, settings card, and model packs that OblivionMod can add or replace.
- Runtime function locator for the engine probe (string anchored) replaces byte signatures.
- Behavior pack OblivionMod_BetterWorldGen_BP (addons/).
