#!/usr/bin/env python3
"""Fast, offline source checks for the Oblivion Android project.

This is a preflight check, not a substitute for Gradle compilation or a real-device test.
"""
from __future__ import annotations

import json
import re
import sys
import tomllib
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
RES = APP / "src/main/res"
errors: list[str] = []
checks = 0


def require(condition: bool, message: str) -> None:
    global checks
    checks += 1
    if not condition:
        errors.append(message)


def read(rel: str) -> str:
    path = ROOT / rel
    if not path.is_file():
        errors.append(f"Missing required file: {rel}")
        return ""
    return path.read_text(encoding="utf-8")


# Project identity and module wiring.
settings = read("settings.gradle")
app_gradle = read("app/build.gradle")
manifest_text = read("app/src/main/AndroidManifest.xml")
manifest = ET.fromstring(manifest_text) if manifest_text else None
require('rootProject.name = "oblivion"' in settings, "Gradle root project is not named oblivion")
require("namespace 'com.oblivion.client'" in app_gradle, "App namespace is not com.oblivion.client")
require('applicationId "com.oblivion.client"' in app_gradle, "Application ID is not com.oblivion.client")
require("include ':oblivion_controls'" in settings, "Oblivion controls module is not included")
require("include ':minecraft'" in settings, "Minecraft runtime facade module is not included")
versions_catalog = read("gradle/libs.versions.toml")
require('conscryptAndroid = "2.8.0"' in versions_catalog,
        "Conscrypt must use the updated 16 KB-aware Android artifact version")

# Validate application and runtime Activity declarations.
ns = {"android": "http://schemas.android.com/apk/res/android"}
application = manifest.find("application") if manifest is not None else None
app_name = application.attrib.get(f"{{{ns['android']}}}name", "") if application is not None else ""
activity_names = {
    el.attrib.get(f"{{{ns['android']}}}name", "")
    for el in (application.findall("activity") if application is not None else [])
}
require(app_name == "com.oblivion.client.core.minecraft.OblivionApplication",
        "Manifest does not use OblivionApplication")
require(".core.minecraft.MinecraftActivity" in activity_names,
        "Manifest does not declare the in-process MinecraftActivity")
require(".core.minecraft.MinecraftLoadingActivity" in activity_names,
        "Manifest does not declare MinecraftLoadingActivity")
require("com.mojang.minecraftpe" in manifest_text,
        "Manifest package visibility for official Minecraft is missing")
main_activity_node = next((node for node in (application if application is not None else [])
                           if node.tag == "activity" and
                           node.attrib.get(f"{{{ns['android']}}}name", "").endswith("MainActivity")), None)
require(main_activity_node is not None and
        main_activity_node.attrib.get(f"{{{ns['android']}}}exported") == "false",
        "MainActivity must not be externally exported")
require(application is not None and
        application.attrib.get(f"{{{ns['android']}}}fullBackupContent") == "@xml/backup_rules" and
        application.attrib.get(f"{{{ns['android']}}}dataExtractionRules") == "@xml/data_extraction_rules",
        "Manifest is missing explicit account-data backup exclusions")
backup_rules = read("app/src/main/res/xml/backup_rules.xml")
data_rules = read("app/src/main/res/xml/data_extraction_rules.xml")
for secret_path in ('path="xal/"', 'path="com.oblivion.client.xal.crypto.xml"'):
    require(secret_path in backup_rules and secret_path in data_rules,
            f"Sensitive account storage is not excluded from backups: {secret_path}")

# Manifest components may be declared in a multi-class Kotlin file; check declarations, not filenames.
component_sources = list((APP / "src/main/java").rglob("*.java")) + list((APP / "src/main/java").rglob("*.kt"))
component_text = [p.read_text(encoding="utf-8", errors="replace") for p in component_sources]
for node in (application if application is not None else []):
    if node.tag not in {"activity", "service", "receiver", "provider"}:
        continue
    component_name = node.attrib.get(f"{{{ns['android']}}}name", "")
    if component_name.startswith("."):
        component_name = "com.oblivion.client" + component_name
    if component_name.startswith("com.oblivion.client."):
        simple_name = component_name.rsplit(".", 1)[-1]
        declared = any(re.search(r"\b(class|object)\s+" + re.escape(simple_name) + r"\b", source)
                       for source in component_text)
        require(declared, f"Manifest component has no source class declaration: {component_name}")

