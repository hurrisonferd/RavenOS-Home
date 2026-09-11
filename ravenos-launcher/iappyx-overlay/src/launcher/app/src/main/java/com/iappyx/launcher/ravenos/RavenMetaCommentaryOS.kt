package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Deterministic whole-phone meta commentary.
 *
 * Converts the current marker plus recent local marker history into a concise observation about
 * what Raven is actually doing on the phone. No model call, screen scraping, notification-body
 * access, or effect authority is involved.
 */
object RavenMetaCommentaryOS {
    data class Commentary(val text: String, val family: String, val noveltyKey: String)

    fun compose(
        context: Context,
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        episode: RavenEpisodeOS.Phase,
    ): Commentary {
        val history = RavenMarkerBus.recent(context, 18)
        val prior = history.dropLast(1)
        val previous = prior.lastOrNull()
        val previousApp = prior.asReversed().firstOrNull { it.key == "APP_ENTER" }?.let(::appLabel)
        val app = appLabel(marker)
        val elapsed = previous?.let { (marker.at - it.at).coerceAtLeast(0L) } ?: Long.MAX_VALUE
        val fast = elapsed < 12_000L
        val n = complex.occurrence

        val text = when (marker.key) {
            "APP_ENTER" -> when {
                previous?.key == "APP_ENTER" && previousApp != null && app != null && previousApp != app && fast ->
                    "$previousApp → $app in ${elapsed / 1000}s. The phone just changed jobs mid-sentence."
                previous?.key == "HOME_ENTER" && app != null && fast ->
                    "Back out of Home and straight into $app. That's a deliberate re-entry, not idle drift."
                "APP_SWITCH_BURST" in complex.tags && app != null ->
                    "$app joined an app-switch burst: ${complex.recentSwitches} transitions in fifteen seconds. Tiny task tornado confirmed."
                "RETURN_LOOP" in complex.tags && app != null ->
                    "$app again. Return loop #$n; this app is part of the current ritual now."
                "MUSIC" in marker.tags && app != null ->
                    "$app entered while the music lane is active. The soundtrack is steering the workspace again."
                app != null ->
                    "$app is foreground. ${mythLine(n)}"
                else -> "Foreground changed. ${mythLine(n)}"
            }
            "HOME_ENTER" -> when {
                previousApp != null && fast -> "Home after $previousApp. Reset point reached in ${elapsed / 1000}s."
                n >= 5 -> "Home checkpoint #$n. This is infrastructure now, not scenery."
                else -> "Home is the current staging ground."
            }
            "MEDIA_ACTIVE" -> when {
                "MUSIC" in marker.tags && previousApp != null -> "Music is active while $previousApp is in the work loop. Soundtrack lane has the floor."
                "MUSIC" in marker.tags -> "Music field active. Let the launcher react to motion instead of repeating a status card."
                else -> "Media state changed. Treat it as context, not decoration."
            }
            "NOTIFICATION_POSTED" -> when {
                app != null -> "$app pinged from the notification lane. Source noted; body stays private."
                previousApp != null -> "A notification landed while the recent work loop was around $previousApp. Source-only awareness; no message-body peeking."
                else -> "Notification source changed. Metadata only."
            }
            "SYSTEM_DECK_OPENED" -> when {
                previous?.key == "MEDIA_ACTIVE" -> "System Deck immediately after media activity. Raven is tuning the machine while the soundtrack is hot."
                previousApp != null -> "System Deck opened after $previousApp. Configuration just became part of the task, not a side quest."
                else -> "System Deck opened. This is an intervention, not passive browsing."
            }
            "SEARCH_OPENED" -> when {
                previousApp != null -> "Search after $previousApp. The phone just switched from doing to finding."
                else -> "Search lane opened. Discovery mode."
            }
            "APP_UNIVERSE_OPENED" -> "App universe opened. Raven is choosing the next tool rather than wandering the drawer."
            "ROOM_CHANGED" -> "Launcher room changed. Surface context moved without pretending the underlying task changed."
            "POWER_CHANGED" -> if (marker.detail.contains("connect", true))
                "Power connected. The machine just gained endurance; background work can breathe a little."
            else "Power state changed."
            "BATTERY_CHANGED" -> if (marker.detail.contains("low", true) || marker.detail.contains("critical", true))
                "Battery pressure is real. Reduce decorative churn before useful awareness."
            else "Battery state changed; no drama required."
            "NIGHT" -> if (marker.detail.contains("screen:off", true))
                "Screen went dark. NYX rules apply: stop performing when nobody is looking."
            else "Night lane changed."
            "DEVICE" -> when {
                marker.detail.contains("screen:on", true) -> "Screen woke. Resume context without replaying the same greeting."
                marker.detail.contains("screen:off", true) -> "Screen slept. Commentary should sleep with it."
                else -> "Device state changed: ${marker.detail.take(90)}"
            }
            else -> when {
                "PAYOFF" in complex.tags -> "Payoff detected. The recurring thing finally changed state."
                "RECOVERY_ARC" in complex.tags -> "Recovery arc detected. Something noisy just returned to usable."
                "RUNNING_BIT" in complex.tags -> "${marker.key.replace('_', ' ')} is a running bit now — occurrence $n."
                marker.detail.isNotBlank() -> "${marker.key.replace('_', ' ')} · ${marker.detail.take(110)}"
                else -> marker.key.replace('_', ' ')
            }
        }.trim()

        val family = when {
            "APP_SWITCH_BURST" in complex.tags -> "META_SWITCH_BURST"
            "RETURN_LOOP" in complex.tags -> "META_RETURN_LOOP"
            "PAYOFF" in complex.tags -> "META_PAYOFF"
            marker.key == "APP_ENTER" -> "META_APP"
            marker.key == "MEDIA_ACTIVE" -> "META_MEDIA"
            marker.key == "HOME_ENTER" -> "META_HOME"
            else -> "META_${marker.key}"
        }
        val noveltyKey = listOf(member.id, family, app ?: "", previousApp ?: "", mythBand(n), marker.detail.substringBefore('|')).joinToString("|")
        return Commentary(text, family, noveltyKey)
    }

    private fun appLabel(marker: RavenMarkerBus.Marker): String? {
        val detail = marker.detail
        val named = Regex("(?:^|\\|)app:([^|]+)").find(detail)?.groupValues?.getOrNull(1)?.trim()
        if (!named.isNullOrBlank()) return named.take(48)
        val pkg = Regex("(?:^|\\|)package:([^|\\s]+)").find(detail)?.groupValues?.getOrNull(1)?.trim()
        return pkg?.substringAfterLast('.')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }?.takeIf { it.isNotBlank() }
    }

    private fun mythLine(n: Int): String = when {
        n >= 34 -> "Historic landmark #$n."
        n >= 21 -> "Local mythology #$n."
        n >= 13 -> "Apparently management now — occurrence $n."
        n >= 8 -> "Tenant status — occurrence $n."
        n >= 5 -> "Employee status — occurrence $n."
        n >= 3 -> "Running bit #$n."
        n >= 2 -> "Second sighting."
        else -> "Fresh context."
    }

    private fun mythBand(n: Int): String = when {
        n >= 34 -> "34"
        n >= 21 -> "21"
        n >= 13 -> "13"
        n >= 8 -> "8"
        n >= 5 -> "5"
        n >= 3 -> "3"
        n >= 2 -> "2"
        else -> "1"
    }
}
