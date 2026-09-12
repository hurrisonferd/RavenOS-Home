package com.iappyx.launcher.ravenos

import android.content.Context

/** Decides whether Goblin Vision earns character speech while preserving continuous screen observation. */
object RavenInterruptibilityOS {
    private const val PREFS = "ravenos_interruptibility_v4"

    data class Decision(
        val speak: Boolean,
        val score: Int,
        val threshold: Int,
        val reason: String,
    )

    fun allow(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        haunt: RavenHauntMode,
        quiet: Boolean,
    ): Boolean = evaluate(
        context, marker, complex, haunt, quiet,
        RavenScreenContextOS.snapshot(context, marker.at),
    ).speak

    fun evaluate(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        haunt: RavenHauntMode,
        quiet: Boolean,
        screen: RavenScreenContextOS.Snapshot,
    ): Decision {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = marker.at
        val boundary = "BOUNDARY" in marker.tags
        val error = "ERROR" in marker.tags
        val payoff = "PAYOFF" in complex.tags
        val critical = boundary || error || payoff || marker.salience >= 9
        if (quiet && !critical) return Decision(false, 0, 99, "QUIET")

        val screenReaderArmed = RavenGoblinReadOS.isEnabled(context) || RavenAccessibilityReadOS.isEnabled(context)
        val actionNoise = marker.key in setOf(
            "APP_ENTER", "WINDOW_CHANGE", "HOME_ENTER", "MEDIA_SESSION", "MEDIA_ACTIVE", "MEDIA_IDLE",
            "NOTIFICATION_POSTED", "NOTIFICATION_REMOVED", "FOREGROUND_APP", "FOREGROUND_USAGE", "FOREGROUND_WINDOW",
        )
        val screenSignal = marker.key in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC", "SCREEN_VISUAL")

        var score = marker.salience * 8
        if (screen.available) score += 24
        if (screen.confidence >= 85) score += 12
        if (screen.meta) score += 28
        if (screen.keyboardLike) score += 3
        if (screen.semanticKind == "CHATGPT") score += 8
        if (screenSignal) score += 10
        if ("RUNNING_BIT" in complex.tags) score += 8
        if ("RETURN_LOOP" in complex.tags) score += 8
        if (payoff) score += 24
        if (boundary) score += 28
        if (error) score += 30
        if (actionNoise) score -= if (screen.available) 18 else 40
        if (actionNoise && screenReaderArmed) score -= 14
        if (!screen.available && screenSignal && marker.key != "SCREEN_VISUAL") score -= 24

        val threshold = when (haunt) {
            RavenHauntMode.CALM -> 92
            RavenHauntMode.LIVED_IN -> 80
            RavenHauntMode.HAUNTED -> 68
            RavenHauntMode.FERAL -> 58
            RavenHauntMode.APOCALYPSE -> 48
        }

        val last = prefs.getLong("last_spoken", 0L)
        val minGap = when (haunt) {
            RavenHauntMode.CALM -> 58_000L
            RavenHauntMode.LIVED_IN -> 38_000L
            RavenHauntMode.HAUNTED -> 26_000L
            RavenHauntMode.FERAL -> 17_000L
            RavenHauntMode.APOCALYPSE -> 10_000L
        }
        val criticalGap = 4_500L
        val firstUsefulGap = when (haunt) {
            RavenHauntMode.CALM -> 24_000L
            RavenHauntMode.LIVED_IN -> 18_000L
            RavenHauntMode.HAUNTED -> 14_000L
            RavenHauntMode.FERAL -> 10_000L
            RavenHauntMode.APOCALYPSE -> 7_000L
        }
        val age = if (last <= 0L) Long.MAX_VALUE else now - last

        val novelty = noveltySignature(marker, screen)
        val lastNovelty = prefs.getString("last_novelty", "").orEmpty()
        val lastNoveltyAt = prefs.getLong("last_novelty_at", 0L)
        val noveltyWindow = when (haunt) {
            RavenHauntMode.CALM -> 240_000L
            RavenHauntMode.LIVED_IN -> 150_000L
            RavenHauntMode.HAUNTED -> 90_000L
            RavenHauntMode.FERAL -> 60_000L
            RavenHauntMode.APOCALYPSE -> 40_000L
        }
        val sameSubject = novelty.isNotBlank() && novelty == lastNovelty && now - lastNoveltyAt < noveltyWindow
        if (sameSubject && !critical) score -= 34

        val milestone = complex.occurrence in setOf(3, 8, 21, 55)
        if (milestone) score += 10

        // First-useful-line guarantee: once the screen is genuinely readable and the office has not
        // spoken for a while, one grounded comment is allowed even if the triggering callback itself
        // was boring. This prevents a permanently silent resident widget.
        val usefulScreen = screen.available && screen.confidence >= 72 && !sameSubject
        val firstUseful = usefulScreen && age >= firstUsefulGap && score >= threshold - 18
        val gapSatisfied = age >= if (critical) criticalGap else minGap
        val scoreSatisfied = score >= threshold
        val speak = when {
            critical && age >= criticalGap -> true
            firstUseful -> true
            gapSatisfied && scoreSatisfied -> true
            else -> false
        }

        val reason = when {
            speak && critical -> "CRITICAL"
            speak && firstUseful -> "FIRST_USEFUL_SCREEN"
            speak -> "SCORED_SCREEN_DIALOGUE"
            sameSubject -> "SAME_SCREEN_SUBJECT"
            actionNoise -> "ACTION_EVIDENCE_ONLY"
            !screen.available && screenReaderArmed -> "WAITING_FOR_SCREEN_MEANING"
            age < minGap -> "CADENCE_FLOOR"
            else -> "BELOW_DIALOGUE_THRESHOLD"
        }

        if (speak) {
            prefs.edit()
                .putLong("last_spoken", now)
                .putString("last_novelty", novelty)
                .putLong("last_novelty_at", now)
                .apply()
        }
        return Decision(speak, score, threshold, reason)
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
