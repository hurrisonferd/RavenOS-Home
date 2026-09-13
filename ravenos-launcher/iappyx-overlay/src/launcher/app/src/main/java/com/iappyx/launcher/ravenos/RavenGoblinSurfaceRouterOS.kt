package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Canonical cross-surface settlement layer for Goblin Brain.
 *
 * One settled ReactionPacket fans out from here. Surfaces may project the packet, but may not
 * independently invent a second owner/dialogue decision. ReactionStateStore owns generated widgets,
 * Watchlet and Tasker fanout; this router owns the launcher-resident surfaces around that state.
 */
object RavenGoblinSurfaceRouterOS {
    data class Settlement(
        val residentText: String,
        val officeState: RavenOfficeStateStore.Snapshot,
        val surfaces: List<String>,
    )

    fun settle(
        context: Context,
        member: RavenOfficeMember,
        packet: RavenReactionPacket,
        signal: String,
        detail: String,
        hauntMode: RavenHauntMode,
        manual: Boolean,
        quiet: Boolean,
    ): Settlement {
        val app = context.applicationContext
        val spoken = packet.dialogue.trim()
        val observation = packet.authorNote.trim()
        val residentText = spoken.ifBlank { observation }
        val surfaces = mutableListOf<String>()

        // Canonical packet first. This exact packet is what widgets, Watchlet and Tasker consume.
        RavenReactionStateStore.write(app, packet)
        surfaces += listOf("REACTION_STATE", "RAVEN_WIDGET", "WATCHLET", "TASKER_BRIDGE")

        // Office state remains the compact current-state projection used by HOME/wallpaper surfaces.
        val officeState = RavenOfficeStateStore.write(
            app,
            member = member,
            signal = signal,
            detail = detail,
            note = residentText,
            hauntMode = hauntMode,
            manual = manual,
            quiet = quiet,
        )
        surfaces += listOf("OFFICE_STATE", "WALLPAPER_CHANNEL")

        if (spoken.isNotBlank()) {
            RavenOfficeTraceStore.record(app, member, signal, detail, spoken, hauntMode)
            surfaces += "OFFICE_TRACE"
        }

        RavenHomeAura.render(member, hauntMode)
        RavenHomeWhisper.render(member, residentText, signal, detail, hauntMode)
        surfaces += listOf("HOME_AURA", "HOME_WHISPER")

        // The newer Goblin Vision body owns Follow-Me presentation. The old Follow-Me renderer is
        // intentionally hidden so two overlay bodies never compete for the same resident state.
        RavenFollowMeOverlay.hide()
        RavenGoblinVisionOverlay.renderReaction(app, packet, hauntMode)
        surfaces += listOf("FOLLOW_ME_STATE", "GOBLIN_OVERLAY")

        return Settlement(residentText, officeState, surfaces.distinct())
    }
}
