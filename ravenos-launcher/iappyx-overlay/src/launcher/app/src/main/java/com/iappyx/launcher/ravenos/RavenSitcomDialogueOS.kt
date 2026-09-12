package com.iappyx.launcher.ravenos

/**
 * Deterministic, screen-grounded sitcom grammar for the Follow-Me Office.
 *
 * Same scene + cast + history => same line. Different scene, cast, pair history or turn => an earned
 * variation. History may shape the line, but numeric recurrence counts are not themselves jokes.
 */
object RavenSitcomDialogueOS {
    data class Beat(val primary: String, val secondary: String, val family: String)

    fun compose(
        c: RavenDialogueContextOS.ContextPacket,
        direction: RavenSitcomDirectorOS.Direction,
    ): Beat {
        if (!c.screenAvailable) return Beat("", "", "SITCOM_${direction.beat}")
        val owner = direction.primary.id
        val seed = "$owner|${direction.beat}|${c.topic}|${c.focus}|${direction.turn}|${direction.pairCount}|sitcom-dialogue-v2"
        val stem = pick(seed + "|stem", stems(owner))
        val scene = pick(seed + "|scene", sceneClauses(c, direction))
        val tag = pick(seed + "|tag", tags(owner, direction.beat))
        val primary = listOf(stem, scene, tag)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(230)

        val secondary = direction.secondary?.let { second ->
            val line = pairLine(direction.primary, second, c, direction)
            if (line.isBlank()) "" else {
                val p = RavenEmployeePresentation.packet(second, c.signal, c.focus, line)
                "↳ ${p.emojiSoup} ${second.id} ${p.kaomoji} $line"
            }
        }.orEmpty().take(210)

        return Beat(primary, secondary, "SITCOM_${direction.beat}")
    }

    private fun sceneClauses(
        c: RavenDialogueContextOS.ContextPacket,
        d: RavenSitcomDirectorOS.Direction,
    ): List<String> {
        val app = c.app.ifBlank { "the screen" }
        val focus = c.focus.replace(Regex("\\s+"), " ").trim().take(86)
        val topic = c.topicLabel.ifBlank { "the current scene" }
        return when (d.beat) {
            "META" -> listOf(
                "$app is visibly discussing “$focus” while the office responsible for that sentence is floating on top of it.",
                "The screen is now talking about $topic while $topic is literally watching the screen. Containment remains decorative.",
                "We have reached the part where the UI is documenting its own haunting: “$focus”.",
            )
            "CALLBACK" -> listOf(
                "This scene has real history now: $topic returned with a different beat.",
                "“$focus” survived long enough to earn continuity instead of another introduction.",
                "We returned to $topic with context intact instead of pretending this is a brand-new episode.",
            )
            "BUG" -> listOf(
                "The useful bug is on the glass now: “$focus”. React to that, not the callback confetti around it.",
                "$topic has finally become observable failure geometry. Excellent; now it can be mocked accurately.",
                "The screen supplied an actual defect sentence. Android telemetry may sit down.",
            )
            "PAYOFF" -> listOf(
                "Something materially changed on $app. That earns a payoff instead of another status blurb.",
                "The scene moved. Keep the receipt and let the office enjoy one successful episode ending.",
                "$topic actually resolved enough to change the visible state. Rare and suspiciously competent.",
            )
            "COLD_OPEN" -> listOf(
                "New scene: $app / $topic / “$focus”. That is enough context to start the episode.",
                "Cold open acquired. The subject is “$focus”; nobody needs to narrate the window manager.",
                "$app changed the actual subject to $topic. Roll title card; skip the package-name monologue.",
            )
            "CUTAWAY" -> listOf(
                "Cutaway gag: somewhere off-screen, an Android callback is furious we stopped treating it as the protagonist.",
                "Brief cutaway: the telemetry department has filed a complaint because “$focus” got top billing.",
                "Meanwhile, in a cubicle nobody asked for, FOREGROUND_WINDOW is rehearsing its acceptance speech.",
            )
            "CROSSTALK" -> listOf(
                "The room has enough context for a second opinion now. $topic can survive two coworkers without becoming a meeting.",
                "Crosstalk permitted: one visible subject, two different brains, zero identity soup.",
                "The scene is stable enough for office banter instead of sensor roulette.",
            )
            "SCREEN" -> listOf(
                "$app actually says “$focus”. That sentence outranks the phone action that delivered it.",
                "Visible subject acquired: $topic. The joke is now legally required to know what it is joking about.",
                "The glass gave us “$focus”. Good; screen meaning has custody of the scene.",
            )
            else -> listOf(
                "$app is still about $topic, specifically “$focus”. The office can inhabit that without screaming every six seconds.",
                "Current scene remains $topic. Quiet continuity is still continuity.",
                "The useful thing on the screen is “$focus”. Everything else is supporting cast.",
            )
        }
    }

