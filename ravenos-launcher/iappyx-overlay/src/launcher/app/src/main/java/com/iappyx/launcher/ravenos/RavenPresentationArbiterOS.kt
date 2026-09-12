package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Settles what the resident overlay is allowed to present.
 *
 * SCREEN: trustworthy owner-authorized screen meaning exists; screen-aware sitcom may speak.
 * PHONE: no readable screen, but a materially novel phone event can still earn legacy Meta Goblin voice.
 * CHIP: no comment-worthy truth; keep resident presence without replacing personality with setup text.
 * DIAGNOSTIC: explicit owner-requested sensor diagnosis only.
 *
 * Recurrence alone never earns speech here. Counts belong to structural memory, not punchline quota.
 * Sensing remains presentation-only. This organ grants no Android effect authority.
 */
object RavenPresentationArbiterOS {
    enum class Mode { SCREEN, PHONE, CHIP, DIAGNOSTIC }

    data class Decision(
        val mode: Mode,
        val speak: Boolean,
        val showObservation: Boolean,
        val reason: String,
    )

    private const val PREFS = "ravenos_presentation_arbiter_v1"
    private const val KEY_LAST_PHONE_SPOKEN = "last_phone_spoken"
    private const val KEY_LAST_PHONE_SIGNATURE = "last_phone_signature"

    fun decide(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        screen: RavenScreenContextOS.Snapshot,
        screenSpeech: RavenInterruptibilityOS.Decision,
        direction: RavenSitcomDirectorOS.Direction,
        haunt: RavenHauntMode,
        quiet: Boolean,
    ): Decision {
        if (quiet) return Decision(Mode.CHIP, false, false, "QUIET_CHIP")

        if (marker.key == "SCREEN_DIAGNOSTIC") {
            return Decision(Mode.DIAGNOSTIC, false, true, "EXPLICIT_DIAGNOSTIC")
        }

        val notificationNoise = marker.key.startsWith("NOTIFICATION")

        if (screen.available && screen.confidence >= 60) {
            // A tray ping may enrich the scene, but it does not interrupt a readable screen on its
            // own. Visible meaning wins; notification churn stays evidence-only unless the actual
            // readable scene independently earned speech.
            val speak = if (notificationNoise) screenSpeech.speak else screenSpeech.speak || direction.shouldSpeak
            return Decision(
                mode = Mode.SCREEN,
                speak = speak,
                showObservation = true,
                reason = when {
                    speak && screenSpeech.speak -> screenSpeech.reason
                    speak -> "SITCOM_${direction.beat}"
                    notificationNoise -> "SCREEN_NOTIFICATION_EVIDENCE_ONLY"
                    else -> "SCREEN_EVIDENCE_ONLY"
                },
            )
        }

        val critical = "BOUNDARY" in marker.tags || "ERROR" in marker.tags ||
            "PAYOFF" in complex.tags || marker.salience >= 9
        if (!meaningfulPhoneEvent(marker) && !critical) {
            return Decision(Mode.CHIP, false, false, if (notificationNoise) "NOTIFICATION_EVIDENCE_ONLY" else "NO_READABLE_SCENE")
        }

        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = marker.at
        val lastAt = prefs.getLong(KEY_LAST_PHONE_SPOKEN, 0L)
        val age = if (lastAt <= 0L) Long.MAX_VALUE else (now - lastAt).coerceAtLeast(0L)
        val signature = phoneSignature(marker)
        val lastSignature = prefs.getString(KEY_LAST_PHONE_SIGNATURE, "").orEmpty()
        val novel = signature.isNotBlank() && signature != lastSignature
        val gap = if (critical) 4_500L else phoneGapMs(haunt)
        val major = marker.key in setOf(
            "APP_ENTER", "HOME_ENTER", "MEDIA_ACTIVE", "MEDIA_IDLE", "MEDIA_SESSION",
            "SYSTEM_DECK_OPENED", "SEARCH_OPENED", "APP_UNIVERSE_OPENED", "ROOM_CHANGED",
            "POWER_CHANGED", "BATTERY_CHANGED",
        )
        val deterministicGate = when {
            critical || major -> true
            marker.key == "SCREEN_VISUAL" -> stableIndex("${marker.id}|vision-phone", 3) == 0
            marker.key == "WINDOW_CHANGE" -> stableIndex("$signature|window-phone", 4) == 0
            else -> stableIndex("$signature|phone", 3) == 0
        }
        // Crucial law: occurrence/milestone counts never make this true. A phone-only interruption
        // must be materially novel or critical, then still survive cadence and deterministic gate.
        val speak = age >= gap && deterministicGate && (novel || critical)

        if (speak) {
            prefs.edit()
                .putLong(KEY_LAST_PHONE_SPOKEN, now)
                .putString(KEY_LAST_PHONE_SIGNATURE, signature)
                .apply()
        }

        return Decision(
            mode = Mode.PHONE,
            speak = speak,
            showObservation = false,
            reason = when {
                speak && critical -> "PHONE_CRITICAL"
                speak && novel -> "PHONE_NOVELTY"
                age < gap -> "PHONE_CADENCE"
                !novel -> "PHONE_REPEAT_SILENT"
                else -> "PHONE_EVIDENCE_ONLY"
            },
        )
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun meaningfulPhoneEvent(marker: RavenMarkerBus.Marker): Boolean = marker.key in setOf(
        "APP_ENTER", "WINDOW_CHANGE", "HOME_ENTER", "ROOM_CHANGED", "SEARCH_OPENED",
        "APP_UNIVERSE_OPENED", "SYSTEM_DECK_OPENED", "MEDIA_ACTIVE", "MEDIA_IDLE", "MEDIA_SESSION",
        "POWER_CHANGED", "BATTERY_CHANGED", "SCREEN_VISUAL", "AUDIO", "DEVICE", "NIGHT",
    )

    private fun phoneGapMs(haunt: RavenHauntMode): Long = when (haunt) {
        RavenHauntMode.CALM -> 42_000L
        RavenHauntMode.LIVED_IN -> 26_000L
        RavenHauntMode.HAUNTED -> 15_000L
        RavenHauntMode.FERAL -> 9_000L
        RavenHauntMode.APOCALYPSE -> 5_500L
    }

    private fun phoneSignature(marker: RavenMarkerBus.Marker): String {
        val d = marker.detail
            .replace(Regex("(?:position|duration|motion|delta|hash|luma|captured_at|at):[^|]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\|+"), "|")
            .trim('|', ' ')
            .take(180)
        return "${marker.key}|$d"
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
