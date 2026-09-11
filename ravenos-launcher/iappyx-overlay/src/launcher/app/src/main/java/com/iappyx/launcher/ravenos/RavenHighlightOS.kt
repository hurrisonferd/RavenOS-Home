package com.iappyx.launcher.ravenos

/** Interpretable save-worthiness score. Suggestion only; never records media itself. */
object RavenHighlightOS {
    enum class Class { NONE, CANDIDATE, STRONG, HISTORIC }
    data class Score(val value: Int, val clazz: Class, val reasons: List<String>)

    fun score(marker: RavenMarkerBus.Marker, complex: RavenComplexEventOS.Result, episode: RavenEpisodeOS.Phase): Score {
        var value = marker.salience
        val reasons = mutableListOf("marker:${marker.salience}")
        if (complex.tags.isNotEmpty()) {
            value += 4
            reasons += "complex"
        }
        if ("PAYOFF" in complex.tags) { value += 4; reasons += "callback-payoff" }
        if ("APP_SWITCH_BURST" in complex.tags) { value += 2; reasons += "switch-burst" }
        if ("LOCAL_MYTHOLOGY" in complex.tags) { value += 4; reasons += "mythology" }
        if ("HISTORIC_LANDMARK" in complex.tags) { value += 7; reasons += "landmark" }
        if (episode == RavenEpisodeOS.Phase.PAYOFF) { value += 4; reasons += "episode-payoff" }
        val clazz = when {
            value >= 21 -> Class.HISTORIC
            value >= 13 -> Class.STRONG
            value >= 8 -> Class.CANDIDATE
            else -> Class.NONE
        }
        return Score(value, clazz, reasons)
    }
}
