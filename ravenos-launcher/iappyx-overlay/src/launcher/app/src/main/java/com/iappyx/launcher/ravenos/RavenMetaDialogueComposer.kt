package com.iappyx.launcher.ravenos

/** Composes presentation-only meta tail after RavenMetaDialogueRenderer has earned one. */
object RavenMetaDialogueComposer {
    fun compose(rendered: RavenMetaDialogueRenderer.Rendered): String {
        val tail = RavenMetaTailLibrary.pick(rendered.trickId, rendered.hashCode())
        val parts = mutableListOf<String>()
        if (rendered.kaomoji.isNotBlank()) parts += rendered.kaomoji
        if (rendered.ensemble.isNotBlank()) parts += rendered.ensemble
        parts += rendered.text
        if (tail.isNotBlank()) parts += "⟡ $tail"
        return parts.joinToString(" ")
    }
}
