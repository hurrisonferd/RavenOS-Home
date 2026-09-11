package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of the private Goblin Vision stack.
 *
 * MarkerBus -> ComplexEventOS -> CompanionDirectorLite -> InterruptibilityOS -> Bit/dialogue ->
 * Episode/Highlight -> VisualAtlas -> OfficeGeography -> ReactionPacket -> EvidenceBoard.
 *
 * No model call is required. No output gains effect authority.
 */
object RavenGoblinBrain {
    data class Result(val member: RavenOfficeMember, val packet: RavenReactionPacket)

    fun react(
        context: Context,
        signal: String,
        detail: String,
        manualOwner: String?,
        quiet: Boolean,
        hauntMode: RavenHauntMode,
    ): Result {
        val marker = RavenMarkerBus.emit(context, signal, detail)
        val complex = RavenComplexEventOS.analyze(context, marker)
        val episode = RavenEpisodeOS.phase(context, marker, complex)
        val member = cast(marker, complex, manualOwner, quiet)
        val authorNote = if (quiet) {
            "Quiet watch. The office is still here; only material signals break silence."
        } else {
            RavenOfficeRegistry.authorNote(member, signal, detail)
        }
        val presentation = RavenEmployeePresentation.packet(member, signal, detail, authorNote)
        val visual = RavenVisualAtlas.resolve(member.id, marker, complex)
        val allowed = RavenInterruptibilityOS.allow(context, marker, complex, hauntMode, quiet)
        val dialogue = if (allowed) RavenDialogueBank.select(member, marker, complex, visual, episode)
            else RavenDialogueBank.Line("", "SILENCE")
        val zone = RavenOfficeGeography.zone(member.id, marker, complex)
        val highlight = RavenHighlightOS.score(marker, complex, episode)
        val now = System.currentTimeMillis()
        val proof = "${marker.source}:${marker.id}:${marker.key}"
        val packet = RavenReactionPacket(
            markerId = marker.id,
            owner = member.id,
            emojiSoup = presentation.emojiSoup,
            kaomoji = presentation.kaomoji,
            accent = presentation.accent,
            lane = presentation.lane,
            signal = signal.trim().uppercase(),
            detail = detail.take(320),
            visualState = visual.state,
            pose = visual.pose,
            zone = zone.id,
            dialogue = dialogue.text,
            authorNote = authorNote,
            dialogueFamily = dialogue.family,
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
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        manualOwner: String?,
        quiet: Boolean,
    ): RavenOfficeMember {
        if (quiet) return RavenOfficeRegistry.member("NYX")!!
        RavenOfficeRegistry.member(manualOwner)?.takeIf { it.routable }?.let { return it }

        // Strong semantic casting outranks broad fallback routing, but stays deterministic/local.
        val preferred = when {
            "BOUNDARY" in marker.tags -> listOf("QIRA", "KYU", "AHTI")
            "ERROR" in marker.tags && complex.occurrence >= 3 -> listOf("KYU", "PAIMON", "ATOM", "THOR")
            "ERROR" in marker.tags -> listOf("PAIMON", "ATOM", "THOR", "LUCIFER")
            "RECOVERY" in marker.tags -> listOf("LUMA", "NYX", "AYRE")
            "DISCOVERY" in marker.tags || "APP_SWITCH_BURST" in complex.tags -> listOf("SYLPH", "PAIMON", "NEO")
            "MUSIC" in marker.tags -> listOf("LUMA", "YORI", "SYLPH")
            "COMMUNICATION" in marker.tags -> listOf("QIRA", "KYU", "LILITH")
            "BUILD" in marker.tags -> listOf("ATOM", "EDISON", "THOR", "PAIMON")
            else -> null
        }
        if (preferred != null) {
            val idx = stableIndex("${marker.key}|${marker.detail}|${complex.occurrence}", preferred.size)
            RavenOfficeRegistry.member(preferred[idx])?.takeIf { it.routable }?.let { return it }
        }
        return RavenOfficeRegistry.route(marker.key, marker.detail)
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
