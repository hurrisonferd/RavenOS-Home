package com.iappyx.launcher.ravenos

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.Keep
import com.iappyx.launcher.WidgetHost
import org.json.JSONObject

/** Read-only canonical Goblin Brain reaction feed for authorized Raven widgets. */
object RavenWidgetGoblinBrainModule {
    fun attach(context: Context, webView: WebView, widgetId: String) {
        if (!authorized(widgetId)) return
        webView.addJavascriptInterface(Bridge(context.applicationContext), "ravenGoblinBrain")
    }

    fun broadcast(context: Context, packet: RavenReactionPacket) {
        val payload = packet.toJson().toString()
        val quoted = JSONObject.quote(payload)
        for ((widgetId, host) in WidgetHost.hostsByWidgetId.entries) {
            if (!authorized(widgetId)) continue
            host.evaluateJavaScript(
                "window.dispatchEvent(new CustomEvent('ravengoblinchange',{detail:JSON.parse($quoted)}));",
            )
        }
    }

    private fun authorized(widgetId: String): Boolean =
        widgetId == "faeryware_resident" || widgetId.startsWith("ravenos_", ignoreCase = true)

    @Keep
    private class Bridge(private val context: Context) {
        @JavascriptInterface
        fun state(): String = RavenReactionStateStore.readJson(context)
            ?: JSONObject().put("ok", false).put("error", "Goblin Brain state unavailable").toString()

        @JavascriptInterface
        fun version(): Int = 1
    }
}
