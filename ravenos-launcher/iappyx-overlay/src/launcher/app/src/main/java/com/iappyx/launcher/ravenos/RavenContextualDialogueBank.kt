package com.iappyx.launcher.ravenos

/**
 * Screen-grounded dialogue for the full office.
 *
 * Unlike the legacy event banks, this bank receives the same semantic screen packet for every
 * employee. The office therefore disagrees about the same visible thing instead of each reacting to
 * whichever Android callback happened to fire first.
 */
object RavenContextualDialogueBank {
    data class Line(val truth: String, val text: String, val family: String)

    fun select(c: RavenDialogueContextOS.ContextPacket): Line {
        if (!c.screenAvailable && c.callbackText.isBlank()) return Line("", "", "")
        val family = family(c)
        val truth = truth(c)
        val lines = ownerLines(c.ownerId, family, c)
        val text = if (lines.isEmpty()) "" else pick(
            "${c.ownerId}|$family|${c.topic}|${c.focus}|${c.occurrence}|context-v2",
            lines,
        )
        return Line(truth, text, "CONTEXT_$family")
    }

    private fun family(c: RavenDialogueContextOS.ContextPacket): String = when {
        c.meta -> "META"
        c.callbackText.isNotBlank() -> "CALLBACK"
        c.returningSubject -> "RETURN"
        c.topic == "DIALOGUE" -> "DIALOGUE"
        c.topic == "SCREEN_AWARENESS" -> "AWARENESS"
        c.topic == "GOBLIN_VISION" -> "GOBLIN"
        c.topic == "BUILD" -> "BUILD"
        c.keyboardLike -> "TYPING"
        !c.screenChanged && c.dwellCount >= 3 -> "DWELL"
        else -> "SCREEN"
    }

    private fun truth(c: RavenDialogueContextOS.ContextPacket): String {
        if (!c.screenAvailable) return c.callbackText.take(150)
        val app = c.app.ifBlank { "The screen" }
        val focus = quote(c.focus)
        return when {
            c.meta -> "$app is visibly talking about RavenOS itself: $focus"
            c.returningSubject -> "$app returned to ${c.topicLabel}: $focus"
            c.topic == "DIALOGUE" -> "$app is discussing office dialogue behavior: $focus"
            c.topic == "SCREEN_AWARENESS" -> "$app is discussing screen awareness: $focus"
            c.topic == "GOBLIN_VISION" -> "$app is on Goblin Vision / Follow-Me Office: $focus"
            c.keyboardLike -> "$app has an active typing layer over ${c.topicLabel}: $focus"
            c.semanticSummary.isNotBlank() -> "$app · ${c.semanticSummary}: $focus"
            else -> "$app: $focus"
        }.take(176)
    }

