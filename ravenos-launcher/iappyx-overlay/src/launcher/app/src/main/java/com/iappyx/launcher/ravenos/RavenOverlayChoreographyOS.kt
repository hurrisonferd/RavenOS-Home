package com.iappyx.launcher.ravenos

import android.content.Context

/** Picks a low-conflict vertical lane for the resident overlay from current local screen evidence. */
object RavenOverlayChoreographyOS {
    data class Placement(val y: Int, val reason: String)

    fun preferred(context: Context): Placement {
        val metrics = context.resources.displayMetrics
        val height = metrics.heightPixels.coerceAtLeast(dp(context, 600))
        val access = RavenAccessibilityReadOS.latest(context)
        val map = RavenScreenMapOS.latest(context)
        val ocr = RavenGoblinReadOS.latest(context)

        if (access?.keyboardLike == true) {
            return Placement(dp(context, 76), "keyboard-open:upper-lane")
        }

        val quiet = map?.quietZone(false) ?: ocr?.leastBusyZone() ?: "top"
        val y = when (quiet) {
            "middle" -> (height * 0.30f).toInt()
            "bottom" -> (height * 0.56f).toInt().coerceAtMost(height - dp(context, 360))
            else -> dp(context, 86)
        }.coerceAtLeast(dp(context, 56))
        val reason = if (map != null) {
            "screen-map:$quiet:t${map.occupancy("top")}-m${map.occupancy("middle")}-b${map.occupancy("bottom")}"
        } else "ocr-quiet-zone:$quiet"
        return Placement(y, reason)
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
