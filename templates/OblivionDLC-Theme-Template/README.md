# OblivionDLC Theme Template

This is a starter manifest for a **data-only** OblivionDLC package. It can supply theme colors, images, intro/loading videos, an optional player model, and cosmetics.

## Package layout

Place `dlc.json` at the ZIP root. Add only the files referenced by the manifest:

```text
my-theme.zip
├── dlc.json
├── assets/
│   ├── logo.png
│   ├── home-background.png
│   ├── intro.mp4
│   └── loading.mp4
├── models/
│   ├── player.json
│   └── player.png
└── cosmetics/
    └── example-cape.png
```

All assets are optional. Remove a manifest entry when you do not include its file. Use a unique `id` containing letters, digits, dots, underscores, or hyphens. Keep every asset path relative to the ZIP root.

## Supported manifest fields

- `theme`: color tokens used by Oblivion screens. Colors must be valid Android color strings such as `#RRGGBB` or `#AARRGGBB`.
- `intro_ms`: intro duration, accepted range 1,000–60,000 milliseconds.
- `loading_ms`: minimum launch-screen duration, accepted range 1,000–120,000 milliseconds.
- `assets.logo`: image used for the Oblivion logo slot.
- `assets.home_background`: image used behind the Home dashboard.
- `assets.intro_video` and `assets.loading_video`: optional MP4 videos.
- `model` and `model_texture`: optional player model data and its texture. The JSON must follow the existing `PlayerModel.fromJson` format in the client source.
- `cosmetics`: optional cape/overlay assets with role, claim, membership, and price metadata.

`price` is metadata only in this local catalogue. It does not create a payment flow or grant an entitlement. Purchase/claim access must be validated by the configured Oblivion service. Never place API secrets, signing keys, executable code, or scripts inside a DLC.

## Install

Build a ZIP whose root contains `dlc.json`, then install it using **Resources → OblivionMod → Install DLC**. Select the ZIP from the file picker. Activating a theme takes effect for screens opened after activation; a restart may be needed for the Home/intro/loading surfaces to refresh consistently.

The launcher validates archive paths and size limits before extracting data. Still, creators should only install packages from sources they trust.
