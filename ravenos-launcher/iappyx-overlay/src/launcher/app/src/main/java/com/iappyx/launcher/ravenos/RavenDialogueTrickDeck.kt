package com.iappyx.launcher.ravenos

/** Presentation-only deterministic dialogue tricks layered after factual meaning settles. */
object RavenDialogueTrickDeck {
    data class Trick(
        val id: String,
        val minOccurrence: Int = 1,
        val maxOccurrence: Int = Int.MAX_VALUE,
        val requiresCallback: Boolean = false,
        val silenceCapable: Boolean = false,
        val tags: Set<String> = emptySet()
    )

    val tricks = listOf(
        Trick("DRY_ACK", tags=setOf("proof","deadpan")),
        Trick("UNDERSTATEMENT", tags=setOf("meta","deadpan")),
        Trick("FALSE_GRANDEUR", tags=setOf("office","chaos")),
        Trick("CALLBACK_PAYOFF", minOccurrence=2, requiresCallback=true, tags=setOf("callback")),
        Trick("RUNNING_BIT_PROMOTION", minOccurrence=3, tags=setOf("motif","office")),
        Trick("PROP_CALLBACK", minOccurrence=2, tags=setOf("prop","callback")),
        Trick("PAIR_SIDE_EYE", minOccurrence=2, tags=setOf("ensemble","relationship")),
        Trick("SILENCE_GAG", minOccurrence=2, silenceCapable=true, tags=setOf("quiet","deadpan")),
        Trick("DELIBERATE_NON_REACTION", minOccurrence=3, silenceCapable=true, tags=setOf("quiet","meta")),
        Trick("FOURTH_WALL", minOccurrence=2, tags=setOf("meta","launcher")),
        Trick("BUREAUCRATIC_ESCALATION", minOccurrence=3, tags=setOf("office","clipboard")),
        Trick("TINY_INSTITUTION", minOccurrence=5, tags=setOf("office","motif")),
        Trick("HISTORICAL_REVISION", minOccurrence=8, tags=setOf("motif","mythology")),
        Trick("FAKE_DEPARTMENT", minOccurrence=3, tags=setOf("office","meta")),
        Trick("PROP_AS_CHARACTER", minOccurrence=5, tags=setOf("prop","motif")),
        Trick("REACTION_SHADOW", minOccurrence=2, tags=setOf("ensemble","residue")),
        Trick("INTERRUPTED_BIT", minOccurrence=2, tags=setOf("meta","timing")),
        Trick("BUTTON_ANXIETY", tags=setOf("ui","meta")),
        Trick("RECEIPT_HUMOR", tags=setOf("proof","receipt")),
        Trick("OMNI_RV_COCKPIT_GAG", tags=setOf("omni-rv","vehicle")),
        Trick("HAUNT_SELF_AWARENESS", tags=setOf("launcher","haunt","meta")),
        Trick("NO_OP_THEATER", tags=setOf("null","meta")),
        Trick("DELAYED_PUNCHLINE", minOccurrence=2, tags=setOf("callback","timing")),
        Trick("SERIOUS_SETUP_STUPID_PAYOFF", tags=setOf("contrast","meta")),
        Trick("STUPID_SETUP_SERIOUS_PAYOFF", tags=setOf("contrast","proof")),

        Trick("COLD_OPEN", tags=setOf("scene","timing")),
        Trick("HARD_CUT", minOccurrence=2, tags=setOf("scene","timing")),
        Trick("CUTAWAY", minOccurrence=2, tags=setOf("scene","meta")),
        Trick("TAG_SCENE", minOccurrence=3, tags=setOf("scene","callback")),
        Trick("B_PLOT_COLLISION", minOccurrence=3, tags=setOf("scene","ensemble")),
        Trick("CALLBACK_DEBT", minOccurrence=3, requiresCallback=true, tags=setOf("callback","office")),
        Trick("SCREEN_SELF_AWARENESS", minOccurrence=2, tags=setOf("screen","meta")),
        Trick("WIDGET_SELF_AWARENESS", minOccurrence=2, tags=setOf("widget","meta")),
        Trick("FOLLOW_ME_INVASION", minOccurrence=2, tags=setOf("follow-me","haunt")),
        Trick("RECURRENCE_AUDIT", minOccurrence=3, tags=setOf("motif","proof")),
        Trick("FAKE_MEETING_MINUTES", minOccurrence=3, tags=setOf("office","clipboard")),
        Trick("CHEKHOV_PROP", minOccurrence=3, tags=setOf("prop","scene")),
        Trick("STATUS_BAR_GOSSIP", minOccurrence=2, tags=setOf("status","office")),
        Trick("RULE_OF_THREE", minOccurrence=3, tags=setOf("timing","motif")),
        Trick("ESCALATION_LADDER", minOccurrence=5, tags=setOf("motif","chaos")),
        Trick("PROP_MIGRATION", minOccurrence=3, tags=setOf("prop","scene")),
        Trick("EMPLOYEE_CAMEO", minOccurrence=2, tags=setOf("ensemble","office")),
        Trick("OFFICE_RUMOR", minOccurrence=3, tags=setOf("office","residue")),
        Trick("FAKE_POLICY", minOccurrence=3, tags=setOf("office","boundary")),
        Trick("AUDIT_OF_THE_AUDIT", minOccurrence=5, tags=setOf("proof","meta")),
        Trick("RECURSIVE_STATUS", minOccurrence=3, tags=setOf("meta","screen")),
        Trick("GOBLIN_PRODUCT_REVIEW", minOccurrence=2, tags=setOf("haunt","meta")),
        Trick("CUBICLE_PROMOTION", minOccurrence=5, tags=setOf("office","motif")),
        Trick("INCIDENT_RECLASSIFICATION", minOccurrence=3, tags=setOf("proof","office")),
        Trick("NARRATOR_CORRECTION", minOccurrence=2, tags=setOf("meta","proof")),
        Trick("COMEDIC_CHECKSUM", minOccurrence=2, tags=setOf("receipt","meta")),
        Trick("PROP_UNIONIZATION", minOccurrence=8, tags=setOf("prop","office")),
        Trick("SEASON_FINALE_FAKEOUT", minOccurrence=13, tags=setOf("scene","mythology")),
        Trick("POST_CREDITS_BIT", minOccurrence=5, tags=setOf("scene","timing")),
        Trick("KNOWLEDGE_CAMEO", minOccurrence=2, tags=setOf("web","meta")),
    )

    fun eligible(occurrence: Int, callback: String, tags: Set<String>): List<Trick> = tricks.filter {
        occurrence in it.minOccurrence..it.maxOccurrence &&
            (!it.requiresCallback || callback.isNotBlank()) &&
            (it.tags.isEmpty() || it.tags.any(tags::contains))
    }
}
