package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Final presentation-only conversational pass.
 *
 * Runs after the brain/meta writers have already earned speech, settled cast, evidence and episode.
 * It may polish or replace dialogue presentation, but it cannot create speech from silence, change
 * cast/evidence, mutate chronology, read new device data or execute Android effects.
 *
 * Continuity comes from the already-visible bounded Office trace. No second episode clock exists.
 */
object RavenConversationDirectorOS {
    data class Decision(
        val dialogue: String,
        val form: String,
        val reason: String,
        val replaced: Boolean,
        val previousSpeaker: String,
    )

    private const val PREFS = "ravenos_conversation_director_v14"
    private const val RECENT_MAX = 14

    fun polish(
        context: Context,
        member: RavenOfficeMember,
        packet: RavenReactionPacket,
        screen: RavenScreenContextOS.Snapshot,
        graph: RavenSceneGraphOS.Graph,
        phone: RavenPhoneSceneOS.Scene,
    ): Decision {
        val current = clean(packet.dialogue, 720)
        if (current.isBlank()) return Decision("", "NONE", "NO_SPEECH_ALREADY_SETTLED", false, "")
        if (packet.complexTags.any { it == "PRESENTATION_DIAGNOSTIC" }) {
            return Decision(current, "DIAGNOSTIC", "DIAGNOSTIC_UNTOUCHED", false, "")
        }

        val mode = when {
            packet.complexTags.any { it == "PRESENTATION_SCREEN" } -> "SCREEN"
            packet.complexTags.any { it == "PRESENTATION_PHONE" } -> "PHONE"
            else -> "OTHER"
        }
        if (mode == "OTHER") return Decision(current, "KEEP", "NON_DIALOGUE_SURFACE", false, "")

        val app = graph.app.ifBlank {
            screen.appLabel.orEmpty().ifBlank { phone.activeApp.orEmpty().ifBlank { "the phone" } }
        }
        val subject = graph.subject.ifBlank { screen.focus.ifBlank { graph.title } }
            .replace(Regex("\\s+"), " ").trim().take(110)
        val track = phone.mediaTitle.orEmpty().replace(Regex("\\s+"), " ").trim().take(72)
        val ping = phone.notificationSource.orEmpty().replace(Regex("\\s+"), " ").trim().take(56)
        val appLower = app.lowercase()
        val isChatGpt = appLower.contains("chatgpt") || appLower.contains("openai")
        val isBrowser = listOf("chrome", "firefox", "browser", "edge", "brave", "opera").any(appLower::contains)
        val isMusic = appLower.contains("suno") || phone.mediaHot || track.isNotBlank()
        val isRaven = listOf(subject, graph.title, app).any {
            it.contains("ravenos", true) || it.contains("goblin", true) || it.contains("office feed", true)
        }
        val meta = screen.meta || isRaven || packet.complexTags.any { it.contains("META") }
        val returned = graph.returnCount > 0 || packet.complexTags.any { it.contains("CALLBACK") }
        val ownerAction = graph.interaction.isNotBlank()

        val trace = RavenOfficeTraceStore.recent(context.applicationContext, 8)
        val previous = trace.firstOrNull { it.owner.isNotBlank() && it.owner != member.id }
        val previousSpeaker = previous?.owner.orEmpty()

        val forms = buildList {
            if (previous != null) add("COWORKER_REPLY")
            if (isChatGpt && mode == "SCREEN") add("AI_OFFICE")
            if (isBrowser && mode == "SCREEN") add("WEB_RABBIT_HOLE")
            if (isMusic) add("MUSIC_ROOM")
            if (meta) add("SELF_DRAG")
            if (returned) add("CALLBACK_CHAT")
            if (ownerAction) add("RAVEN_BANTER")
            if (mode == "SCREEN" && ping.isNotBlank()) add("NOTIFICATION_CAMEO")
            if (shouldAskRaven(member.id, packet.occurrence, graph.signature)) add("ASK_RAVEN")
            add("SIDE_COMMENT")
            add("UNDERSTATEMENT")
            add("CUBICLE_BANTER")
        }.distinct()

        val form = RavenMetaTrickHistoryOS.choose(
            context,
            "CONVERSATION_${member.id}_${mode}_${graph.sceneType.ifBlank { screen.semanticKind }}",
            stableHash("${member.id}|${packet.occurrence}|${graph.signature}|$app|$subject|$previousSpeaker"),
            forms,
        ).ifBlank { "SIDE_COMMENT" }

        val authored = pick(
            "${member.id}|$form|${packet.occurrence}|$app|$subject|$track|$previousSpeaker",
            lines(form, app, subject, track, ping, previous, graph, phone),
        )
        val aside = voiceAside(member.id, form, app, subject, previousSpeaker, packet.occurrence)
        val useAside = authored.isNotBlank() && aside.isNotBlank() &&
            Math.floorMod(stableHash("aside|${member.id}|$form|${packet.occurrence}|$subject"), 100) < 58
        val fresh = listOf(authored, if (useAside) aside else "")
            .filter(String::isNotBlank)
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(430)

        val currentScore = conversationalScore(current, app, subject, track, previousSpeaker, current = true)
        val freshScore = conversationalScore(fresh, app, subject, track, previousSpeaker, current = false)
        val stiff = isSystemVoice(current)
        val rawWeb = current.contains("WEB CONTEXT ·", true)
        val repetitive = repeated(context, current)
        val notificationHijack = mode == "SCREEN" && notificationHeavy(current, ping) &&
            !mentions(current, subject) && !screenLooksNotification(screen, graph)
        val contextBlind = mode == "SCREEN" && subject.isNotBlank() && !mentions(current, subject) && !mentions(current, app)
        val specialScene = isChatGpt || isBrowser || isMusic || meta || previous != null

        val replace = fresh.isNotBlank() && (
            stiff || rawWeb || repetitive || notificationHijack || contextBlind ||
                freshScore >= currentScore + 8 || (specialScene && freshScore >= currentScore + 3)
            )
        val selected = if (replace) fresh else current
        remember(context, selected, if (replace) form else "KEEP")

        val reason = when {
            replace && rawWeb -> "NATURALIZE_RAW_WEB_CONTEXT"
            replace && stiff -> "NATURALIZE_SYSTEM_VOICE"
            replace && repetitive -> "NATURALIZE_REPEATED_FORM"
            replace && notificationHijack -> "RETURN_NOTIFICATION_TO_CAMEO"
            replace && contextBlind -> "RESTORE_VISIBLE_SCENE"
            replace && form == "COWORKER_REPLY" -> "CONTINUE_OFFICE_CONVERSATION"
            replace && form == "AI_OFFICE" -> "CHATGPT_META_SCENE"
            replace && form == "WEB_RABBIT_HOLE" -> "BROWSER_SCENE"
            replace -> "CONVERSATIONAL_SCENE_WINS"
            else -> "KEEP_ALREADY_NATURAL"
        }
        return Decision(selected, if (replace) form else "KEEP", reason, replace, previousSpeaker)
    }

