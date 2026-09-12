package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Screen-grounded observation for Follow-Me Office.
 *
 * Observation exists only when the office actually has trustworthy screen meaning. Sensor/setup
 * diagnostics are exposed separately so a missing Goblin Eye/OCR permission can never replace the
 * resident's personality on the normal overlay again.
 */
object RavenObservationOS {
    data class Observation(val text: String, val family: String)

    fun observe(
        context: Context,
        packet: RavenDialogueContextOS.ContextPacket,
        haunt: RavenHauntMode,
    ): Observation {
        if (!packet.screenAvailable || packet.confidence < minimumConfidence(haunt)) {
            return Observation("", "OBSERVE_UNAVAILABLE")
        }

        val app = packet.app.ifBlank { "Screen" }
        val focus = compact(packet.focus, 142)
        val subject = when {
            packet.meta -> "META · $app is visibly talking about ${quote(focus)}"
            packet.returningSubject && packet.topicLabel.isNotBlank() -> "$app · back to ${packet.topicLabel}: ${quote(focus)}"
            !packet.screenChanged && packet.dwellCount >= 3 -> "$app · still on ${packet.topicLabel}: ${quote(focus)}"
            packet.topic == "DIALOGUE" -> "$app · office dialogue discussion: ${quote(focus)}"
            packet.topic == "SCREEN_AWARENESS" -> "$app · screen-awareness discussion: ${quote(focus)}"
            packet.topic == "GOBLIN_VISION" -> "$app · Goblin Vision / Follow-Me Office: ${quote(focus)}"
            packet.semanticKind == "SETTINGS" -> "$app · ${packet.semanticSummary}: ${quote(focus)}"
            packet.keyboardLike -> "$app · typing into ${packet.topicLabel}: ${quote(focus)}"
            packet.topicLabel.isNotBlank() -> "$app · ${packet.topicLabel}: ${quote(focus)}"
            else -> "$app · ${quote(focus)}"
        }

        return Observation(subject.take(190), when {
            packet.meta -> "OBSERVE_META"
            packet.returningSubject -> "OBSERVE_RETURN"
            !packet.screenChanged -> "OBSERVE_DWELL"
            else -> "OBSERVE_SCREEN"
        })
    }

    /** Explicit owner-facing sensor diagnosis. Never call this as the normal resident observation. */
    fun diagnostic(context: Context, packet: RavenDialogueContextOS.ContextPacket): Observation =
        sensorState(context, packet)

    private fun sensorState(context: Context, packet: RavenDialogueContextOS.ContextPacket): Observation {
        val eye = RavenScreenWatchService.isActive(context)
        val ocr = RavenGoblinReadOS.isEnabled(context)
        val access = RavenAccessibilityReadOS.isEnabled(context)
        val line = when {
            ocr && !eye && access -> "👁 Goblin Read ON · Goblin Eye needs re-arming · Accessibility Read waiting"
            ocr && !eye -> "👁 Goblin Read ON · Goblin Eye needs re-arming after install/restart"
            eye && ocr && access -> "👁 Screen brain armed · waiting for readable OCR / Accessibility text"
            eye && ocr -> "👁 Goblin Eye + Read armed · waiting for readable OCR text"
            access -> "🧭 Accessibility Read armed · waiting for Android-visible screen semantics"
            eye -> "👁 Goblin Eye is watching motion · Goblin Read OFF"
            else -> "👁 Follow-Me alive · Goblin Eye / Read not armed"
        }
        val suffix = if (packet.confidence in 1..minimumConfidence(RavenHauntMode.CALM)) {
            " · confidence ${packet.confidence}"
        } else ""
        return Observation((line + suffix).take(190), "OBSERVE_DIAGNOSTIC")
    }

    private fun minimumConfidence(haunt: RavenHauntMode): Int = when (haunt) {
        RavenHauntMode.CALM -> 78
        RavenHauntMode.LIVED_IN -> 70
        RavenHauntMode.HAUNTED -> 64
        RavenHauntMode.FERAL -> 60
        RavenHauntMode.APOCALYPSE -> 58
    }

    private fun quote(text: String): String = if (text.isBlank()) "the current screen" else "“${compact(text, 132)}”"

    private fun compact(text: String, max: Int): String = text
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(max)
}
