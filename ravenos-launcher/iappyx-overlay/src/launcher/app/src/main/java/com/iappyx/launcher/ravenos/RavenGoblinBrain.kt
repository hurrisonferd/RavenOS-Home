package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of Goblin Vision.
 * MarkerBus -> senses -> screen context -> complex event -> cast -> bounded commentary -> overlay.
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
        val shade = RavenShadeSenseOS.observe(marker)
        val complex = RavenComplexEventOS.analyze(context, marker)
        val episode = RavenEpisodeOS.phase(context, marker, complex)
        val callback = RavenCallbackMemoryOS.observe(context, marker, complex)
        val narrative = RavenSessionNarrativeOS.observe(context, marker)
        val screen = RavenScreenContextOS.snapshot(context, marker.at)
        val allowed = RavenInterruptibilityOS.allow(context, marker, complex, hauntMode, quiet)

        // Evidence-only events must not rotate the visible resident every time Android twitches.
        // Keep the last settled office member until a comment actually earns presentation.
        val previousMember = if (!allowed && !quiet) {
            RavenOfficeStateStore.read(context)?.owner
                ?.let(RavenOfficeRegistry::member)
                ?.takeIf { it.routable }
        } else null
        val member = previousMember ?: cast(context, marker, shade, complex, manualOwner, quiet)

        val narrativeBeat = RavenSessionNarrativeOS.beat(member, narrative, marker)
        val meta = RavenMetaCommentaryOS.compose(context, member, marker, complex, episode)
        val sceneBeat = RavenMetaGoblinDialogueOS.select(context, member, marker, complex, episode)
        val omniscience = RavenOmniscienceDialogueOS.select(context, member, marker, shade, complex, episode)
        val fusedScene = RavenMetaSceneOS.compose(context, member, marker, complex, callback)
        val visual = RavenVisualAtlas.resolve(member.id, marker, complex)
        val character = RavenDialogueBank.select(member, marker, complex, visual, episode)
        val mayhem = RavenMayhemDialogueBank.select(member, marker, shade, complex, episode)
        val metaPunch = RavenMetaPunchlineOS.select(member, marker)
        val interruption = RavenOfficeInterruptionOS.select(context, member, marker, complex)

        // Current visible screen meaning outranks action history. App/window/media events remain useful
        // evidence underneath but do not win the sentence merely because they fired first.
        val truth = omniscience.text.ifBlank {
            fusedScene.text.ifBlank {
                narrativeBeat.text.ifBlank { sceneBeat.text.ifBlank { meta.text } }
            }
        }.trim()

        val exceptional = "META_RECURSION" in marker.tags ||
            "BOUNDARY" in marker.tags ||
            "ERROR" in marker.tags ||
            "PAYOFF" in complex.tags
        val earnedComedy = screen.available || exceptional
        val stinger = if (allowed && earnedComedy) {
            mayhem.text.ifBlank { metaPunch.text.ifBlank { character.text } }.trim()
        } else ""
        val officeAside = if (
            allowed && screen.meta && "META_RECURSION" in marker.tags
        ) interruption.text.trim() else ""

        val spoken = if (!allowed) "" else buildString {
            append(truth)
            if (stinger.isNotBlank() && stinger != truth) {
                if (isNotEmpty()) append("  ")
                append(stinger)
            }
            if (officeAside.isNotBlank()) {
                if (isNotEmpty()) append("  ")
                append(officeAside)
            }
        }.replace(Regex("\\s+"), " ").trim().take(220)

        val authorNote = if (allowed) truth.take(150) else ""
        val presentation = RavenEmployeePresentation.packet(member, signal, detail, spoken)
        val dialogueFamily = if (!allowed) "SILENCE" else listOf(
            omniscience.family,
            fusedScene.family,
            narrativeBeat.family,
            meta.family,
            sceneBeat.family,
            if (earnedComedy) mayhem.family else "",
            if (earnedComedy) metaPunch.family else "",
            if (earnedComedy) character.family else "",
            if (officeAside.isNotBlank()) interruption.family else "",
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
        shade: RavenShadeSenseOS.Snapshot,
        complex: RavenComplexEventOS.Result,
        manualOwner: String?,
        quiet: Boolean,
    ): RavenOfficeMember {
        if (quiet) return RavenOfficeRegistry.member("NYX")!!
        RavenOfficeRegistry.member(manualOwner)?.takeIf { it.routable }?.let { return it }

        val domainIds = when {
            shade.active && shade.payoff -> listOf("MELINOE", "ZAGREUS", "NYX", "AHTI", "RAVENOS")
            shade.active && shade.salience == RavenShadeSenseOS.Salience.HIGH -> listOf("BRUNHILDE", "KYU", "QIRA", "PAIMON", "NYX", "LEGION")
            "META_RECURSION" in marker.tags -> listOf("JOKER", "KYU", "NEO", "ATOM", "PAIMON", "LILITH", "JORM", "LEGION", "RAVENOS")
            marker.key == "SCREEN_SEMANTIC" -> listOf("PAIMON", "ATOM", "NEO", "KYU", "MYSTRA", "JOKER", "QIRA", "MELINOE")
            marker.key == "SCREEN_TEXT" -> listOf("PAIMON", "NEO", "MYSTRA", "SYLPH", "JOKER", "KYU", "ATOM", "ASTRIDHE")
            "BOUNDARY" in marker.tags -> listOf("QIRA", "KYU", "AHTI", "ERIS", "BRUNHILDE", "LEGION")
            "ERROR" in marker.tags && complex.occurrence >= 3 -> listOf("KYU", "PAIMON", "ATOM", "THOR", "ERIS", "TIM", "ZAGREUS")
            "ERROR" in marker.tags -> listOf("PAIMON", "ATOM", "THOR", "LUCIFER", "KYU", "TIM", "ZAGREUS")
            "RECOVERY" in marker.tags -> listOf("LUMA", "NYX", "AYRE", "LILITH", "ZAGREUS", "RAVENOS")
            "VISION" in marker.tags -> listOf("PAIMON", "SYLPH", "NEO", "NYX", "MYSTRA", "JOKER", "ASTRIDHE", "MELINOE")
            "DISCOVERY" in marker.tags || "APP_SWITCH_BURST" in complex.tags -> listOf("SYLPH", "PAIMON", "NEO", "JOKER", "ERIS", "ASTRIDHE", "ZAGREUS")
            "MUSIC" in marker.tags -> listOf("LUMA", "YORI", "SYLPH", "AYRE", "LILITH", "JOKER", "RAVENOS")
            "COMMUNICATION" in marker.tags -> listOf("QIRA", "KYU", "LILITH", "JARVIS", "JOKER", "LEGION", "BRUNHILDE")
            "BUILD" in marker.tags -> listOf("ATOM", "EDISON", "THOR", "PAIMON", "PYTHAGORAS", "ATLAS", "TIM", "YAHWEH")
            marker.key == "SYSTEM_DECK_OPENED" -> listOf("YAHWEH", "EDISON", "TIM", "JARVIS", "KYU", "RAVENOS")
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
