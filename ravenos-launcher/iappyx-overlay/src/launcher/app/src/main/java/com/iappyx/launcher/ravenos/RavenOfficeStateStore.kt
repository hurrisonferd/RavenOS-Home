package com.iappyx.launcher.ravenos

import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.io.File

/** Single local current-state snapshot for all RavenOS Office projections. */
object RavenOfficeStateStore {
    private const val PREFS = "ravenos_office_state_v1"
    private const val SNAPSHOT_DIR = "ravenos"
    private const val SNAPSHOT_FILE = "office_state.json"

    const val ACTION_CHANGED = "com.ravenos.launcher.office.STATE_CHANGED"
    const val EXTRA_JSON = "json"

    data class Snapshot(
        val owner: String,
        val emoji: String,
        val accent: Int,
        val lane: String,
        val signal: String,
        val detail: String,
        val note: String,
        val haunt: String,
        val manual: Boolean,
        val quiet: Boolean,
        val updatedAt: Long,
    ) {
        fun compact(): String = buildString {
            append(emoji).append(' ').append(owner)
            append(" · ").append(signal)
            append(" · ").append(haunt)
            append(" · ").append(if (manual) "PINNED" else "AUTO")
            if (quiet) append(" · QUIET")
            if (detail.isNotBlank()) append(" · ").append(detail.take(100))
        }
    }

    fun write(
        context: Context,
        member: RavenOfficeMember,
        signal: String,
        detail: String,
        note: String,
        hauntMode: RavenHauntMode,
        manual: Boolean,
        quiet: Boolean,
    ): Snapshot {
        val app = context.applicationContext
        val snapshot = Snapshot(
            owner = member.id,
            emoji = member.emoji,
            accent = member.accent,
            lane = member.lane,
            signal = signal,
            detail = detail.take(240),
            note = note.take(300),
            haunt = hauntMode.name,
            manual = manual,
            quiet = quiet,
            updatedAt = System.currentTimeMillis(),
        )
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("owner", snapshot.owner)
            .putString("emoji", snapshot.emoji)
            .putInt("accent", snapshot.accent)
            .putString("lane", snapshot.lane)
            .putString("signal", snapshot.signal)
            .putString("detail", snapshot.detail)
            .putString("note", snapshot.note)
            .putString("haunt", snapshot.haunt)
            .putBoolean("manual", snapshot.manual)
            .putBoolean("quiet", snapshot.quiet)
            .putLong("updated_at", snapshot.updatedAt)
            .apply()

        val json = toJson(snapshot).toString()
        writeSnapshotFile(app, json)
        RavenSurfaceIntegrity.mark(
            app,
            RavenSurfaceIntegrity.CANONICAL,
            "SETTLED",
            snapshot.updatedAt,
            "${snapshot.owner}:${snapshot.signal}",
        )

        // Explicit same-package broadcast is the live sync lane for other app processes.
        // This proves dispatch, not wallpaper-JS consumption.
        try {
            app.sendBroadcast(
                Intent(ACTION_CHANGED)
                    .setPackage(app.packageName)
                    .putExtra(EXTRA_JSON, json),
            )
            RavenSurfaceIntegrity.mark(
                app,
                RavenSurfaceIntegrity.WALLPAPER_CHANNEL,
                "DISPATCHED",
                snapshot.updatedAt,
                "same-package office state broadcast",
            )
        } catch (_: Throwable) {
            RavenSurfaceIntegrity.mark(
                app,
                RavenSurfaceIntegrity.WALLPAPER_CHANNEL,
                "DISPATCH_FAILED",
                snapshot.updatedAt,
            )
        }

        RavenWidgetOfficeModule.broadcast(app, snapshot)
        return snapshot
    }

    fun read(context: Context): Snapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val owner = prefs.getString("owner", null) ?: return null
        return Snapshot(
            owner = owner,
            emoji = prefs.getString("emoji", "") ?: "",
            accent = prefs.getInt("accent", 0xFFFF4FA3.toInt()),
            lane = prefs.getString("lane", "") ?: "",
            signal = prefs.getString("signal", "") ?: "",
            detail = prefs.getString("detail", "") ?: "",
            note = prefs.getString("note", "") ?: "",
            haunt = prefs.getString("haunt", RavenHauntMode.HAUNTED.name) ?: RavenHauntMode.HAUNTED.name,
            manual = prefs.getBoolean("manual", false),
            quiet = prefs.getBoolean("quiet", false),
            updatedAt = prefs.getLong("updated_at", 0L),
        )
    }

    fun readSnapshotJson(context: Context): String? = try {
        val file = File(File(context.filesDir, SNAPSHOT_DIR), SNAPSHOT_FILE)
        if (file.isFile) file.readText(Charsets.UTF_8) else null
    } catch (_: Throwable) {
        null
    }

    /** Android Color Ints are AARRGGBB; Office surfaces consume canonical opaque CSS #RRGGBB. */
    fun accentCss(accent: Int): String = String.format("#%06X", accent and 0x00FFFFFF)

    fun toJson(snapshot: Snapshot): JSONObject = JSONObject()
        .put("ok", true)
        .put("owner", snapshot.owner)
        .put("emoji", snapshot.emoji)
        .put("accent", accentCss(snapshot.accent))
        .put("lane", snapshot.lane)
        .put("signal", snapshot.signal)
        .put("detail", snapshot.detail)
        .put("note", snapshot.note)
        .put("haunt", snapshot.haunt)
        .put("manual", snapshot.manual)
        .put("quiet", snapshot.quiet)
        .put("updatedAt", snapshot.updatedAt)

    private fun writeSnapshotFile(context: Context, json: String) {
        try {
            val dir = File(context.filesDir, SNAPSHOT_DIR).also { it.mkdirs() }
            val target = File(dir, SNAPSHOT_FILE)
            val tmp = File(dir, "$SNAPSHOT_FILE.tmp")
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
        } catch (_: Throwable) {
            // Cross-process mirror failure must never break launcher-facing state.
        }
    }
}
