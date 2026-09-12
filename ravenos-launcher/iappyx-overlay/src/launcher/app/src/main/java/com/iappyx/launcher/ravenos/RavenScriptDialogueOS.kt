package com.iappyx.launcher.ravenos

/** Deterministic dialogue that spends episode continuity instead of narrating raw callbacks. */
object RavenScriptDialogueOS {
    data class Line(val text: String, val family: String)

    fun select(
        member: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        cue: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
    ): Line {
        if (!cue.meaningful) return Line("", "SCRIPT_NONE")
        val owner = member.id
        val seed = "$owner|${cue.act}|${cue.sceneOwner}|${cue.task}|${cue.subject}|${cue.interaction}|${cue.interactionTarget}|${cue.motif}|${cue.motifCount}|${direction.turn}|script-v2"
        val anchor = when {
            cue.callbackEarned && cue.callback.isNotBlank() -> cue.callback
            cue.interactionWorthSpeaking && cue.continuity.isNotBlank() -> cue.continuity
            cue.interruption.isNotBlank() && cue.continuity.isNotBlank() -> cue.continuity
            cue.returned && cue.continuity.isNotBlank() -> cue.continuity
            cue.sceneChanged && cue.continuity.isNotBlank() -> cue.continuity
            cue.dwell >= 5 && cue.continuity.isNotBlank() -> cue.continuity
            else -> ""
        }
        if (anchor.isBlank()) return Line("", "SCRIPT_QUIET")

        val punch = pick("$seed|punch", ownerPhrases(owner, cue, screen))
        val line = listOf(anchor, punch)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(280)
        return Line(line, when {
            cue.callbackEarned -> "SCRIPT_CALLBACK"
            cue.interactionWorthSpeaking -> "SCRIPT_INTERACTION_${cue.interaction}"
            cue.interruption.isNotBlank() -> "SCRIPT_CAMEO"
            cue.returned -> "SCRIPT_RETURN"
            cue.sceneChanged -> "SCRIPT_SCENE_CUT"
            else -> "SCRIPT_DWELL"
        })
    }

