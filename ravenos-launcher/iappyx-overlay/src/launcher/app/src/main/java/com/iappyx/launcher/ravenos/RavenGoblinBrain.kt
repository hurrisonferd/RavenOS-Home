package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Android vertical slice of Goblin Vision.
 * MarkerBus -> senses -> viewport/screen -> script -> sitcom -> Meta-Max -> Gold/Season -> presentation.
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
        RavenGoldEpisodeStatsOS.observe(marker)
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
        val backstage = RavenBackstageOS.observe(screen, script, marker, direction)

        // First snapshot establishes cross-session motif state. Gold may then promote a silently
        // relevant backstage member into its one crosstalk slot; the final snapshot binds pair
        // history to the actual chosen partner rather than the pre-Gold candidate.
        val seasonBase = RavenOfficeSeasonOS.snapshot(
            context = context,
            owner = member.id,
            secondary = null,
            canonicalMotif = script.motif,
            mesh = mesh,
        )
        val gold = RavenGoldSitcomTopologyOS.direct(
            context = context,
            marker = marker,
            complex = complex,
            screen = screen,
            script = script,
            direction = direction,
            memory = seasonBase,
            backstage = backstage,
        )
        val season = RavenOfficeSeasonOS.snapshot(
            context = context,
            owner = member.id,
            secondary = gold.secondary?.id,
            canonicalMotif = script.motif,
            mesh = mesh,
        )
        val reserve = RavenEgoReserveProjectionOS.project(member, season)
        val series = RavenLongSeriesDialogueOS.select(member, screen, script, direction, bit, show, season, reserve, gold)
        RavenOfficeSeasonOS.markPresence(context, member.id, direction.sceneChanged, direction.rotated)

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

        val terminalScene = gold.terminal && !quiet && series.terminal.isNotBlank()
        // Long-series/Meta-Max/plot writers never bypass ordinary cadence except Gold terminal
        // settlement. Richness comes from better structure, not faster chatter.
        val speakNow = displayDecision.speak || terminalScene
        val useSeriesWriter = speakNow && !terminalScene && series.primary.isNotBlank() && (
            gold.reason == "gold-backstage-crosstalk" ||
                season.motifReturningAcrossSessions ||
                season.pairCount in setOf(3, 5, 8, 13, 21) ||
                season.memberLines in setOf(8, 13, 21, 34, 55) ||
                season.memberState == "EVOLVING" ||
                gold.phase == "CALLBACK" && (season.motifLifetimeCount >= 2 || bit.count >= 3) ||
                gold.phase == "ESCALATE" && season.memberLines >= 5 ||
                gold.phase == "OPEN" && season.episode > 1 && direction.turn % 7 == 0
            )
        val useMetaMaxWriter = speakNow && !terminalScene && metaMax.text.isNotBlank() && show.writerEligible && (
            show.level >= 3 || bit.shouldEscalate || bit.brick || script.interactionWorthSpeaking ||
                show.form in setOf("TITLE_CARD", "PREVIOUSLY_ON", "FOURTH_WALL_EMERGENCY")
            )
        val usePlotWriter = speakNow && !terminalScene && plot.crossover && plotDialogue.text.isNotBlank() &&
            (direction.turn % 3 == 0 || script.sceneChanged || script.returned || bit.shouldEscalate)
        val useScriptWriter = !terminalScene && scriptDialogue.text.isNotBlank() && (
            script.callbackEarned || script.interactionWorthSpeaking || script.interruption.isNotBlank() || script.returned ||
                (script.sceneChanged && direction.turn % 2 == 0) || direction.turn % 5 == 0
            )
        val useViewportWriter = !terminalScene && displayDecision.mode == RavenPresentationArbiterOS.Mode.SCREEN &&
            viewportDialogue.text.isNotBlank() &&
            (direction.turn % 4 == 0 || direction.beat in setOf("COLD_OPEN", "META", "CALLBACK") && direction.turn % 2 == 0)

        val stinger = if (speakNow && !terminalScene) when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> when {
                useSeriesWriter -> series.primary
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
                useSeriesWriter -> series.primary
                useMetaMaxWriter -> metaMax.text
                usePlotWriter -> plotDialogue.text
                useScriptWriter -> scriptDialogue.text
                else -> mayhem.text.ifBlank { metaPunch.text.ifBlank { character.text } }.trim()
            }
            else -> ""
        } else ""

        val goldAside = if (useSeriesWriter && series.secondary.isNotBlank() && gold.secondary != null) {
            val p = RavenEmployeePresentation.packet(gold.secondary, signal, detail, series.secondary)
            "↳ ${p.emojiSoup} ${gold.secondary.id} ${p.kaomoji} ${series.secondary}"
        } else if (useSeriesWriter && series.synthesis.isNotBlank() && gold.secondary == null) {
            series.synthesis
        } else ""

        val officeAside = if (speakNow && !terminalScene) when (displayDecision.mode) {
            RavenPresentationArbiterOS.Mode.SCREEN -> goldAside.ifBlank {
                sitcom.secondary.ifBlank {
                    if (screen.meta || show.chorusEligible) interruption.text.trim() else ""
                }
            }
            RavenPresentationArbiterOS.Mode.PHONE -> goldAside.ifBlank {
                if ("ERROR" in marker.tags || "PAYOFF" in complex.tags || "BOUNDARY" in marker.tags)
                    interruption.text.trim() else ""
            }
            else -> ""
        } else ""

        val spoken = when {
            terminalScene -> series.terminal
            !speakNow -> ""
            else -> buildString {
                append(truth)
                if (stinger.isNotBlank() && stinger != truth) {
                    if (isNotEmpty()) append("  ")
                    append(stinger)
                }
                if (officeAside.isNotBlank()) {
                    if (isNotEmpty()) append("  ")
                    append(officeAside)
                }
            }.replace(Regex("[ \\t]+"), " ").trim().take(640)
        }

        val callbackMoment = gold.phase == "CALLBACK" || bit.shouldEscalate || bit.brick
        if (spoken.isNotBlank()) {
            RavenSitcomDirectorOS.markSpoken(context, marker.at)
            RavenBackstageOS.markSpoken(member.id)
            RavenBackstageOS.markSpoken(gold.secondary?.id)
            if (!terminalScene) {
                RavenOfficeSeasonOS.markSpoken(
                    context = context,
                    owner = member.id,
                    secondary = gold.secondary?.id,
                    canonicalMotif = script.motif,
                    form = when {
                        useSeriesWriter -> series.family
                        useMetaMaxWriter -> show.form
                        usePlotWriter -> "PLOT"
                        useScriptWriter -> scriptDialogue.family
                        else -> direction.beat
                    },
                    meta = screen.meta || show.level >= 4,
                    callback = callbackMoment,
                )
            }
        }
        RavenGoldEpisodeStatsOS.recordDecision(spoken.isNotBlank(), callbackMoment, gold, backstage)
        val goldStats = RavenGoldEpisodeStatsOS.snapshot()

        val authorNote = when {
            terminalScene -> ""
            displayDecision.mode == RavenPresentationArbiterOS.Mode.SCREEN -> listOf(
                observation.text,
                script.continuity.takeIf { it.isNotBlank() && !observation.text.contains(it, true) }.orEmpty(),
            ).filter(String::isNotBlank).joinToString("  ").take(340)
            displayDecision.mode == RavenPresentationArbiterOS.Mode.DIAGNOSTIC -> diagnostic.text.take(240)
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
            append("|gold_phase:").append(gold.phase)
            append("|gold_comments:").append(goldStats.comments)
            append("|gold_silences:").append(goldStats.silences)
            append("|gold_crosstalk:").append(goldStats.crosstalk)
            append("|ego_state:").append(reserve.state)
            append("|season:").append(season.season)
            append("|season_episode:").append(season.episodeInSeason)
            append("|lifetime_episode:").append(season.episode)
            append("|member_lines:").append(season.memberLines)
            append("|pair_lifetime:").append(season.pairCount)
            append("|motif_lifetime:").append(season.motifLifetimeCount)
            if (backstage.candidate != null) {
                append("|backstage_candidate:").append(backstage.candidate.id)
                append("|backstage_pressure:").append(backstage.pressure)
            }
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
        val employeePresentation = RavenSceneExpressionOS.decorate(
            context, basePresentation, member, screen, direction, script, bit, show, season, reserve, gold,
        )
        val dialogueFamily = listOfNotNull(
            "PRESENTATION_${displayDecision.mode.name}",
            displayDecision.reason,
            observation.family.takeIf { authorNote.isNotBlank() && it.isNotBlank() },
            series.family.takeIf { (useSeriesWriter || terminalScene) && it.isNotBlank() },
            metaMax.family.takeIf { useMetaMaxWriter && it.isNotBlank() },
            plotDialogue.family.takeIf { usePlotWriter && it.isNotBlank() },
            scriptDialogue.family.takeIf { speakNow && useScriptWriter && it.isNotBlank() },
            sitcom.family.takeIf { speakNow && displayDecision.mode == RavenPresentationArbiterOS.Mode.SCREEN && it.isNotBlank() },
            viewportDialogue.family.takeIf { speakNow && useViewportWriter && it.isNotBlank() },
            "GOLD_${gold.phase}",
            "EGO_${reserve.state}",
            "SERIES_S${season.season}E${season.episodeInSeason}",
            "BACKSTAGE_${backstage.pressure}",
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
        val proof = "${sense.route}:${marker.source}:${marker.id}:${marker.key}:show=${displayDecision.mode.name}:sitcom=${direction.sceneId}:${direction.turn}:script=${script.act}:${script.motifCount}:action=${script.interaction}:bit=${bit.count}:${bit.tier}:meta=${show.level}:${show.form}:gold=${gold.phase}:${goldStats.comments}:${goldStats.silences}:series=${season.season}x${season.episodeInSeason}:ego=${reserve.state}:backstage=${backstage.pressure}:plot=${plot.score}:rv=${mesh.mode}:viewport=${viewport?.task ?: "none"}"
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
                "METAMAX_L${show.level}", "METAMAX_${show.form}", "GOLD_${gold.phase}", "EGO_${reserve.state}",
                "SERIES_S${season.season}E${season.episodeInSeason}", "RV_${mesh.mode}", "PLOT_${plot.score}",
            ) + (if (bit.active) setOf("BIT_${bit.tier}") else emptySet()) +
                (if (plot.crossover) setOf("PLOT_CROSSOVER") else emptySet()) +
                (if (season.motifReturningAcrossSessions) setOf("CROSS_SESSION_CALLBACK") else emptySet()) +
                (if (backstage.candidate != null) setOf("BACKSTAGE_READY") else emptySet()) +
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
        if (gold.terminal) RavenGoldEpisodeStatsOS.close()
        return Result(member, packet)
    }
}
