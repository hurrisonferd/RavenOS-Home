package com.iappyx.launcher.ravenos

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.Keep
import com.iappyx.launcher.WidgetHost
import org.json.JSONObject

/** Read-only brokered online knowledge capability for RavenOS widgets. */
object RavenWidgetKnowledgeModule {
    fun attach(context: Context, webView: WebView, widgetId: String) {
        if (!authorized(widgetId)) return
        webView.addJavascriptInterface(Bridge(context.applicationContext), "ravenKnowledge")
    }

    fun broadcast(context: Context, snapshot: RavenKnowledgeStateStore.Snapshot) {
        val payload = safeJson(snapshot).toString()
        val quoted = JSONObject.quote(payload)
        for ((widgetId, host) in WidgetHost.hostsByWidgetId.entries) {
            if (!authorized(widgetId)) continue
            host.evaluateJavaScript(
                "window.dispatchEvent(new CustomEvent('ravenknowledgechange',{detail:JSON.parse($quoted)}));",
            )
        }
    }

    private fun authorized(widgetId: String): Boolean =
        widgetId == "faeryware_resident" || widgetId.startsWith("ravenos_", ignoreCase = true)

    private fun safeJson(snapshot: RavenKnowledgeStateStore.Snapshot): JSONObject = JSONObject()
        .put("ok", true)
        .put("topic", snapshot.topic)
        .put("summary", snapshot.summary)
        .put("provider", snapshot.provider)
        .put("source", snapshot.source)
        .put("url", snapshot.url)
        .put("scope", snapshot.scope)
        .put("fetchedAt", snapshot.fetchedAt)
        .put("fresh", snapshot.fresh())

    @Keep
    private class Bridge(private val context: Context) {
        @JavascriptInterface
        fun state(): String {
            val snapshot = RavenKnowledgeStateStore.read(context)
                ?: return JSONObject().put("ok", false).put("error", "knowledge unavailable").toString()
            return safeJson(snapshot).toString()
        }

        @JavascriptInterface
        fun version(): Int = 1
    }
}
