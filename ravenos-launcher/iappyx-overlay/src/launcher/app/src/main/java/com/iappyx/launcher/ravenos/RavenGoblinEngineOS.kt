package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Canonical modular Goblin Brain engine for RavenOS Launcher.
 *
 * INGRESS API -> Android host/transport -> BRAIN CORE -> MODULE RACK -> EXTENSIONS
 * -> ONE SETTLED PACKET -> SURFACE ROUTER.
 *
 * Callers should depend on this facade rather than depending on Office Bar as if a UI surface were
 * the brain. Recovered/new organs belong in the registry/racks so the engine can enumerate, shed,
 * verify and recover them deterministically.
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

    /** Stable ingress facade. Office Bar remains the Android foreground-service transport for now. */
    fun signal(context: Context, signal: String, detail: String = "") =
        RavenOfficeBarService.signal(context, signal, detail)

    fun pin(context: Context, owner: String) = RavenOfficeBarService.pin(context, owner)
    fun next(context: Context) = RavenOfficeBarService.next(context)
    fun auto(context: Context) = RavenOfficeBarService.auto(context)
    fun toggleQuiet(context: Context) = RavenOfficeBarService.toggleQuiet(context)
    fun enable(context: Context) = RavenOfficeBarService.enable(context)
    fun disable(context: Context) = RavenOfficeBarService.disable(context)
    fun cycleHaunt(context: Context) = RavenOfficeBarService.cycleHaunt(context)
    fun setHaunt(context: Context, mode: RavenHauntMode) = RavenOfficeBarService.setHaunt(context, mode)
    fun restore(context: Context, reason: String) = RavenOfficeBarService.restore(context, reason)

    /** Human/device-readable current module topology without causing a reaction. */
    fun status(context: Context): String {
        val app = context.applicationContext
        val pulse = RavenRVResilienceOS.snapshot(app, System.currentTimeMillis())
        val pressure = RavenGoblinModuleRackOS.pressureFor(pulse)
        val plan = RavenGoblinModuleRackOS.plan(pressure)
        val issues = (plan.issues + RavenGoblinExtensionRackOS.validate()).distinct()
        return buildString {
            append(RavenGoblinModuleRackOS.compact(pressure))
            append(" · extensions=").append(RavenGoblinExtensionRackOS.ids().joinToString(","))
            append(" · surface=ONE_PACKET")
            if (issues.isNotEmpty()) append(" · issues=").append(issues.joinToString(","))
        }
    }

    /** One deterministic reaction transaction for every accepted RavenOS event. */
    fun react(
        context: Context,
        signal: String,
        detail: String,
        manualOwner: String?,
        quiet: Boolean,
        hauntMode: RavenHauntMode,
    ): Result {
        val app = context.applicationContext
        val graphIssues = RavenGoblinSystemsRegistryOS.validateGraph().toMutableList()
        val bus = RavenGoblinBrainBusOS.react(
            context = app,
            signal = signal,
            detail = detail,
            manualOwner = manualOwner,
            quiet = quiet,
            hauntMode = hauntMode,
        )
        graphIssues += bus.moduleIssues
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
            graphIssues = graphIssues.distinct(),
        )
    }
}
