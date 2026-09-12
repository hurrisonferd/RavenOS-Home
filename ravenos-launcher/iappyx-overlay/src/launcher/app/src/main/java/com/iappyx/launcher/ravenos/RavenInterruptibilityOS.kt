package com.iappyx.launcher.ravenos

import android.content.Context

/** Decides whether Goblin Vision earns spoken presentation or quietly updates evidence. */
object RavenInterruptibilityOS {
    private const val PREFS = "ravenos_interruptibility_v3"

    fun allow(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        haunt: RavenHauntMode,
        quiet: Boolean,
    ): Boolean {
        if (quiet) return marker.salience >= 9

        val screen = RavenScreenContextOS.snapshot(context, marker.at)
        val screenReaderArmed = RavenGoblinReadOS.isEnabled(context) || RavenAccessibilityReadOS.isEnabled(context)
        val boundary = "BOUNDARY" in marker.tags
        val error = "ERROR" in marker.tags
        val payoff = "PAYOFF" in complex.tags
        val critical = boundary || error || payoff || marker.salience >= 9
        val screenSignal = marker.key in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC", "SCREEN_VISUAL")
        val actionNoise = marker.key in setOf(
            "APP_ENTER", "WINDOW_CHANGE", "HOME_ENTER", "MEDIA_SESSION", "MEDIA_ACTIVE", "MEDIA_IDLE",
            "NOTIFICATION_POSTED", "NOTIFICATION_REMOVED",
        )

        // Phone actions update the evidence graph, but when screen reading is armed we wait for the
        // resulting visible screen context instead of narrating every tap/app/window transition.
        if (actionNoise && screenReaderArmed && !critical) return false
        if (actionNoise && !screen.available && !critical) return false
        if (screenSignal && marker.key != "SCREEN_VISUAL" && !screen.available && !critical) return false

        val minimum = when (haunt) {
            RavenHauntMode.CALM -> 7
            RavenHauntMode.LIVED_IN -> 6
            RavenHauntMode.HAUNTED -> 4
            RavenHauntMode.FERAL -> 3
            RavenHauntMode.APOCALYPSE -> 2
        }
        if (!critical && marker.salience < minimum && "RUNNING_BIT" !in complex.tags) return false

        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = marker.at
        val last = prefs.getLong("last_spoken", 0L)
        val minGap = when (haunt) {
            RavenHauntMode.CALM -> 75_000L
            RavenHauntMode.LIVED_IN -> 45_000L
            RavenHauntMode.HAUNTED -> 32_000L
            RavenHauntMode.FERAL -> 22_000L
            RavenHauntMode.APOCALYPSE -> 14_000L
        }
        val criticalGap = 5_000L
        val requiredGap = if (critical) criticalGap else minGap
        if (now - last < requiredGap) return false

        // Screen meaning, not package motion, is the principal novelty key. A changed app with the
        // same visible subject should not produce a fresh speech just because Android emitted edges.
        val novelty = noveltySignature(marker, screen)
        val lastNovelty = prefs.getString("last_novelty", "").orEmpty()
        val lastNoveltyAt = prefs.getLong("last_novelty_at", 0L)
        val noveltyWindow = when (haunt) {
            RavenHauntMode.CALM -> 300_000L
            RavenHauntMode.LIVED_IN -> 180_000L
            RavenHauntMode.HAUNTED -> 120_000L
            RavenHauntMode.FERAL -> 90_000L
            RavenHauntMode.APOCALYPSE -> 60_000L
        }
        val milestone = complex.occurrence in setOf(3, 8, 21, 55)
        if (!critical && !milestone && novelty.isNotBlank() && novelty == lastNovelty && now - lastNoveltyAt < noveltyWindow) return false

        // Visual-only motion is evidence, not necessarily commentary. Require a large change if we
        // still cannot read the screen's actual subject.
        if (marker.key == "SCREEN_VISUAL" && !screen.available && marker.salience < 6 && !critical) return false

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

    private fun noveltySignature(marker: RavenMarkerBus.Marker, screen: RavenScreenContextOS.Snapshot): String {
        if (screen.available && screen.signature.isNotBlank()) return "SCREEN|${screen.signature}"
        val coarse = marker.detail
            .replace(Regex("position:[^|]*", RegexOption.IGNORE_CASE), "position")
            .replace(Regex("duration:[^|]*", RegexOption.IGNORE_CASE), "duration")
            .substringBefore('|')
            .take(72)
        return "${marker.key}|$coarse"
    }
}
