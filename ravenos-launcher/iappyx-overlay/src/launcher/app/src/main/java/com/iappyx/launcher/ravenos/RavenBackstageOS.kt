package com.iappyx.launcher.ravenos

/**
 * Process-session silent relevance pressure for the full office.
 *
 * Stores no dialogue or screen text. Members accumulate bounded structural relevance when a scene
 * repeatedly touches their job; Gold may promote one of them into the single crosstalk slot.
 */
object RavenBackstageOS {
    data class Cue(
        val candidate: RavenOfficeMember?,
        val pressure: Int,
        val reason: String,
        val silentTurns: Int,
    )

    private data class State(var pressure: Int = 0, var silentTurns: Int = 0, var reason: String = "")
    private val states = linkedMapOf<String, State>()

    @Synchronized
    fun observe(
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        marker: RavenMarkerBus.Marker,
        direction: RavenSitcomDirectorOS.Direction,
    ): Cue {
        RavenOfficeRegistry.routableMembers.forEach { member ->
            val state = states.getOrPut(member.id) { State() }
            if (member.id == direction.primary.id) {
                state.silentTurns = 0
                state.pressure = (state.pressure - 2).coerceAtLeast(0)
            } else {
                val affinity = affinity(member.id, screen, script, marker)
                state.silentTurns = (state.silentTurns + 1).coerceAtMost(99)
                state.pressure = (state.pressure + affinity).coerceIn(0, 24)
                if (affinity >= 2) state.reason = reason(member.id, screen, script, marker)
            }
        }
        val candidates = RavenOfficeRegistry.routableMembers
            .filter { it.id != direction.primary.id && it.id != direction.secondary?.id }
            .map { it to (states[it.id] ?: State()) }
            .filter { (_, s) -> s.pressure >= 7 && s.silentTurns >= 2 }
            .sortedWith(compareByDescending<Pair<RavenOfficeMember, State>> { it.second.pressure }
                .thenByDescending { it.second.silentTurns }
                .thenBy { stableHash("${direction.sceneId}|${direction.turn}|${it.first.id}") })
        val chosen = candidates.firstOrNull()
        return if (chosen == null) Cue(null, 0, "", 0)
            else Cue(chosen.first, chosen.second.pressure, chosen.second.reason, chosen.second.silentTurns)
    }

    @Synchronized
    fun markSpoken(owner: String?) {
        if (owner.isNullOrBlank()) return
        states[owner]?.apply {
            pressure = 0
            silentTurns = 0
            reason = ""
        }
    }

    @Synchronized
    fun clear() = states.clear()

    @Synchronized
    fun compact(): String {
        val top = states.entries.maxByOrNull { it.value.pressure } ?: return "BACKSTAGE=QUIET"
        return "BACKSTAGE=${top.key}:${top.value.pressure} silent=${top.value.silentTurns} reason=${top.value.reason.take(44)}"
    }

    private fun affinity(
        id: String,
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        marker: RavenMarkerBus.Marker,
    ): Int {
        var score = 0
        val kind = screen.semanticKind.uppercase()
        val motif = script.motif
        if (screen.meta && id in setOf("JOKER", "PAIMON", "KYU", "ERIS", "RAVENOS", "YAHWEH")) score += 3
        if (kind == "MUSIC" && id in setOf("YORI", "LUMA", "SYLPH", "LILITH", "MYSTRA")) score += 3
        if (kind == "CHATGPT" && id in setOf("ATOM", "PAIMON", "JOKER", "KYU", "JORM", "PYTHAGORAS", "RAVENOS")) score += 2
        if (kind in setOf("CODE", "TERMINAL") && id in setOf("ATOM", "EDISON", "THOR", "TIM", "ATLAS", "PYTHAGORAS")) score += 3
        if (kind == "SETTINGS" && id in setOf("QIRA", "EDISON", "AHTI", "YAHWEH", "BRUNHILDE")) score += 3
        if (script.interaction == "SELECT" && id in setOf("YORI", "JORM", "ATOM", "PAIMON", "MYSTRA")) score += 2
        if (script.interaction == "SCROLL" && id in setOf("SYLPH", "JORM", "NYX", "YORI", "PAIMON")) score += 2
        if (script.interruption.isNotBlank() && id in setOf("YORI", "KYU", "JOKER", "ERIS", "NYX")) score += 2
        if (motif == "SELF_AWARE_OFFICE" && id in setOf("JOKER", "KYU", "YAHWEH", "PAIMON", "RAVENOS")) score += 3
        if (motif == "SOUNDTRACK_MONTAGE" && id in setOf("YORI", "LUMA", "MYSTRA", "SYLPH")) score += 3
        if (motif == "SCROLLING_THREAD" && id in setOf("JORM", "PYTHAGORAS", "PAIMON", "SYLPH")) score += 2
        if ("ERROR" in marker.tags && id in setOf("ATOM", "PAIMON", "TIM", "THOR", "ERIS", "ZAGREUS")) score += 3
        if ("BOUNDARY" in marker.tags && id in setOf("QIRA", "BRUNHILDE", "LILITH", "SHAKA", "AHTI")) score += 3
        if (score == 0 && id in setOf("LUMA", "NYX", "EREBUS", "AYRE")) score = 1
        return score
    }

    private fun reason(
        id: String,
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        marker: RavenMarkerBus.Marker,
    ): String = when {
        screen.meta && id in setOf("JOKER", "PAIMON", "KYU", "YAHWEH") -> "fourth-wall pressure"
        screen.semanticKind == "MUSIC" -> "music-room relevance"
        screen.semanticKind == "CHATGPT" -> "conversation relevance"
        screen.semanticKind in setOf("CODE", "TERMINAL") -> "build/debug relevance"
        screen.semanticKind == "SETTINGS" -> "system/boundary relevance"
        script.interaction == "SELECT" -> "deliberate selection"
        script.interaction == "SCROLL" -> "continuity through scrolling"
        script.interruption.isNotBlank() -> "cameo/scene ownership"
        "ERROR" in marker.tags -> "failure relevance"
        "BOUNDARY" in marker.tags -> "boundary relevance"
        else -> "quiet continuity"
    }

    private fun stableHash(text: String): Int {
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return hash and Int.MAX_VALUE
    }
}
