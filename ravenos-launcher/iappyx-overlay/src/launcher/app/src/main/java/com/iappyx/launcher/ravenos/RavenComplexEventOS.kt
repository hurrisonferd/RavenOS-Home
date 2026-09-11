package com.iappyx.launcher.ravenos

import android.content.Context

/** Bounded recurrence/sequence/convergence detector for launcher-local markers. */
object RavenComplexEventOS {
    private const val PREFS = "ravenos_complex_event_v1"

    data class Result(
        val occurrence: Int,
        val tags: Set<String>,
        val recentSwitches: Int,
        val signature: String,
    )

    fun analyze(context: Context, marker: RavenMarkerBus.Marker): Result {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val signature = signature(marker)
        val countKey = "count_${stableHash(signature)}"
        val occurrence = prefs.getInt(countKey, 0) + 1
        prefs.edit().putInt(countKey, occurrence).apply()

        val now = marker.at
        val recent = RavenMarkerBus.recent(app, 32)
        val switches = recent.count { it.key == "APP_ENTER" && now - it.at <= 15_000L }
        val tags = linkedSetOf<String>()
        if (occurrence >= 2) tags += "RECURRING"
        if (occurrence >= 3) tags += "RUNNING_BIT"
        if (occurrence >= 5) tags += "EMPLOYEE"
        if (occurrence >= 8) tags += "TENANT"
        if (occurrence >= 13) tags += "MANAGEMENT"
        if (occurrence >= 21) tags += "LOCAL_MYTHOLOGY"
        if (occurrence >= 34) tags += "HISTORIC_LANDMARK"
        if (switches >= 4) tags += "APP_SWITCH_BURST"

        val sameRecent = recent.dropLast(1).takeLast(8).count { signature(it) == signature }
        if (sameRecent >= 2) tags += "RETURN_LOOP"
        if ("SUCCESS" in marker.tags && ("RECURRING" in tags || "RETURN_LOOP" in tags)) tags += "PAYOFF"
        if ("RECOVERY" in marker.tags && recent.any { "ERROR" in it.tags || "ATTENTION" in it.tags }) tags += "RECOVERY_ARC"
        return Result(occurrence, tags, switches, signature)
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun signature(marker: RavenMarkerBus.Marker): String {
        val pkg = Regex("package:([^|\\s]+)").find(marker.detail)?.groupValues?.getOrNull(1).orEmpty()
        val coarse = if (pkg.isNotBlank()) pkg else marker.detail.substringBefore('|').take(80)
        return "${marker.key}|$coarse"
    }

    private fun stableHash(text: String): String {
        var hash = 0x811C9DC5.toInt()
        for (c in text) {
            hash = hash xor c.code
            hash *= 16777619
        }
        return (hash and Int.MAX_VALUE).toString(16)
    }
}
