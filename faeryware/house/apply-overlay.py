#!/usr/bin/env python3
"""Apply the minimal Faeryware HOUSE overlay to the pinned iappyx checkout.

Idempotent and fail-closed: refuses an unexpected upstream SHA or missing patch anchor.
"""
from __future__ import annotations

import shutil
import subprocess
from pathlib import Path

HOUSE = Path(__file__).resolve().parent
UPSTREAM = HOUSE / "iappyxOS-Launcher"
OVERLAY = HOUSE / "iappyx-overlay"
EXPECTED_SHA = "3d0fb7517847283a3d1a173ac2d5bd54720a8af2"


def run(*args: str) -> str:
    return subprocess.check_output(args, text=True).strip()


def copy(rel: str) -> None:
    src = OVERLAY / rel
    dst = UPSTREAM / rel
    if not src.is_file():
        raise SystemExit(f"missing overlay file: {src}")
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    print(f"overlay copy: {rel}")


def patch_before(path: Path, marker: str, anchor: str, insertion: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"overlay already present: {path.relative_to(UPSTREAM)}")
        return
    if anchor not in text:
        raise SystemExit(f"upstream drift: anchor missing in {path}: {anchor!r}")
    path.write_text(text.replace(anchor, insertion + anchor, 1), encoding="utf-8")
    print(f"overlay patch: {path.relative_to(UPSTREAM)}")


def patch_after(path: Path, marker: str, anchor: str, insertion: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"overlay already present: {path.relative_to(UPSTREAM)}")
        return
    if anchor not in text:
        raise SystemExit(f"upstream drift: anchor missing in {path}: {anchor!r}")
    path.write_text(text.replace(anchor, anchor + insertion, 1), encoding="utf-8")
    print(f"overlay patch: {path.relative_to(UPSTREAM)}")


def main() -> None:
    if not (UPSTREAM / "src").exists():
        raise SystemExit("iappyx submodule is not initialized; run git submodule update --init first")
    actual = run("git", "-C", str(UPSTREAM), "rev-parse", "HEAD")
    if actual != EXPECTED_SHA:
        raise SystemExit(f"refusing unreviewed upstream: expected {EXPECTED_SHA}, found {actual}")

    bridge_rel = "src/launcher/app/src/main/java/com/iappyx/launcher/faeryware/FaerywareResidentBridge.java"
    widget_rel = "src/launcher/app/src/main/assets/widgets/faeryware_resident.html"
    copy(bridge_rel)
    copy(widget_rel)

    widget_host = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/WidgetHost.java"
    host_marker = "// FAERYWARE HOUSE: resident-only bridge"
    host_anchor = "        // PLUGINS: BEGIN\n"
    host_insert = '''        // FAERYWARE HOUSE: resident-only bridge\n        // Deliberately unavailable to every other generated widget.\n        if ("faeryware_resident".equals(_widgetId)) {\n            webView.addJavascriptInterface(\n                new com.iappyx.launcher.faeryware.FaerywareResidentBridge(getApplicationContext()),\n                "faerywareResident"\n            );\n        }\n'''
    patch_before(widget_host, host_marker, host_anchor, host_insert)

    library = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/widget/WidgetLibrary.kt"
    library_marker = '"faeryware_resident", "Faeryware Resident"'
    library_anchor = "    private val BUNDLED = listOf(\n"
    library_insert = '''        BundledMeta(\n            "faeryware_resident", "Faeryware Resident",\n            "Owner-gated Digi Fae resident state from the Faeryware colony. No model required.",\n            "widgets/faeryware_resident.html",\n        ),\n'''
    patch_after(library, library_marker, library_anchor, library_insert)

    manifest = UPSTREAM / "src/launcher/app/src/main/AndroidManifest.xml"
    manifest_marker = 'android:authorities="com.faeryware.launcher.resident"'
    manifest_anchor = "    <application\n"
    manifest_insert = '''    <!-- FAERYWARE HOUSE: narrow package visibility for the resident provider. -->\n    <queries>\n        <provider android:authorities="com.faeryware.launcher.resident" />\n    </queries>\n\n'''
    patch_before(manifest, manifest_marker, manifest_anchor, manifest_insert)

    print(f"Faeryware HOUSE overlay applied to pinned iappyx {actual}")


if __name__ == "__main__":
    main()
