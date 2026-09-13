#!/usr/bin/env python3
"""Fail-closed RavenOS Goblin Brain + KnowledgeOS widget bridge patch.

Current builds delegate these bridges from RavenWidgetOfficeModule.attach so every overlay path gets
them. This patch remains as a compatibility/recovery seam for older donor layouts and becomes a
no-op when canonical delegation is already present.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
UPSTREAM = ROOT / "faeryware" / "house" / "iappyxOS-Launcher"
TARGET = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/cells/GeneratedWidgetCell.kt"
OFFICE_BRIDGE = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenWidgetOfficeBridge.kt"
MARKER = "RAVENOS WIDGET GOBLIN BRAIN BRIDGE: canonical settled reaction"
ANCHOR = "        com.iappyx.launcher.ravenos.RavenWidgetOfficeModule.attach(activity, wv, widgetId)\n"
INSERT = (
    ANCHOR
    + "        // RAVENOS WIDGET GOBLIN BRAIN BRIDGE: canonical settled reaction.\n"
    + "        com.iappyx.launcher.ravenos.RavenWidgetGoblinBrainModule.attach(activity, wv, widgetId)\n"
    + "        // RAVENOS WIDGET KNOWLEDGE BRIDGE: read-only brokered web knowledge with provenance.\n"
    + "        com.iappyx.launcher.ravenos.RavenWidgetKnowledgeModule.attach(activity, wv, widgetId)\n"
)

if OFFICE_BRIDGE.is_file():
    office = OFFICE_BRIDGE.read_text(encoding="utf-8")
    if "RavenWidgetGoblinBrainModule.attach" in office and "RavenWidgetKnowledgeModule.attach" in office:
        print("RavenOS Goblin Brain / KnowledgeOS widget bridges delegated from Office bridge")
        raise SystemExit(0)

if not TARGET.is_file():
    raise SystemExit(f"missing generated widget source: {TARGET}")
text = TARGET.read_text(encoding="utf-8")
if MARKER in text:
    print("RavenOS Goblin Brain / KnowledgeOS widget bridges already present")
    raise SystemExit(0)
if ANCHOR not in text:
    raise SystemExit("RavenOS widget bridge patch refused: Office widget bridge anchor missing")
TARGET.write_text(text.replace(ANCHOR, INSERT, 1), encoding="utf-8")
print("RavenOS Goblin Brain + KnowledgeOS widget bridges wired")
