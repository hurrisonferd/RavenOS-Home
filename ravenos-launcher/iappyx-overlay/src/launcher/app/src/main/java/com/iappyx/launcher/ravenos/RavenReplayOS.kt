package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Semantic replay/bookmark lane. It saves reaction receipts, never raw screen/audio. */
object RavenReplayOS {
    private const val PREFS = "ravenos_replay_os_v1"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val MAX = 32

    fun saveCurrent(context: Context): String {
        val current = RavenEvidenceBoard.last(context) ?: return "SAVE: no current Goblin Vision receipt"
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = try { JSONArray(prefs.getString(KEY_BOOKMARKS, "[]")) } catch (_: Throwable) { JSONArray() }
        val bookmark = JSONObject(current.toString()).put("bookmarkedAt", System.currentTimeMillis())
        arr.put(bookmark)
        val trimmed = JSONArray()
        val start = (arr.length() - MAX).coerceAtLeast(0)
        for (i in start until arr.length()) trimmed.put(arr.opt(i))
        prefs.edit().putString(KEY_BOOKMARKS, trimmed.toString()).apply()
        return "SAVED ${current.optString("owner")}:${current.optString("visualState")} #${current.optInt("occurrence", 1)}"
    }

    fun compact(context: Context, limit: Int = 6): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = try { JSONArray(prefs.getString(KEY_BOOKMARKS, "[]")) } catch (_: Throwable) { JSONArray() }
        if (arr.length() == 0) return "REPLAY EMPTY"
        val lines = mutableListOf<String>()
        val start = (arr.length() - limit.coerceAtLeast(1)).coerceAtLeast(0)
        for (i in start until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            lines += "${o.optString("owner")}:${o.optString("signal")}:${o.optString("visualState")}"
        }
        return lines.joinToString("\n")
    }
}