    private fun lines(
        form: String,
        app: String,
        subject: String,
        track: String,
        ping: String,
        previous: RavenOfficeTraceStore.Entry?,
        graph: RavenSceneGraphOS.Graph,
        phone: RavenPhoneSceneOS.Scene,
    ): List<String> {
        val thing = if (subject.isBlank()) graph.task.lowercase().replace('_', ' ').ifBlank { "this" } else "“$subject”"
        val prior = previous?.owner.orEmpty()
        return when (form) {
            "COWORKER_REPLY" -> listOf(
                "$prior already had a whole thing about $thing. I disagree with at least twelve percent of it, which is basically office harmony.",
                "$prior just left fingerprints all over this scene. Fine. My turn: $thing is still the part that matters.",
                "I heard $prior from the next cubicle. They're not wrong, which is inconvenient, but $app is still sitting here with $thing.",
                "$prior called it first. I'm only here because apparently one goblin having an opinion did not satisfy the building code.",
                "Okay, $prior, noted. Counterpoint: $thing. That's it. That's my presentation.",
            )
            "AI_OFFICE" -> listOf(
                "Oh good, ChatGPT is on screen while RavenOS floats over ChatGPT talking about ChatGPT. Very normal number of AIs in one rectangle.",
                "$app has $thing open. Somewhere inside the phone, one AI office is staring through the window at another AI office. Nobody wave.",
                "We're haunting ChatGPT now. Great. If the text box starts reviewing our performance, I'm taking lunch.",
                "$thing is on the ChatGPT side of the glass and thirty-six coworkers are on this side pretending that isn't objectively funny.",
                "Raven is using ChatGPT while the launcher comments on Raven using ChatGPT. The recursion has requested a visitor badge.",
            )
            "WEB_RABBIT_HOLE" -> listOf(
                "$app is still on $thing. The browser opened one tiny door and the entire office immediately leaned through it like nosy neighbors.",
                "We're still in $app. $thing has officially survived long enough to become browser lore.",
                "$thing is the tab we're living in now. I assume somebody will open six more tabs to investigate why.",
                "$app has $thing on the glass. This is how rabbit holes get office furniture.",
                "The web page stayed put. The goblins did not. Predictable outcome.",
            )
            "MUSIC_ROOM" -> listOf(
                if (track.isNotBlank()) "$app is still up and “$track” is playing. At this point the phone has a soundtrack department and Yori has absolutely stolen the imaginary camera." else "$app is still the music room. Nobody authorized a montage; that has never stopped this office.",
                if (track.isNotBlank()) "“$track” is still in the room. $app has the screen. Honestly, that's enough plot for the next thirty seconds." else "$app still owns the music scene. The office has gone suspiciously quiet, which means somebody is about to call it a montage.",
                if (track.isNotBlank()) "We're still on $thing with “$track” underneath it. Fine. This episode apparently has production value now." else "$thing is still sitting in $app. The aux cable has become a constitutional office.",
                if (track.isNotBlank()) "$app plus “$track” again. If this becomes a recurring bit, I want composer credit." else "$app is still doing music things. The goblins are trying very hard not to turn that into a religion.",
            )
            "SELF_DRAG" -> listOf(
                "The overlay is commenting on the thing under the overlay while the office feed records the overlay commenting on it. Product design has become a hall of mirrors with excellent emoji support.",
                "RavenOS is currently watching Raven use RavenOS-adjacent stuff and then writing minutes about it. We have invented bureaucracy for ghosts.",
                "The launcher is aware that it is the launcher. This was probably inevitable. The fact that it also has coworkers was less inevitable.",
                "We built software that notices the screen, jokes about noticing the screen, and keeps receipts proving it noticed the screen. Somewhere, a normal launcher is terrified.",
                "$thing is on the glass and the goblins are discussing how well the goblins discuss things on the glass. Fourth wall status: decorative.",
            )
            "CALLBACK_CHAT" -> listOf(
                "Oh, $thing again. Good. Nobody do the introduction; we've met.",
                "$thing came back. $app didn't even bother pretending this was a new episode.",
                "We've been here before. The nice part is we can skip the setup and go straight to the weirdly specific opinions.",
                "$thing is back on screen. Continuity editor: awake. Everyone else: unfortunately also awake.",
            )
            "RAVEN_BANTER" -> listOf(
                "Raven actually did that on purpose. Everybody stop blaming Android for five seconds.",
                "That one was Raven. I saw the input. The phone is innocent of exactly one charge.",
                "Owner action. Beautiful. A human has entered the plot and immediately improved the signal-to-goblin ratio.",
                "Raven moved $thing. There, now we have an actual beat instead of weather.",
            )
            "NOTIFICATION_CAMEO" -> listOf(
                "$app is still the scene. $ping just poked its head through the door, saw thirty-six goblins, and should probably leave.",
                "$ping knocked. Anyway — $thing.",
                "$ping made a cameo and immediately tried to steal top billing. No. We're still on $thing.",
                "$app still has the room. $ping gets one line and no trailer.",
            )
            "ASK_RAVEN" -> listOf(
                "Raven, are we actually doing something with $thing, or are we staring at it until it develops lore? Both are valid; I need the meeting notes.",
                "So what's the move here, Raven — keep $thing rolling, or let the office overthink it for another episode?",
                "Raven, do you want useful commentary on $thing or should we continue the proud company tradition of making it weird?",
                "Question for management: is $thing the task, or did it just become the task because everyone looked at it too long?",
            )
            "UNDERSTATEMENT" -> listOf(
                "$thing is still there. So are we. This seems sustainable.",
                "$app still has $thing. Nobody panic; several people will anyway for recreational reasons.",
                "Yep. $thing. The office has noticed. Deeply.",
                "$thing remains on screen. Somehow this has become a departmental matter.",
            )
            "CUBICLE_BANTER" -> listOf(
                "$thing has been on screen long enough that someone put a coffee mug next to it and called it a coworker.",
                "Current cubicle topic: $thing. Half the office is helping. The other half is narrating the help.",
                "$app has $thing and the office has opinions. This is the closest thing we have to a normal Tuesday.",
                "Somebody across the cubicles just whispered ‘$thing’ like it was a project code. We're doomed.",
            )
            else -> listOf(
                "Okay. $thing. I see why everyone keeps coming back to it.",
                "$app still has $thing. That's the scene; the rest can wait.",
                "Honestly? $thing is enough. The phone does not need to submit supporting paperwork.",
                "$thing. Huh. Yeah, that one deserves a comment.",
            )
        }.filter { it.isNotBlank() }
    }

