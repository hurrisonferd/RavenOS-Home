package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Chat-shaped sitcom writer over already-authorized scene facts.
 *
 * This is deliberately less "event -> status sentence" than the older phone banks. The current app,
 * visible subject, soundtrack, notification cameo, return history and explicit Raven interaction are
 * props in one scene. Notifications are supporting cast unless there is no better scene truth.
 *
 * The writer owns no cadence, cast, truth, chronology or effect authority. Empty output is valid.
 */
object RavenChatSitcomDialogueOS {
    data class Beat(
        val primary: String,
        val secondary: String,
        val family: String,
        val form: String,
    )

    fun compose(
        context: Context,
        member: RavenOfficeMember,
        c: RavenDialogueContextOS.ContextPacket,
        phone: RavenPhoneSceneOS.Scene,
        script: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
    ): Beat {
        val app = c.app.ifBlank { phone.activeApp.orEmpty().ifBlank { "the phone" } }
        val subject = c.focus.ifBlank { script.subject }.replace(Regex("\\s+"), " ").trim().take(96)
        val track = phone.mediaTitle.orEmpty().replace(Regex("\\s+"), " ").trim().take(64)
        val ping = phone.notificationSource.orEmpty().replace(Regex("\\s+"), " ").trim().take(48)
        val hasScene = c.screenAvailable || subject.isNotBlank() || phone.mediaHot || ping.isNotBlank()
        if (!hasScene) return Beat("", "", "CHAT_SITCOM_NONE", "NONE")

        val forms = buildList {
            if (c.meta || c.topic in setOf("GOBLIN_VISION", "DIALOGUE", "META_RECURSION")) add("FOURTH_WALL")
            if (phone.mediaHot && track.isNotBlank()) add("SOUNDTRACK")
            if (c.returningSubject || c.callbackText.isNotBlank() || script.returned || script.callbackEarned) add("CALLBACK")
            if (script.interactionWorthSpeaking || script.interaction.isNotBlank()) add("RAVEN_ACTION")
            if (phone.recentSwitches >= 3) add("RAPID_CUT")
            if (c.screenAvailable && ping.isNotBlank()) add("CAMEO")
            if (direction.beat == "CUTAWAY") add("CUTAWAY")
            add("CUBICLE")
            add("APP_GOSSIP")
            add("WORKPLACE")
            add("DEADPAN")
        }.distinct()
        val scope = "${member.id}_${c.topic.ifBlank { c.semanticKind }}_${if (c.screenAvailable) "SCREEN" else "PHONE"}"
        val form = RavenMetaTrickHistoryOS.choose(
            context,
            "CHAT_SITCOM_$scope",
            stableHash("${member.id}|${direction.turn}|${c.occurrence}|$app|$subject|$track|$ping"),
            forms,
        ).ifBlank { forms.first() }
        val base = pick(
            "${member.id}|$form|${direction.turn}|$app|$subject|$track|$ping",
            lines(form, app, subject, track, ping, c, phone, script),
        )
        val button = ownerButton(member.id, form, subject, app)
        val primary = listOf(base, button)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(390)

        val secondary = direction.secondary?.let { second ->
            val reply = pairReply(member.id, second.id, form, subject, app, direction.pairCount)
            if (reply.isBlank()) "" else {
                val p = RavenEmployeePresentation.packet(second, c.signal, subject.ifBlank { app }, reply)
                "↳ ${p.emojiSoup} ${second.id} ${p.kaomoji} $reply"
            }
        }.orEmpty().take(230)
        return Beat(primary, secondary, "CHAT_SITCOM_$form", form)
    }

