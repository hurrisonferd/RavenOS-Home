package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Canonical modular reaction engine for RavenOS Launcher.
 *
 * BRAIN -> LATE MODULE BUS -> ONE SETTLED PACKET -> SURFACE ROUTER.
 * Android services are hosts/transports; this object owns the reaction transaction.
 */
object RavenGoblinEngineOS {
    data class Result(
        val member: RavenOfficeMember,
        val packet: RavenReactionPacket,
        val residentText: String,
        val systems: List<String>,
        val surfaces: List<String>,
        val graphIssues: List<String>,
    ) {
        val receipt: String get() = buildString {
            append("systems=").append(systems.distinct().joinToString(">"))
            append(";surfaces=").append(surfaces.distinct().joinToString(">"))
            if (graphIssues.isNotEmpty()) append(";graph_issues=").append(graphIssues.joinToString(","))
        }
    }

    fun react(
        context: Context,
        signal: String,
        detail: String,
        manualOwner: String?,
        quiet: Boolean,
        hauntMode: RavenHauntMode,
    ): Result {
        val app = context.applicationContext
        val graphIssues = RavenGoblinSystemsRegistryOS.validateGraph()
        val bus = RavenGoblinBrainBusOS.react(
            context = app,
            signal = signal,
            detail = detail,
            manualOwner = manualOwner,
            quiet = quiet,
            hauntMode = hauntMode,
        )
        val settlement = RavenGoblinSurfaceRouterOS.settle(
            context = app,
            member = bus.member,
            packet = bus.packet,
            signal = signal,
            detail = detail,
            hauntMode = hauntMode,
            manual = manualOwner != null,
            quiet = quiet,
        )
        return Result(
            member = bus.member,
            packet = bus.packet,
            residentText = settlement.residentText,
            systems = (listOf("GOBLIN_ENGINE") + bus.systems + "SURFACE_ROUTER").distinct(),
            surfaces = settlement.surfaces,
            graphIssues = graphIssues,
        )
    }
}
