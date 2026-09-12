package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android adaptation of canonical Gold Council sitcom topology.
 *
 * Gold laws: topology not chatter, targeted crosstalk over parallel monologue, callbacks over random
 * novelty, silence valid. RavenOS refinement: recurrence is not callback material by itself; Gold
 * callbacks must be attached to visible meaning, owner interaction, or a real structural return.
 */
object RavenGoldSitcomTopologyOS {
    data class Beat(
        val phase: String,
        val primary: RavenOfficeMember,
        val secondary: RavenOfficeMember?,
        val crosstalkEligible: Boolean,
        val synthesisEligible: Boolean,
        val terminal: Boolean,
        val authorNote: String,
        val dumbchecksum: String,
        val reason: String,
    )

    private const val PAIR_COOLDOWN_TURNS = 3
    private const val PARTNER_COOLDOWN_TURNS = 2
    private val pairLastTurn = mutableMapOf<String, Int>()
    private val ownerLastPartner = mutableMapOf<String, String>()
    private val ownerLastPartnerTurn = mutableMapOf<String, Int>()

    @Synchronized
    fun direct(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
        memory: RavenOfficeSeasonOS.Memory,
        backstage: RavenBackstageOS.Cue? = null,
    ): Beat {
        val terminal = terminal(marker)
        val systemSupporting = isSystemSupporting(marker)
        val motifIsCameo = script.motif.startsWith("CAMEO_") || script.motif == "CALLBACK_ABOUT_CALLBACKS"
        val structuralReturn = memory.motifReturningAcrossSessions && script.motif.isNotBlank() && !motifIsCameo
        val callbackEarned = when {
            screen.meta && script.callbackEarned -> true
            script.interactionWorthSpeaking && script.callbackEarned -> true
            structuralReturn -> true
            direction.beat == "CALLBACK" && !systemSupporting && screen.available -> true
            else -> false
        }
        val phase = when {
            terminal -> "CLOSE"
            "ERROR" in marker.tags || "PAYOFF" in complex.tags || "BOUNDARY" in marker.tags -> "ESCALATE"
            callbackEarned -> "CALLBACK"
            direction.sceneChanged || script.sceneChanged -> "OPEN"
            else -> "BUILD"
        }
        val backstagePreferred = backstage?.candidate != null && backstage.pressure >= 9
        val candidate = when {
            backstagePreferred -> backstage?.candidate
            direction.secondary != null -> direction.secondary
            backstage?.candidate != null -> backstage.candidate
            else -> null
        }
        val pairKey = pairKey(direction.primary.id, candidate?.id)
        val lastPairTurn = pairLastTurn[pairKey] ?: -999
        val lastPartner = ownerLastPartner[direction.primary.id].orEmpty()
        val lastPartnerTurn = ownerLastPartnerTurn[direction.primary.id] ?: -999
        val pairCooling = pairKey.isNotBlank() && direction.turn - lastPairTurn < PAIR_COOLDOWN_TURNS
        val partnerCooling = candidate != null && candidate.id == lastPartner && direction.turn - lastPartnerTurn < PARTNER_COOLDOWN_TURNS
        val salience = when {
            screen.meta -> 5
            "ERROR" in marker.tags || "PAYOFF" in complex.tags -> 5
            script.interactionWorthSpeaking -> 4
            phase == "CALLBACK" -> 4
            backstagePreferred && !systemSupporting -> 4
            phase == "OPEN" -> 2
            systemSupporting -> 1
            else -> 3
        }
        val threshold = if (phase == "OPEN") 4 else 3
        val crosstalk = candidate != null && salience >= threshold && !pairCooling && !partnerCooling && !terminal
        val secondary = candidate.takeIf { crosstalk }
        if (secondary != null) {
            pairLastTurn[pairKey] = direction.turn
            ownerLastPartner[direction.primary.id] = secondary.id
            ownerLastPartnerTurn[direction.primary.id] = direction.turn
        }
        val synthesis = terminal || phase in setOf("CALLBACK", "ESCALATE") && direction.turn % 3 == 0
        return Beat(
            phase = phase,
            primary = direction.primary,
            secondary = secondary,
            crosstalkEligible = crosstalk,
            synthesisEligible = synthesis,
            terminal = terminal,
            authorNote = if (terminal) authorNote(marker, script, memory) else "",
            dumbchecksum = if (terminal) checksum(marker, memory) else "",
            reason = when {
                terminal -> "gold-close"
                crosstalk && backstagePreferred -> "gold-backstage-crosstalk"
                phase == "CALLBACK" -> "gold-callback"
                phase == "ESCALATE" -> "gold-escalate"
                phase == "OPEN" -> "gold-open"
                systemSupporting -> "gold-supporting-cameo"
                crosstalk -> "gold-build-crosstalk"
                pairCooling -> "gold-pair-cooldown"
                partnerCooling -> "gold-partner-cooldown"
                else -> "gold-build"
            },
        )
    }

