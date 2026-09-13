package com.iappyx.launcher.ravenos

import android.content.Context

/** Deterministic rolling scene model built only from already-authorized RavenOS markers. */
object RavenPhoneSceneOS {
    data class Scene(
        val activeApp: String?,
        val screen: String,
        val mediaHot: Boolean,
        val mediaTitle: String?,
        val mediaArtist: String?,
        val recentSwitches: Int,
        val events60s: Int,
        val notificationSource: String?,
        val notificationBurst: Int,
        val notificationAlerting: Boolean,
        val goblinEyeActive: Boolean,
        val lastVisualMotion: Int?,
        val recentKeys: List<String>,
        val appPackage: String? = null,
        val appKind: String? = null,
        val appSummary: String? = null,
        val appContinuity: String = "UNKNOWN",
        val appDwellSeconds: Long = 0L,
        val appReturnCount: Int = 0,
        val appPrevious: String? = null,
        val appConfidence: Int = 0,
        val appSource: String? = null,
        val sameAppUpdates: Int = 0,
    ) {
        val sameApp: Boolean get() = appContinuity in setOf("STAY", "RESUME", "RETURN")
        fun compact(): String = buildString {
            append("FOCUS=").append(activeApp ?: "?")
            if (appContinuity != "UNKNOWN") append(" · APP_BEAT=").append(appContinuity)
            if (appDwellSeconds > 0) append(" · DWELL=").append(appDwellSeconds).append("s")
            if (sameAppUpdates > 0) append(" · SAME=").append(sameAppUpdates)
            if (!appPrevious.isNullOrBlank()) append(" · PREV=").append(appPrevious)
            append(" · SCREEN=").append(screen)
            append(" · MUSIC=").append(if (mediaHot) "HOT" else "QUIET")
            mediaTitle?.let { append(" · TRACK=").append(it.take(34)) }
            append(" · SWITCHES30=").append(recentSwitches)
            append(" · EVENTS60=").append(events60s)
            notificationSource?.let { append(" · PING=").append(it) }
            if (notificationBurst > 1) append("×").append(notificationBurst)
            if (notificationAlerting) append("!")
            append(" · EYE=").append(if (goblinEyeActive) "ON" else "OFF")
            lastVisualMotion?.let { append(" · MOTION=").append(it).append('%') }
        }
    }

    fun snapshot(context: Context, now: Long = System.currentTimeMillis()): Scene {
        val recent = RavenMarkerBus.recent(context, 64)
        val appSession = RavenAppSessionOS.current(context, now)
        val focusMarker = recent.asReversed().firstOrNull {
            it.key == "APP_ENTER" || it.key == "HOME_ENTER" || it.key == "SYSTEM_DECK_OPENED" ||
                it.key == "SEARCH_OPENED" || it.key == "APP_UNIVERSE_OPENED"
        }
        // A live ordinary-app session is the foreground authority. Marker-based Home/Search/System
        // Deck labels are fallback only, so notifications/unlock/overlay callbacks cannot steal the
        // scene from Suno, ChatGPT, Chrome, or any other app the owner is still using.
        val focus = appSession?.label?.ifBlank { appSession.packageName.substringAfterLast('.') } ?: when (focusMarker?.key) {
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

        val session = recent.asReversed().firstOrNull { it.key == "MEDIA_SESSION" }
        val simpleMedia = recent.asReversed().firstOrNull { it.key == "MEDIA_ACTIVE" || it.key == "MEDIA_IDLE" }
        val sessionState = session?.let { field(it.detail, "state") }
        val mediaHot = when {
            session != null && now - session.at <= 10 * 60_000L -> sessionState == "PLAYING" || sessionState == "BUFFERING"
            else -> simpleMedia?.key == "MEDIA_ACTIVE" && now - (simpleMedia?.at ?: 0L) <= 10 * 60_000L
        }

        val switches = recent.count { it.key == "APP_ENTER" && now - it.at <= 30_000L }
        val events = recent.count { now - it.at <= 60_000L }
        val notifMarker = recent.asReversed().firstOrNull {
            it.key == "NOTIFICATION_POSTED" && now - it.at <= 2 * 60_000L
        }
        val visual = recent.asReversed().firstOrNull {
            it.key == "SCREEN_VISUAL" && it.detail.contains("state:changed") && now - it.at <= 2 * 60_000L
        }
        return Scene(
            activeApp = focus,
            screen = screen,
            mediaHot = mediaHot,
            mediaTitle = session?.let { field(it.detail, "title") },
            mediaArtist = session?.let { field(it.detail, "artist") },
            recentSwitches = switches,
            events60s = events,
            notificationSource = notifMarker?.let(::appLabel),
            notificationBurst = notifMarker?.let { field(it.detail, "burst")?.toIntOrNull() } ?: 0,
            notificationAlerting = notifMarker?.let { field(it.detail, "alerting") == "true" } ?: false,
            goblinEyeActive = RavenScreenWatchService.isActive(context),
            lastVisualMotion = visual?.let { field(it.detail, "motion")?.toIntOrNull() },
            recentKeys = recent.takeLast(7).map { it.key },
            appPackage = appSession?.packageName,
            appKind = appSession?.kind,
            appSummary = appSession?.summary,
            appContinuity = appSession?.transition ?: "UNKNOWN",
            appDwellSeconds = appSession?.dwellMs?.div(1000L) ?: 0L,
            appReturnCount = appSession?.returnCount ?: 0,
            appPrevious = appSession?.previousLabel,
            appConfidence = appSession?.confidence ?: 0,
            appSource = appSession?.source,
            sameAppUpdates = appSession?.sameAppUpdates ?: 0,
        )
    }

    private fun appLabel(marker: RavenMarkerBus.Marker): String? {
        val named = field(marker.detail, "app")
        if (!named.isNullOrBlank()) return named.take(40)
        return field(marker.detail, "package")?.substringAfterLast('.')?.take(40)?.takeIf { it.isNotBlank() }
    }

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)").find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
}
