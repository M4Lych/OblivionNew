#!/usr/bin/env python3
"""Narrowly allow the required, preserved GXCore JNI compatibility bridge."""
from pathlib import Path
import sys

checker = Path("scripts/verify_oblivion_source.py")
if not checker.is_file():
    sys.exit("ERROR: run this script from the Oblivion repository root.")

source = checker.read_text(encoding="utf-8")
start_marker = "# Ensure no source still declares the old first-party Java package.\n"
end_marker = "\n# No app signing keys, personal local.properties, or Google Services config in this source archive."
if source.count(start_marker) != 1 or source.count(end_marker) != 1:
    sys.exit("ERROR: source checker layout differs from the expected commit; no files changed.")
start = source.index(start_marker)
end = source.index(end_marker, start)
old_block = source[start:end]

new_block = '''# The closed, prebuilt libgxcore.so is linked to LeviLauncher's original JNI
# owner class. Permit that compatibility bridge and its Oblivion delegate only;
# every other legacy first-party package reference remains prohibited.
bridge_rel = "app/src/main/java/org/levimc/launcher/util/NativeBridgeHelper.java"
delegate_rel = "app/src/main/java/com/oblivion/client/util/NativeBridgeHelper.kt"
bridge_source = read(bridge_rel)
delegate_source = read(delegate_rel)
require("package org.levimc.launcher.util;" in bridge_source and
        "public final class NativeBridgeHelper" in bridge_source,
        "GXCore JNI compatibility class is missing or has the wrong package")
for signature in (
    "private static native boolean nativeScanImage(String inputPath);",
    "private static native boolean nativeRewriteImage(String inputPath, String outputPath);",
    "private static native void nativeBootstrapGxCore();",
    "private static native String nativeLoadMinecraftRuntime(String libraryPath);",
):
    require(signature in bridge_source,
            "GXCore JNI compatibility method signature is missing: " + signature)
for delegation in (
    "org.levimc.launcher.util.NativeBridgeHelper.scanImage(inputPath)",
    "org.levimc.launcher.util.NativeBridgeHelper.rewriteImage(inputPath, outputPath)",
    "org.levimc.launcher.util.NativeBridgeHelper.bootstrapGxCore()",
    "org.levimc.launcher.util.NativeBridgeHelper.loadMinecraftRuntime(libraryPath)",
):
    require(delegation in delegate_source,
            "Oblivion helper is not delegating through the GXCore compatibility bridge: " + delegation)

allowed_legacy_sources = {bridge_rel, delegate_rel}
legacy_hits: list[str] = []
for base in (APP / "src/main/java", APP / "src/test/java", APP / "src/androidTest/java"):
    if not base.exists():
        continue
    for source_path in list(base.rglob("*.java")) + list(base.rglob("*.kt")):
        relative_path = str(source_path.relative_to(ROOT))
        source_text = source_path.read_text(encoding="utf-8", errors="replace")
        if ("org.levimc.launcher" in source_text or "org/levimc/launcher" in source_text) and \\
                relative_path not in allowed_legacy_sources:
            legacy_hits.append(relative_path)
require(not legacy_hits,
        "Legacy first-party package references remain outside the approved GXCore JNI bridge: " +
        ", ".join(legacy_hits[:8]))
'''

if not old_block.startswith(start_marker) or "legacy_hits" not in old_block:
    sys.exit("ERROR: expected legacy-package check was not found; no files changed.")
backup = checker.with_suffix(checker.suffix + ".bak")
if backup.exists():
    sys.exit(f"ERROR: backup already exists ({backup}); inspect it before retrying.")
backup.write_text(source, encoding="utf-8")
checker.write_text(source[:start] + new_block + source[end:], encoding="utf-8")
print(f"Updated {checker}; original saved to {backup}.")
print("Next run: python3 scripts/verify_oblivion_source.py")
