#!/usr/bin/env python3
"""Patch pinned iappyx donor seams used by RavenOS reactive surfaces.

The donor already has the expensive plumbing: a separate :wallpaper process,
VirtualDisplay/Presentation/WebView, visibility pausing, render-process recovery,
same-package broadcasts, and bundled widget/wallpaper catalogues. RavenOS adds only
narrow, fail-closed hooks on exact reviewed anchors.
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
    "RAVENOS OFFICE WALLPAPER: acknowledgement selection on change",
    "                    activeId = newId\n",
    '''                    // RAVENOS OFFICE WALLPAPER: acknowledgement selection on change.\n                    // Only the bundled Office wallpaper receives the callback interface.\n                    presentation?.setRavenOfficeAckEnabled(newId == "ravenos_office")\n''',
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
    "RAVENOS OFFICE WALLPAPER: acknowledgement selection on build",
    "                    it.show()\n",
    '''                    // RAVENOS OFFICE WALLPAPER: acknowledgement selection on build.\n                    it.setRavenOfficeAckEnabled(activeId == "ravenos_office")\n''',
)

after(
    "RAVENOS OFFICE WALLPAPER: presentation state",
    "        private var widgetHost: WidgetHost? = null\n",
    '''        // RAVENOS OFFICE WALLPAPER: presentation state.\n        private var ravenOfficeJson: String? = null\n''',
)

after(
    "RAVENOS OFFICE WALLPAPER: acknowledgement bridge field",
    "        private var widgetHost: WidgetHost? = null\n",
    '''        // RAVENOS OFFICE WALLPAPER: acknowledgement bridge field.\n        // It exists only while the bundled ravenos_office payload is active.\n        private var ravenOfficeAckBridge:\n            com.iappyx.launcher.ravenos.RavenWallpaperOfficeAckBridge? = null\n''',
)

after(
    "RAVENOS OFFICE WALLPAPER: replay after page load",
    "                webViewClient = object : WebViewClient() {\n",
    '''                    // RAVENOS OFFICE WALLPAPER: replay after page load.\n                    override fun onPageFinished(view: WebView, url: String?) {\n                        super.onPageFinished(view, url)\n                        ravenOfficeJson?.let { dispatchRavenOffice(it) }\n                    }\n\n''',
)

before(
    "RAVENOS OFFICE WALLPAPER: acknowledgement capability API",
    "        fun updateLayout(json: String) {\n",
    '''        // RAVENOS OFFICE WALLPAPER: acknowledgement capability API.\n        // Arbitrary/user-generated wallpapers never receive this interface.\n        fun setRavenOfficeAckEnabled(enabled: Boolean) {\n            if (!::web.isInitialized) return\n            if (enabled) {\n                if (ravenOfficeAckBridge == null) {\n                    val ack = com.iappyx.launcher.ravenos.RavenWallpaperOfficeAckBridge(\n                        context.applicationContext,\n                    )\n                    ravenOfficeAckBridge = ack\n                    web.addJavascriptInterface(ack, "ravenOfficeAck")\n                }\n            } else if (ravenOfficeAckBridge != null) {\n                web.removeJavascriptInterface("ravenOfficeAck")\n                ravenOfficeAckBridge = null\n            }\n        }\n\n''',
)

before(
    "RAVENOS OFFICE WALLPAPER: one-way state API",
    "        fun updateLayout(json: String) {\n",
    '''        // RAVENOS OFFICE WALLPAPER: one-way state API.\n        fun seedRavenOffice(json: String) {\n            ravenOfficeJson = json\n        }\n\n        fun updateRavenOffice(json: String) {\n            ravenOfficeJson = json\n            if (::web.isInitialized) dispatchRavenOffice(json)\n        }\n\n        private fun dispatchRavenOffice(json: String) {\n            if (!::web.isInitialized) return\n            val quoted = org.json.JSONObject.quote(json)\n            try {\n                web.evaluateJavascript(\n                    "(()=>{const s=JSON.parse($quoted);window.ravenOfficeState=s;" +\n                        "window.dispatchEvent(new CustomEvent('ravenofficechange',{detail:s}));})()",\n                    null,\n                )\n            } catch (_: Throwable) {}\n        }\n\n''',
)

path.write_text(text, encoding="utf-8")
print("RavenOS wallpaper Office-state + acknowledgement patch applied")

# Register the RavenOS Office proof widget in the donor's existing bundled-widget library.
widget_library = root / "src/launcher/app/src/main/java/com/iappyx/launcher/widget/WidgetLibrary.kt"
if not widget_library.is_file():
    raise SystemExit(f"widget library missing: {widget_library}")
widget_text = widget_library.read_text(encoding="utf-8")
widget_marker = "RAVENOS BUNDLED OFFICE WIDGET"
widget_anchor = "    private val BUNDLED = listOf(\n"
if widget_marker not in widget_text:
    if widget_anchor not in widget_text:
        raise SystemExit(f"widget donor drift: missing anchor {widget_anchor!r}")
    insertion = '''        // RAVENOS BUNDLED OFFICE WIDGET: canonical Office-state proof surface.\n        BundledMeta(\n            "ravenos_office", "RavenOS Office",\n            "Live resident card for the canonical RavenOS Office member, note, lane, signal, and haunt mode.",\n            "widgets/ravenos_office.html",\n        ),\n'''
    widget_text = widget_text.replace(widget_anchor, widget_anchor + insertion, 1)
    widget_library.write_text(widget_text, encoding="utf-8")
    print("RavenOS bundled Office widget registered")

# Register the Office-reactive live wallpaper without changing iappyx's existing default.
wallpaper_library = root / "src/launcher/app/src/main/java/com/iappyx/launcher/wallpaper/WallpaperLibrary.kt"
if not wallpaper_library.is_file():
    raise SystemExit(f"wallpaper library missing: {wallpaper_library}")
wallpaper_text = wallpaper_library.read_text(encoding="utf-8")
wallpaper_marker = "RAVENOS BUNDLED OFFICE WALLPAPER"
wallpaper_anchor = "    private val BUNDLED = listOf(\n"
if wallpaper_marker not in wallpaper_text:
    if wallpaper_anchor not in wallpaper_text:
        raise SystemExit(f"wallpaper donor drift: missing anchor {wallpaper_anchor!r}")
    insertion = '''        // RAVENOS BUNDLED OFFICE WALLPAPER: one canonical Office state, rendered live.\n        BundledMeta(\n            "ravenos_office", "RavenOS Office Field",\n            "Reactive neon office field driven by the canonical resident, accent, author's note, signal, and haunt level.",\n        ),\n'''
    wallpaper_text = wallpaper_text.replace(wallpaper_anchor, wallpaper_anchor + insertion, 1)
    wallpaper_library.write_text(wallpaper_text, encoding="utf-8")
    print("RavenOS bundled Office wallpaper registered")
