package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of the private Goblin Vision stack.
 * MarkerBus -> LocalSense -> ComplexEvent -> rotating cast -> commentary -> expression -> presentation.
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
        val member = cast(context, marker, complex, manualOwner, quiet)
        val meta = RavenMetaCommentaryOS.compose(context, member, marker, complex, episode)
        val allowed = RavenInterruptibilityOS.allow(context, marker, complex, hauntMode, quiet)
        val visual = RavenVisualAtlas.resolve(member.id, marker, complex)
        val character = RavenDialogueBank.select(member, marker, complex, visual, episode)

        // One visible sentence. Phone truth first; employee joke/posture second when earned.
        // The evidence copy remains separate in authorNote but presentation surfaces do not label it.
        val spoken = if (!allowed) "" else buildString {
            append(meta.text.trim())
            if (character.text.isNotBlank() && character.text != meta.text) {
                if (isNotEmpty()) append("  ")
                append(character.text.trim())
            }
        }.replace(Regex("\\s+"), " ").trim().take(176)

        val authorNote = if (allowed) meta.text.trim().take(160) else ""
        val presentation = RavenEmployeePresentation.packet(member, signal, detail, spoken)
        val dialogueFamily = if (!allowed) "SILENCE" else listOf(meta.family, character.family)
            .filter { it.isNotBlank() }
            .joinToString("+")
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
            detail = detail.take(320),
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
        val last = context.getSharedPreferences(CAST_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_OWNER, null)

        // Every third occurrence opens the floor to the full routable office. Other turns
        // retain domain affinity. This makes the cast broad without making specialist routing random.
        val pool = when {
            roster.isEmpty() -> domain
            complex.occurrence % 3 == 0 -> roster
            domain.isNotEmpty() -> domain
            else -> roster
        }
        val withoutRepeat = pool.filterNot { it.id == last }.ifEmpty { pool }
        val chosen = if (withoutRepeat.isNotEmpty()) {
            withoutRepeat[stableIndex("${marker.key}|${marker.detail}|${complex.occurrence}|office", withoutRepeat.size)]
        } else {
            RavenOfficeRegistry.route(marker.key, marker.detail)
        }

        context.getSharedPreferences(CAST_PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_OWNER, chosen.id).apply()
        return chosen
    }

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
