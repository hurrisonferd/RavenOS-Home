package com.iappyx.launcher.ravenos

import android.content.Context

/** Deterministic viewport dialogue grounded in Scene Graph v2 and the local Dialogue Vault. */
object RavenViewportDialogueOS {
    data class Line(val text: String, val family: String)

    fun select(
        context: Context,
        member: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        direction: RavenSitcomDirectorOS.Direction,
    ): Line {
        if (!screen.available) return Line("", "VIEWPORT_NONE")
        val graph = RavenSceneGraphOS.observe(context, screen)

        // Match GoblinBrain's existing viewport-writer gate so vault usage is only charged when the
        // line is actually eligible to reach presentation, not on every silent sensor update.
        val vaultEligible = direction.turn % 4 == 0 ||
            (direction.beat in setOf("COLD_OPEN", "META", "CALLBACK") && direction.turn % 2 == 0)
        if (vaultEligible) {
            RavenDialogueVaultOS.select(context, member, direction, graph)?.let {
                return Line(it.text, it.family)
            }
        }

        val task = graph.task
        val app = graph.app.ifBlank { screen.semanticKind.lowercase().replaceFirstChar { it.titlecase() } }
        val subject = graph.subject.ifBlank { screen.focus }.replace(Regex("\\s+"), " ").trim().take(92)
        val title = graph.title.take(72)
        val seed = "${member.id}|$task|$app|$title|$subject|${direction.turn}|${graph.signature}|viewport-dialogue-v2"

        val truth = when (task) {
            "COMPOSING" -> pick(seed, listOf(
                "$app has an input layer open while the visible conversation is about “$subject”.",
                "You're composing in $app; the surrounding visible thread is still “$subject”.",
                "$app is in compose mode, but the scene itself is about “$subject”.",
            ))
            "READING_CHAT" -> pick(seed, listOf(
                "You're reading a live conversation in $app about “$subject”.",
                "$app is a chat scene right now; “$subject” is the visible subject, not the toolbar.",
                "The conversation on the glass is centered on “$subject”.",
            ))
            "BROWSING", "WEB" -> pick(seed, listOf(
                "You're browsing ${title.ifBlank { app }}; the visible page is on “$subject”.",
                "${title.ifBlank { app }} is the active web scene, with “$subject” holding the frame.",
                "Browser context acquired: ${title.ifBlank { app }} / “$subject”.",
            ))
            "DEBUGGING" -> pick(seed, listOf(
                "$app is visibly in build/debug territory: “$subject”.",
                "The glass is showing actual source/build work around “$subject”.",
                "Debug scene acquired in $app: “$subject”.",
            ))
            "CONFIGURING" -> pick(seed, listOf(
                "$app is in configuration mode around “$subject”.",
                "The visible settings task is “$subject”.",
                "Configuration scene: $app / “$subject”.",
            ))
            "LISTENING" -> pick(seed, listOf(
                "$app is in a listening scene and “$subject” is the visible media context.",
                "The soundtrack owns the room; “$subject” is what the screen is actually showing.",
                "Media scene acquired: “$subject”.",
            ))
            "WATCHING" -> pick(seed, listOf(
                "$app is in watch mode; “$subject” is the visible subject.",
                "The screen is presenting “$subject” as the current video scene.",
                "Watch scene acquired: $app / “$subject”.",
            ))
            "SEARCHING" -> pick(seed, listOf(
                "$app is visibly searching around “$subject”.",
                "Search scene acquired; “$subject” is the useful visible context.",
                "You're searching in $app and the glass is currently on “$subject”.",
            ))
            "READING" -> pick(seed, listOf(
                "You're reading “$subject” in $app.",
                "$app is a reading scene now; “$subject” is carrying the page.",
                "The visible page is being read, not merely opened: “$subject”.",
            ))
            "TYPING" -> pick(seed, listOf(
                "$app has the keyboard up while “$subject” remains the visible scene.",
                "Typing layer active; the screen context underneath is “$subject”.",
                "Input is open in $app. The actual visible subject is still “$subject”.",
            ))
            else -> pick(seed, listOf(
                "$app is visibly on “$subject”.",
                "The current screen subject in $app is “$subject”.",
                "Scene read: $app / “$subject”.",
            ))
        }
        val punch = ownerPunch(member.id, task, direction, graph, seed)
        return Line(listOf(truth, punch).filter(String::isNotBlank).joinToString(" ").take(240), "VIEWPORT_$task")
    }

    private fun ownerPunch(id: String, task: String, d: RavenSitcomDirectorOS.Direction, graph: RavenSceneGraphOS.Graph, seed: String): String {
        val options = when (id) {
            "KYU" -> listOf("Clipboard says context before bonk.", "Good. We can now bonk the correct problem.", "The toolbar has been denied protagonist status.")
            "JOKER" -> listOf("The fourth wall has requested browser permissions.", "Excellent. The rectangle knows what the other rectangle is doing.", "Containment remains mostly decorative.")
            "ATOM" -> listOf("Task state and visible subject finally agree.", "Good; action and context are separate variables now.", "That is enough state to reason instead of guess.")
            "PAIMON" -> listOf("Premise survives viewport inspection.", "Evidence chain improved; package-name fanfic rejected.", "The visible subject passes inspection.")
            "YORI" -> listOf("Hold the shot; that is the actual composition.", "Do not cut away to telemetry now.", "Good scene. Keep the subject in frame.")
            "LILITH" -> listOf("Mm. Let the rest of the phone wait its turn.", "That is the thing asking for attention; stay with it.", "Presence first. Everything else can knock.")
            "YAHWEH" -> listOf("Apparently looking at the screen required a distributed architecture.", "The old debug console called this literacy.", "Fine. The goblins have achieved reading comprehension.")
            "TIM" -> listOf("At least the stale seam now has an address.", "Good. The defect fossil is in the right layer.", "I can work with a visible failure instead of folklore.")
            "MELINOE" -> listOf("The residue and the current scene are finally distinguishable.", "The ghost state can stop impersonating the present.", "Current presence confirmed; haunting remains optional.")
            "THOR" -> listOf("Target acquired. Hammer remains holstered for one sentence.", "Useful target. Now one strike, not seven callbacks.", "Finally, something with coordinates.")
            else -> listOf("Screen meaning acquired.", "Context locked.", "The glass gets final edit.")
        }
        val returnTag = if (graph.returnCount >= 2) " Structural return #${graph.returnCount}." else ""
        val callback = if (d.pairCount in setOf(3, 5, 8, 13)) " Pair callback #${d.pairCount}." else ""
        return (pick("$seed|punch|$task", options) + returnTag + callback).trim()
    }

    private fun pick(seed: String, options: List<String>): String {
        if (options.isEmpty()) return ""
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
