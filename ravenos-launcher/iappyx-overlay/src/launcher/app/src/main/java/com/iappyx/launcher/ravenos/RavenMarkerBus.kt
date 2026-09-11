package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Android Goblin Vision MarkerBus.
 *
 * Raw launcher/device callbacks are normalized into small typed markers before any comedy,
 * employee casting, optional vision, or model-facing path gets a vote. The bus is local,
 * deterministic, bounded, and carries no effect authority.
 */
object RavenMarkerBus {
    private const val PREFS = "ravenos_marker_bus_v1"
    private const val KEY_RING = "ring"
    private const val MAX_MARKERS = 96

    data class Marker(
        val id: String,
        val key: String,
        val detail: String,
        val source: String,
        val at: Long,
        val salience: Int,
        val tags: Set<String>,
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("id", id)
            .put("key", key)
            .put("detail", detail)
            .put("source", source)
            .put("at", at)
            .put("salience", salience)
            .put("tags", JSONArray(tags.toList()))
    }

    @Synchronized
    fun emit(context: Context, rawKey: String, detail: String, source: String = "ANDROID"): Marker {
        val key = normalizeKey(rawKey)
        val at = System.currentTimeMillis()
        val tags = deriveTags(key, detail)
        val marker = Marker(
            id = "$at-${stableHash("$key|$detail|$source")}",
            key = key,
            detail = detail.take(320),
            source = source,
            at = at,
            salience = salience(key, tags),
            tags = tags,
        )
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ring = readArray(prefs.getString(KEY_RING, null))
        ring.put(marker.toJson())
        while (ring.length() > MAX_MARKERS) {
            val next = JSONArray()
            for (i in 1 until ring.length()) next.put(ring.opt(i))
            prefs.edit().putString(KEY_RING, next.toString()).apply()
            return marker
        }
        prefs.edit().putString(KEY_RING, ring.toString()).apply()
        return marker
    }

    fun recent(context: Context, limit: Int = 24): List<Marker> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ring = readArray(prefs.getString(KEY_RING, null))
        val out = ArrayList<Marker>()
        val start = (ring.length() - limit.coerceAtLeast(1)).coerceAtLeast(0)
        for (i in start until ring.length()) {
            val obj = ring.optJSONObject(i) ?: continue
            val tags = linkedSetOf<String>()
            val arr = obj.optJSONArray("tags")
            if (arr != null) for (j in 0 until arr.length()) arr.optString(j).takeIf { it.isNotBlank() }?.let(tags::add)
            out += Marker(
                id = obj.optString("id"),
                key = obj.optString("key"),
                detail = obj.optString("detail"),
                source = obj.optString("source", "ANDROID"),
                at = obj.optLong("at"),
                salience = obj.optInt("salience", 1),
                tags = tags,
            )
        }
        return out
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun normalizeKey(raw: String): String = when (raw.trim().uppercase()) {
        "FOREGROUND_APP", "APP_LAUNCH" -> "APP_ENTER"
        "NOTIFICATION" -> "NOTIFICATION_POSTED"
        "MEDIA", "MUSIC" -> "MEDIA_ACTIVE"
        "POWER" -> "POWER_CHANGED"
        "BATTERY" -> "BATTERY_CHANGED"
        "HOME" -> "HOME_ENTER"
        "ROOM" -> "ROOM_CHANGED"
        "SEARCH" -> "SEARCH_OPENED"
        "SYSTEM_DECK" -> "SYSTEM_DECK_OPENED"
        "APP_UNIVERSE" -> "APP_UNIVERSE_OPENED"
        else -> raw.trim().uppercase().ifBlank { "UNKNOWN" }
    }

    private fun deriveTags(key: String, detail: String): Set<String> {
        val tags = linkedSetOf<String>()
        when {
            key.startsWith("APP_") -> tags += "APP"
            key.startsWith("NOTIFICATION") -> tags += "NOTIFICATION"
            key.startsWith("MEDIA") -> tags += "MEDIA"
            key.startsWith("BATTERY") || key.startsWith("POWER") -> tags += "POWER"
            key.contains("ERROR") || key.contains("FAIL") || key.contains("CONFLICT") -> tags += "ERROR"
            key.contains("HOME") -> tags += "HOME"
        }
        val d = detail.lowercase()
        if (listOf("spotify", "music", "soundcloud", "youtube.music", "audio").any(d::contains)) tags += "MUSIC"
        if (listOf("github", "gitlab", "termux", "studio", "code", "build").any(d::contains)) tags += "BUILD"
        if (listOf("chrome", "firefox", "browser", "opera", "reddit", "wikipedia").any(d::contains)) tags += "DISCOVERY"
        if (listOf("permission", "settings", "auth", "security", "wallet", "bank").any(d::contains)) tags += "BOUNDARY"
        if (listOf("gmail", "messages", "discord", "slack", "telegram", "whatsapp").any(d::contains)) tags += "COMMUNICATION"
        if (listOf("low", "critical", "fail", "error", "denied").any(d::contains)) tags += "ATTENTION"
        if (listOf("success", "passed", "complete", "green", "done").any(d::contains)) tags += "SUCCESS"
        if (listOf("charging", "restored", "recovered").any(d::contains)) tags += "RECOVERY"
        return tags
    }

    private fun salience(key: String, tags: Set<String>): Int {
        var score = 2
        if ("ERROR" in tags) score += 5
        if ("ATTENTION" in tags) score += 3
        if ("SUCCESS" in tags) score += 2
        if ("BOUNDARY" in tags) score += 2
        if (key == "HOME_ENTER" || key == "ROOM_CHANGED") score -= 1
        return score.coerceIn(0, 10)
    }

    private fun readArray(raw: String?): JSONArray = try { JSONArray(raw ?: "[]") } catch (_: Throwable) { JSONArray() }

    private fun stableHash(text: String): String {
        var hash = 0x811C9DC5.toInt()
        for (c in text) {
            hash = hash xor c.code
            hash *= 16777619
        }
        return (hash and Int.MAX_VALUE).toString(16)
    }
}
