#!/usr/bin/env python3
# Copyright (c) 2026 Tanim Boss. See LICENSE.
"""Verify the packaged confirmed-actions contract, not just the source manifest."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile


def verify(apk: Path) -> None:
    project = Path(__file__).resolve().parent.parent
    analyzer = shutil.which("apkanalyzer")
    if not analyzer:
        sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if not sdk:
            raise RuntimeError("Set ANDROID_HOME or put apkanalyzer on PATH.")
        analyzer = str(Path(sdk) / "cmdline-tools/latest/bin/apkanalyzer")
    manifest = ET.fromstring(subprocess.check_output([analyzer, "manifest", "print", str(apk)], text=True))
    android = "{http://schemas.android.com/apk/res/android}"
    if manifest.get("package") != "com.tanimboss.jerryai":
        raise RuntimeError("Unexpected application ID.")
    permissions = {item.get(android + "name") for item in manifest.findall("uses-permission")}
    allowed = {"com.tanimboss.jerryai.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}
    if permissions - allowed:
        raise RuntimeError(f"Unexpected app permissions: {sorted(permissions - allowed)}")
    sdk = manifest.find("uses-sdk")
    if sdk is None or sdk.get(android + "minSdkVersion") != "29" or sdk.get(android + "targetSdkVersion") != "36":
        raise RuntimeError("Unexpected Android compatibility settings.")
    app = manifest.find("application")
    if app is None or app.get(android + "allowBackup") != "false" or app.get(android + "usesCleartextTraffic") != "false":
        raise RuntimeError("Backup or cleartext policy missing.")
    services = app.findall("service")
    if len(services) != 1:
        raise RuntimeError("Expected only the opt-in accessibility service.")
    service = services[0]
    if service.get(android + "name") != "com.tanimboss.jerryai.automation.JerryAccessibilityService" or service.get(android + "permission") != "android.permission.BIND_ACCESSIBILITY_SERVICE":
        raise RuntimeError("Accessibility service is missing its system binding permission.")
    actions = {item.get(android + "name") for item in service.findall("intent-filter/action")}
    if actions != {"android.accessibilityservice.AccessibilityService"}:
        raise RuntimeError("Unexpected exported service action.")
    metadata = {item.get(android + "name") for item in service.findall("meta-data")}
    if metadata != {"android.accessibilityservice"}:
        raise RuntimeError("Accessibility metadata is missing or unexpected.")
    for file in ("res/xml/jerry_accessibility_service.xml", "res/xml-v31/jerry_accessibility_service.xml"):
        config = ET.fromstring(subprocess.check_output([analyzer, "resources", "xml", "--file", file, str(apk)], text=True))
        if config.get(android + "canRetrieveWindowContent") != "true" or config.get(android + "canPerformGestures") != "true":
            raise RuntimeError("Expected accessibility capabilities are missing.")
        forbidden = ("canTakeScreenshot", "canRequestFilterKeyEvents", "canRequestTouchExplorationMode", "canRequestEnhancedWebAccessibility", "canControlMagnification")
        if any(config.get(android + name) == "true" for name in forbidden):
            raise RuntimeError("Unexpected accessibility capability.")
        if config.get(android + "accessibilityFlags") != "0x10":
            raise RuntimeError("Unexpected accessibility flags.")
        if "v31" in file and config.get(android + "isAccessibilityTool") != "false":
            raise RuntimeError("Automation must not be misclassified as a disability accessibility tool.")
    expected_assets = {
        "assets/LICENSE": project / "LICENSE",
        "assets/THIRD_PARTY_NOTICES.md": project / "THIRD_PARTY_NOTICES.md",
    }
    expected_assets.update({f"assets/licenses/{p.name}": p for p in (project / "licenses").glob("*.txt")})
    with zipfile.ZipFile(apk) as archive:
        for asset, original in expected_assets.items():
            if archive.read(asset) != original.read_bytes():
                raise RuntimeError(f"Packaged notice differs from source: {asset}")
    print("PASS: packaged ID/API levels, permission allowlist, protected service and bounded capabilities, backup/cleartext flags, and license assets.")
    print(f"APK SHA-256: {hashlib.sha256(apk.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    try:
        verify(Path(sys.argv[1] if len(sys.argv) > 1 else "app/build/outputs/apk/debug/app-debug.apk").resolve())
    except (OSError, RuntimeError, subprocess.CalledProcessError, KeyError, ET.ParseError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        sys.exit(1)
