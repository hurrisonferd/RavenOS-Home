package com.iappyx.launcher.ravenos

/** Declarative map of Goblin Brain organs, authority and shedding class. */
object RavenGoblinSystemsRegistryOS {
    enum class Stage { INGEST, SCENE, MEMORY, CAST, WRITERS, GOVERN, KNOWLEDGE, PRESENT, SURFACE }
    enum class Cost { CORE, LIGHT, MEDIUM, HEAVY }

    data class Organ(
        val id: String,
        val stage: Stage,
        val cost: Cost,
        val authority: String,
        val dependsOn: List<String> = emptyList(),
    )

    val organs = listOf(
        Organ("MARKER_BUS", Stage.INGEST, Cost.CORE, "OBSERVE"),
        Organ("LOCAL_SENSE", Stage.INGEST, Cost.LIGHT, "OBSERVE", listOf("MARKER_BUS")),
        Organ("SHADE_SENSE", Stage.INGEST, Cost.LIGHT, "OBSERVE", listOf("MARKER_BUS")),
        Organ("COMPLEX_EVENT", Stage.INGEST, Cost.LIGHT, "DERIVE", listOf("MARKER_BUS")),

        Organ("SCREEN_CONTEXT", Stage.SCENE, Cost.MEDIUM, "OBSERVE", listOf("LOCAL_SENSE")),
        Organ("VIEWPORT_SEMANTICS", Stage.SCENE, Cost.MEDIUM, "OBSERVE", listOf("SCREEN_CONTEXT")),
        Organ("PHONE_SCENE", Stage.SCENE, Cost.LIGHT, "DERIVE", listOf("MARKER_BUS")),
        Organ("SCENE_GRAPH", Stage.SCENE, Cost.MEDIUM, "DERIVE", listOf("SCREEN_CONTEXT")),

        Organ("CALLBACK_MEMORY", Stage.MEMORY, Cost.LIGHT, "MEMORY", listOf("COMPLEX_EVENT")),
        Organ("SESSION_NARRATIVE", Stage.MEMORY, Cost.LIGHT, "MEMORY", listOf("MARKER_BUS")),
        Organ("EPISODE_SCRIPT", Stage.MEMORY, Cost.MEDIUM, "MEMORY", listOf("SCREEN_CONTEXT", "SESSION_NARRATIVE")),
        Organ("BIT_LEDGER", Stage.MEMORY, Cost.LIGHT, "FICTION_ONLY", listOf("EPISODE_SCRIPT")),
        Organ("OFFICE_SEASON", Stage.MEMORY, Cost.LIGHT, "FICTION_ONLY", listOf("BIT_LEDGER")),
        Organ("OFFICE_RECALL", Stage.MEMORY, Cost.MEDIUM, "STRUCTURAL_MEMORY", listOf("SCENE_GRAPH")),
        Organ("DIALOGUE_VAULT", Stage.MEMORY, Cost.MEDIUM, "AUTHORED_MEMORY", listOf("EPISODE_SCRIPT")),
        Organ("ANTI_REPEAT", Stage.MEMORY, Cost.LIGHT, "PRESENTATION_MEMORY", listOf("DIALOGUE_VAULT")),
        Organ("TRICK_HISTORY", Stage.MEMORY, Cost.LIGHT, "PRESENTATION_MEMORY", listOf("ANTI_REPEAT")),

        Organ("SITCOM_DIRECTOR", Stage.CAST, Cost.LIGHT, "CAST", listOf("SCREEN_CONTEXT", "EPISODE_SCRIPT")),
        Organ("BACKSTAGE", Stage.CAST, Cost.LIGHT, "CAST", listOf("SITCOM_DIRECTOR")),
        Organ("GOLD_TOPOLOGY", Stage.CAST, Cost.MEDIUM, "FICTION_ONLY", listOf("BACKSTAGE", "OFFICE_SEASON")),
        Organ("METAMAX_SHOWRUNNER", Stage.CAST, Cost.LIGHT, "FICTION_ONLY", listOf("BIT_LEDGER")),
        Organ("PLOT_STACK", Stage.CAST, Cost.LIGHT, "FICTION_ONLY", listOf("EPISODE_SCRIPT")),
        Organ("EGO_RESERVE", Stage.CAST, Cost.LIGHT, "PRESENTATION", listOf("OFFICE_SEASON")),

        Organ("OBSERVATION", Stage.WRITERS, Cost.LIGHT, "TRUTH_RENDER", listOf("SCREEN_CONTEXT")),
        Organ("CONTEXTUAL_DIALOGUE", Stage.WRITERS, Cost.LIGHT, "PRESENTATION", listOf("OBSERVATION")),
        Organ("SITCOM_DIALOGUE", Stage.WRITERS, Cost.LIGHT, "FICTION_ONLY", listOf("SITCOM_DIRECTOR")),
        Organ("SCRIPT_DIALOGUE", Stage.WRITERS, Cost.LIGHT, "FICTION_ONLY", listOf("EPISODE_SCRIPT")),
        Organ("LONG_SERIES", Stage.WRITERS, Cost.MEDIUM, "FICTION_ONLY", listOf("OFFICE_SEASON", "GOLD_TOPOLOGY")),
        Organ("META_COMMENTARY", Stage.WRITERS, Cost.LIGHT, "PRESENTATION", listOf("COMPLEX_EVENT")),
        Organ("META_GOBLIN", Stage.WRITERS, Cost.LIGHT, "PRESENTATION", listOf("META_COMMENTARY")),
        Organ("OMNISCIENCE_DIALOGUE", Stage.WRITERS, Cost.LIGHT, "PRESENTATION", listOf("SHADE_SENSE", "SCREEN_CONTEXT")),
        Organ("META_SCENE", Stage.WRITERS, Cost.LIGHT, "PRESENTATION", listOf("CALLBACK_MEMORY")),
        Organ("META_GRAMMAR", Stage.WRITERS, Cost.LIGHT, "FICTION_ONLY", listOf("TRICK_HISTORY", "ANTI_REPEAT")),

        Organ("RV_RESILIENCE", Stage.GOVERN, Cost.CORE, "HEALTH", listOf("SCREEN_CONTEXT")),
        Organ("INTERRUPTIBILITY", Stage.GOVERN, Cost.LIGHT, "CADENCE", listOf("SCREEN_CONTEXT")),
        Organ("PRESENTATION_ARBITER", Stage.GOVERN, Cost.LIGHT, "CADENCE", listOf("INTERRUPTIBILITY")),
        Organ("OFFICE_GOVERNOR", Stage.GOVERN, Cost.CORE, "LOAD_SHED"),

        Organ("KNOWLEDGE_BROKER", Stage.KNOWLEDGE, Cost.HEAVY, "EXTERNAL_CONTEXT", listOf("PRESENTATION_ARBITER")),
        Organ("KNOWLEDGE_CACHE", Stage.KNOWLEDGE, Cost.LIGHT, "EXTERNAL_CONTEXT", listOf("KNOWLEDGE_BROKER")),

        Organ("EMPLOYEE_PRESENTATION", Stage.PRESENT, Cost.LIGHT, "PRESENTATION", listOf("SITCOM_DIRECTOR")),
        Organ("SCENE_EXPRESSION", Stage.PRESENT, Cost.LIGHT, "PRESENTATION", listOf("EMPLOYEE_PRESENTATION")),
        Organ("EMOJI_RESERVOIR", Stage.PRESENT, Cost.LIGHT, "PRESENTATION", listOf("SCENE_EXPRESSION")),
        Organ("KAOMOJI_GRAMMAR", Stage.PRESENT, Cost.LIGHT, "PRESENTATION", listOf("SCENE_EXPRESSION")),
        Organ("EXPRESSION_RESERVOIR", Stage.PRESENT, Cost.MEDIUM, "PRESENTATION", listOf("SCENE_EXPRESSION")),

        Organ("OFFICE_BAR", Stage.SURFACE, Cost.CORE, "DISPLAY"),
        Organ("FOLLOW_ME", Stage.SURFACE, Cost.LIGHT, "DISPLAY"),
        Organ("GOBLIN_OVERLAY", Stage.SURFACE, Cost.LIGHT, "DISPLAY"),
        Organ("RAVEN_WIDGET", Stage.SURFACE, Cost.LIGHT, "DISPLAY"),
        Organ("WATCHLET", Stage.SURFACE, Cost.LIGHT, "DISPLAY"),
    )

    fun compact(): String {
        val byStage = organs.groupBy { it.stage }
        return Stage.values().joinToString(" · ") { stage -> "${stage.name}=${byStage[stage]?.size ?: 0}" }
    }

    fun enabledUnder(pressure: RavenOmniRvExpressionBudget.Pressure): List<Organ> = organs.filter { organ ->
        when (pressure) {
            RavenOmniRvExpressionBudget.Pressure.NOMINAL -> true
            RavenOmniRvExpressionBudget.Pressure.BUSY -> organ.cost != Cost.HEAVY
            RavenOmniRvExpressionBudget.Pressure.HOT -> organ.cost in setOf(Cost.CORE, Cost.LIGHT)
            RavenOmniRvExpressionBudget.Pressure.CRITICAL -> organ.cost == Cost.CORE
        }
    }
}
