package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Late, presentation-only sitcom/meta bus for Goblin Brain.
 * Truth/evidence are already settled before this stage. This organ may decorate, escalate,
 * defer, callback, or deliberately not react; it never changes source facts or effect authority.
 */
object RavenGoblinMetaPipelineOS {
    data class Input(
        val context: Context,
        val owner: String,
        val previousOwner: String,
        val event: String,
        val baseDialogue: String,
        val evidence: String,
        val motif: String,
        val callback: String,
        val occurrence: Int,
        val surface: String,
        val semanticFamily: String,
        val quiet: Boolean,
        val launcherCritical: Boolean,
        val pressure: RavenOmniRvExpressionBudget.Pressure,
        val hauntMode: RavenHauntMode,
        val metaLevel: Int,
        val sceneId: String,
    )

    data class Output(
        val dialogue: String,
        val trickId: String,
        val stage: String,
        val ensemble: String,
        val budgetReason: String,
        val antiRepeatFingerprint: String,
        val tags: Set<String>,
    )

    fun enrich(input: Input): Output {
        if (input.baseDialogue.isBlank()) {
            return Output("", "", RavenMetaDialogueStateOS.stageFor(input.occurrence), "", "silent-base", "", emptySet())
        }

        val rendered = RavenMetaDialogueRenderer.render(
            RavenMetaDialogueRenderer.Frame(
                owner = input.owner,
                event = input.event,
                text = input.baseDialogue,
                evidence = input.evidence,
                motif = input.motif,
                callback = input.callback,
                surface = input.surface,
                occurrence = input.occurrence,
                previousOwner = input.previousOwner,
                semanticFamily = input.semanticFamily,
                quiet = input.quiet,
                launcherCritical = input.launcherCritical,
                pressure = input.pressure,
            )
        )

        val invasive = input.hauntMode.ordinal >= RavenHauntMode.HAUNTED.ordinal
        val metaEarned = !input.quiet && rendered.trickId.isNotBlank() && invasive && when {
            input.metaLevel >= 3 -> true
            input.callback.isNotBlank() -> true
            input.occurrence in setOf(2, 3, 5, 8, 13, 21, 34, 55) -> true
            input.surface.uppercase() == "BOARD_MEETING" -> true
            input.semanticFamily.uppercase() == "META" -> true
            else -> stableIndex("${input.sceneId}|${input.owner}|${rendered.trickId}|${input.occurrence}", 3) == 0
        }

        val choice = if (metaEarned) {
            val candidates = RavenMetaGrammarOS.candidates(
                owner = input.owner,
                trickId = rendered.trickId,
                stage = rendered.stage,
                motif = input.motif,
                seed = stableHash("${input.sceneId}|${input.owner}|${input.event}|${input.occurrence}|${rendered.trickId}"),
                count = 16,
            )
            RavenMetaAntiRepeatOS.choose(
                input.context,
                scope = "${input.owner}_${rendered.trickId}_${input.semanticFamily}",
                seed = stableHash("${input.sceneId}|${rendered.stage}|${input.occurrence}|${input.previousOwner}"),
                candidates = candidates,
            )
        } else RavenMetaAntiRepeatOS.Choice("", "", 0, false)

        val includeExpression = input.surface.uppercase() in setOf("DESK", "BOARD_MEETING") &&
            input.hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal
        val composed = RavenMetaDialogueComposer.compose(
            rendered = rendered,
            tailOverride = choice.text,
            includeExpression = includeExpression,
        ).take(if (input.hauntMode == RavenHauntMode.APOCALYPSE) 900 else 760)

        val tags = linkedSetOf<String>()
        if (rendered.stage.isNotBlank()) tags += "META_STAGE_${safeTag(rendered.stage)}"
        if (rendered.trickId.isNotBlank()) tags += "META_TRICK_${safeTag(rendered.trickId)}"
        if (rendered.ensemble.isNotBlank()) tags += "META_ENSEMBLE"
        if (choice.fingerprint.isNotBlank()) tags += "META_ANTIREPEAT"
        if (choice.repeated) tags += "META_REPEAT_FALLBACK"

        return Output(
            dialogue = composed,
            trickId = rendered.trickId,
            stage = rendered.stage,
            ensemble = rendered.ensemble,
            budgetReason = rendered.budgetReason,
            antiRepeatFingerprint = choice.fingerprint,
            tags = tags,
        )
    }

    private fun safeTag(value: String): String = value.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')

    private fun stableIndex(seed: String, size: Int): Int = if (size <= 1) 0 else Math.floorMod(stableHash(seed), size)

    private fun stableHash(value: String): Int {
        var h = 17
        value.forEach { h = 31 * h + it.code }
        return h
    }
}
