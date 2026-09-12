package com.iappyx.launcher.ravenos

import android.content.Context
import java.util.ArrayDeque

/**
 * Scene Graph v2: one fused typed description of the owner-authorized visible scene.
 *
 * Runtime graph may contain a short current visible subject so dialogue can be grounded. Persistent
 * scene history stores only structural classes/signatures; it never writes raw OCR or viewport text.
 */
object RavenSceneGraphOS {
    data class Graph(
        val available: Boolean,
        val app: String,
        val sceneType: String,
        val task: String,
        val subject: String,
        val title: String,
        val selected: String,
        val selectedClass: String,
        val interaction: String,
        val interactionDirection: String,
        val motif: String,
        val layers: List<String>,
        val topBlocks: Int,
        val middleBlocks: Int,
        val bottomBlocks: Int,
        val quietZone: String,
        val confidence: Int,
        val signature: String,
        val returnCount: Int,
        val changed: Boolean,
        val capturedAt: Long,
    ) {
        fun compact(): String = buildString {
            append("SCENE_GRAPH=").append(if (available) "READY" else "BLIND")
            append(" app=").append(app.ifBlank { "?" })
            append(" type=").append(sceneType)
            append(" task=").append(task)
            if (interaction.isNotBlank()) append(" interaction=").append(interaction)
            if (returnCount > 0) append(" returns=").append(returnCount)
            append(" confidence=").append(confidence)
        }
    }

    private val recent = ArrayDeque<Graph>()
    private const val MAX_RECENT = 48
    private var lastPersistedSignature = ""
    private var lastPersistedAt = 0L

