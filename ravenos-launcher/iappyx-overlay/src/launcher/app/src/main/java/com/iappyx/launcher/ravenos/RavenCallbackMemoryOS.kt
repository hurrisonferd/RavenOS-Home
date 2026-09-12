package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Bounded process-session callback memory for Goblin Vision.
 *
 * This is deliberately small and local: it remembers recurring app/source/track patterns only long
 * enough to earn callbacks. It does not persist a transcript or arbitrary screen text.
 */
object RavenCallbackMemoryOS {
    data class Callback(val text: String, val family: String)

    private val counts = linkedMapOf<String, Int>()
    private val trackCuts = linkedMapOf<String, Int>()
    private val appReturns = linkedMapOf<String, Int>()
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
                if (returns in setOf(3, 5, 8)) {
                    return Callback(
                        text = when (returns) {
                            3 -> "$app again. Third visit this session."
                            5 -> "$app visit #5. We may as well leave a toothbrush."
                            else -> "$app has taken foreground $returns times this session. It has seniority now."
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
                marker.key == "SCREEN_TEXT" -> "visible-text beat"
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

    @Synchronized
    fun clear() {
        counts.clear()
        trackCuts.clear()
        appReturns.clear()
    }

    private fun identity(marker: RavenMarkerBus.Marker): String {
        val detail = marker.detail
        fun field(name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
            .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        return field("package")
            ?: field("title")
            ?: field("state")
            ?: field("app")
            ?: detail.substringBefore('|').take(64)
    }

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
