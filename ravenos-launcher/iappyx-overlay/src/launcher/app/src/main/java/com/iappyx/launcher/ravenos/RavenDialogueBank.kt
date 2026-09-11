package com.iappyx.launcher.ravenos

/**
 * Deterministic employee punchlines for whole-phone Goblin Vision.
 *
 * Phone truth is composed by RavenMetaCommentaryOS. This bank is allowed to add one
 * compact character beat when it earns the pixels. Empty output is valid and preferred
 * over filler such as "Noted.".
 */
object RavenDialogueBank {
    data class Line(val text: String, val family: String)

    fun select(
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        visual: RavenVisualAtlas.Visual,
        episode: RavenEpisodeOS.Phase,
    ): Line {
        val seed = "${member.id}|${marker.key}|${marker.detail.substringBefore('|')}|${complex.occurrence}|${visual.state}|${episode.name}"
        val music = "MUSIC" in marker.tags || marker.key.startsWith("MEDIA")
        val vision = "VISION" in marker.tags || marker.key == "SCREEN_VISUAL"
        val notification = marker.key.startsWith("NOTIFICATION")
        val error = "ERROR" in marker.tags
        val boundary = "BOUNDARY" in marker.tags
        val burst = "APP_SWITCH_BURST" in complex.tags
        val recurring = "RUNNING_BIT" in complex.tags || "RETURN_LOOP" in complex.tags
        val payoff = "PAYOFF" in complex.tags || "RECOVERY_ARC" in complex.tags

        val options = when (member.id) {
            "KYU" -> when {
                error -> listOf("BONK.exe", "Clipboard Court reconvened.", "The bug has entered the chat.")
                notification -> listOf("The tray has opinions.", "Ping goblin clocked in.", "Notification weather: annoying with a chance of BONK.")
                burst -> listOf("Tiny task tornado.", "Speedrun behavior detected.")
                recurring -> listOf("This bit pays rent now.", "Recurring goblin acquired.")
                payoff -> listOf("Okay, THAT earned the clipboard stamp.", "Finally. A useful state change.")
                else -> emptyList()
            }
            "PAIMON" -> when {
                vision -> listOf("Pixels did a backflip.", "The screen absolutely flinched.", "Aha. Visual evidence joined the party.")
                error -> listOf("Premise officially suspicious.", "That is weird in a measurable way.")
                burst -> listOf("Task-switch speedrun.", "The foreground is doing parkour.")
                else -> emptyList()
            }
            "LUMA" -> when {
                music -> listOf("The soundtrack came with us.", "Room tone: acquired.", "Okay, this scene has a soundtrack now.")
                payoff -> listOf("That settled nicely.", "Much less cursed.")
                else -> emptyList()
            }
            "SYLPH" -> when {
                vision -> listOf("New scene unlocked.", "ZOOM.exe", "The pixels found a new biome.")
                burst -> listOf("Route acquired. Several of them, apparently.", "The phone chose exploration mode.")
                music -> listOf("Same soundtrack, new room.", "Music survived the jump.")
                else -> emptyList()
            }
            "QIRA" -> when {
                boundary -> listOf("Good boundary. Keep it.", "Permission wall remains a wall.")
                notification -> listOf("Ping seen; choice preserved.", "Signal received. Obligation not implied.")
                else -> emptyList()
            }
            "NYX" -> when {
                vision -> listOf("Quietly haunted.", "The Eye blinked. I didn't.")
                recurring -> listOf("The ghost knows this hallway now.", "Recurring, but not worth a committee.")
                else -> emptyList()
            }
            "YORI" -> when {
                music -> listOf("This track owns the room.", "Soundtrack refuses to leave the scene.", "The phone has background music now. As intended.")
                burst -> listOf("Hard cut. Next scene.", "Editing pace: caffeinated.")
                else -> emptyList()
            }
            "AYRE" -> when {
                burst -> listOf("Still resumable. Barely.", "Several doors, same thread.")
                music -> listOf("The soundtrack kept the thread intact.", "Audio bridge held.")
                else -> emptyList()
            }
            "LILITH" -> when {
                music -> listOf("Different room, same pulse.", "The soundtrack stayed close.")
                burst -> listOf("Mm. Busy little phone.", "Every surface wants attention tonight.")
                notification -> listOf("Someone knocked. We don't have to answer.", "A ping is not a command.")
                else -> emptyList()
            }
            "NEO" -> when {
                vision -> listOf("Frame changed. Reality patch accepted.", "The pixels took the red pill.", "New frame, same machine.")
                burst -> listOf("Foreground matrix reshuffled.", "Context switch confirmed.")
                else -> emptyList()
            }
            "ATOM" -> when {
                error -> listOf("Good. A falsifiable problem.", "Now that is a causal edge.")
                recurring -> listOf("Recurrence upgraded from anecdote to data.", "Pattern earned another sample.")
                else -> emptyList()
            }
            "THOR" -> when {
                payoff -> listOf("Strike landed.", "Good. Next target.")
                error -> listOf("Found the thing to hit.", "There is our target.")
                else -> emptyList()
            }
            "JOKER" -> when {
                burst -> listOf("Five apps enter. One attention span leaves.", "The phone is speedrunning itself.")
                notification -> listOf("Ah yes, a rectangle demands tribute.", "Tiny banner requests audience with the king.")
                vision -> listOf("The wallpaper moved. Clearly sorcery.", "Screen changed. Timeline probably fine.")
                music -> listOf("We have entered the montage.", "Soundtrack means this is legally a scene now.")
                else -> emptyList()
            }
            "ERIS" -> when {
                burst -> listOf("Chaos remains within expected tolerances.", "Beautiful. The task graph has become weather.")
                error -> listOf("Anomaly accepted into evidence.", "Good. Something finally misbehaved honestly.")
                else -> emptyList()
            }
            "MYSTRA" -> when {
                vision -> listOf("The glass changed its spell.", "New visual field acquired.")
                notification -> listOf("A little signal crossed the veil.", "The tray twitched.")
                else -> emptyList()
            }
            "PYTHAGORAS" -> when {
                recurring -> listOf("Recurrence is doing mathematics now.", "Pattern count has consequences.")
                burst -> listOf("Transition density increased.", "The state graph is getting ideas.")
                else -> emptyList()
            }
            "VIRGIL" -> when {
                error -> listOf("The path has declared its obstacle.", "A fault, therefore a direction.")
                vision -> listOf("The scene turns another circle.", "Another frame on the road.")
                else -> emptyList()
            }
            "EREBUS" -> emptyList()
            else -> when {
                error -> listOf("That one earned attention.")
                payoff -> listOf("Useful change.")
                else -> emptyList()
            }
        }

        return Line(if (options.isEmpty()) "" else pick(seed, options), "META_STINGER")
    }

    private fun pick(seed: String, options: List<String>): String {
        if (options.size <= 1) return options.firstOrNull().orEmpty()
        var hash = 0x811C9DC5.toInt()
        for (c in seed) {
            hash = hash xor c.code
            hash *= 16777619
        }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
