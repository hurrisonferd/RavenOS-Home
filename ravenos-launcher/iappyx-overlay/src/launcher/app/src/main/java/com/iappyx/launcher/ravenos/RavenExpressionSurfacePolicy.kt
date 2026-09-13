package com.iappyx.launcher.ravenos

/** Surface-specific expression density caps. */
object RavenExpressionSurfacePolicy {
    data class SurfacePolicy(val maxGlyphClusters: Int, val allowEnsemble: Boolean, val allowTail: Boolean, val preferredFamily: String?)

    fun forSurface(surface: String): SurfacePolicy = when (surface.uppercase()) {
        "DOT" -> SurfacePolicy(0, false, false, null)
        "PEEK" -> SurfacePolicy(1, false, false, "PEEK")
        "CHIP" -> SurfacePolicy(1, false, false, null)
        "WIDGET_COMPACT" -> SurfacePolicy(1, false, false, null)
        "WIDGET" -> SurfacePolicy(2, false, true, null)
        "DESK" -> SurfacePolicy(3, true, true, null)
        "BOARD_MEETING" -> SurfacePolicy(4, true, true, "BOARD_MEETING")
        "DIAGNOSTIC" -> SurfacePolicy(0, false, false, null)
        else -> SurfacePolicy(2, false, true, null)
    }
}