    @Synchronized
    fun clear(context: Context) {
        pairLastTurn.clear()
        ownerLastPartner.clear()
        ownerLastPartnerTurn.clear()
    }

    private fun isSystemSupporting(marker: RavenMarkerBus.Marker): Boolean {
        val pkg = field(marker.detail, "package")
        return marker.key.startsWith("NOTIFICATION") || marker.key in setOf("SYSTEM_UI", "SYSTEM_DECK") || pkg == "com.android.systemui"
    }

    private fun terminal(marker: RavenMarkerBus.Marker): Boolean {
        val k = marker.key.uppercase()
        val d = marker.detail.lowercase()
        return k in setOf("CAPTURE_STOP", "STOP_EYES", "RECORDING_STOPPED", "TARGET_CLOSED") ||
            "goblin eye stopped" in d || "recording stopped" in d
    }

    private fun authorNote(marker: RavenMarkerBus.Marker, script: RavenEpisodeScriptOS.Cue, memory: RavenOfficeSeasonOS.Memory): String {
        val scene = script.sceneOwner.ifBlank { "the phone" }
        val stats = RavenGoldEpisodeStatsOS.snapshot()
        return when {
            marker.key.uppercase() in setOf("STOP_EYES", "CAPTURE_STOP") ->
                "🐦‍⬛ RAVENOS — AUTHOR'S NOTE: The eyes closed, but S${memory.season}E${memory.episodeInSeason} keeps its structural receipts. $scene was a scene, not a transcript: ${stats.events} events, ${stats.comments} comments, ${stats.callbacks} callbacks, ${stats.silences} silences, ${stats.crosstalk} targeted cross-talk beats. Observation ends here; history does not pretend otherwise."
            else ->
                "🐦‍⬛ RAVENOS — AUTHOR'S NOTE: This episode settled after ${stats.events} events, ${stats.comments} comments, ${stats.callbacks} callbacks, ${stats.silences} silences and ${stats.windowSwitches} window/app transitions. The screen supplied events; the cast supplied relationships between them."
        }
    }

    private fun checksum(marker: RavenMarkerBus.Marker, memory: RavenOfficeSeasonOS.Memory): String = when (marker.key.uppercase()) {
        "STOP_EYES", "CAPTURE_STOP" -> "🟠 DUMBCHECKSUM: EYES_CLOSED; HISTORY_KEPT; GHOST_GETS_NO_AUTHORITY"
        "RECORDING_STOPPED" -> "🟠 DUMBCHECKSUM: VIDEO_STOPPED; CALLBACKS_REMAIN; NO_MATERIAL_AFTER_CHECKSUM"
        else -> "🟠 DUMBCHECKSUM: S${memory.season}E${memory.episodeInSeason}; CROSSTALK_GT_MONOLOGUE; CALLBACK_GT_RANDOM_NOVELTY"
    }

    private fun field(detail: String, name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
        .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun pairKey(a: String, b: String?): String {
        if (b.isNullOrBlank()) return ""
        return listOf(a.uppercase(), b.uppercase()).sorted().joinToString("_").take(56)
    }
}
