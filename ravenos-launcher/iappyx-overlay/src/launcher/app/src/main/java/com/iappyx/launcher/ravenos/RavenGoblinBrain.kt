package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of Goblin Vision.
 * MarkerBus -> senses -> screen context -> sitcom director -> observation -> dialogue -> overlay.
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
        val shade = RavenShadeSenseOS.observe(marker)
        val complex = RavenComplexEventOS.analyze(context, marker)
        val episode = RavenEpisodeOS.phase(context, marker, complex)
        val callback = RavenCallbackMemoryOS.observe(context, marker, complex)
        val narrative = RavenSessionNarrativeOS.observe(context, marker)
        val screen = RavenScreenContextOS.snapshot(context, marker.at)
        // RavenInterruptibilityOS.allow remains the boolean compatibility seam; evaluate returns the typed reason/score used here.
        val speech = RavenInterruptibilityOS.evaluate(context, marker, complex, hauntMode, quiet, screen)

        // The resident is now a cast position, not ownership of the widget. The director can rotate a
        // quiet office on semantic scene changes, dwell and deterministic sitcom cadence even when
        // the interruptibility layer correctly suppresses full dialogue.
        val direction = RavenSitcomDirectorOS.direct(
            context = context,
            marker = marker,
            complex = complex,
            screen = screen,
            haunt = hauntMode,
            manualOwner = manualOwner,
            quiet = quiet,
        )
        val member = direction.primary

        val dialogueContext = RavenDialogueContextOS.compose(context, member, marker, complex, episode, screen, callback, narrative)
        val observation = RavenObservationOS.observe(context, dialogueContext, hauntMode)
        val sitcom = RavenSitcomDialogueOS.compose(dialogueContext, direction)
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
        // sits downstream of it. The sitcom director may earn a screen-grounded line sooner than a
        // low-value Android event, but never when no trustworthy screen meaning exists.
        val fallbackTruth = omniscience.text.ifBlank {
            fusedScene.text.ifBlank {
                narrativeBeat.text.ifBlank { sceneBeat.text.ifBlank { meta.text } }
            }
        }.trim()
        val truth = observation.text.ifBlank { fallbackTruth }

        val exceptional = screen.meta || "META_RECURSION" in marker.tags ||
            "BOUNDARY" in marker.tags || "ERROR" in marker.tags || "PAYOFF" in complex.tags
        val earnedComedy = screen.available || exceptional
        val speakNow = !quiet && earnedComedy && (speech.speak || direction.shouldSpeak)
        val stinger = if (speakNow) {
            sitcom.primary.ifBlank {
                contextual.text.ifBlank {
                    mayhem.text.ifBlank { metaPunch.text.ifBlank { character.text } }
                }
            }.trim()
        } else ""
        val officeAside = if (speakNow) {
            sitcom.secondary.ifBlank {
                if (screen.meta && exceptional) interruption.text.trim() else ""
            }
        } else ""

        val spoken = if (!speakNow) "" else buildString {
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
        }.replace(Regex("\\s+"), " ").trim().take(340)

        if (spoken.isNotBlank()) RavenSitcomDirectorOS.markSpoken(context, marker.at)

        val authorNote = observation.text.take(220)
        val presentation = RavenEmployeePresentation.packet(member, signal, detail, spoken.ifBlank { authorNote })
        val dialogueFamily = listOfNotNull(
            observation.family.takeIf { it.isNotBlank() },
            sitcom.family.takeIf { speakNow && it.isNotBlank() },
            "DIRECTOR_${direction.beat}",
            "CAST_${direction.reason.uppercase().replace('-', '_')}",
            if (speakNow) contextual.family.takeIf { it.isNotBlank() } else null,
            if (speakNow) omniscience.family.takeIf { it.isNotBlank() } else null,
            if (speakNow) fusedScene.family.takeIf { it.isNotBlank() } else null,
            speech.reason.takeIf { it.isNotBlank() },
        ).distinct().joinToString("+")
        val zone = RavenOfficeGeography.zone(member.id, marker, complex)
        val highlight = RavenHighlightOS.score(marker, complex, episode)
        val now = System.currentTimeMillis()
        val proof = "${sense.route}:${marker.source}:${marker.id}:${marker.key}:sitcom=${direction.sceneId}:${direction.turn}"
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
            complexTags = complex.tags + setOf("SITCOM", direction.beat),
            episode = episode.name,
            highlight = highlight.clazz.name,
            highlightScore = highlight.value,
            interruptible = speakNow,
            lifetimeMs = if (speakNow) maxOf(visual.lifetimeMs, 6_500L) else 7_000L,
            proof = proof,
            effectAuthority = "NONE",
            updatedAt = now,
        )
        RavenEvidenceBoard.record(context, packet)
        return Result(member, packet)
    }
}
