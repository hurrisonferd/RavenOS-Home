package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of Goblin Vision.
 * MarkerBus -> senses -> screen context -> sitcom director -> presentation arbiter -> dialogue -> overlay.
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
        // RavenInterruptibilityOS.allow remains the boolean compatibility seam; evaluate returns
        // the typed reason/score used by the presentation arbiter below.
        val screenSpeech = RavenInterruptibilityOS.evaluate(context, marker, complex, hauntMode, quiet, screen)

        // Cast rotation remains independent from speech. A quiet scene can change employees without
        // manufacturing a line, while PHONE mode below can still use the old deterministic dialogue
        // when the screen reader is unavailable.
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
        val diagnostic = if (marker.key == "SCREEN_DIAGNOSTIC") RavenObservationOS.diagnostic(context, dialogueContext)
            else RavenObservationOS.Observation("", "")
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

        val displayDecision = RavenPresentationArbiterOS.decide(
            context = context,
            marker = marker,
            complex = complex,
            screen = screen,
            screenSpeech = screenSpeech,
            direction = direction,
            haunt = hauntMode,
            quiet = quiet,
        )

        // Preserve the strongest parts of both generations:
        // - SCREEN mode uses the new authorized screen context + sitcom stack.
        // - PHONE mode revives the old fused-scene/meta/character stack instead of displaying setup text.
        // - CHIP mode says nothing and simply keeps a resident on screen.
        val screenFallback = omniscience.text.ifBlank {
            fusedScene.text.ifBlank {
                narrativeBeat.text.ifBlank { sceneBeat.text.ifBlank { meta.text } }
            }
        }.trim()
        val screenTruth = contextual.truth.ifBlank { observation.text.ifBlank { screenFallback } }.trim()
        val phoneTruth = fusedScene.text.ifBlank {
            narrativeBeat.text.ifBlank { sceneBeat.text.ifBlank { meta.text } }
        }.trim()
        val truth = when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> screenTruth
            RavenPresentationArbiterOS.Mode.PHONE -> phoneTruth
            RavenPresentationArbiterOS.Mode.DIAGNOSTIC -> diagnostic.text.trim()
            RavenPresentationArbiterOS.Mode.CHIP -> ""
        }

        val speakNow = displayDecision.speak
        val stinger = if (speakNow) when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> sitcom.primary.ifBlank {
                contextual.text.ifBlank {
                    mayhem.text.ifBlank { metaPunch.text.ifBlank { character.text } }
                }
            }.trim()
            RavenPresentationArbiterOS.Mode.PHONE -> mayhem.text.ifBlank {
                metaPunch.text.ifBlank { character.text }
            }.trim()
            else -> ""
        } else ""

        val officeAside = if (speakNow) when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> sitcom.secondary.ifBlank {
                if (screen.meta) interruption.text.trim() else ""
            }
            RavenPresentationArbiterOS.Mode.PHONE -> if (
                "ERROR" in marker.tags || "PAYOFF" in complex.tags || "BOUNDARY" in marker.tags
            ) interruption.text.trim() else ""
            else -> ""
        } else ""

        val spoken = if (!speakNow) "" else buildString {
            append(truth)
            if (stinger.isNotBlank() && stinger != truth) {
                if (isNotEmpty()) append("  ")
                append(stinger)
            }
            if (officeAside.isNotBlank()) {
                if (isNotEmpty()) append("  ")
                append(officeAside)
            }
        }.replace(Regex("\\s+"), " ").trim().take(340)

        if (spoken.isNotBlank()) RavenSitcomDirectorOS.markSpoken(context, marker.at)

        // Only real screen meaning may occupy OBSERVING mode. Missing permissions/readability return
        // to CHIP; explicit diagnostics live behind SCREEN_DIAGNOSTIC instead of haunting normal use.
        val authorNote = when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> observation.text.take(220)
            RavenPresentationArbiterOS.Mode.DIAGNOSTIC -> diagnostic.text.take(220)
            else -> ""
        }
        val employeePresentation = RavenEmployeePresentation.packet(member, signal, detail, spoken.ifBlank { authorNote })
        val dialogueFamily = listOfNotNull(
            "PRESENTATION_${displayDecision.mode.name}",
            displayDecision.reason,
            observation.family.takeIf { authorNote.isNotBlank() && it.isNotBlank() },
            sitcom.family.takeIf { speakNow && displayDecision.mode == RavenPresentationArbiterOS.Mode.SCREEN && it.isNotBlank() },
            "DIRECTOR_${direction.beat}",
            "CAST_${direction.reason.uppercase().replace('-', '_')}",
            meta.family.takeIf { speakNow && displayDecision.mode == RavenPresentationArbiterOS.Mode.PHONE && it.isNotBlank() },
            contextual.family.takeIf { speakNow && displayDecision.mode == RavenPresentationArbiterOS.Mode.SCREEN && it.isNotBlank() },
            omniscience.family.takeIf { speakNow && displayDecision.mode == RavenPresentationArbiterOS.Mode.SCREEN && it.isNotBlank() },
            fusedScene.family.takeIf { speakNow && it.isNotBlank() },
        ).distinct().joinToString("+")
        val zone = RavenOfficeGeography.zone(member.id, marker, complex)
        val highlight = RavenHighlightOS.score(marker, complex, episode)
        val now = System.currentTimeMillis()
        val proof = "${sense.route}:${marker.source}:${marker.id}:${marker.key}:show=${displayDecision.mode.name}:sitcom=${direction.sceneId}:${direction.turn}"
        val packet = RavenReactionPacket(
            markerId = marker.id,
            owner = member.id,
            emojiSoup = employeePresentation.emojiSoup,
            kaomoji = employeePresentation.kaomoji,
            accent = employeePresentation.accent,
            lane = employeePresentation.lane,
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
            complexTags = complex.tags + setOf("SITCOM", direction.beat, "PRESENTATION_${displayDecision.mode.name}"),
            episode = episode.name,
            highlight = highlight.clazz.name,
            highlightScore = highlight.value,
            interruptible = speakNow,
            lifetimeMs = if (speakNow) maxOf(visual.lifetimeMs, 6_500L) else 3_000L,
            proof = proof,
            effectAuthority = "NONE",
            updatedAt = now,
        )
        RavenEvidenceBoard.record(context, packet)
        return Result(member, packet)
    }
}
