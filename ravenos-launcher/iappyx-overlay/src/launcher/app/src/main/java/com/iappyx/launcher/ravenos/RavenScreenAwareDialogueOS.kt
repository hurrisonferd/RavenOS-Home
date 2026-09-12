package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Deterministic screen-first writer.
 *
 * This writer talks about the actual visible app/task/subject/selection before it reaches for
 * recurrence, notification, or generic Android-event comedy. It consumes the fused Scene Graph v2
 * at runtime and stores no raw screen text itself.
 */
object RavenScreenAwareDialogueOS {
    data class Beat(val text: String, val family: String)

    fun select(
        context: Context,
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
        priority: RavenDialoguePriorityOS.Decision,
    ): Beat {
        if (!priority.preferScreenWriter || !screen.available) return Beat("", "SCREEN_FIRST_NONE")
        val graph = runCatching { RavenSceneGraphOS.observe(context, screen, marker.at) }.getOrNull()
            ?: return Beat("", "SCREEN_FIRST_NONE")
        if (!graph.available) return Beat("", "SCREEN_FIRST_NONE")

        val app = graph.app.ifBlank { "the screen" }
        val subject = graph.selected.ifBlank { graph.subject }.replace(Regex("\\s+"), " ").trim().take(92)
        val title = graph.title.replace(Regex("\\s+"), " ").trim().take(68)
        val task = graph.task.ifBlank { "VIEWING" }
        val interaction = graph.interaction
        val media = RavenPhoneSceneOS.snapshot(context, marker.at).mediaTitle.orEmpty().replace(Regex("\\s+"), " ").trim().take(58)
        val seed = listOf(member.id, app, subject, title, task, interaction, direction.sceneId, direction.turn.toString()).joinToString("|")
        val fact = fact(app, subject, title, task, interaction)
        val line = when (member.id) {
            "YORI" -> pick(seed, listOf(
                "$fact Keep the shot on the visible subject; Android chrome is supporting cast.",
                "$fact That's the actual edit. The rest of the phone can stop auditioning for top billing.",
                "$fact Good blocking: subject first, interface second, cameo third.",
                soundtrackLine(app, subject, media, "The soundtrack can stay in the B-plot; it does not get to steal the frame."),
            ))
            "KYU" -> pick(seed, listOf(
                "$fact Clipboard ruling: this is what the office is allowed to be funny about right now.",
                "$fact Excellent. A visible premise. The notification tray has been denied comedy jurisdiction.",
                "$fact BONK reserved for the thing actually on the glass, not whichever callback yelled loudest.",
                "$fact Management has located the plot. Nobody page System UI.",
            ))
            "JOKER" -> pick(seed, listOf(
                "$fact The fourth wall appreciates finally being mocked for something it can see.",
                "$fact Excellent. Context has entered the building; generic Android jokes may now collect unemployment.",
                "$fact The haunted office has identified the actual scene. This is devastating news for telemetry-based comedy.",
                "$fact We are now reacting to the screen instead of numerically celebrating pings. Character development.",
            ))
            "ATOM" -> pick(seed, listOf(
                "$fact Screen evidence now outranks event frequency. Correct causal ordering.",
                "$fact Visible subject resolved. Treat system callbacks as inputs, not conclusions.",
                "$fact The scene has an observable object now; recurrence counts are no longer carrying the model.",
                "$fact Good. We can reason from state instead of counting interrupts.",
            ))
            "PAIMON" -> pick(seed, listOf(
                "$fact Premise check passes: that is actually visible, not inferred from a noisy phone event.",
                "$fact Evidence chain is screen-grounded. Keep the joke attached to that fact.",
                "$fact I can verify this premise from the current scene. Much better than callback numerology.",
                "$fact The visible claim survives inspection. Proceed with one joke, not a notification census.",
            ))
            "LILITH" -> pick(seed, listOf(
                "$fact Let that have the room. Everything else can wait its turn.",
                "$fact That's where the attention actually is. Stay beside it instead of pawing at every interruption.",
                "$fact The screen already told us what matters. We do not need the tray competing for affection.",
                soundtrackLine(app, subject, media, "Mm. Let the soundtrack sit underneath it instead of trying to become the whole scene."),
            ))
            "JORM" -> pick(seed, listOf(
                "$fact Current world-state has a real branch target now. Record the subject, not the interrupt count.",
                "$fact Scene state resolved. The visible choice owns this branch until something material changes.",
                "$fact That is the active world object. System cameos do not constitute a new universe.",
                "$fact State machine note: visible subject persists; background events remain subordinate transitions.",
            ))
            "PYTHAGORAS" -> pick(seed, listOf(
                "$fact Recurrence is useful only after the visible pattern is known. Geometry restored.",
                "$fact The important invariant is the subject surviving scene noise, not the number of pings around it.",
                "$fact Pattern first, count second. The screen has finally supplied the missing variable.",
            ))
            "LUMA" -> pick(seed, listOf(
                "$fact Good. The room has a center again.",
                "$fact That is enough context to feel lived-in without making every notification furniture.",
                soundtrackLine(app, subject, media, "The soundtrack can keep the room warm while the visible subject keeps the chair."),
            ))
            "NYX" -> pick(seed, listOf(
                "$fact The quiet part is knowing what not to promote.",
                "$fact One visible subject is enough. The other events can remain offstage.",
                "$fact Keep this. Silence the census.",
            ))
            "ERIS" -> pick(seed, listOf(
                "$fact Better. Now I can attack the actual scene instead of a clean little recurrence counter.",
                "$fact Visible premise acquired. I can perturb the interpretation without inventing the evidence.",
                "$fact The screen has an edge. Finally, something worth disagreeing with besides Android bookkeeping.",
            ))
            "YAHWEH" -> pick(seed, listOf(
                "$fact Ancient debugging technique: look at the thing currently on the screen. Miraculous.",
                "$fact We have rediscovered context. The modern stack will now require six services to celebrate.",
                "$fact Good. The interface is finally talking about its subject instead of filing attendance for callbacks.",
            ))
            "MYSTRA" -> pick(seed, listOf(
                "$fact SIGN: visible thing. FLIP: noisy event != plot. WINK: context wins 😉",
                "$fact Tiny sign acquired. The screen did the hard part; now the goblin can be weird accurately.",
                "$fact Oho. Actual on-glass evidence. Sparkles approved; census denied.",
            ))
            "THOR" -> pick(seed, listOf(
                "$fact Target acquired. Strike the visible cause; ignore decorative interrupt shrapnel.",
                "$fact One screen-grounded line beats five callback announcements. Done.",
                "$fact The subject is load-bearing. Everything else can get out of hammer range.",
            ))
            "AHTI" -> pick(seed, listOf(
                "$fact Smallest true version: that is what is here now.",
                "$fact Keep the visible stone; let the event-river pass around it.",
                "$fact Truth fits in the hand again. Good.",
            ))
            "RAVENOS" -> pick(seed, listOf(
                "$fact Settled projection: present screen > event recurrence.",
                "$fact Screen-grounded scene accepted. Supporting events remain supporting events.",
                "$fact Dumbchecksum passes: context outranks callback count.",
            ))
            else -> pick(seed, listOf(
                "$fact That's the visible scene. React to it, not the interrupt counter.",
                "$fact The screen supplied a subject; keep the commentary attached to it.",
                "$fact Current plot acquired. Background phone events may remain background phone events.",
            ))
        }.replace(Regex("\\s+"), " ").trim()
        return Beat(line.take(300), "SCREEN_FIRST_${priority.tier}")
    }

