package com.iappyx.launcher.ravenos

import android.content.Context

/** Provider-health ledger for RavenOS phone sensing lanes. Missing/degraded data never means an event did not happen. */
object RavenTelemetryPackOS {
    enum class State { HEALTHY, DEGRADED, OFFLINE }
    data class Provider(val name: String, val state: State, val fallback: String)

    fun providers(context: Context): List<Provider> {
        val awareness = RavenAwarenessStatus.snapshot(context)
        return listOf(
            Provider("LAUNCHER_NATIVE", if (awareness.defaultHome) State.HEALTHY else State.DEGRADED, "ANDROID_CALLBACK"),
            Provider("FOREGROUND_APP", if (awareness.foregroundAwareness) State.HEALTHY else State.OFFLINE, "LAUNCHER_NATIVE"),
            Provider("NOTIFICATION_SOURCE", if (awareness.notificationAwareness) State.HEALTHY else State.OFFLINE, "SILENCE"),
            Provider("GOBLIN_OVERLAY", if (awareness.overlayAccess && awareness.followMeEnabled) State.HEALTHY else if (awareness.overlayAccess) State.DEGRADED else State.OFFLINE, "HOME_PRESENTATION"),
            Provider("OFFICE_BAR", if (awareness.officeBarEnabled) State.HEALTHY else State.OFFLINE, "HOME_PRESENTATION"),
            Provider("TASKER_BRIDGE", if (RavenTaskerBridge.isTaskerInstalled(context)) State.HEALTHY else State.OFFLINE, "NONE"),
        )
    }

    fun compact(context: Context): String = providers(context).joinToString(" · ") {
        "${it.name}=${it.state.name}${if (it.state != State.HEALTHY) "→${it.fallback}" else ""}"
    }
}
