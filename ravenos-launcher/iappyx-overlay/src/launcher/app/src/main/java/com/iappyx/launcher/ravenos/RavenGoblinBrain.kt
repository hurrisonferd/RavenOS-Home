package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of Goblin Vision.
 * MarkerBus -> senses -> viewport/screen context -> episode script -> sitcom director -> Meta-Max -> presentation -> overlay.
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
        val script = RavenEpisodeScriptOS.observe(context, screen, marker, narrative)
        // RavenInterruptibilityOS.allow remains the boolean compatibility seam; evaluate returns
        // the typed reason/score used by the presentation arbiter below.
        val screenSpeech = RavenInterruptibilityOS.evaluate(context, marker, complex, hauntMode, quiet, screen)

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
        val mesh = RavenRVResilienceOS.snapshot(context, marker.at)
        val bit = RavenBitLedgerOS.observe(screen, script, direction, marker.at)
        val show = RavenMetaMaxShowrunnerOS.direct(screen, script, direction, bit, mesh)
        val plot = RavenPlotStackOS.snapshot(context, screen, script, bit)

        val dialogueContext = RavenDialogueContextOS.compose(context, member, marker, complex, episode, screen, callback, narrative)
        val observation = RavenObservationOS.observe(context, dialogueContext, hauntMode)
        val diagnostic = if (marker.key == "SCREEN_DIAGNOSTIC") RavenObservationOS.diagnostic(context, dialogueContext)
            else RavenObservationOS.Observation("", "")
        val sitcom = RavenSitcomDialogueOS.compose(dialogueContext, direction)
        val viewportDialogue = RavenViewportDialogueOS.select(context, member, screen, direction)
        val scriptDialogue = RavenScriptDialogueOS.select(member, screen, script, direction)
        val metaMax = RavenMetaMaxDialogueOS.select(member, screen, script, direction, bit, show, mesh)
        val plotDialogue = RavenPlotDialogueOS.select(member, plot, direction)
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

        // Meta-Max/plot writers do not force additional interruptions. They only compete for lines
        // after the existing presentation/interruptibility stack has already earned speech.
        val speakNow = displayDecision.speak
        val useMetaMaxWriter = speakNow && metaMax.text.isNotBlank() && show.writerEligible && (
            show.level >= 3 || bit.shouldEscalate || bit.brick || script.interactionWorthSpeaking ||
                show.form in setOf("TITLE_CARD", "PREVIOUSLY_ON", "FOURTH_WALL_EMERGENCY")
            )
        val usePlotWriter = speakNow && plot.crossover && plotDialogue.text.isNotBlank() &&
            (direction.turn % 3 == 0 || script.sceneChanged || script.returned || bit.shouldEscalate)
        val useScriptWriter = scriptDialogue.text.isNotBlank() && (
            script.callbackEarned || script.interactionWorthSpeaking || script.interruption.isNotBlank() || script.returned ||
                (script.sceneChanged && direction.turn % 2 == 0) || direction.turn % 5 == 0
            )
        val useViewportWriter = displayDecision.mode == RavenPresentationArbiterOS.Mode.SCREEN &&
            viewportDialogue.text.isNotBlank() &&
            (direction.turn % 4 == 0 || direction.beat in setOf("COLD_OPEN", "META", "CALLBACK") && direction.turn % 2 == 0)

        val stinger = if (speakNow) when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> when {
                useMetaMaxWriter -> metaMax.text
                usePlotWriter -> plotDialogue.text
                useScriptWriter -> scriptDialogue.text
                useViewportWriter -> viewportDialogue.text
                else -> sitcom.primary.ifBlank {
                    contextual.text.ifBlank {
                        mayhem.text.ifBlank { metaPunch.text.ifBlank { character.text } }
                    }
                }.trim()
            }
            RavenPresentationArbiterOS.Mode.PHONE -> when {
                useMetaMaxWriter -> metaMax.text
                usePlotWriter -> plotDialogue.text
                useScriptWriter -> scriptDialogue.text
                else -> mayhem.text.ifBlank { metaPunch.text.ifBlank { character.text } }.trim()
            }
            else -> ""
        } else ""

        val officeAside = if (speakNow) when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> sitcom.secondary.ifBlank {
                if (screen.meta || show.chorusEligible) interruption.text.trim() else ""
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
        }.replace(Regex("\\s+"), " ").trim().take(540)

        if (spoken.isNotBlank()) RavenSitcomDirectorOS.markSpoken(context, marker.at)

        val authorNote = when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> listOf(
                observation.text,
                script.continuity.takeIf { it.isNotBlank() && !observation.text.contains(it, true) }.orEmpty(),
            ).filter(String::isNotBlank).joinToString("  ").take(340)
            RavenPresentationArbiterOS.Mode.DIAGNOSTIC -> diagnostic.text.take(240)
            else -> ""
        }
        val viewport = RavenViewportSemanticsOS.latest(context, marker.at)
        val presentationDetail = buildString {
            append(detail)
            append("|screen_kind:").append(screen.semanticKind)
            append("|screen_meta:").append(screen.meta)
            append("|script_act:").append(script.act)
            append("|script_dwell:").append(script.dwell)
            if (script.motif.isNotBlank()) append("|script_motif:").append(script.motif)
            if (script.interruption.isNotBlank()) append("|script_cameo:").append(script.interruption.replace('|', '/'))
            if (script.interaction.isNotBlank()) append("|script_interaction:").append(script.interaction)
            if (script.interactionTarget.isNotBlank()) append("|script_target:").append(script.interactionTarget.replace('|', '/').take(90))
            if (script.interactionDirection.isNotBlank()) append("|script_direction:").append(script.interactionDirection)
            if (bit.active) {
                append("|bit_id:").append(bit.id.replace('|', '/').take(90))
                append("|bit_count:").append(bit.count)
                append("|bit_tier:").append(bit.tier)
            }
            append("|meta_level:").append(show.level)
            append("|meta_form:").append(show.form)
            append("|rv_mesh:").append(mesh.mode)
            append("|plot_score:").append(plot.score)
            if (plot.bPlot.isNotBlank()) append("|plot_b:").append(plot.bPlot.replace('|', '/').take(100))
            if (plot.cPlot.isNotBlank()) append("|plot_c:").append(plot.cPlot.replace('|', '/').take(100))
            viewport?.let {
                append("|screen_task:").append(it.task)
                if (it.title.isNotBlank()) append("|screen_title:").append(it.title.replace('|', '/').take(90))
                if (it.selected.isNotBlank()) append("|screen_selected:").append(it.selected.replace('|', '/').take(90))
            }
        }
        val basePresentation = RavenEmployeePresentation.packet(member, signal, presentationDetail, spoken.ifBlank { authorNote })
        val employeePresentation = RavenSceneExpressionOS.decorate(context, basePresentation, member, screen, direction, script, bit, show)
        val dialogueFamily = listOfNotNull(
            "PRESENTATION_${displayDecision.mode.name}",
            displayDecision.reason,
            observation.family.takeIf { authorNote.isNotBlank() && it.isNotBlank() },
            metaMax.family.takeIf { useMetaMaxWriter && it.isNotBlank() },
            plotDialogue.family.takeIf { usePlotWriter && it.isNotBlank() },
            scriptDialogue.family.takeIf { speakNow && useScriptWriter && it.isNotBlank() },
            sitcom.family.takeIf { speakNow && displayDecision.mode == RavenPresentationArbiterOS.Mode.SCREEN && it.isNotBlank() },
            viewportDialogue.family.takeIf { speakNow && useViewportWriter && it.isNotBlank() },
            "METAMAX_L${show.level}_${show.form}",
            "PLOT_${plot.score}",
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
        val proof = "${sense.route}:${marker.source}:${marker.id}:${marker.key}:show=${displayDecision.mode.name}:sitcom=${direction.sceneId}:${direction.turn}:script=${script.act}:${script.motifCount}:action=${script.interaction}:bit=${bit.count}:${bit.tier}:meta=${show.level}:${show.form}:plot=${plot.score}:rv=${mesh.mode}:viewport=${viewport?.task ?: "none"}"
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
            complexTags = complex.tags + setOf(
                "SITCOM", direction.beat, "PRESENTATION_${displayDecision.mode.name}", "SCRIPT_ACT_${script.act}",
                "METAMAX_L${show.level}", "METAMAX_${show.form}", "RV_${mesh.mode}", "PLOT_${plot.score}",
            ) + (if (bit.active) setOf("BIT_${bit.tier}") else emptySet()) +
                (if (plot.crossover) setOf("PLOT_CROSSOVER") else emptySet()) +
                if (viewport != null) setOf("VIEWPORT", "TASK_${viewport.task}") else emptySet(),
            episode = episode.name,
            highlight = highlight.clazz.name,
            highlightScore = highlight.value,
            interruptible = speakNow,
            lifetimeMs = if (speakNow) maxOf(visual.lifetimeMs, 8_000L) else 3_500L,
            proof = proof,
            effectAuthority = "NONE",
            updatedAt = now,
        )
        RavenEvidenceBoard.record(context, packet)
        return Result(member, packet)
    }
}
