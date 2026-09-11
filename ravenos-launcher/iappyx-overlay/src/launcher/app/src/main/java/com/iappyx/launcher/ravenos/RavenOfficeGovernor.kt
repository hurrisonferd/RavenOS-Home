package com.iappyx.launcher.ravenos

import android.content.Context
import android.os.SystemClock

/**
 * Deterministic anti-flap governor for reactive Office signals.
 *
 * The Office should feel alive, not like a notification slot machine. This governor runs before
 * RavenOfficeBarService is started, so rejected noise creates no FGS churn, no trace receipt, and
 * no cross-surface re-render. It changes cadence only; it never grants or revokes capability.
 *
 * Haunt mode intentionally changes the hold window:
 * CALM holds a resident longest, APOCALYPSE permits rapid switching. High-priority safety/state
 * changes always interrupt lower-priority presentation noise.
 */
object RavenOfficeGovernor {
    private const val PREFS = "ravenos_office_governor_v1"
    private const val KEY_SIGNAL = "last_signal"
    private const val KEY_DETAIL = "last_detail"
    private const val KEY_AT = "last_elapsed_ms"
    private const val KEY_PRIORITY = "last_priority"

    fun accept(context: Context, signal: String, detail: String, mode: RavenHauntMode): Boolean {
        val normalized = signal.trim().uppercase()
        val now = SystemClock.elapsedRealtime()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previousSignal = prefs.getString(KEY_SIGNAL, "").orEmpty()
        val previousDetail = prefs.getString(KEY_DETAIL, "").orEmpty()
        val previousAt = prefs.getLong(KEY_AT, -1L)
        val previousPriority = prefs.getInt(KEY_PRIORITY, Int.MIN_VALUE)
        val priority = priority(normalized, detail)

        // elapsedRealtime resets at reboot. A persisted timestamp from the prior boot must not
        // suppress the first signal of the new boot.
        val age = if (previousAt < 0L || now < previousAt) Long.MAX_VALUE else now - previousAt

        // Collapse exact bursts from duplicated Android callbacks / launcher lifecycle edges.
        if (normalized == previousSignal && detail == previousDetail && age < duplicateWindow(mode)) {
            return false
        }

        // Critical/higher-priority context can preempt immediately. Equal/lower-priority chatter
        // waits for the current resident's minimum dwell time.
        if (age < holdWindow(mode, previousSignal) && priority <= previousPriority) {
            return false
        }

        prefs.edit()
            .putString(KEY_SIGNAL, normalized)
            .putString(KEY_DETAIL, detail.take(240))
            .putLong(KEY_AT, now)
            .putInt(KEY_PRIORITY, priority)
            .apply()
        return true
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
        // Foreground/app transitions are especially prone to lifecycle bounce through SystemUI,
        // launcher, permission dialogs, and the destination app. Give them one extra beat.
        return if (previousSignal == "FOREGROUND_APP" || previousSignal == "APP_LAUNCH") {
            (base * 1.35).toLong()
        } else base
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
