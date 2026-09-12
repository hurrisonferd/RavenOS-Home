package com.iappyx.launcher.ravenos

import android.content.Context

/** Deterministic sitcom plot stack: visible task, persistent media B-plot, and running-bit C-plot. */
object RavenPlotStackOS {
    data class Plot(
        val aPlot: String,
        val bPlot: String,
        val cPlot: String,
        val crossover: Boolean,
        val score: Int,
    ) {
        val active: Boolean get() = aPlot.isNotBlank()
        fun compact(): String = buildString {
            append("A=").append(aPlot.take(70))
            if (bPlot.isNotBlank()) append(" · B=").append(bPlot.take(70))
            if (cPlot.isNotBlank()) append(" · C=").append(cPlot.take(70))
            if (crossover) append(" · CROSSOVER")
        }
    }

    fun snapshot(
        context: Context,
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        bit: RavenBitLedgerOS.Cue,
    ): Plot {
        val media = RavenMediaSessionSenseOS.snapshot(context)
        val scene = script.sceneOwner.ifBlank { screen.appLabel.orEmpty().ifBlank { screen.semanticKind } }
        val subject = script.subject.ifBlank { screen.focus }.replace(Regex("\\s+"), " ").trim().take(74)
        val a = listOf(scene, script.task.takeIf(String::isNotBlank), subject.takeIf(String::isNotBlank))
            .filterNotNull().filter(String::isNotBlank).joinToString(" · ").take(150)

        val mediaApp = media.packageName.substringAfterLast('.').lowercase()
        val sceneKey = scene.lowercase().replace(" ", "")
        val mediaIsSeparate = media.playing && media.title.isNotBlank() &&
            mediaApp.isNotBlank() && !sceneKey.contains(mediaApp) && !mediaApp.contains(sceneKey.takeIf { it.length >= 3 }.orEmpty())
        val b = when {
            mediaIsSeparate -> "soundtrack: “${media.title.take(64)}”${media.artist.takeIf(String::isNotBlank)?.let { " · ${it.take(34)}" }.orEmpty()}"
            media.playing && screen.semanticKind != "MUSIC" && media.title.isNotBlank() -> "soundtrack: “${media.title.take(64)}”"
            else -> ""
        }

        val c = when {
            bit.active -> "${bit.tier.lowercase().replace('_', ' ')}: ${bit.label.take(68)} ×${bit.count}"
            script.interruption.isNotBlank() -> "cameo: ${script.interruption}"
            else -> ""
        }
        val score = (if (a.isNotBlank()) 1 else 0) + (if (b.isNotBlank()) 1 else 0) + (if (c.isNotBlank()) 1 else 0)
        return Plot(a, b, c, score >= 2, score)
    }
}
