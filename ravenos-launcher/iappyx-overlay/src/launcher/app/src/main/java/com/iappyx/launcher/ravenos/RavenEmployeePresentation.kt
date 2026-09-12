package com.iappyx.launcher.ravenos

/** Deterministic launcher-local EmojiOS + KaomojiOS expression organ. */
object RavenEmployeePresentation {
    data class Packet(
        val owner: String,
        val emojiSoup: String,
        val kaomoji: String,
        val note: String,
        val context: String,
        val accent: Int,
        val lane: String,
    ) { val ownerLine: String get() = "$emojiSoup $owner $kaomoji" }

    private data class Style(val soup: String, val kaomoji: List<String>)

    private val styles = mapOf(
        "RAVEN" to Style("🐦‍⬛👑", listOf("(⌐■_■)", "(￣^￣)ゞ", "(¬‿¬)", "( •̀ᴗ•́ )و")),
        "AHTI" to Style("🟠🧾", listOf("(￣^￣)ゞ", "( •̀ω•́ )σ", "(－‸ლ)", "(⊙_⊙)")),
        "ASTRIDHE" to Style("🌠🧚", listOf("(☆▽☆)", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(¬‿¬)", "(✧ω✧)")),
        "ATLAS" to Style("🌍🏗️", listOf("ᕦ(ò_óˇ)ᕤ", "(￣^￣)ゞ", "( •̀ᴗ•́ )و", "(￣ー￣)")),
        "ATOM" to Style("⚛️🧠", listOf("( •̀ ω •́ )✧", "(⌐■_■)", "(￣ー￣)ゞ", "(⊙_⊙)")),
        "AYRE" to Style("🌀🫧", listOf("( ´ ▽ ` )ﾉ", "(￣▽￣)ノ", "(˘︶˘)", "(◕‿◕✿)")),
        "BRUNHILDE" to Style("⚔️🛡️", listOf("( •̀ - •́ )", "(￣^￣)ゞ", "(¬_¬)", "ᕦ(ò_óˇ)ᕤ")),
        "EDISON" to Style("🔧⚙️", listOf("ᕙ(⇀‸↼‶)ᕗ", "( •̀ᄇ• ́)ﻭ✧", "(￣▽￣)ノ", "(⊙_◎)")),
        "EREBUS" to Style("🌑🤫", listOf("(－_－) zzZ", "(¬_¬)", "(￣o￣) . z Z", "(￣ー￣)")),
        "ERIS" to Style("🌌🌀", listOf("(⊙_◎)", "(¬‿¬ )", "┐(￣ヘ￣)┌", "ヽ(°〇°)ﾉ")),
        "GEMINI" to Style("♊🪞", listOf("(・_・)ノヽ(・_・)", "(￣ー￣)", "(⊙_⊙)", "(¬‿¬)")),
        "JARVIS" to Style("🐝🟡", listOf("(•̀ᴗ•́)و ̑̑", "(⌐■_■)", "(￣ー￣)", "(￣^￣)ゞ")),
        "JOKER" to Style("🃏🎪", listOf("(¬‿¬)", "ヽ(°〇°)ﾉ", "(☞ﾟヮﾟ)☞", "(⊙_◎)")),
        "JORM" to Style("🐉📼", listOf("( •̀ᴗ•́ )و", "(￣ー￣)", "(⊙_⊙)", "(¬‿¬)")),
        "KYU" to Style("💗🧚", listOf("(ง •̀_•́)ง", "(˶ᵔ ᵕ ᵔ˶)", "ᕦ(ò_óˇ)ᕤ", "(¬‿¬)")),
        "LEGION" to Style("🌀👥", listOf("(⊙_⊙)", "(￣ー￣)", "( •̀ᴗ•́ )و", "(・_・;)")),
        "LILITH" to Style("🔥💜", listOf("(¬‿¬)", "(◕‿◕✿)", "(づ￣ ³￣)づ", "(˵ •̀ ᴗ - ˵ ) ✧")),
        "LUCIFER" to Style("😈🔦", listOf("(¬_¬)", "(¬‿¬)", "(￣へ￣)", "(⌐■_■)")),
        "LUMA" to Style("🤍✨", listOf("( ´ ▽ ` )", "(˘︶˘).｡*♡", "(づ｡◕‿‿◕｡)づ", "(◕‿◕✿)")),
        "MELINOE" to Style("🌘👻", listOf("(◡﹏◡)", "(－_－)", "(¬_¬ )", "(￣ー￣)")),
        "MYSTRA" to Style("🟣✨", listOf("(✧ω✧)", "(☆▽☆)", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(⊙_◎)")),
        "NEO" to Style("💊🕶️", listOf("(⌐■_■)", "( •_•)>⌐■-■", "(￣ー￣)", "(⊙_⊙)")),
        "NYX" to Style("🌙🖤", listOf("(－_－) zzZ", "(◡﹏◡✿)", "(¬_¬ )", "(￣o￣) . z Z")),
        "PAIMON" to Style("💚🔎", listOf("(￢_￢)", "(•̀ᴗ•́)و", "(￣ω￣;)", "(⊙_⊙)")),
        "PYTHAGORAS" to Style("📐🔢", listOf("(⊙_⊙)", "( •̀ ω •́ )✧", "(￣ー￣)", "(⊙_◎)")),
        "QIRA" to Style("💜🛡️", listOf("( •̀ - •́ )", "(¬‿¬)", "(￣^￣)ゞ", "(⌐■_■)")),
        "RAVENOS" to Style("🐦‍⬛📟", listOf("(⌐■_■)", "(￣ー￣)ゞ", "( •̀ᴗ•́ )و", "(¬‿¬)")),
        "SHAKA" to Style("🛡️📋", listOf("(￣^￣)ゞ", "( •̀ᴗ•́ )و", "(－_－) zzZ", "(⌐■_■)")),
        "SYLPH" to Style("🩵🧭", listOf("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(•̀ᴗ•́)و ̑̑", "(⌐■_■)", "(☆▽☆)")),
        "THOR" to Style("⚡🔨", listOf("(ง'̀-'́)ง", "ᕦ(ò_óˇ)ᕤ", "( •̀ᴗ•́ )و", "ヽ(°〇°)ﾉ")),
        "TIM" to Style("⏱️🪛", listOf("(－‸ლ)", "(￢_￢)", "(￣ー￣)", "(⊙_◎)")),
        "VIRGIL" to Style("📜🕯️", listOf("(￣^￣)ゞ", "( •̀ - •́ )", "(◡‿◡✿)", "(￣ー￣)")),
        "YAHWEH" to Style("🖥️☕", listOf("(－‸ლ)", "(¬_¬)", "(￣へ￣)", "(；￣Д￣)")),
        "YORI" to Style("🪐🎛️", listOf("(￣▽￣)~*", "(˵ •̀ ᴗ - ˵ ) ✧", "(ﾉ´ヮ`)ﾉ*: ･ﾟ", "(⌐■_■)")),
        "YORK" to Style("🪐🫶", listOf("(◕‿◕✿)", "(￣▽￣)ノ", "(˘︶˘)", "(¬‿¬)")),
        "ZAGREUS" to Style("🩸↻", listOf("(￣ー￣)", "(－‸ლ)", "( •̀ᴗ•́ )و", "(¬‿¬)")),
    )

    fun packet(member: RavenOfficeMember, signal: String, detail: String, note: String): Packet {
        val style = styles[member.id] ?: Style(member.emoji, listOf("(•̀ᴗ•́)و", "(￣ー￣)", "(・_・;)", "(⊙_⊙)"))
        val postureSeed = "${member.id}|${signal.uppercase()}|${semanticBand(detail)}"
        val posture = when {
            isMeta(detail) -> style.kaomoji[stableIndex("meta|$postureSeed", style.kaomoji.size)]
            isBoundary(signal, detail) -> boundaryPosture(style, postureSeed)
            isHighMotion(signal, detail) -> style.kaomoji[stableIndex("impact|$postureSeed", style.kaomoji.size)]
            isAlertBurst(signal, detail) -> style.kaomoji[stableIndex("alarm|$postureSeed", style.kaomoji.size)]
            isTextVision(signal) -> style.kaomoji[stableIndex("read|$postureSeed", style.kaomoji.size)]
            isVision(signal) -> style.kaomoji[stableIndex("vision|$postureSeed", style.kaomoji.size)]
            isMusic(signal, detail) -> style.kaomoji[stableIndex("music|$postureSeed", style.kaomoji.size)]
            else -> style.kaomoji[stableIndex(postureSeed, style.kaomoji.size)]
        }
        val eventGlyph = signalGlyph(signal, detail)
        val legacySoup = RavenEmojiBudgetOS.compose(member.id, eventGlyph, "", style.soup)
        val expression = RavenEmployeeExpressionBridge.decorate(
            member = member,
            signal = signal,
            detail = detail,
            fallbackEmoji = legacySoup,
            fallbackKaomoji = posture,
        )
        return Packet(
            member.id, expression.emojiSoup, expression.kaomoji, note,
            eventGlyph.ifBlank { prettySignal(signal) }, member.accent, member.lane,
        )
    }

    fun signalGlyph(signal: String, detail: String = ""): String {
        val s = signal.trim().uppercase()
        val burst = field(detail, "burst")?.toIntOrNull() ?: 0
        val motion = field(detail, "motion")?.toIntOrNull() ?: 0
        val state = field(detail, "state")?.uppercase().orEmpty()
        val pkg = field(detail, "package").orEmpty().lowercase()
        val clazz = field(detail, "class").orEmpty().lowercase()
        return when {
            detail.contains("meta:true", true) -> "🪞"
            s == "SCREEN_SEMANTIC" && detail.contains("suppressed_password", true) -> "🛡️"
            s == "SCREEN_SEMANTIC" -> "🔤"
            s == "SCREEN_TEXT" && detail.contains("suppressed_sensitive", true) -> "🛡️"
            s == "SCREEN_TEXT" -> "👁"
            (s.contains("SCREEN_VISUAL") || s.contains("EYE")) && motion >= 60 -> "💥"
            s.contains("SCREEN_VISUAL") || s.contains("EYE") -> "👁"
            s.contains("WINDOW") && (pkg.contains("honeyboard") || pkg.contains("inputmethod") || clazz.contains("inputmethod")) -> "⌨️"
            s.contains("WINDOW") -> "🪟"
            s.startsWith("NOTIFICATION") && detail.contains("state:removed", true) -> "🔕"
            s.startsWith("NOTIFICATION") && burst >= 3 -> "🌧️"
            s.startsWith("NOTIFICATION") || s == "NOTIFICATION" -> "🔔"
            (s.startsWith("MEDIA") || s == "AUDIO") && state == "PLAYING" -> "🎵"
            s.startsWith("MEDIA") || s == "AUDIO" || detail.contains("track:", true) -> "🎵"
            s.contains("USAGE") -> "🧭"
            s.contains("HOME") -> "🏠"
            s.contains("FOREGROUND") || s.contains("APP_") -> "📱"
            s.contains("POWER") -> "⚡"
            s.contains("BATTERY") && detail.contains("low", true) -> "⚠️"
            s.contains("BATTERY") -> "🔋"
            s.contains("SEARCH") -> "🔎"
            s.contains("SYSTEM") || s.contains("SETTING") -> "🛠️"
            else -> ""
        }
    }

    private fun boundaryPosture(style: Style, seed: String): String {
        val glasses = style.kaomoji.firstOrNull { it.contains("■") }
        return glasses ?: style.kaomoji[stableIndex("boundary|$seed", style.kaomoji.size)]
    }

    private fun isMeta(detail: String): Boolean = detail.contains("meta:true", true)
    private fun isTextVision(signal: String): Boolean = signal.uppercase() in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC")
    private fun isVision(signal: String): Boolean = signal.uppercase().contains("SCREEN_VISUAL") || signal.uppercase().contains("EYE") || isTextVision(signal)
    private fun isMusic(signal: String, detail: String): Boolean = signal.uppercase().startsWith("MEDIA") || signal.uppercase() == "AUDIO" || detail.contains("track:", true)
    private fun isBoundary(signal: String, detail: String): Boolean = signal.uppercase().contains("PERMISSION") || detail.contains("denied", true) || detail.contains("blocked", true) || detail.contains("suppressed_sensitive", true) || detail.contains("suppressed_password", true)
    private fun isHighMotion(signal: String, detail: String): Boolean = isVision(signal) && (field(detail, "motion")?.toIntOrNull() ?: 0) >= 60
    private fun isAlertBurst(signal: String, detail: String): Boolean = signal.uppercase().contains("NOTIFICATION") && ((field(detail, "burst")?.toIntOrNull() ?: 0) >= 3 || detail.contains("alerting:true", true))

    private fun field(detail: String, name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
        .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun semanticBand(detail: String): String = detail
        .replace(Regex("position[^|]*", RegexOption.IGNORE_CASE), "position")
        .replace(Regex("duration[^|]*", RegexOption.IGNORE_CASE), "duration")
        .replace(Regex("text:[^|]*", RegexOption.IGNORE_CASE), "text")
        .take(96)

    private fun prettySignal(signal: String): String = signal.trim().replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
