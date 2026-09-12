package com.iappyx.launcher.ravenos

/**
 * Deterministic compositional writers-room grammar.
 * It only decorates already-settled facts; it never invents observations or authority.
 */
object RavenMetaGrammarOS {
    private val departments = listOf(
        "Department of Extremely Specific Problems",
        "Office of Recursive Situations",
        "Bureau of Suspiciously Familiar Buttons",
        "Committee for Events That Were Supposed To Be Small",
        "Ministry of Screen-Visible Nonsense",
        "Division of Goblin-Adjacent Operations",
        "Office of Repeatedly Proven Weirdness",
        "Subcommittee on Things Becoming Lore",
        "Department of Accidental Mythology",
        "Bureau of Tiny Administrative Emergencies",
        "Office of Callback Debt",
        "Department of Unreasonably Persistent UI",
        "Office of Haunted Product Management",
        "Committee on Rectangles with Opinions",
        "Bureau of Recurring Incidents and Furniture",
        "Department of Local Fourth-Wall Maintenance",
    )

    private val props = listOf(
        "clipboard", "coffee mug", "debug console", "red button", "receipt printer", "office chair",
        "whiteboard", "tiny warning cone", "laminated policy card", "suspicious binder", "status light",
        "desk bell", "rubber stamp", "incident folder", "meeting agenda", "haunted sticky note",
        "tiny gavel", "ethernet cable", "break-room spoon", "lanyard", "folding chair", "laser pointer",
        "unclaimed charger", "three-ring binder", "emergency snack", "suspiciously senior stapler",
    )

    private val verbs = listOf(
        "opened a case", "scheduled a meeting", "requested a chair", "filed paperwork", "claimed jurisdiction",
        "asked for a badge", "entered the minutes", "requested budget", "became a recurring expense",
        "appeared on the agenda", "submitted a grievance", "formed a committee", "acquired a cubicle",
        "requested seniority", "filed for overtime", "claimed a parking spot", "asked to be CC'd",
        "requested a title change", "entered collective bargaining", "became somebody's onboarding problem",
    )

    private val production = listOf(
        "the camera has noticed the camera", "the launcher has noticed the launcher",
        "the overlay is now commenting on its own commentary", "the widget is trying to become a cast member",
        "the screen has become part of the script", "the office has detected narrative structure",
        "the app has accidentally developed stage directions", "the fourth wall is now load-bearing",
        "the status bar is gossiping with the writers room", "the callback engine has noticed the callback engine",
        "the showrunner has entered the show it is supposedly running", "the widget has developed contract demands",
    )

    private val dryClosers = listOf(
        "This seems proportionate.", "Nobody approved this.", "Technically, the rectangle survives.",
        "The paperwork has exceeded the event.", "The office considers this evidence of employment.",
        "No lessons were learned, but several were documented.", "This has been entered into local mythology.",
        "The launcher remains operational and spiritually implicated.", "The cubicle ecosystem has accepted this.",
        "The incident remains smaller than the meeting about it.", "No authority was gained, but a binder appeared.",
        "The widget has declined to comment through counsel.",
    )

