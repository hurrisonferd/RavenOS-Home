package com.iappyx.launcher.ravenos

/** Composes presentation-only meta tail after RavenMetaDialogueRenderer has earned one. */
object RavenMetaDialogueComposer {
    fun compose(
        rendered: RavenMetaDialogueRenderer.Rendered,
        tailOverride: String? = null,
        includeExpression: Boolean = true,
    ): String {
        val tail = tailOverride ?: RavenMetaTailLibrary.pick(rendered.trickId, rendered.hashCode())
        val parts = mutableListOf<String>()
        if (includeExpression && rendered.kaomoji.isNotBlank()) parts += rendered.kaomoji
        if (includeExpression && rendered.ensemble.isNotBlank()) parts += rendered.ensemble
        if (rendered.text.isNotBlank()) parts += rendered.text
        if (tail.isNotBlank()) parts += "⟡ $tail"
        return parts.joinToString(" ")
    }
}
