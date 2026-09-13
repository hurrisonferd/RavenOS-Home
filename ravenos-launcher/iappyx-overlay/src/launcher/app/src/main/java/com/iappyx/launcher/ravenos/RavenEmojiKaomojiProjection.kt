package com.iappyx.launcher.ravenos

/** Final deterministic symbolic projection helper. */
object RavenEmojiKaomojiProjection {
    data class Projection(val emoji: String, val kaomoji: String, val ensemble: String, val family: String)

    fun project(input: RavenExpressionSelectorOS.Input): Projection {
        val expression = RavenExpressionSelectorOS.select(input)
        val emoji = if (expression.budget.allowEmoji) {
            RavenEmojiReactionBank.pick(input.owner, expression.family, expression.seed)
        } else ""
        return Projection(emoji, expression.kaomoji, expression.ensemble, expression.family)
    }
}
