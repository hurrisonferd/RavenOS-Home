package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Deterministic cross-signal awareness for the resident Meta Goblin.
 *
 * This organ never reads new private content. It only combines already-authorized MarkerBus
 * and PhoneScene facts into one compact human-readable beat before employee comedy runs.
 */
object RavenMetaGoblinDialogueOS {
    data class Beat(val text: String, val family: String)

    fun select(
        context: Context,
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        episode: RavenEpisodeOS.Phase,
    ): Beat {
        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)
        val seed = "${member.id}|${marker.key}|${marker.detail}|${complex.occurrence}|${episode.name}|meta-goblin"
        val active = scene.activeApp
        val track = scene.mediaTitle?.take(42)
        val source = sourceLabel(marker.detail)
        val motion = field(marker.detail, "motion")?.toIntOrNull() ?: scene.lastVisualMotion

        val options: List<String> = when {
            marker.key == "APP_ENTER" && scene.recentSwitches >= 5 && !active.isNullOrBlank() -> listOf(
                "$active is stop #${scene.recentSwitches} in thirty seconds.",
                "Another hard cut: $active is app #${scene.recentSwitches} in thirty seconds.",
                "$active took foreground after ${scene.recentSwitches} quick app changes.",
            )

            marker.key == "APP_ENTER" && scene.mediaHot && !track.isNullOrBlank() && !active.isNullOrBlank() -> listOf(
                "“$track” followed you into $active.",
                "$active changed; “$track” stayed in the scene.",
                "New app, same soundtrack: “$track” survived the cut.",
            )

            marker.key == "WINDOW_CHANGE" && !active.isNullOrBlank() -> listOf(
                "$active changed rooms without changing apps.",
                "Same app, different room inside $active.",
                "$active stayed foreground; its window changed underneath us.",
            )

            marker.key == "SCREEN_VISUAL" && motion != null && motion >= 60 && !active.isNullOrBlank() -> listOf(
                "$active stayed foreground while the pixels hard-cut.",
                "Same $active foreground, completely different visual scene.",
                "Goblin Eye saw $active change costumes without an app switch.",
            )

            marker.key == "SCREEN_VISUAL" && motion != null && motion >= 30 && !active.isNullOrBlank() -> listOf(
                "$active visibly changed without leaving foreground.",
                "Goblin Eye caught the scene moving inside $active.",
                "$active stayed put; the screen did not.",
            )

            marker.key == "NOTIFICATION_POSTED" && scene.notificationBurst >= 3 -> listOf(
                "${source ?: "An app"} brought ${scene.notificationBurst} pings to the door at once.",
                "Notification weather turned into a ${scene.notificationBurst}-ping squall.",
                "${scene.notificationBurst} notifications landed together${source?.let { " from $it" } ?: ""}.",
            )

            marker.key == "NOTIFICATION_POSTED" && !source.isNullOrBlank() && !active.isNullOrBlank() && source != active -> listOf(
                "$source pinged from backstage while $active owns the screen.",
                "$active has foreground; $source just knocked from the notification tray.",
                "$source sent a ping across the room while you're in $active.",
            )

            marker.key == "MEDIA_SESSION" && scene.mediaHot && !track.isNullOrBlank() && !active.isNullOrBlank() -> listOf(
                "“$track” is still scoring the $active scene.",
                "$active has foreground; “$track” has soundtrack duty.",
                "The screen says $active. The soundtrack says “$track”.",
            )

            marker.key == "HOME_ENTER" && scene.mediaHot && !track.isNullOrBlank() -> listOf(
                "Home came back; “$track” never left.",
                "Back at Home with “$track” still carrying the scene.",
                "Launcher returned. Soundtrack continuity survived.",
            )

            complex.occurrence >= 5 && "RUNNING_BIT" in complex.tags -> listOf(
                "This has happened ${complex.occurrence} times. It is officially a recurring bit.",
                "Occurrence ${complex.occurrence}: the phone has promoted this to a running joke.",
                "Same pattern, ${complex.occurrence} sightings. The goblin has receipts now.",
            )

            else -> emptyList()
        }

        if (options.isEmpty()) return Beat("", "")
        return Beat(pick(seed, options), "META_SCENE_COMBO")
    }

    private fun sourceLabel(detail: String): String? =
        field(detail, "app")?.takeIf { it.isNotBlank() }?.take(42)
            ?: field(detail, "package")?.substringAfterLast('.')?.takeIf { it.isNotBlank() }?.take(42)

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
