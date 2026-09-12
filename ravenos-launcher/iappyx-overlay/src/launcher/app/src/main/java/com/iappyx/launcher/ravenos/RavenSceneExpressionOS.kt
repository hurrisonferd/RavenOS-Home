package com.iappyx.launcher.ravenos

import android.content.Context

/** Screen-aware EmojiOS + KaomojiOS decoration downstream of owner-native presentation. */
object RavenSceneExpressionOS {
    fun decorate(
        context: Context,
        base: RavenEmployeePresentation.Packet,
        member: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
    ): RavenEmployeePresentation.Packet {
        if (!screen.available) return base
        val viewport = RavenViewportSemanticsOS.latest(context)
        val task = viewport?.task.orEmpty().ifBlank { inferTask(screen.semanticSummary) }
        val sceneGlyph = sceneGlyph(screen.semanticKind)
        val taskGlyph = taskGlyph(task)
        val metaGlyph = if (screen.meta) "🪞" else ""
        val beatGlyph = when (direction.beat) {
            "CALLBACK" -> "🔁"
            "BUG" -> "🐛"
            "PAYOFF" -> "✅"
            "COLD_OPEN" -> "🎬"
            "CROSSTALK" -> "💬"
            "CUTAWAY" -> "✂️"
            "META" -> "🪞"
            else -> ""
        }
        val extras = listOf(metaGlyph, sceneGlyph, taskGlyph, beatGlyph)
            .filter(String::isNotBlank)
            .distinct()
            .take(3)
        val soup = buildString {
            append(base.emojiSoup)
            extras.forEach { glyph -> if (!contains(glyph)) append(glyph) }
        }
        val face = expressiveFace(member.id, task, screen.meta, direction, base.kaomoji)
        return base.copy(
            emojiSoup = soup,
            kaomoji = face,
            context = listOfNotNull(
                sceneGlyph.takeIf(String::isNotBlank),
                task.lowercase().replace('_', ' ').takeIf(String::isNotBlank),
            ).joinToString(" ").ifBlank { base.context },
        )
    }

    private fun sceneGlyph(kind: String): String = when (kind.uppercase()) {
        "CHATGPT" -> "🤖💬"
        "BROWSER" -> "🌐👁"
        "COMMUNITY" -> "🧵💬"
        "CODE" -> "💻🧪"
        "TERMINAL" -> "⌨️💻"
        "SETTINGS" -> "⚙️🛠️"
        "SYSTEM_UI" -> "📱🪟"
        "KEYBOARD" -> "⌨️"
        "HOME" -> "🏠"
        "MUSIC" -> "🎵✨"
        "VIDEO" -> "📺👁"
        "MAIL" -> "✉️"
        "MESSAGING" -> "💬📨"
        "FILES" -> "📁"
        "GALLERY" -> "🖼️"
        "CAMERA" -> "📷"
        "STORE" -> "🛍️📲"
        else -> "👁️📱"
    }

    private fun taskGlyph(task: String): String = when (task.uppercase()) {
        "COMPOSING" -> "✍️"
        "SEARCHING" -> "🔎"
        "CONFIGURING" -> "🛠️"
        "DEBUGGING" -> "🧪"
        "LISTENING" -> "🎧"
        "WATCHING" -> "🍿"
        "BROWSING", "WEB" -> "🧭"
        "READING_CHAT" -> "📖💬"
        "READING" -> "📖"
        "TYPING" -> "⌨️"
        "VIEWING" -> "👀"
        else -> ""
    }

    private fun inferTask(summary: String): String {
        val s = summary.lowercase()
        return when {
            "composing" in s -> "COMPOSING"
            "reading chat" in s -> "READING_CHAT"
            "debugging" in s -> "DEBUGGING"
            "configuring" in s -> "CONFIGURING"
            "searching" in s -> "SEARCHING"
            "browsing" in s -> "BROWSING"
            "listening" in s -> "LISTENING"
            "watching" in s -> "WATCHING"
            "reading" in s -> "READING"
            "typing" in s -> "TYPING"
            else -> "VIEWING"
        }
    }

    private fun expressiveFace(
        owner: String,
        task: String,
        meta: Boolean,
        direction: RavenSitcomDirectorOS.Direction,
        fallback: String,
    ): String {
        val family = when (owner) {
            "KYU", "JOKER", "MYSTRA", "ASTRIDHE" -> listOf("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(☞ﾟヮﾟ)☞", "(ง •̀_•́)ง", "(☆▽☆)", "(¬‿¬)")
            "ATOM", "PAIMON", "PYTHAGORAS", "EDISON", "NEO", "TIM" -> listOf("( •̀ ω •́ )✧", "(￢_￢)", "(⊙_◎)", "(⌐■_■)", "(￣ー￣)ゞ")
            "LILITH", "LUMA", "AYRE", "YORK", "YORI" -> listOf("(˵ •̀ ᴗ - ˵ ) ✧", "(◕‿◕✿)", "(￣▽￣)~*", "(˘︶˘).｡*♡", "(ﾉ´ヮ`)ﾉ*: ･ﾟ")
            "MELINOE", "NYX", "EREBUS", "VIRGIL" -> listOf("(◡﹏◡)", "(¬_¬ )", "(￣ー￣)", "(－_－) zzZ", "(◡‿◡✿)")
            "BRUNHILDE", "QIRA", "THOR", "SHAKA", "LUCIFER" -> listOf("ᕦ(ò_óˇ)ᕤ", "( •̀ - •́ )", "(ง'̀-'́)ง", "(￣^￣)ゞ", "(¬_¬)")
            "ERIS", "LEGION", "JORM", "ZAGREUS", "AHTI", "ATLAS", "JARVIS", "RAVENOS", "YAHWEH" -> listOf("(⊙_◎)", "( •̀ᴗ•́ )و", "(￣ー￣)", "(－‸ლ)", "(¬‿¬)")
            else -> return fallback
        }
        val seed = "$owner|$task|$meta|${direction.beat}|${direction.turn}|${direction.sceneId}"
        val gate = stableIndex("gate|$seed", 4)
        if (gate == 0 && !meta && direction.beat == "OBSERVE") return fallback
        return family[stableIndex(seed, family.size)]
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
