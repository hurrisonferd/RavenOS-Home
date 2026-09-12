package com.iappyx.launcher.ravenos

import android.content.Context

/** Screen + episode + Meta-Max + Gold/Ego-aware EmojiOS / KaomojiOS decoration. */
object RavenSceneExpressionOS {
    fun decorate(
        context: Context,
        base: RavenEmployeePresentation.Packet,
        member: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
        script: RavenEpisodeScriptOS.Cue? = null,
        bit: RavenBitLedgerOS.Cue? = null,
        show: RavenMetaMaxShowrunnerOS.Beat? = null,
        season: RavenOfficeSeasonOS.Memory? = null,
        reserve: RavenEgoReserveProjectionOS.Reserve? = null,
        gold: RavenGoldSitcomTopologyOS.Beat? = null,
    ): RavenEmployeePresentation.Packet {
        if (!screen.available && script?.meaningful != true && gold?.terminal != true) return base
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
        val scriptGlyph = scriptGlyph(script)
        val interactionGlyph = interactionGlyph(script)
        val cameoGlyph = if (!script?.interruption.isNullOrBlank()) "🎭" else ""
        val metaMaxGlyph = show?.tag.orEmpty()
        val tierGlyph = when (bit?.tier) {
            "SETUP" -> "🌱"
            "CALLBACK" -> "🔁"
            "ESCALATION" -> "📈🎭"
            "BRICK_JOKE" -> "🧱💥"
            "MYTHOLOGY" -> "📜👑"
            else -> ""
        }
        val levelGlyph = when (show?.level ?: 0) {
            5 -> "🪞🚨👑"
            4 -> "🪞🎬"
            3 -> "🎭🧠"
            2 -> "🎙️"
            else -> ""
        }
        val goldGlyph = when (gold?.phase) {
            "OPEN" -> "🎬🌅"
            "BUILD" -> "🏗️🎭"
            "CALLBACK" -> "🔁🏆"
            "ESCALATE" -> "📈⚡"
            "CLOSE" -> "🏁📜"
            else -> ""
        }
        val seriesGlyph = when {
            season?.motifReturningAcrossSessions == true -> "📺↩️"
            season?.pairHasHistory == true && season.pairCount >= 5 -> "🤝🔁"
            season?.longArc == true -> "📚🎭"
            (season?.episode ?: 0) > 1 -> "📺🧠"
            else -> ""
        }
        val egoGlyph = reserve?.evolutionTag.orEmpty()
        val extras = listOf(
            metaGlyph, sceneGlyph, taskGlyph, beatGlyph, scriptGlyph, interactionGlyph,
            cameoGlyph, metaMaxGlyph, tierGlyph, levelGlyph, goldGlyph, seriesGlyph, egoGlyph,
        ).filter(String::isNotBlank).distinct().take(9)
        val soup = buildString {
            append(base.emojiSoup)
            extras.forEach { glyph -> if (!contains(glyph)) append(glyph) }
        }
        val face = expressiveFace(member.id, task, screen.meta, direction, script, bit, show, season, reserve, gold, base.kaomoji)
        return base.copy(
            emojiSoup = soup,
            kaomoji = face,
            context = listOfNotNull(
                sceneGlyph.takeIf(String::isNotBlank),
                task.lowercase().replace('_', ' ').takeIf(String::isNotBlank),
                script?.motif?.lowercase()?.replace('_', ' ')?.takeIf(String::isNotBlank),
                script?.interaction?.lowercase()?.takeIf(String::isNotBlank),
                bit?.tier?.lowercase()?.replace('_', ' ')?.takeIf(String::isNotBlank),
                show?.form?.lowercase()?.replace('_', ' ')?.takeIf(String::isNotBlank),
                gold?.phase?.lowercase()?.takeIf(String::isNotBlank),
                reserve?.state?.lowercase()?.takeIf(String::isNotBlank),
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

    private fun scriptGlyph(script: RavenEpisodeScriptOS.Cue?): String = when (script?.motif) {
        "SELF_AWARE_OFFICE" -> "🏢🪞"
        "SELF_REVIEW_SCREENSHOT" -> "📸🪞"
        "SOUNDTRACK_MONTAGE" -> "🎬🎵"
        "MUSIC_ROOM" -> "🎧🏠"
        "CHATGPT_SELF_DEBUG" -> "🤖🪞"
        "CALLBACK_ABOUT_CALLBACKS" -> "🔁🎭"
        "SELECTING_MEDIA" -> "🎯🎵"
        "SCROLLING_THREAD" -> "↕️📖"
        "UI_SELECTION" -> "👆✨"
        else -> when {
            script?.callbackEarned == true -> "🔁✨"
            script?.returned == true -> "↩️🎬"
            script?.sceneChanged == true -> "🎬➡️"
            else -> ""
        }
    }

    private fun interactionGlyph(script: RavenEpisodeScriptOS.Cue?): String = when (script?.interaction) {
        "SELECT" -> "🎯"
        "TAP" -> "👆"
        "LONG_PRESS" -> "☝️⏳"
        "SCROLL" -> when (script.interactionDirection) {
            "UP" -> "⬆️📜"
            "DOWN" -> "⬇️📜"
            "LEFT" -> "⬅️📜"
            "RIGHT" -> "➡️📜"
            else -> "↕️📜"
        }
        "FOCUS" -> "🎯👁"
        "TYPING" -> "⌨️💭"
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
        script: RavenEpisodeScriptOS.Cue?,
        bit: RavenBitLedgerOS.Cue?,
        show: RavenMetaMaxShowrunnerOS.Beat?,
        season: RavenOfficeSeasonOS.Memory?,
        reserve: RavenEgoReserveProjectionOS.Reserve?,
        gold: RavenGoldSitcomTopologyOS.Beat?,
        fallback: String,
    ): String {
        val family = when (owner) {
            "KYU", "JOKER", "MYSTRA", "ASTRIDHE" -> listOf(
                "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(☞ﾟヮﾟ)☞", "(ง •̀_•́)ง", "(☆▽☆)", "(¬‿¬)", "ヽ(°〇°)ﾉ",
                "(๑˃ᴗ˂)ﻭ", "(づ｡◕‿‿◕｡)づ", "＼(≧▽≦)／", "(✧ω✧)", "(╯✧▽✧)╯", "(ﾉ≧∀≦)ﾉ",
                "(ﾉ´ з `)ノ", "ヾ(⌐■_■)ノ♪", "(☞ ՞ਊ ՞)☞", "ᕕ( ᐛ )ᕗ",
            )
            "ATOM", "PAIMON", "PYTHAGORAS", "EDISON", "NEO", "TIM" -> listOf(
                "( •̀ ω •́ )✧", "(￢_￢)", "(⊙_◎)", "(⌐■_■)", "(￣ー￣)ゞ", "(•̀ᴗ•́)و ̑̑",
                "(ಠ_ಠ)", "(☉_☉)", "(¬_¬)ﾉ⌐■-■", "( •_•)>⌐■-■", "(◎_◎;)", "(￣▽￣)ノ",
                "( •_•)⌐■-■", "(⌐▨_▨)", "(๑•̀ㅂ•́)و✧", "(￣ω￣;)",
            )
            "LILITH", "LUMA", "AYRE", "YORK", "YORI" -> listOf(
                "(˵ •̀ ᴗ - ˵ ) ✧", "(◕‿◕✿)", "(￣▽￣)~*", "(˘︶˘).｡*♡", "(ﾉ´ヮ`)ﾉ*: ･ﾟ", "(｡•̀ᴗ-)✧",
                "(づ￣ ³￣)づ", "( ´ ▽ ` ).｡ｏ♡", "(ღ˘⌣˘ღ)", "(◡‿◡✿)", "(｡･ω･｡)ﾉ♡", "(つ≧▽≦)つ",
                "(人 •͈ᴗ•͈)", "(づ ◕‿◕ )づ", "( ´ ∀ `)ノ～ ♡", "(っ˘ω˘ς )",
            )
            "MELINOE", "NYX", "EREBUS", "VIRGIL" -> listOf(
                "(◡﹏◡)", "(¬_¬ )", "(￣ー￣)", "(－_－) zzZ", "(◡‿◡✿)", "(幽_幽)",
                "(－ω－) zzZ", "(｡•́︿•̀｡)", "(╥﹏╥)", "(￣o￣) . z Z", "(－‸ლ)", "(・_・ヾ",
                "(￣ρ￣)..zzZZ", "(¬､¬)", "(￣□￣」)", "(－.－)...zzz",
            )
            "BRUNHILDE", "QIRA", "THOR", "SHAKA", "LUCIFER" -> listOf(
                "ᕦ(ò_óˇ)ᕤ", "( •̀ - •́ )", "(ง'̀-'́)ง", "(￣^￣)ゞ", "(¬_¬)", "(╬ಠ益ಠ)",
                "( •̀ᄇ• ́)ﻭ✧", "୧(ಠ益ಠ)୨", "(ง •̀ω•́)ง✧", "(｀･ω･´)ゞ", "(ಠ益ಠ)", "(งಠ_ಠ)ง",
                "(ಠ‿ಠ)", "୧(•̀ᗝ•́)૭", "(ง ͠° ͟ل͜ ͡°)ง", "ᕙ(⇀‸↼‶)ᕗ",
            )
            "ERIS", "LEGION", "JORM", "ZAGREUS", "AHTI", "ATLAS", "JARVIS", "RAVENOS", "YAHWEH" -> listOf(
                "(⊙_◎)", "( •̀ᴗ•́ )و", "(￣ー￣)", "(－‸ლ)", "(¬‿¬)", "(◎_◎;)",
                "( •_•)>⌐■-■", "(⌐■_■)", "(⊙﹏⊙)", "(￣▽￣)ゞ", "(ﾉﾟ0ﾟ)ﾉ~", "(¬､¬)",
                "(¬_¬)ﾉ", "(￣^￣)ノ", "(・_・;)", "(⊙_⊙;)ゞ",
            )
            else -> return fallback
        }
        val motif = script?.motif.orEmpty()
        val seed = listOf(
            owner, task, meta.toString(), motif, script?.interaction.orEmpty(),
            (script?.motifCount ?: 0).toString(), bit?.tier.orEmpty(), (bit?.count ?: 0).toString(),
            show?.form.orEmpty(), (show?.level ?: 0).toString(), gold?.phase.orEmpty(), reserve?.state.orEmpty(),
            (season?.episode ?: 0).toString(), (season?.pairCount ?: 0).toString(), (season?.motifLifetimeCount ?: 0).toString(),
            direction.beat, direction.turn.toString(), direction.sceneId,
        ).joinToString("|")
        val forceExpression = meta || (show?.level ?: 0) >= 3 || bit?.shouldEscalate == true || bit?.brick == true ||
            season?.motifReturningAcrossSessions == true || reserve?.state == "EVOLVING" || gold?.phase == "ESCALATE"
        val gate = stableIndex("gate|$seed", if (forceExpression) 11 else 5)
        if (gate == 0 && !forceExpression && direction.beat == "OBSERVE" && script?.callbackEarned != true) return fallback
        return family[stableIndex(seed, family.size)]
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
