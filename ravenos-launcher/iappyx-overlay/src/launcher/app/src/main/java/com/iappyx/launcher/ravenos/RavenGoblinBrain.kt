package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of Goblin Vision.
 * MarkerBus -> senses -> screen context -> complex event -> cast -> observation -> dialogue -> overlay.
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
        val speech = RavenInterruptibilityOS.evaluate(context, marker, complex, hauntMode, quiet, screen)

        // Evidence-only events must not rotate the visible resident every time Android twitches.
        val previousMember = if (!speech.speak && !quiet) {
            RavenOfficeStateStore.read(context)?.owner
                ?.let(RavenOfficeRegistry::member)
                ?.takeIf { it.routable }
        } else null
        val member = previousMember ?: cast(context, marker, shade, complex, manualOwner, quiet, screen)

        val dialogueContext = RavenDialogueContextOS.compose(context, member, marker, complex, episode, screen, callback, narrative)
        val observation = RavenObservationOS.observe(context, dialogueContext, hauntMode)
        val narrativeBeat = RavenSessionNarrativeOS.beat(member, narrative, marker)
        val meta = RavenMetaCommentaryOS.compose(context, member, marker, complex, episode)
        val sceneBeat = RavenMetaGoblinDialogueOS.select(context, member, marker, complex, episode)
        val omniscience = RavenOmniscienceDialogueOS.select(context, member, marker, shade, complex, episode)
        val fusedScene = RavenMetaSceneOS.compose(context, member, marker, complex, callback)
        val visual = RavenVisualAtlas.resolve(member.id, marker, complex)
        val contextual = RavenContextualDialogueBank.select(dialogueContext)
        val character = RavenDialogueBank.select(member, marker, complex, visual, episode)
        val mayhem = RavenMayhemDialogueBank.select(member, marker, shade, complex, episode)
        val metaPunch = RavenMetaPunchlineOS.select(member, marker)
        val interruption = RavenOfficeInterruptionOS.select(context, member, marker, complex)

        // Screen-grounded observation is the stable resident layer. Character speech is rarer and
        // sits downstream of it. This keeps the office visibly aware without returning to callback spam.
        val fallbackTruth = omniscience.text.ifBlank {
            fusedScene.text.ifBlank {
                narrativeBeat.text.ifBlank { sceneBeat.text.ifBlank { meta.text } }
            }
        }.trim()
        val truth = observation.text.ifBlank { fallbackTruth }

        val exceptional = screen.meta || "META_RECURSION" in marker.tags ||
            "BOUNDARY" in marker.tags || "ERROR" in marker.tags || "PAYOFF" in complex.tags
        val earnedComedy = screen.available || exceptional
        val stinger = if (speech.speak && earnedComedy) {
            contextual.text.ifBlank {
                mayhem.text.ifBlank { metaPunch.text.ifBlank { character.text } }
            }.trim()
        } else ""
        val officeAside = if (speech.speak && screen.meta && exceptional) interruption.text.trim() else ""

        val spoken = if (!speech.speak) "" else buildString {
            val speechTruth = if (contextual.truth.isNotBlank()) contextual.truth else truth
            append(speechTruth)
            if (stinger.isNotBlank() && stinger != speechTruth) {
                if (isNotEmpty()) append("  ")
                append(stinger)
            }
            if (officeAside.isNotBlank()) {
                if (isNotEmpty()) append("  ")
                append(officeAside)
            }
        }.replace(Regex("\\s+"), " ").trim().take(280)

        val authorNote = observation.text.take(190)
        val presentation = RavenEmployeePresentation.packet(member, signal, detail, spoken.ifBlank { authorNote })
        val dialogueFamily = listOfNotNull(
            observation.family.takeIf { it.isNotBlank() },
            if (speech.speak) contextual.family.takeIf { it.isNotBlank() } else null,
            if (speech.speak) omniscience.family.takeIf { it.isNotBlank() } else null,
            if (speech.speak) fusedScene.family.takeIf { it.isNotBlank() } else null,
            speech.reason.takeIf { it.isNotBlank() },
        ).distinct().joinToString("+")
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
            interruptible = speech.speak,
            lifetimeMs = if (speech.speak) visual.lifetimeMs else 5_000L,
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
        screen: RavenScreenContextOS.Snapshot,
    ): RavenOfficeMember {
        if (quiet) return RavenOfficeRegistry.member("NYX")!!
        RavenOfficeRegistry.member(manualOwner)?.takeIf { it.routable }?.let { return it }

        val domainIds = when {
            screen.meta -> listOf("JOKER", "KYU", "NEO", "ATOM", "PAIMON", "LILITH", "JORM", "LEGION", "RAVENOS", "YORK")
            screen.semanticKind == "CHATGPT" -> listOf("ATOM", "KYU", "PAIMON", "JOKER", "NEO", "YORK", "LILITH", "MYSTRA", "PYTHAGORAS")
            screen.semanticKind == "SETTINGS" -> listOf("KYU", "PAIMON", "QIRA", "EDISON", "THOR", "YAHWEH", "ATOM")
            screen.semanticKind in setOf("MUSIC", "VIDEO") -> listOf("YORI", "LUMA", "SYLPH", "AYRE", "JOKER", "MYSTRA")
            screen.semanticKind in setOf("MAIL", "MESSAGING") -> listOf("QIRA", "LILITH", "KYU", "JARVIS", "BRUNHILDE", "LEGION")
            shade.active && shade.payoff -> listOf("MELINOE", "ZAGREUS", "NYX", "AHTI", "RAVENOS")
            shade.active && shade.salience == RavenShadeSenseOS.Salience.HIGH -> listOf("BRUNHILDE", "KYU", "QIRA", "PAIMON", "NYX", "LEGION")
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
            complex.occurrence % 4 == 0 && screen.available -> roster
            domain.isNotEmpty() -> domain
            else -> roster
        }
        val withoutRepeat = pool.filterNot { it.id == last }.ifEmpty { pool }
        val chosen = if (withoutRepeat.isNotEmpty()) {
            withoutRepeat[stableIndex("${screen.signature}|${marker.key}|${complex.occurrence}|office-v3", withoutRepeat.size)]
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
