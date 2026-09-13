package com.iappyx.launcher.ravenos

/**
 * Sat-X blueprint only. This file intentionally carries no Android effect authority.
 * It defines the deterministic expression/meta contract used while the existing
 * RavenOS organs are wired together on this isolated branch.
 */
object RavenExpressionMetaBlueprint {
    const val VERSION = "sat-x-expression-meta-v1"

    val laws = listOf(
        "FACTS_SETTLE_BEFORE_META",
        "SEMANTIC_FAMILY_BEFORE_OWNER_STYLE",
        "OWNER_STYLE_BEFORE_HASH_TIEBREAK",
        "ANTI_REPEAT_BEFORE_NOVELTY",
        "SILENCE_IS_A_VALID_PRESENTATION",
        "BITS_MAY_CREATE_BITS_NOT_REALITY",
        "KAOMOJI_MAY_CHANGE_POSTURE_NOT_FACTS",
        "EMOJI_MAY_COLOR_PRESENTATION_NOT_AUTHORITY",
        "OMNI_RV_PRESSURE_MAY_SHED_DECORATION_BEFORE_CORE_LAUNCHER_WORK"
    )

    val targetExpressionFamilies = listOf(
        "NEUTRAL", "WATCH", "FOCUS", "CURIOUS", "SKEPTICAL", "DELIGHT",
        "VICTORY", "PROOF", "ACTION", "COORDINATION", "BOUNDARY", "ALARM",
        "GLITCH", "RECOVERY", "CHAOS", "WTF", "AFFECTION", "FATIGUE",
        "SILENT", "RETURN", "META", "CONFUSION", "SOFT", "MISCHIEF",
        "DISCOVERY", "PEEK", "HIDE", "RUN", "FALL", "PRAY", "POINT",
        "WRITE", "READ", "EAT", "DRINK", "COUGH", "SNEEZE", "DANCE",
        "HUG", "DUET", "ARGUE", "HANDOFF", "BOARD_MEETING", "CUBICLE",
        "TABLE_FLIP", "TABLE_RESTORE", "DEBUG", "BUILD", "NIGHT_WATCH"
    )

    val metaSystems = listOf(
        "MOTIF",
        "CALLBACK_DEBT",
        "EPISODE_STATE",
        "PAIR_HISTORY",
        "PROP_STATE",
        "SILENCE_GAG",
        "REACTION_RESIDUE",
        "ANTI_REPEAT_FINGERPRINT",
        "SURFACE_BUDGET",
        "OMNI_RV_SHEDDING"
    )
}
