package com.iappyx.launcher.ravenos

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import org.json.JSONObject

/**
 * One-method proof bridge for the bundled RavenOS Office wallpaper only.
 *
 * The :wallpaper process may acknowledge an exact canonical updatedAt after its JavaScript has
 * consumed/rendered that state. This bridge has no routing, permission, storage, or resident
 * mutation authority.
 */
@Keep
class RavenWallpaperOfficeAckBridge(context: Context) {
    private val app = context.applicationContext

    @JavascriptInterface
    fun acknowledge(updatedAt: String) {
        val at = updatedAt.toLongOrNull() ?: return
        if (at <= 0L) return
        val canonicalAt = try {
            RavenOfficeStateStore.readSnapshotJson(app)
                ?.let { JSONObject(it).optLong("updatedAt", 0L) }
                ?: 0L
        } catch (_: Throwable) {
            0L
        }
        if (canonicalAt <= 0L || at > canonicalAt) return
        RavenSurfaceIntegrity.markCrossProcess(
            app,
            RavenSurfaceIntegrity.WALLPAPER_CHANNEL,
            "CONSUMED",
            at,
            "wallpaper_js",
        )
    }

    @JavascriptInterface
    fun version(): Int = 1
}