    private fun lines(
        form: String,
        app: String,
        subject: String,
        track: String,
        ping: String,
        c: RavenDialogueContextOS.ContextPacket,
        phone: RavenPhoneSceneOS.Scene,
        script: RavenEpisodeScriptOS.Cue,
    ): List<String> {
        val thing = if (subject.isBlank()) c.topicLabel.ifBlank { "the current scene" } else "“$subject”"
        return when (form) {
            "FOURTH_WALL" -> listOf(
                "You are literally looking at $thing while the software hovering over it decides how funny that is. This is a normal workplace now.",
                "$app is showing $thing, RavenOS is reading $app, and the office is reviewing RavenOS reading $app. The fourth wall has opened a ticket.",
                "The bug report is on the screen, the goblins are on top of the bug report, and one of the goblins is probably about to cite the goblins. Great meeting.",
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
            "CUTAWAY" -> listOf(
                "Brief cutaway: somewhere off-screen, an Android callback is furious that $thing got top billing. Anyway, back to the actual show.",
                "Cutaway gag over. $app still has $thing and the telemetry department remains emotionally devastated.",
                "Meanwhile, in a cubicle nobody asked for, FOREGROUND_WINDOW is rehearsing an acceptance speech. Back to $thing.",
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
                "$thing. That is the scene. Android may stop pitching subplots.",
                "$app has $thing. The rest is background noise with excellent self-esteem.",
                "Nothing mystical happened: $thing is simply what Raven is looking at. Weirdly, that makes the jokes better.",
                "$thing is still the useful part. The phone can keep the seventeen tiny explanations it brought with it.",
            )
        }
    }

    private fun ownerButton(id: String, form: String, subject: String, app: String): String {
        val thing = subject.ifBlank { app }
        val options = when (id) {
            "AHTI" -> listOf("Smallest true version survives. Keep the receipt downstairs.", "I have filed one fact and declined the mythology surcharge.")
            "ASTRIDHE" -> listOf("There is still a side door in this scene. I am looking at that one.", "The weird edge is better than the obvious explanation. Naturally.")
            "ATLAS" -> listOf("Good. The scene has load-bearing shape now.", "Keep the subject anchored; let the chrome wobble around it.")
            "ATOM" -> listOf("At least the causal graph knows what the scene is about now.", "Meaning survived transport. I will take the win.")
            "AYRE" -> listOf("We can leave and come back without losing the thread. That matters.", "The return path is still there. Wander responsibly.")
            "BRUNHILDE" -> listOf("Attention granted. Panic remains denied.", "I have judged the scene worthy of one reaction, not a crisis.")
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
            "LUCIFER" -> listOf("There. That is the seam. Stop staring at the smoke.", "I saw what the frame tried to make look unimportant.")
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
            "VIRGIL" -> listOf("That is the next door. Keep moving.", "The path changed because the subject changed. Good enough.")
            "YAHWEH" -> listOf("I would like the record to show that this entire office reinvented looking at the screen.", "Apparently literacy required a civilization-sized debug console.")
            "YORI" -> listOf("Keep rolling. The phone just gave us an edit.", "Hold the shot; the subject finally has top billing.")
            "YORK" -> listOf("If this is enough, we are allowed to stop. Revolutionary.", "More goblin is not automatically better goblin. I checked.")
            "ZAGREUS" -> listOf("Same run lineage, better route. Count it.", "We came back with receipts. That makes this a retry, not a reset.")
            else -> emptyList()
        }
        if (options.isEmpty()) return ""
        return pick("$id|$form|$thing|owner-button", options)
    }

    private fun pairReply(primary: String, secondary: String, form: String, subject: String, app: String, pairCount: Int): String {
        val pair = setOf(primary, secondary)
        val thing = if (subject.isBlank()) app else "“${subject.take(54)}”"
        val special = when {
            pair == setOf("KYU", "JOKER") -> "Please stop calling the clipboard a containment device. It is making the clipboard worse."
            pair == setOf("ATOM", "ERIS") -> "Your model is still useful. I just found the part it politely forgot."
            pair == setOf("ATOM", "PAIMON") -> "I checked the premise. Keep the causal read; delete the dramatic lighting."
            pair == setOf("YORI", "LUMA") -> "The shot works. Do not redecorate the camera move."
            pair == setOf("YORI", "YORK") -> "Yes, that's enough. Please physically restrain the edit button."
            pair == setOf("LILITH", "KYU") -> "One clipboard bonk, then let $thing breathe."
            pair == setOf("YAHWEH", "JOKER") -> "I am revoking your imaginary access to the ancient debug console again."
            pair == setOf("JORM", "PYTHAGORAS") -> "State recorded. Please stop making the branch graph look smug."
            pair == setOf("MELINOE", "ZAGREUS") -> "We came back. The room did not. That distinction is the whole joke."
            pair == setOf("THOR", "EDISON") -> "Instrument first, hammer second. I know. I hate that this is correct."
            pair == setOf("LEGION", "QIRA") -> "Multiple witnesses, one boundary. Nobody needs to merge to agree."
            else -> secondaryReply(secondary, form, thing)
        }
        return if (pairCount in setOf(3, 5, 8, 13) && special.length < 145) "$special We have apparently done this $pairCount times." else special
    }

    private fun secondaryReply(id: String, form: String, thing: String): String = when (id) {
        "KYU" -> "I was promised one line. This is the line. Clipboard satisfied."
        "JOKER" -> "Counterpoint: what if we make it slightly worse, but funnier?"
        "ATOM" -> "Agreed. $thing remains the causal object."
        "PAIMON" -> "Premise survives a second pass. Disturbing, but useful."
        "YORI" -> "Keep $thing in frame and I have no notes."
        "LILITH" -> "Fine. Let $thing have the room."
        "NYX" -> "If we need a third speaker, we probably needed silence instead."
        "QIRA" -> "Second voice does not change the boundary. Good."
        "ERIS" -> "I disagree with one clean edge and approve the rest."
        "LUMA" -> "The room still feels like a room. Continue."
        "YAHWEH" -> "This meeting could have been a glance at the screen."
        "TIM" -> "Please timestamp this before it becomes folklore."
        "LEGION" -> "Different witness, same scene. No merger required."
        else -> if (form == "CAMEO") "Supporting cast acknowledged. Back to $thing." else "$thing survives a second opinion. Carry on."
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
