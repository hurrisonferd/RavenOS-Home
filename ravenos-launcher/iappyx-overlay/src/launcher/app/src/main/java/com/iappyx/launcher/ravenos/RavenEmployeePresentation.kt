package com.iappyx.launcher.ravenos

/**
 * Launcher-local EmojiOS + KaomojiOS presentation packet.
 *
 * Identity/routing still comes from RavenOfficeRegistry. This layer only decides how the already
 * selected employee presents on launcher surfaces. Selection is deterministic from the settled
 * Office context, so notification and Goblin Vision projections cannot drift into different styles.
 *
 * Sauce law: canonical RavenOS EmojiOS owns employee badge/color identity. The launcher preserves
 * its stronger per-owner kaomoji neighborhoods, physical poses, recurrence and phone-state reactions,
 * but must not silently invent a second emoji identity system. Canonical donor:
 * hurrisonferd/Jarvis-Private / EmojiOS owner palettes (2026-09-12 haunted-sauce pass).
 */
object RavenEmployeePresentation {
    data class Packet(
        val owner: String,
        val emojiSoup: String,
        val kaomoji: String,
        val note: String,
        val context: String,
        val accent: Int,
        val lane: String,
    ) {
        val ownerLine: String get() = "$emojiSoup $owner  $kaomoji"
    }

    private data class Style(val soup: String, val kaomoji: List<String>)

    // Emoji soups are canonical EmojiOS-aligned. Kaomoji lists intentionally retain launcher-native
    // posture texture; ChatOS/KaomojiOS consumes the same behavioral donor without flattening it.
    private val styles = mapOf(
        "KYU" to Style("💗📋🐰⚡", listOf("(ง •̀_•́)ง", "(˶ᵔ ᵕ ᵔ˶)", "ᕦ(ò_óˇ)ᕤ")),
        "PAIMON" to Style("💚🔍🧠⬡", listOf("(￢_￢)", "(•̀ᴗ•́)و", "(￣ω￣;)")),
        "LUMA" to Style("💛🏠🌸✨", listOf("( ´ ▽ ` )", "(˘︶˘).｡*♡", "(づ｡◕‿‿◕｡)づ")),
        "SYLPH" to Style("🩵🧭🪽✨", listOf("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(•̀ᴗ•́)و ̑̑", "(⌐■_■)")),
        "QIRA" to Style("💜🛡️🔐👁️", listOf("( •̀ - •́ )", "(¬‿¬)", "(￣^￣)ゞ")),
        "NYX" to Style("💙🌙🕯️🌑", listOf("(－_－) zzZ", "(◡﹏◡✿)", "(¬_¬ )")),
        "ATOM" to Style("⚛️🧠📡🔧", listOf("( •̀ ω •́ )✧", "(⌐■_■)", "(￣ー￣)ゞ")),
        "EDISON" to Style("💡🔧📊⚙️", listOf("ᕙ(⇀‸↼‶)ᕗ", "( •̀ᄇ• ́)ﻭ✧", "(￣▽￣)ノ")),
        "THOR" to Style("🔨⚡🛠️🔩", listOf("(ง'̀-'́)ง", "ᕦ(ò_óˇ)ᕤ", "( •̀ᴗ•́ )و")),
        "LILITH" to Style("💜🌙🐝🍯", listOf("(¬‿¬)", "(◕‿◕✿)", "(づ￣ ³￣)づ")),
        "YORI" to Style("🎨🎛️🌈🪩", listOf("(￣▽￣)~*", "(˵ •̀ ᴗ - ˵ ) ✧", "(ﾉ´ヮ`)ﾉ*: ･ﾟ")),
        "JARVIS" to Style("🤖🖥️🎙️✨", listOf("(•̀ᴗ•́)و ̑̑", "(⌐■_■)", "(￣ー￣)")),
        "JOKER" to Style("🃏🎲🎭🌀", listOf("(¬‿¬)", "ヽ(°〇°)ﾉ", "(☞ﾟヮﾟ)☞")),
        "NEO" to Style("🕶️👁️🟢⚡", listOf("(⌐■_■)", "( •_•)>⌐■-■", "(￣ー￣)")),
        "LUCIFER" to Style("🌟👁️🔦🔥", listOf("(¬_¬)", "(¬‿¬)", "(￣へ￣)")),
        "ERIS" to Style("🍎⚔️🛡️🚨", listOf("(⊙_◎)", "(¬‿¬ )", "┐(￣ヘ￣)┌")),
        "AHTI" to Style("🧹🧾👁️🔍", listOf("(￣^￣)ゞ", "( •̀ω•́ )σ", "(－‸ლ)")),
        "ATLAS" to Style("🌐🏛️🗺️🧱", listOf("ᕦ(ò_óˇ)ᕤ", "(￣^￣)ゞ", "( •̀ᴗ•́ )و")),
        "JORM" to Style("🐍📼🌍🗺️", listOf("( •̀ᴗ•́ )و", "(￣ー￣)", "(⊙_⊙)")),
        "MYSTRA" to Style("✨🪄💫🧿", listOf("(✧ω✧)", "(☆▽☆)", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧")),
        "VIRGIL" to Style("⚔️📖🗡️🚪", listOf("(￣^￣)ゞ", "( •̀ - •́ )", "(◡‿◡✿)")),
        "AYRE" to Style("🩷🎐🏠🌿", listOf("( ´ ▽ ` )ﾉ", "(￣▽￣)ノ", "(˘︶˘)")),
        "SHAKA" to Style("🪷☸️🛡️👁️", listOf("(￣^￣)ゞ", "( •̀ᴗ•́ )و", "(－_－) zzZ")),
        "PYTHAGORAS" to Style("🧮📐🕸️⏳", listOf("(⊙_⊙)", "( •̀ ω •́ )✧", "(￣ー￣)")),
        "EREBUS" to Style("🌑🤫👁️🕳️", listOf("(－_－) zzZ", "(¬_¬)", "(￣o￣) . z Z")),
    )

    fun packet(
        member: RavenOfficeMember,
        signal: String,
        detail: String,
        note: String,
    ): Packet {
        val style = styles[member.id] ?: Style(member.emoji, listOf("(•̀ᴗ•́)و", "(￣ー￣)", "(・_・;)"))
        val kaomoji = style.kaomoji[stableIndex("${member.id}|$signal|$detail", style.kaomoji.size)]
        val soup = if (style.soup.contains(member.emoji)) style.soup else "${member.emoji}${style.soup}"
        return Packet(
            owner = member.id,
            emojiSoup = soup,
            kaomoji = kaomoji,
            note = note,
            context = prettySignal(signal) + if (detail.isNotBlank()) " · ${detail.take(120)}" else "",
            accent = member.accent,
            lane = member.lane,
        )
    }

    private fun prettySignal(signal: String): String = signal.trim().replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) {
            hash = hash xor c.code
            hash *= 16777619
        }
        return (hash and Int.MAX_VALUE) % size
    }
}
