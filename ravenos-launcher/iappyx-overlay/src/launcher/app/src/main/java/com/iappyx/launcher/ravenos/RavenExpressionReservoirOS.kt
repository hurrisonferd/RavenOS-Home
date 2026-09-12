package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Large authored expression reservoir with local usage memory.
 *
 * Expression history stores only authored expression ids/counters. It never stores screen content.
 * Kaomoji carry posture; EmojiOS keeps the compact slot budget and only varies the glyph in a slot.
 */
object RavenExpressionReservoirOS {
    fun select(
        context: Context,
        owner: String,
        mood: String,
        seed: String,
        fallback: String,
        turn: Int,
    ): String {
        val candidates = (moodFaces(mood) + familyFaces(owner) + listOf(fallback))
            .filter(String::isNotBlank).distinct()
        if (candidates.isEmpty()) return fallback
        val dao = RavenDialogueVaultDatabase.get(context).dao()
        val now = System.currentTimeMillis()
        val scored = candidates.map { face ->
            val id = "K:${owner.uppercase()}:${stableHex(face)}"
            val old = dao.expression(id)
            val age = if (old == null) Long.MAX_VALUE else now - old.lastAt
            val recencyPenalty = when {
                age < 45_000L -> 120
                age < 4L * 60_000L -> 35
                age < 20L * 60_000L -> 12
                else -> 0
            }
            val usePenalty = (old?.useCount ?: 0).coerceAtMost(20) * 2
            val deterministic = stableIndex("$seed|$face", 17)
            Triple(face, id, 80 - recencyPenalty - usePenalty + deterministic)
        }
        val bestScore = scored.maxOf { it.third }
        val pool = scored.filter { it.third >= bestScore - 3 }
        val pick = pool[stableIndex("$seed|reservoir|$turn", pool.size)]
        val old = dao.expression(pick.second)
        dao.putExpression(
            RavenExpressionUseEntity(
                expressionId = pick.second,
                owner = owner.uppercase(),
                mood = mood.uppercase(),
                useCount = (old?.useCount ?: 0) + 1,
                lastAt = now,
                lastTurn = turn,
            ),
        )
        return pick.first
    }

    fun compact(context: Context, owner: String, mood: String): String = runCatching {
        val recent = RavenDialogueVaultDatabase.get(context).dao().recentExpressions(owner.uppercase(), mood.uppercase(), 6)
        "EXPRESSIONS=${recent.size} recent/$owner/$mood"
    }.getOrDefault("EXPRESSIONS=UNAVAILABLE")

