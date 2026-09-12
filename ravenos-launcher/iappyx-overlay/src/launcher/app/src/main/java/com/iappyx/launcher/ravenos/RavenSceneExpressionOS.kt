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
        val viewport = RavenViewportSemanticsOS.latest(context)
        val task = viewport?.task.orEmpty().ifBlank { inferTask(screen.semanticSummary) }
        val seed = listOf(
            member.id, task, screen.semanticKind, direction.sceneId, direction.turn.toString(),
            script?.motif.orEmpty(), script?.interaction.orEmpty(), bit?.tier.orEmpty(),
            show?.form.orEmpty(), gold?.phase.orEmpty(), reserve?.state.orEmpty(),
            (season?.episode ?: 0).toString(),
        ).joinToString("|")

        // Visual law: stable identity + ONE scene glyph + ONE earned exceptional-state glyph.
        // The reservoirs add variance inside those slots; they never increase the slot count.
        val sceneCanonical = firstNotBlank(
            interactionGlyph(script), motifGlyph(script), taskGlyph(task), sceneGlyph(screen.semanticKind),
            RavenEmojiBudgetOS.glyphCandidate(base.context),
        )
        val stateCanonical = firstNotBlank(
            exceptionalGlyph(screen, direction, bit, show, season, gold), beatGlyph(direction.beat),
        )
        val scene = RavenEmojiReservoirOS.variant(context, member.id, "SCENE", sceneCanonical, seed, direction.turn)
        val state = RavenEmojiReservoirOS.variant(context, member.id, "STATE", stateCanonical, seed, direction.turn)
        val soup = RavenEmojiBudgetOS.compose(member.id, scene, state, base.emojiSoup)
        val mood = mood(task, screen, direction, bit, show, gold)
        val face = RavenExpressionReservoirOS.select(context, member.id, mood, seed, base.kaomoji, direction.turn)

        return base.copy(
            emojiSoup = soup,
            kaomoji = face,
            context = listOfNotNull(
                screen.semanticKind.lowercase().takeIf(String::isNotBlank),
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

    private fun mood(
        task: String,
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
        bit: RavenBitLedgerOS.Cue?,
        show: RavenMetaMaxShowrunnerOS.Beat?,
        gold: RavenGoldSitcomTopologyOS.Beat?,
    ): String = when {
        screen.meta || direction.beat == "META" || (show?.level ?: 0) >= 4 -> "META"
        direction.beat == "BUG" -> "BUG"
        direction.beat == "PAYOFF" || gold?.phase == "CLOSE" -> "PAYOFF"
        bit?.brick == true || bit?.shouldEscalate == true || gold?.phase in setOf("CALLBACK", "ESCALATE") -> "CALLBACK"
        task == "LISTENING" || screen.semanticKind == "MUSIC" -> "MUSIC"
        task == "COMPOSING" || task == "TYPING" -> "COMPOSING"
        task in setOf("READING", "READING_CHAT", "BROWSING") -> "READING"
        direction.beat == "OBSERVE" -> "QUIET"
        memberAnalytic(screen.semanticKind) -> "ANALYTIC"
        else -> "SMUG"
    }

    private fun memberAnalytic(kind: String): Boolean = kind in setOf("CODE", "TERMINAL", "SETTINGS")

    private fun firstNotBlank(vararg values: String): String = values.firstOrNull(String::isNotBlank).orEmpty()

    private fun sceneGlyph(kind: String): String = when (kind.uppercase()) {
        "CHATGPT" -> "🤖"
        "BROWSER" -> "🌐"
        "COMMUNITY" -> "🧵"
        "CODE" -> "💻"
        "TERMINAL" -> "⌨️"
        "SETTINGS" -> "⚙️"
        "SYSTEM_UI" -> "📱"
        "KEYBOARD" -> "⌨️"
        "HOME" -> "🏠"
        "MUSIC" -> "🎵"
        "VIDEO" -> "📺"
        "MAIL" -> "✉️"
        "MESSAGING" -> "💬"
        "FILES" -> "📁"
        "GALLERY" -> "🖼️"
        "CAMERA" -> "📷"
        "STORE" -> "🛍️"
        else -> if (kind.isNotBlank()) "👁" else ""
    }

    private fun taskGlyph(task: String): String = when (task.uppercase()) {
        "COMPOSING" -> "✍️"
        "SEARCHING" -> "🔎"
        "CONFIGURING" -> "🛠️"
        "DEBUGGING" -> "🧪"
        "LISTENING" -> "🎧"
        "WATCHING" -> "🍿"
        "BROWSING", "WEB" -> "🧭"
        "READING_CHAT", "READING" -> "📖"
        "TYPING" -> "⌨️"
        "VIEWING" -> "👀"
        else -> ""
    }

    private fun motifGlyph(script: RavenEpisodeScriptOS.Cue?): String = when (script?.motif) {
        "SELF_AWARE_OFFICE", "CHATGPT_SELF_DEBUG" -> "🪞"
        "SELF_REVIEW_SCREENSHOT" -> "📸"
        "SOUNDTRACK_MONTAGE", "MUSIC_ROOM", "SELECTING_MEDIA" -> "🎵"
        "CALLBACK_ABOUT_CALLBACKS" -> "🔁"
        "SCROLLING_THREAD" -> "📖"
        "UI_SELECTION" -> "🎯"
        else -> when {
            script?.returned == true -> "↩️"
            script?.sceneChanged == true -> "🎬"
            else -> ""
        }
    }

    private fun interactionGlyph(script: RavenEpisodeScriptOS.Cue?): String = when (script?.interaction) {
        "SELECT" -> "🎯"
        "TAP" -> "👆"
        "LONG_PRESS" -> "☝️"
        "SCROLL" -> when (script.interactionDirection) {
            "UP" -> "⬆️"
            "DOWN" -> "⬇️"
            "LEFT" -> "⬅️"
            "RIGHT" -> "➡️"
            else -> "↕️"
        }
        "FOCUS" -> "🎯"
        "TYPING" -> "✍️"
        else -> ""
    }

    private fun beatGlyph(beat: String): String = when (beat) {
        "CALLBACK" -> "🔁"
        "BUG" -> "🐛"
        "PAYOFF" -> "✅"
        "COLD_OPEN" -> "🎬"
        "CROSSTALK" -> "💬"
        "CUTAWAY" -> "✂️"
        "META" -> "🪞"
        else -> ""
    }

    private fun exceptionalGlyph(
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
        bit: RavenBitLedgerOS.Cue?,
        show: RavenMetaMaxShowrunnerOS.Beat?,
        season: RavenOfficeSeasonOS.Memory?,
        gold: RavenGoldSitcomTopologyOS.Beat?,
    ): String = when {
        show?.level == 5 -> "🚨"
        bit?.tier == "MYTHOLOGY" -> "👑"
        bit?.tier == "BRICK_JOKE" -> "🧱"
        gold?.phase == "ESCALATE" -> "⚡"
        gold?.phase == "CLOSE" -> "🏁"
        season?.motifReturningAcrossSessions == true -> "↩️"
        season?.pairHasHistory == true && season.pairCount >= 5 -> "🤝"
        bit?.tier == "ESCALATION" -> "📈"
        gold?.phase == "CALLBACK" || bit?.tier == "CALLBACK" -> "🔁"
        screen.meta || direction.beat == "META" || (show?.level ?: 0) >= 4 -> "🪞"
        gold?.phase == "OPEN" -> "🎬"
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
}