main_java = read("app/src/main/java/com/oblivion/client/ui/activities/MainActivity.java")
mc_activity = read("app/src/main/java/com/oblivion/client/core/minecraft/MinecraftActivity.kt")
mc_launcher = read("app/src/main/java/com/oblivion/client/core/minecraft/MinecraftLauncher.java")
app_class = read("app/src/main/java/com/oblivion/client/core/minecraft/OblivionApplication.kt")
play_validator = read("app/src/main/java/com/oblivion/client/util/PlayStoreValidator.java")
require("class OblivionApplication : Application()" in app_class,
        "OblivionApplication class declaration is missing")
require("class MinecraftActivity : MainActivity()" in mc_activity,
        "MinecraftActivity does not subclass Mojang's MainActivity")
require("import com.mojang.minecraftpe.MainActivity" in mc_activity,
        "MinecraftActivity is not importing the Minecraft runtime Activity")
require('public static final String MC_PACKAGE_NAME = "com.mojang.minecraftpe"' in mc_launcher,
        "Minecraft package constant is incorrect")
require("PlayStoreValidator.isMinecraftFromPlayStore(activity)" in mc_launcher,
        "Native launch path is missing the Play Store install gate")
require('private static final String PLAY_STORE_INSTALLER = "com.android.vending"' in play_validator,
        "Play Store installer validation is missing")
require("hasActiveMinecraftAccount()" in main_java and "active.minecraftUsername" in main_java,
        "MainActivity is missing the Microsoft/Xbox account precondition")
require("hasVerifiedOfficialMinecraft()" in main_java,
        "MainActivity is missing the official Minecraft precondition")
require("minecraftLauncher.launch(launchIntent, version" in main_java,
        "MainActivity does not route Launch through MinecraftLauncher")
require('setClassName(this, "com.mojang.minecraftpe.' not in main_java,
        "MainActivity contains a direct external Minecraft Activity launch path")

# The requested visible content set is limited to worlds and three pack types.
content_list = read("app/src/main/java/com/oblivion/client/ui/activities/ContentListActivity.java")
for token in ("TYPE_WORLDS = 0", "TYPE_SKIN_PACKS = 1", "TYPE_RESOURCE_PACKS = 2", "TYPE_BEHAVIOR_PACKS = 3"):
    require(token in content_list, f"Expected content category is missing: {token}")
require("contentType > TYPE_BEHAVIOR_PACKS" in content_list,
        "Content list does not reject non-requested content categories")
require("TYPE_BEHAVIOR_PACKS" in main_java and "TYPE_SKIN_PACKS" in main_java,
        "Main screen is missing pack categories")
require("PackagesActivity.TAB_BROWSE" in main_java,
        "Main screen is not wired to the OblivionMod package browser")

# Phase 2: verify the complete requested content workflow and retired app surfaces.
content_hub = read("app/src/main/java/com/oblivion/client/ui/activities/ContentManagementActivity.java")
content_archive = read("app/src/main/java/com/oblivion/client/core/content/ContentArchive.java")
content_list = read("app/src/main/java/com/oblivion/client/ui/activities/ContentListActivity.java")
require("ContentImporter" in content_hub and "importContent(uris" in content_hub,
        "Content hub is missing direct archive import")
require("StorageAccess.downloadsPicker(!useMinecraft)" in content_hub,
        "Content hub is missing Android document-picker import")
require("contentManager.exportWorld" in content_list and "ContentManager" in content_list,
        "World export flow is missing")
require("exportResourcePack" in content_list and "ACTION_CREATE_DOCUMENT" in content_list,
        "Pack export flow is missing")
