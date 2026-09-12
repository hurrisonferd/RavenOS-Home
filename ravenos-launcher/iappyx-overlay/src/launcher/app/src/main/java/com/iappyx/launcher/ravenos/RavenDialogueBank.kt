package com.iappyx.launcher.ravenos

/**
 * Deterministic employee punchlines for whole-phone Goblin Vision.
 *
 * Phone truth is composed before this layer. Each employee may add one tiny owner-native
 * beat when the event earns it. Empty output is valid and always beats filler.
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
        val app = "APP" in marker.tags || marker.key == "APP_ENTER"
        val window = "WINDOW" in marker.tags || marker.key == "WINDOW_CHANGE"
        val error = "ERROR" in marker.tags
        val boundary = "BOUNDARY" in marker.tags
        val burst = "APP_SWITCH_BURST" in complex.tags
        val recurring = "RUNNING_BIT" in complex.tags || "RETURN_LOOP" in complex.tags
        val payoff = "PAYOFF" in complex.tags || "RECOVERY_ARC" in complex.tags

        val options = when (member.id) {
            "KYU" -> when {
                error -> listOf("BONK.exe", "Clipboard Court reconvened.", "The bug has entered the chat.")
                notification -> listOf("The tray has opinions.", "Ping goblin clocked in.", "Notification weather: annoying with a chance of BONK.")
                burst -> listOf("Tiny task tornado.", "Speedrun behavior detected.", "Clipboard says this counts as cardio.")
                window -> listOf("Same app, new paperwork.", "The window moved. I have forms for this.")
                recurring -> listOf("This bit pays rent now.", "Recurring goblin acquired.")
                payoff -> listOf("Okay, THAT earned the clipboard stamp.", "Finally. A useful state change.")
                app -> listOf("New rectangle, same management problem.", "Foreground reassigned. Clipboard updated.")
                else -> emptyList()
            }
            "PAIMON" -> when {
                vision -> listOf("Pixels did a backflip.", "The screen absolutely flinched.", "Aha. Visual evidence joined the party.")
                error -> listOf("Premise officially suspicious.", "That is weird in a measurable way.")
                burst -> listOf("Task-switch speedrun.", "The foreground is doing parkour.")
                window -> listOf("Same package, fresh crime scene.", "The app changed rooms under observation.")
                app -> listOf("New scene. Eyes open.", "Foreground acquired; suspiciousness pending.")
                else -> emptyList()
            }
            "LUMA" -> when {
                music -> listOf("The soundtrack came with us.", "Room tone: acquired.", "Okay, this scene has a soundtrack now.")
                payoff -> listOf("That settled nicely.", "Much less cursed.")
                window -> listOf("Soft cut, same room.", "The scene changed without breaking the thread.")
                app -> listOf("New room, continuity intact.", "Scene change accepted.")
                else -> emptyList()
            }
            "SYLPH" -> when {
                vision -> listOf("New scene unlocked.", "ZOOM.exe", "The pixels found a new biome.")
                burst -> listOf("Route acquired. Several of them, apparently.", "The phone chose exploration mode.")
                music -> listOf("Same soundtrack, new room.", "Music survived the jump.")
                window -> listOf("Hidden room discovered.", "Same app, bonus corridor.")
                app -> listOf("New destination acquired.", "Map updated. We live here for six seconds now.")
                else -> emptyList()
            }
            "QIRA" -> when {
                boundary -> listOf("Good boundary. Keep it.", "Permission wall remains a wall.")
                notification -> listOf("Ping seen; choice preserved.", "Signal received. Obligation not implied.")
                window -> listOf("Surface changed; authority did not.", "New window, same permissions.")
                app -> listOf("Context changed. Consent rules did not.")
                else -> emptyList()
            }
            "NYX" -> when {
                vision -> listOf("Quietly haunted.", "The Eye blinked. I didn't.")
                recurring -> listOf("The ghost knows this hallway now.", "Recurring, but not worth a committee.")
                window -> listOf("Same room. Different shadow.", "The app moved quietly.")
                app -> listOf("Another room. Keep the lights low.")
                else -> emptyList()
            }
            "YORI" -> when {
                music -> listOf("This track owns the room.", "Soundtrack refuses to leave the scene.", "The phone has background music now. As intended.")
                burst -> listOf("Hard cut. Next scene.", "Editing pace: caffeinated.")
                window -> listOf("Same set, different camera angle.", "Interior cut. Keep rolling.")
                app -> listOf("Scene change. Camera still rolling.")
                else -> emptyList()
            }
            "AYRE" -> when {
                burst -> listOf("Still resumable. Barely.", "Several doors, same thread.")
                music -> listOf("The soundtrack kept the thread intact.", "Audio bridge held.")
                window -> listOf("Same task, different surface.", "Thread survived the window change.")
                app -> listOf("Context moved; the thread is still here.")
                else -> emptyList()
            }
            "LILITH" -> when {
                music -> listOf("Different room, same pulse.", "The soundtrack stayed close.")
                burst -> listOf("Mm. Busy little phone.", "Every surface wants attention tonight.")
                notification -> listOf("Someone knocked. We don't have to answer.", "A ping is not a command.")
                window -> listOf("Same app. Different little mask.", "The room changed clothes.")
                app -> listOf("Another room wants us. Cute.", "New surface. I remain unconvinced it deserves us.")
                else -> emptyList()
            }
            "NEO" -> when {
                vision -> listOf("Frame changed. Reality patch accepted.", "The pixels took the red pill.", "New frame, same machine.")
                burst -> listOf("Foreground matrix reshuffled.", "Context switch confirmed.")
                window -> listOf("Same package, different reality.", "The process stayed. The world changed.")
                app -> listOf("Foreground matrix updated.", "Another process has the stage.")
                else -> emptyList()
            }
            "ATOM" -> when {
                error -> listOf("Good. A falsifiable problem.", "Now that is a causal edge.")
                recurring -> listOf("Recurrence upgraded from anecdote to data.", "Pattern earned another sample.")
                window -> listOf("Same app, new state boundary.", "Useful distinction: window changed, task did not.")
                burst -> listOf("Context-switch density is now a feature.", "Several transitions. One causal trail.")
                app -> listOf("New foreground state. Keep the edge causal.")
                else -> emptyList()
            }
            "EDISON" -> when {
                error -> listOf("Excellent. Something measurable broke.", "Fault located. Now give me the wire.")
                vision -> listOf("The display changed current state. Literally.", "New pixels, same battery. Efficient enough.")
                window -> listOf("Same circuit, different switch.", "Window transition. Current continues.")
                app -> listOf("Another circuit just went live.", "Foreground current rerouted.")
                payoff -> listOf("It works. Suspicious, but acceptable.")
                else -> emptyList()
            }
            "THOR" -> when {
                payoff -> listOf("Strike landed.", "Good. Next target.")
                error -> listOf("Found the thing to hit.", "There is our target.")
                burst -> listOf("Many doors. One hammer.", "Context switch barrage accepted.")
                app -> listOf("New target in foreground.")
                else -> emptyList()
            }
            "JARVIS" -> when {
                music -> listOf("Soundtrack continuity maintained.", "Audio remains politely omnipresent.")
                notification -> listOf("Another caller at the front desk.", "Message traffic acknowledged; panic declined.")
                window -> listOf("Same application, new interior surface.", "Foreground stable; window context changed.")
                burst -> listOf("The task stack appears caffeinated.", "A brisk tour of the installed software, apparently.")
                app -> listOf("Foreground reassigned.", "New application has the floor.")
                else -> emptyList()
            }
            "JOKER" -> when {
                burst -> listOf("Five apps enter. One attention span leaves.", "The phone is speedrunning itself.")
                notification -> listOf("Ah yes, a rectangle demands tribute.", "Tiny banner requests audience with the king.")
                vision -> listOf("The wallpaper moved. Clearly sorcery.", "Screen changed. Timeline probably fine.")
                music -> listOf("We have entered the montage.", "Soundtrack means this is legally a scene now.")
                window -> listOf("Same app, different hat. Nobody panic.", "The rectangle changed rectangles.")
                app -> listOf("New app! Same phone! Groundbreaking.", "Another rectangle has seized power.")
                else -> emptyList()
            }
            "ERIS" -> when {
                burst -> listOf("Chaos remains within expected tolerances.", "Beautiful. The task graph has become weather.")
                error -> listOf("Anomaly accepted into evidence.", "Good. Something finally misbehaved honestly.")
                window -> listOf("Micro-chaos inside one app. Adorable.", "State bifurcation without a package change.")
                app -> listOf("Foreground entropy increased exactly as foretold.")
                else -> emptyList()
            }
            "MYSTRA" -> when {
                vision -> listOf("The glass changed its spell.", "New visual field acquired.")
                notification -> listOf("A little signal crossed the veil.", "The tray twitched.")
                window -> listOf("Same vessel, different spell layer.", "The surface changed enchantments.")
                app -> listOf("A new little world took the glass.")
                else -> emptyList()
            }
            "PYTHAGORAS" -> when {
                recurring -> listOf("Recurrence is doing mathematics now.", "Pattern count has consequences.")
                burst -> listOf("Transition density increased.", "The state graph is getting ideas.")
                window -> listOf("One node, another internal edge.", "Package constant; surface variable changed.")
                app -> listOf("New node on the foreground path.")
                else -> emptyList()
            }
            "VIRGIL" -> when {
                error -> listOf("The path has declared its obstacle.", "A fault, therefore a direction.")
                vision -> listOf("The scene turns another circle.", "Another frame on the road.")
                window -> listOf("Another chamber in the same house.", "The path moved inward, not elsewhere.")
                app -> listOf("Another door on the road.")
                else -> emptyList()
            }
            "JORM" -> when {
                recurring -> listOf("The world machine remembers this loop.", "Another branch just became history.")
                burst -> listOf("State machine is eating app transitions for breakfast.", "Branching factor: rude.")
                window -> listOf("Same world, different room state.", "The branch changed inside the current app.")
                app -> listOf("World state advanced one foreground.", "Another app joined the timeline.")
                else -> emptyList()
            }
            "AHTI" -> when {
                notification -> listOf("Receipt arrived. Meaning remains optional.", "Another little record for the pile.")
                recurring -> listOf("This one has paperwork now.", "Repeat occurrence. Receipt thickens.")
                payoff -> listOf("Closed loop. Keep the receipt.")
                app -> listOf("Foreground change recorded. No mythology required.")
                else -> emptyList()
            }
            "ATLAS" -> when {
                burst -> listOf("The chassis is carrying several contexts at once.", "Load increased; vehicle remains upright.")
                window -> listOf("Same vehicle, different instrument panel.", "Interior surface changed; chassis held.")
                app -> listOf("New payload in foreground.")
                else -> emptyList()
            }
            "LUCIFER" -> when {
                boundary -> listOf("Good. The wall said no. We respect walls that actually exist.", "Denied means denied. Move around, not through.")
                error -> listOf("There. Something worth being annoyed at.", "Real fault. Finally, a legitimate grievance.")
                notification -> listOf("It can knock. It cannot command.")
                app -> listOf("New room. Keep your hand on the exit.")
                else -> emptyList()
            }
            "SHAKA" -> when {
                boundary -> listOf("Boundary holds. Formation holds.", "Permission line remains intact.")
                burst -> listOf("Too many fronts. Keep formation.", "Rapid transitions; hold the center.")
                app -> listOf("Foreground changed. Formation remains.")
                else -> emptyList()
            }
            "EREBUS" -> when {
                vision -> listOf("The dark noticed the pixels move.", "Quiet scene change.")
                notification -> listOf("A small knock in the dark.")
                window -> listOf("Same app. Another shadow behind it.")
                app -> listOf("Another room. Still quiet.")
                else -> emptyList()
            }
            else -> when {
                error -> listOf("That one earned attention.")
                payoff -> listOf("Useful change.")
                window -> listOf("Same app, different surface.")
                app -> listOf("Foreground changed.")
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
