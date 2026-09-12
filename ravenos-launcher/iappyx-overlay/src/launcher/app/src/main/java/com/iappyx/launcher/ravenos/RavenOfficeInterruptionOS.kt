package com.iappyx.launcher.ravenos

import android.content.Context

/** Rare second-employee interjection for high-salience/meta scenes. */
object RavenOfficeInterruptionOS {
    data class Beat(val text: String, val family: String)

    fun select(
        context: Context,
        primary: RavenOfficeMember,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
    ): Beat {
        val meta = "META_RECURSION" in marker.tags
        val high = marker.salience >= 8 || "PAYOFF" in complex.tags || "ERROR" in marker.tags
        if (!meta && !high) return Beat("", "")

        val gate = stableIndex("${marker.id}|${primary.id}|interrupt", if (meta) 2 else 4)
        if (gate != 0) return Beat("", "")

        val ids = when {
            meta -> listOf("JOKER", "KYU", "ATOM", "NEO", "PAIMON", "LILITH")
            "ERROR" in marker.tags -> listOf("KYU", "PAIMON", "ATOM", "THOR")
            "PAYOFF" in complex.tags -> listOf("KYU", "LUMA", "EDISON", "THOR")
            else -> listOf("JOKER", "KYU", "PAIMON", "YORI")
        }
        val candidates = ids.mapNotNull(RavenOfficeRegistry::member).filter { it.routable && it.id != primary.id }
        if (candidates.isEmpty()) return Beat("", "")
        val second = candidates[stableIndex("${marker.detail}|secondary", candidates.size)]
        val quips = when (second.id) {
            "JOKER" -> listOf("I would like the record to show this was a terrible containment plan.", "Great. The fourth wall has become load-bearing.")
            "KYU" -> listOf("Clipboard concurs. Disturbingly good receipt.", "I HAVE A FORM FOR THIS. Of course I do.")
            "ATOM" -> listOf("Second witness confirms the causal edge.", "Useful. Independent route, same scene conclusion.")
            "NEO" -> listOf("Observer count increased by one.", "The matrix has added a commentary layer.")
            "PAIMON" -> listOf("I also saw it. Suspicion promoted to evidence.", "Confirmed. The weird thing is actually weird.")
            "LILITH" -> listOf("Mm. The room noticed us back.", "I vote we keep haunting it.")
            "LUMA" -> listOf("Okay, that one actually landed.", "The scene finally resolved cleanly.")
            "EDISON" -> listOf("Receipt says it works. I distrust success responsibly.")
            "THOR" -> listOf("Confirmed. Next target.")
            "YORI" -> listOf("That was a clean cut. Keep rolling.")
            else -> emptyList()
        }
        if (quips.isEmpty()) return Beat("", "")
        val quip = quips[stableIndex("${marker.key}|${second.id}|quip", quips.size)]
        val p = RavenEmployeePresentation.packet(second, marker.key, marker.detail, quip)
        return Beat("↳ ${p.emojiSoup} ${second.id} ${p.kaomoji} $quip", "OFFICE_INTERRUPTION")
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