content_files = read("app/src/main/java/com/oblivion/client/core/content/ContentFiles.java")
require("MAX_ARCHIVE_ENTRIES" in content_archive and "MAX_ARCHIVE_FILE_BYTES" in content_archive
        and "MAX_ARCHIVE_UNCOMPRESSED_BYTES" in content_archive and "MAX_TOTAL_NESTED_EXPANSION_BYTES" in content_archive
        and "MAX_SINGLE_ENTRY_BYTES" in content_archive and "MAX_NESTED_ARCHIVE_DEPTH" in content_archive
        and "duplicate paths" in content_archive and "budget.add(copied)" in content_archive and
        "MAX_TOTAL_NESTED_EXPANSION_BYTES" in content_archive,
        "Archive import is missing source-size, expansion, nesting, or duplicate-path safeguards")
require("copyLimited" in content_files and "copyLimited" in content_archive,
        "Archive copies are not protected by streaming byte limits")
require((ROOT / "app/src/test/java/com/oblivion/client/core/content/ContentFilesLimitTest.java").is_file(),
        "Archive streaming-limit unit tests are missing")
require(".." in content_archive and "Invalid archive path" in content_archive,
        "Archive path traversal validation is missing")
retired_activities = {"SettingsActivity", "GameDownloadActivity", "GoogleLoginActivity",
                      "InstancesActivity", "ExternalModsActivity", "AboutActivity",
                      "QuickLaunchActivity", "OptionsEditorActivity", "ModsFullscreenActivity"}
registered_names = {name.rsplit(".", 1)[-1] for name in activity_names}
require(not (retired_activities & registered_names),
        "Retired launcher surfaces are still registered: " + ", ".join(sorted(retired_activities & registered_names)))
require("binding.screenshotsButton.setVisibility(android.view.View.GONE)" in content_hub
        and "binding.serversButton.setVisibility(android.view.View.GONE)" in content_hub,
        "Screenshots/server surfaces are not retired from the content hub")

# English-only resources: keep Android configuration qualifiers, not translated locales.
translated = {"values-es", "values-fr", "values-hi", "values-idn", "values-in", "values-ja",
              "values-pt", "values-ru", "values-tr", "values-vi", "values-zh-rCN"}
existing_translations = sorted(p.name for p in RES.iterdir() if p.is_dir() and p.name in translated)
require(not existing_translations,
        "Translated resource folders remain: " + ", ".join(existing_translations))
base_strings = read("app/src/main/res/values/strings.xml")
require('<string name="app_name">Oblivion</string>' in base_strings,
        "Default application label is not Oblivion")
require("oblivion_subtitle" in base_strings and "OblivionMod" in base_strings,
        "Oblivion branding strings are incomplete")

# Native dependency bootstrap and CI must be present and pinned.
prepare = read("scripts/prepare_oblivion_android_dependencies.sh")
workflow = read(".github/workflows/android.yml")
require("PRELOADER_SHA=\"cc08b40a31471d3e2dedb267f5f17b28da2b0604\"" in prepare,
        "Android preloader is not pinned to the reviewed commit")
require("HTTPCLIENT_SHA=\"f8ae1d9cf931d9d55542865eee395adca5bdbe41\"" in prepare,
        "Microsoft libHttpClient is not pinned to the reviewed commit")
require("Java_org_levimc_launcher_preloader_PreloaderInput_" in prepare and
        "Java_com_oblivion_client_preloader_PreloaderInput_" in prepare,
        "Native preloader JNI package adaptation is missing")
require("workflow_dispatch:" in workflow and ":app:assembleDebug" in workflow and
        ":app:assembleRelease" in workflow and ":app:testDebugUnitTest" in workflow,
        "GitHub Actions workflow is missing manual builds, unit tests, or the release APK task")
require("Oblivion-Android-ARM64-Debug" in workflow and
        "Oblivion-Android-ARM64-Release-Unsigned" in workflow,
        "GitHub Actions debug/release APK artifacts are not both published")
require("verify_apk.py" in workflow and "SHA256SUMS.txt" in workflow,
        "GitHub Actions workflow is missing APK package verification or checksums")
apk_verifier = read("scripts/verify_apk.py")
require("EXPECTED_PACKAGE = \"com.oblivion.client\"" in apk_verifier and
        "MIN_LOAD_ALIGNMENT = 16 * 1024" in apk_verifier and "EXPECTED_MACHINE_AARCH64 = 183" in apk_verifier,
        "APK verifier is missing package, ARM64, or 16 KB native alignment checks")
