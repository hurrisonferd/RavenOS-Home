package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Bounded explainability ledger for Goblin Vision reactions. */
object RavenEvidenceBoard {
    private const val PREFS = "ravenos_evidence_board_v1"
    private const val KEY_RING = "ring"
    private const val MAX = 48

    @Synchronized
    fun record(context: Context, packet: RavenReactionPacket) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ring = try { JSONArray(prefs.getString(KEY_RING, "[]")) } catch (_: Throwable) { JSONArray() }
        ring.put(packet.toJson())
        val trimmed = JSONArray()
        val start = (ring.length() - MAX).coerceAtLeast(0)
        for (i in start until ring.length()) trimmed.put(ring.opt(i))
        prefs.edit().putString(KEY_RING, trimmed.toString()).apply()
    }

    fun last(context: Context): JSONObject? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ring = try { JSONArray(prefs.getString(KEY_RING, "[]")) } catch (_: Throwable) { JSONArray() }
        return if (ring.length() > 0) ring.optJSONObject(ring.length() - 1) else null
    }

    fun why(context: Context): String {
        val obj = last(context) ?: return "WHY: no Goblin Vision reaction has been receipted yet."
        val tags = obj.optJSONArray("complexTags")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }.joinToString(",")
        }.orEmpty()
        return buildString {
            append("WHY ").append(obj.optString("owner")).append('\n')
            append("event=").append(obj.optString("signal"))
            append(" · occurrence=").append(obj.optInt("occurrence", 1)).append('\n')
            append("visual=").append(obj.optString("visualState"))
            append(" · pose=").append(obj.optString("pose"))
            append(" · zone=").append(obj.optString("zone")).append('\n')
            append("episode=").append(obj.optString("episode"))
            append(" · highlight=").append(obj.optString("highlight"))
            append('(').append(obj.optInt("highlightScore", 0)).append(')').append('\n')
            if (tags.isNotBlank()) append("complex=").append(tags).append('\n')
            append("proof=").append(obj.optString("proof"))
            append(" · authority=").append(obj.optString("effectAuthority", "NONE"))
        }
    }

    fun compact(context: Context, limit: Int = 8): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ring = try { JSONArray(prefs.getString(KEY_RING, "[]")) } catch (_: Throwable) { JSONArray() }
        if (ring.length() == 0) return "EVIDENCE BOARD EMPTY"
        val lines = mutableListOf<String>()
        val start = (ring.length() - limit.coerceAtLeast(1)).coerceAtLeast(0)
        for (i in start until ring.length()) {
            val o = ring.optJSONObject(i) ?: continue
            lines += "${o.optString("owner")}:${o.optString("signal")}:${o.optString("visualState")}#${o.optInt("occurrence", 1)}"
        }
        return lines.joinToString("\n")
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
