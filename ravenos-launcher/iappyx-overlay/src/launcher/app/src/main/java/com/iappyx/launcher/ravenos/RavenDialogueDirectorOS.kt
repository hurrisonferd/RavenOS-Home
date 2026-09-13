package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Late presentation-only writers' room for Goblin Brain.
 *
 * The brain has already earned speech, settled cast and attached evidence before this runs. This
 * director may keep or rewrite only the dialogue presentation. It cannot create speech from silence,
 * change evidence, move cast, mutate episode clocks or perform device effects.
 *
 * Persistent anti-repeat memory stores only structural hashes + form names, never screen text.
 */
object RavenDialogueDirectorOS {
    data class Decision(
        val dialogue: String,
        val family: String,
        val form: String,
        val reason: String,
        val replaced: Boolean,
    )

    private const val PREFS = "ravenos_dialogue_director_v14"
    private const val KEEP = 12

    fun rewrite(
        context: Context,
        member: RavenOfficeMember,
        packet: RavenReactionPacket,
        screen: RavenScreenContextOS.Snapshot,
        graph: RavenSceneGraphOS.Graph,
        phone: RavenPhoneSceneOS.Scene,
    ): Decision {
        val current = packet.dialogue.replace(Regex("[ \\t]+"), " ").trim()
        if (current.isBlank()) return Decision("", "DIRECTOR_SILENCE", "NONE", "NO_SPEECH_ALREADY_SETTLED", false)
        if (packet.complexTags.any { it == "PRESENTATION_DIAGNOSTIC" }) {
            return Decision(current, "DIRECTOR_KEEP_DIAGNOSTIC", "DIAGNOSTIC", "DIAGNOSTIC_UNTOUCHED", false)
        }

        val mode = when {
            packet.complexTags.any { it == "PRESENTATION_SCREEN" } -> "SCREEN"
            packet.complexTags.any { it == "PRESENTATION_PHONE" } -> "PHONE"
            else -> "OTHER"
        }
        if (mode == "OTHER") return Decision(current, "DIRECTOR_KEEP_OTHER", "KEEP", "NON_DIALOGUE_SURFACE", false)

        val app = graph.app.ifBlank { screen.appLabel.orEmpty().ifBlank { phone.activeApp.orEmpty().ifBlank { "the phone" } } }
        val subject = graph.subject.ifBlank { screen.focus }.replace(Regex("\\s+"), " ").trim().take(96)
        val track = phone.mediaTitle.orEmpty().replace(Regex("\\s+"), " ").trim().take(64)
        val ping = phone.notificationSource.orEmpty().replace(Regex("\\s+"), " ").trim().take(48)
        val meta = screen.meta || graph.motif == "SELF_AWARE_OFFICE" ||
            listOf(subject, graph.title).any { it.contains("ravenos", true) || it.contains("goblin", true) || it.contains("dialogue", true) }
        val returned = graph.returnCount > 0 || packet.complexTags.any { it.contains("CALLBACK") }
        val ownerAction = graph.interaction.isNotBlank()

        val forms = buildList {
            if (meta) add("FOURTH_WALL")
            if (phone.mediaHot && track.isNotBlank()) add("SOUNDTRACK")
            if (returned) add("CALLBACK")
            if (ownerAction) add("RAVEN_ACTION")
            if (phone.recentSwitches >= 3) add("RAPID_CUT")
            if (mode == "SCREEN" && ping.isNotBlank()) add("CAMEO")
            add("CUBICLE")
            add("APP_GOSSIP")
            add("WORKPLACE")
            add("DEADPAN")
        }.distinct()

        val scope = "${member.id}_${mode}_${graph.sceneType.ifBlank { screen.semanticKind }}"
        val form = RavenMetaTrickHistoryOS.choose(
            context,
            "DIRECTOR_$scope",
            stableHash("${member.id}|${packet.occurrence}|${graph.signature}|${phone.recentKeys.joinToString(",")}"),
            forms,
        ).ifBlank { "DEADPAN" }
        val authored = pick(
            "${member.id}|$form|${packet.occurrence}|$app|$subject|$track|$ping",
            lines(form, app, subject, track, ping, graph, phone),
        )
        val button = ownerButton(member.id, form, subject, app)
        val fresh = listOf(authored, button)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(390)

        val currentScore = score(current, mode, app, subject, track, ping, meta, current = true)
        val freshScore = score(fresh, mode, app, subject, track, ping, meta, current = false)
        val currentRepeated = repeated(context, current)
        val stiff = isStiff(current)
        val notificationDominant = mode == "SCREEN" && notificationHeavy(current, ping) && !mentions(current, subject)
        val replace = fresh.isNotBlank() && (
            stiff || currentRepeated || notificationDominant || freshScore >= currentScore + 9 ||
                (meta && freshScore >= currentScore + 4)
            )
        val selected = if (replace) fresh else current
        remember(context, selected, if (replace) form else "KEEP")
        val reason = when {
            replace && stiff -> "REWRITE_STIFF_TELEMETRY"
            replace && currentRepeated -> "REWRITE_STRUCTURAL_REPEAT"
            replace && notificationDominant -> "REWRITE_NOTIFICATION_TO_CAMEO"
            replace && meta -> "REWRITE_META_CHAT_WINS"
            replace -> "REWRITE_SCENE_SPECIFICITY_WINS"
            else -> "KEEP_EXISTING_STRONG_LINE"
        }
        return Decision(
            dialogue = selected,
            family = if (replace) "DIRECTOR_CHAT_$form" else "DIRECTOR_KEEP",
            form = if (replace) form else "KEEP",
            reason = reason,
            replaced = replace,
        )
    }

