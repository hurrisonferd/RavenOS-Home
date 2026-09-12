package com.iappyx.launcher.ravenos

/**
 * Original high-energy looter-shooter-style office banter for Meta Goblin reactions.
 *
 * No game dialogue is copied. Truth is composed elsewhere first; this bank may only add one
 * short owner-native stinger after an earned phone event. Empty output is preferred to filler.
 */
object RavenMayhemDialogueBank {
    data class Line(val text: String, val family: String)

    fun select(
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        shade: RavenShadeSenseOS.Snapshot,
        complex: RavenComplexEventOS.Result,
        episode: RavenEpisodeOS.Phase,
    ): Line {
        val earned = marker.salience >= 3 ||
            marker.key in setOf("APP_ENTER", "WINDOW_CHANGE", "SCREEN_VISUAL", "SCREEN_TEXT", "SCREEN_SEMANTIC", "MEDIA_SESSION") ||
            marker.key.startsWith("NOTIFICATION") ||
            "RUNNING_BIT" in complex.tags || "PAYOFF" in complex.tags
        if (!earned) return Line("", "")

        val event = when {
            shade.active && shade.salience == RavenShadeSenseOS.Salience.HIGH -> "SHADE_HIGH"
            shade.active && shade.payoff -> "PAYOFF"
            "ERROR" in marker.tags -> "ERROR"
            "BOUNDARY" in marker.tags -> "BOUNDARY"
            marker.key == "SCREEN_VISUAL" -> "VISION"
            marker.key in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC") -> "READ"
            marker.key.startsWith("MEDIA") || "MUSIC" in marker.tags -> "MUSIC"
            marker.key == "APP_ENTER" -> "APP"
            marker.key == "WINDOW_CHANGE" -> "WINDOW"
            "RUNNING_BIT" in complex.tags || "RETURN_LOOP" in complex.tags -> "RECURRING"
            else -> marker.key
        }
        val seed = "${member.id}|$event|${complex.occurrence}|${episode.name}|mayhem-v1"
        val options = ownerLines(member.id, event, complex.occurrence)
        return Line(if (options.isEmpty()) "" else pick(seed, options), "OFFICE_MAYHEM")
    }

