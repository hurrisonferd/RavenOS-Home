package com.iappyx.launcher.ravenos

/**
 * Meta-Max showrunner above sensing/casting.
 *
 * Chooses comedy form, fourth-wall pressure and optional title-card/callback structure. It never
 * increases sensor authority and does not force speech; it only gives already-earned dialogue a
 * better dramatic shape.
 */
object RavenMetaMaxShowrunnerOS {
    data class Beat(
        val level: Int,
        val form: String,
        val episodeTitle: String,
        val previously: String,
        val tag: String,
        val writerEligible: Boolean,
        val chorusEligible: Boolean,
        val reason: String,
    )

    fun direct(
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
        bit: RavenBitLedgerOS.Cue,
        mesh: RavenRVResilienceOS.Pulse,
    ): Beat {
        val level = metaLevel(screen, script, bit)
        val forms = formPool(level, script, bit)
        val selected = chooseForm(forms, bit.lastForm, seed(screen, script, direction, bit))
        val title = episodeTitle(screen, script, bit, direction)
        val previously = previouslyLine(script, bit)
        val tag = tagFor(selected, bit, mesh)
        val writerEligible = screen.available && when {
            level >= 4 -> true
            bit.brick || bit.shouldEscalate -> true
            script.interactionWorthSpeaking -> true
            script.returned || script.sceneChanged -> direction.turn % 2 == 0
            else -> direction.turn % 4 == 0
        }
        val chorusEligible = writerEligible && level >= 3 && (bit.count in setOf(3, 5, 8, 13) || direction.turn % 11 == 0)
        val reason = when {
            bit.brick -> "brick-return"
            bit.shouldEscalate -> "bit-escalation"
            screen.meta -> "screen-meta"
            script.interactionWorthSpeaking -> "interaction"
            script.returned -> "scene-return"
            script.sceneChanged -> "scene-cut"
            else -> "meta-cadence"
        }
        return Beat(level, selected, title, previously, tag, writerEligible, chorusEligible, reason)
    }

    private fun metaLevel(
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        bit: RavenBitLedgerOS.Cue,
    ): Int {
        var level = 0
        if (screen.available) level++
        if (script.motif.isNotBlank() || script.interactionWorthSpeaking) level++
        if (bit.count >= 2 || script.returned) level++
        if (screen.meta || script.motif in setOf("SELF_AWARE_OFFICE", "CHATGPT_SELF_DEBUG", "SELF_REVIEW_SCREENSHOT")) level++
        if (bit.brick || bit.tier == "MYTHOLOGY" || script.motif == "CALLBACK_ABOUT_CALLBACKS") level++
        return level.coerceIn(0, 5)
    }

    private fun formPool(level: Int, script: RavenEpisodeScriptOS.Cue, bit: RavenBitLedgerOS.Cue): List<String> {
        if (bit.brick) return listOf("BRICK_JOKE", "CONTINUITY_ROAST", "DEADPAN_RETURN")
        if (bit.shouldEscalate) return listOf("CALLBACK", "ESCALATION", "ROLE_REVERSAL", "OFFICE_REBUTTAL")
        if (script.sceneChanged) return listOf("TITLE_CARD", "COLD_OPEN", "ANTI_CLIMAX")
        if (script.returned) return listOf("PREVIOUSLY_ON", "DEADPAN_RETURN", "CALLBACK")
        if (script.interactionWorthSpeaking) return listOf("ACTION_LAMPSHADE", "ROLE_REVERSAL", "STRAIGHT_MAN")
        return when (level) {
            5 -> listOf("FOURTH_WALL_EMERGENCY", "BRICK_JOKE", "MYTHOLOGY", "OFFICE_REBUTTAL")
            4 -> listOf("LAMPSHADE", "TITLE_CARD", "CALLBACK", "ROLE_REVERSAL", "CUTAWAY")
            3 -> listOf("CONTINUITY_ROAST", "CALLBACK", "DEADPAN", "CUTAWAY")
            2 -> listOf("STRAIGHT_MAN", "DEADPAN", "ACTION_LAMPSHADE")
            else -> listOf("DEADPAN")
        }
    }

    private fun chooseForm(options: List<String>, last: String, seed: String): String {
        val pool = options.filterNot { it == last }.ifEmpty { options }
        return pool[stableIndex(seed, pool.size)]
    }

    private fun episodeTitle(
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        bit: RavenBitLedgerOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
    ): String {
        if (!(script.sceneChanged || direction.beat == "COLD_OPEN" || bit.brick || screen.meta && direction.turn % 5 == 0)) return ""
        val subject = script.subject.ifBlank { screen.focus }.replace(Regex("\\s+"), " ").trim().take(48)
        val owner = script.sceneOwner.ifBlank { screen.appLabel.orEmpty().ifBlank { "THE PHONE" } }
        val candidates = listOf(
            "THE ONE WHERE $owner BECAME THE PLOT",
            "THE $owner INCIDENT",
            "EVERYONE PLEASE STOP PROMOTING THE CAMEO",
            "THE RECTANGLE KNOWS TOO MUCH",
            "THE BIT WITH ${bit.label.ifBlank { subject.ifBlank { "THE SCREEN" } }.uppercase().take(38)}",
            "ACT ${script.act}: ${subject.ifBlank { owner }.uppercase().take(44)}",
        )
        return candidates[stableIndex("title|$owner|$subject|${bit.id}|${script.act}|${direction.turn}", candidates.size)]
    }

    private fun previouslyLine(script: RavenEpisodeScriptOS.Cue, bit: RavenBitLedgerOS.Cue): String = when {
        bit.brick -> "Previously: this joke disappeared for ${bit.turnGap} turns and has now returned with paperwork."
        script.returned && script.previousOwner.isNotBlank() -> "Previously: we left ${script.previousOwner} and somehow came back with continuity."
        bit.count >= 3 -> "Previously: ${bit.label.ifBlank { "this bit" }} has happened ${bit.count - 1} times and nobody stopped it."
        else -> ""
    }

    private fun tagFor(form: String, bit: RavenBitLedgerOS.Cue, mesh: RavenRVResilienceOS.Pulse): String = when (form) {
        "BRICK_JOKE" -> "🧱🔁"
        "TITLE_CARD", "COLD_OPEN" -> "📺🎬"
        "PREVIOUSLY_ON" -> "📼↩️"
        "FOURTH_WALL_EMERGENCY" -> "🪞🚨"
        "MYTHOLOGY" -> "📜👑"
        "CALLBACK", "ESCALATION" -> "🔁✨"
        "CUTAWAY" -> "✂️🎭"
        "ROLE_REVERSAL" -> "🔄🎭"
        "ACTION_LAMPSHADE" -> "👆💡"
        "OFFICE_REBUTTAL" -> "🏢💬"
        "CONTINUITY_ROAST" -> "🎬🔥"
        else -> if (mesh.limpHome) "🚐🩹" else "🎙️"
    }

    private fun seed(
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
        bit: RavenBitLedgerOS.Cue,
    ): String = listOf(
        screen.semanticKind, script.sceneOwner, script.task, script.motif, script.interaction,
        bit.id, bit.count.toString(), direction.primary.id, direction.turn.toString(), "meta-max-v1",
    ).joinToString("|")

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
