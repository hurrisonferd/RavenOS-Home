package com.iappyx.launcher.ravenos

import android.content.Context
import java.util.ArrayDeque

/**
 * Process-session script memory for Follow-Me Office.
 *
 * Stores only bounded derived scene state: app/task/short subject, motifs, callbacks and transient
 * interruptions. It does not retain screenshots, raw OCR frames, editable values, or a transcript.
 * The goal is continuity: who owns the scene, what changed, what returned, and which running bits
 * have actually earned another callback.
 */
object RavenEpisodeScriptOS {
    data class Cue(
        val act: Int,
        val sceneOwner: String,
        val task: String,
        val subject: String,
        val previousOwner: String,
        val previousSubject: String,
        val sceneChanged: Boolean,
        val returned: Boolean,
        val interruption: String,
        val motif: String,
        val motifCount: Int,
        val dwell: Int,
        val continuity: String,
        val callback: String,
    ) {
        val meaningful: Boolean get() = sceneOwner.isNotBlank() || subject.isNotBlank() || interruption.isNotBlank()
        val callbackEarned: Boolean get() = motifCount in setOf(3, 5, 8, 13, 21)
    }

    private data class Scene(val owner: String, val task: String, val subject: String, val signature: String)

    private val history = ArrayDeque<Scene>()
    private val motifCounts = linkedMapOf<String, Int>()
    private var act = 0
    private var current = Scene("", "", "", "")
    private var dwell = 0
    private const val MAX_HISTORY = 8
    private const val MAX_MOTIFS = 32

    @Synchronized
    fun observe(
        context: Context,
        screen: RavenScreenContextOS.Snapshot,
        marker: RavenMarkerBus.Marker,
        narrative: RavenSessionNarrativeOS.Narrative,
    ): Cue {
        val viewport = RavenViewportSemanticsOS.latest(context, marker.at)
        val owner = screen.appLabel.orEmpty().ifBlank {
            viewport?.packageName?.substringAfterLast('.')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }.orEmpty()
        }.ifBlank { current.owner }
        val task = viewport?.task.orEmpty().ifBlank {
            when {
                screen.keyboardLike -> "TYPING"
                screen.semanticKind == "MUSIC" -> "LISTENING"
                screen.semanticKind == "VIDEO" -> "WATCHING"
                screen.semanticKind == "SETTINGS" -> "CONFIGURING"
                else -> "VIEWING"
            }
        }
        val subject = screen.focus.replace(Regex("\\s+"), " ").trim().take(120).ifBlank { current.subject }
        val signature = if (screen.signature.isNotBlank()) screen.signature.take(220)
            else "$owner|$task|${subject.lowercase().take(100)}"

        val previous = current
        val sceneChanged = signature.isNotBlank() && signature != current.signature
        val returned = sceneChanged && history.any { it.signature == signature }
        if (sceneChanged) {
            if (current.signature.isNotBlank()) {
                history.addLast(current)
                while (history.size > MAX_HISTORY) history.removeFirst()
            }
            act++
            dwell = 1
            current = Scene(owner, task, subject, signature)
        } else if (signature.isNotBlank()) {
            dwell++
            current = Scene(owner, task, subject, signature)
        }

        val interruption = transientInterruption(marker, current.owner)
        val motif = motifFor(screen, marker, narrative, interruption)
        val motifCount = if (motif.isBlank()) 0 else bumpMotif(motif)
        val continuity = continuityLine(previous, current, sceneChanged, returned, interruption, dwell)
        val callback = callbackLine(motif, motifCount, current)

