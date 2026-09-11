package com.iappyx.launcher.ravenos

import android.content.Context

/** Decides whether Goblin Vision earns spoken/expanded presentation or stays quiet. */
object RavenInterruptibilityOS {
    private const val PREFS = "ravenos_interruptibility_v2"

    fun allow(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        haunt: RavenHauntMode,
        quiet: Boolean,
    ): Boolean {
        if (quiet) return marker.salience >= 8
        if ("BOUNDARY" in marker.tags || "ERROR" in marker.tags || "PAYOFF" in complex.tags) return true
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
        if (now - last < minGap && marker.salience < 8) return false

        // A fast launcher should react to change, not repeat the same sentence on a timer. Repeated
        // semantic events stay silent until a recurrence milestone upgrades the running bit.
        val novelty = noveltySignature(marker)
        val lastNovelty = prefs.getString("last_novelty", "").orEmpty()
        val lastNoveltyAt = prefs.getLong("last_novelty_at", 0L)
        val noveltyWindow = when (haunt) {
            RavenHauntMode.CALM -> 180_000L
            RavenHauntMode.LIVED_IN -> 90_000L
            RavenHauntMode.HAUNTED -> 45_000L
            RavenHauntMode.FERAL -> 20_000L
            RavenHauntMode.APOCALYPSE -> 6_000L
        }
        val milestone = complex.occurrence in setOf(2, 3, 5, 8, 13, 21, 34)
        if (!milestone && novelty == lastNovelty && now - lastNoveltyAt < noveltyWindow) return false

        prefs.edit()
            .putLong("last_spoken", now)
            .putString("last_novelty", novelty)
            .putLong("last_novelty_at", now)
            .apply()
        return true
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun noveltySignature(marker: RavenMarkerBus.Marker): String {
        val pkg = Regex("(?:^|\\|)package:([^|\\s]+)").find(marker.detail)?.groupValues?.getOrNull(1).orEmpty()
        val app = Regex("(?:^|\\|)app:([^|]+)").find(marker.detail)?.groupValues?.getOrNull(1).orEmpty()
        val coarse = when {
            pkg.isNotBlank() -> pkg
            app.isNotBlank() -> app
            marker.key == "AUDIO" -> marker.detail
                .replace(Regex("media:\\d+"), "media")
                .replace(Regex("ring:\\d+"), "ring")
                .replace(Regex("alarm:\\d+"), "alarm")
            else -> marker.detail.substringBefore('|').take(64)
        }
        return "${marker.key}|$coarse"
    }
}
