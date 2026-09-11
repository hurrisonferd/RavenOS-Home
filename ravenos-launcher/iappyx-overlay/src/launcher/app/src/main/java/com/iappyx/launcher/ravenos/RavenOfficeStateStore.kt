package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Single local current-state snapshot for all RavenOS Office projections.
 *
 * The routing decision is made once by RavenOfficeBarService, then persisted here so Home,
 * Follow-Me, generated surfaces, wallpaper effects, and future bridges can consume the same
 * owner/note/context rather than independently re-routing and drifting apart.
 */
object RavenOfficeStateStore {
    private const val PREFS = "ravenos_office_state_v1"

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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
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

        // Push only the safe/presentation subset to capability-granted generated widgets.
        RavenWidgetOfficeModule.broadcast(context.applicationContext, snapshot)
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
}
