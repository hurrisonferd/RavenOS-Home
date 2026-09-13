package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Final writers-room mixer. Chooses among already-grounded dialogue organs without granting speech.
 *
 * The old failure mode was deterministic but dumb: whichever high-level writer fired first could
 * replace a rich screen scene with a generic callback/notification sentence. This mixer scores
 * specificity, conversational shape and structural novelty, then strongly penalizes telemetry prose.
 */
object RavenDialogueMixerOS {
    data class Candidate(
        val source: String,
        val family: String,
        val text: String,
        val baseScore: Int,
    )

    data class Choice(
        val text: String,
        val family: String,
        val source: String,
        val score: Int,
        val reason: String,
    )

    fun choose(
        context: Context,
        owner: String,
        mode: String,
        c: RavenDialogueContextOS.ContextPacket,
        graph: RavenSceneGraphOS.Graph,
        phone: RavenPhoneSceneOS.Scene,
        direction: RavenSitcomDirectorOS.Direction,
        candidates: List<Candidate>,
    ): Choice {
        val usable = candidates
            .filter { it.text.isNotBlank() }
            .distinctBy { normalize(it.text) }
        if (usable.isEmpty()) return Choice("", "MIXER_SILENCE", "NONE", 0, "NO_CANDIDATE")

        val scored = usable.map { candidate ->
            val score = score(candidate, mode, c, graph, phone, direction)
            candidate to score
        }
        val bestScore = scored.maxOf { it.second }
        val nearTop = scored.filter { it.second >= bestScore - 10 }
        val familyTokens = nearTop.map { "${it.first.source}:${it.first.family}" }.distinct()
        val scope = "${owner}_${mode}_${c.topic.ifBlank { graph.sceneType }}"
        val preferred = RavenMetaTrickHistoryOS.choose(
            context,
            "DIALOGUE_MIXER_$scope",
            stableHash("$owner|$mode|${graph.signature}|${direction.turn}|${c.occurrence}"),
            familyTokens,
        )
        val pool = nearTop.filter { "${it.first.source}:${it.first.family}" == preferred }.ifEmpty { nearTop }
        val max = pool.maxOf { it.second }
        val finalists = pool.filter { it.second == max }
        val picked = finalists[Math.floorMod(
            stableHash("$owner|$mode|${graph.signature}|${direction.turn}|$preferred|mixer-v14"),
            finalists.size,
        )]
        val reason = buildString {
            append(if (mode == "SCREEN") "SCREEN_CONTEXT_FIRST" else "PHONE_CONTEXT_FIRST")
            if (picked.first.source == "CHAT_SITCOM") append("+CHAT_SHAPED")
            if (picked.first.source == "VAULT") append("+VAULT_NOVELTY")
            if (mentionsSubject(picked.first.text, c, graph)) append("+SUBJECT")
            if (phone.mediaHot && mentions(picked.first.text, phone.mediaTitle.orEmpty())) append("+TRACK")
            if (isNotificationHeavy(picked.first.text, phone) && mode == "SCREEN") append("+CAMEO_ONLY")
        }
        return Choice(picked.first.text.take(420), picked.first.family, picked.first.source, picked.second, reason)
    }

