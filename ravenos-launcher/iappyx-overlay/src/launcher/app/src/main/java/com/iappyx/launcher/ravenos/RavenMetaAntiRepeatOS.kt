package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Hash-only recent-history ledger for presentation gags.
 * Stores structural fingerprints, never screen text or dialogue source evidence.
 */
object RavenMetaAntiRepeatOS {
    data class Choice(
        val text: String,
        val fingerprint: String,
        val attempt: Int,
        val repeated: Boolean,
    )

    private const val PREFS = "ravenos_meta_antirepeat_v1"
    private const val MAX_GLOBAL = 24
    private const val MAX_SCOPE = 12

    fun choose(
        context: Context,
        scope: String,
        seed: Int,
        candidates: List<String>,
    ): Choice {
        val clean = candidates.map(String::trim).filter(String::isNotBlank).distinct()
        if (clean.isEmpty()) return Choice("", "", 0, false)

        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val safeScope = scope.uppercase().replace(Regex("[^A-Z0-9_]+"), "_").take(80)
        val global = decode(prefs.getString("global", ""))
        val local = decode(prefs.getString("scope_$safeScope", ""))
        val blocked = (global.takeLast(MAX_GLOBAL) + local.takeLast(MAX_SCOPE)).toSet()
        val start = Math.floorMod(seed, clean.size)

        var fallback: Choice? = null
        for (offset in clean.indices) {
            val index = (start + offset) % clean.size
            val text = clean[index]
            val fp = fingerprint(text)
            val repeated = fp in blocked
            val choice = Choice(text, fp, offset, repeated)
            if (fallback == null) fallback = choice
            if (!repeated) {
                remember(prefs, safeScope, fp, global, local)
                return choice
            }
        }

        val picked = fallback ?: Choice(clean[start], fingerprint(clean[start]), 0, true)
        remember(prefs, safeScope, picked.fingerprint, global, local)
        return picked
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun remember(
        prefs: android.content.SharedPreferences,
        scope: String,
        fp: String,
        global: List<String>,
        local: List<String>,
    ) {
        val nextGlobal = (global + fp).takeLast(MAX_GLOBAL)
        val nextLocal = (local + fp).takeLast(MAX_SCOPE)
        prefs.edit()
            .putString("global", nextGlobal.joinToString(","))
            .putString("scope_$scope", nextLocal.joinToString(","))
            .apply()
    }

    private fun decode(value: String?): List<String> = value.orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotBlank)

    private fun fingerprint(text: String): String {
        val structural = text.lowercase()
            .replace(Regex("[0-9]+"), "#")
            .replace(Regex("\\s+"), " ")
            .trim()
        return structural.hashCode().toUInt().toString(16)
    }
}
