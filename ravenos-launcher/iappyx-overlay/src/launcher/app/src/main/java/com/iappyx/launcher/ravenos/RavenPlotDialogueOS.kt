package com.iappyx.launcher.ravenos

/** Dialogue for collisions between visible A-plot, soundtrack B-plot, and running-bit C-plot. */
object RavenPlotDialogueOS {
    data class Line(val text: String, val family: String)

    fun select(
        member: RavenOfficeMember,
        plot: RavenPlotStackOS.Plot,
        direction: RavenSitcomDirectorOS.Direction,
    ): Line {
        if (!plot.crossover) return Line("", "PLOT_NONE")
        val seed = "${member.id}|${plot.aPlot}|${plot.bPlot}|${plot.cPlot}|${direction.turn}|plot-v1"
        val lines = when (member.id) {
            "YORI" -> listOf(
                "A-plot is ${plot.aPlot}. ${plot.bPlot.ifBlank { plot.cPlot }} is the B-roll. Keep the cut legible.",
                "The phone has an A-plot and a soundtrack now. Someone accidentally hired an editor.",
            )
            "KYU" -> listOf(
                "Clipboard plot audit: A=${plot.aPlot.take(72)}. ${plot.bPlot.ifBlank { plot.cPlot }} may remain employed as supporting cast.",
                "We have multiple plots. Nobody panic; that would create a D-plot and I am out of tabs.",
            )
            "JOKER" -> listOf(
                "Excellent. The phone has developed an A-plot, a B-plot, and enough C-plot bureaucracy to qualify for syndication.",
                "Three plots detected. Continuity has become a zoning problem.",
            )
            "JORM" -> listOf(
                "Primary world state: ${plot.aPlot}. Secondary thread preserved: ${plot.bPlot.ifBlank { plot.cPlot }}. Branches remain distinct.",
                "A-plot and B-plot are concurrent state, not alternate universes. Thank you for attending basic timeline maintenance.",
            )
            "ATOM" -> listOf(
                "Concurrent variables identified: ${plot.aPlot} / ${plot.bPlot.ifBlank { plot.cPlot }}. Good. We can stop blaming one callback for the whole episode.",
                "The scene has orthogonal context now. That is healthier than stuffing every event into one causal bucket.",
            )
            "LUMA" -> listOf(
                "The room can hold ${plot.aPlot} while ${plot.bPlot.ifBlank { plot.cPlot }} keeps the atmosphere. That feels lived in.",
                "A-plot on the glass, B-plot in the air. The phone finally has ambience instead of interruption spam.",
            )
            "LILITH" -> listOf(
                "Keep ${plot.aPlot} in front. ${plot.bPlot.ifBlank { plot.cPlot }} can stay beside it without demanding custody of the room.",
                "Two things can matter at once without fighting for the whole screen. Revolutionary office policy.",
            )
            "YAHWEH" -> listOf(
                "The ancient debug console called this multitasking. Naturally, the goblins renamed it narrative architecture.",
                "A-plot. B-plot. C-plot. We are one lower-third graphic away from a network executive.",
            )
            "LEGION" -> listOf(
                "Multiple plot lines, separate identities, shared scene. Good. Complexity without flattening.",
                "The room contains several threads and has not become a hive mind. Mark the calendar.",
            )
            else -> listOf(
                "A-plot: ${plot.aPlot}. Supporting plot: ${plot.bPlot.ifBlank { plot.cPlot }}. The office can track both without confusing them.",
                "The episode has more than one live thread now. Keep the hierarchy; keep the joke.",
            )
        }
        return Line(lines[stableIndex(seed, lines.size)].take(300), "PLOT_CROSSOVER")
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
