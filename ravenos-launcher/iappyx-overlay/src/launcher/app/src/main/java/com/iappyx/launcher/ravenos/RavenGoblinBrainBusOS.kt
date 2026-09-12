package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Canonical RavenOS meta bus.
 *
 * RavenGoblinBrain settles phone/screen evidence, cast, scene, episode, callbacks and base dialogue.
 * This bus then applies bounded late-stage systems that may decorate but never rewrite evidence:
 * Omni RV pressure -> meta trick grammar -> anti-repeat -> web knowledge -> final packet receipt.
 */
object RavenGoblinBrainBusOS {
    data class Result(
        val member: RavenOfficeMember,
        val packet: RavenReactionPacket,
        val systems: List<String>,
    )

    fun react(
        context: Context,
        signal: String,
        detail: String,
        manualOwner: String?,
        quiet: Boolean,
        hauntMode: RavenHauntMode,
    ): Result {
        val app = context.applicationContext
        val previousOwner = RavenOfficeStateStore.read(app)?.owner.orEmpty()
        val base = RavenGoblinBrain.react(app, signal, detail, manualOwner, quiet, hauntMode)
        var packet = base.packet
        val systems = mutableListOf(
            "MARKER", "LOCAL_SENSE", "SCENE", "CALLBACK", "EPISODE", "SITCOM_DIRECTOR",
            "BIT_LEDGER", "SHOWRUNNER", "SEASON", "LONG_SERIES", "PRESENTATION_ARBITER", "EXPRESSION",
        )

        val rv = RavenRVResilienceOS.snapshot(app, packet.updatedAt)
        val pressure = pressure(rv)
        systems += "OMNI_RV_${pressure.name}"

        val motif = field(packet.detail, "script_motif").orEmpty()
        val callback = when {
            "CROSS_SESSION_CALLBACK" in packet.complexTags -> "CROSS_SESSION_CALLBACK"
            packet.complexTags.any { it == "GOLD_CALLBACK" || it == "META_STAGE_CALLBACK" } -> "CALLBACK"
            else -> ""
        }
        val presentation = packet.complexTags.firstOrNull { it.startsWith("PRESENTATION_") }
            ?.removePrefix("PRESENTATION_").orEmpty()
        val metaLevel = packet.complexTags.firstOrNull { it.startsWith("METAMAX_L") }
            ?.removePrefix("METAMAX_L")?.toIntOrNull() ?: 0
        val semanticKind = field(packet.detail, "screen_kind").orEmpty()
        val semanticFamily = when {
            field(packet.detail, "screen_meta") == "true" || metaLevel >= 4 -> "META"
            semanticKind in setOf("CODE", "TERMINAL", "SETTINGS") -> "FOCUS"
            packet.complexTags.any { it.contains("ERROR") || it.contains("BUG") } -> "FAILURE"
            packet.complexTags.any { it.contains("PAYOFF") } -> "DELIGHT"
            else -> "WATCH"
        }
        val surface = when {
            quiet -> "CHIP"
            presentation == "SCREEN" && (metaLevel >= 4 || hauntMode == RavenHauntMode.APOCALYPSE) -> "BOARD_MEETING"
            presentation == "SCREEN" -> "DESK"
            presentation == "PHONE" -> "PEEK"
            else -> "CHIP"
        }
        val launcherCritical = packet.signal in setOf("HOME", "HOME_ENTER", "APP_LAUNCH", "SEARCH", "APP_UNIVERSE") &&
            rv.mode in setOf("COCKPIT_OFFLINE", "PHONE_LIMP_HOME")

        if (packet.dialogue.isNotBlank()) {
            val meta = RavenGoblinMetaPipelineOS.enrich(
                RavenGoblinMetaPipelineOS.Input(
                    context = app,
                    owner = packet.owner,
                    previousOwner = previousOwner,
                    event = packet.signal,
                    baseDialogue = packet.dialogue,
                    evidence = packet.proof,
                    motif = motif,
                    callback = callback,
                    occurrence = packet.occurrence,
                    surface = surface,
                    semanticFamily = semanticFamily,
                    quiet = quiet,
                    launcherCritical = launcherCritical,
                    pressure = pressure,
                    hauntMode = hauntMode,
                    metaLevel = metaLevel,
                    sceneId = sceneId(packet),
                )
            )
            if (meta.dialogue.isNotBlank()) {
                packet = packet.copy(
                    dialogue = meta.dialogue,
                    dialogueFamily = listOf(packet.dialogueFamily, "META_${meta.stage}", meta.trickId)
                        .filter(String::isNotBlank).distinct().joinToString("+"),
                    complexTags = packet.complexTags + meta.tags,
                    proof = packet.proof + ":late_meta=${meta.stage}:${meta.trickId}:${meta.budgetReason}",
                )
                systems += "META_GRAMMAR"
                if (meta.antiRepeatFingerprint.isNotBlank()) systems += "ANTI_REPEAT"
                if (meta.ensemble.isNotBlank()) systems += "ENSEMBLE"
            }
        }

        val knowledge = RavenKnowledgeBrokerOS.observe(app, packet)
        if (knowledge.available) {
            systems += "KNOWLEDGE_${knowledge.scope}"
            val inject = shouldInjectKnowledge(packet, knowledge, hauntMode, pressure, metaLevel, quiet)
            packet = packet.copy(
                dialogue = if (inject) appendKnowledge(packet.dialogue, knowledge) else packet.dialogue,
                complexTags = packet.complexTags + setOf("WEB_KNOWLEDGE", "WEB_${knowledge.provider}"),
                proof = packet.proof + ":web=${knowledge.provider}:${knowledge.scope}:${knowledge.ageMs}ms",
            )
        }

        val systemReceipt = systems.distinct().joinToString(">")
        packet = packet.copy(
            proof = packet.proof + ":bus=$systemReceipt",
            complexTags = packet.complexTags + "GOBLIN_BRAIN_BUS",
        )
        return Result(base.member, packet, systems.distinct())
    }