    private fun score(
        candidate: Candidate,
        mode: String,
        c: RavenDialogueContextOS.ContextPacket,
        graph: RavenSceneGraphOS.Graph,
        phone: RavenPhoneSceneOS.Scene,
        direction: RavenSitcomDirectorOS.Direction,
    ): Int {
        val text = candidate.text
        val lower = text.lowercase()
        var score = candidate.baseScore
        val screen = mode == "SCREEN"

        if (screen) {
            if (mentionsSubject(text, c, graph)) score += 34
            if (mentions(text, c.app)) score += 13
            if (mentions(text, graph.title)) score += 10
            if (mentions(text, graph.selected)) score += 12
            if (c.meta && containsAny(lower, "fourth wall", "office", "goblin", "ravenos", "launcher", "clipboard", "cubicle")) score += 12
            if (graph.interaction.isNotBlank() && containsAny(lower, "raven", "selected", "chose", "touched", "scroll")) score += 10
            if (isNotificationHeavy(text, phone) && !notificationIsSubject(c, graph)) score -= 24
            if (!mentionsSubject(text, c, graph) && graph.subject.length >= 4 && candidate.source in setOf("SERIES", "METAMAX", "PLOT", "SCRIPT")) score -= 14
        } else {
            if (phone.mediaHot && mentions(text, phone.mediaTitle.orEmpty())) score += 22
            if (!phone.notificationSource.isNullOrBlank() && mentions(text, phone.notificationSource.orEmpty())) score += 8
        }

        if (phone.mediaHot && mentions(text, phone.mediaTitle.orEmpty())) score += 14
        if (c.returningSubject && containsAny(lower, "back", "returned", "again", "callback", "continuity", "history")) score += 10
        if (direction.beat == "META" && containsAny(lower, "office", "screen", "rectangle", "fourth wall", "meeting", "goblin")) score += 8

        score += when (candidate.source) {
            "CHAT_SITCOM" -> 20
            "VAULT" -> 16
            "SITCOM" -> 12
            "CONTEXT" -> 10
            "VIEWPORT" -> 7
            "SERIES" -> 5
            "METAMAX" -> 4
            "PLOT" -> 3
            "SCRIPT" -> 2
            else -> 0
        }

        if (text.length in 58..330) score += 5
        if (text.count { it == '.' || it == '!' || it == '?' } in 1..3) score += 3
        if (containsAny(lower, "i ", "we ", "you ", "nobody", "somebody", "apparently", "fine.", "great.", "oh,")) score += 4

        val stiff = listOf(
            "callback privileges unlocked",
            "third media move",
            "occurrence ",
            "gold phase=",
            "phase=",
            "reset point reached",
            "keep the move reversible",
            "compress toward the decision",
            "shipped result",
            "foreground reassigned",
            "resident systems nominal",
            "current meta commentary",
            "structural bit",
            "one evidence angle per speaker",
        )
        for (phrase in stiff) if (phrase in lower) score -= 32
        if (lower.startsWith("occurrence") || lower.startsWith("state read:") || lower.startsWith("series briefing:")) score -= 12
        if (lower.count { it == ':' } >= 3) score -= 8
        return score
    }

    private fun mentionsSubject(text: String, c: RavenDialogueContextOS.ContextPacket, graph: RavenSceneGraphOS.Graph): Boolean =
        listOf(c.focus, graph.subject, graph.selected, graph.title, c.topicLabel)
            .any { anchor -> mentions(text, anchor) }

    private fun notificationIsSubject(c: RavenDialogueContextOS.ContextPacket, graph: RavenSceneGraphOS.Graph): Boolean {
        val all = "${c.topic}|${c.topicLabel}|${graph.subject}|${graph.selectedClass}".lowercase()
        return "notification" in all || "message" in all || "communication" in all
    }

    private fun isNotificationHeavy(text: String, phone: RavenPhoneSceneOS.Scene): Boolean {
        val lower = text.lowercase()
        val source = phone.notificationSource.orEmpty().lowercase()
        return containsAny(lower, "notification", "ping", "knocked", "tray", "shade", "backstage") ||
            (source.length >= 3 && source in lower)
    }

    private fun mentions(text: String, anchor: String): Boolean {
        val a = anchor.replace(Regex("\\s+"), " ").trim().lowercase()
        return a.length >= 3 && text.lowercase().contains(a)
    }

    private fun containsAny(text: String, vararg needles: String): Boolean = needles.any(text::contains)
    private fun normalize(text: String): String = text.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun stableHash(seed: String): Int {
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return hash and Int.MAX_VALUE
    }
}
