# Ave Mujica theme — change notes
- Launcher: dark-first, crimson (#C2234A) + antique gold on near-black, flat cards with hairline borders, 14dp radius.
- Client (in-game mod menu): separate violet/silver "Mask" palettes (Mujica, Mask, Oblivion, Crucifix, Requiem, Silver, Vesper, Gilded, Ashen), sharp 4dp panel, squared FAB. Intentionally different from the launcher.
- Icon: generated from the supplied gear/crescent artwork (launcher icon, adaptive, monochrome, in-app logo, mod menu logo).
- Build: not compiled here (no Gradle/network). Run ./gradlew assembleDebug.

## Menu redesign (v2)
- Navbar: 56dp, text-only uppercase tabs, crimson underline on the active tab, letter-spaced wordmark.
- Home: hero title uppercase with a faint logo watermark; columns reordered (Download rail | Mods | Content+Xbox).
- Cards: 2dp corners with a thin crimson hairline; icon chips are outlined and tinted with the accent instead of multicolour fills.
- Logo: all density copies of the leaf icon replaced (drawable, mdpi..xxxhdpi, launcher foreground, notification icon).

## Vertical rail navbar (v3)
- Default navbar position is now "left": a 64dp icon-only rail (logo, Launch/Download/Instances/About/Settings, then news, account, theme at the bottom).
- Active item shows a crimson bar on its left edge.
- Hovering an icon (mouse/stylus/trackpad) or long-pressing it shows its name in a label to the right (NavHoverLabel).
- Existing installs that already saved a navbar position keep it; change it in Settings > Personalization or clear app data.