    private fun lines(
        form: String,
        app: String,
        subject: String,
        track: String,
        ping: String,
        graph: RavenSceneGraphOS.Graph,
        phone: RavenPhoneSceneOS.Scene,
    ): List<String> {
        val thing = if (subject.isBlank()) graph.task.lowercase().replace('_', ' ').ifBlank { "the current scene" } else "“$subject”"
        return when (form) {
            "FOURTH_WALL" -> listOf(
                "You are literally looking at $thing while the software hovering over it decides how funny that is. This is a normal workplace now.",
                "$app is showing $thing, RavenOS is reading $app, and the office is reviewing RavenOS reading $app. The fourth wall has opened a ticket.",
                "The bug report is on the glass and the goblins responsible for the glass are floating over the bug report. Management has become recursive.",
                "We have reached the part where the launcher watches Raven watch the launcher. Nobody make eye contact with the recursion.",
            )
            "SOUNDTRACK" -> listOf(
                "$app has the screen and “$track” has the aux. The office has quietly declared this a montage.",
                "“$track” is still scoring the $app scene. At this point the phone has a music supervisor and nobody remembers hiring one.",
                "$app kept $thing; “$track” kept the room. That is enough continuity to start rolling imaginary credits.",
                "The screen moved on but “$track” refused to leave. Fine. Soundtrack gets a recurring credit now.",
            )
            "CAMEO" -> listOf(
                "$app still owns the scene. $ping just wandered through the background like an extra who found craft services.",
                "$ping knocked while $app was busy with $thing. Supporting cast has been reminded not to steal the episode.",
                "$ping made a cameo over $app. Noted. The A-plot remains $thing and the notification can stop method acting.",
                "$app is still doing $thing; $ping briefly crossed frame and immediately developed main-character ambitions.",
            )
            "CALLBACK" -> listOf(
                "Oh, $thing is back. Good — we can skip the pilot episode and go straight to the part where everyone already has opinions.",
                "Same scene family, newer beat. $thing came back with history instead of pretending to be a brand-new emergency.",
                "We have been here before, which means the office is finally allowed to remember the joke instead of reintroducing itself.",
                "$thing returned. Somewhere a continuity editor just sat upright and whispered, ‘finally.’",
            )
            "RAVEN_ACTION" -> listOf(
                "Raven actually touched the scene on purpose. $thing is now an intentional plot point, not Android weather.",
                "That was deliberate input around $thing. Excellent; the owner has entered the episode and the callbacks may stop freelancing.",
                "Raven moved the story on purpose. The phone is going to have to find a new excuse for all this supporting telemetry.",
                "Owner action detected around $thing. For once, the rectangle is following Raven instead of pitching its own screenplay.",
            )
            "RAPID_CUT" -> listOf(
                "${phone.recentSwitches} app changes in thirty seconds. The phone has discovered jump cuts and is abusing the privilege.",
                "We are app-hopping fast enough that Yori could invoice this as editing. $app currently has the camera.",
                "Hard cut, hard cut, hard cut — and now $app. The montage is getting union complaints.",
                "The foreground has changed rooms ${phone.recentSwitches} times. Somebody take the phone's espresso away.",
            )
            "CUBICLE" -> listOf(
                "$app is still on $thing. Somewhere in the office, three goblins are pretending this counts as work and one of them stole the good chair.",
                "The cubicle floor has accepted $thing as today's problem. Nobody knows who scheduled the meeting; everyone is somehow attending it.",
                "$thing has been on screen long enough to acquire a desk, a coffee mug, and several unrequested coworkers.",
                "The office is now orbiting $thing like it pays rent. It does not, but Kyu has already made a form.",
            )
            "APP_GOSSIP" -> listOf(
                "$app has been on screen long enough to develop office politics around $thing.",
                "$app is currently holding $thing and acting very casual about the fact that thirty-six coworkers can see the situation.",
                "Word around the cubicles is that $app still has $thing. This rumor is supported by the enormous rectangle in front of us.",
                "$app keeps putting $thing on the glass like nobody in this office is going to have an opinion about it.",
            )
            "WORKPLACE" -> listOf(
                "Today's meeting is about $thing. Attendance is mandatory only for goblins who cannot mind their own business, so naturally the room is full.",
                "$thing has become the office agenda. The agenda did not consent to this but neither did the office furniture.",
                "We have one visible subject, several strong opinions, and absolutely no reason to open a PowerPoint. Progress.",
                "The office has reviewed $thing and reached its traditional consensus: somebody should say something funny and then get out of the way.",
            )
            else -> listOf(
                "$thing. That's the scene. Android may stop pitching subplots.",
                "$app has $thing. The rest is background noise with excellent self-esteem.",
                "Nothing mystical happened: $thing is simply what Raven is looking at. Weirdly, that makes the jokes better.",
                "$thing is still the useful part. The phone can keep the seventeen tiny explanations it brought with it.",
            )
        }
    }