    private fun shouldInjectKnowledge(
        packet: RavenReactionPacket,
        knowledge: RavenKnowledgeBrokerOS.ContextualKnowledge,
        hauntMode: RavenHauntMode,
        pressure: RavenOmniRvExpressionBudget.Pressure,
        metaLevel: Int,
        quiet: Boolean,
    ): Boolean {
        if (quiet || packet.dialogue.isBlank()) return false
        if (pressure in setOf(RavenOmniRvExpressionBudget.Pressure.HOT, RavenOmniRvExpressionBudget.Pressure.CRITICAL)) return false
        if (knowledge.scope == "CONTEXTUAL" && hauntMode.ordinal >= RavenHauntMode.HAUNTED.ordinal) return packet.occurrence % 5 == 0 || metaLevel >= 4
        if (knowledge.scope == "SELF_REPO" && metaLevel >= 4) return packet.occurrence % 8 == 0
        return false
    }

    private fun appendKnowledge(text: String, knowledge: RavenKnowledgeBrokerOS.ContextualKnowledge): String {
        val web = "WEB CONTEXT · ${knowledge.source}: ${knowledge.summary}"
        if (text.contains(knowledge.summary.take(32), ignoreCase = true)) return text
        return listOf(text, web).filter(String::isNotBlank).joinToString("  ⟡  ").take(980)
    }

    private fun pressure(pulse: RavenRVResilienceOS.Pulse): RavenOmniRvExpressionBudget.Pressure = when {
        pulse.mode == "COCKPIT_OFFLINE" || pulse.score < 25 -> RavenOmniRvExpressionBudget.Pressure.CRITICAL
        pulse.mode == "PHONE_LIMP_HOME" || pulse.score < 45 -> RavenOmniRvExpressionBudget.Pressure.HOT
        pulse.score < 70 -> RavenOmniRvExpressionBudget.Pressure.BUSY
        else -> RavenOmniRvExpressionBudget.Pressure.NOMINAL
    }

    private fun sceneId(packet: RavenReactionPacket): String = packet.proof.substringAfter("sitcom=", "")
        .substringBefore(':').ifBlank { packet.markerId }

    private fun field(detail: String, name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
        .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
}
