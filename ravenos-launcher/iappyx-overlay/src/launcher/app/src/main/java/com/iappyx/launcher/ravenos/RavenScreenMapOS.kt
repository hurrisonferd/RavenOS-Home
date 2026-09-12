package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bounded geometric map of OCR-visible text blocks.
 * Coordinates are normalized to 0..1000; no bitmap or screenshot is persisted.
 */
object RavenScreenMapOS {
    data class Block(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val centerX: Int get() = (left + right) / 2
        val centerY: Int get() = (top + bottom) / 2
    }
    data class Map(val blocks: List<Block>, val capturedAt: Long) {
        fun occupancy(zone: String): Int = blocks.count { block ->
            when (zone) {
                "top" -> block.centerY < 333
                "middle" -> block.centerY in 333..665
                "bottom" -> block.centerY >= 666
                "left" -> block.centerX < 333
                "center" -> block.centerX in 333..665
                "right" -> block.centerX >= 666
                else -> false
            }
        }
        fun quietZone(keyboard: Boolean = false): String {
            val zones = if (keyboard) listOf("top", "middle") else listOf("top", "middle", "bottom")
            return zones.minByOrNull { occupancy(it) } ?: "top"
        }
        fun layout(): String = buildString {
            append("rows=").append(occupancy("top")).append('/').append(occupancy("middle")).append('/').append(occupancy("bottom"))
            append(" cols=").append(occupancy("left")).append('/').append(occupancy("center")).append('/').append(occupancy("right"))
        }
    }

    private const val PREFS = "ravenos_screen_map_v1"
    private const val KEY_JSON = "map"
    private const val KEY_AT = "at"
    private const val TTL_MS = 24_000L

    fun record(context: Context, frameWidth: Int, frameHeight: Int, blocks: List<Block>) {
        if (frameWidth <= 0 || frameHeight <= 0) return
        val arr = JSONArray()
        blocks.take(32).forEach { b ->
            fun nx(v: Int) = (v.coerceIn(0, frameWidth) * 1000 / frameWidth).coerceIn(0, 1000)
            fun ny(v: Int) = (v.coerceIn(0, frameHeight) * 1000 / frameHeight).coerceIn(0, 1000)
            arr.put(JSONObject()
                .put("t", b.text.take(96))
                .put("l", nx(b.left)).put("r", nx(b.right))
                .put("y1", ny(b.top)).put("y2", ny(b.bottom)))
        }
        val now = System.currentTimeMillis()
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_JSON, arr.toString()).putLong(KEY_AT, now).apply()
    }

    fun latest(context: Context, now: Long = System.currentTimeMillis()): Map? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val at = prefs.getLong(KEY_AT, 0L)
        if (at <= 0L || now - at > TTL_MS) return null
        val raw = prefs.getString(KEY_JSON, "[]").orEmpty()
        val arr = try { JSONArray(raw) } catch (_: Throwable) { return null }
        val out = ArrayList<Block>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val text = o.optString("t").trim()
            if (text.isBlank()) continue
            out += Block(text, o.optInt("l"), o.optInt("y1"), o.optInt("r"), o.optInt("y2"))
        }
        return Map(out, at)
    }

    fun compact(context: Context): String {
        val map = latest(context) ?: return "SCREEN_MAP=EMPTY"
        return "SCREEN_MAP=${map.blocks.size} blocks · ${map.layout()}"
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
