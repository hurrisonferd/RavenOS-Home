package com.iappyx.launcher.ravenos

/**
 * Deterministic mobile dialogue subset.
 * No random selection, no model call, silence remains valid, and text never grants authority.
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
        val owner = member.id
        val n = complex.occurrence
        val myth = when {
            n >= 34 -> "HISTORIC LANDMARK"
            n >= 21 -> "LOCAL MYTHOLOGY"
            n >= 13 -> "MANAGEMENT"
            n >= 8 -> "TENANT"
            n >= 5 -> "EMPLOYEE"
            n >= 3 -> "RUNNING BIT"
            n >= 2 -> "RECURRING"
            else -> "INCIDENT"
        }

        val text = when (owner) {
            "KYU" -> when {
                visual.state == "BONK" && n >= 34 -> "BONK. This bug is a historic landmark now."
                visual.state == "BONK" && n >= 13 -> "BONK. Apparently this problem is management."
                visual.state == "BONK" && n >= 8 -> "BONK. It pays rent now."
                visual.state == "BONK" && n >= 5 -> "BONK. This bug has tenure."
                visual.state == "BONK" && n >= 3 -> "BONK. Clipboard Court has reconvened."
                visual.state == "BONK" -> "BONK. That's evidence."
                "SUCCESS" in marker.tags -> "ON IT. That one moved."
                episode == RavenEpisodeOS.Phase.CHAOS_PEAK -> "Okay who scheduled every department for the same minute?"
                else -> "Let's go. Make the next move visible."
            }
            "PAIMON" -> when {
                visual.state == "I_SEE_IT" && n >= 3 -> "I see it. Same pattern, occurrence $n."
                visual.state == "EXACTLY" -> "Exactly. Evidence survived comparison."
                visual.state == "BIG_BRAIN" -> "Big brain moment: the recurrence is now the data."
                visual.state == "SUS" -> "Sus. Check the premise before optimizing it."
                else -> "Hmm. One clean question first."
            }
            "LUMA" -> when {
                visual.state == "HOME" -> "Home. Make the settled truth easier to inhabit."
                visual.state == "COMFY" -> "Comfy. Restore before expanding."
                visual.state == "ITS_OKAY" -> "It's okay. Reduce burden, keep the map honest."
                visual.state == "BEAUTIFUL" -> "Beautiful. Keep the room this easy to live in."
                else -> "You got this. One gentle move."
            }
            "SYLPH" -> when {
                visual.state == "ZOOM" -> "ZOOM. That route is officially a trail now."
                visual.state == "CURIOUS" -> "Curious... same path, new evidence."
                visual.state == "IDEA" -> "Idea. Test the adjacency, then return."
                visual.state == "SO_COOL" -> "So cool. New path confirmed."
                else -> "Let's explore. Follow the signal."
            }
            "QIRA" -> when {
                visual.state == "NO" -> "No. Boundary first."
                visual.state == "BOUNDARIES" -> "Boundaries. Preserve choice."
                visual.state == "SAY_IT" -> "Say it cleanly."
                visual.state == "YES" -> "Yes. Explicit and receipted."
                else -> "Real talk. Consent, proof, reversibility."
            }
            "NYX" -> when {
                visual.state == "SILENCE" -> ""
                visual.state == "REST" -> "Rest. Nothing material needs the floor."
                visual.state == "NOTED" -> "Noted. The ghost came back."
                visual.state == "WATCHING" -> "Watching. Quiet until it matters."
                else -> "Understood."
            }
            else -> when {
                "ERROR" in marker.tags -> "Confirmed anomaly. ${member.signatureNotes.firstOrNull().orEmpty()}"
                "SUCCESS" in marker.tags -> "Confirmed. ${member.signatureNotes.lastOrNull().orEmpty()}"
                "RECURRING" in complex.tags -> "$myth · occurrence $n. ${member.signatureNotes.firstOrNull().orEmpty()}"
                else -> member.signatureNotes.firstOrNull().orEmpty()
            }
        }
        return Line(text.trim(), if (n >= 3) "RUNNING_BIT" else "BASE")
    }
}
