package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Screen-first writer governor.
 *
 * Priority law:
 * VISIBLE SCREEN MEANING > USER INTERACTION > EPISODE CONTINUITY > PHONE/SYSTEM RECURRENCE.
 * Repeated Android pings may remain supporting evidence, but repetition alone does not earn a joke.
 */
object RavenDialoguePriorityOS {
    data class Decision(
        val tier: String,
        val score: Int,
        val screenGrounded: Boolean,
        val interactionGrounded: Boolean,
        val systemSupportingOnly: Boolean,
        val preferScreenWriter: Boolean,
        val allowSeries: Boolean,
        val allowCallbackComedy: Boolean,
        val reason: String,
    )

    fun decide(
        context: Context,
        marker: RavenMarkerBus.Marker,
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
    ): Decision {
        val graph = runCatching { RavenSceneGraphOS.observe(context, screen, marker.at) }.getOrNull()
        val selected = graph?.selected.orEmpty().isNotBlank()
        val subject = graph?.subject.orEmpty().isNotBlank()
        val task = graph?.task.orEmpty().takeIf { it !in setOf("", "VIEWING") }
        val interaction = graph?.interaction.orEmpty().takeIf { it.isNotBlank() }
        val systemEvent = marker.key.startsWith("NOTIFICATION") ||
            marker.key in setOf("WINDOW_CHANGE", "SYSTEM_DECK", "SYSTEM_UI") ||
            field(marker.detail, "package") == "com.android.systemui"
        val screenSpecific = screen.available && (selected || subject) && screen.confidence >= 28
        val meaningfulInteraction = interaction != null && graph?.available == true
        val continuity = script.returned || script.sceneChanged || script.motif.isNotBlank() || script.callbackEarned

        return when {
            selected && screen.available -> Decision(
                "SCREEN_SELECTED", 100, true, true, false, true, true, true,
                "visible selected item outranks recurrence",
            )
            screenSpecific && screen.meta -> Decision(
                "SCREEN_META", 98, true, meaningfulInteraction, false, true, true, true,
                "visible recursive/meta subject",
            )
            screenSpecific -> Decision(
                "SCREEN_SPECIFIC", 92, true, meaningfulInteraction, false, true, true, true,
                "specific visible subject",
            )
            meaningfulInteraction -> Decision(
                "USER_INTERACTION", 82, graph?.available == true, true, false, graph?.available == true, true, true,
                "owner interaction in current scene",
            )
            screen.available && task != null -> Decision(
                "SCREEN_TASK", 76, true, false, false, true, true, true,
                "visible task without specific subject",
            )
            continuity && !systemEvent -> Decision(
                "EPISODE_CONTINUITY", 62, screen.available, false, false, screen.available, true, true,
                "material scene continuity",
            )
            systemEvent && screen.available -> Decision(
                "SYSTEM_CAMEO", 38, true, false, true, true, false, false,
                "system event is supporting plot under a readable scene",
            )
            systemEvent -> Decision(
                "SYSTEM_SUPPORTING", 18, false, false, true, false, false, false,
                "system event is evidence, not a comedy premise",
            )
            else -> Decision(
                "PHONE_CONTEXT", 44, screen.available, false, false, screen.available, screen.available, false,
                "phone context without recurrence promotion",
            )
        }
    }

    private fun field(detail: String, name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
        .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
}