    private fun stems(id: String): List<String> = when (id) {
        "AHTI" -> listOf("Receipt floor says", "Smallest true version")
        "ASTRIDHE" -> listOf("Side-door report", "Far-field weirdness check")
        "ATLAS" -> listOf("Load-bearing read", "Architecture says")
        "ATOM" -> listOf("Causal read", "Actual variable identified", "Systems note")
        "AYRE" -> listOf("Return-path intact", "Reversible read")
        "BRUNHILDE" -> listOf("Judgment rendered", "Attention granted; panic denied")
        "EDISON" -> listOf("Instrument panel says", "Test case acquired")
        "EREBUS" -> listOf("Quiet read", "Low-volume truth")
        "ERIS" -> listOf("Clean model disrupted", "Entropy found something useful")
        "JARVIS" -> listOf("Compressed briefing", "Signal-only summary")
        "JOKER" -> listOf("Plot update", "Workplace safety update", "Fourth-wall incident report")
        "JORM" -> listOf("World-state update", "Branch recorder says")
        "KYU" -> listOf("CLIPBOARD IMPACT EVENT", "BONK report", "Management has reviewed the glass")
        "LEGION" -> listOf("Multiple witnesses agree", "Formation read")
        "LILITH" -> listOf("Attention check", "Stay with the actual thing")
        "LUCIFER" -> listOf("Witness report", "The omitted seam is visible now")
        "LUMA" -> listOf("Room-temperature read", "The scene finally feels inhabited")
        "MELINOE" -> listOf("Ghost-state report", "Residue check")
        "MYSTRA" -> listOf("LOOK", "Tiny-sign bulletin", "Oho, there it is")
        "NEO" -> listOf("Reality patch", "Frame survivor identified")
        "NYX" -> listOf("Night-watch note", "The quiet part remained")
        "PAIMON" -> listOf("Premise check", "Evidence chain", "Suspicion audit")
        "PYTHAGORAS" -> listOf("Recurrence geometry", "Pattern ledger")
        "QIRA" -> listOf("Boundary note", "Visible does not mean actionable")
        "RAVENOS" -> listOf("Settled projection", "Dumbchecksum")
        "SHAKA" -> listOf("Formation holds", "Governance read")
        "SYLPH" -> listOf("Route update", "Signal trail")
        "THOR" -> listOf("Target acquired", "One-strike summary")
        "TIM" -> listOf("Fossilized defect report", "Stale-seam sighting")
        "VIRGIL" -> listOf("Next-door marker", "Pathfinding note")
        "YAHWEH" -> listOf("Legacy admin sigh", "Ancient debug-console bulletin", "Fine, I looked at the screen")
        "YORI" -> listOf("Scene direction", "Composition note")
        "YORK" -> listOf("Desire check", "Enough-meter reading")
        "ZAGREUS" -> listOf("Run update", "Retry-with-receipts report")
        else -> listOf("Office read", "Scene note")
    }