    private fun ownerPhrases(
        id: String,
        cue: RavenEpisodeScriptOS.Cue,
        screen: RavenScreenContextOS.Snapshot,
    ): List<String> {
        val scene = cue.sceneOwner.ifBlank { screen.appLabel.orEmpty().ifBlank { "the screen" } }
        val target = cue.interactionTarget.take(64).ifBlank { cue.subject.take(64).ifBlank { "that" } }
        val actionPhrases = when (cue.interaction) {
            "SELECT" -> mapOf(
                "KYU" to "Selection logged. Clipboard has promoted “$target” from scenery to plot.",
                "JOKER" to "“$target” has been cast. Negotiations with its agent have failed.",
                "ATOM" to "Selection changed state without changing scene ownership. Correct abstraction.",
                "PAIMON" to "Target verified: “$target”. Premise now has a clickable witness.",
                "YORI" to "Good cut. “$target” entered frame because Raven chose it, not because Android sneezed.",
                "LILITH" to "Mm. “$target” got the attention because Raven gave it the attention.",
                "JORM" to "Branch choice recorded: “$target”. World state continues from here.",
                "PYTHAGORAS" to "Selection has sequence position now. “$target” is not random recurrence.",
                "RAVENOS" to "Dumbchecksum: Raven selected “$target”; the scene remained $scene.",
            )[id]
            "TAP", "LONG_PRESS" -> mapOf(
                "KYU" to "Actual finger event. Finally, a callback with motive.",
                "JOKER" to "Raven touched the rectangle. The rectangle has interpreted this as character development.",
                "ATOM" to "User action distinguished from ambient phone noise. Useful causal edge.",
                "PAIMON" to "Intent evidence improved. This one came from Raven, not weather.",
                "YORI" to "That action belongs in the scene; keep the camera here.",
                "THOR" to "Direct input. Target confirmed. Side quests may remain unhammered.",
            )[id]
            "SCROLL" -> mapOf(
                "KYU" to "This page has become a hallway and Raven is pacing it with purpose.",
                "JOKER" to "The rectangle has more rectangle below it. Television executives are stunned.",
                "ATOM" to "Repeated scroll implies exploration, not scene transition. Keep one context frame.",
                "PAIMON" to "Scroll pattern confirms active inspection rather than accidental foreground time.",
                "YORI" to "Tracking shot. Do not cut just because the viewport moved.",
                "LILITH" to "Still here. Same thing, deeper into it.",
            )[id]
            else -> null
        }
        if (!actionPhrases.isNullOrBlank()) return listOf(actionPhrases)

        return when (id) {
            "KYU" -> listOf(
                "Clipboard continuity says $scene still has top billing.",
                "BONK denied to the cameo; the actual scene gets the joke.",
                "Management has reviewed the episode and rejected random callback inflation.",
            )
            "JOKER" -> listOf(
                "The fourth wall has now been renewed for another season.",
                "Excellent. Even the interruption knows it is supporting cast now.",
                "Continuity has escaped containment and acquired a laugh track.",
            )
            "ATOM" -> listOf(
                "Scene owner, interruption, interaction and callback are finally separate variables.",
                "Good. The state machine remembers what matters instead of merely counting noises.",
                "Causal continuity preserved; incidental callback demoted.",
            )
            "PAIMON" -> listOf(
                "Premise check: the scene survived the interruption.",
                "Evidence chain says the cameo did not become the protagonist.",
                "Continuity passes inspection; callback privileges remain earned, not automatic.",
            )
            "YORI" -> listOf(
                "Keep $scene in frame. The interruption is an insert shot, not a scene change.",
                "Continuity edit approved. Do not cut away from the actual subject.",
                "Composition survived; the cameo gets three seconds and no trailer credit.",
            )
            "LILITH" -> listOf(
                "Mm. $scene still owns the attention. Everything else can knock.",
                "The interruption can wait its turn; presence stayed with the real scene.",
                "Good. We remembered what we were actually doing.",
            )
            "LUMA" -> listOf(
                "The room kept its atmosphere through the interruption.",
                "Continuity intact. The scene did not have to start over to stay alive.",
                "The room remembers its own temperature now.",
            )
            "MELINOE" -> listOf(
                "The residue is history now, not a replacement for the present.",
                "Ghost state separated from current state. Much cleaner haunting.",
                "The previous scene left a trace without possessing this one.",
            )
            "JORM" -> listOf(
                "World state preserved. That was a branch event, not a universe reset.",
                "Scene state persisted across the transition. Excellent; fewer fake timelines.",
                "The branch moved. The world did not forget where it was.",
            )
            "PYTHAGORAS" -> listOf(
                "Recurrence has lineage now instead of mere frequency.",
                "Pattern preserved across the cut; this callback actually has ancestry.",
                "The recurrence survived contact with sequence order.",
            )
            "ERIS" -> listOf(
                "I checked the edge case: the cameo did not get to rewrite the model.",
                "Good. Chaos is allowed; amnesia is not.",
                "The interruption perturbed the scene without replacing its identity.",
            )
            "THOR" -> listOf(
                "Target remains $scene. One strike; ignore the glittering side quest.",
                "The load-bearing scene survived. Hammer still pointed correctly.",
                "Cameo noted. Target unchanged.",
            )
            "YAHWEH" -> listOf(
                "Apparently remembering the previous screen required a writers' room.",
                "The ancient debug console called this continuity.",
                "Fine. The goblins have discovered episode state.",
            )
            "RAVENOS" -> listOf(
                "Dumbchecksum: same episode, richer state, no fake reset.",
                "Settled projection: continuity survived the callback storm.",
                "The scene has memory now. Extremely suspicious launcher behavior.",
            )
            "LEGION" -> listOf(
                "Multiple witnesses kept the same scene without collapsing into one voice.",
                "Formation stayed plural; continuity stayed shared.",
                "The room remembers together without becoming a hive mind.",
            )
            "SYLPH" -> listOf(
                "Route preserved. The soundtrack and scene made it through the cut.",
                "Signal trail intact; we know where this episode came from.",
                "The route survived the interruption without losing the destination.",
            )
            else -> listOf(
                "$scene retained scene ownership.",
                "Continuity held through the interruption.",
                "The office remembered what the phone was actually doing.",
            )
        }
    }

    private fun pick(seed: String, options: List<String>): String {
        if (options.isEmpty()) return ""
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
