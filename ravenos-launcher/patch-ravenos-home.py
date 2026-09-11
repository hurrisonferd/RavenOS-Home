#!/usr/bin/env python3
"""Promote RavenOS native HOME, demote donor workspace to Studio, and keep HOME hot-path lean."""
from __future__ import annotations

import sys
from pathlib import Path


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        print(f"RavenOS Home already present: {path}")
        return
    if old not in text:
        raise SystemExit(f"RavenOS Home anchor missing in {path}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"RavenOS Home patch: {path}")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: patch-ravenos-home.py <iappyx-root>")
    root = Path(sys.argv[1]).resolve()
    manifest = root / "src/launcher/app/src/main/AndroidManifest.xml"
    launcher = root / "src/launcher/app/src/main/java/com/iappyx/launcher/LauncherActivity.kt"
    app = root / "src/launcher/app/src/main/java/com/iappyx/launcher/IappyxApp.kt"
    office = root / "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenOfficeBarService.kt"
    follow = root / "src/launcher/app/src/main/java/com/iappyx/launcher/ravenos/RavenFollowMeOverlay.kt"

    old_activity = '''        <activity\n            android:name=".LauncherActivity"\n            android:exported="true"\n            android:launchMode="singleTask"\n            android:stateNotNeeded="true"\n            android:resizeableActivity="true"\n            android:configChanges="keyboard|keyboardHidden|mcc|mnc|navigation|orientation|screenSize|smallestScreenSize|screenLayout|uiMode">\n\n            <!-- Standard launcher entry -->\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN" />\n                <category android:name="android.intent.category.LAUNCHER" />\n            </intent-filter>\n\n            <!-- Register as a HOME launcher so it shows up in the default-launcher picker -->\n            <intent-filter android:priority="100">\n                <action android:name="android.intent.action.MAIN" />\n                <category android:name="android.intent.category.HOME" />\n                <category android:name="android.intent.category.DEFAULT" />\n            </intent-filter>\n        </activity>'''

    new_activity = '''        <!-- RAVENOS NATIVE HOME: utility-first front door. The donor workspace is Studio. -->\n        <activity\n            android:name=".ravenos.RavenHomeActivity"\n            android:exported="true"\n            android:launchMode="singleTask"\n            android:stateNotNeeded="true"\n            android:resizeableActivity="true"\n            android:configChanges="keyboard|keyboardHidden|mcc|mnc|navigation|orientation|screenSize|smallestScreenSize|screenLayout|uiMode">\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN" />\n                <category android:name="android.intent.category.LAUNCHER" />\n            </intent-filter>\n            <intent-filter android:priority="100">\n                <action android:name="android.intent.action.MAIN" />\n                <category android:name="android.intent.category.HOME" />\n                <category android:name="android.intent.category.DEFAULT" />\n            </intent-filter>\n        </activity>\n\n        <!-- RAVENOS STUDIO: mature iappyx workspace retained as the advanced workshop. -->\n        <activity\n            android:name=".LauncherActivity"\n            android:exported="false"\n            android:launchMode="singleTask"\n            android:stateNotNeeded="true"\n            android:resizeableActivity="true"\n            android:configChanges="keyboard|keyboardHidden|mcc|mnc|navigation|orientation|screenSize|smallestScreenSize|screenLayout|uiMode" />'''
    replace_once(manifest, old_activity, new_activity)

    replace_once(
        launcher,
        '''        pager.setCurrentItem(1, false)\n''',
        '''        pager.setCurrentItem(1, false)\n        // RAVENOS STUDIO: explicit workshop launches land on the command/System page.\n        if (intent.getBooleanExtra("RAVEN_OPEN_STUDIO", false)) pager.setCurrentItem(0, false)\n''',
    )

    # The donor theme walker is useful for Studio but wasteful on RavenOS-owned HOME. Raven Home
    # owns its own palette and view tree, so skip the traversal entirely on that one Activity.
    replace_once(
        app,
        '''            override fun onActivityResumed(activity: Activity) {\n                val decor = activity.window?.decorView ?: return\n''',
        '''            override fun onActivityResumed(activity: Activity) {\n                // RAVENOS NATIVE HOME FAST PATH: no donor theme-tree traversal on Raven Home.\n                if (activity is com.iappyx.launcher.ravenos.RavenHomeActivity) return\n                val decor = activity.window?.decorView ?: return\n''',
    )

    for path in (office, follow):
        text = path.read_text(encoding="utf-8")
        text = text.replace("import com.iappyx.launcher.LauncherActivity\n", "")
        text = text.replace("Intent(this, LauncherActivity::class.java)", "Intent(this, RavenHomeActivity::class.java)")
        text = text.replace("Intent(context, LauncherActivity::class.java)", "Intent(context, RavenHomeActivity::class.java)")
        path.write_text(text, encoding="utf-8")
        print(f"RavenOS Home routing: {path}")

    print("RAVENOS_NATIVE_HOME=true")
    print("RAVENOS_NATIVE_HOME_FAST_PATH=true")


if __name__ == "__main__":
    main()
