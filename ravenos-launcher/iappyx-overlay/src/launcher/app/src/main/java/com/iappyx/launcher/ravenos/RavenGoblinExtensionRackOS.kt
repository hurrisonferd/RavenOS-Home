package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Executable late-module rack for Goblin Brain.
 *
 * RavenGoblinBrain owns the evidence/cast/scene transaction. Extensions here may enrich the settled
 * packet, but they may not invent a second source of truth or bypass the module load plan. This is
 * the preferred integration seam for recovered/new non-core organs.
 */
object RavenGoblinExtensionRackOS {
    data class Frame(
        val context: Context,
        val packet: RavenReactionPacket,
        val previousOwner: String,
        val quiet: Boolean,
        val hauntMode: RavenHauntMode,
        val pressure: RavenOmniRvExpressionBudget.Pressure,
        val enabledIds: Set<String>,
    )

    data class ExtensionResult(
        val packet: RavenReactionPacket,
        val systems: List<String> = emptyList(),
    )

    data class Result(
        val packet: RavenReactionPacket,
        val systems: List<String>,
    )

    interface Extension {
        val id: String
        fun apply(frame: Frame): ExtensionResult
    }

    private val extensions: List<Extension> = listOf(
        MetaGrammarExtension,
        KnowledgeBrokerExtension,
    )

    fun ids(): List<String> = extensions.map { it.id }

    fun validate(): List<String> {
        val issues = mutableListOf<String>()
        val ids = ids()
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach {
            issues += "duplicate_extension:$it"
        }
        val registered = RavenGoblinSystemsRegistryOS.organs.mapTo(hashSetOf()) { it.id }
        ids.filterNot { it in registered }.forEach { issues += "unregistered_extension:$it" }
        return issues.distinct()
    }

    fun apply(frame: Frame): Result {
        var packet = frame.packet
        val systems = mutableListOf("LATE_EXTENSION_RACK")
        for (extension in extensions) {
            if (extension.id !in frame.enabledIds) continue
            val result = extension.apply(frame.copy(packet = packet))
            packet = result.packet
            systems += result.systems
        }
        if ("KNOWLEDGE_BROKER" !in frame.enabledIds) systems += "KNOWLEDGE_SHED"
        return Result(packet, systems.distinct())
    }

    private object MetaGrammarExtension : Extension {
        override val id: String = "META_GRAMMAR"

        override fun apply(frame: Frame): ExtensionResult {
            val packet = frame.packet
            if (packet.dialogue.isBlank()) return ExtensionResult(packet)

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
                frame.quiet -> "CHIP"
                presentation == "SCREEN" && (metaLevel >= 4 || frame.hauntMode == RavenHauntMode.APOCALYPSE) -> "BOARD_MEETING"
                presentation == "SCREEN" -> "DESK"
                presentation == "PHONE" -> "PEEK"
                else -> "CHIP"
            }
            val pulse = RavenRVResilienceOS.snapshot(frame.context, packet.updatedAt)
            val launcherCritical = packet.signal in setOf("HOME", "HOME_ENTER", "APP_LAUNCH", "SEARCH", "APP_UNIVERSE") &&
                pulse.mode in setOf("COCKPIT_OFFLINE", "PHONE_LIMP_HOME")

            val meta = RavenGoblinMetaPipelineOS.enrich(
                RavenGoblinMetaPipelineOS.Input(
                    context = frame.context,
                    owner = packet.owner,
                    previousOwner = frame.previousOwner,
                    event = packet.signal,
                    baseDialogue = packet.dialogue,
                    evidence = packet.proof,
                    motif = motif,
                    callback = callback,
                    occurrence = packet.occurrence,
                    surface = surface,
                    semanticFamily = semanticFamily,
                    quiet = frame.quiet,
                    launcherCritical = launcherCritical,
                    pressure = frame.pressure,
                    hauntMode = frame.hauntMode,
                    metaLevel = metaLevel,
                    sceneId = sceneId(packet),
                )
            )
            if (meta.dialogue.isBlank()) return ExtensionResult(packet)

            val systems = mutableListOf("META_GRAMMAR")
            if (meta.antiRepeatFingerprint.isNotBlank()) systems += "ANTI_REPEAT"
            if (meta.ensemble.isNotBlank()) systems += "ENSEMBLE"
            return ExtensionResult(
                packet = packet.copy(
                    dialogue = meta.dialogue,
                    dialogueFamily = listOf(packet.dialogueFamily, "META_${meta.stage}", meta.trickId)
                        .filter(String::isNotBlank).distinct().joinToString("+"),
                    complexTags = packet.complexTags + meta.tags,
                    proof = packet.proof + ":late_meta=${meta.stage}:${meta.trickId}:${meta.budgetReason}",
                ),
                systems = systems,
            )
        }
    }

    private object KnowledgeBrokerExtension : Extension {
        override val id: String = "KNOWLEDGE_BROKER"

        override fun apply(frame: Frame): ExtensionResult {
            val knowledge = RavenKnowledgeBrokerOS.observe(frame.context, frame.packet)
            if (!knowledge.available) return ExtensionResult(frame.packet)

            val metaLevel = frame.packet.complexTags.firstOrNull { it.startsWith("METAMAX_L") }
                ?.removePrefix("METAMAX_L")?.toIntOrNull() ?: 0
            val inject = shouldInjectKnowledge(
                packet = frame.packet,
                knowledge = knowledge,
                hauntMode = frame.hauntMode,
                pressure = frame.pressure,
                metaLevel = metaLevel,
                quiet = frame.quiet,
            )
            return ExtensionResult(
                packet = frame.packet.copy(
                    dialogue = if (inject) appendKnowledge(frame.packet.dialogue, knowledge) else frame.packet.dialogue,
                    complexTags = frame.packet.complexTags + setOf("WEB_KNOWLEDGE", "WEB_${knowledge.provider}"),
                    proof = frame.packet.proof + ":web=${knowledge.provider}:${knowledge.scope}:${knowledge.ageMs}ms",
                ),
                systems = listOf("KNOWLEDGE_${knowledge.scope}"),
            )
        }
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

    private fun sceneId(packet: RavenReactionPacket): String = packet.proof.substringAfter("sitcom=", "")
        .substringBefore(':').ifBlank { packet.markerId }

    private fun field(detail: String, name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
        .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
}
