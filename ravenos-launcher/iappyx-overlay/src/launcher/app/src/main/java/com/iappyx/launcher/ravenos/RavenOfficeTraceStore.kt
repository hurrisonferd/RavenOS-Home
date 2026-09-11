package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Bounded local flight recorder for deterministic Office routing decisions. */
object RavenOfficeTraceStore {
    private const val PREFS = "ravenos_office_trace_v1"
    private const val KEY_TRACE = "trace"
    private const val KEY_LAST_SIGNATURE = "last_signature"
    private const val MAX_ENTRIES = 24

    data class Entry(
        val at: Long,
        val owner: String,
        val signal: String,
        val detail: String,
        val note: String,
        val haunt: String,
    )

    fun record(
        context: Context,
        member: RavenOfficeMember,
        signal: String,
        detail: String,
        note: String,
        hauntMode: RavenHauntMode,
    ) {
        val signature = listOf(member.id, signal, detail, note, hauntMode.name).joinToString("|")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_SIGNATURE, null) == signature) return

        val existing = parseArray(prefs.getString(KEY_TRACE, null))
        val next = JSONArray()
        val fresh = JSONObject()
            .put("at", System.currentTimeMillis())
            .put("owner", member.id)
            .put("signal", signal)
            .put("detail", detail.take(220))
            .put("note", note.take(240))
            .put("haunt", hauntMode.name)
        next.put(fresh)
        for (i in 0 until minOf(existing.length(), MAX_ENTRIES - 1)) {
            next.put(existing.optJSONObject(i) ?: continue)
        }
        prefs.edit()
            .putString(KEY_TRACE, next.toString())
            .putString(KEY_LAST_SIGNATURE, signature)
            .apply()
    }

    fun recent(context: Context, limit: Int = 8): List<Entry> {
        val array = parseArray(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TRACE, null),
        )
        val out = ArrayList<Entry>()
        for (i in 0 until minOf(array.length(), limit.coerceIn(1, MAX_ENTRIES))) {
            val obj = array.optJSONObject(i) ?: continue
            out += Entry(
                at = obj.optLong("at", 0L),
                owner = obj.optString("owner", "?"),
                signal = obj.optString("signal", "?"),
                detail = obj.optString("detail", ""),
                note = obj.optString("note", ""),
                haunt = obj.optString("haunt", "?"),
            )
        }
        return out
    }

    fun compact(context: Context, limit: Int = 6): String {
        val entries = recent(context, limit)
        if (entries.isEmpty()) return "No Office routing receipts yet."
        return entries.joinToString("\n") { e ->
            val detail = if (e.detail.isBlank()) "" else " · ${e.detail.take(70)}"
            "${e.owner} · ${e.signal} · ${e.haunt}$detail"
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun parseArray(raw: String?): JSONArray = try {
        if (raw.isNullOrBlank()) JSONArray() else JSONArray(raw)
    } catch (_: Throwable) {
        JSONArray()
    }
}