require("debuggable true" in app_gradle and "minifyEnabled false" in app_gradle and
        "release {" in app_gradle and "minifyEnabled true" in app_gradle,
        "Debug build must be inspectable while release build remains minified")
require("verifyDiscordPartnerSdk" in app_gradle and
        "libdiscord_partner_sdk.so" in app_gradle and
        "machine != 183" in app_gradle,
        "Native build must validate the required Discord Partner SDK ARM64 library")
native_bridge = read("app/src/main/java/com/oblivion/client/util/NativeBridgeHelper.kt")
require("Log.e(TAG" in native_bridge and "getGxCoreLoadError" in native_bridge and
        "System.loadLibrary(\"gxcore\")" in native_bridge,
        "Native gxcore load failures must preserve and log the underlying linker error")
image_guard = read("app/src/main/java/com/oblivion/client/util/NativeImageGuard.java")
require("NativeBridgeHelper.getGxCoreLoadError()" in image_guard,
        "Native image preparation errors must include the original gxcore linker detail")
sdk_installer = read("scripts/install_discord_partner_sdk.py")
require("libdiscord_partner_sdk.so" in sdk_installer and "machine != 183" in sdk_installer and
        "os.replace(temp_name, output)" in sdk_installer,
        "Local SDK installer must validate AArch64 and atomically install the native library")
upstream_notices = read("UPSTREAM_NOTICES.md")
require("separately licensed component" in upstream_notices and
        "does not redistribute this SDK binary" in upstream_notices,
        "Discord Partner SDK must be documented as a separate non-bundled licensed dependency")

# Parse all XML/JSON resources and the version catalog.
xml_count = 0
for xml_path in ROOT.rglob("*.xml"):
    if any(part in {"build", ".gradle", ".cxx"} for part in xml_path.parts):
        continue
    try:
        ET.parse(xml_path)
        xml_count += 1
    except ET.ParseError as exc:
        errors.append(f"Invalid XML {xml_path.relative_to(ROOT)}: {exc}")

json_count = 0
for json_path in ROOT.rglob("*.json"):
    if any(part in {"build", ".gradle", ".cxx"} for part in json_path.parts):
        continue
    try:
        json.loads(json_path.read_text(encoding="utf-8"))
        json_count += 1
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        errors.append(f"Invalid JSON {json_path.relative_to(ROOT)}: {exc}")

try:
    with (ROOT / "gradle/libs.versions.toml").open("rb") as fp:
        tomllib.load(fp)
    checks += 1
except (tomllib.TOMLDecodeError, OSError) as exc:
    errors.append(f"Invalid Gradle version catalog: {exc}")

# Ensure no source still declares the old first-party Java package.
legacy_hits: list[str] = []
for base in (APP / "src/main/java", APP / "src/test/java", APP / "src/androidTest/java"):
    if not base.exists():
        continue
    for source in list(base.rglob("*.java")) + list(base.rglob("*.kt")):
        text = source.read_text(encoding="utf-8", errors="replace")
        if "org.levimc.launcher" in text or "org/levimc/launcher" in text:
            legacy_hits.append(str(source.relative_to(ROOT)))
require(not legacy_hits,
        "Legacy first-party package references remain in source: " + ", ".join(legacy_hits[:8]))

# No app signing keys, personal local.properties, or Google Services config in this source archive.
for forbidden in ("local.properties", "app/google-services.json", "app/google-services.xml"):
    require(not (ROOT / forbidden).exists(), f"Local/private config must not be distributed: {forbidden}")

print(f"Oblivion source preflight: {checks} checks; parsed {xml_count} XML and {json_count} JSON files.")
if errors:
    print(f"FAILED: {len(errors)} issue(s)", file=sys.stderr)
    for issue in errors:
        print(f" - {issue}", file=sys.stderr)
    sys.exit(1)
print("PASS: project identity, in-process runtime wiring, official install/account gates, content import/export, backup exclusions, archive limits, release CI, APK validation, and structured resources.")