    private fun fact(app: String, subject: String, title: String, task: String, interaction: String): String {
        val target = subject.ifBlank { title }.ifBlank { app }
        val action = when {
            interaction == "SELECT" -> "selected “$target” in $app."
            interaction == "SCROLL" && task == "READING_CHAT" -> "is reading through “$target” in $app."
            interaction == "SCROLL" -> "is moving through “$target” in $app."
            task == "COMPOSING" || task == "TYPING" -> "is composing in $app around “$target”."
            task == "LISTENING" -> "$app has “$target” in the active music scene."
            task == "WATCHING" -> "$app is visibly on “$target”."
            task == "DEBUGGING" -> "is debugging “$target” in $app."
            task == "CONFIGURING" -> "is configuring “$target” in $app."
            task == "READING_CHAT" -> "is reading the $app conversation around “$target”."
            task in setOf("READING", "BROWSING") -> "$app is visibly about “$target”."
            else -> "$app currently shows “$target”."
        }
        return action.replace(Regex("\\s+"), " ").trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun soundtrackLine(app: String, subject: String, media: String, tail: String): String {
        if (media.isBlank()) return "$app is still centered on “${subject.ifBlank { app }}”. $tail"
        return "$app is centered on “${subject.ifBlank { app }}” while “$media” keeps soundtrack duty. $tail"
    }

    private fun pick(seed: String, options: List<String>): String {
        val clean = options.filter(String::isNotBlank)
        if (clean.isEmpty()) return ""
        var h = 0x811C9DC5.toInt()
        for (c in seed) { h = h xor c.code; h *= 16777619 }
        return clean[(h and Int.MAX_VALUE) % clean.size]
    }
}