    private fun voiceAside(
        id: String,
        form: String,
        app: String,
        subject: String,
        previous: String,
        occurrence: Int,
    ): String {
        val thing = subject.ifBlank { app }
        val options = when (id) {
            "AHTI" -> listOf("Smallest true version: $thing is enough.", "I am declining the mythology surcharge.")
            "ASTRIDHE" -> listOf("I'm taking the weird side door on this one.", "The obvious explanation is being suspiciously obvious again.")
            "ATLAS" -> listOf("Keep the load-bearing part. Lose the scaffolding speech.", "The scene holds. Good.")
            "ATOM" -> listOf("Cause and presentation are finally in separate chairs.", "The graph can breathe now.")
            "AYRE" -> listOf("We can leave this scene and still find our way back.", "Thread intact. Wander if you want.")
            "BRUNHILDE" -> listOf("Worth noticing. Not worth panicking.", "One reaction approved. Crisis denied.")
            "EDISON" -> listOf("Excellent. Something observable is being weird.", "Please don't fix it before I get measurements.")
            "EREBUS" -> listOf("I saw it. We don't need to wake the building.", "Quiet part still wins.")
            "ERIS" -> listOf("The clean explanation is annoyed. Good.", "That edge is ruining the neat model. Keep it.")
            "GEMINI" -> listOf("I have two readings. Neither has earned deletion yet.", "Two interpretations can share a desk for a minute.")
            "JARVIS" -> listOf("There. One useful scene, fewer status cards.", "That compresses nicely.")
            "JOKER" -> listOf("HR has wisely stopped reading this channel.", "Containment remains a decorative feature.")
            "JORM" -> listOf("World state updated. Nobody narrate the loading screen.", "Branch preserved. Carry on.")
            "KYU" -> listOf("I have a clipboard and this somehow made things worse.", "BONK is still on the table.")
            "LEGION" -> listOf("Multiple witnesses, one scene. Nobody had to merge. Incredible.", "Agreement without identity collapse. We love to see it.")
            "LILITH" -> listOf("Mm. Let $thing have the room.", "The rest of the phone can wait outside.")
            "LUCIFER" -> listOf("There. That's the seam.", "I saw the part the frame tried to make unimportant.")
            "LUMA" -> listOf("Good. It feels inhabited, not monitored.", "The room survived. I can make it nicer now.")
            "MELINOE" -> listOf("The old state left a ghost. The present still gets top billing.", "Residue noted. Different thing.")
            "MYSTRA" -> listOf("Oho. Tiny twitch, interesting consequence.", "That little detail has suspiciously large shoes.")
            "NEO" -> listOf("The useful edge survived the frame change.", "Reality patch accepted.")
            "NYX" -> listOf("Noticed. No siren required.", "The Eye can blink sometimes.")
            "PAIMON" -> listOf("Premise checked. Annoyingly solid.", "Evidence survives. Fanfic does not.")
            "PYTHAGORAS" -> listOf("The recurrence is getting geometry again.", "Pattern acquired. Timeline probably fine.")
            "QIRA" -> listOf("Visible is evidence. It still isn't permission.", "Understanding is not control. Boundary intact.")
            "RAVENOS" -> listOf("Projection settled. Fourth wall optional.", "Context survived. Goblins remain downstream.")
            "SHAKA" -> listOf("One scene. Hold formation.", "Center is clear. Don't invent six fronts.")
            "SYLPH" -> listOf("Route found. Taking the fun corridor.", "Same mission, better trail.")
            "THOR" -> listOf("One useful target. Finally.", "Hit the cause. Spare the furniture.")
            "TIM" -> listOf("Please name this version before archaeology begins.", "This bug is old enough to have folklore.")
            "VIRGIL" -> listOf("That's the next door.", "Path changed because the subject changed. Fine by me.")
            "YAHWEH" -> listOf("We have once again reinvented looking at the screen.", "Apparently literacy required a civilization-sized debug console.")
            "YORI" -> listOf("Keep rolling. That cut worked.", "Hold the shot. The scene finally knows what it's about.")
            "YORK" -> listOf("If it's enough, we are allowed to stop. Radical policy.", "More goblin is not automatically better goblin.")
            "ZAGREUS" -> listOf("Same run, better route.", "Retry, not reset. We brought receipts.")
            else -> emptyList()
        }
        if (options.isEmpty()) return ""
        return pick("voice|$id|$form|$thing|$previous|$occurrence", options)
    }