    private fun ownerButton(id: String, form: String, subject: String, app: String): String {
        val thing = subject.ifBlank { app }
        val options = when (id) {
            "AHTI" -> listOf("Smallest true version survives. Keep the receipt downstairs.", "I filed one fact and declined the mythology surcharge.")
            "ASTRIDHE" -> listOf("There is still a side door in this scene. I am looking at that one.", "The weird edge is better than the obvious explanation. Naturally.")
            "ATLAS" -> listOf("Good. The scene has load-bearing shape now.", "Keep the subject anchored; let the chrome wobble around it.")
            "ATOM" -> listOf("At least the causal graph knows what the scene is about now.", "Meaning survived transport. I will take the win.")
            "AYRE" -> listOf("We can leave and come back without losing the thread. That matters.", "The return path is still there. Wander responsibly.")
            "BRUNHILDE" -> listOf("Attention granted. Panic remains denied.", "I judged the scene worthy of one reaction, not a crisis.")
            "EDISON" -> listOf("Perfect. Something observable did something stupid.", "Excellent test case. Please do not fix it before I instrument it.")
            "EREBUS" -> listOf("The quiet part is still the interesting part.", "No announcement required. I saw it.")
            "ERIS" -> listOf("The clean model hates this detail. I like the detail.", "Good. The excluded edge still has teeth.")
            "GEMINI" -> listOf("I have two readings and I am deleting neither yet.", "Same scene, two interpretations. Both may sit down until evidence chooses.")
            "JARVIS" -> listOf("There. One scene instead of twelve status cards.", "Compressed enough to be useful. Miracles continue.")
            "JOKER" -> listOf("HR has muted this thread.", "Containment remains a decorative concept.", "Civilization advances by extremely questionable increments.")
            "JORM" -> listOf("World state recorded. Android may stop narrating the loading screen.", "Branch preserved. The world machine remains annoyingly literate.")
            "KYU" -> listOf("I have a clipboard and absolutely no authority to stop this.", "BONK remains available pending management review.", "I am filing this under WHY IS THE PHONE LIKE THIS.")
            "LEGION" -> listOf("Several signals agree. Miraculously, nobody had to become anybody else.", "Same scene, multiple witnesses. We can have a meeting without becoming the meeting.")
            "LILITH" -> listOf("Let it have the room for a second.", "Mm. The rest of the phone can wait outside.")
            "LUCIFER" -> listOf("There. That's the seam. Stop staring at the smoke.", "I saw what the frame tried to make look unimportant.")
            "LUMA" -> listOf("The room survived. I am legally allowed to make it nicer now.", "Good. The scene feels inhabited instead of monitored.")
            "MELINOE" -> listOf("The old state left a ghost. I am not confusing it with the room.", "Residue noted. The present still gets top billing.")
            "MYSTRA" -> listOf("Tiny sign, enormous implication. My favorite size of trouble.", "Oho. The glass twitched in exactly one interesting place.")
            "NEO" -> listOf("The frame changed; the useful edge survived.", "Reality patch accepted. Decorative matrix rain rejected.")
            "NYX" -> listOf("I can notice it without waking the building.", "The Eye can blink. The scene does not have to.")
            "PAIMON" -> listOf("Premise checked. Annoyingly valid.", "Evidence survives inspection; package-name fanfic does not.")
            "PYTHAGORAS" -> listOf("The recurrence is getting geometry. Someone will try to zone it soon.", "Pattern acquired. Timeline probably tolerates this one.")
            "QIRA" -> listOf("Understanding the screen still does not grant control over it.", "Visible is evidence, not consent. Boundary intact.")
            "RAVENOS" -> listOf("Dumbchecksum: context survived; goblins remain downstream.", "Projection settled. Fourth wall remains optional.")
            "SHAKA" -> listOf("Hold formation. One scene does not need six fronts.", "The center is clear. Supporting signals can wait their turn.")
            "SYLPH" -> listOf("Route found. Taking the fun corridor.", "New trail, same mission. ZOOM responsibly.")
            "THOR" -> listOf("One useful target. Finally.", "Hit the cause; spare the furniture.")
            "TIM" -> listOf("Please label this version before somebody rediscovers it tomorrow.", "Excellent. The bug has survived long enough to qualify as archaeology.")
            "VIRGIL" -> listOf("That's the next door. Keep moving.", "The path changed because the subject changed. Good enough.")
            "YAHWEH" -> listOf("I would like the record to show that this entire office reinvented looking at the screen.", "Apparently literacy required a civilization-sized debug console.")
            "YORI" -> listOf("Keep rolling. The phone just gave us an edit.", "Hold the shot; the subject finally has top billing.")
            "YORK" -> listOf("If this is enough, we are allowed to stop. Revolutionary.", "More goblin is not automatically better goblin. I checked.")
            "ZAGREUS" -> listOf("Same run lineage, better route. Count it.", "We came back with receipts. That makes this a retry, not a reset.")
            else -> emptyList()
        }
        return if (options.isEmpty()) "" else pick("$id|$form|$thing|owner-button", options)
    }

