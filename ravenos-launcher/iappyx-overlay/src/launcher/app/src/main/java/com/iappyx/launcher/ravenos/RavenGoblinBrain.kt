package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of the private Goblin Vision stack.
 * MarkerBus -> LocalSense -> ComplexEvent -> casting -> interruptibility -> commentary -> presentation.
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
        val sense = RavenLocalSenseOS.resolve(marker)
        val complex = RavenComplexEventOS.analyze(context, marker)
        val episode = RavenEpisodeOS.phase(context, marker, complex)
        val member = cast(marker, complex, manualOwner, quiet)
        val meta = RavenMetaCommentaryOS.compose(context, member, marker, complex, episode)
        val allowed = RavenInterruptibilityOS.allow(context, marker, complex, hauntMode, quiet)
        val authorNote = if (allowed) {
            if (quiet) "Quiet watch. ${meta.text}" else meta.text
        } else ""
        val presentation = RavenEmployeePresentation.packet(member, signal, detail, authorNote)
        val visual = RavenVisualAtlas.resolve(member.id, marker, complex)
        val character = RavenDialogueBank.select(member, marker, complex, visual, episode)
        val dialogue = if (!allowed) {
            RavenDialogueBank.Line("", "SILENCE")
        } else {
            val characterEarned = "RUNNING_BIT" in complex.tags || "PAYOFF" in complex.tags ||
                "RECOVERY_ARC" in complex.tags || "ERROR" in marker.tags || "BOUNDARY" in marker.tags
            val text = when {
                characterEarned && character.text.isNotBlank() && character.text != meta.text ->
                    "${character.text} ${meta.text}".take(280)
                else -> meta.text
            }
            RavenDialogueBank.Line(text, if (characterEarned) "${character.family}+${meta.family}" else meta.family)
        }
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

        val preferred = when {
            "BOUNDARY" in marker.tags -> listOf("QIRA", "KYU", "AHTI")
            "ERROR" in marker.tags && complex.occurrence >= 3 -> listOf("KYU", "PAIMON", "ATOM", "THOR")
            "ERROR" in marker.tags -> listOf("PAIMON", "ATOM", "THOR", "LUCIFER")
            "RECOVERY" in marker.tags -> listOf("LUMA", "NYX", "AYRE")
            "VISION" in marker.tags -> listOf("PAIMON", "SYLPH", "NEO", "NYX")
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
