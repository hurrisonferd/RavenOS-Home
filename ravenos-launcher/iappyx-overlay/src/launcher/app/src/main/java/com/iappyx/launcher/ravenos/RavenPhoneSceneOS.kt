package com.iappyx.launcher.ravenos

import android.content.Context

/** Deterministic rolling scene model built only from already-authorized RavenOS markers. */
object RavenPhoneSceneOS {
    data class Scene(
        val activeApp: String?,
        val screen: String,
        val mediaHot: Boolean,
        val recentSwitches: Int,
        val events60s: Int,
        val notificationSource: String?,
        val recentKeys: List<String>,
    ) {
        fun compact(): String = buildString {
            append("APP=").append(activeApp ?: "?")
            append(" · SCREEN=").append(screen)
            append(" · MUSIC=").append(if (mediaHot) "HOT" else "QUIET")
            append(" · SWITCHES30=").append(recentSwitches)
            append(" · EVENTS60=").append(events60s)
            notificationSource?.let { append(" · PING=").append(it) }
        }
    }

    fun snapshot(context: Context, now: Long = System.currentTimeMillis()): Scene {
        val recent = RavenMarkerBus.recent(context, 64)
        val activeApp = recent.asReversed().firstOrNull { it.key == "APP_ENTER" }?.let(::appLabel)
        val screenMarker = recent.asReversed().firstOrNull {
            it.key == "DEVICE" || it.key == "NIGHT" || it.detail.contains("screen:", true)
        }
        val screen = when {
            screenMarker?.detail?.contains("screen:off", true) == true -> "OFF"
            screenMarker?.detail?.contains("screen:on", true) == true -> "ON"
            else -> "?"
        }
        val media = recent.asReversed().firstOrNull { it.key == "MEDIA_ACTIVE" }
        val mediaHot = media != null && now - media.at <= 10 * 60_000L
        val switches = recent.count { it.key == "APP_ENTER" && now - it.at <= 30_000L }
        val events = recent.count { now - it.at <= 60_000L }
        val notif = recent.asReversed().firstOrNull {
            it.key == "NOTIFICATION_POSTED" && now - it.at <= 2 * 60_000L
        }?.let(::appLabel)
        return Scene(
            activeApp = activeApp,
            screen = screen,
            mediaHot = mediaHot,
            recentSwitches = switches,
            events60s = events,
            notificationSource = notif,
            recentKeys = recent.takeLast(6).map { it.key },
        )
    }

    private fun appLabel(marker: RavenMarkerBus.Marker): String? {
        val named = Regex("(?:^|\\|)app:([^|]+)").find(marker.detail)?.groupValues?.getOrNull(1)?.trim()
        if (!named.isNullOrBlank()) return named.take(40)
        val pkg = Regex("(?:^|\\|)package:([^|\\s]+)").find(marker.detail)?.groupValues?.getOrNull(1)?.trim()
        return pkg?.substringAfterLast('.')?.take(40)?.takeIf { it.isNotBlank() }
    }
}