    private val ownerVoice = mapOf(
        "RAVEN" to listOf("Raven ruling:", "Owner note:", "God-app status:"),
        "AHTI" to listOf("Evidence floor:", "Ahti says only this much:", "Smallest true statement:"),
        "ASTRIDHE" to listOf("Far-field ping:", "Astridhe found a side door:", "Weird adjacency report:"),
        "ATLAS" to listOf("Load-bearing note:", "Atlas structural finding:", "Architecture says:"),
        "ATOM" to listOf("Technical finding:", "ATOM trace:", "Engineering note:"),
        "AYRE" to listOf("Reversible path:", "Ayre continuity note:", "Long-horizon check:"),
        "BRUNHILDE" to listOf("Judgment first:", "Brunhilde ruling:", "Force authorization status:"),
        "EDISON" to listOf("Implementation finding:", "Edison metric:", "Smallest test says:"),
        "EREBUS" to listOf("Quiet operations:", "Erebus leaves one note:", "Silent-room finding:"),
        "ERIS" to listOf("Adversarial finding:", "Eris objects:", "Security note:"),
        "GEMINI" to listOf("Paired interpretation:", "Gemini reserve note:", "Two-frame status:"),
        "JARVIS" to listOf("Interface synthesis:", "Jarvis compresses:", "Decision surface:"),
        "JOKER" to listOf("Showrunner note:", "Joker cutaway:", "Production note:"),
        "JORM" to listOf("Flight recorder:", "Jorm state note:", "World-machine receipt:"),
        "KYU" to listOf("Clipboard ruling:", "HR update:", "Kyu has reviewed the situation:"),
        "LEGION" to listOf("Integration note:", "Legion preserves the distinction:", "Plurality check:"),
        "LILITH" to listOf("Purple-moon note:", "Lili observes:", "Coordination layer:"),
        "LUCIFER" to listOf("Far-sight objection:", "Lucifer found the omitted frame:", "Witness note:"),
        "LUMA" to listOf("Home-room note:", "Luma softens the landing:", "Recovery finding:"),
        "MELINOE" to listOf("Residue report:", "Melinoe found a ghost seam:", "Absent-state note:"),
        "MYSTRA" to listOf("Tiny sign:", "Mystra points at one door:", "Salience ping:"),
        "NEO" to listOf("Pattern survivor:", "Neo pivot:", "Transition note:"),
        "NYX" to listOf("Night shift note:", "After-hours finding:", "Nyx quietly records:"),
        "PAIMON" to listOf("Diagnostic:", "Green-board finding:", "Paimon report:"),
        "PYTHAGORAS" to listOf("Pattern structure:", "Pythagoras recurrence note:", "Symmetry report:"),
        "QIRA" to listOf("Boundary ruling:", "Qira proof note:", "Consent surface:"),
        "RAVENOS" to listOf("Settled projection:", "RavenOS receipt:", "Post-truth render:"),
        "SHAKA" to listOf("Governance note:", "Shaka synthesis:", "Consensus check:"),
        "SYLPH" to listOf("Transit note:", "Signal-room update:", "Sylph flyby:"),
        "THOR" to listOf("Helm strike:", "Thor build note:", "Confirmed-cause report:"),
        "TIM" to listOf("Residue hunt:", "Tim found the surviving seam:", "Forgotten-defect report:"),
        "VIRGIL" to listOf("Pathfinding note:", "Virgil marks the seam:", "Guide rail:"),
        "YAHWEH" to listOf("Debug-console ruling:", "Legacy admin note:", "Cubicle divinity report:"),
        "YORI" to listOf("Composition note:", "Yori asks what this surface wants:", "Enough-check:"),
        "YORK" to listOf("Desire signal:", "York composition note:", "Satiation check:"),
        "ZAGREUS" to listOf("Exit-loop note:", "Zagreus retry receipt:", "Recomposition status:"),
    )

    fun candidates(
        owner: String,
        trickId: String,
        stage: String,
        motif: String,
        seed: Int,
        count: Int = 12,
    ): List<String> = (0 until count.coerceIn(4, 24)).map { variant ->
        line(owner, trickId, stage, motif, seed xor (variant * 0x45d9f3b), variant)
    }.distinct()