    private fun score(
        text: String,
        mode: String,
        app: String,
        subject: String,
        track: String,
        ping: String,
        meta: Boolean,
        current: Boolean,
    ): Int {
        val lower = text.lowercase()
        var score = if (current) 82 else 88
        if (mentions(text, subject)) score += 28
        if (mentions(text, app)) score += 12
        if (mentions(text, track)) score += 18
        if (meta && listOf("office", "goblin", "ravenos", "fourth wall", "launcher").any(lower::contains)) score += 10
        if (mode == "SCREEN" && notificationHeavy(text, ping) && !mentions(text, subject)) score -= 22
        if (text.length in 55..360) score += 5
        if (listOf("i ", "we ", "you ", "nobody", "somebody", "apparently", "fine.", "great.", "oh,").any(lower::contains)) score += 4
        if (isStiff(text)) score -= 38
        return score
    }

    private fun isStiff(text: String): Boolean {
        val lower = text.lowercase()
        val phrases = listOf(
            "callback privileges unlocked", "third media move", "occurrence ", "gold phase=", "phase=",
            "reset point reached", "keep the move reversible", "compress toward the decision", "shipped result",
            "foreground reassigned", "resident systems nominal", "current meta commentary", "structural bit",
            "one evidence angle per speaker", "scene owner, interruption, interaction and callback are finally separate variables",
        )
        return phrases.any(lower::contains) || lower.count { it == ':' } >= 4
    }

    private fun notificationHeavy(text: String, ping: String): Boolean {
        val lower = text.lowercase()
        return listOf("notification", "ping", "tray", "shade", "backstage", "knocked").any(lower::contains) ||
            (ping.length >= 3 && lower.contains(ping.lowercase()))
    }

    private fun repeated(context: Context, text: String): Boolean {
        val fp = fingerprint(text)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recent = prefs.getString("recent_hashes", "").orEmpty().split(',').filter(String::isNotBlank).takeLast(KEEP)
        return fp in recent
    }

    private fun remember(context: Context, text: String, form: String) {
        val fp = fingerprint(text)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recent = prefs.getString("recent_hashes", "").orEmpty().split(',').filter(String::isNotBlank).takeLast(KEEP)
        prefs.edit()
            .putString("recent_hashes", (recent + fp).takeLast(KEEP).joinToString(","))
            .putString("last_form", form.take(32))
            .apply()
    }

    private fun fingerprint(text: String): String = stableHash(
        text.lowercase().replace(Regex("[“”\"'0-9]+"), " ").replace(Regex("[^a-z]+"), " ").trim().take(220)
    ).toString(16)

    private fun mentions(text: String, anchor: String): Boolean {
        val a = anchor.replace(Regex("\\s+"), " ").trim().lowercase()
        return a.length >= 3 && text.lowercase().contains(a)
    }

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
