package com.iappyx.launcher.ravenos

/** Small deterministic meta-tail library; selected only after a trick is earned. */
object RavenMetaTailLibrary {
    private val tails = mapOf(
        "DRY_ACK" to listOf("Noted. Catastrophe downgraded to paperwork.", "Acknowledged. The rectangle survives."),
        "UNDERSTATEMENT" to listOf("This appears to be mildly a situation.", "Operationally, that was a thing."),
        "FALSE_GRANDEUR" to listOf("The office has declared this a historic administrative moment.", "A tiny ceremony has been scheduled without cause."),
        "CALLBACK_PAYOFF" to listOf("The callback landed. The joke has receipts now.", "Old bit, new consequence, same suspicious clipboard."),
        "RUNNING_BIT_PROMOTION" to listOf("The bit has tenure now.", "This joke has survived probation and requested a cubicle."),
        "PROP_CALLBACK" to listOf("The prop has returned and is acting like management.", "The object remembers. HR regrets everything."),
        "PAIR_SIDE_EYE" to listOf("Two departments have independently reached the same side-eye.", "Cross-team alignment achieved through suspicion."),
        "FOURTH_WALL" to listOf("The launcher has noticed it is a launcher again.", "Fifth wall integrity: decorative."),
        "BUREAUCRATIC_ESCALATION" to listOf("A committee has formed around a fact that fit in one sentence.", "The paperwork is now larger than the event."),
        "TINY_INSTITUTION" to listOf("A department now exists for this. Nobody remembers approving it.", "The bit has acquired office hours."),
        "HISTORICAL_REVISION" to listOf("Archive staff now insist this was always inevitable.", "History has been edited by people who were present five minutes ago."),
        "FAKE_DEPARTMENT" to listOf("The Department of Extremely Specific Problems is on it.", "This has been routed to a ministry that did not exist this morning."),
        "PROP_AS_CHARACTER" to listOf("The prop has a vote now. This was a mistake.", "The furniture has entered the cast list."),
        "REACTION_SHADOW" to listOf("A previous reaction is still standing in the doorway.", "The last speaker left emotional fingerprints on the cubicle."),
        "BUTTON_ANXIETY" to listOf("The big red button continues to look employable.", "One UI control is trying very hard to become lore."),
        "RECEIPT_HUMOR" to listOf("Proof first. Goblin second. Receipt tree remains load-bearing.", "The receipt has receipts."),
        "OMNI_RV_COCKPIT_GAG" to listOf("Cockpit says drive. Office says meeting. Raven says NEXT.", "Omni RV remains one bad button away from becoming a staff car."),
        "HAUNT_SELF_AWARENESS" to listOf("This haunting is now aware it has a settings page.", "The goblin has discovered product management."),
        "NO_OP_THEATER" to listOf("No-op, but with dramatic lighting.", "Nothing changed. The office applauded anyway."),
        "DELAYED_PUNCHLINE" to listOf("The punchline arrived one event late carrying documentation.", "Timing debt repaid."),
        "SERIOUS_SETUP_STUPID_PAYOFF" to listOf("After rigorous analysis: tiny hat.", "Conclusion: technically sound, spiritually ridiculous."),
        "STUPID_SETUP_SERIOUS_PAYOFF" to listOf("The goblin joke accidentally found the real constraint.", "Ridiculous premise, annoyingly useful result.")
    )

    fun pick(id: String, seed: Int): String {
        val options = tails[id] ?: return ""
        return options[Math.floorMod(seed, options.size)]
    }
}
