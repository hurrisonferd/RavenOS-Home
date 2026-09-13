package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Canonical RavenOS Goblin Brain late-module bus.
 *
 * RavenGoblinBrain settles phone/screen evidence, cast, scene, episode, callbacks and base dialogue.
 * The dependency-aware module rack then decides which additive organs may run under current Omni RV
 * pressure. Executable extensions enrich one packet; they may decorate but never rewrite evidence.
 */
object RavenGoblinBrainBusOS {
    data class Result(
        val member: RavenOfficeMember,
        val packet: RavenReactionPacket,
        val systems: List<String>,
        val moduleIssues: List<String>,
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
        val pressure = RavenGoblinModuleRackOS.pressureFor(rv)
        val plan = RavenGoblinModuleRackOS.plan(pressure)
        val enabledOrgans = plan.enabledIds
        systems += "OMNI_RV_${pressure.name}"
        systems += "MODULE_RACK_${plan.enabled.size}_${RavenGoblinSystemsRegistryOS.organs.size}"
        // Compatibility receipt for older diagnostics that still search REGISTRY_*.
        systems += "REGISTRY_${plan.enabled.size}_${RavenGoblinSystemsRegistryOS.organs.size}"

        val late = RavenGoblinExtensionRackOS.apply(
            RavenGoblinExtensionRackOS.Frame(
                context = app,
                packet = packet,
                previousOwner = previousOwner,
                quiet = quiet,
                hauntMode = hauntMode,
                pressure = pressure,
                enabledIds = enabledOrgans,
            )
        )
        packet = late.packet
        systems += late.systems

        val moduleIssues = (plan.issues + RavenGoblinExtensionRackOS.validate()).distinct()
        val systemReceipt = systems.distinct().joinToString(">")
        packet = packet.copy(
            proof = buildString {
                append(packet.proof)
                append(":rack=").append(pressure.name).append(':').append(plan.enabled.size)
                    .append('/').append(RavenGoblinSystemsRegistryOS.organs.size)
                if (moduleIssues.isNotEmpty()) append(":rack_issues=").append(moduleIssues.joinToString(","))
                append(":bus=").append(systemReceipt)
            },
            complexTags = packet.complexTags + setOf("GOBLIN_BRAIN_BUS", "GOBLIN_MODULE_RACK"),
        )
        return Result(base.member, packet, systems.distinct(), moduleIssues)
    }
}
