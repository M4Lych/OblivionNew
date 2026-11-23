# OblivionMod: 3D Model Stage

The home screen renders a skinned, animated 3D character behind the dashboard cards (OpenGL ES 3.0, toon shading with outline and crimson rim light).

## Controls
- Touch or hover on the stage: the character looks toward the pointer. Tap: a short reaction (smile, head tilt, small hop).
- Eye button (bottom right): hides or shows the cards to see the model fully.
- Settings > Basic > OblivionMod Model Stage: show or hide the stage, choose the model, 30 or 60 fps, glass (translucent) cards.

## Animation
Procedural, driven by the rig: breathing on the spine, slow body sway, spring-bone wave on hair, skirt, tail, ears, ribbons and sleeves, random blinking and a gentle smile through morph targets, head and neck following the pointer. Arms are lowered from the T pose when the model is converted.

## Adding or replacing a model (OblivionMod)
Put a folder in `Android/data/<app package>/files/oblivion/models/<id>/` (or the app's internal `files/oblivion/models/<id>/`). A folder with the same id as a built-in model replaces it.

```
<id>/
  model.json
  model.obm
  textures/*.png
```

`model.json` fields: `id`, `name`, `mesh`, `materials[]` (`name`, `diffuse`, `shade`, `emissive`, `alpha`, `outline`; texture paths are relative to the folder), `camera` (`fov`, `targetY`, `distance`, `offsetX` where offsetX shifts the model sideways in screen space), `animation` (`breath`, `sway`, `springs`, `lookAt`, `blink`), `bones` (`hip`, `spine[]`, `neck`, `head`, `springPrefix`), `morphs` (`blink`, `smile` keys inside the OBM file).

## Converting a model
```
pip install numpy pillow
python3 tools/model/fbx_to_obm.py model.fbx textures_dir out_dir my_id "My Model"
python3 tools/model/preview_obm.py out_dir preview.png
```
Requirements and limits: binary FBX with a skin, up to 65535 vertices after splitting, top 4 bone influences per vertex, textures downscaled to 1024. Materials are matched to textures by name tokens, so check `model.json` afterwards. The blink and smile morphs are picked from blend shape names (`Eye_2_*(CloseA)`, `Mouth_4_0(WaraiA)`); edit the patterns in `fbx_to_obm.py` for other rigs. Set `OBM_ARM_DROP=0` to keep the original arm pose.

## OBM format (little endian)
Header `OBM1`, then `vertexCount, indexCount, skinBoneCount, nodeCount, submeshCount, morphCount, reserved` (uint32), world matrix (16 float, column major), positions, normals, uvs, bone indices (uint16 x4), bone weights (float x4), indices (uint16), submeshes (`start, count, material`), nodes (`parent int32, nameLength uint16, skinIndex int32, name, local matrix, inverse bind matrix`), morph targets (`nameLength uint16, name, vertexCount x 3 float deltas`).

## Notes
- The bundled default model is a character rip shared by fans. Characters from commercial games are owned by their publishers; keep it for personal use and replace it before distributing the app publicly.
- Not yet verified on a device. If the stage stays empty, check `adb logcat -s OblivionStage`.
- The stage is a TextureView drawn at 30 fps by default and is paused while the window is not visible.