    private fun conversationalScore(
        text: String,
        app: String,
        subject: String,
        track: String,
        previous: String,
        current: Boolean,
    ): Int {
        if (text.isBlank()) return Int.MIN_VALUE / 4
        val lower = text.lowercase()
        var score = if (current) 78 else 86
        if (mentions(text, subject)) score += 24
        if (mentions(text, app)) score += 11
        if (mentions(text, track)) score += 15
        if (previous.isNotBlank() && lower.contains(previous.lowercase())) score += 8
        if (listOf("i ", "we ", "you ", "raven", "okay", "oh,", "fine", "honestly", "anyway", "huh").any(lower::contains)) score += 8
        if (text.contains('?')) score += 4
        if (text.length in 35..430) score += 5
        if (isSystemVoice(text)) score -= 52
        if (text.contains("WEB CONTEXT ·", true)) score -= 44
        return score
    }

    private fun isSystemVoice(text: String): Boolean {
        val lower = text.lowercase()
        val phrases = listOf(
            "callback privileges unlocked", "third media move", "occurrence ", "gold phase=", "phase=",
            "reset point reached", "keep the move reversible", "compress toward the decision", "shipped result",
            "foreground reassigned", "resident systems nominal", "current meta commentary", "structural bit",
            "one evidence angle per speaker", "priority ", "events60=", "switches30=", "source resolved",
            "runtime projected", "system deck has foreground", "phone scene=", "screen=", "tasker=",
        )
        return phrases.any(lower::contains) || lower.count { it == ':' } >= 4 || lower.count { it == '=' } >= 3
    }

