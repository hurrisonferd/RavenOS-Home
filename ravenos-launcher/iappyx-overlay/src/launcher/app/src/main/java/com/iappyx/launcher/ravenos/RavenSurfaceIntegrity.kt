package com.iappyx.launcher.ravenos

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Local proof ledger for RavenOS Office projections.
 *
 * Proof vocabulary is intentionally causal rather than optimistic:
 * SETTLED = canonical state exists.
 * POSTED = Android accepted the Office Bar foreground notification.
 * RENDERED = RavenOS updated an owned native View.
 * DISPATCHED = state was sent to a WebView/cross-process channel.
 * CONSUMED = RavenOS-owned JavaScript rendered the exact canonical updatedAt and called back.
 *
 * Wallpaper lives in the app's :wallpaper process. SharedPreferences are not trusted for that
 * receipt, so wallpaper acknowledgements use a tiny atomic JSON mirror in files/ravenos/.
 */
object RavenSurfaceIntegrity {
    private const val PREFS = "ravenos_surface_integrity_v1"
    private const val ACK_DIR = "ravenos"

    const val CANONICAL = "CANONICAL"
    const val OFFICE_BAR = "OFFICE_BAR"
    const val HOME_AURA = "HOME_AURA"
    const val HOME_WHISPER = "HOME_WHISPER"
    const val FOLLOW_ME = "FOLLOW_ME"
    const val WIDGETS = "WIDGETS"
    const val WALLPAPER_CHANNEL = "WALLPAPER_CHANNEL"

    private val surfaces = listOf(
        CANONICAL,
        OFFICE_BAR,
        HOME_AURA,
        HOME_WHISPER,
        FOLLOW_ME,
        WIDGETS,
        WALLPAPER_CHANNEL,
    )

    private data class Entry(
        val status: String,
        val stateAt: Long,
        val observedAt: Long,
        val detail: String,
    )

    fun mark(
        context: Context,
        surface: String,
        status: String,
        stateUpdatedAt: Long,
        detail: String = "",
    ) {
        val key = surface.trim().uppercase()
        if (key !in surfaces) return
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("$key.status", status.trim().uppercase())
            .putLong("$key.state_at", stateUpdatedAt)
            .putLong("$key.observed_at", System.currentTimeMillis())
            .putString("$key.detail", detail.take(180))
            .apply()
    }

    /**
     * Cross-process acknowledgement lane. Deliberately restricted to wallpaper proof.
     * Nothing here mutates Office state, permissions, routing, or resident identity.
     */
    fun markCrossProcess(
        context: Context,
        surface: String,
        status: String,
        stateUpdatedAt: Long,
        detail: String = "",
    ) {
        val key = surface.trim().uppercase()
        if (key != WALLPAPER_CHANNEL || stateUpdatedAt <= 0L) return
        val entry = Entry(
            status = status.trim().uppercase(),
            stateAt = stateUpdatedAt,
            observedAt = System.currentTimeMillis(),
            detail = detail.take(180),
        )
        writeCrossProcessEntry(context.applicationContext, key, entry)
    }

    fun compact(context: Context): String {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val canonical = RavenOfficeStateStore.read(context)
        val target = canonical?.updatedAt ?: readCanonicalTargetCrossProcess(app)
        val now = System.currentTimeMillis()
        return surfaces.joinToString(" | ") { surface ->
            val prefEntry = Entry(
                status = prefs.getString("$surface.status", "UNSEEN").orEmpty(),
                stateAt = prefs.getLong("$surface.state_at", 0L),
                observedAt = prefs.getLong("$surface.observed_at", 0L),
                detail = prefs.getString("$surface.detail", "").orEmpty(),
            )
            val cross = if (surface == WALLPAPER_CHANNEL) readCrossProcessEntry(app, surface) else null
            val entry = if (cross != null && cross.observedAt > prefEntry.observedAt) cross else prefEntry
            val sync = when {
                entry.stateAt <= 0L -> "NO_STATE"
                target > 0L && entry.stateAt == target -> "CURRENT"
                target > 0L && entry.stateAt < target -> "STALE"
                target > 0L && entry.stateAt > target -> "FUTURE"
                else -> "STATE"
            }
            val age = if (entry.observedAt <= 0L) -1L else (now - entry.observedAt).coerceAtLeast(0L)
            buildString {
                append(surface).append('=').append(entry.status).append('/').append(sync)
                if (age >= 0L) append('@').append(age).append("ms")
                if (entry.detail.isNotBlank()) append('(').append(entry.detail).append(')')
            }
        }
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        for (surface in surfaces) crossProcessFile(app, surface).delete()
    }

    private fun readCanonicalTargetCrossProcess(context: Context): Long = try {
        RavenOfficeStateStore.readSnapshotJson(context)
            ?.let { JSONObject(it).optLong("updatedAt", 0L) }
            ?: 0L
    } catch (_: Throwable) {
        0L
    }

    private fun crossProcessFile(context: Context, surface: String): File {
        val dir = File(context.filesDir, ACK_DIR).also { it.mkdirs() }
        return File(dir, "surface_integrity_${surface.lowercase()}.json")
    }

    private fun writeCrossProcessEntry(context: Context, surface: String, entry: Entry) {
        try {
            val target = crossProcessFile(context, surface)
            val tmp = File(target.parentFile, "${target.name}.tmp")
            val json = JSONObject()
                .put("status", entry.status)
                .put("stateAt", entry.stateAt)
                .put("observedAt", entry.observedAt)
                .put("detail", entry.detail)
                .toString()
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
        } catch (_: Throwable) {
            // Proof failure must never destabilize HOME.
        }
    }

    private fun readCrossProcessEntry(context: Context, surface: String): Entry? = try {
        val file = crossProcessFile(context, surface)
        if (!file.isFile) return null
        val json = JSONObject(file.readText(Charsets.UTF_8))
        Entry(
            status = json.optString("status", "UNSEEN"),
            stateAt = json.optLong("stateAt", 0L),
            observedAt = json.optLong("observedAt", 0L),
            detail = json.optString("detail", ""),
        )
    } catch (_: Throwable) {
        null
    }
}