    private fun moodFaces(mood: String): List<String> = when (mood.uppercase()) {
        "META" -> listOf("(☞ﾟヮﾟ)☞", "(¬‿¬)", "(⊙_◎)", "(ﾉﾟ0ﾟ)ﾉ~", "(☉_☉)", "(ಠ‿ಠ)", "(✧ω✧)", "(¬_¬)ﾉ⌐■-■")
        "CALLBACK" -> listOf("(￣ー￣)ゞ", "(｡•̀ᴗ-)✧", "(¬‿¬)", "( •̀ᴗ•́ )و", "(づ｡◕‿‿◕｡)づ", "(⌐■_■)", "(｀･ω･´)ゞ")
        "BUG" -> listOf("(ಠ_ಠ)", "(╬ಠ益ಠ)", "(◎_◎;)", "(－‸ლ)", "(งಠ_ಠ)ง", "(⊙﹏⊙)", "(・_・;)")
        "PAYOFF" -> listOf("(๑˃ᴗ˂)ﻭ", "＼(≧▽≦)／", "(✧ω✧)", "(｡•̀ᴗ-)✧", "( •̀ ω •́ )✧", "(◕‿◕✿)", "(￣▽￣)ゞ")
        "READING" -> listOf("(•ิ_•ิ)?", "(・_・ヾ", "(¬_¬)", "(｡•́‿•̀｡)", "(￣ー￣)", "(•‿•)", "(－ω－)")
        "COMPOSING" -> listOf("(๑•̀ㅂ•́)و✧", "( •̀ ω •́ )✧", "(ง •̀_•́)ง", "(｡•̀ᴗ-)✧", "(｀･ω･´)ゞ", "(￣▽￣)ノ")
        "MUSIC" -> listOf("ヾ(⌐■_■)ノ♪", "(〜￣▽￣)〜", "ヽ(o＾▽＾o)ノ", "(ﾉ´ヮ`)ﾉ*: ･ﾟ", "♪(┌・。・)┌", "(￣▽￣)~*", "(づ￣ ³￣)づ")
        "QUIET" -> listOf("(◡﹏◡)", "(－_－) zzZ", "(￣ρ￣)..zzZZ", "(¬､¬)", "(－.－)...zzz", "(っ˘ω˘ς )", "(◡‿◡✿)")
        "ALERT" -> listOf("ヽ(°〇°)ﾉ", "(⊙_⊙;)", "(╬ಠ益ಠ)", "(ง ͠° ͟ل͜ ͡°)ง", "(◎_◎;)", "(￣□￣」)", "୧(•̀ᗝ•́)૭")
        "SMUG" -> listOf("(¬‿¬)", "(￣ー￣)", "(ಠ‿ಠ)", "(｡•̀ᴗ-)✧", "(⌐■_■)", "(￣▽￣)ゞ", "(¬_¬)ﾉ")
        "ANALYTIC" -> listOf("( •̀ ω •́ )✧", "(￢_￢)", "(⌐■_■)", "( •_•)>⌐■-■", "(☉_☉)", "(￣ー￣)ゞ", "(๑•̀ㅂ•́)و✧")
        else -> emptyList()
    }

