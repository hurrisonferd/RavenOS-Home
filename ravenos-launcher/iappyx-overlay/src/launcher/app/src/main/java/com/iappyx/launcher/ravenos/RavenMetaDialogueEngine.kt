package com.iappyx.launcher.ravenos

/**
 * Unified deterministic meta-expression engine for launcher surfaces.
 * Facts/evidence/text are inputs; this engine only chooses presentation decoration.
 */
object RavenMetaDialogueEngine {
    data class Input(
        val owner: String,
        val event: String,
        val semanticFamily: String,
        val motif: String = "",
        val callback: String = "",
        val occurrence: Int = 1,
        val previousOwner: String = "",
        val surface: String = "PEEK",
        val noveltyLow: Boolean = false,
        val quiet: Boolean = false,
        val launcherCritical: Boolean = false,
        val pressure: RavenOmniRvExpressionBudget.Pressure = RavenOmniRvExpressionBudget.Pressure.NOMINAL
    )

    fun project(input: Input): RavenMetaDialoguePack {
        val stage = RavenMetaDialogueStateOS.stageFor(input.occurrence)
        val selectorInput = RavenExpressionSelectorOS.Input(
            owner=input.owner, event=input.event, semanticFamily=input.semanticFamily,
            motif=input.motif, stage=stage, callback=input.callback, surface=input.surface,
            occurrence=input.occurrence, previousOwner=input.previousOwner, pressure=input.pressure,
            quiet=input.quiet, launcherCritical=input.launcherCritical
        )
        val projection = RavenEmojiKaomojiProjection.project(selectorInput)
        val tags = RavenMetaDialogueStateOS.tags(input.event, input.motif, input.callback, input.surface)
        val tricks = RavenMetaDialogueStateOS.trickIds(input.occurrence, input.callback, tags)
        val seed = stableSeed(input)
        val silence = RavenSilenceGagOS.evaluate(input.event, input.occurrence, input.noveltyLow, input.quiet, seed)
        val trick = if (silence.emit || tricks.isEmpty()) "" else tricks[Math.floorMod(seed, tricks.size)]
        val tail = if (silence.emit) silence.token else RavenMetaTailLibrary.pick(trick, seed)
        val budget = RavenOmniRvExpressionBudget.forPressure(input.pressure, input.quiet, input.launcherCritical)
        val surfacePolicy = RavenExpressionSurfacePolicy.forSurface(input.surface)
        return RavenMetaDialoguePack(
            emoji = if (surfacePolicy.maxGlyphClusters > 0) projection.emoji else "",
            kaomoji = if (surfacePolicy.maxGlyphClusters > 0) projection.kaomoji else "",
            ensemble = if (surfacePolicy.allowEnsemble) projection.ensemble else "",
            metaTail = if (surfacePolicy.allowTail && budget.allowMetaTail) tail else "",
            family = projection.family,
            stage = stage,
            budgetReason = budget.reason
        )
    }

    private fun stableSeed(i: Input): Int = listOf(
        i.owner, i.event, i.semanticFamily, i.motif, i.callback,
        i.occurrence.toString(), i.previousOwner, i.surface
    ).fold(17) { h, v -> 31 * h + v.hashCode() }
}
