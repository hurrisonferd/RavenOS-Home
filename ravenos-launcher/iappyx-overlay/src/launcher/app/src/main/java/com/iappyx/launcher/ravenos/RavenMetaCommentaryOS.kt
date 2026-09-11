package com.iappyx.launcher.ravenos

import android.content.Context

/** Deterministic whole-phone meta commentary compiled from bounded local evidence. */
object RavenMetaCommentaryOS {
    data class Commentary(val text: String, val family: String, val noveltyKey: String)

    private enum class SurfaceKind { APP, SYSTEM_UI, KEYBOARD, LAUNCHER_SURFACE }

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
        val pkg = field(marker.detail, "package")
        val surface = surfaceKind(pkg, app)
        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)
        val elapsed = previous?.let { (marker.at - it.at).coerceAtLeast(0L) } ?: Long.MAX_VALUE
        val fast = elapsed < 12_000L
        val n = complex.occurrence

        val baseText = when (marker.key) {
            "APP_ENTER" -> when {
                surface == SurfaceKind.SYSTEM_UI -> systemUiCommentary(previousApp, elapsed, fast)
                surface == SurfaceKind.KEYBOARD -> keyboardCommentary(app ?: "Keyboard", previousApp, elapsed, fast)
                surface == SurfaceKind.LAUNCHER_SURFACE -> launcherSurfaceCommentary(app ?: "Launcher", previousApp, elapsed, fast, scene.mediaHot)
                previous?.key == "APP_ENTER" && previousApp != null && app != null && previousApp != app && fast ->
                    "$previousApp → $app in ${elapsed / 1000}s. Real foreground handoff observed."
                previous?.key == "HOME_ENTER" && app != null && fast ->
                    "RavenOS Home → $app in ${elapsed / 1000}s. Deliberate app re-entry, not idle drift."
                "APP_SWITCH_BURST" in complex.tags && app != null ->
                    "$app joined a foreground-switch burst: ${complex.recentSwitches} transitions in fifteen seconds. Tiny task tornado confirmed by window focus."
                "RETURN_LOOP" in complex.tags && app != null ->
                    "$app again. Return loop #$n; this foreground destination is part of the current ritual now."
                scene.mediaHot && app != null ->
                    "$app is foreground while media remains active. The soundtrack survived the context switch."
                app != null -> "$app is foreground. ${mythLine(n)}"
                else -> "Foreground changed. ${mythLine(n)}"
            }
            "HOME_ENTER" -> when {
                previousApp != null && fast -> "RavenOS Home after $previousApp. Launcher reset point reached in ${elapsed / 1000}s."
                scene.mediaHot -> "RavenOS Home with media still playing. This is a pit stop, not a shutdown."
                n >= 5 -> "RavenOS Home checkpoint #$n. This is infrastructure now, not scenery."
                else -> "RavenOS Home checkpoint #$n. Staging ground ready."
            }
            "MEDIA_ACTIVE" -> when {
                scene.activeApp != null && scene.activeApp != "RavenOS Home" ->
                    "Media became active under ${scene.activeApp}. The phone acquired a soundtrack lane without changing foreground ownership."
                previousApp != null -> "Media is active around $previousApp. Soundtrack lane has the floor."
                else -> "Media field active. The launcher should move with it, not repeat a status card."
            }
            "MEDIA_IDLE" -> when {
                scene.activeApp != null -> "Media stopped while ${scene.activeApp} still owns focus. Soundtrack lane released."
                else -> "Media stopped. Drop the performance layer; keep only useful state."
            }
            "MEDIA_SESSION" -> mediaSessionCommentary(marker.detail, scene.activeApp)
            "SCREEN_VISUAL" -> visualCommentary(marker.detail, scene.activeApp)
            "AUDIO" -> audioCommentary(marker.detail)
            "NOTIFICATION_POSTED" -> notificationCommentary(marker.detail, previousApp)
            "NOTIFICATION_REMOVED" -> notificationRemovedCommentary(marker.detail, scene.activeApp)
            "SYSTEM_DECK_OPENED" -> when {
                scene.mediaHot -> "System Deck opened while media remains active. Raven is tuning the machine without dropping the soundtrack."
                previousApp != null -> "System Deck after $previousApp. Configuration became part of the current task."
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
                else -> "Battery state changed."
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

        val sceneTail = sceneTail(scene, marker.key, app)
        val text = if (sceneTail.isBlank()) baseText else "$baseText $sceneTail"

        val family = when {
            surface == SurfaceKind.SYSTEM_UI && marker.key == "APP_ENTER" -> "META_SYSTEM_UI"
            surface == SurfaceKind.KEYBOARD && marker.key == "APP_ENTER" -> "META_INPUT_SURFACE"
            surface == SurfaceKind.LAUNCHER_SURFACE && marker.key == "APP_ENTER" -> "META_LAUNCHER_SURFACE"
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
        val noveltyKey = listOf(member.id, family, app ?: "", previousApp ?: "", mythBand(n), semanticDetail(marker), sceneSignature(scene)).joinToString("|")
        return Commentary(text.take(430), family, noveltyKey)
    }

    private fun systemUiCommentary(previousApp: String?, elapsed: Long, fast: Boolean): String = when {
        previousApp != null && fast -> "System UI surfaced over $previousApp in ${elapsed / 1000}s. System surface transition observed; this is not being counted as a new user task in the commentary."
        previousApp != null -> "System UI owns the top surface; $previousApp was the previous foreground app. System surface, not task identity."
        else -> "System UI owns the top surface. Android chrome is foreground; no underlying app semantics inferred."
    }

    private fun keyboardCommentary(keyboard: String, previousApp: String?, elapsed: Long, fast: Boolean): String = when {
        previousApp != null && fast -> "$keyboard surfaced over $previousApp in ${elapsed / 1000}s. Input layer observed; the underlying task is treated as continuous."
        previousApp != null -> "$keyboard is the top input surface over the recent $previousApp context. Keyboard focus is not a task switch."
        else -> "$keyboard surfaced. Input layer observed; underlying app context is unknown."
    }

    private fun launcherSurfaceCommentary(launcher: String, previousApp: String?, elapsed: Long, fast: Boolean, mediaHot: Boolean): String = buildString {
        append(launcher).append(" surfaced")
        if (previousApp != null && previousApp != launcher) {
            append(" after ").append(previousApp)
            if (fast) append(" in ").append(elapsed / 1000).append('s')
        }
        append(". Launcher boundary observed, not treated as content semantics.")
        if (mediaHot) append(" Media is still active across the boundary.")
    }

    private fun notificationCommentary(detail: String, previousApp: String?): String {
        val source = field(detail, "app") ?: field(detail, "package")?.substringAfterLast('.') ?: "An app"
        val burst = field(detail, "burst")?.toIntOrNull() ?: 1
        val conversation = field(detail, "conversation") == "true"
        val alerting = field(detail, "alerting") == "true"
        val matches = field(detail, "matches_filter") != "false"
        val ongoing = field(detail, "ongoing") == "true"
        val category = field(detail, "category")
        val importance = field(detail, "importance")
        val title = field(detail, "title")
        val privacy = field(detail, "privacy") ?: "SOURCE"
        val rankFacts = listOfNotNull(
            category?.let { "category $it" },
            importance?.let { "importance $it" },
            if (ongoing) "ongoing" else null,
        ).joinToString(" · ")
        return when {
            burst >= 3 -> "$source posted $burst notifications inside one burst${rankFacts.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}. Notification weather system confirmed from ranking metadata."
            conversation && alerting -> "$source posted an alerting conversation notification${rankFacts.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}. Communication lane earned attention."
            !matches -> "$source posted, but the current interruption filter does not match it. Phone noticed; Raven did not need the interruption."
            !title.isNullOrBlank() && privacy != "SOURCE" -> "$source posted “${title.take(70)}”${rankFacts.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}. Local semantic awareness is active; no cloud/content action implied."
            previousApp != null -> "$source pinged while $previousApp was the recent app context${rankFacts.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}. Content stays private in $privacy mode."
            else -> "$source pinged${rankFacts.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}. Privacy mode $privacy; no notification content inferred."
        }
    }

    private fun notificationRemovedCommentary(detail: String, activeApp: String?): String {
        val source = field(detail, "app") ?: field(detail, "package")?.substringAfterLast('.') ?: "A notification"
        val reason = field(detail, "reason")
        return buildString {
            append(source).append(" left the notification lane. Removal observed")
            if (!reason.isNullOrBlank()) append(" · reason ").append(reason.take(40))
            append("; content, dismissal intent, and action outcome are not inferred.")
            if (!activeApp.isNullOrBlank()) append(" Current scene focus: ").append(activeApp).append('.')
        }
    }

    private fun mediaSessionCommentary(detail: String, activeApp: String?): String {
        val source = field(detail, "app") ?: field(detail, "package")?.substringAfterLast('.') ?: "Media"
        val state = field(detail, "state") ?: "UNKNOWN"
        val title = field(detail, "title")
        val artist = field(detail, "artist")
        val position = field(detail, "position")
        val duration = field(detail, "duration")
        return when (state) {
            "PLAYING" -> buildString {
                append(source).append(" is playing")
                if (!title.isNullOrBlank()) append(" “").append(title.take(70)).append('”')
                if (!artist.isNullOrBlank()) append(" by ").append(artist.take(50))
                if (!position.isNullOrBlank() || !duration.isNullOrBlank()) append(" · ").append(position ?: "?").append('/').append(duration ?: "?")
                append('.')
                if (!activeApp.isNullOrBlank() && !activeApp.equals(source, true)) append(" Playback continues while $activeApp owns scene focus.")
            }
            "PAUSED" -> "$source paused${title?.let { " “${it.take(60)}”" } ?: ""}. The soundtrack lane is holding position."
            "SKIP_NEXT", "SKIP_PREVIOUS" -> "$source changed tracks. Music context moved without claiming a foreground-task change."
            "BUFFERING", "CONNECTING" -> "$source is ${state.lowercase()}. Soundtrack lane is waiting on transport."
            else -> "$source media session changed to $state."
        }
    }

    private fun visualCommentary(detail: String, activeApp: String?): String {
        val state = field(detail, "state") ?: "unknown"
        if (state == "armed") return "Goblin Eye armed. Raven explicitly opened a local visual session; raw frames are not being persisted."
        if (state.startsWith("stopped")) return "Goblin Eye closed. Pixel awareness ended with the capture session."
        val motion = field(detail, "motion")?.toIntOrNull()
        val delta = field(detail, "delta")?.toIntOrNull()
        val hash = field(detail, "hash")?.toIntOrNull() ?: field(detail, "hash_distance")?.toIntOrNull()
        val luma = field(detail, "luma")?.toIntOrNull()
        return when {
            motion != null && motion >= 60 -> "Large screen transition${activeApp?.let { " while $it owns scene focus" } ?: ""}: $motion% sampled-region motion${delta?.let { ", delta $it" } ?: ""}${hash?.let { ", hash distance $it" } ?: ""}. Hard visual context change."
            motion != null && motion >= 30 -> "Screen changed materially${activeApp?.let { " under $it" } ?: ""}: motion $motion%${delta?.let { ", delta $it" } ?: ""}. Goblin Eye confirms pixels moved without claiming what they mean."
            delta != null && delta >= 20 -> "Visual state shifted without a major foreground handoff. Average sampled delta $delta${luma?.let { ", luminance $it" } ?: ""}."
            luma != null -> "Screen appearance changed; sampled luminance $luma. No OCR or semantic pixel claim attached."
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

    /** Adds at most two independently-derived scene facts so commentary feels aware, not templated. */
    private fun sceneTail(scene: RavenPhoneSceneOS.Scene, markerKey: String, currentApp: String?): String {
        val facts = ArrayList<String>(2)
        if (!markerKey.startsWith("MEDIA") && scene.mediaHot) {
            val track = scene.mediaTitle?.takeIf { it.isNotBlank() }
            facts += if (track != null) "Track “${track.take(46)}” remains active." else "Media remains active."
        }
        if (facts.size < 2 && markerKey != "SCREEN_VISUAL" && scene.goblinEyeActive && scene.lastVisualMotion != null) {
            facts += "Eye ON · last sampled motion ${scene.lastVisualMotion}%."
        }
        if (facts.size < 2 && markerKey != "APP_ENTER" && scene.recentSwitches >= 4) {
            facts += "${scene.recentSwitches} foreground transitions/30s."
        }
        if (facts.size < 2 && !markerKey.startsWith("NOTIFICATION") && scene.notificationBurst >= 2) {
            val source = scene.notificationSource ?: "notification source"
            facts += "$source burst ×${scene.notificationBurst}."
        }
        if (facts.size < 2 && markerKey != "APP_ENTER" && !scene.activeApp.isNullOrBlank() && scene.activeApp != currentApp && scene.activeApp != "RavenOS Home") {
            facts += "Scene focus ${scene.activeApp}."
        }
        return facts.take(2).joinToString(" ")
    }

    private fun sceneSignature(scene: RavenPhoneSceneOS.Scene): String = listOf(
        scene.activeApp ?: "",
        if (scene.mediaHot) "media:hot" else "media:quiet",
        scene.mediaTitle ?: "",
        "sw:${scene.recentSwitches}",
        "eye:${scene.goblinEyeActive}",
        "motion:${scene.lastVisualMotion ?: -1}",
        "burst:${scene.notificationBurst}",
    ).joinToString(":")

    private fun surfaceKind(pkg: String?, app: String?): SurfaceKind {
        val p = pkg.orEmpty().lowercase()
        val a = app.orEmpty().lowercase()
        return when {
            p == "com.android.systemui" || a == "system ui" -> SurfaceKind.SYSTEM_UI
            p.contains("honeyboard") || p.contains("keyboard") || a.contains("keyboard") -> SurfaceKind.KEYBOARD
            p == "com.sec.android.app.launcher" || a == "one ui home" -> SurfaceKind.LAUNCHER_SURFACE
            else -> SurfaceKind.APP
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
