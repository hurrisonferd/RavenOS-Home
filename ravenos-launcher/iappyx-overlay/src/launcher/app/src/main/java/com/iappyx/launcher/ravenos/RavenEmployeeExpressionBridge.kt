package com.iappyx.launcher.ravenos

/**
 * Runtime bridge from the existing employee-presentation seam into the Sat-X
 * EmojiOS/KaomojiOS/meta expression systems. Facts are already settled upstream.
 */
object RavenEmployeeExpressionBridge {
    data class Result(val emojiSoup: String, val kaomoji: String, val family: String)

    fun decorate(
        member: RavenOfficeMember,
        signal: String,
        detail: String,
        fallbackEmoji: String,
        fallbackKaomoji: String,
    ): Result {
        val family = familyFor(signal, detail)
        val occurrence = field(detail, "bit_count")?.toIntOrNull()
            ?: field(detail, "occurrence")?.toIntOrNull()
            ?: 1
        val motif = field(detail, "script_motif").orEmpty()
        val callback = field(detail, "callback").orEmpty()
        val surface = when {
            detail.contains("presentation_board", true) -> "BOARD_MEETING"
            detail.contains("presentation_diagnostic", true) -> "DIAGNOSTIC"
            detail.contains("presentation_screen", true) -> "DESK"
            else -> "PEEK"
        }
        val projected = RavenMetaDialogueEngine.project(
            RavenMetaDialogueEngine.Input(
                owner = member.id,
                event = signal,
                semanticFamily = family,
                motif = motif,
                callback = callback,
                occurrence = occurrence,
                surface = surface,
                noveltyLow = detail.contains("novelty:low", true),
                quiet = detail.contains("quiet:true", true),
                launcherCritical = detail.contains("launcher_critical:true", true),
            )
        )
        val emoji = projected.emoji.ifBlank { fallbackEmoji }
        val kaomoji = projected.kaomoji.ifBlank { fallbackKaomoji }
        return Result(emoji, kaomoji, projected.family)
    }

    private fun familyFor(signal: String, detail: String): String {
        val s = signal.uppercase()
        return when {
            detail.contains("meta:true", true) -> "META"
            detail.contains("denied", true) || detail.contains("blocked", true) -> "BOUNDARY"
            s.contains("SCREEN_OFF") || s.contains("NIGHT") -> "NIGHT_WATCH"
            s.contains("MEDIA") || s.contains("AUDIO") -> "MUSIC"
            s.contains("ERROR") || s.contains("FAIL") -> "FAILURE"
            s.contains("USER_PRESENT") || s.contains("BOOT") || s.contains("RETURN") -> "DELIGHT"
            s.contains("NOTIFICATION") -> "WATCH"
            s.contains("SCREEN") || s.contains("EYE") -> "FOCUS"
            else -> "WATCH"
        }
    }

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
            .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
}
