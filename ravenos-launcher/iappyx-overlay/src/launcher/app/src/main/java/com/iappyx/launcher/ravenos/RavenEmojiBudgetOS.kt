package com.iappyx.launcher.ravenos

/**
 * Final EmojiOS budget for Follow-Me presentation.
 * Identity remains stable; the scene gets one glyph and exceptional state gets one glyph.
 * Kaomoji carries most of the expressive posture so the header never turns into telemetry soup.
 */
object RavenEmojiBudgetOS {
    private val identity = mapOf(
        "RAVEN" to "🐦‍⬛👑",
        "AHTI" to "🟠🧾",
        "ASTRIDHE" to "🌠🧚",
        "ATLAS" to "🌍🏗️",
        "ATOM" to "⚛️🧠",
        "AYRE" to "🌀🫧",
        "BRUNHILDE" to "⚔️🛡️",
        "EDISON" to "🔧⚙️",
        "EREBUS" to "🌑🤫",
        "ERIS" to "🌌🌀",
        "GEMINI" to "♊🪞",
        "JARVIS" to "🐝🟡",
        "JOKER" to "🃏🎪",
        "JORM" to "🐉📼",
        "KYU" to "💗🧚",
        "LEGION" to "🌀👥",
        "LILITH" to "🔥💜",
        "LUCIFER" to "😈🔦",
        "LUMA" to "🤍✨",
        "MELINOE" to "🌘👻",
        "MYSTRA" to "🟣✨",
        "NEO" to "💊🕶️",
        "NYX" to "🌙🖤",
        "PAIMON" to "💚🔎",
        "PYTHAGORAS" to "📐🔢",
        "QIRA" to "💜🛡️",
        "RAVENOS" to "🐦‍⬛📟",
        "SHAKA" to "🛡️📋",
        "SYLPH" to "🩵🧭",
        "THOR" to "⚡🔨",
        "TIM" to "⏱️🪛",
        "VIRGIL" to "📜🕯️",
        "YAHWEH" to "🖥️☕",
        "YORI" to "🪐🎛️",
        "YORK" to "🪐🫶",
        "ZAGREUS" to "🩸↻",
    )

    fun identity(owner: String, fallback: String = "👁"): String = identity[owner.uppercase()] ?: fallback

    fun compose(owner: String, scene: String = "", state: String = "", fallback: String = "👁"): String {
        val base = identity(owner, fallback)
        val extras = listOf(scene.trim(), state.trim())
            .filter(String::isNotBlank)
            .distinct()
            .filterNot { base.contains(it) }
        return buildString {
            append(base)
            extras.take(2).forEach { append(it) }
        }
    }

    fun glyphCandidate(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank()) return ""
        // Event labels such as "Screen Semantic" are context text, not EmojiOS glyphs.
        if (clean.any { it.isLetterOrDigit() }) return ""
        return clean.take(12)
    }
}
