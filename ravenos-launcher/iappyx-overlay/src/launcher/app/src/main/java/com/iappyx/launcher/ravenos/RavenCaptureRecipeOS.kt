package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Typed evidence/capture work orders. This module never silently starts screen capture.
 * Pixel recipes remain blocked until Raven explicitly grants a future MediaProjection session.
 */
object RavenCaptureRecipeOS {
    enum class Recipe {
        EVIDENCE_CARD,
        REPLAY_EVIDENCE,
        PIN_REGION,
        DIAGNOSTIC_SNAPSHOT,
        CLIP_RECIPE,
    }

    data class Result(val status: String, val message: String, val effectAuthority: String = "NONE")

    fun execute(context: Context, recipe: Recipe): Result {
        return when (recipe) {
            Recipe.EVIDENCE_CARD -> Result("PASS", RavenEvidenceBoard.why(context))
            Recipe.REPLAY_EVIDENCE -> Result("PASS", RavenReplayOS.compact(context, 8))
            Recipe.CLIP_RECIPE -> {
                val last = RavenEvidenceBoard.last(context)
                if (last == null) {
                    Result("EMPTY", "CLIP_RECIPE: no receipted semantic event")
                } else {
                    Result(
                        "READY",
                        "CLIP_RECIPE marker=${last.optString("markerId")} highlight=${last.optString("highlight")} score=${last.optInt("highlightScore", 0)} · owner replay request required",
                    )
                }
            }
            Recipe.PIN_REGION, Recipe.DIAGNOSTIC_SNAPSHOT -> Result(
                "REQUIRES_OWNER_CAPTURE_SESSION",
                "${recipe.name}: pixel capture is not armed. Raven must explicitly grant a user-visible screen-capture session first.",
            )
        }
    }
}
