package com.iappyx.launcher.ravenos

import android.content.Context
import android.os.SystemClock

/** Deterministic anti-flap governor: screen meaning outranks phone-action chatter. */
object RavenOfficeGovernor {
    private const val PREFS = "ravenos_office_governor_v2"
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

        val appEdges = setOf("APP_LAUNCH", "FOREGROUND_APP", "FOREGROUND_USAGE")
        if (normalized in appEdges && previousSignal in appEdges && samePackage(detail, previousDetail) && age < dupWindow * 3L) {
            recordSuppressed(context, "semantic-app-edge:$previousSignal->$normalized:${age}ms")
            return false
        }

        if (normalized == "FOREGROUND_WINDOW" && previousSignal == "FOREGROUND_WINDOW" && samePackage(detail, previousDetail) && age < dupWindow * 2L) {
            recordSuppressed(context, "window-flap:${age}ms")
            return false
        }

        val hold = holdWindow(mode, previousSignal)
        val recursive = detail.contains("meta:true", true)
        val screenMeaning = normalized in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC")
        val screenProtected = screenMeaning && detail.contains("suppressed", true)
        val canPreempt = recursive || screenProtected || priority >= previousPriority + 18
        if (!canPreempt && age < hold && priority <= previousPriority) {
            recordSuppressed(context, "hold:$normalized:p$priority<=p$previousPriority:${age}ms<$hold")
            return false
        }

        // Action edges can keep updating evidence, but they no longer get to hammer the visible
        // Office surface several times per second. Fresh screen meaning may still preempt them.
        if (isActionNoise(normalized) && age < actionFloor(mode)) {
            recordSuppressed(context, "action-floor:$normalized:${age}ms")
            return false
        }

        prefs.edit()
            .putString(KEY_SIGNAL, normalized)
            .putString(KEY_DETAIL, detail.take(360))
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
        return "CADENCE=${mode.label} accepted=$accepted suppressed=$suppressed hold=${hold}ms duplicate=${dup}ms screen-first=true last=$lastSuppression"
    }

    fun clearStats(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_ACCEPTED).remove(KEY_SUPPRESSED).remove(KEY_LAST_SUPPRESSION).apply()
    }

    private fun samePackage(a: String, b: String): Boolean {
        fun pkg(s: String): String? = Regex("(?:^|\\|)package:([^|]+)").find(s)?.groupValues?.getOrNull(1)
        val pa = pkg(a); val pb = pkg(b)
        return pa != null && pa == pb
    }

    private fun isActionNoise(signal: String): Boolean = signal in setOf(
        "APP_LAUNCH", "FOREGROUND_APP", "FOREGROUND_USAGE", "FOREGROUND_WINDOW",
        "MEDIA", "MUSIC", "MEDIA_SESSION", "NOTIFICATION", "NOTIFICATION_SENSE",
    )

    private fun recordSuppressed(context: Context, reason: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_SUPPRESSED, prefs.getLong(KEY_SUPPRESSED, 0L) + 1L)
            .putString(KEY_LAST_SUPPRESSION, reason.take(180)).apply()
    }

    private fun duplicateWindow(mode: RavenHauntMode): Long = when (mode) {
        RavenHauntMode.CALM -> 10_000L
        RavenHauntMode.LIVED_IN -> 8_000L
        RavenHauntMode.HAUNTED -> 6_000L
        RavenHauntMode.FERAL -> 4_000L
        RavenHauntMode.APOCALYPSE -> 2_500L
    }

    private fun actionFloor(mode: RavenHauntMode): Long = when (mode) {
        RavenHauntMode.CALM -> 14_000L
        RavenHauntMode.LIVED_IN -> 11_000L
        RavenHauntMode.HAUNTED -> 8_000L
        RavenHauntMode.FERAL -> 6_000L
        RavenHauntMode.APOCALYPSE -> 4_000L
    }

    private fun holdWindow(mode: RavenHauntMode, previousSignal: String): Long {
        val base = when (mode) {
            RavenHauntMode.CALM -> 18_000L
            RavenHauntMode.LIVED_IN -> 14_000L
            RavenHauntMode.HAUNTED -> 10_000L
            RavenHauntMode.FERAL -> 7_000L
            RavenHauntMode.APOCALYPSE -> 5_000L
        }
        return when (previousSignal) {
            "SCREEN_TEXT", "SCREEN_SEMANTIC" -> (base * 1.5).toLong()
            "SCREEN_VISUAL" -> (base * 1.2).toLong()
            "FOREGROUND_APP", "FOREGROUND_USAGE", "APP_LAUNCH", "FOREGROUND_WINDOW" -> (base * 1.25).toLong()
            else -> base
        }
    }

    private fun priority(signal: String, detail: String): Int = when (signal) {
        "ERROR", "FAILURE", "CONFLICT" -> 100
        "BATTERY" -> if (detail.contains("low", true)) 96 else 60
        "SCREEN_SEMANTIC" -> if (detail.contains("meta:true", true)) 94 else if (detail.contains("suppressed", true)) 92 else 84
        "SCREEN_TEXT" -> if (detail.contains("meta:true", true)) 93 else if (detail.contains("suppressed", true)) 91 else 82
        "NIGHT" -> if (detail.contains("screen:off", true)) 90 else 55
        "POWER" -> 76
        "HOME" -> if (detail.contains("user:present", true)) 68 else 38
        "SYSTEM_DECK", "AUDIO", "DEVICE" -> 62
        "SCREEN_VISUAL" -> if (detail.contains("motion:6") || detail.contains("motion:7") || detail.contains("motion:8") || detail.contains("motion:9")) 58 else 48
        "SEARCH", "STUDIO", "EDIT" -> 50
        "INCOMING", "CLIPPING", "SHARE" -> 48
        "APP_LAUNCH", "FOREGROUND_APP", "FOREGROUND_USAGE" -> 38
        "MEDIA_SESSION", "MEDIA", "MUSIC" -> 34
        "NOTIFICATION", "NOTIFICATION_SENSE" -> if (detail.contains("alerting:true")) 42 else 30
        "FOREGROUND_WINDOW" -> 26
        "ROOM", "APP_UNIVERSE" -> 28
        "IDLE" -> 15
        else -> 36
    }
}
