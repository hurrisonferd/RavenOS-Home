package com.iappyx.launcher.ravenos

import android.content.Context
import android.os.SystemClock

/**
 * Deterministic anti-flap governor for reactive Office signals.
 *
 * The Office should feel alive, not like a notification slot machine. This governor runs before
 * RavenOfficeBarService is started, so rejected noise creates no FGS churn, no trace receipt, and
 * no cross-surface re-render. It changes cadence only; it never grants or revokes capability.
 */
object RavenOfficeGovernor {
    private const val PREFS = "ravenos_office_governor_v1"
    private const val KEY_SIGNAL = "last_signal"
    private const val KEY_DETAIL = "last_detail"
    private const val KEY_AT = "last_elapsed_ms"
    private const val KEY_PRIORITY = "last_priority"
    private const val KEY_ACCEPTED = "accepted_count"
    private const val KEY_SUPPRESSED = "suppressed_count"
    private const val KEY_LAST_SUPPRESSION = "last_suppression"

    fun accept(context: Context, signal: String, detail: String, mode: RavenHauntMode): Boolean {
        val normalized = signal.trim().uppercase()
        val now = SystemClock.elapsedRealtime()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previousSignal = prefs.getString(KEY_SIGNAL, "").orEmpty()
        val previousDetail = prefs.getString(KEY_DETAIL, "").orEmpty()
        val previousAt = prefs.getLong(KEY_AT, -1L)
        val previousPriority = prefs.getInt(KEY_PRIORITY, Int.MIN_VALUE)
        val priority = priority(normalized, detail)

        val age = if (previousAt < 0L || now < previousAt) Long.MAX_VALUE else now - previousAt
        val dupWindow = duplicateWindow(mode)

        if (normalized == previousSignal && detail == previousDetail && age < dupWindow) {
            recordSuppressed(context, "duplicate:$normalized:${age}ms")
            return false
        }

        // Android commonly reports one real app transition twice: launcher APP_LAUNCH followed by
        // Accessibility FOREGROUND_APP. Treat equal-detail edges as one semantic event so Goblin
        // Vision comments on the transition instead of narrating callback plumbing.
        val appEdges = setOf("APP_LAUNCH", "FOREGROUND_APP")
        if (normalized in appEdges && previousSignal in appEdges && detail == previousDetail && age < dupWindow * 2L) {
            recordSuppressed(context, "semantic-app-edge:$previousSignal->$normalized:${age}ms")
            return false
        }

        val hold = holdWindow(mode, previousSignal)
        if (age < hold && priority <= previousPriority) {
            recordSuppressed(context, "hold:$normalized:p$priority<=p$previousPriority:${age}ms<$hold")
            return false
        }

        prefs.edit()
            .putString(KEY_SIGNAL, normalized)
            .putString(KEY_DETAIL, detail.take(240))
            .putLong(KEY_AT, now)
            .putInt(KEY_PRIORITY, priority)
            .putLong(KEY_ACCEPTED, prefs.getLong(KEY_ACCEPTED, 0L) + 1L)
            .apply()
        return true
    }

    fun compact(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = RavenHauntModeStore.get(context)
        val accepted = prefs.getLong(KEY_ACCEPTED, 0L)
        val suppressed = prefs.getLong(KEY_SUPPRESSED, 0L)
        val signal = prefs.getString(KEY_SIGNAL, "none").orEmpty()
        val lastSuppression = prefs.getString(KEY_LAST_SUPPRESSION, "none").orEmpty()
        val hold = holdWindow(mode, signal)
        val dup = duplicateWindow(mode)
        return "CADENCE=${mode.label} accepted=$accepted suppressed=$suppressed hold=${hold}ms duplicate=${dup}ms last=$lastSuppression"
    }

    fun clearStats(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_ACCEPTED)
            .remove(KEY_SUPPRESSED)
            .remove(KEY_LAST_SUPPRESSION)
            .apply()
    }

    private fun recordSuppressed(context: Context, reason: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_SUPPRESSED, prefs.getLong(KEY_SUPPRESSED, 0L) + 1L)
            .putString(KEY_LAST_SUPPRESSION, reason.take(180))
            .apply()
    }

    private fun duplicateWindow(mode: RavenHauntMode): Long = when (mode) {
        RavenHauntMode.CALM -> 2_500L
        RavenHauntMode.LIVED_IN -> 1_800L
        RavenHauntMode.HAUNTED -> 1_200L
        RavenHauntMode.FERAL -> 700L
        RavenHauntMode.APOCALYPSE -> 300L
    }

    private fun holdWindow(mode: RavenHauntMode, previousSignal: String): Long {
        val base = when (mode) {
            RavenHauntMode.CALM -> 4_500L
            RavenHauntMode.LIVED_IN -> 3_000L
            RavenHauntMode.HAUNTED -> 1_800L
            RavenHauntMode.FERAL -> 900L
            RavenHauntMode.APOCALYPSE -> 350L
        }
        return if (previousSignal == "FOREGROUND_APP" || previousSignal == "APP_LAUNCH") (base * 1.35).toLong() else base
    }

    private fun priority(signal: String, detail: String): Int = when (signal) {
        "ERROR", "FAILURE", "CONFLICT" -> 100
        "BATTERY" -> if (detail.contains("low", ignoreCase = true)) 95 else 72
        "NIGHT" -> if (detail.contains("screen:off", ignoreCase = true)) 90 else 60
        "POWER" -> 78
        "HOME" -> if (detail.contains("user:present", ignoreCase = true)) 75 else 58
        "SYSTEM_DECK", "AUDIO", "DEVICE" -> 68
        "APP_LAUNCH" -> 62
        "FOREGROUND_APP" -> 60
        "SEARCH", "STUDIO", "EDIT" -> 56
        "INCOMING", "CLIPPING", "SHARE" -> 52
        "MEDIA", "MUSIC" -> 50
        "NOTIFICATION" -> 36
        "ROOM", "APP_UNIVERSE" -> 32
        "IDLE" -> 20
        else -> 45
    }
}
