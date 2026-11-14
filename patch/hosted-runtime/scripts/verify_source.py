#!/usr/bin/env python3
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
manifest = ROOT / "app/src/main/AndroidManifest.xml"
factory = ROOT / "app/src/main/java/com/oblivion/client/hosted/OblivionHostedAppComponentFactory.java"
app = ROOT / "app/src/main/java/com/oblivion/client/hosted/OblivionApplication.java"
forwarder = ROOT / "app/src/main/cpp/oblivion_minecraftpe_forwarder.c"
errors = []

try:
    tree = ET.parse(manifest)
    root = tree.getroot()
    ns = {"a": "http://schemas.android.com/apk/res/android"}
    application = root.find("application")
    if application is None:
        errors.append("manifest has no application element")
    else:
        if application.get("{http://schemas.android.com/apk/res/android}appComponentFactory") != \
                "com.oblivion.client.hosted.OblivionHostedAppComponentFactory":
            errors.append("custom AppComponentFactory is not declared")
        hosted = [a for a in application.findall("activity")
                  if a.get("{http://schemas.android.com/apk/res/android}name") ==
                  "com.mojang.minecraftpe.MainActivity"]
        if len(hosted) != 1:
            errors.append("exactly one hosted Minecraft MainActivity declaration is required")
        else:
            node = hosted[0]
            if node.get("{http://schemas.android.com/apk/res/android}process") != ":minecraft":
                errors.append("Minecraft Activity must use the :minecraft process")
            meta = node.find("meta-data")
            if meta is None or meta.get("{http://schemas.android.com/apk/res/android}value") != "oblivion_minecraftpe":
                errors.append("NativeActivity forwarder metadata is missing or incorrect")
except Exception as exc:
    errors.append(f"manifest XML parse failed: {exc}")

paths = [
    factory, app, forwarder,
    ROOT / "app/src/main/java/com/oblivion/client/hosted/OblivionHostedProcess.java",
    ROOT / "app/src/main/java/com/oblivion/client/hosted/HostedMinecraftContext.java",
    ROOT / "app/src/main/java/com/oblivion/client/hosted/HostedActivityLifecycleCallbacks.java",
    ROOT / "app/src/main/java/com/oblivion/client/hosted/OblivionNativeBridge.java",
    ROOT / "app/src/main/cpp/CMakeLists.txt",
]
for path in paths:
    if not path.is_file(): errors.append(f"missing file: {path.relative_to(ROOT)}")

if factory.is_file():
    source = factory.read_text(encoding="utf-8")
    for required in ("OblivionHostedProcess.requireMinecraftClassLoader()", "Class.forName(className, true, gameLoader)"):
        if required not in source: errors.append(f"factory missing required behavior: {required}")
if forwarder.is_file():
    source = forwarder.read_text(encoding="utf-8")
    for required in ("dlopen(path, RTLD_NOW | RTLD_LOCAL)", 'dlsym(handle, "ANativeActivity_onCreate")', "ANativeActivity_finish(activity)", 'visibility("default")'):
        if required not in source: errors.append(f"native forwarder missing required guard/export: {required}")

if (ROOT / "app/src/main/cpp/CMakeLists.txt").is_file():
    cmake = (ROOT / "app/src/main/cpp/CMakeLists.txt").read_text(encoding="utf-8")
    if "add_library(oblivion_minecraftpe SHARED" not in cmake:
        errors.append("CMake does not build the expected native forwarder library")

all_text = "\n".join(p.read_text(encoding="utf-8", errors="replace")
                   for p in ROOT.rglob("*") if p.is_file() and p.suffix in {".java", ".c", ".xml", ".gradle"})
if "org.levimc.launcher" in all_text:
    errors.append("prototype unexpectedly depends on the LeviLauncher Java package")

if errors:
    print("FAIL")
    print("\n".join(f" - {e}" for e in errors))
    sys.exit(1)
print("PASS: manifest, hosted Activity factory and native forwarder structure validated")
