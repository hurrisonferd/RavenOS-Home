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
            append("FOCUS=").append(activeApp ?: "?")
            append(" · SCREEN=").append(screen)
            append(" · MUSIC=").append(if (mediaHot) "HOT" else "QUIET")
            append(" · SWITCHES30=").append(recentSwitches)
            append(" · EVENTS60=").append(events60s)
            notificationSource?.let { append(" · PING=").append(it) }
        }
    }

    fun snapshot(context: Context, now: Long = System.currentTimeMillis()): Scene {
        val recent = RavenMarkerBus.recent(context, 64)
        val focusMarker = recent.asReversed().firstOrNull {
            it.key == "APP_ENTER" || it.key == "HOME_ENTER" || it.key == "SYSTEM_DECK_OPENED" ||
                it.key == "SEARCH_OPENED" || it.key == "APP_UNIVERSE_OPENED"
        }
        val focus = when (focusMarker?.key) {
            "APP_ENTER" -> appLabel(focusMarker)
            "HOME_ENTER" -> "RavenOS Home"
            "SYSTEM_DECK_OPENED" -> "System Deck"
            "SEARCH_OPENED" -> "Search"
            "APP_UNIVERSE_OPENED" -> "App Universe"
            else -> null
        }
        val screenMarker = recent.asReversed().firstOrNull {
            it.key == "DEVICE" || it.key == "NIGHT" || it.detail.contains("screen:", true)
        }
        val screen = when {
            screenMarker?.detail?.contains("screen:off", true) == true -> "OFF"
            screenMarker?.detail?.contains("screen:on", true) == true -> "ON"
            else -> "?"
        }
        val mediaMarker = recent.asReversed().firstOrNull { it.key == "MEDIA_ACTIVE" || it.key == "MEDIA_IDLE" }
        val mediaHot = mediaMarker?.key == "MEDIA_ACTIVE" && now - mediaMarker.at <= 10 * 60_000L
        val switches = recent.count { it.key == "APP_ENTER" && now - it.at <= 30_000L }
        val events = recent.count { now - it.at <= 60_000L }
        val notif = recent.asReversed().firstOrNull {
            it.key == "NOTIFICATION_POSTED" && now - it.at <= 2 * 60_000L
        }?.let(::appLabel)
        return Scene(
            activeApp = focus,
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