        return Cue(
            act = act.coerceAtLeast(1),
            sceneOwner = current.owner,
            task = current.task,
            subject = current.subject,
            previousOwner = previous.owner,
            previousSubject = previous.subject,
            sceneChanged = sceneChanged,
            returned = returned,
            interruption = interruption,
            motif = motif,
            motifCount = motifCount,
            dwell = dwell,
            continuity = continuity,
            callback = callback,
        )
    }

    @Synchronized
    fun clear() {
        history.clear()
        motifCounts.clear()
        act = 0
        current = Scene("", "", "", "")
        dwell = 0
    }

    @Synchronized
    fun compact(): String = buildString {
        append("SCRIPT=ACT_").append(act.coerceAtLeast(1))
        if (current.owner.isNotBlank()) append(" owner=").append(current.owner)
        if (current.task.isNotBlank()) append(" task=").append(current.task)
        append(" dwell=").append(dwell)
        if (motifCounts.isNotEmpty()) {
            val top = motifCounts.entries.maxByOrNull { it.value }
            if (top != null) append(" motif=").append(top.key).append('×').append(top.value)
        }
    }

    private fun transientInterruption(marker: RavenMarkerBus.Marker, sceneOwner: String): String {
        val detail = marker.detail.lowercase()
        val signal = marker.key.uppercase()
        val packageName = Regex("(?:^|\\|)package:([^|]+)").find(marker.detail)?.groupValues?.getOrNull(1).orEmpty()
        val transient = when {
            "smartcapture" in detail || "capture" in packageName.lowercase() -> "Smart Capture"
            packageName.equals("com.android.systemui", true) || signal == "SYSTEM_UI" -> "System UI"
            "notification tray" in detail || "notification shade" in detail -> "notification shade"
            signal.startsWith("NOTIFICATION") -> "notification"
            signal.contains("WINDOW") && ("honeyboard" in detail || "inputmethod" in detail) -> "keyboard"
            else -> ""
        }
        return transient.takeIf { it.isNotBlank() && !sceneOwner.equals(it, true) }.orEmpty()
    }

    private fun motifFor(
        screen: RavenScreenContextOS.Snapshot,
        marker: RavenMarkerBus.Marker,
        narrative: RavenSessionNarrativeOS.Narrative,
        interruption: String,
    ): String {
        val all = "${screen.text} ${screen.focus} ${marker.detail}".lowercase()
        return when {
            screen.meta && ("office" in all || "goblin" in all || "ravenos" in all) -> "SELF_AWARE_OFFICE"
            interruption == "Smart Capture" && ("office" in all || "recent hauntings" in all || "goblin" in all) -> "SELF_REVIEW_SCREENSHOT"
            narrative.id == "SOUNDTRACK_MONTAGE" -> "SOUNDTRACK_MONTAGE"
            screen.semanticKind == "MUSIC" -> "MUSIC_ROOM"
            screen.semanticKind == "CHATGPT" && screen.meta -> "CHATGPT_SELF_DEBUG"
            "callback privileges unlocked" in all -> "CALLBACK_ABOUT_CALLBACKS"
            interruption.isNotBlank() -> "CAMEO_${interruption.uppercase().replace(' ', '_')}"
            else -> ""
        }
    }

    private fun bumpMotif(motif: String): Int {
        val count = (motifCounts[motif] ?: 0) + 1
        motifCounts.remove(motif)
        motifCounts[motif] = count.coerceAtMost(99)
        while (motifCounts.size > MAX_MOTIFS) {
            motifCounts.remove(motifCounts.entries.firstOrNull()?.key ?: break)
        }
        return count
    }

    private fun continuityLine(
        previous: Scene,
        current: Scene,
        sceneChanged: Boolean,
        returned: Boolean,
        interruption: String,
        dwell: Int,
    ): String = when {
        interruption.isNotBlank() && current.owner.isNotBlank() -> "${current.owner} still owns the scene; $interruption is a cameo."
        returned && current.owner.isNotBlank() -> "Back to ${current.owner}; this scene has history now."
        sceneChanged && previous.owner.isNotBlank() && current.owner.isNotBlank() && previous.owner != current.owner ->
            "Scene cut: ${previous.owner} → ${current.owner}."
        dwell >= 5 && current.owner.isNotBlank() -> "${current.owner} has held the room for $dwell beats."
        else -> ""
    }

    private fun callbackLine(motif: String, count: Int, current: Scene): String {
        if (motif.isBlank() || count !in setOf(3, 5, 8, 13, 21)) return ""
        return when (motif) {
            "SELF_AWARE_OFFICE" -> "The office has now caught itself being the subject $count times. Fourth-wall rent is due."
            "SELF_REVIEW_SCREENSHOT" -> "Self-review callback #$count: the haunted phone is inspecting evidence of the haunted phone."
            "SOUNDTRACK_MONTAGE" -> "Montage callback #$count: the soundtrack has survived another scene cut."
            "MUSIC_ROOM" -> "Music-room callback #$count: ${current.owner.ifBlank { "the player" }} still has the aux cord."
            "CHATGPT_SELF_DEBUG" -> "Self-debug callback #$count: ChatGPT and RavenOS are reviewing RavenOS reviewing ChatGPT."
            "CALLBACK_ABOUT_CALLBACKS" -> "Callback-about-callbacks #$count. The bit has developed administrative overhead."
            else -> "Running bit #$count earned: ${motif.lowercase().replace('_', ' ')}."
        }
    }
}
