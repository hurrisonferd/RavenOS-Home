#!/usr/bin/env python3
"""Patch the pinned iappyx live-wallpaper engine with RavenOS Office state events.

The donor already has the right expensive plumbing: separate :wallpaper process,
VirtualDisplay/Presentation/WebView, visibility pausing, render-process recovery, and
same-package broadcasts for cross-process state. RavenOS adds only a safe one-way
Office presentation-state channel. Exact anchors fail closed on donor drift.
"""
from __future__ import annotations

import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: patch-ravenos-wallpaper.py <iappyx-root>")

root = Path(sys.argv[1])
path = root / "src/launcher/app/src/main/java/com/iappyx/launcher/wallpaper/IappyxWallpaperService.kt"
if not path.is_file():
    raise SystemExit(f"wallpaper service missing: {path}")

text = path.read_text(encoding="utf-8")


def after(marker: str, anchor: str, insertion: str) -> None:
    global text
    if marker in text:
        return
    if anchor not in text:
        raise SystemExit(f"wallpaper donor drift: missing anchor {anchor!r}")
    text = text.replace(anchor, anchor + insertion, 1)


def before(marker: str, anchor: str, insertion: str) -> None:
    global text
    if marker in text:
        return
    if anchor not in text:
        raise SystemExit(f"wallpaper donor drift: missing anchor {anchor!r}")
    text = text.replace(anchor, insertion + anchor, 1)


after(
    "RAVENOS OFFICE WALLPAPER: cross-process state fields",
    "        private var layoutChangedReceiver: BroadcastReceiver? = null\n",
    '''        // RAVENOS OFFICE WALLPAPER: cross-process state fields.\n        private var ravenOfficeChangedReceiver: BroadcastReceiver? = null\n        private var pendingRavenOfficeJson: String? = null\n''',
)

after(
    "RAVENOS OFFICE WALLPAPER: state receiver",
    '            Log.d(TAG, "engine onCreate: wallpaper-change + layout-change receivers registered")\n',
    '''\n            // RAVENOS OFFICE WALLPAPER: state receiver. SharedPreferences are not\n            // trusted cross-process; seed from the atomic JSON mirror, then consume\n            // explicit same-package broadcasts from RavenOfficeStateStore.\n            pendingRavenOfficeJson =\n                com.iappyx.launcher.ravenos.RavenOfficeStateStore.readSnapshotJson(this@IappyxWallpaperService)\n            val ravenOfficeRx = object : BroadcastReceiver() {\n                override fun onReceive(ctx: Context, intent: Intent) {\n                    val json = intent.getStringExtra(\n                        com.iappyx.launcher.ravenos.RavenOfficeStateStore.EXTRA_JSON,\n                    ) ?: return\n                    pendingRavenOfficeJson = json\n                    presentation?.updateRavenOffice(json)\n                }\n            }\n            ravenOfficeChangedReceiver = ravenOfficeRx\n            val ravenOfficeFilter = IntentFilter(\n                com.iappyx.launcher.ravenos.RavenOfficeStateStore.ACTION_CHANGED,\n            )\n            if (Build.VERSION.SDK_INT >= 33) {\n                registerReceiver(ravenOfficeRx, ravenOfficeFilter, Context.RECEIVER_NOT_EXPORTED)\n            } else {\n                @Suppress("UnspecifiedRegisterReceiverFlag")\n                registerReceiver(ravenOfficeRx, ravenOfficeFilter)\n            }\n''',
)

after(
    "RAVENOS OFFICE WALLPAPER: unregister state receiver",
    "            layoutChangedReceiver = null\n",
    '''            // RAVENOS OFFICE WALLPAPER: unregister state receiver.\n            ravenOfficeChangedReceiver?.let {\n                try { unregisterReceiver(it) } catch (_: Throwable) {}\n            }\n            ravenOfficeChangedReceiver = null\n''',
)

before(
    "RAVENOS OFFICE WALLPAPER: seed presentation before load",
    "                    it.loadUrl(WallpaperLibrary.urlFor(this@IappyxWallpaperService, activeId))\n",
    '''                    // RAVENOS OFFICE WALLPAPER: seed presentation before load.\n                    pendingRavenOfficeJson?.let { json -> it.seedRavenOffice(json) }\n''',
)

after(
    "RAVENOS OFFICE WALLPAPER: presentation state",
    "        private var widgetHost: WidgetHost? = null\n",
    '''        // RAVENOS OFFICE WALLPAPER: presentation state. One-way JS event only;\n        // wallpaper HTML receives no new native-call authority.\n        private var ravenOfficeJson: String? = null\n''',
)

# Replay the most recent state after each HTML load. Inline wallpaper JS can register
# a ravenofficechange listener during page execution; onPageFinished then delivers state.
after(
    "RAVENOS OFFICE WALLPAPER: replay after page load",
    "                webViewClient = object : WebViewClient() {\n",
    '''                    // RAVENOS OFFICE WALLPAPER: replay after page load.\n                    override fun onPageFinished(view: WebView, url: String?) {\n                        super.onPageFinished(view, url)\n                        ravenOfficeJson?.let { dispatchRavenOffice(it) }\n                    }\n\n''',
)

before(
    "RAVENOS OFFICE WALLPAPER: one-way state API",
    "        fun updateLayout(json: String) {\n",
    '''        // RAVENOS OFFICE WALLPAPER: one-way state API.\n        fun seedRavenOffice(json: String) {\n            ravenOfficeJson = json\n        }\n\n        fun updateRavenOffice(json: String) {\n            ravenOfficeJson = json\n            if (::web.isInitialized) dispatchRavenOffice(json)\n        }\n\n        private fun dispatchRavenOffice(json: String) {\n            if (!::web.isInitialized) return\n            val quoted = org.json.JSONObject.quote(json)\n            try {\n                web.evaluateJavascript(\n                    "(()=>{const s=JSON.parse($quoted);window.ravenOfficeState=s;" +\n                        "window.dispatchEvent(new CustomEvent('ravenofficechange',{detail:s}));})()",\n                    null,\n                )\n            } catch (_: Throwable) {}\n        }\n\n''',
)

path.write_text(text, encoding="utf-8")
print("RavenOS wallpaper Office-state patch applied")
