#!/usr/bin/env python3
"""Apply RavenOS Launcher identity/runtime on top of the reviewed Faeryware HOUSE chassis.

Layering:
    pinned iappyx MIT chassis
        -> Faeryware HOUSE overlay
        -> RavenOS Launcher overlay

Fail-closed: exact upstream anchors are required; unexpected drift stops the build.
"""
from __future__ import annotations

import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAVEN = ROOT / "ravenos-launcher"
HOUSE = ROOT / "faeryware" / "house"
UPSTREAM = HOUSE / "iappyxOS-Launcher"
OVERLAY = RAVEN / "iappyx-overlay"
EXPECTED_SHA = "3d0fb7517847283a3d1a173ac2d5bd54720a8af2"


def run(*args: str) -> str:
    return subprocess.check_output(args, text=True).strip()


def copy(rel: str) -> None:
    src = OVERLAY / rel
    dst = UPSTREAM / rel
    if not src.is_file():
        raise SystemExit(f"missing RavenOS overlay file: {src}")
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    print(f"RavenOS copy: {rel}")


def patch_after(path: Path, marker: str, anchor: str, insertion: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"RavenOS already present: {path.relative_to(UPSTREAM)}")
        return
    if anchor not in text:
        raise SystemExit(f"upstream drift: anchor missing in {path}: {anchor!r}")
    path.write_text(text.replace(anchor, anchor + insertion, 1), encoding="utf-8")
    print(f"RavenOS patch: {path.relative_to(UPSTREAM)}")


def patch_before(path: Path, marker: str, anchor: str, insertion: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"RavenOS already present: {path.relative_to(UPSTREAM)}")
        return
    if anchor not in text:
        raise SystemExit(f"upstream drift: anchor missing in {path}: {anchor!r}")
    path.write_text(text.replace(anchor, insertion + anchor, 1), encoding="utf-8")
    print(f"RavenOS patch: {path.relative_to(UPSTREAM)}")