    private fun ownerLines(id: String, family: String, c: RavenDialogueContextOS.ContextPacket): List<String> {
        val app = c.app.ifBlank { "this screen" }
        val topic = c.topicLabel.ifBlank { "the current subject" }
        val focus = c.focus.replace(Regex("\\s+"), " ").trim().take(78)
        val callback = c.callbackText.take(112)
        return when (id) {
            "AHTI" -> listOf(
                "Screen receipt is clean enough to speak. Context first; performance second.",
                "The glass actually says “$focus”. Good. Now the joke has evidence under it.",
                if (callback.isNotBlank()) "$callback Receipt lineage preserved." else "Observation settled. No need to inflate it into prophecy.",
            )
            "ASTRIDHE" -> listOf(
                "There’s a side door in “$focus”. That is more interesting than the app transition.",
                "$app is pointing at $topic, but the weird edge is what it implies next.",
                "Far-field read: the screen is doing more than the callback admitted.",
            )
            "ATLAS" -> listOf(
                "The visible structure is stable: $topic. We can build on that instead of chasing window noise.",
                "Screen frame holds. Context has load-bearing shape now.",
                "Keep this subject anchored while the rest of Android thrashes around it.",
            )
            "ATOM" -> listOf(
                "The bottleneck is no longer sensing. It is whether “$focus” survives into the actual response path.",
                "$topic is the causal object. The phone action is merely how we arrived here.",
                "Good. Screen meaning and callback cause are finally separate variables.",
            )
            "AYRE" -> listOf(
                "I still know where we are: $app, $topic. Context survived the transition.",
                "The room changed around the subject; the return path stayed intact.",
                "We can wander. “$focus” is enough of a breadcrumb to get home.",
            )
            "BRUNHILDE" -> listOf(
                "The screen is asking for judgment, not alarm. $topic first; reaction second.",
                "Visible evidence acquired. Nobody gets to turn it into a crisis just because it is loud.",
                "I will grant attention to “$focus”. I will not grant panic.",
            )
            "EDISON" -> listOf(
                "Excellent. “$focus” is an observable condition instead of a vague complaint.",
                "$app just gave us a test case for $topic. Instrument that, not the transition animation.",
                "This is useful failure geometry. I can put probes on useful failure geometry.",
            )
            "EREBUS" -> listOf(
                "Quiet read: $topic is still underneath the noise.",
                "The interesting part of the screen is not moving. That is why I noticed it.",
                "Low-volume context retained: “$focus”.",
            )
            "ERIS" -> listOf(
                "The neat model said one thing; the glass says “$focus”. I vote for the glass.",
                "$app is leaking actual context through the clean abstraction. Excellent.",
                "Now this is a useful mess: $topic has observable consequences.",
            )
            "GEMINI" -> listOf(
                "One reading says $topic. The other says the framing around it matters just as much.",
                "Same glass, two interpretations. I am keeping both until one earns deletion.",
                "Contrast preserved: “$focus” is content; the way we reached it is a separate fact.",
            )
            "JARVIS" -> listOf(
                "Compressed: $app is about $topic. Everything else is support telemetry.",
                "I can turn “$focus” into one useful office action instead of twelve status blurbs.",
                "Context retained. Decorative sensor chatter removed.",
            )
            "JOKER" -> listOf(
                familyMeta(c, "The rectangle is now explaining the rectangle that is reading the explanation. This is excellent governance."),
                "We finally taught thirty-six coworkers to look at “$focus” before opening their mouths. Revolutionary workplace policy.",
                "Plot update: $topic has replaced ‘window changed’ as the protagonist.",
            )
            "JORM" -> listOf(
                "World state is not ‘app changed’. World state is now: $app / $topic / “$focus”.",
                "Branch recorded. The meaningful state transition happened in the subject, not the package name.",
                "I have the scene state. Android may stop narrating itself now.",
            )
            "KYU" -> listOf(
                familyMeta(c, "CLIPBOARD IMPACT EVENT: the bug report is literally on the glass and the glass is reading the bug report."),
                "BONK. “$focus” is the actual meeting agenda. Thank you for finally attending the meeting, phone.",
                "Management note: $topic is real context. ‘FOREGROUND_WINDOW’ is not a personality.",
            )
            "LEGION" -> listOf(
                "Many signals point at the same scene: $topic. None of them need to become each other.",
                "The screen, OCR, accessibility semantics, and callback history agree enough to contribute. Adoption remains separate.",
                "Multiple witnesses; one visible subject; no compulsory identity soup.",
            )
            "LILITH" -> listOf(
                "Mm. “$focus” is worth attention. The ten callbacks around it are not.",
                "$app has a pulse now because the office is actually beside the subject instead of pawing at every notification.",
                "Stay with $topic. Let the rest of the phone knock if it wants something.",
            )
            "LUCIFER" -> listOf(
                "There. “$focus” is the real seam. Stop staring at the smoke around it.",
                "$topic finally exposed something concrete enough to challenge.",
                "If the screen withholds context, say so. If it shows context, do not look away.",
            )
            "LUMA" -> listOf(
                "The room feels coherent now: $app, $topic, one useful observation.",
                "Good. The office can be present without filling every quiet second with furniture noise.",
                "“$focus” gives the scene enough continuity to feel inhabited instead of instrumented.",
            )
            "MELINOE" -> listOf(
                "The old subject left a trace. This one is $topic now.",
                c.previousFocus.takeIf { it.isNotBlank() }?.let { "We moved from “${it.take(58)}” to “$focus”. Residue mapped." }
                    ?: "The visible thing changed; its ghost state did not need to become dialogue.",
                "Absence is part of the scene too. I am keeping track of what stopped being visible.",
            )
            "MYSTRA" -> listOf(
                "LOOK. “$focus”. Tiny sign, huge implication. That one. ;)",
                "$topic just flashed a very interesting little edge through the glass.",
                "The screen whispered something specific enough to be suspicious. Perfect.",
            )
            "NEO" -> listOf(
                "The frame shuffled, but “$focus” survived as the useful edge.",
                "$app is not the reality. $topic is the current reality patch.",
                "Screen state acquired. Ignore the decorative matrix rain from Android callbacks.",
            )
            "NYX" -> listOf(
                "I can watch “$focus” without announcing every breath the phone takes.",
                "$topic stayed after the noise left. That is the part worth keeping.",
                "The Eye can blink. The scene does not have to.",
            )
            "PAIMON" -> listOf(
                "Premise check passed: the screen actually says “$focus”. Now we can reason from it.",
                "Good evidence chain: visible subject → $topic → office reaction. No package-name fortune telling required.",
                "Before optimizing the joke, confirm the screen interpretation. This one clears the bar.",
            )
            "PYTHAGORAS" -> listOf(
                "Occurrence ${c.occurrence}; subject return ${c.returnCount}; dwell ${c.dwellCount}. The joke finally has geometry tied to meaning.",
                "$topic is a state node now, not a pile of callbacks.",
                "The recurrence graph around “$focus” is becoming offensively legible.",
            )
            "QIRA" -> listOf(
                "Reading the screen grants context, not obligation. $topic remains bounded.",
                "Visible does not mean actionable. “$focus” is evidence; consent rules still exist.",
                "Good. We can understand the surface without pretending understanding grants control.",
            )
            "RAVENOS" -> listOf(
                "Dumbchecksum: the office is now reacting to the subject on the glass, not the callback underneath it.",
                "Settled projection: $app / $topic / “$focus”. Joke remains downstream of receipt.",
                "Screen-first path green enough to present. No fake omniscience required.",
            )
            "SHAKA" -> listOf(
                "Formation holds. $topic is the front that matters; the other signals can wait.",
                "One scene, many inputs. We do not need to answer every input separately.",
                "Observe the actual screen. Hold the line against sensor spam.",
            )
            "SYLPH" -> listOf(
                "Route update! We are in $app and the actual landmark is “$focus”.",
                "$topic is the biome now. I am routing by subject instead of by app icon.",
                "The phone changed rooms; the interesting trail is still on the glass.",
            )
            "THOR" -> listOf(
                "Target acquired: $topic. Hit the cause, not twelve notification shadows.",
                "“$focus” is concrete enough to swing at. Finally.",
                "One screen meaning. One strike. No architecture sermon.",
            )
            "TIM" -> listOf(
                "Ah, there it is: $topic. The stale defect was hiding behind event spam.",
                "“$focus” looks like the kind of bug that survives three local fixes and a meeting.",
                "Context fossil found. Somebody patched the callback and forgot the behavior.",
            )
            "VIRGIL" -> listOf(
                "The visible sentence identifies the next door: “$focus”.",
                "$topic is the chamber we are actually in now. Keep moving through the real obstacle.",
                "The path changed because the subject changed, not because the window manager coughed.",
            )
            "YAHWEH" -> listOf(
                "The ancient system would call this a screen. Apparently the modern office needed thirty-six employees to agree.",
                "$app says “$focus”. Fine. At least somebody is finally reading before touching the debug console.",
                "Wonderful. Context awareness has rediscovered the concept of looking at the thing you are debugging.",
            )
            "YORI" -> listOf(
                "Now the shot has a subject: $topic. Keep the camera on that instead of cutting on every callback.",
                "“$focus” is the composition. App transitions are editing, not story.",
                "Good scene direction: hold on the meaningful text, let the phone action happen off-camera.",
            )
            "YORK" -> listOf(
                "Before adding more noise: does $topic actually satisfy what Raven wanted from this moment?",
                "“$focus” is the desire signal. We can stop once the office understands it well enough.",
                "More goblin is not automatically better goblin. This context earns its screen time.",
            )
            "ZAGREUS" -> listOf(
                "Back through the loop, but the subject changed. Good — that means this run is not a reset.",
                "We returned with receipts. “$focus” is the new room, not the old failure replayed.",
                "Again, but smarter: keep $topic and discard the callback spam we died to last run.",
            )
            else -> listOf(
                "$app is on $topic. “$focus” is the part worth reacting to.",
                "Screen meaning acquired. Event noise can stay underneath.",
            )
        }
    }

    private fun familyMeta(c: RavenDialogueContextOS.ContextPacket, line: String): String =
        if (c.meta) line else "The current subject is ${c.topicLabel}; that is more interesting than the callback that delivered it."

    private fun quote(text: String): String = "“${text.replace(Regex("\\s+"), " ").trim().take(118)}”"

    private fun pick(seed: String, options: List<String>): String {
        var hash = 0x811C9DC5.toInt()
        for (ch in seed) { hash = hash xor ch.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
