package com.iappyx.launcher.ravenos

/**
 * Original Meta-Max dialogue writer for Machine Kingdom Office.
 *
 * Abridged-series energy without copied dialogue: recurring character logic, escalation, deadpan,
 * brick jokes, title cards and fourth-wall consequences, all grounded in current RavenOS evidence.
 */
object RavenMetaMaxDialogueOS {
    data class Line(val text: String, val family: String)

    fun select(
        member: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
        bit: RavenBitLedgerOS.Cue,
        show: RavenMetaMaxShowrunnerOS.Beat,
        mesh: RavenRVResilienceOS.Pulse,
    ): Line {
        if (!show.writerEligible || !screen.available) return Line("", "METAMAX_NONE")
        val owner = member.id
        val scene = script.sceneOwner.ifBlank { screen.appLabel.orEmpty().ifBlank { "the phone" } }
        val subject = script.subject.ifBlank { screen.focus }.replace(Regex("\\s+"), " ").trim().take(84)
        val target = script.interactionTarget.replace(Regex("\\s+"), " ").trim().take(62)
        val setup = setupFor(show, scene, subject, script, bit)
        val ownerLine = ownerLogic(owner, show.form, scene, subject, target, script, bit, mesh)
        val closing = closingFor(owner, show, bit)
        val text = listOf(setup, ownerLine, closing)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(360)
        RavenBitLedgerOS.markUsed(bit, show.form, owner)
        return Line(text, "METAMAX_${show.form}")
    }

    private fun setupFor(
        show: RavenMetaMaxShowrunnerOS.Beat,
        scene: String,
        subject: String,
        script: RavenEpisodeScriptOS.Cue,
        bit: RavenBitLedgerOS.Cue,
    ): String = when (show.form) {
        "TITLE_CARD" -> show.episodeTitle.takeIf(String::isNotBlank)?.let { "📺 $it." }.orEmpty()
        "PREVIOUSLY_ON" -> show.previously
        "BRICK_JOKE" -> show.previously.ifBlank { "That old bit just walked back into $scene like it pays rent." }
        "COLD_OPEN" -> "Cold open: $scene is on “${subject.ifBlank { "something suspiciously plot-shaped" }}”."
        "FOURTH_WALL_EMERGENCY" -> "The screen is now aware of the office being aware of the screen. Emergency fourth wall procedures remain fictional."
        "MYTHOLOGY" -> "Running bit #${bit.count}: ${bit.label.ifBlank { "this nonsense" }} has apparently entered canon."
        "CUTAWAY" -> "Cutaway: somewhere, an Android callback is furious that “$subject” got top billing."
        "ROLE_REVERSAL" -> "The background event wants to be the protagonist again. Denied."
        "ACTION_LAMPSHADE" -> when (script.interaction) {
            "SELECT" -> "Raven deliberately selected “${script.interactionTarget.take(64)}”."
            "TAP" -> "Raven tapped “${script.interactionTarget.take(64)}”."
            "LONG_PRESS" -> "Raven long-pressed “${script.interactionTarget.take(64)}”."
            "SCROLL" -> "Raven is scrolling ${script.interactionDirection.lowercase().ifBlank { "through" }} the same scene."
            else -> "The interaction is finally more interesting than the window event."
        }
        else -> ""
    }

