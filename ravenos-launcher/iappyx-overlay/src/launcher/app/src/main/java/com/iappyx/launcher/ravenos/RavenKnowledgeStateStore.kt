package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONObject

/** Small bounded cache for externally sourced knowledge. */
object RavenKnowledgeStateStore {
    data class Snapshot(
        val topic: String,
        val summary: String,
        val provider: String,
        val source: String,
        val url: String,
        val scope: String,
        val fetchedAt: Long,
    ) {
        fun fresh(now: Long = System.currentTimeMillis()): Boolean =
            now - fetchedAt <= RavenKnowledgePolicyOS.ttlMs(scope)
    }

    private const val PREFS = "ravenos_knowledge_state_v1"
    private const val KEY_LATEST = "latest"

    fun write(context: Context, snapshot: Snapshot) {
        val json = JSONObject()
            .put("topic", snapshot.topic)
            .put("summary", snapshot.summary)
            .put("provider", snapshot.provider)
            .put("source", snapshot.source)
            .put("url", snapshot.url)
            .put("scope", snapshot.scope)
            .put("fetchedAt", snapshot.fetchedAt)
            .toString()
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LATEST, json).apply()
    }

    fun read(context: Context): Snapshot? {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LATEST, null) ?: return null
        return runCatching {
            val j = JSONObject(raw)
            Snapshot(
                topic = j.optString("topic"),
                summary = j.optString("summary"),
                provider = j.optString("provider"),
                source = j.optString("source"),
                url = j.optString("url"),
                scope = j.optString("scope"),
                fetchedAt = j.optLong("fetchedAt"),
            )
        }.getOrNull()?.takeIf { it.topic.isNotBlank() && it.summary.isNotBlank() }
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
