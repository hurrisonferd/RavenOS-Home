package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Shared semantic packet for every dialogue organ.
 *
 * Character banks react to what is on the glass, not reconstruct screen meaning from a package/window
 * callback. The semantic summary may include the viewport task inferred from visible non-editable UI.
 */
object RavenDialogueContextOS {
    data class ContextPacket(
        val ownerId: String,
        val app: String,
        val semanticKind: String,
        val semanticSummary: String,
        val focus: String,
        val previousFocus: String,
        val previousApp: String,
        val topic: String,
        val topicLabel: String,
        val meta: Boolean,
        val keyboardLike: Boolean,
        val screenAvailable: Boolean,
        val screenChanged: Boolean,
        val returningSubject: Boolean,
        val dwellCount: Int,
        val returnCount: Int,
        val screenSource: String,
        val confidence: Int,
        val quietZone: String,
        val callbackText: String,
        val callbackFamily: String,
        val narrativeId: String,
        val narrativeLabel: String,
        val narrativeChanged: Boolean,
        val signal: String,
        val occurrence: Int,
        val tags: Set<String>,
        val episode: String,
        val recentSubjects: List<String>,
    )

    fun compose(
        context: Context,
        member: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        episode: RavenEpisodeOS.Phase,
        screen: RavenScreenContextOS.Snapshot,
        callback: RavenCallbackMemoryOS.Callback,
        narrative: RavenSessionNarrativeOS.Narrative,
    ): ContextPacket {
        val memory = RavenScreenMemoryOS.observe(screen)
        val topic = classifyTopic(screen, marker, narrative)
        return ContextPacket(
            ownerId = member.id,
            app = screen.appLabel.orEmpty().ifBlank { screen.semanticKind.lowercase().replaceFirstChar { it.titlecase() } },
            semanticKind = screen.semanticKind,
            semanticSummary = screen.semanticSummary,
            focus = screen.focus,
            previousFocus = memory.previousFocus,
            previousApp = memory.previousApp,
            topic = topic.first,
            topicLabel = topic.second,
            meta = screen.meta,
            keyboardLike = screen.keyboardLike,
            screenAvailable = screen.available,
            screenChanged = memory.changed,
            returningSubject = memory.returning,
            dwellCount = memory.dwellCount,
            returnCount = memory.returnCount,
            screenSource = screen.source,
            confidence = screen.confidence,
            quietZone = screen.quietZone,
            callbackText = callback.text,
            callbackFamily = callback.family,
            narrativeId = narrative.id,
            narrativeLabel = narrative.label,
            narrativeChanged = narrative.changed,
            signal = marker.key,
            occurrence = complex.occurrence,
            tags = marker.tags + complex.tags,
            episode = episode.name,
            recentSubjects = memory.recentSubjects,
        )
    }

    private fun classifyTopic(
        screen: RavenScreenContextOS.Snapshot,
        marker: RavenMarkerBus.Marker,
        narrative: RavenSessionNarrativeOS.Narrative,
    ): Pair<String, String> {
        val text = "${screen.focus} ${screen.text} ${screen.semanticSummary} ${narrative.label}".lowercase()
        fun has(vararg words: String) = words.any(text::contains)
        val task = taskLabel(screen.semanticSummary)
        fun withTask(base: String): String = if (task.isBlank() || base.contains(task, true)) base else "$base · $task"
        return when {
            has("goblin vision", "meta goblin", "follow-me office", "follow me office", "follow-me", "ravenos launcher") ->
                "GOBLIN_VISION" to withTask("Goblin Vision / Follow-Me Office")
            has("dialogue bank", "dialogue", "statements", "talking", "speech", "commentary", "too rapid", "too quiet") ->
                "DIALOGUE" to withTask("office dialogue behavior")
            has("screen awareness", "screen itself", "actual context", "ocr", "accessibility read", "visible text", "screen context", "viewport") ->
                "SCREEN_AWARENESS" to withTask("screen awareness")
            has("permission", "accessibility", "appear on top", "overlay permission", "notification access") ->
                "PERMISSIONS" to withTask("Android permissions")
            has("commit", "compile", "apk", "kotlin", "github", "branch", "source", "build") || "BUILD" in marker.tags ->
                "BUILD" to withTask("RavenOS build work")
            screen.meta || narrative.id == "RAVENOS_SELF_DEBUG" ->
                "META_RECURSION" to withTask("RavenOS self-reference")
            screen.semanticKind == "MUSIC" || has("music", "track", "song", "album") ->
                "MUSIC" to withTask("music")
            screen.semanticKind == "VIDEO" -> "VIDEO" to withTask("video")
            screen.semanticKind == "CHATGPT" -> "CHATGPT" to withTask("ChatGPT conversation")
            screen.semanticKind == "BROWSER" -> "BROWSER" to withTask(screen.semanticSummary.ifBlank { "web page" })
            screen.semanticKind == "COMMUNITY" -> "COMMUNITY" to withTask(screen.semanticSummary.ifBlank { "community thread" })
            screen.semanticKind == "CODE" -> "CODE" to withTask(screen.semanticSummary.ifBlank { "source / CI" })
            screen.semanticKind == "TERMINAL" -> "TERMINAL" to withTask(screen.semanticSummary.ifBlank { "terminal" })
            screen.semanticKind == "SETTINGS" -> "SETTINGS" to withTask(screen.semanticSummary)
            screen.semanticKind in setOf("MAIL", "MESSAGING") -> "COMMUNICATION" to withTask("communication")
            screen.semanticKind == "FILES" -> "FILES" to withTask("files")
            screen.semanticKind == "GALLERY" -> "GALLERY" to withTask("gallery")
            screen.semanticKind == "CAMERA" -> "CAMERA" to withTask("camera")
            else -> screen.semanticKind to withTask(screen.semanticSummary.ifBlank { "current screen" })
        }
    }

    private fun taskLabel(summary: String): String {
        val s = summary.lowercase()
        return when {
            "composing" in s -> "composing"
            "reading chat" in s -> "reading chat"
            "debugging" in s -> "debugging"
            "configuring" in s -> "configuring"
            "searching" in s -> "searching"
            "browsing" in s -> "browsing"
            "listening" in s -> "listening"
            "watching" in s -> "watching"
            "reading" in s -> "reading"
            "typing" in s -> "typing"
            else -> ""
        }
    }
}
