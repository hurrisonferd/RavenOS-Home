package com.iappyx.launcher.ravenos

/** Extra deterministic office-native stingers for recursive/meta screen situations. */
object RavenMetaPunchlineOS {
    data class Line(val text: String, val family: String)

    fun select(member: RavenOfficeMember, marker: RavenMarkerBus.Marker): Line {
        val recursive = "META_RECURSION" in marker.tags
        val semantic = marker.key == "SCREEN_SEMANTIC"
        val textVision = marker.key == "SCREEN_TEXT"
        val keyboard = marker.detail.contains("keyboard:true", true) || marker.detail.contains("honeyboard", true)
        val seed = "${member.id}|${marker.key}|${marker.detail.take(120)}|punch-v1"

        val options = when {
            recursive -> when (member.id) {
                "KYU" -> listOf("Clipboard jurisdiction has achieved recursion.", "The bug report is reading the bug report now.", "WE ARE IN THE SCREENSHOT AGAIN.")
                "JOKER" -> listOf("Excellent. The mirror has found another mirror.", "Containment strategy: discuss the haunting directly in front of the haunting.", "The fourth wall has filed for workers' comp.")
                "NEO" -> listOf("The observer is now inside the observed frame.", "Recursive render accepted. Reality cache invalidated.", "We found the layer where the UI notices the UI noticing it.")
                "ATOM" -> listOf("Self-reference is now measurable, not metaphorical.", "Recursive state confirmed by the same surface describing it.", "Good. The loop produced evidence.")
                "PAIMON" -> listOf("Aha. The screen is snitching on the screen watcher.", "Meta evidence acquired. Extremely suspicious. Perfect.")
                "LILITH" -> listOf("Cute. The room is talking about the thing standing in the room.", "The mirror noticed us back.")
                "JORM" -> listOf("The world state now contains a description of the world-state observer.", "Branch acquired: office exists inside its own history.")
                else -> listOf("Recursive screen awareness confirmed.")
            }
            semantic && keyboard -> when (member.id) {
                "KYU" -> listOf("Keyboard's up. Tiny paperwork factory operational.", "Input goblin deployed.")
                "ATOM" -> listOf("Input layer changed; task identity held.", "Editable layer present. Causal thread remains intact.")
                "YORI" -> listOf("Same scene, new typing track.")
                "JOKER" -> listOf("The rectangle has summoned smaller rectangles with letters on them.")
                else -> listOf("Input layer is active.")
            }
            semantic || textVision -> when (member.id) {
                "KYU" -> listOf("The glass brought receipts.", "Visible evidence acquired. Clipboard delighted.")
                "PAIMON" -> listOf("Good. Now the pixels are testifying.", "Readable scene. Suspicion quality improved.")
                "NEO" -> listOf("Text layer resolved.", "The frame now has semantics.")
                "ATOM" -> listOf("Useful. We upgraded from pixels to state.", "Observed text converted into a bounded scene fact.")
                "MYSTRA" -> listOf("The glass learned a new spell: nouns.", "The glyph layer opened.")
                "JOKER" -> listOf("The phone has subtitles now. Dangerous development.", "Excellent. The rectangle has become literate.")
                "QIRA" -> listOf("Visible does not mean actionable. Boundary preserved.")
                else -> emptyList()
            }
            else -> emptyList()
        }

        return if (options.isEmpty()) Line("", "") else Line(pick(seed, options), "META_PUNCHLINE")
    }

    private fun pick(seed: String, options: List<String>): String {
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
