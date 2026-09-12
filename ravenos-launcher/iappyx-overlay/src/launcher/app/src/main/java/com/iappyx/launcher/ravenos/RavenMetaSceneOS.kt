package com.iappyx.launcher.ravenos

import android.content.Context
import kotlin.math.abs

/**
 * Fuses current phone evidence into one compact scene-level observation before character comedy.
 * Evidence authority stays explicit: OCR and Accessibility semantics must be separately owner-armed.
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
        val ocr = RavenGoblinReadOS.latest(context, marker.at)
        val access = RavenAccessibilityReadOS.latest(context, marker.at)
        val pkg = field(marker.detail, "package") ?: access?.packageName
        val app = field(marker.detail, "app")?.take(48) ?: scene.activeApp?.take(48)
        val track = scene.mediaTitle?.take(48)

        val directText = field(marker.detail, "text")?.take(180)
        val accessText = access?.takeIf { abs(marker.at - it.capturedAt) <= 5_000L }?.text
        val ocrText = ocr?.takeIf { abs(marker.at - it.capturedAt) <= 6_000L }?.text
        val visibleText = directText ?: accessText ?: ocrText
        val keyboardLike = access?.keyboardLike == true || pkg.orEmpty().contains("honeyboard", true) || pkg.orEmpty().contains("inputmethod", true)
        val semantic = RavenAppSemanticsOS.interpret(context, pkg, app, visibleText, keyboardLike)
        val recursive = field(marker.detail, "meta") == "true" || (!visibleText.isNullOrBlank() && RavenMetaRecursionOS.detect(visibleText))
        val seed = "${member.id}|${marker.key}|${marker.detail}|${complex.occurrence}|${semantic.kind}|scene-v2"

        if (callback.text.isNotBlank()) return Beat(callback.text, callback.family)

        val options = when {
            recursive && semantic.kind == "CHATGPT" -> listOf(
                "ChatGPT is visibly discussing RavenOS while RavenOS is floating over ChatGPT. Recursion confirmed.",
                "The conversation is talking about the goblin currently reading the conversation. Excellent containment.",
                "RavenOS is on top of ChatGPT, reading ChatGPT talk about RavenOS. The loop has closed.",
            )

            recursive && !visibleText.isNullOrBlank() -> listOf(
                "The screen is visibly talking about RavenOS itself: “${focus(visibleText)}”.",
                "Meta event: “${focus(visibleText)}” is on the glass while the office is watching.",
                "The phone has begun discussing its own haunting. “${focus(visibleText)}”.",
            )

            marker.key == "SCREEN_SEMANTIC" && marker.detail.contains("suppressed_password", true) -> listOf(
                "Accessibility Read found a password surface and refused to narrate it.",
                "Credential-shaped UI detected. The office looked away on purpose.",
            )

            marker.key == "SCREEN_SEMANTIC" && !visibleText.isNullOrBlank() -> listOf(
                "${semantic.label}: ${semantic.summary}. “${focus(visibleText)}” is visibly present.",
                "${semantic.summary.replaceFirstChar { it.uppercase() }} in ${semantic.label}; the glass says “${focus(visibleText)}”.",
                "${semantic.label} is in ${semantic.summary} mode. Visible cue: “${focus(visibleText)}”.",
            )

            marker.key == "SCREEN_TEXT" && marker.detail.contains("suppressed_sensitive", true) -> listOf(
                "Goblin Read hit sensitive-looking text and shut its mouth.",
                "Text vision reached credential-shaped territory and politely looked away.",
            )

            marker.key == "SCREEN_TEXT" && !visibleText.isNullOrBlank() -> listOf(
                "${semantic.label} is visibly showing “${focus(visibleText)}”.",
                "Goblin Read caught “${focus(visibleText)}” on ${semantic.label}.",
                "The glass currently says “${focus(visibleText)}” inside ${semantic.label}.",
            )

            marker.key == "APP_ENTER" && semantic.kind != "APP" -> listOf(
                "${semantic.label}: ${semantic.summary}.",
                "${semantic.summary.replaceFirstChar { it.uppercase() }} just took foreground.",
                "New foreground scene: ${semantic.label} · ${semantic.summary}.",
            )

            marker.key == "WINDOW_CHANGE" && keyboardLike && semantic.kind != "KEYBOARD" -> listOf(
                "Keyboard layer changed; ${semantic.label} is still the real task.",
                "Typing surface moved inside ${semantic.label}; mission unchanged.",
                "${semantic.label} stayed put while the input layer rearranged itself.",
            )

            marker.key == "WINDOW_CHANGE" && semantic.kind != "APP" -> listOf(
                "${semantic.label} changed internal surfaces; still ${semantic.summary}.",
                "Same ${semantic.label} task, different internal room.",
                "${semantic.label} stayed foreground; its scene changed underneath us.",
            )

            marker.key == "SCREEN_VISUAL" && !app.isNullOrBlank() && scene.mediaHot && !track.isNullOrBlank() && (field(marker.detail, "motion")?.toIntOrNull() ?: 0) >= 45 -> listOf(
                "$app visually hard-cut while “$track” kept scoring the scene.",
                "Big pixel change in $app; “$track” survived untouched.",
                "$app changed what it was showing. “$track” refused to leave soundtrack duty.",
            )

            marker.key == "APP_ENTER" && scene.notificationBurst >= 3 && scene.mediaHot && !track.isNullOrBlank() && !app.isNullOrBlank() -> listOf(
                "$app took foreground under “$track” while ${scene.notificationBurst} pings rattled the tray.",
                "$app has the screen, “$track” has audio, and the notification tray has unionized.",
            )

            scene.recentSwitches >= 6 && scene.mediaHot && !track.isNullOrBlank() && !app.isNullOrBlank() -> listOf(
                "$app is foreground after ${scene.recentSwitches} recent switches. “$track” somehow survived all of it.",
                "Six-plus context cuts, one soundtrack: “$track” is carrying this entire phone session.",
            )

            else -> emptyList()
        }

        return if (options.isEmpty()) Beat("", "") else Beat(pick(seed, options), "META_SCENE_FUSED_V2")
    }

    private fun focus(text: String): String = RavenMetaRecursionOS.focus(text)
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.trimEnd('.', ',', ':', ';')
        ?.take(82)
        ?: text.replace(Regex("\\s+"), " ").trim().take(82)

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
