#!/usr/bin/env python3
"""Fail-closed RavenOS KnowledgeOS widget bridge patch."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parent.parent
UPSTREAM = ROOT / "faeryware" / "house" / "iappyxOS-Launcher"
TARGET = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/cells/GeneratedWidgetCell.kt"
MARKER = "RAVENOS WIDGET KNOWLEDGE BRIDGE: read-only brokered web knowledge"
ANCHOR = "        com.iappyx.launcher.ravenos.RavenWidgetOfficeModule.attach(activity, wv, widgetId)\n"
INSERT = (
    ANCHOR
    + "        // RAVENOS WIDGET KNOWLEDGE BRIDGE: read-only brokered web knowledge with provenance.\n"
    + "        com.iappyx.launcher.ravenos.RavenWidgetKnowledgeModule.attach(activity, wv, widgetId)\n"
)

if not TARGET.is_file():
    raise SystemExit(f"missing generated widget source: {TARGET}")
text = TARGET.read_text(encoding="utf-8")
if MARKER in text:
    print("RavenOS KnowledgeOS widget bridge already present")
    raise SystemExit(0)
if ANCHOR not in text:
    raise SystemExit("RavenOS KnowledgeOS refused: Office widget bridge anchor missing")
TARGET.write_text(text.replace(ANCHOR, INSERT, 1), encoding="utf-8")
print("RavenOS KnowledgeOS widget bridge wired")
