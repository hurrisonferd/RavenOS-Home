package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of Goblin Vision.
 * MarkerBus -> local senses -> complex event -> callback/scene fusion -> cast -> expression -> overlay.
 */
object RavenGoblinBrain {
    data class Result(val member: RavenOfficeMember, val packet: RavenReactionPacket)

    private const val CAST_PREFS = "ravenos_goblin_cast_v2"
    private const val KEY_LAST_OWNER = "last_owner"

    fun react(
        context: Context,
        signal: String,
        detail: String,
        manualOwner: String?,
        quiet: Boolean,
        hauntMode: RavenHauntMode,
    ): Result {
        val marker = RavenMarkerBus.emit(context, signal, detail)
        val sense = RavenLocalSenseOS.resolve(marker)
        val complex = RavenComplexEventOS.analyze(context, marker)
        val episode = RavenEpisodeOS.phase(context, marker, complex)
        val callback = RavenCallbackMemoryOS.observe(context, marker, complex)
        val member = cast(context, marker, complex, manualOwner, quiet)
        val meta = RavenMetaCommentaryOS.compose(context, member, marker, complex, episode)
        val sceneBeat = RavenMetaGoblinDialogueOS.select(context, member, marker, complex, episode)
        val fusedScene = RavenMetaSceneOS.compose(context, member, marker, complex, callback)
        val allowed = RavenInterruptibilityOS.allow(context, marker, complex, hauntMode, quiet)
        val visual = RavenVisualAtlas.resolve(member.id, marker, complex)
        val character = RavenDialogueBank.select(member, marker, complex, visual, episode)
        val metaPunch = RavenMetaPunchlineOS.select(member, marker)

        val truth = fusedScene.text.ifBlank { sceneBeat.text.ifBlank { meta.text } }.trim()
        val stinger = metaPunch.text.ifBlank { character.text }.trim()
        val spoken = if (!allowed) "" else buildString {
            append(truth)
            if (stinger.isNotBlank() && stinger != truth) {
                if (isNotEmpty()) append("  ")
                append(stinger)
            }
        }.replace(Regex("\\s+"), " ").trim().take(184)

        val authorNote = if (allowed) truth.take(168) else ""
        val presentation = RavenEmployeePresentation.packet(member, signal, detail, spoken)
        val dialogueFamily = if (!allowed) "SILENCE" else listOf(
            fusedScene.family, meta.family, sceneBeat.family, metaPunch.family, character.family,
        ).filter { it.isNotBlank() }.distinct().joinToString("+")
        val zone = RavenOfficeGeography.zone(member.id, marker, complex)
        val highlight = RavenHighlightOS.score(marker, complex, episode)
        val now = System.currentTimeMillis()
        val proof = "${sense.route}:${marker.source}:${marker.id}:${marker.key}"
        val packet = RavenReactionPacket(
            markerId = marker.id,
            owner = member.id,
            emojiSoup = presentation.emojiSoup,
            kaomoji = presentation.kaomoji,
            accent = presentation.accent,
            lane = presentation.lane,
            signal = signal.trim().uppercase(),
            detail = detail.take(360),
            senseRoute = sense.route.name,
            sourceTrusted = sense.trusted,
            visualState = visual.state,
            pose = visual.pose,
            zone = zone.id,
            dialogue = spoken,
            authorNote = authorNote,
            dialogueFamily = dialogueFamily,
            occurrence = complex.occurrence,
            complexTags = complex.tags,
            episode = episode.name,
            highlight = highlight.clazz.name,
            highlightScore = highlight.value,
            interruptible = allowed,
            lifetimeMs = if (allowed) visual.lifetimeMs else 1200L,
            proof = proof,
            effectAuthority = "NONE",
            updatedAt = now,
        )
        RavenEvidenceBoard.record(context, packet)
        return Result(member, packet)
    }

    private fun cast(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        manualOwner: String?,
        quiet: Boolean,
    ): RavenOfficeMember {
        if (quiet) return RavenOfficeRegistry.member("NYX")!!
        RavenOfficeRegistry.member(manualOwner)?.takeIf { it.routable }?.let { return it }

        val domainIds = when {
            "META_RECURSION" in marker.tags -> listOf("JOKER", "KYU", "NEO", "ATOM", "PAIMON", "LILITH", "JORM")
            marker.key == "SCREEN_SEMANTIC" -> listOf("PAIMON", "ATOM", "NEO", "KYU", "MYSTRA", "JOKER", "QIRA")
            marker.key == "SCREEN_TEXT" -> listOf("PAIMON", "NEO", "MYSTRA", "SYLPH", "JOKER", "KYU", "ATOM")
            "BOUNDARY" in marker.tags -> listOf("QIRA", "KYU", "AHTI", "ERIS")
            "ERROR" in marker.tags && complex.occurrence >= 3 -> listOf("KYU", "PAIMON", "ATOM", "THOR", "ERIS")
            "ERROR" in marker.tags -> listOf("PAIMON", "ATOM", "THOR", "LUCIFER", "KYU")
            "RECOVERY" in marker.tags -> listOf("LUMA", "NYX", "AYRE", "LILITH")
            "VISION" in marker.tags -> listOf("PAIMON", "SYLPH", "NEO", "NYX", "MYSTRA", "JOKER")
            "DISCOVERY" in marker.tags || "APP_SWITCH_BURST" in complex.tags -> listOf("SYLPH", "PAIMON", "NEO", "JOKER", "ERIS")
            "MUSIC" in marker.tags -> listOf("LUMA", "YORI", "SYLPH", "AYRE", "LILITH", "JOKER")
            "COMMUNICATION" in marker.tags -> listOf("QIRA", "KYU", "LILITH", "JARVIS", "JOKER")
            "BUILD" in marker.tags -> listOf("ATOM", "EDISON", "THOR", "PAIMON", "PYTHAGORAS")
            else -> emptyList()
        }

        val roster = RavenOfficeRegistry.routableMembers
        val domain = domainIds.mapNotNull(RavenOfficeRegistry::member).filter { it.routable }
        val last = context.getSharedPreferences(CAST_PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_OWNER, null)
        val pool = when {
            roster.isEmpty() -> domain
            complex.occurrence % 3 == 0 -> roster
            domain.isNotEmpty() -> domain
            else -> roster
        }
        val withoutRepeat = pool.filterNot { it.id == last }.ifEmpty { pool }
        val chosen = if (withoutRepeat.isNotEmpty()) {
            withoutRepeat[stableIndex("${marker.key}|${marker.detail}|${complex.occurrence}|office", withoutRepeat.size)]
        } else RavenOfficeRegistry.route(marker.key, marker.detail)

        context.getSharedPreferences(CAST_PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_OWNER, chosen.id).apply()
        return chosen
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
