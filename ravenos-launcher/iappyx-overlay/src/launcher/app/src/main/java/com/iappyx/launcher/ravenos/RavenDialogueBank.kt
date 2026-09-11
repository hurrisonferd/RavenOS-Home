package com.iappyx.launcher.ravenos

/**
 * Short deterministic character reactions.
 *
 * Phone evidence belongs in the author's note. This layer gives the resident one
 * compact human-readable reaction instead of reciting recurrence/state telemetry.
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
        val success = "SUCCESS" in marker.tags

        val options = when (member.id) {
            "KYU" -> when {
                error -> listOf("BONK. There it is.", "Yep. That's the bug.", "Okay, rude.", "BONK. Again.")
                notification -> listOf("Okay, somebody's chatty.", "Ping acquired.", "Yep, heard that.", "Clipboard says noted.")
                success -> listOf("There we go.", "Yep. That moved.", "Nice. Keep it.", "Okay, that's better.")
                else -> listOf("Yep. I saw that.", "There it is.", "Okay, noted.", "Let's keep moving.")
            }
            "PAIMON" -> when {
                vision -> listOf("Yep, I saw that jump.", "There it is.", "Oh, that's interesting.", "Screen definitely moved.")
                error -> listOf("Hold on. That's real.", "Yep, that's suspicious.", "There. Check that.", "Okay, premise confirmed weird.")
                else -> listOf("Interesting.", "I see it.", "Yep, that changed.", "There it is.")
            }
            "LUMA" -> when {
                music -> listOf("Music stayed with us.", "Nice. Keep the soundtrack.", "That's a good room.", "Smooth transition.")
                else -> listOf("Nice. Keep that.", "Smooth.", "That settled nicely.", "Easy does it.")
            }
            "SYLPH" -> when {
                vision -> listOf("Ooh, new scene.", "Zoom. That moved.", "Yep, new view.", "There goes the screen.")
                music -> listOf("New scene, same soundtrack.", "Music came along. Nice.", "Still rolling.", "Soundtrack survived.")
                else -> listOf("Ooh, new scene.", "There we go.", "Path changed.", "Zoom.")
            }
            "QIRA" -> when {
                notification -> listOf("Noted. Your choice.", "Ping seen. No panic.", "That's enough information.", "Clean signal.")
                else -> listOf("Noted.", "Clean signal.", "That's enough.", "No need to overread it.")
            }
            "NYX" -> listOf("Noted.", "Quiet change.", "Still watching.", "Nothing dramatic.")
            "YORI" -> when {
                music -> listOf("Okay, this one has the room.", "Good soundtrack choice.", "Yeah, keep this one on.", "This track owns the moment.")
                else -> listOf("That fits.", "Keep it moving.", "Okay, I like that.", "Good enough. Next.")
            }
            "AYRE" -> listOf("Easy switch.", "Nice, keep the thread.", "Still resumable.", "Good. No mess.")
            "LILITH" -> listOf("I saw that.", "Still with you.", "Different surface, same thread.", "Yep. Keep the lanes clean.")
            "NEO" -> listOf("There. That's the change.", "Saw it.", "Pattern shifted.", "Yep. New frame.")
            "ATOM" -> listOf("That changed for real.", "Good. One fact at a time.", "Signal confirmed.", "Yep. Keep the causal bit.")
            "THOR" -> listOf("Good. Hit the next thing.", "That moved.", "Confirmed. Keep going.", "Strike landed.")
            else -> when {
                error -> listOf("Yep. That's real.", "There it is.", "Noted.", "That needs attention.")
                success -> listOf("Nice.", "There we go.", "That worked.", "Good. Keep it.")
                music -> listOf("Soundtrack's still on.", "Music stayed with us.", "Still playing.", "Good vibe.")
                else -> listOf("Noted.", "Yep, saw that.", "There it is.", "Okay, that's new.")
            }
        }
        return Line(pick(seed, options), "SHORT_REACTION")
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