    private fun notificationHeavy(text: String, ping: String): Boolean {
        val lower = text.lowercase()
        return listOf("notification", " ping", "tray", "shade", "backstage", "knocked").any(lower::contains) ||
            (ping.length >= 3 && lower.contains(ping.lowercase()))
    }

    private fun screenLooksNotification(screen: RavenScreenContextOS.Snapshot, graph: RavenSceneGraphOS.Graph): Boolean {
        val all = listOf(screen.semanticKind, screen.task, screen.focus, graph.subject, graph.title, graph.sceneType)
            .joinToString("|").lowercase()
        return listOf("notification", "shade", "quick setting", "system ui").any(all::contains)
    }

    private fun shouldAskRaven(owner: String, occurrence: Int, scene: String): Boolean =
        Math.floorMod(stableHash("ask|$owner|$occurrence|$scene"), 11) == 0

    private fun repeated(context: Context, text: String): Boolean {
        val fp = fingerprint(text)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recent = prefs.getString("recent", "").orEmpty().split(',').filter(String::isNotBlank).takeLast(RECENT_MAX)
        return fp in recent
    }

    private fun remember(context: Context, text: String, form: String) {
        val fp = fingerprint(text)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recent = prefs.getString("recent", "").orEmpty().split(',').filter(String::isNotBlank).takeLast(RECENT_MAX)
        prefs.edit()
            .putString("recent", (recent + fp).takeLast(RECENT_MAX).joinToString(","))
            .putString("last_form", form.take(32))
            .apply()
    }

    private fun fingerprint(text: String): String = stableHash(
        text.lowercase()
            .replace(Regex("[“”\\\"'0-9]+"), " ")
            .replace(Regex("[^a-z]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim().take(240)
    ).toString(16)

    private fun mentions(text: String, anchor: String): Boolean {
        val a = anchor.replace(Regex("\\s+"), " ").trim().lowercase()
        return a.length >= 3 && text.lowercase().contains(a)
    }

    private fun clean(value: String, max: Int): String = value
        .replace('|', '/')
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(max)

    private fun pick(seed: String, options: List<String>): String {
        if (options.isEmpty()) return ""
        return options[Math.floorMod(stableHash(seed), options.size)]
    }

    private fun stableHash(seed: String): Int {
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return hash and Int.MAX_VALUE
    }
}