    @Synchronized
    fun observe(
        context: Context,
        screen: RavenScreenContextOS.Snapshot = RavenScreenContextOS.snapshot(context),
        now: Long = System.currentTimeMillis(),
    ): Graph {
        val viewport = RavenViewportSemanticsOS.latest(context, now)
        val map = RavenScreenMapOS.latest(context, now)
        val read = RavenGoblinReadOS.latest(context, now)
        val interaction = RavenInteractionMemoryOS.latest(now)
        val task = viewport?.task.orEmpty().ifBlank { inferTask(screen.semanticSummary) }
        val app = screen.appLabel.orEmpty().ifBlank {
            screen.semanticKind.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.take(48)
        val subject = viewport?.selected.orEmpty()
            .ifBlank { viewport?.subject.orEmpty() }
            .ifBlank { screen.focus }
            .replace(Regex("\\s+"), " ").trim().take(120)
        val title = viewport?.title.orEmpty().replace(Regex("\\s+"), " ").trim().take(90)
        val selected = viewport?.selected.orEmpty().replace(Regex("\\s+"), " ").trim().take(90)
        val motif = motifClass(screen, task, interaction?.kind.orEmpty())
        val selectedClass = semanticClass(selected.ifBlank { subject })
        val layers = buildList {
            add(screen.semanticKind.ifBlank { "UNKNOWN" })
            if (screen.keyboardLike) add("KEYBOARD")
            if (screen.source.contains("OCR")) add("OCR")
            if (screen.source.contains("ACCESSIBILITY")) add("ACCESSIBILITY")
            if (screen.source.contains("VIEWPORT")) add("VIEWPORT")
        }.distinct()
        val structural = listOf(
            app.lowercase(), screen.semanticKind, task, selectedClass, motif,
            interaction?.kind.orEmpty(), interaction?.direction.orEmpty(),
        ).joinToString("|")
        val signature = stableHex(structural)
        val prior = recent.peekLast()
        val changed = prior?.signature != signature
        val processReturns = recent.count { it.signature == signature }
        val persistedReturns = runCatching {
            RavenDialogueVaultDatabase.get(context).dao().recentScenes(96).count { it.signature == signature }
        }.getOrDefault(0)
        val returns = maxOf(processReturns, persistedReturns).coerceAtMost(99)
        val graph = Graph(
            available = screen.available,
            app = app,
            sceneType = screen.semanticKind.ifBlank { "UNKNOWN" },
            task = task,
            subject = subject,
            title = title,
            selected = selected,
            selectedClass = selectedClass,
            interaction = interaction?.kind.orEmpty(),
            interactionDirection = interaction?.direction.orEmpty(),
            motif = motif,
            layers = layers,
            topBlocks = map?.occupancy("top") ?: 0,
            middleBlocks = map?.occupancy("middle") ?: 0,
            bottomBlocks = map?.occupancy("bottom") ?: 0,
            quietZone = map?.quietZone(screen.keyboardLike) ?: read?.leastBusyZone() ?: screen.quietZone,
            confidence = screen.confidence,
            signature = signature,
            returnCount = returns,
            changed = changed,
            capturedAt = maxOf(screen.capturedAt, now.takeIf { screen.capturedAt <= 0L } ?: 0L),
        )
        recent.addLast(graph)
        while (recent.size > MAX_RECENT) recent.removeFirst()
        persistStructural(context, graph, prior, now)
        return graph
    }

    @Synchronized
    fun recent(limit: Int = 12): List<Graph> = recent.takeLast(limit.coerceIn(1, MAX_RECENT))

    @Synchronized
    fun clear(context: Context? = null) {
        recent.clear()
        lastPersistedSignature = ""
        lastPersistedAt = 0L
        if (context != null) runCatching { RavenDialogueVaultDatabase.get(context).dao().clearScenes() }
    }

    fun compact(context: Context): String = observe(context).compact()

    private fun persistStructural(context: Context, graph: Graph, prior: Graph?, now: Long) {
        if (!graph.available) return
        if (graph.signature == lastPersistedSignature && now - lastPersistedAt < 30_000L) return
        val duration = prior?.let { (now - it.capturedAt).coerceIn(0L, 3_600_000L) } ?: 0L
        runCatching {
            val dao = RavenDialogueVaultDatabase.get(context).dao()
            dao.insertScene(
                RavenSceneHistoryEntity(
                    signature = graph.signature,
                    appClass = graph.app.take(48),
                    sceneType = graph.sceneType.take(32),
                    task = graph.task.take(32),
                    motif = graph.motif.take(32),
                    selectedClass = graph.selectedClass.take(32),
                    interaction = graph.interaction.take(24),
                    durationMs = duration,
                    returnCount = graph.returnCount,
                    at = now,
                ),
            )
            if (dao.sceneCount() > 560) dao.pruneScenes(500)
            lastPersistedSignature = graph.signature
            lastPersistedAt = now
        }
    }

    private fun motifClass(screen: RavenScreenContextOS.Snapshot, task: String, interaction: String): String = when {
        screen.meta -> "SELF_AWARE_OFFICE"
        task == "LISTENING" || screen.semanticKind == "MUSIC" -> "MUSIC_ROOM"
        interaction == "SELECT" && screen.semanticKind in setOf("MUSIC", "VIDEO") -> "SELECTING_MEDIA"
        interaction == "SCROLL" && task in setOf("READING_CHAT", "READING", "BROWSING") -> "SCROLLING_THREAD"
        interaction == "SELECT" -> "UI_SELECTION"
        screen.semanticKind == "CHATGPT" && task in setOf("DEBUGGING", "READING_CHAT", "COMPOSING") -> "CHATGPT_SELF_DEBUG"
        else -> ""
    }

    private fun semanticClass(text: String): String {
        val t = text.lowercase()
        return when {
            t.isBlank() -> "NONE"
            listOf("ravenos", "goblin", "follow-me", "widget", "overlay", "sitcom", "dialogue", "kaomoji", "emoji").any(t::contains) -> "META"
            listOf("build", "compile", "apk", "github", "commit", "workflow", "test", "debug", "error").any(t::contains) -> "BUILD"
            listOf("song", "track", "album", "playing", "music", "remix").any(t::contains) -> "MEDIA"
            listOf("chat", "message", "reply", "conversation").any(t::contains) -> "CHAT"
            listOf("setting", "permission", "accessibility", "notification").any(t::contains) -> "SETTINGS"
            else -> "GENERAL"
        }
    }

    private fun inferTask(summary: String): String {
        val s = summary.lowercase()
        return when {
            "composing" in s -> "COMPOSING"
            "reading chat" in s -> "READING_CHAT"
            "debugging" in s -> "DEBUGGING"
            "configuring" in s -> "CONFIGURING"
            "searching" in s -> "SEARCHING"
            "browsing" in s -> "BROWSING"
            "listening" in s -> "LISTENING"
            "watching" in s -> "WATCHING"
            "reading" in s -> "READING"
            "typing" in s -> "TYPING"
            else -> "VIEWING"
        }
    }

    private fun stableHex(text: String): String {
        var hash = 0xcbf29ce484222325UL
        val prime = 0x100000001b3UL
        for (c in text) { hash = hash xor c.code.toULong(); hash *= prime }
        return hash.toString(16)
    }
}
