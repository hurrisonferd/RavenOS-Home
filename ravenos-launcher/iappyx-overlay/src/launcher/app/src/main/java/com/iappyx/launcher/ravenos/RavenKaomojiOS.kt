package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * KaomojiOS v2 — owner-native expression direction rather than a tiny face lookup table.
 *
 * Inputs are structural scene state only. The selected expression is deterministic, owner-native,
 * scene/posture aware, and anti-repetitive through the local expression-use vault. No screen text is
 * persisted by this organ; only authored expression ids/counters are recorded.
 */
object RavenKaomojiOS {
    data class Expression(
        val face: String,
        val posture: String,
        val intensity: Int,
        val reason: String,
    )

    fun direct(
        context: Context,
        member: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
        script: RavenEpisodeScriptOS.Cue?,
        bit: RavenBitLedgerOS.Cue?,
        show: RavenMetaMaxShowrunnerOS.Beat?,
        season: RavenOfficeSeasonOS.Memory?,
        reserve: RavenEgoReserveProjectionOS.Reserve?,
        gold: RavenGoldSitcomTopologyOS.Beat?,
        fallback: String,
    ): Expression {
        val graph = runCatching { RavenSceneGraphOS.observe(context, screen) }.getOrNull()
        val posture = posture(member.id, screen, direction, script, bit, show, gold, graph)
        val intensity = intensity(screen, direction, script, bit, show, gold)
        val seed = listOf(
            member.id,
            posture,
            intensity.toString(),
            graph?.sceneType.orEmpty(),
            graph?.task.orEmpty(),
            graph?.selectedClass.orEmpty(),
            graph?.interaction.orEmpty(),
            direction.sceneId,
            direction.turn.toString(),
            direction.beat,
            script?.motif.orEmpty(),
            show?.form.orEmpty(),
            gold?.phase.orEmpty(),
            reserve?.state.orEmpty(),
            (season?.episode ?: 0).toString(),
        ).joinToString("|")
        val candidates = buildList {
            addAll(ownerPosture(member.id, posture))
            addAll(postureFaces(posture))
            addAll(ownerNative(member.id))
            if (fallback.isNotBlank()) add(fallback)
        }.filter(String::isNotBlank).distinct()

        if (candidates.isEmpty()) return Expression(fallback, posture, intensity, "fallback")
        val dao = runCatching { RavenDialogueVaultDatabase.get(context).dao() }.getOrNull()
        val now = System.currentTimeMillis()
        val scored = candidates.map { face ->
            val id = "K2:${member.id.uppercase()}:$posture:${stableHex(face)}"
            val old = runCatching { dao?.expression(id) }.getOrNull()
            val age = if (old == null) Long.MAX_VALUE else now - old.lastAt
            val recencyPenalty = when {
                age < 30_000L -> 180
                age < 2L * 60_000L -> 70
                age < 10L * 60_000L -> 24
                age < 45L * 60_000L -> 8
                else -> 0
            }
            val usePenalty = (old?.useCount ?: 0).coerceAtMost(40) * 2
            val ownerBonus = if (face in ownerPosture(member.id, posture)) 24 else if (face in ownerNative(member.id)) 10 else 0
            val postureBonus = if (face in postureFaces(posture)) 14 else 0
            val deterministic = stableIndex("$seed|$face", 31)
            Candidate(face, id, 100 + ownerBonus + postureBonus + deterministic - recencyPenalty - usePenalty)
        }
        val best = scored.maxOf { it.score }
        val pool = scored.filter { it.score >= best - 5 }
        val pick = pool[stableIndex("$seed|kaomojios-v2", pool.size)]
        if (dao != null) {
            val old = runCatching { dao.expression(pick.id) }.getOrNull()
            runCatching {
                dao.putExpression(
                    RavenExpressionUseEntity(
                        expressionId = pick.id,
                        owner = member.id.uppercase(),
                        mood = posture,
                        useCount = (old?.useCount ?: 0) + 1,
                        lastAt = now,
                        lastTurn = direction.turn,
                    ),
                )
            }
        }
        return Expression(pick.face, posture, intensity, reason(posture, graph, direction))
    }

    private data class Candidate(val face: String, val id: String, val score: Int)