    private fun familyFaces(owner: String): List<String> = when (owner.uppercase()) {
        "KYU", "JOKER", "MYSTRA", "ASTRIDHE" -> listOf(
            "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(☞ﾟヮﾟ)☞", "(ง •̀_•́)ง", "(☆▽☆)", "(¬‿¬)", "ヽ(°〇°)ﾉ",
            "(๑˃ᴗ˂)ﻭ", "(づ｡◕‿‿◕｡)づ", "＼(≧▽≦)／", "(✧ω✧)", "(╯✧▽✧)╯", "(ﾉ≧∀≦)ﾉ",
            "(ﾉ´ з `)ノ", "ヾ(⌐■_■)ノ♪", "(☞ ՞ਊ ՞)☞", "ᕕ( ᐛ )ᕗ", "(づ￣ ³￣)づ", "(≧◡≦)",
            "(ﾉﾟ▽ﾟ)ﾉ", "(◕‿◕)♡", "(つ✧ω✧)つ", "(☞ﾟ∀ﾟ)☞", "(ﾉಥ益ಥ）ﾉ", "(๑•̀ᗝ•́)૭",
        )
        "ATOM", "PAIMON", "PYTHAGORAS", "EDISON", "NEO", "TIM", "JARVIS" -> listOf(
            "( •̀ ω •́ )✧", "(￢_￢)", "(⊙_◎)", "(⌐■_■)", "(￣ー￣)ゞ", "(•̀ᴗ•́)و ̑̑",
            "(ಠ_ಠ)", "(☉_☉)", "(¬_¬)ﾉ⌐■-■", "( •_•)>⌐■-■", "(◎_◎;)", "(￣▽￣)ノ",
            "( •_•)⌐■-■", "(⌐▨_▨)", "(๑•̀ㅂ•́)و✧", "(￣ω￣;)", "(・_・ヾ", "(¬､¬)",
            "(⊙_⊙;)", "( •̀_•́ )", "(￣^￣)ゞ", "(ಠ‿ಠ)", "(•ิ_•ิ)?", "(￣へ￣)",
        )
        "LILITH", "LUMA", "AYRE", "YORK", "YORI", "SYLPH" -> listOf(
            "(˵ •̀ ᴗ - ˵ ) ✧", "(◕‿◕✿)", "(￣▽￣)~*", "(˘︶˘).｡*♡", "(ﾉ´ヮ`)ﾉ*: ･ﾟ", "(｡•̀ᴗ-)✧",
            "(づ￣ ³￣)づ", "( ´ ▽ ` ).｡ｏ♡", "(ღ˘⌣˘ღ)", "(◡‿◡✿)", "(｡･ω･｡)ﾉ♡", "(つ≧▽≦)つ",
            "(人 •͈ᴗ•͈)", "(づ ◕‿◕ )づ", "( ´ ∀ `)ノ～ ♡", "(っ˘ω˘ς )", "(｡•́‿•̀｡)", "(◕ᴗ◕✿)",
            "(っ˘ڡ˘ς)", "(´｡• ᵕ •｡`) ♡", "(＾▽＾)", "(⌒‿⌒)", "(◡ ω ◡)", "(￣︶￣)",
        )
        "MELINOE", "NYX", "EREBUS", "VIRGIL", "AHTI" -> listOf(
            "(◡﹏◡)", "(¬_¬ )", "(￣ー￣)", "(－_－) zzZ", "(◡‿◡✿)", "(幽_幽)",
            "(－ω－) zzZ", "(｡•́︿•̀｡)", "(╥﹏╥)", "(￣o￣) . z Z", "(－‸ლ)", "(・_・ヾ",
            "(￣ρ￣)..zzZZ", "(¬､¬)", "(￣□￣」)", "(－.－)...zzz", "(◡_◡)", "(￣.￣)",
            "(－‿－)", "(∪｡∪)｡｡｡zzz", "(︶︹︺)", "(－_－)ノ", "(￣～￣;)", "(・_・)",
        )
        "BRUNHILDE", "QIRA", "THOR", "SHAKA", "LUCIFER", "ATLAS" -> listOf(
            "ᕦ(ò_óˇ)ᕤ", "( •̀ - •́ )", "(ง'̀-'́)ง", "(￣^￣)ゞ", "(¬_¬)", "(╬ಠ益ಠ)",
            "( •̀ᄇ• ́)ﻭ✧", "୧(ಠ益ಠ)୨", "(ง •̀ω•́)ง✧", "(｀･ω･´)ゞ", "(ಠ益ಠ)", "(งಠ_ಠ)ง",
            "(ಠ‿ಠ)", "୧(•̀ᗝ•́)૭", "(ง ͠° ͟ل͜ ͡°)ง", "ᕙ(⇀‸↼‶)ᕗ", "( •̀ᴗ•́ )و", "(￣へ￣)",
            "(ง •̀_•́)ง", "(｀Д´)ゞ", "(ಠ_ಠ)ノ", "ᕦ(ಠ_ಠ)ᕤ", "(ง ͡ʘ ͜ʖ ͡ʘ)ง", "(｀･д･´)ゞ",
        )
        else -> listOf(
            "(⊙_◎)", "( •̀ᴗ•́ )و", "(￣ー￣)", "(－‸ლ)", "(¬‿¬)", "(◎_◎;)",
            "( •_•)>⌐■-■", "(⌐■_■)", "(⊙﹏⊙)", "(￣▽￣)ゞ", "(ﾉﾟ0ﾟ)ﾉ~", "(¬､¬)",
            "(¬_¬)ﾉ", "(￣^￣)ノ", "(・_・;)", "(⊙_⊙;)ゞ", "(☉_☉)", "(￣ω￣;)",
            "(｡•̀ᴗ-)✧", "(ಠ‿ಠ)", "(・∀・)", "(￣▽￣)", "(•‿•)", "(｀･ω･´)",
        )
    }

    private fun stableHex(text: String): String {
        var h = 0x811C9DC5.toInt()
        for (c in text) { h = h xor c.code; h *= 16777619 }
        return (h and Int.MAX_VALUE).toString(16)
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var h = 0x811C9DC5.toInt()
        for (c in text) { h = h xor c.code; h *= 16777619 }
        return (h and Int.MAX_VALUE) % size
    }
}