def replace_once(path: Path, marker: str, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"RavenOS already present: {path.relative_to(UPSTREAM)}")
        return
    if old not in text:
        raise SystemExit(f"upstream drift: replacement anchor missing in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"RavenOS replace: {path.relative_to(UPSTREAM)}")


def rebrand_strings() -> None:
    res = UPSTREAM / "src/launcher/app/src/main/res"
    touched = 0
    for path in sorted(res.glob("values*/strings.xml")):
        text = path.read_text(encoding="utf-8")
        new = re.sub(
            r'(<string\s+name="app_name"[^>]*>).*?(</string>)',
            r'\1RavenOS Launcher\2',
            text,
            count=1,
            flags=re.S,
        )
        new = re.sub(
            r'(<string\s+name="wallpaper_label"[^>]*>).*?(</string>)',
            r'\1RavenOS Live\2',
            new,
            count=1,
            flags=re.S,
        )
        if new != text:
            path.write_text(new, encoding="utf-8")
            touched += 1
    if touched == 0:
        raise SystemExit("RavenOS rebrand failed: no app_name strings changed")
    print(f"RavenOS rebrand: {touched} locale string files")


def main() -> None:
    if not (UPSTREAM / "src").exists():
        raise SystemExit("pinned iappyx chassis is not initialized")
    actual = run("git", "-C", str(UPSTREAM), "rev-parse", "HEAD")
    if actual != EXPECTED_SHA:
        raise SystemExit(f"refusing unreviewed chassis: expected {EXPECTED_SHA}, found {actual}")

    # Ensure the previously-reviewed resident/possession layer lands first.
    subprocess.check_call(["python3", str(HOUSE / "apply-overlay.py")])

    files = [
        "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenOfficeMember.kt",
        "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenOfficeBarService.kt",
        "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenForegroundAwarenessService.kt",
        "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenSystemDeck.kt",
        "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenSystemDeckPane.kt",
        "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenSurfaceModel.kt",
        "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenIntelligenceProvider.kt",
        "src/launcher/app/src/main/res/xml/ravenos_foreground_awareness.xml",
    ]
    for rel in files:
        copy(rel)

    rebrand_strings()

    manifest = UPSTREAM / "src/launcher/app/src/main/AndroidManifest.xml"
    patch_before(
        manifest,
        "RAVENOS OFFICE BAR: persistent visible presence",
        "        <!-- Push (FCM) messaging service.",
        '''        <!-- RAVENOS OFFICE BAR: persistent visible presence; no hidden scraping. -->\n        <service\n            android:name=".ravenos.RavenOfficeBarService"\n            android:exported="false"\n            android:foregroundServiceType="specialUse">\n            <property\n                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"\n                android:value="Persistent user-visible RavenOS office-member status and deterministic author notes" />\n        </service>\n\n        <!-- RAVENOS FOREGROUND AWARENESS: opt-in package-only Accessibility lane. -->\n        <service\n            android:name=".ravenos.RavenForegroundAwarenessService"\n            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"\n            android:exported="true"\n            android:label="RavenOS Foreground Awareness">\n            <intent-filter>\n                <action android:name="android.accessibilityservice.AccessibilityService" />\n            </intent-filter>\n            <meta-data\n                android:name="android.accessibilityservice"\n                android:resource="@xml/ravenos_foreground_awareness" />\n        </service>\n\n''',
    )
    text = manifest.read_text(encoding="utf-8")
    text = text.replace('android:label="iappyxOS notification badges"', 'android:label="RavenOS notification awareness"')
    manifest.write_text(text, encoding="utf-8")

    launcher = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/LauncherActivity.kt"
    patch_after(
        launcher,
        "RAVENOS OFFICE BAR: initial home signal",
        "        setContentView(R.layout.activity_launcher)\n",
        '''        // RAVENOS OFFICE BAR: initial home signal\n        com.iappyx.launcher.ravenos.RavenOfficeBarService.signal(this, "HOME", "launcher:home")\n''',
    )
    patch_after(
        launcher,
        "RAVENOS OFFICE BAR: page context",
        "            override fun onPageSelected(position: Int) {\n",
        '''                // RAVENOS OFFICE BAR: page context\n                com.iappyx.launcher.ravenos.RavenOfficeBarService.signal(this@LauncherActivity, "ROOM", "page:$position")\n''',
    )
    patch_after(
        launcher,
        "RAVENOS OFFICE BAR: app universe",
        "    private fun showAppDrawer() {\n",
        '''        // RAVENOS OFFICE BAR: app universe\n        com.iappyx.launcher.ravenos.RavenOfficeBarService.signal(this, "APP_UNIVERSE", "drawer")\n''',
    )
    patch_after(
        launcher,
        "RAVENOS OFFICE BAR: search",
        "    private fun showSearch() {\n",
        '''        // RAVENOS OFFICE BAR: search\n        com.iappyx.launcher.ravenos.RavenOfficeBarService.signal(this, "SEARCH", "universal-search")\n''',
    )

    app_lock = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/applock/AppLockManager.kt"
    patch_before(
        app_lock,
        "RAVENOS OFFICE BAR: app launch signal",
        "        if (!isLocked(activity, packageName)) {\n",
        '''        // RAVENOS OFFICE BAR: app launch signal\n        com.iappyx.launcher.ravenos.RavenOfficeBarService.signal(activity, "APP_LAUNCH", "package:$packageName")\n''',
    )

    notification_listener = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/notify/NotificationBadgeListener.kt"
    patch_after(
        notification_listener,
        "RAVENOS OFFICE BAR: notification-source signal",
        "    override fun onNotificationPosted(sbn: StatusBarNotification?) {\n        scheduleRecount()\n",
        '''        // RAVENOS OFFICE BAR: notification-source signal. Metadata only here; no body/text routing.\n        if (sbn != null) {\n            com.iappyx.launcher.ravenos.RavenOfficeBarService.signal(\n                this, "NOTIFICATION", "package:${sbn.packageName}",\n            )\n        }\n''',
    )

    command = UPSTREAM / "src/launcher/app/src/main/java/com/iappyx/launcher/widget/CommandPanelHost.kt"
    patch_after(
        command,
        "private val systemDeckPane: com.iappyx.launcher.ravenos.RavenSystemDeckPane",
        "    private val iconsPane: ManageIconFiltersTab\n",
        "    private val systemDeckPane: com.iappyx.launcher.ravenos.RavenSystemDeckPane\n",
    )
    patch_after(
        command,
        'newTab().setText("SYSTEM")',
        "            addTab(newTab().setText(activity.getString(com.iappyx.launcher.R.string.tab_icons)))\n",
        '            addTab(newTab().setText("SYSTEM"))\n',
    )
    patch_after(
        command,
        "systemDeckPane = com.iappyx.launcher.ravenos.RavenSystemDeckPane(activity)",
        "        iconsPane = ManageIconFiltersTab(activity, host = this)\n",
        "        systemDeckPane = com.iappyx.launcher.ravenos.RavenSystemDeckPane(activity)\n",
    )
    patch_after(
        command,
        "contentFrame.addView(systemDeckPane, frameMatch())",
        "        contentFrame.addView(iconsPane, frameMatch())\n",
        "        contentFrame.addView(systemDeckPane, frameMatch())\n",
    )
    patch_after(
        command,
        "systemDeckPane.visibility = if (index == 5)",
        "        iconsPane.visibility = if (index == 4) View.VISIBLE else View.GONE\n",
        "        systemDeckPane.visibility = if (index == 5) View.VISIBLE else View.GONE\n",
    )
    patch_after(
        command,
        "5 -> systemDeckPane.refresh()",
        "            4 -> iconsPane.refresh()\n",
        "            5 -> systemDeckPane.refresh()\n",
    )
    patch_before(
        command,
        "RAVENOS SYSTEM DECK: default command-page tab",
        "    }\n\n    private fun frameMatch()",
        '''        // RAVENOS SYSTEM DECK: default command-page tab\n        tabLayout.getTabAt(5)?.select()\n''',
    )
    replace_once(
        command,
        "RAVENOS SYSTEM DECK: return default",
        '''    fun resetToAi() {\n        if (tabLayout.selectedTabPosition != 0) tabLayout.getTabAt(0)?.select()\n    }''',
        '''    fun resetToAi() {\n        // RAVENOS SYSTEM DECK: return default. Name retained for upstream API compatibility.\n        if (tabLayout.selectedTabPosition != 5) tabLayout.getTabAt(5)?.select()\n    }''',
    )

    print(f"RavenOS Launcher overlay applied over pinned iappyx {actual}")


if __name__ == "__main__":
    main()