    private fun posture(
        owner: String,
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
        script: RavenEpisodeScriptOS.Cue?,
        bit: RavenBitLedgerOS.Cue?,
        show: RavenMetaMaxShowrunnerOS.Beat?,
        gold: RavenGoldSitcomTopologyOS.Beat?,
        graph: RavenSceneGraphOS.Graph?,
    ): String = when {
        (show?.level ?: 0) >= 5 || show?.form == "FOURTH_WALL_EMERGENCY" -> "RECURSIVE"
        bit?.brick == true -> "BRICK"
        direction.beat == "BUG" -> if (owner in hardOwners) "FURIOUS" else "ALARM"
        direction.beat == "PAYOFF" || gold?.phase == "CLOSE" -> "VICTORY"
        screen.meta || direction.beat == "META" || (show?.level ?: 0) >= 3 -> "META"
        gold?.phase == "ESCALATE" || bit?.shouldEscalate == true -> "CHAOS"
        gold?.phase == "CALLBACK" || direction.beat == "CALLBACK" -> "CALLBACK"
        direction.beat == "CROSSTALK" -> "CROSSTALK"
        graph?.interaction == "SELECT" -> "DECISIVE"
        graph?.interaction == "SCROLL" -> if (graph.task in setOf("READING", "READING_CHAT", "BROWSING")) "READING" else "FOCUSED"
        graph?.task in setOf("COMPOSING", "TYPING") -> "COMPOSING"
        graph?.task in setOf("DEBUGGING", "CONFIGURING") || screen.semanticKind in setOf("CODE", "TERMINAL", "SETTINGS") -> "ANALYTIC"
        graph?.task == "LISTENING" || screen.semanticKind == "MUSIC" -> "MUSIC"
        graph?.task == "WATCHING" || screen.semanticKind == "VIDEO" -> "WATCHING"
        graph?.task in setOf("READING", "READING_CHAT", "BROWSING") -> "READING"
        script?.returned == true -> "RECOGNITION"
        script?.interruption?.isNotBlank() == true -> "SKEPTICAL"
        direction.beat == "COLD_OPEN" -> "CURIOUS"
        direction.beat == "OBSERVE" && owner in quietOwners -> "QUIET"
        direction.beat == "OBSERVE" -> "ATTENTIVE"
        else -> "SMUG"
    }

    private fun intensity(
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
        script: RavenEpisodeScriptOS.Cue?,
        bit: RavenBitLedgerOS.Cue?,
        show: RavenMetaMaxShowrunnerOS.Beat?,
        gold: RavenGoldSitcomTopologyOS.Beat?,
    ): Int = (1 +
        (if (screen.meta) 1 else 0) +
        (if ((show?.level ?: 0) >= 4) 1 else 0) +
        (if (bit?.brick == true || bit?.shouldEscalate == true) 1 else 0) +
        (if (gold?.phase == "ESCALATE" || direction.beat in setOf("BUG", "PAYOFF")) 1 else 0) +
        (if (script?.interactionWorthSpeaking == true) 1 else 0)
        ).coerceIn(1, 5)

    private fun reason(posture: String, graph: RavenSceneGraphOS.Graph?, direction: RavenSitcomDirectorOS.Direction): String =
        "$posture:${graph?.task ?: "VIEWING"}:${graph?.interaction ?: "NONE"}:${direction.beat}"

    private val hardOwners = setOf("BRUNHILDE", "THOR", "QIRA", "LUCIFER", "SHAKA", "ATLAS")
    private val quietOwners = setOf("NYX", "MELINOE", "EREBUS", "AHTI", "VIRGIL")

