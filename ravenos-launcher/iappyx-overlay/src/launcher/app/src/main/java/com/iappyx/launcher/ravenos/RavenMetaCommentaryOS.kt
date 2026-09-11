package com.iappyx.launcher.ravenos

import android.content.Context

/** Deterministic short-form phone commentary. Raw telemetry stays in evidence stores. */
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
        val previousApp = prior.asReversed().firstOrNull { it.key == "APP_ENTER" }?.let { markerAppLabel(context, it) }
        val app = markerAppLabel(context, marker)
        val pkg = field(marker.detail, "package")
        val surface = surfaceKind(pkg, app)
        val scene = RavenPhoneSceneOS.snapshot(context, marker.at)
        val elapsed = previous?.let { (marker.at - it.at).coerceAtLeast(0L) } ?: Long.MAX_VALUE
        val fast = elapsed < 12_000L
        val seed = "${member.id}|${marker.key}|${marker.detail}|${complex.occurrence}|${episode.name}"

        val text = when (marker.key) {
            "APP_ENTER" -> appComment(seed, surface, app, previousApp, elapsed, fast, scene.mediaHot, complex.recentSwitches)
            "WINDOW_CHANGE" -> windowCommentary(context, seed, marker.detail, previousApp)
            "HOME_ENTER" -> homeComment(seed, previousApp, elapsed, fast, scene.mediaHot)
            "MEDIA_ACTIVE" -> pick(seed, listOf(
                scene.activeApp?.let { "Music started while you're in $it." } ?: "Music started.",
                scene.activeApp?.let { "Soundtrack's on under $it." } ?: "Soundtrack's on.",
                "Music just woke up.",
            ))
            "MEDIA_IDLE" -> pick(seed, listOf(
                scene.activeApp?.let { "Music stopped while you're still in $it." } ?: "Music stopped.",
                "Soundtrack went quiet.",
                "The music lane went dark.",
            ))
            "MEDIA_SESSION" -> mediaSessionCommentary(context, seed, marker.detail, scene.activeApp)
            "SCREEN_VISUAL" -> visualCommentary(seed, marker.detail, scene.activeApp)
            "AUDIO" -> audioCommentary(seed, marker.detail)
            "NOTIFICATION_POSTED" -> notificationCommentary(context, seed, marker.detail)
            "NOTIFICATION_REMOVED" -> notificationRemovedCommentary(context, seed, marker.detail)
            "SYSTEM_DECK_OPENED" -> pick(seed, listOf("System Deck is open.", "Quick settings pit stop.", "You're tuning RavenOS for a second."))
            "SEARCH_OPENED" -> pick(seed, listOf("Search is open.", "Looking for something.", "Search mode."))
            "APP_UNIVERSE_OPENED" -> pick(seed, listOf("App drawer's open.", "Picking the next app.", "App universe time."))
            "ROOM_CHANGED" -> pick(seed, listOf("Home room changed.", "Different launcher room.", "Home shifted scenes."))
            "POWER_CHANGED" -> powerComment(seed, marker.detail)
            "BATTERY_CHANGED" -> batteryComment(seed, marker.detail)
            "NIGHT" -> if (marker.detail.contains("screen:off", true)) "Screen went dark." else "Night state changed."
            "DEVICE" -> when {
                marker.detail.contains("screen:on", true) -> pick(seed, listOf("Screen woke up.", "Phone's awake.", "Display is back on."))
                marker.detail.contains("screen:off", true) -> pick(seed, listOf("Screen went to sleep.", "Display is off.", "Phone screen went dark."))
                else -> pick(seed, listOf("Device state shifted.", "The phone changed state.", "Something on the device moved."))
            }
            else -> fallback(seed, marker)
        }

        val family = when {
            marker.key == "WINDOW_CHANGE" -> "META_WINDOW"
            surface == SurfaceKind.SYSTEM_UI && marker.key == "APP_ENTER" -> "META_SYSTEM_UI"
            surface == SurfaceKind.KEYBOARD && marker.key == "APP_ENTER" -> "META_INPUT"
            surface == SurfaceKind.LAUNCHER_SURFACE && marker.key == "APP_ENTER" -> "META_LAUNCHER"
            marker.key.startsWith("MEDIA") -> "META_MEDIA"
            marker.key == "SCREEN_VISUAL" -> "META_VISION"
            marker.key.startsWith("NOTIFICATION") -> "META_NOTIFICATION"
            marker.key == "APP_ENTER" -> "META_APP"
            marker.key == "HOME_ENTER" -> "META_HOME"
            else -> "META_${marker.key}"
        }
        val short = shorten(text, 128)
        return Commentary(short, family, listOf(member.id, family, short, complex.occurrence.toString()).joinToString("|"))
    }

    private fun appComment(
        seed: String,
        surface: SurfaceKind,
        app: String?,
        previousApp: String?,
        elapsed: Long,
        fast: Boolean,
        mediaHot: Boolean,
        recentSwitches: Int,
    ): String = when (surface) {
        SurfaceKind.SYSTEM_UI -> when {
            previousApp != null && previousApp != "System UI" -> pick(seed, listOf(
                "Quick System UI detour; $previousApp was underneath.",
                "System UI popped up over $previousApp.",
                "You pulled up System UI from $previousApp.",
            ))
            else -> pick(seed, listOf("System UI is up.", "Android's system panel is on top.", "System UI popped up."))
        }
        SurfaceKind.KEYBOARD -> when {
            previousApp != null -> pick(seed, listOf(
                "Keyboard's up in $previousApp.",
                "Typing mode in $previousApp.",
                "Keyboard popped up; $previousApp is still the task.",
            ))
            else -> pick(seed, listOf("Keyboard's up.", "Typing mode.", "Input surface opened."))
        }
        SurfaceKind.LAUNCHER_SURFACE -> buildString {
            append(pick(seed, listOf("Back on Home.", "Launcher popped up.", "Home screen pit stop.")))
            if (mediaHot) append(" Music kept playing.")
        }
        SurfaceKind.APP -> when {
            previousApp != null && app != null && previousApp != app && fast -> pick(seed, listOf(
                "$previousApp → $app in ${elapsed / 1000}s.",
                "Quick jump from $previousApp to $app.",
                "You switched from $previousApp to $app.",
            ))
            recentSwitches >= 4 && app != null -> pick(seed, listOf(
                "You've bounced through $recentSwitches apps pretty fast; $app is up now.",
                "$app is the latest stop in a fast app-hopping streak.",
                "Busy minute: $recentSwitches app changes, now $app.",
            ))
            app != null && mediaHot -> pick(seed, listOf(
                "$app is open and the music kept going.",
                "Opened $app without losing the soundtrack.",
                "$app is up; music is still rolling.",
            ))
            app != null -> pick(seed, listOf("$app is open.", "You're in $app now.", "$app is on screen."))
            else -> pick(seed, listOf("Foreground app changed.", "Another app took the screen.", "Foreground changed hands."))
        }
    }

    private fun windowCommentary(context: Context, seed: String, detail: String, previousApp: String?): String {
        val pkg = field(detail, "package")
        val app = pkg?.let { packageLabel(context, it) } ?: previousApp ?: "This app"
        val clazz = field(detail, "class").orEmpty()
        return when {
            pkg == "com.android.systemui" -> pick(seed, listOf("System UI changed panels.", "Android chrome shifted again.", "System UI swapped surfaces."))
            pkg?.contains("honeyboard", true) == true || clazz.contains("InputMethod", true) -> pick(seed, listOf(
                "Keyboard surface changed; $previousApp is still the task.",
                "Typing surface shifted without changing the app underneath.",
                "Keyboard changed windows; task context stayed put.",
            ))
            clazz.contains("Dialog", true) -> pick(seed, listOf("$app opened a dialog.", "$app put a dialog on top.", "A $app dialog took the front layer."))
            else -> pick(seed, listOf("$app changed screens.", "$app swapped windows.", "New surface inside $app."))
        }
    }

    private fun homeComment(seed: String, previousApp: String?, elapsed: Long, fast: Boolean, mediaHot: Boolean): String = when {
        mediaHot -> pick(seed, listOf("Back on Home; music's still playing.", "Home screen pit stop with the soundtrack still on.", "Home again. Music survived."))
        previousApp != null && fast -> pick(seed, listOf("Back on Home from $previousApp.", "Home screen after $previousApp.", "$previousApp → Home in ${elapsed / 1000}s."))
        else -> pick(seed, listOf("Back on Home.", "Home screen.", "Launcher is up."))
    }

    private fun mediaSessionCommentary(context: Context, seed: String, detail: String, activeApp: String?): String {
        val source = sourceLabel(context, detail)
        val state = field(detail, "state") ?: "UNKNOWN"
        val title = field(detail, "title")?.take(54)
        val artist = field(detail, "artist")?.take(38)
        val track = when {
            !title.isNullOrBlank() && !artist.isNullOrBlank() -> "“$title” by $artist"
            !title.isNullOrBlank() -> "“$title”"
            else -> source
        }
        return when (state) {
            "PLAYING" -> {
                val base = pick(seed, listOf("Now playing $track.", "$track is playing.", "Still on $track."))
                if (!activeApp.isNullOrBlank() && activeApp != source && activeApp != "System UI") "$base You're in $activeApp." else base
            }
            "PAUSED" -> pick(seed, listOf("Paused $track.", "$track is paused.", "Playback paused on $track."))
            "BUFFERING", "CONNECTING" -> pick(seed, listOf("$track is buffering.", "Waiting on $track to load.", "$track hit a buffer."))
            "SKIP_NEXT", "SKIP_PREVIOUS" -> pick(seed, listOf("Track changed.", "Music skipped to another track.", "New track."))
            "STOPPED", "NONE" -> pick(seed, listOf("Playback stopped.", "Music stopped.", "Media session went quiet."))
            else -> "$source playback changed."
        }
    }

    private fun notificationCommentary(context: Context, seed: String, detail: String): String {
        val source = sourceLabel(context, detail)
        val burst = field(detail, "burst")?.toIntOrNull() ?: 1
        val conversation = field(detail, "conversation") == "true"
        val alerting = field(detail, "alerting") == "true"
        val title = field(detail, "title")?.take(58)
        val privacy = field(detail, "privacy") ?: "SOURCE"
        return when {
            burst >= 3 -> pick(seed, listOf("$source got noisy: $burst notifications together.", "$source just dropped $burst notifications at once.", "$burst pings from $source in one burst."))
            conversation && alerting -> pick(seed, listOf("$source wants your attention.", "$source has an active conversation ping.", "Message alert from $source."))
            !title.isNullOrBlank() && privacy != "SOURCE" -> pick(seed, listOf("$source: “$title”.", "$source posted “$title”.", "New $source notification: “$title”."))
            else -> pick(seed, listOf("$source pinged.", "New notification from $source.", "$source dropped a notification."))
        }
    }

    private fun notificationRemovedCommentary(context: Context, seed: String, detail: String): String {
        val source = sourceLabel(context, detail)
        return pick(seed, listOf("$source notification cleared.", "$source left the notification shade.", "One $source notification disappeared."))
    }

    private fun visualCommentary(seed: String, detail: String, activeApp: String?): String {
        val state = field(detail, "state") ?: "unknown"
        if (state == "armed") return pick(seed, listOf("Goblin Eye is on.", "Goblin Eye armed.", "Screen watch is live."))
        if (state.startsWith("stopped")) return pick(seed, listOf("Goblin Eye is off.", "Screen watch stopped.", "Goblin Eye closed."))
        val motion = field(detail, "motion")?.toIntOrNull()
        val delta = field(detail, "delta")?.toIntOrNull()
        return when {
            motion != null && motion >= 60 -> pick(seed, listOf(
                "Big screen change${activeApp?.let { " in $it" } ?: ""}: $motion% sampled motion.",
                "The screen jumped hard${activeApp?.let { " in $it" } ?: ""}; $motion% of samples moved.",
                "Goblin Eye caught a big visual shift: $motion% motion.",
            ))
            motion != null && motion >= 30 -> pick(seed, listOf(
                "Screen shifted${activeApp?.let { " in $it" } ?: ""}; $motion% sampled motion.",
                "Goblin Eye caught a visible change: $motion% motion.",
                "That screen actually moved: $motion% sampled change.",
            ))
            delta != null && delta >= 20 -> pick(seed, listOf("Screen appearance changed noticeably.", "Visual state shifted.", "Goblin Eye caught a smaller scene change."))
            else -> pick(seed, listOf("Goblin Eye caught a screen change.", "The screen changed.", "Visual change noticed."))
        }
    }

    private fun audioCommentary(seed: String, detail: String): String {
        fun value(name: String): Int? = field(detail, name)?.toIntOrNull()
        val media = value("media")
        val ring = value("ring")
        return when (field(detail, "ringer")) {
            "vibrate" -> pick(seed, listOf("Phone's on vibrate.", "Vibrate mode is on.", "Ringer switched to vibrate."))
            "silent" -> pick(seed, listOf("Phone went silent.", "Silent mode is on.", "Ringer is muted."))
            else -> if (media != null && ring != null) pick(seed, listOf("Media $media%, ring $ring%.", "Volume changed: media $media%, ring $ring%.", "Audio levels moved — media $media%, ring $ring%.")) else "Audio settings changed."
        }
    }

    private fun powerComment(seed: String, detail: String): String = when {
        detail.contains("charging", true) || detail.contains("connect", true) -> pick(seed, listOf(
            "Charging${percentIn(detail)?.let { " at $it%" } ?: ""}.",
            "Power connected${percentIn(detail)?.let { " — $it%" } ?: ""}.",
            "Plugged in${percentIn(detail)?.let { " at $it%" } ?: ""}.",
        ))
        detail.contains("unplug", true) -> pick(seed, listOf("Unplugged.", "Back on battery.", "Power cable disconnected."))
        else -> "Power state changed."
    }

    private fun batteryComment(seed: String, detail: String): String = when {
        detail.contains("low", true) -> pick(seed, listOf("Battery's getting low.", "Low battery.", "Battery needs attention."))
        detail.contains("recovered", true) -> pick(seed, listOf("Battery recovered.", "Battery's back in a comfortable range.", "Power level looks better now."))
        else -> "Battery level changed."
    }

    private fun fallback(seed: String, marker: RavenMarkerBus.Marker): String = when {
        "RECOVERY" in marker.tags -> pick(seed, listOf("Back to normal.", "That recovered.", "The noisy bit settled down."))
        "ERROR" in marker.tags -> pick(seed, listOf("That fault is real.", "Something actually broke state.", "The phone hit a real anomaly."))
        else -> pick(seed, listOf("Phone state shifted.", "Something on the phone changed.", "The scene moved a little."))
    }

    private fun markerAppLabel(context: Context, marker: RavenMarkerBus.Marker): String? {
        field(marker.detail, "app")?.takeIf { it.isNotBlank() }?.let { return it.take(48) }
        val pkg = field(marker.detail, "package") ?: return null
        return packageLabel(context, pkg)
    }

    private fun sourceLabel(context: Context, detail: String): String {
        field(detail, "app")?.takeIf { it.isNotBlank() }?.let { return it.take(48) }
        val pkg = field(detail, "package") ?: return "An app"
        return packageLabel(context, pkg)
    }

    private fun packageLabel(context: Context, pkg: String): String {
        if (pkg == "com.android.systemui") return "System UI"
        if (pkg.contains("honeyboard", true)) return "Samsung Keyboard"
        return try {
            val info = context.packageManager.getApplicationInfo(pkg, 0)
            context.packageManager.getApplicationLabel(info).toString().trim().takeIf { it.isNotBlank() }?.take(48)
                ?: pkg.substringAfterLast('.')
        } catch (_: Throwable) {
            pkg.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun surfaceKind(pkg: String?, app: String?): SurfaceKind = when (pkg) {
        "com.android.systemui" -> SurfaceKind.SYSTEM_UI
        "com.samsung.android.honeyboard", "com.google.android.inputmethod.latin" -> SurfaceKind.KEYBOARD
        "com.sec.android.app.launcher", "com.ravenos.launcher" -> SurfaceKind.LAUNCHER_SURFACE
        else -> when {
            app?.contains("keyboard", true) == true -> SurfaceKind.KEYBOARD
            app?.contains("one ui home", true) == true || app?.contains("launcher", true) == true -> SurfaceKind.LAUNCHER_SURFACE
            else -> SurfaceKind.APP
        }
    }

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)").find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun percentIn(detail: String): String? = Regex("(\\d{1,3})%").find(detail)?.groupValues?.getOrNull(1)

    private fun shorten(text: String, max: Int): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= max) return clean
        val cut = clean.take(max - 1).substringBeforeLast(' ').trimEnd('.', ',', ';', ':')
        return "$cut…"
    }

    private fun pick(seed: String, options: List<String>): String {
        if (options.size <= 1) return options.firstOrNull().orEmpty()
        var hash = 0x811C9DC5.toInt()
        for (c in seed) {
            hash = hash xor c.code
            hash *= 16777619
        }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
