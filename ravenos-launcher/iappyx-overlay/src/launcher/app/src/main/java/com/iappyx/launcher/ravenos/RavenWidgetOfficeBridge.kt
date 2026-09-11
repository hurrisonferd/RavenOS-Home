package com.iappyx.launcher.ravenos

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.Keep
import com.iappyx.launcher.WidgetHost
import org.json.JSONObject

/**
 * Read-only RavenOS Office capability for generated widgets.
 *
 * Default deny. The bridge is attached only to RavenOS-owned widget ids or ids Raven has
 * explicitly granted. It exposes presentation state only: no notification body, page text,
 * clipboard, private memory, keys, credentials, or write authority.
 */
object RavenWidgetOfficeModule {
    private const val PREFS = "ravenos_widget_capabilities_v1"
    private const val CAP_OFFICE = "office_state"

    fun attach(context: Context, webView: WebView, widgetId: String) {
        if (!hasOfficeCapability(context, widgetId)) return
        webView.addJavascriptInterface(OfficeBridge(context.applicationContext), "ravenOffice")
    }

    fun grantOffice(context: Context, widgetId: String) {
        if (widgetId.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key(widgetId, CAP_OFFICE), true).apply()
    }

    fun revokeOffice(context: Context, widgetId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(key(widgetId, CAP_OFFICE)).apply()
    }

    fun hasOfficeCapability(context: Context, widgetId: String): Boolean {
        if (widgetId == "faeryware_resident") return true
        if (widgetId.startsWith("ravenos_", ignoreCase = true)) return true
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(widgetId, CAP_OFFICE), false)
    }

    /** Push the already-settled canonical snapshot to live allowed generated widgets. */
    fun broadcast(context: Context, snapshot: RavenOfficeStateStore.Snapshot) {
        val payload = safeJson(snapshot).toString()
        val quoted = JSONObject.quote(payload)
        for ((widgetId, host) in WidgetHost.hostsByWidgetId.entries) {
            if (!hasOfficeCapability(context, widgetId)) continue
            host.evaluateJavaScript(
                "window.dispatchEvent(new CustomEvent('ravenofficechange',{detail:JSON.parse($quoted)}));",
            )
        }
    }

    private fun key(widgetId: String, capability: String): String =
        "${widgetId.trim()}::$capability"

    private fun safeJson(snapshot: RavenOfficeStateStore.Snapshot): JSONObject = JSONObject()
        .put("ok", true)
        .put("owner", snapshot.owner)
        .put("emoji", snapshot.emoji)
        .put("accent", String.format("#%08X", snapshot.accent))
        .put("lane", snapshot.lane)
        .put("signal", snapshot.signal)
        .put("note", snapshot.note)
        .put("haunt", snapshot.haunt)
        .put("manual", snapshot.manual)
        .put("quiet", snapshot.quiet)
        .put("updatedAt", snapshot.updatedAt)

    @Keep
    private class OfficeBridge(private val context: Context) {
        @JavascriptInterface
        fun state(): String {
            val snapshot = RavenOfficeStateStore.read(context)
                ?: return JSONObject().put("ok", false).put("error", "office state unavailable").toString()
            return safeJson(snapshot).toString()
        }

        @JavascriptInterface
        fun version(): Int = 1
    }
}