    private fun ownerPosture(owner: String, posture: String): List<String> = when (owner.uppercase()) {
        "KYU" -> when (posture) {
            "META", "RECURSIVE" -> listOf("(╯✧▽✧)╯", "(ﾉ≧∀≦)ﾉ", "(☞ﾟ∀ﾟ)☞", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧")
            "ALARM", "FURIOUS" -> listOf("(ง •̀_•́)ง", "୧(•̀ᗝ•́)૭", "(ﾉಥ益ಥ）ﾉ", "(╬ Ò﹏Ó)")
            "VICTORY" -> listOf("＼(≧▽≦)／", "(๑˃ᴗ˂)ﻭ", "(づ｡◕‿‿◕｡)づ")
            "SKEPTICAL" -> listOf("(¬_¬)", "(ಠ_ಠ)", "(￣へ￣)")
            else -> listOf("(ง •̀_•́)ง", "(☆▽☆)", "(≧◡≦)", "(ﾉﾟ▽ﾟ)ﾉ")
        }
        "JOKER" -> when (posture) {
            "META", "RECURSIVE", "BRICK" -> listOf("(☞ﾟヮﾟ)☞", "(¬‿¬)", "(☞ ՞ਊ ՞)☞", "(ಠ‿ಠ)")
            "QUIET" -> listOf("(¬_¬)", "(￣ー￣)", "( •_•)")
            "VICTORY" -> listOf("(⌐■_■)", "( •_•)>⌐■-■", "(￣▽￣)ゞ")
            else -> listOf("(¬‿¬)", "(☞ﾟ∀ﾟ)☞", "(⌐■_■)")
        }
        "ATOM" -> when (posture) {
            "ANALYTIC", "FOCUSED" -> listOf("( •̀ ω •́ )✧", "(￢_￢)", "(⌐■_■)", "( •_•)>⌐■-■")
            "RECURSIVE", "META" -> listOf("(⊙_◎)", "(☉_☉)", "(¬_¬)ﾉ⌐■-■")
            "VICTORY" -> listOf("(￣ー￣)ゞ", "( •̀ᴗ•́ )و", "(๑•̀ㅂ•́)و✧")
            else -> listOf("(￢_￢)", "( •̀_•́ )", "(￣^￣)ゞ")
        }
        "PAIMON" -> when (posture) {
            "SKEPTICAL", "ANALYTIC" -> listOf("(•ิ_•ิ)?", "(￢_￢)", "(ಠ_ಠ)", "(¬､¬)")
            "META" -> listOf("(☉_☉)", "(⊙_◎)", "(ಠ‿ಠ)")
            "VICTORY" -> listOf("( •̀ᴗ•́ )و", "(￣▽￣)ゞ", "(｡•̀ᴗ-)✧")
            else -> listOf("(•ิ_•ิ)?", "(・_・ヾ", "(￢_￢)")
        }
        "YORI" -> when (posture) {
            "DECISIVE", "FOCUSED" -> listOf("(｡•̀ᴗ-)✧", "(ﾉ´ヮ`)ﾉ*:･ﾟ", "( •̀ᴗ•́ )و")
            "MUSIC" -> listOf("ヾ(⌐■_■)ノ♪", "(〜￣▽￣)〜", "(ﾉ´ヮ`)ﾉ*:･ﾟ")
            "SKEPTICAL" -> listOf("(¬_¬)", "(￣～￣;)", "(・_・ヾ")
            else -> listOf("(｡•̀ᴗ-)✧", "(￣▽￣)~*", "(＾▽＾)")
        }
        "LILITH" -> when (posture) {
            "ATTENTIVE", "READING", "QUIET" -> listOf("(˵ •̀ ᴗ - ˵ ) ✧", "(｡•́‿•̀｡)", "(◡‿◡✿)", "(っ˘ω˘ς )")
            "SKEPTICAL" -> listOf("(¬_¬)", "(￣へ￣)", "(｡•́︿•̀｡)")
            "META" -> listOf("(｡•̀ᴗ-)✧", "(◕ᴗ◕✿)", "(￣▽￣)~*")
            else -> listOf("(˵ •̀ ᴗ - ˵ ) ✧", "(◕‿◕✿)", "(ღ˘⌣˘ღ)")
        }
        "NYX" -> when (posture) {
            "QUIET" -> listOf("(－_－) zzZ", "(￣ρ￣)..zzZZ", "(∪｡∪)｡｡｡zzz", "(◡﹏◡)")
            "SKEPTICAL" -> listOf("(¬_¬ )", "(－‸ლ)", "(￣～￣;)")
            else -> listOf("(◡_◡)", "(－‿－)", "(￣.￣)")
        }
        "MELINOE" -> when (posture) {
            "META", "RECOGNITION" -> listOf("(幽_幽)", "(◡﹏◡)", "(－ω－)")
            "QUIET" -> listOf("(－.－)...zzz", "(￣o￣) . z Z", "(◡_◡)")
            else -> listOf("(幽_幽)", "(・_・)", "(¬_¬ )")
        }
        "THOR", "BRUNHILDE", "QIRA", "LUCIFER", "SHAKA", "ATLAS" -> when (posture) {
            "FURIOUS", "ALARM" -> listOf("(╬ಠ益ಠ)", "୧(ಠ益ಠ)୨", "(งಠ_ಠ)ง", "ᕦ(ò_óˇ)ᕤ")
            "VICTORY" -> listOf("(｀･ω･´)ゞ", "( •̀ᴗ•́ )و", "(￣^￣)ゞ")
            "SKEPTICAL" -> listOf("(¬_¬)", "(ಠ_ಠ)ノ", "(￣へ￣)")
            else -> listOf("( •̀ - •́ )", "(ง'̀-'́)ง", "(｀･д･´)ゞ")
        }
        else -> emptyList()
    }

    private fun postureFaces(posture: String): List<String> = when (posture) {
        "RECURSIVE" -> listOf("(⊙_◎)", "(☉_☉)", "(ﾉﾟ0ﾟ)ﾉ~", "(¬‿¬)", "(✧ω✧)")
        "META" -> listOf("(☞ﾟヮﾟ)☞", "(ಠ‿ಠ)", "(⊙_◎)", "(¬_¬)ﾉ⌐■-■")
        "BRICK" -> listOf("(￣ー￣)", "(¬‿¬)", "(⌐■_■)", "(ಠ‿ಠ)")
        "CALLBACK", "RECOGNITION" -> listOf("(￣ー￣)ゞ", "(｡•̀ᴗ-)✧", "( •̀ᴗ•́ )و", "(｀･ω･´)ゞ")
        "VICTORY" -> listOf("＼(≧▽≦)／", "(๑˃ᴗ˂)ﻭ", "(✧ω✧)", "(￣▽￣)ゞ")
        "FURIOUS" -> listOf("(╬ಠ益ಠ)", "୧(ಠ益ಠ)୨", "(งಠ_ಠ)ง", "(ಠ益ಠ)")
        "ALARM" -> listOf("ヽ(°〇°)ﾉ", "(⊙_⊙;)", "(◎_◎;)", "(￣□￣」)")
        "CHAOS" -> listOf("ᕕ( ᐛ )ᕗ", "(ﾉ≧∀≦)ﾉ", "(☞ ՞ਊ ՞)☞", "(ﾉﾟ0ﾟ)ﾉ~")
        "CROSSTALK" -> listOf("(¬‿¬)", "(｡•̀ᴗ-)✧", "(・∀・)ノ", "(￣▽￣)ノ")
        "DECISIVE" -> listOf("( •̀ᴗ•́ )و", "(๑•̀ㅂ•́)و✧", "(｀･ω･´)ゞ", "(｡•̀ᴗ-)✧")
        "COMPOSING" -> listOf("(๑•̀ㅂ•́)و✧", "( •̀ ω •́ )✧", "(ง •̀_•́)ง", "(｀･ω･´)ゞ")
        "ANALYTIC" -> listOf("( •̀ ω •́ )✧", "(￢_￢)", "(⌐■_■)", "( •_•)>⌐■-■", "(☉_☉)")
        "SKEPTICAL" -> listOf("(¬_¬)", "(ಠ_ಠ)", "(￣へ￣)", "(•ิ_•ิ)?", "(－‸ლ)")
        "READING" -> listOf("(・_・ヾ", "(｡•́‿•̀｡)", "(￣ー￣)", "(－ω－)", "(•‿•)")
        "MUSIC" -> listOf("ヾ(⌐■_■)ノ♪", "(〜￣▽￣)〜", "ヽ(o＾▽＾o)ノ", "♪(┌・。・)┌")
        "WATCHING" -> listOf("(☉_☉)", "(・∀・)", "(⊙_⊙)", "(￣▽￣)")
        "CURIOUS" -> listOf("(•ิ_•ิ)?", "(・_・ヾ", "(⊙_⊙)", "(｡･ω･｡)")
        "ATTENTIVE", "FOCUSED" -> listOf("( •̀_•́ )", "(・_・)", "(｀･ω･´)", "(｡•̀ᴗ-)✧")
        "QUIET" -> listOf("(◡﹏◡)", "(－_－) zzZ", "(っ˘ω˘ς )", "(◡‿◡✿)")
        else -> listOf("(￣ー￣)", "(¬‿¬)", "(•‿•)", "(｡•̀ᴗ-)✧")
    }

    private fun ownerNative(owner: String): List<String> = when (owner.uppercase()) {
        "KYU", "JOKER", "MYSTRA", "ASTRIDHE" -> listOf("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(☞ﾟヮﾟ)☞", "(ง •̀_•́)ง", "(☆▽☆)", "(¬‿¬)")
        "ATOM", "PAIMON", "PYTHAGORAS", "EDISON", "NEO", "TIM", "JARVIS" -> listOf("( •̀ ω •́ )✧", "(￢_￢)", "(⌐■_■)", "(￣ー￣)ゞ", "(☉_☉)")
        "LILITH", "LUMA", "AYRE", "YORK", "YORI", "SYLPH" -> listOf("(˵ •̀ ᴗ - ˵ ) ✧", "(◕‿◕✿)", "(￣▽￣)~*", "(｡•̀ᴗ-)✧", "(ღ˘⌣˘ღ)")
        "MELINOE", "NYX", "EREBUS", "VIRGIL", "AHTI" -> listOf("(◡﹏◡)", "(¬_¬ )", "(－_－) zzZ", "(幽_幽)", "(－‿－)")
        "BRUNHILDE", "QIRA", "THOR", "SHAKA", "LUCIFER", "ATLAS" -> listOf("ᕦ(ò_óˇ)ᕤ", "( •̀ - •́ )", "(ง'̀-'́)ง", "(￣^￣)ゞ", "(¬_¬)")
        else -> listOf("(⊙_◎)", "( •̀ᴗ•́ )و", "(￣ー￣)", "(¬‿¬)", "(・_・)")
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
