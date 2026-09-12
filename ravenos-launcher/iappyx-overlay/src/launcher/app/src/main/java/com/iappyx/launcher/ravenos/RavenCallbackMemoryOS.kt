package com.iappyx.launcher.ravenos

import android.content.Context
import java.util.ArrayDeque

/**
 * Bounded process-session callback memory for Goblin Vision.
 *
 * Callback law: callbacks must describe a real state evolution, not merely announce that an Android
 * event happened N times. Notification churn is evidence, not a recurring comedy premise.
 *
 * Stores app-loop shapes, soundtrack survival, and rare recursive-meta transitions. It deliberately
 * does not retain transcripts or arbitrary screen text.
 */
object RavenCallbackMemoryOS {
    data class Callback(val text: String, val family: String)

    private val trackCuts = linkedMapOf<String, Int>()
    private val appReturns = linkedMapOf<String, Int>()
    private val appPairs = linkedMapOf<String, Int>()
    private val recentApps = ArrayDeque<String>()
    private var recursiveHits = 0
    private const val MAX_KEYS = 96

    @Synchronized
    fun observe(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
    ): Callback {
        // Notification posted/removed events may update scene state, but do not earn comedy simply
        // by repeating. This prevents stale "third ping / fifth ping" head-count jokes.
        if (marker.key.startsWith("NOTIFICATION")) return Callback("", "")

        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)

        if (marker.key in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC") && field(marker.detail, "meta") == "true") {
            recursiveHits++
            if (recursiveHits == 1) {
                return Callback(
                    text = "The screen is talking about RavenOS while RavenOS is reading the screen. Recursion unlocked.",
                    family = "CALLBACK_META_RECURSION",
                )
            }
        }

        if ((marker.key == "APP_ENTER" || marker.key == "HOME_ENTER") && scene.mediaHot && !scene.mediaTitle.isNullOrBlank()) {
            val track = scene.mediaTitle.take(56)
            val cuts = bump(trackCuts, track.lowercase())
            if (cuts == 5) {
                return Callback(
                    text = "“$track” is still scoring the scene after several foreground changes. It has become part of the room.",
                    family = "CALLBACK_TRACK_SURVIVAL",
                )
            }
            if (cuts == 13) {
                return Callback(
                    text = "“$track” keeps surviving app changes. At this point the phone has a theme song.",
                    family = "CALLBACK_TRACK_SURVIVAL",
                )
            }
        }

        if (marker.key == "APP_ENTER") {
            val app = scene.activeApp?.take(48)
            if (!app.isNullOrBlank()) {
                val returns = bump(appReturns, app.lowercase())
                val loop = observeAppLoop(app)
                if (loop != null) return loop
                if (returns == 5) {
                    return Callback(
                        text = "$app keeps reclaiming foreground. This is becoming an actual working room, not a drive-by visit.",
                        family = "CALLBACK_APP_RETURN",
                    )
                }
                if (returns == 13) {
                    return Callback(
                        text = "$app has become a persistent part of this session. The office may as well keep a chair there.",
                        family = "CALLBACK_APP_RETURN",
                    )
                }
            }
        }

        // No generic occurrence callbacks here. Meaningful screen semantics, visible content, scene
        // changes, and explicit structural returns belong to the dialogue systems above this layer.
        return Callback("", "")
    }

    private fun observeAppLoop(app: String): Callback? {
        if (recentApps.peekLast() != app) {
            recentApps.addLast(app)
            while (recentApps.size > 6) recentApps.removeFirst()
        }
        if (recentApps.size < 3) return null
        val list = recentApps.toList()
        val n = list.size
        val a = list[n - 3]
        val b = list[n - 2]
        val c = list[n - 1]
        if (a != c || a == b) return null
        val pair = listOf(a, b).sorted().joinToString("↔").lowercase()
        val loops = bump(appPairs, pair)
        return if (loops == 3) {
            Callback(
                text = "$a ↔ $b keeps recurring. That route has become part of the scene now.",
                family = "CALLBACK_APP_LOOP",
            )
        } else null
    }

    @Synchronized
    fun clear() {
        trackCuts.clear()
        appReturns.clear()
        appPairs.clear()
        recentApps.clear()
        recursiveHits = 0
    }

    private fun field(detail: String, name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
        .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun bump(map: LinkedHashMap<String, Int>, key: String): Int {
        val next = (map[key] ?: 0) + 1
        map.remove(key)
        map[key] = next
        while (map.size > MAX_KEYS) {
            val oldest = map.entries.firstOrNull()?.key ?: break
            map.remove(oldest)
        }
        return next
    }
}
