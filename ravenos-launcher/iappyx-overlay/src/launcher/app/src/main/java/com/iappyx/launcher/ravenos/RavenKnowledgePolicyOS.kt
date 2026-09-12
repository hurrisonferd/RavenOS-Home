package com.iappyx.launcher.ravenos

import android.content.Context

/** Owner-selectable policy for online knowledge enrichment. */
object RavenKnowledgePolicyOS {
    enum class Mode { OFF, GENERAL, CONTEXTUAL }

    private const val PREFS = "ravenos_knowledge_policy_v1"
    private const val KEY_MODE = "mode"

    fun mode(context: Context): Mode {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, Mode.GENERAL.name)
        return runCatching { Mode.valueOf(raw.orEmpty()) }.getOrDefault(Mode.GENERAL)
    }

    fun setMode(context: Context, mode: Mode) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
    }

    fun ttlMs(scope: String): Long = when (scope.uppercase()) {
        "SELF_REPO" -> 30L * 60L * 1000L
        "CONTEXTUAL" -> 6L * 60L * 60L * 1000L
        else -> 12L * 60L * 60L * 1000L
    }

    fun allowsContextual(context: Context): Boolean = mode(context) == Mode.CONTEXTUAL
}
