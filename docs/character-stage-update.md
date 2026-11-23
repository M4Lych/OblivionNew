# Character Stage Update

The built-in 3D stage now presents the default Daitaku Helios Ballroom model in a closer upper-body composition and uses a more expressive procedural idle animation.

## What changed

- Camera framing is closer to the reference composition, with the character shifted slightly toward the right.
- Idle motion now includes gentle breathing, a small hip/body weight shift, head and neck micro-movements, and subtle shoulder movement.
- Existing spring-bone motion remains enabled for hair, ears, skirt details, ribbons, and tail.
- Existing blink and smile morphs remain enabled.
- Touching the character still triggers a short reaction; dragging or hovering continues to steer the character's gaze.

## Model source and runtime format

The supplied `Daitaku Helios Ballroom Joyful Jamboree!` FBX was checked against the built-in `default` OBM asset. Converting the supplied FBX with `tools/model/fbx_to_obm.py` produced a `model.obm` identical to the existing asset, and all supplied texture maps matched the built-in texture maps pixel-for-pixel. The model was therefore not duplicated in the APK.

The FBX contains a skinned rig and the `blink` / `smile` morph targets, but it does not contain FBX animation-stack clips. The stage uses the app's runtime procedural animator for its idle and interaction motion.

## Where to configure it

Open Settings and find **OblivionMod · 3D Model Stage**. The stage is enabled by default on a fresh install. The existing controls let the user toggle the stage, toggle glass cards, change the selected model pack, and switch between 30 and 60 fps.

Custom model packs can be placed in the external app files directory under `oblivion/models`, with a `model.json`, `model.obm`, and referenced textures.
