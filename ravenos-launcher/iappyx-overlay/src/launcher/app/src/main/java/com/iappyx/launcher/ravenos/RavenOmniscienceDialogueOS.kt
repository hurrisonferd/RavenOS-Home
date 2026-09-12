package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Screen-first Meta Goblin compositor.
 *
 * "Omniscience" remains presentation language, never an evidence upgrade. The visible screen is
 * the preferred commentary subject when owner-armed local semantics/OCR are fresh. Phone actions
 * remain evidence underneath and become dialogue only when no trustworthy screen read exists and
 * the event itself is materially important.
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
        val screen = RavenScreenContextOS.snapshot(context, marker.at)
        val active = screen.appLabel ?: scene.activeApp?.take(42)
        val track = scene.mediaTitle?.take(42)
        val motion = field(marker.detail, "motion")?.toIntOrNull() ?: scene.lastVisualMotion
        val seed = "${member.id}|${marker.key}|${screen.signature}|${shade.safeDetail}|${complex.occurrence}|${episode.name}|screen-first-v2"

        val options = when {
            screen.available && screen.meta && screen.semanticKind == "CHATGPT" -> listOf(
                "This exact screen is talking about RavenOS while RavenOS is reading it: “${quote(screen.focus)}”.",
                "The thread on the glass is about RavenOS, and the goblin reading it is RavenOS. “${quote(screen.focus)}”.",
                "Fourth wall status: load-bearing. ChatGPT is visibly on “${quote(screen.focus)}” while the office watches itself being discussed.",
            )

            screen.available && screen.meta -> listOf(
                "The screen itself has become part of the bit: “${quote(screen.focus)}”.",
                "Meta event on the actual glass: “${quote(screen.focus)}”.",
                "The haunting is now visible in its own evidence: “${quote(screen.focus)}”.",
            )

            screen.available && screen.semanticKind == "CHATGPT" -> listOf(
                "ChatGPT is actually on “${quote(screen.focus)}” right now.",
                "The visible thread is centered on “${quote(screen.focus)}”.",
                "What is physically on the ChatGPT screen: “${quote(screen.focus)}”.",
            )

            screen.available && screen.semanticKind == "SETTINGS" -> listOf(
                "Settings is actually showing “${quote(screen.focus)}”.",
                "The visible settings surface is on “${quote(screen.focus)}”.",
                "Current glass, not app telemetry: “${quote(screen.focus)}”.",
            )

            screen.available && screen.semanticKind == "SYSTEM_UI" -> listOf(
                "System UI is visibly showing “${quote(screen.focus)}”.",
                "The shade itself says “${quote(screen.focus)}”.",
                "What is on the glass right now: “${quote(screen.focus)}”.",
            )

            screen.available && screen.semanticKind == "MUSIC" -> listOf(
                "Suno is visibly on “${quote(screen.focus)}”.",
                "The music screen itself is showing “${quote(screen.focus)}”.",
                "Actual screen context: “${quote(screen.focus)}”.",
            )

            screen.available && screen.top.isNotBlank() && screen.middle.isNotBlank() && screen.top != screen.middle -> listOf(
                "The screen has “${quote(screen.top)}” up top and “${quote(screen.middle)}” through the middle.",
                "I can read the actual layout: top “${quote(screen.top)}”; middle “${quote(screen.middle)}”.",
            )

            screen.available -> listOf(
                "${active ?: "The screen"} is actually showing “${quote(screen.focus)}”.",
                "What is on the glass: “${quote(screen.focus)}”.",
                "Current visible context, not a phone-action guess: “${quote(screen.focus)}”.",
                "The office is looking at “${quote(screen.focus)}” on ${active ?: "this screen"}.",
            )

            marker.key == "SCREEN_SEMANTIC" && "BOUNDARY" in marker.tags -> listOf(
                "The visible-semantic reader hit a protected surface, so there is no screen-content comment to invent.",
                "Protected screen boundary. The office knows that it cannot truthfully read this one.",
            )

            marker.key == "SCREEN_TEXT" && "BOUNDARY" in marker.tags -> listOf(
                "Goblin Read backed off the sensitive-looking screen. No fake context will replace it.",
                "The text reader hit a privacy wall; the office stays deliberately ignorant here.",
            )

            marker.key == "SCREEN_VISUAL" && motion != null && motion >= 45 && !active.isNullOrBlank() -> listOf(
                "I can see $active change substantially, but I do not have a trustworthy text read for what it says yet.",
                "$active visibly changed. Screen meaning is still unknown, so the goblin is not filling it in.",
            )

            shade.active && shade.payoff && shade.lifetimeMs != null -> listOf(
                "That shade event resolved after ${formatDuration(shade.lifetimeMs)}. There still is not enough screen context to say more.",
            )

            shade.active && shade.salience == RavenShadeSenseOS.Salience.HIGH -> listOf(
                "A high-salience shade event landed, but its structure is not the same thing as knowing what this screen means.",
            )

            scene.mediaHot && !track.isNullOrBlank() && marker.key.startsWith("MEDIA") -> emptyList()
            marker.key in setOf("APP_ENTER", "WINDOW_CHANGE", "HOME_ENTER") -> emptyList()
            else -> emptyList()
        }

        return if (options.isEmpty()) Beat("", "") else Beat(pick(seed, options), "SCREEN_FIRST_OMNISCIENCE")
    }

    private fun quote(text: String): String = text.replace(Regex("\\s+"), " ").trim().take(86)

    private fun formatDuration(ms: Long): String = when {
        ms < 1_000L -> "${ms}ms"
        ms < 60_000L -> "${ms / 1_000L}s"
        else -> "${ms / 60_000L}m ${ms % 60_000L / 1_000L}s"
    }

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
            .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun pick(seed: String, options: List<String>): String {
        if (options.size <= 1) return options.firstOrNull().orEmpty()
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
