package com.iappyx.launcher.ravenos

/** Presentation-only budget contract. No sensing or effect authority. */
object RavenOmniRvExpressionBudget {
    enum class Pressure { NOMINAL, BUSY, HOT, CRITICAL }

    data class Budget(
        val allowKaomoji: Boolean,
        val allowEmoji: Boolean,
        val allowMetaTail: Boolean,
        val allowEnsemble: Boolean,
        val maxDecorators: Int,
        val reason: String
    )

    fun forPressure(pressure: Pressure, quiet: Boolean, launcherCritical: Boolean): Budget {
        if (quiet) return Budget(false, false, false, false, 0, "quiet-mode")
        if (launcherCritical) return Budget(false, true, false, false, 1, "launcher-critical-path")
        return when (pressure) {
            Pressure.NOMINAL -> Budget(true, true, true, true, 4, "nominal")
            Pressure.BUSY -> Budget(true, true, true, false, 3, "busy-shed-ensemble")
            Pressure.HOT -> Budget(true, true, false, false, 2, "hot-shed-meta")
            Pressure.CRITICAL -> Budget(false, false, false, false, 0, "critical-core-only")
        }
    }
}
