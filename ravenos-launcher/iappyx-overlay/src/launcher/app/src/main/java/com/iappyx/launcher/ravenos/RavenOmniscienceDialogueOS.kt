package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Cross-signal Meta Goblin compositor.
 *
 * "Omniscience" is presentation language, not an evidence upgrade. This layer may only combine
 * already-authorized local facts and the privacy-safe Shade Sense projection. It never grants
 * effect authority and never upgrades inference into observation.
 */
object RavenOmniscienceDialogueOS {
    data class Beat(val text: String, val family: String)

    fun select(
        context: Context,
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        shade: RavenShadeSenseOS.Snapshot,
        complex: RavenComplexEventOS.Result,
        episode: RavenEpisodeOS.Phase,
    ): Beat {
        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)
        val active = scene.activeApp?.take(42)
        val track = scene.mediaTitle?.take(42)
        val motion = field(marker.detail, "motion")?.toIntOrNull() ?: scene.lastVisualMotion
        val seed = "${member.id}|${marker.key}|${shade.safeDetail}|${complex.occurrence}|${episode.name}|omniscience-v1"

        val options = when {
            shade.active && shade.payoff && shade.lifetimeMs != null -> listOf(
                "Shade Sense closed the loop after ${formatDuration(shade.lifetimeMs)}. No message content needed.",
                "The notification left after ${formatDuration(shade.lifetimeMs)}; payoff logged without reading the note.",
                "Shade event resolved in ${formatDuration(shade.lifetimeMs)}. Structure was enough.",
            )
            shade.active && shade.salience == RavenShadeSenseOS.Salience.HIGH && shade.conversation -> listOf(
                "Shade Sense sees high-salience conversation traffic${burstSuffix(shade.burst)}. Content stays behind the curtain.",
                "Conversation-shaped alert pressure is high${burstSuffix(shade.burst)}; the office gets geometry, not the message.",
                "The shade is loud in a conversation lane${burstSuffix(shade.burst)}. Nobody here needs the words to notice the weather.",
            )
            shade.active && shade.salience == RavenShadeSenseOS.Salience.HIGH -> listOf(
                "Shade Sense marks this HIGH${burstSuffix(shade.burst)}: Android presentation geometry, zero message quotation.",
                "The notification shade just raised its voice${burstSuffix(shade.burst)}. Structural salience only.",
                "High-salience shade event acquired${burstSuffix(shade.burst)}; content remains out of the goblin's mouth.",
            )
            shade.active && shade.burst >= 3 -> listOf(
                "Shade pressure reached ${shade.burst} events in the current burst.",
                "The notification tray has become weather: burst ${shade.burst}.",
                "Shade Sense counted ${shade.burst} structural hits. The office is officially allowed one eyebrow raise.",
            )
            marker.key == "SCREEN_SEMANTIC" && "BOUNDARY" in marker.tags -> listOf(
                "Accessibility semantics hit a protected boundary and stopped. Good sensor behavior.",
                "Visible semantics ended at the boundary instead of pretending permission.",
                "Goblin Read found the wall and, critically, did not invent a door.",
            )
            marker.key == "SCREEN_SEMANTIC" && !active.isNullOrBlank() -> listOf(
                "$active supplied owner-authorized visible semantics; Goblin Eye and Accessibility now agree on the room.",
                "The screen map has semantic anchors inside $active without upgrading them into action authority.",
                "Goblin Vision can name the visible surface in $active and still keep its hands to itself.",
            )
            marker.key == "SCREEN_TEXT" && !active.isNullOrBlank() -> listOf(
                "Local Goblin Read found visible text geometry in $active; the frame stays local and transient.",
                "$active now has a local text map layered onto the visual field.",
                "Pixels became a bounded local reading surface inside $active. No cloud oracle required.",
            )
            marker.key == "SCREEN_VISUAL" && motion != null && motion >= 60 && scene.mediaHot && !active.isNullOrBlank() && !track.isNullOrBlank() -> listOf(
                "$active hard-cut at ${motion}% sampled motion while “$track” kept scoring the scene.",
                "Goblin Eye saw a ${motion}% visual upheaval in $active; audio continuity never broke.",
                "The pixels in $active staged a coup at ${motion}% motion. “$track” retained soundtrack authority.",
            )
            marker.key == "APP_ENTER" && scene.recentSwitches >= 5 && !active.isNullOrBlank() -> listOf(
                "$active is foreground after ${scene.recentSwitches} rapid switches; the session narrative is now a chase scene.",
                "The phone crossed ${scene.recentSwitches} app boundaries in thirty seconds. $active currently owns the camera.",
                "Context velocity is rude: ${scene.recentSwitches} switches, current stop $active.",
            )
            scene.mediaHot && !track.isNullOrBlank() && !active.isNullOrBlank() && marker.key.startsWith("MEDIA") -> listOf(
                "$active owns the pixels while “$track” owns continuity.",
                "MediaSession says “$track”; foreground says $active. Both facts survive the cut.",
                "Two channels, one scene: $active in front, “$track” underneath.",
            )
            "META_RECURSION" in marker.tags && complex.occurrence >= 5 -> listOf(
                "Meta recursion occurrence ${complex.occurrence}: the goblin is now commenting on the fact that it has receipts.",
                "The feedback loop has ${complex.occurrence} sightings and enough paperwork to qualify as office furniture.",
                "Recursive scene detected again. The observer is now part of the running bit, with receipts.",
            )
            else -> emptyList()
        }

        return if (options.isEmpty()) Beat("", "") else Beat(pick(seed, options), "SAFE_OMNISCIENCE")
    }

    private fun burstSuffix(burst: Int): String = if (burst >= 2) " in burst $burst" else ""

    private fun formatDuration(ms: Long): String = when {
        ms < 1_000L -> "${ms}ms"
        ms < 60_000L -> "${ms / 1_000L}s"
        else -> "${ms / 60_000L}m ${ms % 60_000L / 1_000L}s"
    }

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
            .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun pick(seed: String, options: List<String>): String {
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
