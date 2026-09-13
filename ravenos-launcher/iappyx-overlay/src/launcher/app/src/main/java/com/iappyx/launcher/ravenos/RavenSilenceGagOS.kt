package com.iappyx.launcher.ravenos

/** Deterministic non-verbal reactions. Silence is presentation, never evidence. */
object RavenSilenceGagOS {
    data class Silence(val emit: Boolean, val token: String, val reason: String)

    fun evaluate(event: String, occurrence: Int, noveltyLow: Boolean, quiet: Boolean, seed: Int): Silence {
        if (quiet) return Silence(true, "", "quiet-mode")
        if (!noveltyLow || occurrence < 2) return Silence(false, "", "fresh-event")
        val options = listOf("…", "|ω･)", "(¬_¬)", "(._.)", "📋", "☕", "")
        val token = options[Math.floorMod(seed, options.size)]
        return Silence(true, token, "low-novelty-deliberate-non-reaction")
    }
}
