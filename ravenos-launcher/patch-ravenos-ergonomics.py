#!/usr/bin/env python3
"""Patch RavenOS navigation ergonomics into the pinned, already-overlaid launcher chassis.

This is intentionally small and fail-closed. It does not replace iappyx paging; it adds an
explicit Raven Menu escape hatch and makes vertical gestures user-configurable shortcuts.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: patch-ravenos-ergonomics.py <iappyx-root>")

root = Path(sys.argv[1])
path = root / "src/launcher/app/src/main/java/com/iappyx/launcher/LauncherActivity.kt"
if not path.is_file():
    raise SystemExit(f"launcher activity missing: {path}")

text = path.read_text(encoding="utf-8")


def after(marker: str, anchor: str, insertion: str) -> None:
    global text
    if marker in text:
        return
    if anchor not in text:
        raise SystemExit(f"ergonomics donor drift: missing anchor {anchor!r}")
    text = text.replace(anchor, anchor + insertion, 1)


def replace_once(marker: str, old: str, new: str) -> None:
    global text
    if marker in text:
        return
    if old not in text:
        raise SystemExit(f"ergonomics donor drift: missing replacement {old!r}")
    text = text.replace(old, new, 1)


# The menu is an explicit fallback, so gestures are never the only navigation path.
after(
    "RAVENOS ERGONOMICS: persistent Raven edge menu",
    "        com.iappyx.launcher.ravenos.RavenHomeWhisper.attach(this)\n",
    "        // RAVENOS ERGONOMICS: persistent Raven edge menu. Tap = menu; long-press = gesture settings.\n"
    "        com.iappyx.launcher.ravenos.RavenMenu.attach(this)\n",
)

# Public, narrow navigation methods for RavenMenu / RavenGesturePrefs. They stay inside
# LauncherActivity so private donor overlays (drawer/search) remain encapsulated.
after(
    "RAVENOS ERGONOMICS: explicit navigation API",
    "    fun launchForWidgetHost(globalRc: Int, intent: Intent) {\n"
    "        pendingWidgetRcs.addLast(globalRc)\n"
    "        try {\n"
    "            widgetActivityLauncher.launch(intent)\n"
    "        } catch (t: Throwable) {\n"
    "            pendingWidgetRcs.removeLastOccurrence(globalRc)\n"
    "            WidgetHost.activeRequests.remove(globalRc)\n"
    "            throw t\n"
    "        }\n"
    "    }\n",
    "\n    // RAVENOS ERGONOMICS: explicit navigation API used by the edge menu and configurable gestures.\n"
    "    fun ravenOpenHome() {\n"
    "        appDrawer.visibility = View.GONE\n"
    "        searchPanel.visibility = View.GONE\n"
    "        overviewPanel.visibility = View.GONE\n"
    "        pager.setCurrentItem(1, true)\n"
    "    }\n"
    "\n"
    "    fun ravenOpenApps() { showAppDrawer() }\n"
    "    fun ravenOpenSearch() { showSearch() }\n"
    "    fun ravenOpenSystemDeck() {\n"
    "        appDrawer.visibility = View.GONE\n"
    "        searchPanel.visibility = View.GONE\n"
    "        overviewPanel.visibility = View.GONE\n"
    "        pager.setCurrentItem(0, true)\n"
    "    }\n",
)

# Fast-fling path: delegate to user gesture preferences.
replace_once(
    "RAVENOS ERGONOMICS: configurable fling up",
    "                    showAppDrawer(); return true\n",
    "                    // RAVENOS ERGONOMICS: configurable fling up.\n"
    "                    return com.iappyx.launcher.ravenos.RavenGesturePrefs.fire(\n"
    "                        this@LauncherActivity, com.iappyx.launcher.ravenos.RavenGestureDirection.UP,\n"
    "                    )\n",
)
replace_once(
    "RAVENOS ERGONOMICS: configurable fling down",
    "                    snapPagerBackToGestureStart()\n                    showSearch(); return true\n",
    "                    snapPagerBackToGestureStart()\n"
    "                    // RAVENOS ERGONOMICS: configurable fling down.\n"
    "                    return com.iappyx.launcher.ravenos.RavenGesturePrefs.fire(\n"
    "                        this@LauncherActivity, com.iappyx.launcher.ravenos.RavenGestureDirection.DOWN,\n"
    "                    )\n",
)

# Slow-drag path: same routing, so fling and displacement gestures cannot disagree.
replace_once(
    "RAVENOS ERGONOMICS: configurable drag up",
    "        if (dy < 0) {\n            showAppDrawer()\n        } else {\n",
    "        if (dy < 0) {\n"
    "            // RAVENOS ERGONOMICS: configurable drag up.\n"
    "            com.iappyx.launcher.ravenos.RavenGesturePrefs.fire(\n"
    "                this, com.iappyx.launcher.ravenos.RavenGestureDirection.UP,\n"
    "            )\n"
    "        } else {\n",
)
replace_once(
    "RAVENOS ERGONOMICS: configurable drag down",
    "            if (gestureDownY > statusBarGuard) showSearch()\n",
    "            if (gestureDownY > statusBarGuard) {\n"
    "                // RAVENOS ERGONOMICS: configurable drag down.\n"
    "                com.iappyx.launcher.ravenos.RavenGesturePrefs.fire(\n"
    "                    this, com.iappyx.launcher.ravenos.RavenGestureDirection.DOWN,\n"
    "                )\n"
    "            }\n",
)

path.write_text(text, encoding="utf-8")
print("RavenOS ergonomics patch applied: edge menu + configurable vertical gestures")

# Keep the native Home ecology in the same deterministic build path as ergonomics so CI and
# local builds cannot accidentally produce different RavenOS launchers.
ec = Path(__file__).resolve().with_name("patch-ravenos-ecology.py")
subprocess.check_call(["python3", str(ec), str(root)])

# Whole-phone awareness is another fail-closed layer over the same reviewed donor.
whole = Path(__file__).resolve().with_name("patch-ravenos-whole-phone.py")
subprocess.check_call(["python3", str(whole), str(root)])
