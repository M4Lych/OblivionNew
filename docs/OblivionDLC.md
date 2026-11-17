# OblivionDLC package format

A DLC is a **zip** (any extension, e.g. `.dlc`) with a `dlc.json` at the top level. It is data only - no code runs -
so it cannot break the launch pipeline. Install it in **Resources → OblivionMod → Install**.

```json
{
  "id": "my-dlc",                 // unique id (required)
  "name": "My DLC", "version": "1.0", "author": "You",
  "intro_ms": 5000,               // optional: intro length (1000-60000)
  "loading_ms": 10000,            // optional: minimum loading animation length (1000-120000)
  "theme": { "primary": "#1FD77F", "launch": "#1FD77F", "glass": "#B3262626", "text": "#FFFFFF" },
  "assets": {                     // optional files inside the zip
    "logo": "logo.png", "home_background": "bg.jpg",
    "intro_video": "intro.mp4", "loading_video": "loading.mp4"
  },
  "model": "model.json",          // optional custom player model
  "model_texture": "model.png",   // optional texture for that model (default: the player's skin)
  "cosmetics": [                  // optional extra cosmetics
    { "id": "halo", "name": "Halo", "type": "overlay", "asset": "halo.png", "roles": ["staff"], "price": 0 }
  ]
}
```

Theme tokens: `bg surface surfaceAlt stroke primary accent gold text textDim glass glassStroke launch launchLight info danger`.

## model.json
Boxes use Minecraft's box UV layout; units are pixels, y up, feet at the origin, the model faces +z.
`group` can be `head body rarm larm rleg lleg cape` to get the idle animation.
```json
{ "name": "Blocky", "parts": [
  { "name": "head", "group": "head", "origin": [-4,24,-4], "size": [8,8,8], "uv": [0,0] },
  { "name": "body", "group": "body", "origin": [-4,12,-2], "size": [8,12,4], "uv": [16,16] }
] }
```
`dlc-samples/ave-mujica-theme.dlc` is a ready-made theme example.
