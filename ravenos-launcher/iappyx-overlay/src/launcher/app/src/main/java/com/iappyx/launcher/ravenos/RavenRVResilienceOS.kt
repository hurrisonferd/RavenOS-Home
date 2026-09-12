package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Omni-RV-inspired health mesh for Follow-Me Office.
 *
 * This is observation/routing only: no permission is granted, no MediaProjection is re-armed, and
 * no Android setting is changed here. It tells the cockpit which senses are alive and when RavenOS
 * should deliberately operate in PHONE_LIMP_HOME rather than pretend the screen is readable.
 */
object RavenRVResilienceOS {
    data class Pulse(
        val mode: String,
        val score: Int,
        val overlayReady: Boolean,
        val semanticFresh: Boolean,
        val viewportFresh: Boolean,
        val ocrFresh: Boolean,
        val interactionFresh: Boolean,
        val mediaReady: Boolean,
        val shadeReady: Boolean,
        val faults: List<String>,
        val at: Long,
    ) {
        val limpHome: Boolean get() = mode == "PHONE_LIMP_HOME"
        fun compact(): String = buildString {
            append("RV_MESH=").append(mode).append(' ').append(score).append('%')
            append(" · VIEWPORT=").append(flag(viewportFresh))
            append(" · SEMANTIC=").append(flag(semanticFresh))
            append(" · OCR=").append(flag(ocrFresh))
            append(" · ACTION=").append(flag(interactionFresh))
            append(" · MEDIA=").append(flag(mediaReady))
            append(" · SHADE=").append(flag(shadeReady))
            if (faults.isNotEmpty()) append(" · FAULTS=").append(faults.take(4).joinToString(","))
        }
        private fun flag(value: Boolean) = if (value) "HOT" else "COLD"
    }

    fun snapshot(context: Context, now: Long = System.currentTimeMillis()): Pulse {
        val app = context.applicationContext
        val readiness = RavenAwarenessStatus.snapshot(app)
        val access = RavenAccessibilityReadOS.latest(app, now)
        val viewport = RavenViewportSemanticsOS.latest(app, now)
        val ocr = RavenGoblinReadOS.latest(app, now)
        val interaction = RavenInteractionMemoryOS.latest(now)
        val semanticFresh = access != null && now - access.capturedAt <= 12_000L
        val viewportFresh = viewport != null && now - viewport.capturedAt <= 12_000L
        val ocrFresh = ocr != null && now - ocr.capturedAt <= 12_000L
        val interactionFresh = interaction != null && now - interaction.at <= 8_000L
        val overlayReady = readiness.overlayAccess && readiness.followMeEnabled
        val faults = buildList {
            if (!readiness.overlayAccess) add("OVERLAY_PERMISSION")
            if (!readiness.followMeEnabled) add("FOLLOW_ME_OFF")
            if (!readiness.foregroundAwareness) add("ACCESS_SERVICE_OFF")
            if (RavenAccessibilityReadOS.isEnabled(app) && !semanticFresh) add("SEMANTIC_STALE")
            if (RavenGoblinReadOS.isEnabled(app) && !readiness.goblinEyeActive) add("EYE_NEEDS_ARM")
            if (RavenGoblinReadOS.isEnabled(app) && readiness.goblinEyeActive && !ocrFresh) add("OCR_COLD")
            if (!readiness.notificationAwareness) add("SHADE_OFF")
            if (!readiness.mediaSessionReady) add("MEDIA_OFF")
        }
        val mode = when {
            !overlayReady -> "COCKPIT_OFFLINE"
            viewportFresh && semanticFresh && ocrFresh -> "FULL_VISION"
            viewportFresh && semanticFresh -> "SEMANTIC_VISION"
            semanticFresh && ocrFresh -> "HYBRID_VISION"
            semanticFresh -> "SEMANTIC_ONLY"
            ocrFresh -> "OCR_ONLY"
            else -> "PHONE_LIMP_HOME"
        }
        val score = listOf(
            overlayReady to 18,
            readiness.foregroundAwareness to 12,
            viewportFresh to 18,
            semanticFresh to 16,
            ocrFresh to 14,
            interactionFresh to 8,
            readiness.mediaSessionReady to 7,
            readiness.notificationAwareness to 7,
        ).sumOf { (alive, weight) -> if (alive) weight else 0 }.coerceIn(0, 100)
        return Pulse(
            mode = mode,
            score = score,
            overlayReady = overlayReady,
            semanticFresh = semanticFresh,
            viewportFresh = viewportFresh,
            ocrFresh = ocrFresh,
            interactionFresh = interactionFresh,
            mediaReady = readiness.mediaSessionReady,
            shadeReady = readiness.notificationAwareness,
            faults = faults,
            at = now,
        )
    }
}