object RavenEmojiReservoirOS {
    fun variant(context: Context, owner: String, role: String, canonical: String, seed: String, turn: Int): String {
        if (canonical.isBlank()) return ""
        val options = alternatives(canonical).distinct()
        if (options.size <= 1) return canonical
        val dao = RavenDialogueVaultDatabase.get(context).dao()
        val now = System.currentTimeMillis()
        val scored = options.map { glyph ->
            val id = "E:${owner.uppercase()}:${role.uppercase()}:${glyph.codePoints().toArray().joinToString("-")}"
            val old = dao.expression(id)
            val age = if (old == null) Long.MAX_VALUE else now - old.lastAt
            val penalty = if (age < 60_000L) 80 else if (age < 8L * 60_000L) 18 else 0
            Triple(glyph, id, 40 - penalty - (old?.useCount ?: 0).coerceAtMost(12) + stableIndex("$seed|$glyph", 9))
        }
        val high = scored.maxOf { it.third }
        val pool = scored.filter { it.third >= high - 2 }
        val pick = pool[stableIndex("$seed|emoji|$role", pool.size)]
        val old = dao.expression(pick.second)
        dao.putExpression(RavenExpressionUseEntity(pick.second, "EMOJI_${owner.uppercase()}", role.uppercase(), (old?.useCount ?: 0) + 1, now, turn))
        return pick.first
    }

    private fun alternatives(glyph: String): List<String> = when (glyph) {
        "🤖" -> listOf("🤖", "💬", "🧠")
        "🌐" -> listOf("🌐", "🧭", "🔗")
        "🧵" -> listOf("🧵", "💬", "📣")
        "💻" -> listOf("💻", "🧪", "🛠️")
        "⌨️" -> listOf("⌨️", "✍️", "📝")
        "⚙️" -> listOf("⚙️", "🛠️", "🔧")
        "📱" -> listOf("📱", "🪟", "🧩")
        "🏠" -> listOf("🏠", "🛋️", "🗝️")
        "🎵" -> listOf("🎵", "🎧", "🎶")
        "📺" -> listOf("📺", "🎬", "🍿")
        "✉️" -> listOf("✉️", "📨", "💌")
        "💬" -> listOf("💬", "🗨️", "📣")
        "📁" -> listOf("📁", "🗂️", "📚")
        "🖼️" -> listOf("🖼️", "🎨", "👁️")
        "📷" -> listOf("📷", "📸", "🎞️")
        "🛍️" -> listOf("🛍️", "🏪", "📦")
        "✍️" -> listOf("✍️", "📝", "⌨️")
        "🔎" -> listOf("🔎", "🕵️", "👓")
        "🛠️" -> listOf("🛠️", "🔧", "⚙️")
        "🧪" -> listOf("🧪", "🔬", "🛠️")
        "🎧" -> listOf("🎧", "🎵", "🎶")
        "🍿" -> listOf("🍿", "📺", "🎬")
        "🧭" -> listOf("🧭", "🌐", "🗺️")
        "📖" -> listOf("📖", "👓", "📰")
        "👀" -> listOf("👀", "👁️", "🔭")
        "🪞" -> listOf("🪞", "👁️", "🎭")
        "📸" -> listOf("📸", "🪞", "🎞️")
        "🔁" -> listOf("🔁", "↩️", "🧱")
        "🎯" -> listOf("🎯", "👆", "📌")
        "👆" -> listOf("👆", "🎯", "☝️")
        "↕️" -> listOf("↕️", "📜", "🧭")
        "✅" -> listOf("✅", "🏁", "✨")
        "🐛" -> listOf("🐛", "🧪", "🔧")
        "🚨" -> listOf("🚨", "🪞", "⚠️")
        "👑" -> listOf("👑", "📜", "🏛️")
        "🧱" -> listOf("🧱", "↩️", "🔁")
        "⚡" -> listOf("⚡", "📈", "🔥")
        "🏁" -> listOf("🏁", "✅", "📜")
        "🤝" -> listOf("🤝", "💬", "🔗")
        "📈" -> listOf("📈", "⚡", "🔁")
        else -> listOf(glyph)
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var h = 0x811C9DC5.toInt()
        for (c in text) { h = h xor c.code; h *= 16777619 }
        return (h and Int.MAX_VALUE) % size
    }
}