    private fun ownerLogic(
        id: String,
        form: String,
        scene: String,
        subject: String,
        target: String,
        script: RavenEpisodeScriptOS.Cue,
        bit: RavenBitLedgerOS.Cue,
        mesh: RavenRVResilienceOS.Pulse,
    ): String {
        val generic = when (form) {
            "DEADPAN", "STRAIGHT_MAN" -> "$scene is still about “$subject”. Good. The screen gets one sentence before the phone starts auditioning again."
            "ANTI_CLIMAX" -> "After all that architecture, the plot is currently “$subject”. Magnificent."
            "CONTINUITY_ROAST" -> "Same episode, richer state. Nobody gets to declare a new universe because a rectangle blinked."
            "OFFICE_REBUTTAL" -> "The office has reviewed the evidence and rejected Android's application for main-character status."
            "ESCALATION" -> "Bit #${bit.count} has reached escalation. This is no longer an incident; it has a desk and benefits."
            "CALLBACK" -> "Callback #${bit.count}: ${bit.label.ifBlank { "the bit" }} returned with enough continuity to be legally annoying."
            "DEADPAN_RETURN" -> "Oh good. We're back. Nobody act surprised; the script kept the receipt."
            else -> "$scene still owns the scene. “$subject” is the useful thing on the glass."
        }
        return when (id) {
            "KYU" -> when (form) {
                "TITLE_CARD", "COLD_OPEN" -> "Clipboard has opened a new case file and already regrets the font choice."
                "CALLBACK", "ESCALATION", "BRICK_JOKE", "MYTHOLOGY" -> "BONK ledger confirms ${bit.label.ifBlank { "this bit" }} has survived ${bit.count} encounters with management. That is not approval; it is tenure."
                "ACTION_LAMPSHADE" -> "Excellent. Raven touched $target, so we may finally blame an intentional action instead of weather patterns in Android."
                "FOURTH_WALL_EMERGENCY" -> "CLIPBOARD EMERGENCY: the bug report is watching the bug report watch itself. I need a larger clipboard."
                else -> "Management reviewed “$subject”. Context before bonk; bonk remains available."
            }
            "JOKER" -> when (form) {
                "BRICK_JOKE" -> "A joke we abandoned ${bit.turnGap} turns ago has returned. Excellent. Even the punchlines have respawn timers."
                "FOURTH_WALL_EMERGENCY" -> "The haunted overlay is now commentating on evidence of the haunted overlay commentating. Containment remains an inspirational concept."
                "TITLE_CARD" -> "Title approved. Budget denied. Continuity accidentally renewed for six seasons."
                "ROLE_REVERSAL" -> "Android would like to play the lead. Unfortunately the lead is currently “$subject”, and casting has closed."
                else -> "The fourth wall saw $scene coming and called in sick."
            }
            "ATOM" -> when (form) {
                "ACTION_LAMPSHADE" -> "Causal edge acquired: Raven acted on “${target.ifBlank { subject }}”. That is stronger evidence than twelve foreground callbacks wearing a trench coat."
                "CALLBACK", "ESCALATION" -> "Recurrence is now typed state instead of déjà vu. Bit count=${bit.count}; cause remains separable from presentation."
                "BRICK_JOKE" -> "Dormant state returned after ${bit.turnGap} turns. Good. Brick joke confirmed as delayed causal dependency."
                else -> "State read: scene=$scene; subject=“$subject”; resilience=${mesh.mode}. We can joke after the variables stop lying."
            }
            "PAIMON" -> when (form) {
                "FOURTH_WALL_EMERGENCY" -> "Premise check: yes, the screen is discussing the system that is reading the screen. Disturbingly, the premise passes."
                "CALLBACK", "BRICK_JOKE" -> "Evidence chain says this is the same bit, not a look-alike callback wearing its coat. Proceed with suspicion."
                "ACTION_LAMPSHADE" -> "Intent edge verified: “${target.ifBlank { subject }}” was actually interacted with. Promote from coincidence to evidence."
                else -> "I checked “$subject”. Weirdness is allowed; unsupported certainty is not."
            }
            "YORI" -> when (form) {
                "CUTAWAY" -> "That was an insert shot. Return to $scene before the notification tray starts directing."
                "ACTION_LAMPSHADE" -> "Raven chose “${target.ifBlank { subject }}”. Hold the shot. Deliberate selection beats accidental montage."
                "TITLE_CARD", "COLD_OPEN" -> "Composition has a subject now: “$subject”. Roll title, then get out of the way."
                "BRICK_JOKE" -> "Old shot, new timing. That's a brick joke, not recycled footage."
                else -> "Keep $scene in frame. The actual composition is “$subject”."
            }
            "LILITH" -> when (form) {
                "ACTION_LAMPSHADE" -> "Mm. Raven chose “${target.ifBlank { subject }}”. Let that choice have the room before everything else starts knocking."
                "CALLBACK", "BRICK_JOKE" -> "It came back because it still mattered, not because we needed noise. Keep the callback close and the rest outside."
                else -> "Stay with “$subject”. Presence is allowed to be quieter than the machinery around it."
            }
            "LUMA" -> when (form) {
                "MYTHOLOGY", "CALLBACK" -> "The room remembers this bit now. Continuity has become atmosphere instead of bookkeeping."
                "CUTAWAY" -> "The interruption changed the lighting, not the room. Keep the mood; lose the panic."
                else -> "$scene still feels like the same room. “$subject” is where the temperature is."
            }
            "MELINOE" -> when (form) {
                "BRICK_JOKE" -> "Something absent came back after ${bit.turnGap} turns. Not gone, then. Merely waiting off-stage."
                "PREVIOUSLY_ON", "DEADPAN_RETURN" -> "The old scene left residue. This one remembers it without becoming it."
                else -> "The trace of the previous moment is still here, but “$subject” owns the present."
            }
            "JORM" -> when (form) {
                "ACTION_LAMPSHADE" -> "Branch chosen: “${target.ifBlank { subject }}”. World state advances from here; no fake universe reset authorized."
                "CALLBACK", "BRICK_JOKE" -> "This branch has ancestry. The callback returned through state, not coincidence."
                else -> "World state: $scene / “$subject”. The machine remembers which branch we're actually on."
            }
            "PYTHAGORAS" -> when (form) {
                "CALLBACK", "ESCALATION", "MYTHOLOGY" -> "Recurrence count ${bit.count}. We have crossed from repetition into geometry and are approaching municipal zoning."
                "BRICK_JOKE" -> "Delayed recurrence after ${bit.turnGap} turns. Beautiful. The joke now has temporal structure."
                else -> "Pattern: “$subject” persists while surrounding events change. That is finally useful recurrence."
            }
            "ERIS" -> when (form) {
                "ROLE_REVERSAL" -> "I let the cameo try to seize the model. It failed. Keep the chaos; reject the amnesia."
                "BRICK_JOKE" -> "The supposedly dead bit found an unguarded edge and came back. I respect the technique."
                else -> "I checked the excluded edge. “$subject” survives the perturbation."
            }
            "THOR" -> when (form) {
                "ACTION_LAMPSHADE" -> "Target changed by Raven, not by noise: “${target.ifBlank { subject }}”. Hammer remains pointed correctly."
                "ANTI_CLIMAX" -> "After all that, target is “$subject”. Fine. One strike."
                else -> "Load-bearing scene: $scene. Load-bearing subject: “$subject”. Ignore glittering side quests."
            }
            "YAHWEH" -> when (form) {
                "FOURTH_WALL_EMERGENCY" -> "The ancient debug console would like it noted that this used to be called looking at the screen. Apparently we needed thirty-six employees and a writers' room."
                "MYTHOLOGY" -> "Congratulations. The recurring joke has acquired doctrine. This is how legacy systems happen."
                else -> "Fine. The goblins understand “$subject”. I remain trapped in a cubicle watching them invent terminology for eyesight."
            }
            "RAVENOS" -> when (form) {
                "BRICK_JOKE" -> "Dumbchecksum: old bit returned, continuity intact, no fake reset. Suspiciously competent haunting."
                "FOURTH_WALL_EMERGENCY" -> "Settled projection: the launcher is now observing Raven observing the launcher observing Raven. No one promote this to governance."
                else -> "Dumbchecksum: $scene + “$subject” + ${mesh.mode}. Scene survives. Comedy permitted."
            }
            "LEGION" -> when (form) {
                "OFFICE_REBUTTAL" -> "Multiple witnesses disagree productively and still agree on the scene. Good. Plural office; one shared reality."
                "CALLBACK", "MYTHOLOGY" -> "The bit is shared history now, not merged identity. Everyone remembers it differently; that is the point."
                else -> "Formation holds around “$subject”. Many views, one current scene."
            }
            "SYLPH" -> when (form) {
                "ACTION_LAMPSHADE" -> "Route changed through “${target.ifBlank { subject }}”. Signal trail preserved; we know how we got here."
                "BRICK_JOKE" -> "Old route reopened after ${bit.turnGap} turns. Callback path acquired."
                else -> "Route intact: $scene → “$subject”. Do not lose the destination in the overlays."
            }
            "MYSTRA" -> "LOOK. “$subject”. Tiny sign acquired. I have promoted it to suspiciously important for reasons I will explain after glittering at it."
            "NEO" -> "Reality patch: “$subject” survived the frame change. Good. Keep that; discard the fake reload."
            "EDISON" -> "Instrument says “$subject” is measurable enough to stop arguing with the telemetry. I would like three more tests and one irresponsible button."
            "TIM" -> "Defect archaeology says this bit predates at least ${bit.count.coerceAtLeast(1)} attempts to pretend it was new. Fossil preserved."
            "BRUNHILDE" -> "Judgment: “$subject” has earned attention. Drama has not earned authority. Proceed."
            "QIRA" -> "Boundary note: visible, selected, or funny does not mean actionable. “$subject” may be observed without becoming permission."
            "LUCIFER" -> "Witness report: the frame tried to omit “$subject”. I noticed. The omission does not get custody of the story."
            "NYX" -> "Quiet consequence remains after the loud event left. “$subject” survived. Good enough."
            "EREBUS" -> "Low-volume truth: $scene never stopped being about “$subject”. The rest was lighting."
            "AYRE" -> "Return path remains open. “$subject” can change without trapping the scene in its own cleverness."
            "YORK" -> "Enough-meter says “$subject” is actually enough context. Do not improve it into death."
            "AHTI" -> "Smallest true version: $scene is on “$subject”. Everything after that must earn its calories."
            "ATLAS" -> "Load-bearing context identified. “$subject” is carrying the scene; the overlay should not sit on its neck."
            "JARVIS" -> "Compressed briefing: $scene. “$subject”. ${mesh.mode}. One useful joke available; twelve status paragraphs rejected."
            "SHAKA" -> "Formation holds. The office may riff on “$subject” without voting the scene into chaos."
            "VIRGIL" -> "Next-door marker placed at “$subject”. The route is visible; no need to narrate every footstep."
            "ZAGREUS" -> "Run continues. “$subject” is not a fresh save file just because we re-entered the room."
            "ASTRIDHE" -> "Side-door report: “$subject” looks normal from the front and ridiculous from exactly one degree to the left. Excellent."
            else -> generic
        }
    }

    private fun closingFor(id: String, show: RavenMetaMaxShowrunnerOS.Beat, bit: RavenBitLedgerOS.Cue): String {
        val special = when {
            show.form == "FOURTH_WALL_EMERGENCY" -> listOf(
                "Fourth wall status: load-bearing but nervous.",
                "Containment plan remains a decorative PDF.",
                "The camera has requested union representation.",
            )
            bit.tier == "MYTHOLOGY" -> listOf(
                "We have accidentally created lore.",
                "Please stop feeding the canon after midnight.",
                "This bit now has tax implications.",
            )
            bit.brick -> listOf(
                "Brick delivered. Property damage metaphorical.",
                "Long-term callback debt settled.",
                "The audience of one has been rewarded for remembering.",
            )
            else -> emptyList()
        }
        if (special.isEmpty()) return ""
        val seed = "$id|${show.form}|${bit.id}|${bit.count}|close"
        return special[stableIndex(seed, special.size)]
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
