package com.iappyx.launcher.ravenos

import android.content.Context

/** Recognizes bounded session-level activities from recent phone facts, not transcripts. */
object RavenSessionNarrativeOS {
    data class Narrative(val id: String, val label: String, val changed: Boolean, val count: Int)
    data class Beat(val text: String, val family: String)

    private const val PREFS = "ravenos_session_narrative_v1"
    private const val KEY_ID = "id"
    private const val KEY_COUNT = "count"

    fun observe(context: Context, marker: RavenMarkerBus.Marker): Narrative {
        val app = context.applicationContext
        val scene = RavenPhoneSceneOS.snapshot(app, marker.at)
        val recent = RavenMarkerBus.recent(app, 18)
        val visible = RavenAccessibilityReadOS.latest(app, marker.at)?.text
            ?: RavenGoblinReadOS.latest(app, marker.at)?.text
        val pkg = field(marker.detail, "package")
        val semantic = RavenAppSemanticsOS.interpret(app, pkg, scene.activeApp, visible, RavenAccessibilityReadOS.latest(app, marker.at)?.keyboardLike == true)

        val boundaryHits = recent.count { "BOUNDARY" in it.tags || it.detail.contains("settings", true) || it.detail.contains("permission", true) }
        val communicationHits = recent.count { "COMMUNICATION" in it.tags }
        val metaHits = recent.count { "META_RECURSION" in it.tags }
        val id = when {
            metaHits >= 2 || (RavenMetaRecursionOS.detect(visible.orEmpty()) && semantic.kind == "CHATGPT") -> "RAVENOS_SELF_DEBUG"
            boundaryHits >= 4 || (semantic.kind == "SETTINGS" && boundaryHits >= 2) -> "PERMISSION_PILGRIMAGE"
            scene.mediaHot && scene.recentSwitches >= 5 -> "SOUNDTRACK_MONTAGE"
            scene.recentSwitches >= 7 -> "APP_SPEEDRUN"
            semantic.kind == "CHATGPT" -> "CHATGPT_SESSION"
            communicationHits >= 4 || semantic.kind in setOf("MESSAGING", "MAIL") -> "COMMUNICATION_RUN"
            semantic.kind == "MUSIC" || (scene.mediaHot && scene.recentSwitches <= 2) -> "MUSIC_SESSION"
            else -> "GENERAL_PHONE"
        }
        val label = when (id) {
            "RAVENOS_SELF_DEBUG" -> "RavenOS self-debug recursion"
            "PERMISSION_PILGRIMAGE" -> "Android permission pilgrimage"
            "SOUNDTRACK_MONTAGE" -> "cross-app soundtrack montage"
            "APP_SPEEDRUN" -> "app-switch speedrun"
            "CHATGPT_SESSION" -> "ChatGPT session"
            "COMMUNICATION_RUN" -> "communication run"
            "MUSIC_SESSION" -> "music session"
            else -> "general phone session"
        }
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_ID, "").orEmpty()
        val changed = previous.isNotBlank() && previous != id
        val count = if (previous == id) prefs.getInt(KEY_COUNT, 0) + 1 else 1
        prefs.edit().putString(KEY_ID, id).putInt(KEY_COUNT, count).apply()
        return Narrative(id, label, changed, count)
    }

    fun beat(member: RavenOfficeMember, narrative: Narrative, marker: RavenMarkerBus.Marker): Beat {
        if (!narrative.changed || narrative.id == "GENERAL_PHONE") return Beat("", "")
        val seed = "${member.id}|${narrative.id}|${marker.key}|narrative-v1"
        val lines = when (narrative.id) {
            "RAVENOS_SELF_DEBUG" -> listOf(
                "Session arc updated: RavenOS is now debugging RavenOS while floating over the debugging conversation.",
                "We have entered the self-debug chapter. The office is both subject and witness.",
            )
            "PERMISSION_PILGRIMAGE" -> listOf(
                "This has become an Android permission pilgrimage. Settings may charge rent soon.",
                "Session arc: permissions, settings, permissions again. Ancient Samsung ritual confirmed.",
            )
            "SOUNDTRACK_MONTAGE" -> listOf(
                "Session arc upgraded to montage: apps keep changing and the soundtrack refuses to leave.",
                "We are officially in a cross-app music montage now.",
            )
            "APP_SPEEDRUN" -> listOf(
                "Session arc: app-switch speedrun. Foreground ownership is now a temporary position.",
                "The phone has entered competitive context switching.",
            )
            "CHATGPT_SESSION" -> listOf(
                "Session arc: ChatGPT has the room now. The office has joined the meeting uninvited.",
                "We have apparently moved the board meeting into ChatGPT.",
            )
            "COMMUNICATION_RUN" -> listOf(
                "Session arc: communication run. Several rectangles would like a word.",
                "Messaging phase detected. The phone has become a small switchboard.",
            )
            "MUSIC_SESSION" -> listOf(
                "Session arc: music has taken over scene management.",
                "The phone has entered soundtrack-first mode.",
            )
            else -> emptyList()
        }
        return if (lines.isEmpty()) Beat("", "") else Beat(pick(seed, lines), "SESSION_NARRATIVE")
    }

    private fun field(detail: String, name: String): String? = Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
        .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun pick(seed: String, options: List<String>): String {
        var hash = 0x811C9DC5.toInt()
        for (c in seed) { hash = hash xor c.code; hash *= 16777619 }
        return options[(hash and Int.MAX_VALUE) % options.size]
    }
}
