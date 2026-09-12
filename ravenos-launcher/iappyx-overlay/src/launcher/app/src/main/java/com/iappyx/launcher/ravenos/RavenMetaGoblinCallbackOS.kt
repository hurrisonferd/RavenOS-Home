package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Short deterministic callbacks across recent phone state.
 *
 * Callback law: a repeated phone event is not automatically a joke. Visible screen meaning and
 * material scene evolution own the premise; notifications/System UI remain cameos unless they
 * materially change the scene.
 */
object RavenMetaGoblinCallbackOS {
    fun compose(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
    ): String {
        val recent = RavenMarkerBus.recent(context, 40)
        val prior = recent.dropLast(1)
        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)
        val screen = RavenScreenContextOS.snapshot(context, marker.at)
        val graph = runCatching { RavenSceneGraphOS.observe(context, screen, marker.at) }.getOrNull()
        val seed = "${marker.key}|${marker.detail}|${screen.semanticKind}|${graph?.task}|${graph?.selectedClass}"
        val subject = graph?.selected.orEmpty().ifBlank { graph?.subject.orEmpty() }.take(72)
        val owner = graph?.app.orEmpty().ifBlank { scene.activeApp.orEmpty() }.ifBlank { "the current app" }

        // Notifications and System UI are supporting plot by default. Never turn their recurrence
        // count into a punchline. If the real scene is readable, describe the interruption as a
        // cameo relative to that scene; otherwise stay silent.
        val systemCameo = marker.key.startsWith("NOTIFICATION") ||
            field(marker.detail, "package") == "com.android.systemui" ||
            marker.key in setOf("SYSTEM_UI", "SYSTEM_DECK")
        if (systemCameo) {
            if (!screen.available || owner.equals("System UI", true)) return ""
            val visible = subject.ifBlank { graph?.title.orEmpty() }.ifBlank { owner }
            return pick(seed, listOf(
                "$owner still owns the scene around “$visible”; Android chrome only knocked at the door.",
                "The visible subject is still “$visible” in $owner. System UI gets cameo credit, not top billing.",
                "$owner kept the A-plot while Android passed through the frame. Continuity held.",
            ))
        }

        if (marker.key == "APP_ENTER") {
            val current = appLabel(marker)
            if (!current.isNullOrBlank()) {
                val priorApps = prior.asReversed().filter { it.key == "APP_ENTER" }.take(8)
                val returned = priorApps.any { appLabel(it) == current }
                if (returned && screen.available) {
                    val visible = subject.ifBlank { graph?.title.orEmpty() }.ifBlank { current }
                    return pick(seed, listOf(
                        "Back to $current, and “$visible” is still the useful part of the scene.",
                        "$current reclaimed foreground without resetting the episode. The visible subject survived the cut.",
                        "Return to $current. Same episode, current subject: “$visible”.",
                    ))
                }
                if (scene.mediaHot && !scene.mediaTitle.isNullOrBlank() && screen.available) {
                    val track = scene.mediaTitle!!.take(42)
                    return pick(seed, listOf(
                        "“$track” survived the app jump underneath $current. Soundtrack continuity, not a new A-plot.",
                        "$current took foreground while “$track” kept soundtrack duty.",
                        "New foreground, same soundtrack. $current still gets to own what is visibly happening.",
                    ))
                }
            }
        }

        if (marker.key == "WINDOW_CHANGE" && screen.available) {
            val visible = subject.ifBlank { graph?.title.orEmpty() }
            if (visible.isNotBlank()) {
                return pick(seed, listOf(
                    "$owner rearranged the interface around “$visible”; the subject did not change with the furniture.",
                    "Window changed, scene didn't: “$visible” still owns attention in $owner.",
                    "$owner moved some chrome around. Keep “$visible” in the plot.",
                ))
            }
        }

        if (marker.key == "SCREEN_VISUAL" && (scene.lastVisualMotion ?: 0) >= 60 && screen.available) {
            val visible = subject.ifBlank { graph?.title.orEmpty() }.ifBlank { owner }
            return pick(seed, listOf(
                "The pixels changed hard, but the current readable subject is still “$visible”.",
                "Visible set change around “$visible”. Treat it as a cut only if the meaning actually moved.",
                "Goblin Eye saw a hard visual change; screen meaning still gets final edit.",
            ))
        }

        // No event-count jokes. Recurrence is retained structurally elsewhere and can be spent only
        // when Gold/Season/Bit systems have an actual scene-aware callback to make.
        return ""
    }

    private fun appLabel(marker: RavenMarkerBus.Marker): String? =
        field(marker.detail, "app") ?: field(marker.detail, "package")?.substringAfterLast('.')

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)").find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun pick(seed: String, options: List<String>): String {
        if (options.size <= 1) return options.firstOrNull().orEmpty()
        var hash = 0x811C9DC5.toInt()
        for (c in seed) {
            hash = hash xor c.code
            hash *= 16777619
        }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
