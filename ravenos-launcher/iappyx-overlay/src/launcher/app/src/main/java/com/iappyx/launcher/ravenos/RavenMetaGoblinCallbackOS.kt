package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Short deterministic callbacks across recent phone state.
 *
 * This layer never invents new sensors. It only connects already-authorized markers into
 * small fourth-wall observations so Goblin Vision can remember the scene it is inhabiting.
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
        val now = marker.at
        val seed = "${marker.key}|${marker.detail}|${complex.occurrence}|${scene.activeApp}"

        if (marker.key == "APP_ENTER") {
            val current = appLabel(marker)
            if (!current.isNullOrBlank()) {
                val priorApps = prior.asReversed().filter { it.key == "APP_ENTER" }.take(8)
                val firstSame = priorApps.indexOfFirst { appLabel(it) == current }
                if (firstSame >= 1) {
                    return pick(seed, listOf(
                        "Back to $current. The goblin remembers this hallway.",
                        "$current again. We have officially made a loop.",
                        "Return trip to $current. Same phone, recurring bit.",
                    ))
                }
                if (scene.mediaHot && !scene.mediaTitle.isNullOrBlank()) {
                    return pick(seed, listOf(
                        "“${scene.mediaTitle!!.take(34)}” survived the app jump.",
                        "Same soundtrack, new foreground.",
                        "The music followed us into $current.",
                    ))
                }
                if (scene.recentSwitches >= 5) {
                    return pick(seed, listOf(
                        "${scene.recentSwitches} app changes in 30s. The phone is editing itself.",
                        "Foreground has changed hands ${scene.recentSwitches} times. Tiny montage acquired.",
                        "${scene.recentSwitches} switches. Attention graph currently doing parkour.",
                    ))
                }
                if (scene.goblinEyeActive) {
                    return pick(seed, listOf(
                        "Eye armed; $current just took the screen.",
                        "$current has foreground and the Eye is still awake.",
                        "Goblin Eye followed the handoff into $current.",
                    ))
                }
            }
        }

        if (marker.key == "WINDOW_CHANGE") {
            val pkg = field(marker.detail, "package")
            val sameWindowFamily = prior.count {
                it.key == "WINDOW_CHANGE" &&
                    field(it.detail, "package") == pkg &&
                    now - it.at in 0..45_000L
            }
            if (sameWindowFamily >= 3) {
                val label = field(marker.detail, "app") ?: pkg?.substringAfterLast('.') ?: "This app"
                return pick(seed, listOf(
                    "$label has changed internal surfaces ${sameWindowFamily + 1} times. Busy little room.",
                    "Fourth wall count: ${sameWindowFamily + 1} window shifts inside $label.",
                    "$label keeps rearranging the furniture without leaving the app.",
                ))
            }
        }

        val systemUiDetours = prior.count {
            it.key == "APP_ENTER" && field(it.detail, "package") == "com.android.systemui" && now - it.at in 0..60_000L
        }
        if (marker.key == "APP_ENTER" && field(marker.detail, "package") == "com.android.systemui" && systemUiDetours >= 2) {
            return pick(seed, listOf(
                "System UI detour #${systemUiDetours + 1} this minute. That's a ritual now.",
                "Back in System UI again. Android chrome has tenure.",
                "System UI keeps guest-starring in this episode.",
            ))
        }

        if (marker.key.startsWith("NOTIFICATION") && scene.notificationBurst >= 3) {
            return pick(seed, listOf(
                "The notification tray is attempting a hostile takeover.",
                "Notification weather: crowded.",
                "The tray is now a small percussion section.",
            ))
        }

        if (marker.key == "SCREEN_VISUAL" && (scene.lastVisualMotion ?: 0) >= 60) {
            return pick(seed, listOf(
                "That was not a subtle scene change.",
                "The pixels changed jobs mid-sentence.",
                "Goblin Eye just watched the set get rebuilt.",
            ))
        }

        val recentEventCount = prior.count { now - it.at in 0..20_000L }
        if (recentEventCount >= 8 && complex.occurrence % 3 == 0) {
            return pick(seed, listOf(
                "$recentEventCount phone events in 20s. The office has stopped pretending this is calm.",
                "The phone is generating plot faster than the goblins can invoice it.",
                "This minute has acquired editing velocity.",
            ))
        }
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
