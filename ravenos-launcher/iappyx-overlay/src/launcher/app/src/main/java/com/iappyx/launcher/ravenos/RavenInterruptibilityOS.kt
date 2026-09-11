package com.iappyx.launcher.ravenos

import android.content.Context

/** Decides whether Goblin Vision earns spoken/expanded presentation or stays quiet. */
object RavenInterruptibilityOS {
    private const val PREFS = "ravenos_interruptibility_v1"

    fun allow(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        haunt: RavenHauntMode,
        quiet: Boolean,
    ): Boolean {
        if (quiet) return marker.salience >= 8
        if ("BOUNDARY" in marker.tags || "ERROR" in marker.tags) return true
        val minimum = when (haunt) {
            RavenHauntMode.CALM -> 7
            RavenHauntMode.LIVED_IN -> 5
            RavenHauntMode.HAUNTED -> 3
            RavenHauntMode.FERAL -> 2
            RavenHauntMode.APOCALYPSE -> 0
        }
        if (marker.salience < minimum && "RUNNING_BIT" !in complex.tags) return false

        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = marker.at
        val last = prefs.getLong("last_spoken", 0L)
        val minGap = when (haunt) {
            RavenHauntMode.CALM -> 30_000L
            RavenHauntMode.LIVED_IN -> 18_000L
            RavenHauntMode.HAUNTED -> 9_000L
            RavenHauntMode.FERAL -> 4_000L
            RavenHauntMode.APOCALYPSE -> 1_500L
        }
        if (now - last < minGap && marker.salience < 8 && "PAYOFF" !in complex.tags) return false
        prefs.edit().putLong("last_spoken", now).apply()
        return true
    }
}
