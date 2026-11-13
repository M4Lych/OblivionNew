# Oblivion Android — Phase 2 Delivery Report

**Result: SOURCE PREFLIGHT PASS — Android build and device behavior remain unverified.**

## Delivered changes

1. **Feature-surface consolidation**
   - Confirmed the registered application activities are limited to the Oblivion home shell, worlds/packs content hub, account sign-in, OblivionMod package screens, in-client Minecraft runtime, and required XAL/authentication/crash components.
   - Confirmed old game-download, multi-instance, settings, about, external-mod, quick-launch, and options-editor screens are not registered as app entry points.
   - Confirmed screenshots and server lists are hidden in the content hub. The active content categories are worlds, resource packs, behavior packs, and skin packs.

2. **Import workflow review**
   - The content hub uses Android's document picker and imports supported Bedrock world/pack archives into the selected Minecraft storage profile.
   - The separate import-to-Minecraft path remains available for handing a selected supported file to Minecraft itself.
   - Archive extraction retains staged-directory handling and path traversal rejection.
   - Added defensive limits: maximum 30,000 ZIP entries, 2 GiB declared expanded archive size, 1 GiB per entry, and 1 MiB per pack manifest. Duplicate normalized paths are rejected before extraction.

3. **Export workflow review**
   - World export uses Android's create-document flow and `.mcworld` naming.
   - Pack export uses Android's create-document flow and `.mcpack` naming.
   - Existing batch export and selected-storage-profile handling are retained.

4. **Automated preflight expansion**
   - The offline source checker now validates import/export wiring, archive safety limits, and retired app-surface registration in addition to Phase 1 checks.

## Validation performed

- `python3 scripts/verify_oblivion_source.py` — **PASS: 61 checks**.
- XML resources — **466 parsed successfully**.
- JSON resources — **18 parsed successfully**.
- `python3 -m py_compile scripts/verify_oblivion_source.py` — **PASS**.
- ZIP archive integrity and re-extraction — performed before packaging.

## Not verified in this environment

- Gradle/Android compilation and unit-test execution.
- Actual Microsoft/Xbox sign-in on-device.
- Import/export against a real installed Google Play Minecraft Bedrock instance.
- OblivionMod package catalogue and native package installation on-device.
- In-process Minecraft launch, gameplay, and return-to-Oblivion behavior.

The environment does not have the Android SDK/NDK and could not retrieve the Gradle wrapper distribution. The included GitHub Actions workflow remains the supported ARM64 debug-build path. Phase 3 is reserved for cloud-build and real-device validation plus release hardening.