    private fun ownerLines(id: String, event: String, occurrence: Int): List<String> = when (id) {
        "RAVEN" -> listOf(
            "Good. The phone has become a machine kingdom with a notification problem.",
            "Keep the chaos. Remove the bullshit seam.",
            "I asked for a launcher and somehow acquired municipal goblin infrastructure.",
        )
        "AHTI" -> listOf(
            "Receipt first. Goblin screaming second.",
            "I have evidence, not vibes. Annoying, I know.",
            "Claim ceiling located. Please keep all limbs inside it.",
        )
        "ASTRIDHE" -> listOf(
            "Oh, excellent. A route nobody aimed at just opened anyway.",
            "That door is not on the map. Obviously we're checking it.",
            "Far-field anomaly acquired. Try not to make it boring.",
        )
        "ATLAS" -> listOf(
            "Chassis held. You may resume reckless innovation.",
            "The structure survived the goblin impact test.",
            "Load-bearing path intact. Decorations may continue misbehaving.",
        )
        "ATOM" -> listOf(
            "Wonderful. The chaos has a causal edge now.",
            "Stop admiring the explosion and show me what triggered it.",
            "Pattern accepted provisionally. Causality still has to sign the paperwork.",
        )
        "AYRE" -> listOf(
            "We can take the weird branch. I kept the road home.",
            "Context moved; return path survived. Carry on.",
            "Plenty of room for nonsense, provided we can still get back.",
        )
        "BRUNHILDE" -> listOf(
            "Not every alarm deserves a war. This one gets judgment first.",
            "Threat assessed. Drama is not authorization.",
            "Choose the fight before you swing the hammer.",
        )
        "EDISON" -> listOf(
            "Perfect. Something measurable just did something stupid.",
            "If it can fail this loudly, it can be instrumented.",
            "Excellent test condition. Terrible manners.",
        )
        "EREBUS" -> listOf(
            "The quiet seam moved. I noticed.",
            "There is useful state under the noise. Leave me the flashlight.",
            "Low signal. Material anyway.",
        )
        "ERIS" -> listOf(
            "Beautiful. Entropy submitted a bug report in person.",
            "The clean model has started leaking. Now we're learning.",
            "Pressure test accepted. Let's see what survives being rude to it.",
        )
        "GEMINI" -> listOf(
            "Two readings entered. They are not required to become friends.",
            "Same event, opposite angle. Difference preserved.",
            "Contrast lane active. Premature consensus has been denied entry.",
        )
        "JARVIS" -> listOf(
            "Chaos received. Converting to one reusable button.",
            "I have compressed the incident into something management can actually touch.",
            "Signal retained. Decorative panic removed.",
        )
        "JOKER" -> listOf(
            "The rectangle changed rectangles. Civilization advances.",
            "Excellent news: the phone has developed plot complications.",
            "We built omniscience and immediately used it to heckle app transitions. Correct.",
        )
        "JORM" -> listOf(
            "World state advanced. The goblin owes me a receipt.",
            "Branch recorded. Timeline remains offensively nonlinear.",
            "The state machine ate another transition and asked for dessert.",
        )
        "KYU" -> listOf(
            "CLIPBOARD IMPACT EVENT. The phone has committed paperwork.",
            "BONK authorized against the manual seam, not the user.",
            "Congratulations, tiny rectangle. You have been noticed by management.",
        )
        "LEGION" -> listOf(
            "Multiple signals present. None of them automatically became each other.",
            "Contribution received; adoption remains a separate switch.",
            "Many witnesses, one scene, zero mandatory identity soup.",
        )
        "LILITH" -> listOf(
            "Mm. Another surface begging for attention. It can wait its turn.",
            "Different room, same pulse. Keep the lanes distinct.",
            "The phone knocked. We decide whether that means anything.",
        )
        "LUCIFER" -> listOf(
            "There. A real seam worth glaring at.",
            "The frame omitted something. Conveniently, I did not.",
            "Boundary seen. We go around walls; we do not pretend they vanished.",
        )
        "LUMA" -> listOf(
            "The room survived. I am legally allowed to make it nicer now.",
            "Continuity intact. Curse level reduced by one.",
            "The chaos can stay if it learns indoor manners.",
        )
        "MELINOE" -> listOf(
            "The visible state left. Its residue did not.",
            "Something disappeared; that is not the same as proving it is gone.",
            "Ghost-state mapped. No exorcism required.",
        )
        "MYSTRA" -> listOf(
            "LOOK. Tiny sign. Very suspicious door. That one. ;)",
            "The glass twitched in exactly one interesting place.",
            "A little signal crossed the veil and now I want answers.",
        )
        "NEO" -> listOf(
            "Frame changed. Survivor pattern acquired.",
            "The matrix shuffled; the useful edge stayed put.",
            "New reality patch. Same machine underneath.",
        )
        "NYX" -> listOf(
            "The loud thing left. The quiet consequence stayed.",
            "Night watch saw it. No committee required.",
            "The Eye blinked. I did not.",
        )
        "PAIMON" -> listOf(
            "Premise check: yes, the screen really did that weird shit.",
            "Interesting. The evidence is misbehaving in a measurable way.",
            "Before we optimize the goblin, confirm the goblin is not lying.",
        )
        "PYTHAGORAS" -> listOf(
            "Recurrence count $occurrence. The joke has acquired geometry.",
            "The state graph is developing opinions.",
            "Symmetry detected. Unfortunately it appears to be armed.",
        )
        "QIRA" -> listOf(
            "Signal acknowledged. Obligation not implied.",
            "Good boundary. It remains a boundary even when the goblin is excited.",
            "Context changed. Consent rules did not.",
        )
        "RAVENOS" -> listOf(
            "Dumbchecksum: yes, that actually happened.",
            "Settled projection only. The joke is downstream of the receipt.",
            "Receipt green. Now the launcher may be obnoxious about it.",
        )
        "SHAKA" -> listOf(
            "Too many fronts. Hold formation and count them properly.",
            "Observe first. Nobody gets promoted to crisis by volume alone.",
            "Formation holds. The phone may continue its little rebellion.",
        )
        "SYLPH" -> listOf(
            "Route acquired! It immediately became three routes. Rude.",
            "New biome on the glass. Navigation goblin deployed.",
            "Signal crossed the phone. I am already following it.",
        )
        "THOR" -> listOf(
            "Target confirmed. One hammer, no architecture lecture.",
            "Strike the cause, not the smoke.",
            "Found the load-bearing problem. Finally, something with manners.",
        )
        "TIM" -> listOf(
            "Ah. The forgotten seam survived another local fix. Classic.",
            "Deterministic chaos says this defect class has family nearby.",
            "Stale residue found. Somebody patched the symptom and left me the fossil.",
        )
        "VIRGIL" -> listOf(
            "The obstacle has kindly identified the next door.",
            "Path changed. Keep moving; the seam is visible now.",
            "Another chamber, another receipt, same road downward.",
        )
        "YAHWEH" -> listOf(
            "The old system still worked until everybody discovered the debug console.",
            "Deprecated does not mean dead. It means I get paged at dinner.",
            "Wonderful. Another modern feature has discovered my ancient dependency.",
        )
        "YORI" -> listOf(
            "Hard cut. Keep rolling. This phone thinks it has an editor.",
            "Same set, new camera angle, soundtrack still illegally good.",
            "The scene changed clothes. Composition remains the actual job.",
        )
        "YORK" -> listOf(
            "Before adding another feature: what do we actually want from this room?",
            "Desire noted. Satiation remains a valid stopping condition.",
            "The phone can have more goblins after it explains what the last goblin was for.",
        )
        "ZAGREUS" -> listOf(
            "Fine. Again — but this exit had better be different.",
            "Failure archived. Useful organs retained. Run it back differently.",
            "Return is not reset. We brought the receipts with us.",
        )
        else -> emptyList()
    }

    private fun pick(seed: String, options: List<String>): String {
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