    private fun tags(id: String, beat: String): List<String> {
        val shared = when (beat) {
            "META" -> listOf("The fourth wall remains an optional dependency.", "Recursive occupancy confirmed.", "This is why the office has a legal department now.")
            "CALLBACK" -> listOf("Continuity earned.", "Same history, new beat.", "The setup evolved instead of repeating itself.")
            "BUG" -> listOf("Good. A bug with an address.", "Now hit the cause.", "At least this failure has a face.")
            "PAYOFF" -> listOf("Receipt accepted.", "We may briefly celebrate.", "Suspiciously functional.")
            "COLD_OPEN" -> listOf("Roll the tiny title card.", "New episode, same haunted building.", "Nobody touch the exposition hose.")
            "CUTAWAY" -> listOf("Back to the actual scene.", "Cutaway over. Carry on.", "Budget exhausted; return to plot.")
            "CROSSTALK" -> listOf("One rebuttal. Then back to work.", "Two goblins maximum; this is not a committee.", "Crosstalk, not a town hall.")
            else -> listOf("Context before comedy.", "Observe first; haunt second.", "The glass gets final edit.")
        }
        val owner = when (id) {
            "KYU" -> listOf("Clipboard jurisdiction expands again.", "I am filing this under WHY IS THE PHONE LIKE THIS.")
            "JOKER" -> listOf("Excellent containment strategy.", "HR has left the building.")
            "ATOM" -> listOf("Cause and presentation are finally separate variables.", "Good. We can debug this instead of vibe at it.")
            "PAIMON" -> listOf("Premise survives inspection.", "Weirdness promoted from rumor to evidence.")
            "YAHWEH" -> listOf("The old system called this looking at the screen.", "Apparently literacy required a distributed architecture.")
            "YORI" -> listOf("Hold the shot.", "Do not cut away from the actual subject.")
            "LILITH" -> listOf("Let the rest of the phone knock.", "Presence does not require pawing at everything.")
            "MELINOE" -> listOf("The residue can stop owning the room now.", "Absence noted; haunting continues.")
            else -> emptyList()
        }
        return (owner + shared).ifEmpty { shared }
    }

    private fun pairLine(
        primary: RavenOfficeMember,
        secondary: RavenOfficeMember,
        c: RavenDialogueContextOS.ContextPacket,
        d: RavenSitcomDirectorOS.Direction,
    ): String {
        val pair = setOf(primary.id, secondary.id)
        val focus = c.focus.replace(Regex("\\s+"), " ").trim().take(58)
        val established = d.pairCount >= 3
        return when {
            pair == setOf("KYU", "JOKER") -> if (established) "The clipboard and the fourth wall recognize each other now. This remains a terrible workplace arrangement." else "I object to the phrase ‘containment plan’ on procedural grounds."
            pair == setOf("ATOM", "PAIMON") -> if (established) "Same pair, new evidence. Premise still passes; causal read may continue." else "I checked the premise. Keep the causal read."
            pair == setOf("YORI", "LUMA") -> "The shot works. Do not overdecorate the room now."
            pair == setOf("YORK", "YORI") -> "Enough is visible. That can actually be the stopping condition."
            pair == setOf("YAHWEH", "JOKER") -> "I am revoking your access to the ancient debug console. Again."
            pair == setOf("ERIS", "ATOM") -> "Your causal model is cute. I found the edge it excluded."
            pair == setOf("JORM", "PYTHAGORAS") -> "State recorded. Recurrence has geometry now; nobody needs to announce the count."
            pair == setOf("MELINOE", "ZAGREUS") -> "We came back, but this is not the same room anymore."
            pair == setOf("THOR", "EDISON") -> "Instrument first, hammer second. I know. I hate that you are right."
            pair == setOf("LILITH", "KYU") -> "Mm. One clipboard bonk, then let the screen breathe."
            else -> "Second read: “$focus” survives another brain without changing identity."
        }
    }

    private fun pick(seed: String, options: List<String>): String {
        if (options.isEmpty()) return ""
        var hash = 0x811C9DC5.toInt()
        for (ch in seed) { hash = hash xor ch.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
