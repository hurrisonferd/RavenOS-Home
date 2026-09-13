package com.iappyx.launcher.ravenos

/**
 * Late presentation decorator. It assumes text/evidence are already settled.
 * It may add posture, ensemble reaction, or one short meta tail, but never alter evidence.
 */
object RavenMetaDialogueRenderer {
    data class Frame(
        val owner: String,
        val event: String,
        val text: String,
        val evidence: String,
        val motif: String = "",
        val callback: String = "",
        val surface: String = "PEEK",
        val occurrence: Int = 1,
        val previousOwner: String = "",
        val semanticFamily: String = "WATCH",
        val quiet: Boolean = false,
        val launcherCritical: Boolean = false,
        val pressure: RavenOmniRvExpressionBudget.Pressure = RavenOmniRvExpressionBudget.Pressure.NOMINAL
    )

    data class Rendered(
        val owner: String,
        val text: String,
        val evidence: String,
        val kaomoji: String,
        val ensemble: String,
        val trickId: String,
        val stage: String,
        val budgetReason: String
    )

    fun render(frame: Frame): Rendered {
        val stage = RavenMetaDialogueStateOS.stageFor(frame.occurrence)
        val tags = RavenMetaDialogueStateOS.tags(frame.event, frame.motif, frame.callback, frame.surface)
        val tricks = RavenMetaDialogueStateOS.trickIds(frame.occurrence, frame.callback, tags)
        val expression = RavenExpressionSelectorOS.select(
            RavenExpressionSelectorOS.Input(
                owner = frame.owner,
                event = frame.event,
                semanticFamily = frame.semanticFamily,
                motif = frame.motif,
                stage = stage,
                callback = frame.callback,
                surface = frame.surface,
                occurrence = frame.occurrence,
                previousOwner = frame.previousOwner,
                pressure = frame.pressure,
                quiet = frame.quiet,
                launcherCritical = frame.launcherCritical
            )
        )
        val trick = if (tricks.isEmpty() || !expression.budget.allowMetaTail) "" else tricks[Math.floorMod(expression.seed, tricks.size)]
        return Rendered(
            owner = frame.owner,
            text = frame.text,
            evidence = frame.evidence,
            kaomoji = expression.kaomoji,
            ensemble = expression.ensemble,
            trickId = trick,
            stage = stage,
            budgetReason = expression.budget.reason
        )
    }
}
