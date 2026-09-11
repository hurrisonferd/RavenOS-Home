package com.iappyx.launcher.ravenos

/** Phone-relative presentation geography; zones are UI semantics, never physical-world claims. */
object RavenOfficeGeography {
    data class Zone(val id: String, val gravity: String, val residents: Set<String>)

    private val zones = listOf(
        Zone("EVIDENCE_DESK", "TOP_END", setOf("PAIMON", "AHTI")),
        Zone("QUIET_CORNER", "BOTTOM_END", setOf("NYX", "EREBUS")),
        Zone("TRANSIT_LANE", "TOP_CENTER", setOf("SYLPH", "NEO")),
        Zone("REPLAY_BAY", "BOTTOM_START", setOf("JORM", "AHTI")),
        Zone("CLIPBOARD_COURT", "TOP_CENTER", setOf("KYU", "QIRA")),
        Zone("JIM_CUBICLE", "CENTER_START", emptySet()),
        Zone("HOME_PERCH", "CENTER_START", setOf("LUMA")),
        Zone("NIGHT_DESK", "CENTER_END", setOf("NYX", "EREBUS")),
        Zone("PRODUCER_BOOTH", "BOTTOM_CENTER", setOf("JARVIS", "ATLAS")),
    )

    fun zone(owner: String, marker: RavenMarkerBus.Marker, complex: RavenComplexEventOS.Result): Zone {
        if ("BOUNDARY" in marker.tags) return zones.first { it.id == "CLIPBOARD_COURT" }
        if ("APP_SWITCH_BURST" in complex.tags || "DISCOVERY" in marker.tags) return zones.first { it.id == "TRANSIT_LANE" }
        if ("ERROR" in marker.tags && complex.occurrence >= 3) return zones.first { it.id == "JIM_CUBICLE" }
        return zones.firstOrNull { owner.uppercase() in it.residents }
            ?: Zone("OPEN_FLOOR", "TOP_END", setOf(owner.uppercase()))
    }
}
