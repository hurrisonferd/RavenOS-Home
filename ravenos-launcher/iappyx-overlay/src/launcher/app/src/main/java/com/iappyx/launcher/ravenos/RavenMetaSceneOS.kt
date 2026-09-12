package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Fuses current phone evidence into one compact scene-level observation before character comedy.
 * It never upgrades evidence authority: OCR text must come from owner-armed Goblin Read and all
 * other facts come from existing local RavenOS sensors.
 */
object RavenMetaSceneOS {
    data class Beat(val text: String, val family: String)

    fun compose(
        context: Context,
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        callback: RavenCallbackMemoryOS.Callback,
    ): Beat {
        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)
        val reading = RavenGoblinReadOS.latest(context, marker.at)
        val app = scene.activeApp?.take(48)
        val track = scene.mediaTitle?.take(48)
        val text = field(marker.detail, "text")?.take(150)
            ?: reading?.takeIf { kotlin.math.abs(marker.at - it.capturedAt) <= 6_000L }?.text?.take(150)
        val seed = "${member.id}|${marker.key}|${marker.detail}|${complex.occurrence}|scene-v1"

        if (callback.text.isNotBlank()) return Beat(callback.text, callback.family)

        val options = when {
            marker.key == "SCREEN_TEXT" && !text.isNullOrBlank() && !app.isNullOrBlank() -> listOf(
                "$app is visibly showing “${focus(text)}”.",
                "Goblin Read caught “${focus(text)}” on $app.",
                "$app put “${focus(text)}” right on the glass.",
                "The screen actually says “${focus(text)}” inside $app.",
            )

            marker.key == "SCREEN_TEXT" && !text.isNullOrBlank() -> listOf(
                "Goblin Read caught “${focus(text)}”.",
                "Visible text says “${focus(text)}”.",
                "The glass currently says “${focus(text)}”.",
            )

            marker.key == "SCREEN_TEXT" && marker.detail.contains("suppressed_sensitive", true) -> listOf(
                "Goblin Read hit a sensitive-looking surface and shut its mouth.",
                "Text vision saw credential-shaped territory and politely looked away.",
                "Sensitive-looking text detected. Goblin Read redacted itself.",
            )

            marker.key == "SCREEN_TEXT" && marker.detail.contains("ocr_unavailable", true) -> listOf(
                "Goblin Read couldn't get a text pass on that frame.",
                "Pixels arrived; text recognition did not.",
            )

            marker.key == "SCREEN_VISUAL" && !app.isNullOrBlank() && scene.mediaHot && !track.isNullOrBlank() && (field(marker.detail, "motion")?.toIntOrNull() ?: 0) >= 45 -> listOf(
                "$app visually hard-cut while “$track” kept scoring the scene.",
                "Big pixel change in $app; “$track” survived untouched.",
                "$app changed what it was showing. “$track” refused to leave the soundtrack department.",
            )

            marker.key == "APP_ENTER" && scene.notificationBurst >= 3 && scene.mediaHot && !track.isNullOrBlank() && !app.isNullOrBlank() -> listOf(
                "$app took foreground under “$track” while ${scene.notificationBurst} pings rattled the tray.",
                "$app has the screen, “$track” has audio, and the notification tray has apparently unionized.",
            )

            marker.key == "WINDOW_CHANGE" && !app.isNullOrBlank() && !text.isNullOrBlank() -> listOf(
                "$app changed internal screens; the visible text now includes “${focus(text)}”.",
                "Same app, new room: “${focus(text)}” is on the glass now.",
            )

            scene.recentSwitches >= 6 && scene.mediaHot && !track.isNullOrBlank() && !app.isNullOrBlank() -> listOf(
                "$app is foreground after ${scene.recentSwitches} recent switches. “$track” somehow survived all of it.",
                "Six-plus context cuts, one soundtrack: “$track” is carrying this entire phone session.",
            )

            else -> emptyList()
        }

        return if (options.isEmpty()) Beat("", "") else Beat(pick(seed, options), "META_SCENE_FUSED")
    }

    private fun focus(text: String): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        val first = clean.split(" · ").firstOrNull { it.length >= 3 } ?: clean
        return first.take(72).trim().trimEnd('.', ',', ':', ';')
    }

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
            .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

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
