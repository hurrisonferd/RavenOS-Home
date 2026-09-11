package com.iappyx.launcher.ravenos

import android.content.Context

/** Deterministic whole-phone meta commentary compiled from bounded local evidence. */
object RavenMetaCommentaryOS {
    data class Commentary(val text: String, val family: String, val noveltyKey: String)

    fun compose(
        context: Context,
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        episode: RavenEpisodeOS.Phase,
    ): Commentary {
        val history = RavenMarkerBus.recent(context, 24)
        val prior = history.dropLast(1)
        val previous = prior.lastOrNull()
        val previousApp = prior.asReversed().firstOrNull { it.key == "APP_ENTER" }?.let(::appLabel)
        val app = appLabel(marker)
        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)
        val elapsed = previous?.let { (marker.at - it.at).coerceAtLeast(0L) } ?: Long.MAX_VALUE
        val fast = elapsed < 12_000L
        val n = complex.occurrence

        val text = when (marker.key) {
            "APP_ENTER" -> when {
                previous?.key == "APP_ENTER" && previousApp != null && app != null && previousApp != app && fast ->
                    "$previousApp → $app in ${elapsed / 1000}s. The phone changed jobs mid-sentence."
                previous?.key == "HOME_ENTER" && app != null && fast ->
                    "Back out of Home and straight into $app. Deliberate re-entry, not idle drift."
                "APP_SWITCH_BURST" in complex.tags && app != null ->
                    "$app joined an app-switch burst: ${complex.recentSwitches} transitions in fifteen seconds. Tiny task tornado confirmed."
                "RETURN_LOOP" in complex.tags && app != null ->
                    "$app again. Return loop #$n; this app is part of the current ritual now."
                scene.mediaHot && app != null ->
                    "$app is foreground with music still hot. The soundtrack survived the context switch."
                app != null -> "$app is foreground. ${mythLine(n)}"
                else -> "Foreground changed. ${mythLine(n)}"
            }
            "HOME_ENTER" -> when {
                previousApp != null && fast -> "Home after $previousApp. Reset point reached in ${elapsed / 1000}s."
                scene.mediaHot -> "Home with music still running. This is a pit stop, not a shutdown."
                n >= 5 -> "Home checkpoint #$n. This is infrastructure now, not scenery."
                else -> "Home checkpoint #$n. Staging ground ready."
            }
            "MEDIA_ACTIVE" -> when {
                scene.activeApp != null && scene.activeApp != "RavenOS Home" ->
                    "Music came alive under ${scene.activeApp}. The phone just acquired a soundtrack lane."
                previousApp != null -> "Music is active around $previousApp. Soundtrack lane has the floor."
                else -> "Music field active. The launcher should move with it, not repeat a status card."
            }
            "MEDIA_IDLE" -> when {
                scene.activeApp != null -> "Music stopped while ${scene.activeApp} still owns focus. Soundtrack lane released."
                else -> "Music stopped. Drop the performance layer; keep only useful state."
            }
            "MEDIA_SESSION" -> mediaSessionCommentary(marker.detail, scene.activeApp)
            "SCREEN_VISUAL" -> visualCommentary(marker.detail, scene.activeApp)
            "AUDIO" -> audioCommentary(marker.detail)
            "NOTIFICATION_POSTED" -> notificationCommentary(marker.detail, previousApp)
            "NOTIFICATION_REMOVED" -> {
                val source = app ?: field(marker.detail, "package")?.substringAfterLast('.') ?: "A notification"
                "$source left the notification lane. One open loop just closed."
            }
            "SYSTEM_DECK_OPENED" -> when {
                scene.mediaHot -> "System Deck opened with music hot. Raven is tuning the machine without leaving the vibe."
                previousApp != null -> "System Deck after $previousApp. Configuration just became part of the task, not a side quest."
                else -> "System Deck opened. Active intervention, not passive browsing."
            }
            "SEARCH_OPENED" -> when {
                previousApp != null -> "Search after $previousApp. The phone switched from doing to finding."
                else -> "Search lane opened. Discovery mode."
            }
            "APP_UNIVERSE_OPENED" -> "App universe opened. Raven is choosing the next tool rather than wandering the drawer."
            "ROOM_CHANGED" -> "Launcher room changed. Surface context moved without pretending the underlying task changed."
            "POWER_CHANGED" -> when {
                marker.detail.contains("charging", true) || marker.detail.contains("connect", true) ->
                    "Power connected at ${percentIn(marker.detail) ?: "?"}%. The machine just gained endurance."
                marker.detail.contains("unplug", true) -> "Power unplugged at ${percentIn(marker.detail) ?: "?"}%. Back to battery law."
                else -> "Power state changed."
            }
            "BATTERY_CHANGED" -> when {
                marker.detail.contains("low", true) -> "Battery crossed the low-power floor. Useful awareness outranks decorative haunting now."
                marker.detail.contains("recovered", true) -> "Battery climbed back above the low-power floor. Goblins may resume normal nonsense."
                else -> "Battery state changed; no drama required."
            }
            "NIGHT" -> if (marker.detail.contains("screen:off", true))
                "Screen went dark. NYX law: stop performing when nobody is looking."
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
            marker.key == "MEDIA_SESSION" -> "META_MEDIA_SESSION"
            marker.key.startsWith("MEDIA_") -> "META_MEDIA"
            marker.key == "SCREEN_VISUAL" -> "META_VISION"
            marker.key.startsWith("NOTIFICATION") -> "META_NOTIFICATION"
            marker.key == "AUDIO" -> "META_AUDIO"
            marker.key == "HOME_ENTER" -> "META_HOME"
            else -> "META_${marker.key}"
        }
        val noveltyKey = listOf(member.id, family, app ?: "", previousApp ?: "", mythBand(n), semanticDetail(marker)).joinToString("|")
        return Commentary(text, family, noveltyKey)
    }

    private fun notificationCommentary(detail: String, previousApp: String?): String {
        val source = field(detail, "app") ?: field(detail, "package")?.substringAfterLast('.') ?: "An app"
        val burst = field(detail, "burst")?.toIntOrNull() ?: 1
        val conversation = field(detail, "conversation") == "true"
        val alerting = field(detail, "alerting") == "true"
        val matches = field(detail, "matches_filter") != "false"
        val title = field(detail, "title")
        val privacy = field(detail, "privacy") ?: "SOURCE"
        return when {
            burst >= 3 -> "$source posted $burst notifications inside one burst. This is a notification weather system now."
            conversation && alerting -> "$source posted an alerting conversation notification. Communication lane just earned the floor."
            !matches -> "$source posted, but the current interruption filter blocked it. Phone noticed; Raven did not need the interruption."
            !title.isNullOrBlank() && privacy != "SOURCE" -> "$source posted “${title.take(70)}”. Semantic notification awareness is active; body stays out of this commentary."
            previousApp != null -> "$source pinged during the $previousApp work loop. Ranking noted; notification body stays private in $privacy mode."
            else -> "$source pinged from the notification lane. Ranking noted; privacy mode is $privacy."
        }
    }

    private fun mediaSessionCommentary(detail: String, activeApp: String?): String {
        val source = field(detail, "package")?.substringAfterLast('.') ?: "Media"
        val state = field(detail, "state") ?: "UNKNOWN"
        val title = field(detail, "title")
        val artist = field(detail, "artist")
        return when (state) {
            "PLAYING" -> buildString {
                append(source).append(" is playing")
                if (!title.isNullOrBlank()) append(" “").append(title.take(70)).append('”')
                if (!artist.isNullOrBlank()) append(" by ").append(artist.take(50))
                append('.')
                if (!activeApp.isNullOrBlank() && !activeApp.equals(source, true)) append(" Soundtrack continues under $activeApp.")
            }
            "PAUSED" -> "$source paused${title?.let { " “${it.take(60)}”" } ?: ""}. The soundtrack lane is holding position."
            "SKIP_NEXT", "SKIP_PREVIOUS" -> "$source changed tracks. Music context moved without changing the foreground task."
            "BUFFERING", "CONNECTING" -> "$source is $state. Soundtrack lane is waiting on transport."
            else -> "$source media session changed to $state."
        }
    }

    private fun visualCommentary(detail: String, activeApp: String?): String {
        val state = field(detail, "state") ?: "unknown"
        if (state == "armed") return "Goblin Eye armed. Raven explicitly opened a local visual session; raw frames are not being persisted."
        if (state.startsWith("stopped")) return "Goblin Eye closed. Pixel awareness ended with the capture session."
        val motion = field(detail, "motion")?.toIntOrNull()
        val delta = field(detail, "delta")?.toIntOrNull()
        val luma = field(detail, "luma")?.toIntOrNull()
        return when {
            motion != null && motion >= 60 -> "Large screen transition${activeApp?.let { " inside $it" } ?: ""}: $motion% of sampled regions moved. Hard visual context change."
            motion != null && motion >= 30 -> "Screen changed materially${activeApp?.let { " under $it" } ?: ""}. Motion field $motion%; Goblin Eye confirms the scene actually moved."
            delta != null && delta >= 20 -> "Visual state shifted without a major app transition. Average sampled delta $delta."
            luma != null -> "Screen appearance changed; sampled luminance is $luma. No OCR or semantic pixel claim attached."
            else -> "Goblin Eye observed a visual change."
        }
    }

    private fun audioCommentary(detail: String): String {
        fun value(name: String): Int? = field(detail, name)?.toIntOrNull()
        val media = value("media")
        val ring = value("ring")
        val alarm = value("alarm")
        val ringer = field(detail, "ringer")
        return when {
            media != null && media >= 80 && ring == 0 -> "Media $media%, ring 0%. The phone is in studio mode whether it admits it or not."
            media == 0 && ring == 0 -> "Media and ring are both at zero. The phone just chose monastery mode."
            ringer == "vibrate" -> "Ringer switched to vibrate. Quiet outside, goblins still operational."
            ringer == "silent" -> "Ringer went silent. Commentary stays visual unless something material changes."
            else -> "Audio deck shifted: media ${media ?: "?"}% · ring ${ring ?: "?"}% · alarm ${alarm ?: "?"}%."
        }
    }

    private fun appLabel(marker: RavenMarkerBus.Marker): String? {
        val named = field(marker.detail, "app")
        if (!named.isNullOrBlank()) return named.take(48)
        return field(marker.detail, "package")?.substringAfterLast('.')
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            ?.takeIf { it.isNotBlank() }
    }

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)").find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun percentIn(detail: String): String? = Regex("(\\d{1,3})%").find(detail)?.groupValues?.getOrNull(1)

    private fun semanticDetail(marker: RavenMarkerBus.Marker): String = when (marker.key) {
        "SCREEN_VISUAL" -> listOf(field(marker.detail, "state"), field(marker.detail, "motion"), field(marker.detail, "delta")).joinToString(":")
        "MEDIA_SESSION" -> listOf(field(marker.detail, "package"), field(marker.detail, "state"), field(marker.detail, "title")).joinToString(":")
        "NOTIFICATION_POSTED" -> listOf(field(marker.detail, "package"), field(marker.detail, "burst"), field(marker.detail, "alerting"), field(marker.detail, "conversation")).joinToString(":")
        else -> marker.detail.substringBefore('|')
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
