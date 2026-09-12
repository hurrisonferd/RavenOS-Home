package com.iappyx.launcher.ravenos

/**
 * Deterministic expression selector. It never decides facts or authority.
 * Callers provide already-settled semantic/context fields.
 */
object RavenExpressionSelectorOS {
    data class Input(
        val owner: String,
        val event: String,
        val semanticFamily: String,
        val motif: String = "",
        val stage: String = "",
        val callback: String = "",
        val surface: String = "PEEK",
        val occurrence: Int = 1,
        val previousOwner: String = "",
        val pressure: RavenOmniRvExpressionBudget.Pressure = RavenOmniRvExpressionBudget.Pressure.NOMINAL,
        val quiet: Boolean = false,
        val launcherCritical: Boolean = false
    )

    data class Output(
        val kaomoji: String,
        val ensemble: String,
        val family: String,
        val budget: RavenOmniRvExpressionBudget.Budget,
        val seed: Int
    )

    fun select(input: Input): Output {
        val budget = RavenOmniRvExpressionBudget.forPressure(input.pressure, input.quiet, input.launcherCritical)
        val family = effectiveFamily(input)
        val seed = stableSeed(input)
        val kaomoji = if (budget.allowKaomoji) RavenKaomojiGrammarOS.pick(input.owner, family, seed) else ""
        val ensemble = if (budget.allowEnsemble && input.previousOwner.isNotBlank() && input.previousOwner != input.owner) {
            RavenEnsembleKaomojiOS.pick(ensembleKind(input), seed xor input.previousOwner.hashCode())
        } else ""
        return Output(kaomoji, ensemble, family, budget, seed)
    }

    private fun effectiveFamily(i: Input): String {
        val e = i.event.uppercase()
        val s = i.stage.uppercase()
        val surface = i.surface.uppercase()
        if (i.quiet) return "WATCH"
        if (surface.contains("BOARD")) return "BOARD_MEETING"
        if (s.contains("MYTH") || s.contains("INSTITUTION") || s.contains("MANAGEMENT")) return "META"
        if (i.callback.isNotBlank()) return "RECOVERY"
        if (e.contains("FAIL") || e.contains("ERROR") || e.contains("BLOCK")) return "FAILURE"
        if (e.contains("SCREEN_OFF") || e.contains("NIGHT")) return "NIGHT_WATCH"
        if (e.contains("AUDIO") || e.contains("MEDIA") || e.contains("HEADSET")) return "MUSIC"
        if (e.contains("PERMISSION") || e.contains("BOUNDARY")) return "BOUNDARY"
        if (e.contains("USER_PRESENT") || e.contains("RETURN") || e.contains("BOOT")) return "DELIGHT"
        return i.semanticFamily.ifBlank { "WATCH" }.uppercase()
    }

    private fun ensembleKind(i: Input): String {
        if (i.quiet) return "SILENT"
        if (i.callback.isNotBlank()) return "HANDOFF"
        if (i.surface.uppercase().contains("BOARD")) return "BOARD"
        if (i.event.uppercase().contains("FAIL") || i.event.uppercase().contains("ERROR")) return "PANIC"
        return if ((stableSeed(i) and 1) == 0) "SIDE_EYE" else "AGREE"
    }

    private fun stableSeed(i: Input): Int {
        var h = 17
        listOf(i.owner, i.event, i.semanticFamily, i.motif, i.stage, i.callback, i.surface, i.occurrence.toString(), i.previousOwner)
            .forEach { h = 31 * h + it.hashCode() }
        return h
    }
}
