package com.iappyx.launcher.ravenos

import android.content.Context
import java.util.ArrayDeque

/**
 * Bounded process-session callback memory for Goblin Vision.
 *
 * Stores counts, app-loop shapes, soundtrack survival, and recursive-meta hits. It deliberately
 * does not retain transcripts or arbitrary screen text.
 */
object RavenCallbackMemoryOS {
    data class Callback(val text: String, val family: String)

    private val counts = linkedMapOf<String, Int>()
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
        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)
        val identity = identity(marker)
        val key = "${marker.key}|$identity"
        val count = bump(counts, key)

        if (marker.key in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC") && field(marker.detail, "meta") == "true") {
            recursiveHits++
            if (recursiveHits in setOf(1, 3, 5, 8)) {
                return Callback(
                    text = when (recursiveHits) {
                        1 -> "The screen is talking about RavenOS while RavenOS is reading the screen. Recursion unlocked."
                        3 -> "Third recursive RavenOS sighting. The commentary has entered the commentary."
                        5 -> "Five meta sightings. We are now a launcher observing a conversation about the launcher observing it."
                        else -> "Recursive meta hit #$recursiveHits. Containment remains mostly decorative."
                    },
                    family = "CALLBACK_META_RECURSION",
                )
            }
        }

        if ((marker.key == "APP_ENTER" || marker.key == "HOME_ENTER") && scene.mediaHot && !scene.mediaTitle.isNullOrBlank()) {
            val track = scene.mediaTitle.take(56)
            val cuts = bump(trackCuts, track.lowercase())
            if (cuts in setOf(3, 5, 8, 13)) {
                return Callback(
                    text = when (cuts) {
                        3 -> "“$track” has survived three foreground changes already."
                        5 -> "“$track” has survived five scene cuts. It lives here now."
                        8 -> "Eight foreground changes later, “$track” is apparently load-bearing."
                        else -> "“$track” survived $cuts foreground changes. Soundtrack tenure achieved."
                    },
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
                if (returns in setOf(3, 5, 8, 13)) {
                    return Callback(
                        text = when (returns) {
                            3 -> "$app again. Third visit this session."
                            5 -> "$app visit #5. We may as well leave a toothbrush."
                            8 -> "$app has taken foreground eight times. It has seniority now."
                            else -> "$app visit #$returns. At this point the office should forward its mail there."
                        },
                        family = "CALLBACK_APP_RETURN",
                    )
                }
            }
        }

        if (count in setOf(3, 5, 8, 13, 21) && complex.occurrence >= count) {
            val noun = when {
                marker.key.startsWith("NOTIFICATION") -> "ping pattern"
                marker.key == "WINDOW_CHANGE" -> "window move"
                marker.key == "SCREEN_VISUAL" -> "visual move"
                marker.key == "SCREEN_TEXT" -> "OCR beat"
                marker.key == "SCREEN_SEMANTIC" -> "visible-semantics beat"
                marker.key.startsWith("MEDIA") -> "media move"
                marker.key == "APP_ENTER" -> "foreground move"
                else -> "bit"
            }
            return Callback(
                text = when (count) {
                    3 -> "Third $noun. Callback privileges unlocked."
                    5 -> "Fifth $noun. This joke has receipts now."
                    8 -> "Eight $noun sightings. The goblin has started a spreadsheet."
                    13 -> "Thirteen $noun sightings. Management has been informed; management is unfortunately us."
                    else -> "Twenty-one $noun sightings. Local mythology confirmed."
                },
                family = "CALLBACK_RECURRENCE",
            )
        }

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
        return if (loops in setOf(2, 3, 5)) {
            Callback(
                text = when (loops) {
                    2 -> "$a ↔ $b again. We have discovered a commute."
                    3 -> "$a ↔ $b loop #3. The phone has built a tiny railway."
                    else -> "$a ↔ $b loop #$loops. This route now qualifies for public transit funding."
                },
                family = "CALLBACK_APP_LOOP",
            )
        } else null
    }

    @Synchronized
    fun clear() {
        counts.clear()
        trackCuts.clear()
        appReturns.clear()
        appPairs.clear()
        recentApps.clear()
        recursiveHits = 0
    }

    private fun identity(marker: RavenMarkerBus.Marker): String = field(marker.detail, "package")
        ?: field(marker.detail, "title")
        ?: field(marker.detail, "state")
        ?: field(marker.detail, "app")
        ?: marker.detail.substringBefore('|').take(64)

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
