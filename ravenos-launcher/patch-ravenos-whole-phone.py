#!/usr/bin/env python3
"""Fail-closed Android wiring for RavenOS whole-phone awareness."""
from __future__ import annotations

import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: patch-ravenos-whole-phone.py <iappyx-root>")

root = Path(sys.argv[1]).resolve()
manifest = root / "src/launcher/app/src/main/AndroidManifest.xml"
listener = root / "src/launcher/app/src/main/java/com/iappyx/launcher/notify/NotificationBadgeListener.kt"
home = root / "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenHomeActivity.kt"
if not manifest.is_file() or not listener.is_file() or not home.is_file():
    raise SystemExit("whole-phone donor paths missing")


def replace_once(path: Path, marker: str, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"RavenOS whole-phone already present: {path}")
        return
    if old not in text:
        raise SystemExit(f"whole-phone donor drift in {path}: missing {old[:180]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"RavenOS whole-phone patch: {path}")


replace_once(
    manifest,
    "RAVENOS WHOLE PHONE: MediaProjection permission",
    "<application",
    '''<!-- RAVENOS WHOLE PHONE: MediaProjection permission; each capture session still requires Android's owner consent UI. -->\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />\n\n    <application''',
)

replace_once(
    manifest,
    "RAVENOS WHOLE PHONE: owner-armed Goblin Eye",
    "    </application>",
    '''        <!-- RAVENOS WHOLE PHONE: owner-armed Goblin Eye. No boot start; Android consent required per session. -->\n        <activity\n            android:name=".ravenos.RavenScreenWatchActivity"\n            android:exported="false"\n            android:excludeFromRecents="true"\n            android:noHistory="true" />\n\n        <service\n            android:name=".ravenos.RavenScreenWatchService"\n            android:exported="false"\n            android:foregroundServiceType="mediaProjection" />\n\n    </application>''',
)

old_post = '''        // RAVENOS OFFICE BAR: notification-source signal. Metadata only here; no body/text routing.\n        if (sbn != null) {\n            com.iappyx.launcher.ravenos.RavenOfficeBarService.signal(\n                this, "NOTIFICATION", "package:${sbn.packageName}",\n            )\n        }\n'''
new_post = '''        // RAVENOS WHOLE PHONE: notification ranking/category/privacy sense.\n        if (sbn != null) {\n            com.iappyx.launcher.ravenos.RavenNotificationSenseOS.onPosted(\n                this,\n                sbn,\n                try { currentRanking } catch (_: Throwable) { null },\n                try { currentInterruptionFilter } catch (_: Throwable) { 0 },\n            )\n        }\n'''
replace_once(
    listener,
    "RAVENOS WHOLE PHONE: notification ranking/category/privacy sense",
    old_post,
    new_post,
)

replace_once(
    listener,
    "RAVENOS WHOLE PHONE: notification removal payoff",
    '''    override fun onNotificationRemoved(sbn: StatusBarNotification?) {\n        scheduleRecount()\n    }''',
    '''    override fun onNotificationRemoved(sbn: StatusBarNotification?) {\n        scheduleRecount()\n        // RAVENOS WHOLE PHONE: notification removal payoff.\n        if (sbn != null) com.iappyx.launcher.ravenos.RavenNotificationSenseOS.onRemoved(this, sbn)\n    }''',
)

replace_once(
    home,
    "RAVENOS WHOLE PHONE: native Home senses menu",
    '''        val items = arrayOf(\n            "Summoning Wheel",\n            "Command Palette",\n            "Apps",\n            "Sound Deck",\n            "Office Feed",\n            "Ghost Hotspots",\n            "Tasker / Automation",\n            "RavenOS Studio",\n            "Office Auto",\n            "Cycle Haunt",\n            "Sleep Office",\n        ) // RAVENOS ECOLOGY: menu entries''',
    '''        val items = arrayOf(\n            "👁 Whole-phone senses",\n            "Summoning Wheel",\n            "Command Palette",\n            "Apps",\n            "Sound Deck",\n            "Office Feed",\n            "Ghost Hotspots",\n            "Tasker / Automation",\n            "RavenOS Studio",\n            "Office Auto",\n            "Cycle Haunt",\n            "Sleep Office",\n        ) // RAVENOS ECOLOGY: menu entries\n          // RAVENOS WHOLE PHONE: native Home senses menu''',
)

replace_once(
    home,
    "RAVENOS WHOLE PHONE: native Home senses routing",
    '''                when (which) {\n                    0 -> RavenSummoningWheel.show(this)\n                    1 -> RavenCommandPalette.show(this)\n                    2 -> showAppUniverse(false)\n                    3 -> showSoundDeck()\n                    4 -> RavenOfficeFeed.show(this)\n                    5 -> RavenGhostHotspots.toggle(this)\n                    6 -> RavenTaskerBridge.showSetup(this)\n                    7 -> openStudio()\n                    8 -> RavenOfficeBarService.auto(this)\n                    9 -> RavenOfficeBarService.cycleHaunt(this)\n                    10 -> RavenOfficeBarService.disable(this)\n                } // RAVENOS ECOLOGY: menu routing''',
    '''                when (which) {\n                    0 -> RavenWholePhonePanel.show(this)\n                    1 -> RavenSummoningWheel.show(this)\n                    2 -> RavenCommandPalette.show(this)\n                    3 -> showAppUniverse(false)\n                    4 -> showSoundDeck()\n                    5 -> RavenOfficeFeed.show(this)\n                    6 -> RavenGhostHotspots.toggle(this)\n                    7 -> RavenTaskerBridge.showSetup(this)\n                    8 -> openStudio()\n                    9 -> RavenOfficeBarService.auto(this)\n                    10 -> RavenOfficeBarService.cycleHaunt(this)\n                    11 -> RavenOfficeBarService.disable(this)\n                } // RAVENOS ECOLOGY: menu routing\n                  // RAVENOS WHOLE PHONE: native Home senses routing''',
)

print("RAVENOS_WHOLE_PHONE=true")
