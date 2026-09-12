package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONObject

/** Small bounded cache for externally sourced knowledge, partitioned by provenance lane. */
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

    private const val PREFS = "ravenos_knowledge_state_v2"

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
            .edit().putString(key(snapshot.scope), json).apply()
    }

    fun read(context: Context, scope: String): Snapshot? {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(scope), null) ?: return null
        return decode(raw)
    }

    fun latest(context: Context): Snapshot? = listOfNotNull(
        read(context, "CONTEXTUAL"),
        read(context, "SELF_REPO"),
    ).filter { it.fresh() }.maxByOrNull { it.fetchedAt }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun key(scope: String): String = "latest_${scope.uppercase().replace(Regex("[^A-Z0-9_]+"), "_")}" 

    private fun decode(raw: String): Snapshot? = runCatching {
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