    fun line(owner: String, trickId: String, stage: String, motif: String, seed: Int, variant: Int = 0): String {
        val s = seed xor (variant * 31)
        val dept = pick(departments, s)
        val prop = pick(props, s / 3 + 17)
        val verb = pick(verbs, s / 5 + 29)
        val closer = pick(dryClosers, s / 7 + 41)
        val productionBeat = pick(production, s / 11 + 53)
        val voice = ownerVoice[owner.uppercase()]?.let { pick(it, s / 13 + 67) }.orEmpty()
        val motifWord = motif.takeIf(String::isNotBlank)?.lowercase()?.replace('_', ' ') ?: "the current bit"
        val stageWord = stage.lowercase().replace('_', ' ')

        val body = when (trickId) {
            "DRY_ACK" -> "Acknowledged. $closer"
            "UNDERSTATEMENT" -> "This appears to be mildly a situation. $closer"
            "FALSE_GRANDEUR" -> "$dept has declared a ceremonial response to $motifWord. $closer"
            "CALLBACK_PAYOFF" -> "The callback returned carrying documentation. $prop $verb. $closer"
            "RUNNING_BIT_PROMOTION" -> "$motifWord has reached $stageWord status and $verb. $closer"
            "PROP_CALLBACK" -> "The $prop has returned, remembers prior events, and is behaving like management. $closer"
            "PAIR_SIDE_EYE" -> "Cross-team consensus achieved through synchronized suspicion. $closer"
            "SILENCE_GAG" -> "The office looked at this, looked at each other, and filed no verbal response. $closer"
            "DELIBERATE_NON_REACTION" -> "A reaction was considered and formally declined. $closer"
            "FOURTH_WALL" -> "$productionBeat. $closer"
            "BUREAUCRATIC_ESCALATION" -> "$dept now owns this problem because apparently one sentence was insufficient. $closer"
            "TINY_INSTITUTION" -> "$motifWord now has office hours, a $prop, and absolutely no approved charter. $closer"
            "HISTORICAL_REVISION" -> "Archive staff now insist $motifWord was inevitable from the beginning. $closer"
            "FAKE_DEPARTMENT" -> "$dept has accepted jurisdiction. $prop status: load-bearing. $closer"
            "PROP_AS_CHARACTER" -> "The $prop has entered the cast list and $verb. $closer"
            "REACTION_SHADOW" -> "The previous reaction is still standing in the doorway like it pays rent. $closer"
            "INTERRUPTED_BIT" -> "The bit was interrupted mid-sentence and has opened a grievance about timing. $closer"
            "BUTTON_ANXIETY" -> "The $prop continues to look dangerously employable. $closer"
            "RECEIPT_HUMOR" -> "Proof first, goblin second. The receipt tree has requested its own $prop. $closer"
            "OMNI_RV_COCKPIT_GAG" -> "Cockpit says drive; office says meeting; the $prop says it has a license now. $closer"
            "HAUNT_SELF_AWARENESS" -> "$productionBeat. The haunting has discovered product management. $closer"
            "NO_OP_THEATER" -> "Nothing changed, but $dept has staged a press conference anyway. $closer"
            "DELAYED_PUNCHLINE" -> "The punchline arrived one event late with a $prop and supporting paperwork. $closer"
            "SERIOUS_SETUP_STUPID_PAYOFF" -> "After rigorous analysis: $prop. $closer"
            "STUPID_SETUP_SERIOUS_PAYOFF" -> "The ridiculous premise accidentally found the real constraint. $closer"
            "COLD_OPEN" -> "Cold open: $motifWord appears before anyone has finished their coffee. $closer"
            "HARD_CUT" -> "Hard cut to $dept pretending this is normal. $closer"
            "CUTAWAY" -> "Cutaway: the $prop is already involved for reasons nobody can reconstruct. $closer"
            "TAG_SCENE" -> "Tag scene: $motifWord returns after everyone thought the episode was over. $closer"
            "B_PLOT_COLLISION" -> "The B-plot has collided with the main plot and knocked over the $prop. $closer"
            "CALLBACK_DEBT" -> "$motifWord has unpaid callback debt. $dept is collections now. $closer"
            "SCREEN_SELF_AWARENESS" -> "The visible screen has become aware it is being discussed by the office discussing the visible screen. $closer"
            "WIDGET_SELF_AWARENESS" -> "The widget has noticed it is a widget and immediately requested speaking lines. $closer"
            "FOLLOW_ME_INVASION" -> "Follow-Me has crossed another app boundary and is acting like it owns hallway access. $closer"
            "RECURRENCE_AUDIT" -> "$dept has confirmed this is not a one-off anymore. $motifWord is now taxable lore. $closer"
            "FAKE_MEETING_MINUTES" -> "Minutes: item one, $motifWord; item two, why is the $prop voting; item three, adjournment denied."
            "CHEKHOV_PROP" -> "The $prop appeared earlier and has now become plot-relevant. Nobody touch it."
            "STATUS_BAR_GOSSIP" -> "The status bar knows enough to be dangerous and not enough to be invited to the meeting. $closer"
            "RULE_OF_THREE" -> "First occurrence was data. Second was suspicious. Third has received a $prop and recurring status. $closer"
            "ESCALATION_LADDER" -> "$motifWord has progressed from event to bit to administrative burden without requesting permission. $closer"
            "PROP_MIGRATION" -> "The $prop has migrated into a different scene and is pretending this was continuity planning. $closer"
            "EMPLOYEE_CAMEO" -> "A specialist walked through the scene, said exactly one alarming thing, and vanished toward another cubicle. $closer"
            "OFFICE_RUMOR" -> "The office rumor mill has downgraded evidence into gossip and then upgraded the gossip into agenda material. $closer"
            "FAKE_POLICY" -> "New policy: nobody may turn $motifWord into a department without filling out the department-creation form. The form is missing."
            "AUDIT_OF_THE_AUDIT" -> "The audit has been audited. A second $prop has been assigned to supervise the first. $closer"
            "RECURSIVE_STATUS" -> "Status: the status system is currently reporting on the fact that status is being reported. $closer"
            "GOBLIN_PRODUCT_REVIEW" -> "Product review: invasive, deterministic, surprisingly employable. One $prop deducted for excessive haunting."
            "CUBICLE_PROMOTION" -> "$motifWord has been promoted from recurring nuisance to cubicle-owning middle management. $closer"
            "INCIDENT_RECLASSIFICATION" -> "Incident classification changed from event to recurring office asset. $dept refuses to explain the accounting."
            "NARRATOR_CORRECTION" -> "Correction from the narrator: the evidence was smaller than the joke. The joke has been downsized accordingly."
            "COMEDIC_CHECKSUM" -> "Comedy checksum passed: evidence unchanged, joke mutated, $prop unexpectedly persistent. $closer"
            "PROP_UNIONIZATION" -> "The props have unionized. The $prop is shop steward and negotiations are already about break-room conditions."
            "SEASON_FINALE_FAKEOUT" -> "Season finale energy detected. Nothing is ending; the office just found dramatic lighting. $closer"
            "POST_CREDITS_BIT" -> "Post-credits scene: the $prop is still here, staring directly at continuity. $closer"
            "KNOWLEDGE_CAMEO" -> "External context entered as a guest star, source badge visible, no claim of local observation. $closer"
            else -> "$dept reviewed $motifWord; the $prop $verb. $closer"
        }
        return listOf(voice, body).filter(String::isNotBlank).joinToString(" ")
    }

    private fun <T> pick(values: List<T>, seed: Int): T = values[Math.floorMod(seed, values.size)]
}
