package com.iappyx.launcher.ravenos

/**
 * Launcher-side projection of EgoOS identity principles.
 *
 * Canonical EgoOS remains authoritative. This class does not mutate ISO identity or import private
 * memory. It derives presentation/casting hints from the office registry, current performance
 * history, and canonical role anchors already shipped in RavenOS Launcher.
 */
object RavenEgoReserveProjectionOS {
    data class Reserve(
        val owner: String,
        val state: String,
        val purposeAnchor: String,
        val valueAnchors: List<String>,
        val favoredForms: List<String>,
        val tensionForms: List<String>,
        val cadence: String,
        val evolutionTag: String,
    )

    fun project(member: RavenOfficeMember, memory: RavenOfficeSeasonOS.Memory): Reserve {
        val id = member.id
        val favored = favoredForms(id)
        val tension = tensionForms(id)
        val cadence = cadence(id)
        val evolution = when (memory.memberState) {
            "EVOLVING" -> "🧬✨"
            "GROWING" -> "🌱🎭"
            "CHALLENGED" -> "⚠️🧠"
            else -> "⚓🎭"
        }
        return Reserve(
            owner = id,
            state = memory.memberState,
            purposeAnchor = member.lane,
            valueAnchors = member.signatureNotes.take(3),
            favoredForms = favored,
            tensionForms = tension,
            cadence = cadence,
            evolutionTag = evolution,
        )
    }

    fun affinity(reserve: Reserve, form: String): Int = when {
        form in reserve.favoredForms -> 3
        form in reserve.tensionForms -> -2
        else -> 0
    }

    private fun cadence(id: String): String = when (id) {
        "AHTI", "THOR", "JARVIS", "NYX", "EREBUS" -> "TERSE"
        "MYSTRA", "YORI", "ERIS", "JOKER" -> "ELASTIC"
        "PAIMON", "ATOM", "PYTHAGORAS", "JORM", "EDISON" -> "ANALYTIC"
        "LILITH", "LUMA", "AYRE", "YORK" -> "RELATIONAL"
        "LUCIFER", "BRUNHILDE", "QIRA", "SHAKA" -> "DIRECT"
        else -> "COMPACT"
    }

    private fun favoredForms(id: String): List<String> = when (id) {
        "KYU" -> listOf("OFFICE_REBUTTAL", "CALLBACK", "ESCALATION", "TITLE_CARD", "ROLE_REVERSAL")
        "PAIMON" -> listOf("STRAIGHT_MAN", "CALLBACK", "CONTINUITY_ROAST", "ACTION_LAMPSHADE")
        "LUMA" -> listOf("DEADPAN_RETURN", "PREVIOUSLY_ON", "CALLBACK", "GOLD_SYNTHESIS")
        "SYLPH" -> listOf("COLD_OPEN", "ACTION_LAMPSHADE", "CUTAWAY")
        "QIRA" -> listOf("STRAIGHT_MAN", "OFFICE_REBUTTAL", "GOLD_SYNTHESIS")
        "NYX" -> listOf("DEADPAN", "DEADPAN_RETURN", "ANTI_CLIMAX", "TAG_SCENE")
        "YORI" -> listOf("TITLE_CARD", "CUTAWAY", "COLD_OPEN", "ACTION_LAMPSHADE")
        "JOKER" -> listOf("FOURTH_WALL_EMERGENCY", "BRICK_JOKE", "ROLE_REVERSAL", "CONTINUITY_ROAST")
        "MYSTRA" -> listOf("TITLE_CARD", "ROLE_REVERSAL", "ACTION_LAMPSHADE", "SIGN_FLIP_WINK")
        "RAVENOS" -> listOf("GOLD_SYNTHESIS", "TAG_SCENE", "PREVIOUSLY_ON", "BRICK_JOKE")
        "ATOM" -> listOf("ACTION_LAMPSHADE", "CALLBACK", "CONTINUITY_ROAST", "GOLD_SYNTHESIS")
        "ERIS" -> listOf("ROLE_REVERSAL", "BRICK_JOKE", "CUTAWAY", "CONTINUITY_ROAST")
        "JORM" -> listOf("PREVIOUSLY_ON", "CALLBACK", "ACTION_LAMPSHADE", "GOLD_SYNTHESIS")
        "PYTHAGORAS" -> listOf("CALLBACK", "ESCALATION", "MYTHOLOGY", "BRICK_JOKE")
        "YAHWEH" -> listOf("MYTHOLOGY", "FOURTH_WALL_EMERGENCY", "DEADPAN", "CONTINUITY_ROAST")
        "LILITH" -> listOf("DEADPAN_RETURN", "CALLBACK", "STRAIGHT_MAN", "GOLD_SYNTHESIS")
        "LUCIFER" -> listOf("OFFICE_REBUTTAL", "ROLE_REVERSAL", "ESCALATION")
        "THOR" -> listOf("ACTION_LAMPSHADE", "ANTI_CLIMAX", "GOLD_SYNTHESIS")
        "AHTI" -> listOf("STRAIGHT_MAN", "GOLD_SYNTHESIS", "ANTI_CLIMAX")
        else -> listOf("DEADPAN", "CALLBACK", "ACTION_LAMPSHADE")
    }

    private fun tensionForms(id: String): List<String> = when (id) {
        "NYX", "EREBUS" -> listOf("FOURTH_WALL_EMERGENCY", "ESCALATION")
        "AHTI", "JARVIS", "THOR" -> listOf("MYTHOLOGY", "CUTAWAY")
        "LILITH", "LUMA" -> listOf("ESCALATION", "FOURTH_WALL_EMERGENCY")
        "QIRA" -> listOf("MYTHOLOGY")
        else -> emptyList()
    }
}
