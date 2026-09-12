package com.iappyx.launcher.ravenos

/**
 * Pure deterministic state helpers for meta dialogue. Storage lives in existing RavenOS organs;
 * this object only derives presentation stages and trick tags from settled state.
 */
object RavenMetaDialogueStateOS {
    fun stageFor(occurrence: Int): String = when {
        occurrence >= 55 -> "INSTITUTIONAL"
        occurrence >= 34 -> "HISTORIC"
        occurrence >= 21 -> "LOCAL_MYTHOLOGY"
        occurrence >= 13 -> "MANAGEMENT"
        occurrence >= 8 -> "SECOND_ORDER"
        occurrence >= 5 -> "TENURED_BIT"
        occurrence >= 3 -> "RUNNING_BIT"
        occurrence >= 2 -> "CALLBACK"
        else -> "SEED"
    }

    fun tags(event: String, motif: String, callback: String, surface: String): Set<String> {
        val e = event.uppercase()
        val out = linkedSetOf<String>()
        if (e.contains("FAIL") || e.contains("ERROR") || e.contains("BLOCK")) out += "proof"
        if (e.contains("SCREEN") || e.contains("USER_PRESENT") || e.contains("LAUNCH")) out += "launcher"
        if (e.contains("NOTIFICATION")) out += "office"
        if (e.contains("POWER") || e.contains("BATTERY")) out += "vehicle"
        if (motif.isNotBlank()) out += "motif"
        if (callback.isNotBlank()) out += "callback"
        if (surface.uppercase().contains("BOARD")) out += "office"
        out += "meta"
        return out
    }

    fun trickIds(occurrence: Int, callback: String, tags: Set<String>): List<String> =
        RavenDialogueTrickDeck.eligible(occurrence, callback, tags).map { it.id }
}
