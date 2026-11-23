# OblivionMod: Better World Generation

## What is in this build
- `app/src/main/cpp/oblivion_worldgen`: a native terrain engine (C++17, no dependencies).
  - Overworld: warped multi-scale noise, smooth continental/erosion/mountain layers, configurable height range up to 4000 (default 1000 in the preview), meandering rivers with banks and valleys, lakes, deeper oceans, cheese caverns plus tunnel caves with natural entrances.
  - Nether: floor/ceiling shaped cavern with pillars and a lava sea band.
  - End: central island plus smooth outer islands.
  - C API (`owg_configure`, `owg_surface_height`, `owg_overworld_density`, `owg_nether_density`, `owg_end_density`) for the future engine binding, and JNI used by the launcher.
- `tools/preview.cpp`: host-only tool that renders maps and prints terrain statistics. Build with `g++ -std=c++17 -O2 -Iinclude -Isrc src/*.cpp tools/preview.cpp`.
- Launcher side: `core/worldgen/OblivionWorldGen.java` and a Settings card (enable, Nether, End, maximum height, terrain style, river size).
- Previews in `docs/worldgen_preview`.

## Preview statistics (seed 424242, 8192 x 8192 blocks, max height 1000)
- ocean 52.5%, land 47.5%
- median slope 0.056, 95th percentile 0.41, only 0.28% of land steeper than 1.5
- mean river channel width about 21 blocks
- highest sampled peak about 850 blocks

## What is NOT done yet
The engine binding. Minecraft Bedrock's own generator lives in `libminecraftpe.so`; replacing it needs the target game version, its symbol or signature table, and the chunk and sub-chunk layout, none of which can be known or tested without that binary. Until then the setting prepares the native engine but world creation in the game is unchanged.

Other open items: world heights above the vanilla limit need engine patches (dimension bounds, sub-chunk count, network and storage format); new dimensions, biomes, bosses and entities, and rebuilt vanilla structures come after the binding exists.

## Engine analysis (libminecraftpe.so 1.26.70.24_RC0, arm64-v8a)
- The library is stripped and does not export Minecraft classes by name, but it embeds `__PRETTY_FUNCTION__` strings. `tools/mcpe_scan.py` maps the `.eh_frame_hdr` function table and follows ADRP/ADD references to those strings.
- The same method now runs on the device (`src/locator.cpp`), so no byte signatures are needed and it keeps working across builds as long as those strings exist. `tools/locate_test.cpp` runs it on a copy of the library on a PC; for 1.26.70.24 it finds all 8 targets (`docs/mcpe_targets_1.26.70.24.json`).
- Note: in this library read-only data shares one segment with code (R+E), so strings must be searched in every readable segment.
- Targets: `OverworldGeneratorMultinoise::_prepareHeightsForCells`, `::prepareHeightsForColumn`, `OverworldGenerator2d::_prepareHeights`, `ChunkBlender::attenuateDensity`, `NetherGenerator::decorationPostProcessChunk`, `TheEndGenerator::decorationPostProcessChunk`, `::findSpawnPosition`, `BiomeSource3d::getBiomeArea`.

## Probe (developer option, off by default)
Settings > Better World Generation > Developer: probe engine hooks. It waits for the game library, locates the targets, and installs pass-through observers on `_prepareHeightsForCells` and `attenuateDensity`. Logcat tag `OblivionWG` prints the module path, base address, and for every target either its offset or `miss(str,xref,fn)` (strings found, references found, functions found). It does not change terrain.

## Behavior pack (data driven)
`addons/OblivionMod_BetterWorldGen_BP` overrides 26 overworld land biomes taken from the vanilla 1.26.70 pack. Only `minecraft:overworld_height` changes: the variation part of `noise_params` is reduced, which is what makes hills jagged. Rebuild it with `tools/brarchive.py` (unpack and merge the vanilla layers) and `tools/make_biome_pack.py`.
Limits: the effect on the multinoise generator used by this build is untested, rivers and oceans are not data driven in these files, and nothing here changes world height.
