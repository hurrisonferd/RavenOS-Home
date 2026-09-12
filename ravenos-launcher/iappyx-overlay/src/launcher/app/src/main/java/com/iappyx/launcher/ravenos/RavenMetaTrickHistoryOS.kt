package com.iappyx.launcher.ravenos

import android.content.Context

/** Prevents the same comedy form from monopolizing a scene/owner lane. */
object RavenMetaTrickHistoryOS {
    private const val PREFS = "ravenos_meta_trick_history_v1"
    private const val KEEP = 10

    fun choose(
        context: Context,
        scope: String,
        seed: Int,
        eligible: List<String>,
    ): String {
        val options = eligible.map(String::trim).filter(String::isNotBlank).distinct()
        if (options.isEmpty()) return ""
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "lane_${scope.uppercase().replace(Regex("[^A-Z0-9_]+"), "_").take(96)}"
        val recent = prefs.getString(key, "").orEmpty().split(',').filter(String::isNotBlank).takeLast(KEEP)
        val start = Math.floorMod(seed, options.size)
        val fresh = options.indices
            .map { options[(start + it) % options.size] }
            .firstOrNull { it !in recent }
            ?: options[start]
        val next = (recent + fresh).takeLast(KEEP)
        prefs.edit().putString(key, next.joinToString(",")).apply()
        return fresh
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
