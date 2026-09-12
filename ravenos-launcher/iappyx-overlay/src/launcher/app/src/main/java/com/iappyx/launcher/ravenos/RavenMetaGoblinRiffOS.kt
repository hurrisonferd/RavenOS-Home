package com.iappyx.launcher.ravenos

/**
 * Dense deterministic meta commentary for already-earned speaking moments.
 * Never bypasses cadence. It only gives the writers' room a stronger line when the screen is meta,
 * a cameo interrupts the A-plot, a running bit returns, or Raven is visibly working on RavenOS itself.
 */
object RavenMetaGoblinRiffOS {
    data class Line(val text: String, val family: String)

    fun select(
        member: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
        season: RavenOfficeSeasonOS.Memory,
        gold: RavenGoldSitcomTopologyOS.Beat,
        show: RavenMetaMaxShowrunnerOS.Beat,
    ): Line {
        if (!screen.available) return Line("", "META_RIFF_NONE")
        val subject = script.subject.ifBlank { screen.focus }.replace(Regex("\\s+"), " ").trim().take(96)
        val owner = member.id
        val scene = script.sceneOwner.ifBlank { screen.appLabel.orEmpty().ifBlank { "the screen" } }
        val selfReferential = screen.meta || subject.contains("ravenos", true) || subject.contains("goblin", true) ||
            subject.contains("follow-me", true) || subject.contains("sitcom", true) || subject.contains("dialogue", true) ||
            subject.contains("kaomoji", true) || subject.contains("emoji", true)
        val cameo = script.interruption.isNotBlank()
        val returnBeat = script.returned || season.motifReturningAcrossSessions
        val elevated = gold.phase in setOf("CALLBACK", "ESCALATE") || show.level >= 3
        if (!selfReferential && !cameo && !returnBeat && !elevated && direction.turn % 7 != 0) return Line("", "META_RIFF_NONE")

        val form = when {
            selfReferential && show.level >= 4 -> "RECURSIVE"
            selfReferential -> "SELF_REVIEW"
            cameo -> "CAMEO"
            returnBeat -> "RETURN"
            gold.phase == "ESCALATE" -> "ESCALATION"
            gold.phase == "CALLBACK" -> "CALLBACK"
            else -> "ROOM_TONE"
        }
        val base = when (form) {
            "RECURSIVE" -> listOf(
                "$scene is visibly discussing the system currently floating over $scene and reading that discussion. We have achieved editorial recursion without improving anyone's insurance.",
                "The screen is now reviewing the haunted office while the haunted office reviews the review. The fourth wall has submitted a maintenance ticket.",
                "RavenOS is watching evidence about RavenOS watching RavenOS. This is no longer a mirror; it is a meeting with reflective surfaces.",
            )
            "SELF_REVIEW" -> listOf(
                "The current subject is literally “$subject”. The office has become its own focus group and somehow morale increased.",
                "$scene is showing “$subject”. Good: the writers' room has direct access to its own bug report and can no longer blame catering.",
                "We are now commenting on the system that generates the comments. This is either observability or a very small television show.",
            )
            "CAMEO" -> listOf(
                "${script.interruption} barged into frame, but $scene still owns the episode. Guest stars do not get the deed to the building.",
                "Temporary cameo: ${script.interruption}. A-plot remains “$subject”. Somebody tell Android supporting cast means supporting cast.",
                "The interruption changed the costume, not the plot. $scene keeps top billing.",
            )
            "RETURN" -> listOf(
                "We have been here before, but the scene brought receipts this time. “$subject” is a return, not a reset.",
                "Old location, newer continuity. The office remembers enough to skip the pilot episode explanation.",
                "The scene returned across history with context intact. Nobody say ‘fresh start’ unless they want Jorm to throw a ledger.",
            )
            "ESCALATION" -> listOf(
                "Gold has moved this from callback to escalation. The joke now has paperwork, witnesses, and an unnecessary chair at the meeting.",
                "This bit survived long enough to become infrastructure. Congratulations to everyone who failed to kill it early.",
                "The running joke crossed the line from recurring to load-bearing. Facilities has been notified and immediately resigned.",
            )
            "CALLBACK" -> listOf(
                "Callback earned. Not because the app blinked again—because the same meaning came back with continuity attached.",
                "The office recognized its own history instead of hallucinating novelty. Small miracle; somebody protect it from product management.",
                "Same bit, new beat. Repetition has finally learned timing.",
            )
            else -> listOf(
                "$scene still has a coherent subject: “$subject”. The office may now be funny without pretending every rectangle is a plot twist.",
                "Context survived another turn. This is excellent news for everyone except the telemetry department.",
                "The scene still makes sense. Unreasonably strong foundation for a room full of goblins.",
            )
        }
        val tail = when (owner) {
            "KYU" -> listOf("Clipboard approves the scene and denies responsibility for the consequences.", "BONK reserved for later; context currently wins.")
            "JOKER" -> listOf("Containment remains aspirational.", "HR has asked to stop being tagged in recursive incidents.")
            "ATOM" -> listOf("Causal continuity preserved; comedy may proceed.", "Good. Meaning survived transport.")
            "PAIMON" -> listOf("Premise checked. Annoyingly valid.", "Evidence survives inspection.")
            "YORI" -> listOf("Hold the shot. The scene finally knows what it is about.", "Composition intact; do not cut to telemetry.")
            "JORM" -> listOf("World state preserved across the bit.", "Branch ancestry remains intact.")
            "LILITH" -> listOf("Let the scene keep its room.", "Attention can stay where Raven actually put it.")
            "YAHWEH" -> listOf("Apparently this required a civilization-sized debug console.", "The legacy system would have called this ‘paying attention.’")
            "ERIS" -> listOf("Chaos accepted; amnesia rejected.", "The excluded edge still has teeth.")
            "RAVENOS" -> listOf("Dumbchecksum: context survived; goblins authorized.", "Projection settled. Fourth wall remains optional.")
            else -> listOf("Scene held.", "Continuity intact.")
        }
        val seed = listOf(owner, form, subject, direction.turn, season.episode, season.motifLifetimeCount, gold.phase, show.level).joinToString("|")
        val text = "${pick(seed + "|a", base)} ${pick(seed + "|b", tail)}".replace(Regex("\\s+"), " ").trim().take(360)
        return Line(text, "META_RIFF_$form")
    }

    private fun pick(seed: String, options: List<String>): String {
        if (options.size <= 1) return options.firstOrNull().orEmpty()
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
