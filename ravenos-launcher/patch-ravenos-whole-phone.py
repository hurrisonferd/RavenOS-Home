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
if not manifest.is_file() or not listener.is_file():
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

print("RAVENOS_WHOLE_PHONE=true")
