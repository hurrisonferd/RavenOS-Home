package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Bounded local flight recorder for deterministic Office routing decisions. */
object RavenOfficeTraceStore {
    private const val PREFS = "ravenos_office_trace_v2"
    private const val KEY_TRACE = "trace"
    private const val MAX_ENTRIES = 24
    private const val COALESCE_MS = 120_000L

    data class Entry(
        val at: Long,
        val owner: String,
        val signal: String,
        val detail: String,
        val note: String,
        val haunt: String,
        val repeats: Int,
    )

    @Synchronized
    fun record(
        context: Context,
        member: RavenOfficeMember,
        signal: String,
        detail: String,
        note: String,
        hauntMode: RavenHauntMode,
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = parseArray(prefs.getString(KEY_TRACE, null))
        val now = System.currentTimeMillis()
        val clippedNote = note.take(240)
        val first = existing.optJSONObject(0)

        // Stale-repeat killer: same resident + same commentary within two minutes becomes one
        // evolving receipt instead of ten visually identical feed rows. The newest signal/detail
        // are retained and the repeat count stays explicit.
        val canCoalesce = first != null &&
            first.optString("owner") == member.id &&
            first.optString("note") == clippedNote &&
            first.optString("haunt") == hauntMode.name &&
            now - first.optLong("at", 0L) in 0..COALESCE_MS

        val next = JSONArray()
        if (canCoalesce) {
            first!!.put("at", now)
                .put("signal", signal)
                .put("detail", detail.take(220))
                .put("repeats", first.optInt("repeats", 1) + 1)
            next.put(first)
            for (i in 1 until minOf(existing.length(), MAX_ENTRIES)) {
                next.put(existing.optJSONObject(i) ?: continue)
            }
        } else {
            next.put(
                JSONObject()
                    .put("at", now)
                    .put("owner", member.id)
                    .put("signal", signal)
                    .put("detail", detail.take(220))
                    .put("note", clippedNote)
                    .put("haunt", hauntMode.name)
                    .put("repeats", 1),
            )
            for (i in 0 until minOf(existing.length(), MAX_ENTRIES - 1)) {
                next.put(existing.optJSONObject(i) ?: continue)
            }
        }
        prefs.edit().putString(KEY_TRACE, next.toString()).apply()
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
                repeats = obj.optInt("repeats", 1).coerceAtLeast(1),
            )
        }
        return out
    }

    fun compact(context: Context, limit: Int = 6): String {
        val entries = recent(context, limit)
        if (entries.isEmpty()) return "No Office routing receipts yet."
        return entries.joinToString("\n") { e ->
            val detail = if (e.detail.isBlank()) "" else " · ${e.detail.take(70)}"
            val repeat = if (e.repeats > 1) " ×${e.repeats}" else ""
            "${e.owner} · ${e.signal} · ${e.haunt}$repeat$detail"
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
