package com.iappyx.launcher.ravenos

import android.content.Context

/** Lightweight deterministic scene pacing for Android sessions. */
object RavenEpisodeOS {
    enum class Phase { COLD_OPEN, SETUP, RISING_ACTION, CHAOS_PEAK, PAYOFF, RECOVERY, TAG }

    fun phase(context: Context, marker: RavenMarkerBus.Marker, complex: RavenComplexEventOS.Result): Phase {
        if ("PAYOFF" in complex.tags || "SUCCESS" in marker.tags) return Phase.PAYOFF
        if ("RECOVERY_ARC" in complex.tags || "RECOVERY" in marker.tags) return Phase.RECOVERY
        if ("APP_SWITCH_BURST" in complex.tags) return Phase.CHAOS_PEAK
        val now = marker.at
        val count = RavenMarkerBus.recent(context, 48).count { now - it.at <= 10 * 60_000L }
        return when {
            count <= 2 -> Phase.COLD_OPEN
            count <= 5 -> Phase.SETUP
            count <= 12 -> Phase.RISING_ACTION
            count <= 24 -> Phase.CHAOS_PEAK
            else -> Phase.TAG
        }
    }
}
