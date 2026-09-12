package com.iappyx.launcher.ravenos

/**
 * Long-running-series writer: spends bounded multi-session history without persisting dialogue.
 */
object RavenLongSeriesDialogueOS {
    data class Beat(
        val primary: String,
        val secondary: String,
        val synthesis: String,
        val terminal: String,
        val family: String,
    )

    fun select(
        member: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
        bit: RavenBitLedgerOS.Cue,
        show: RavenMetaMaxShowrunnerOS.Beat,
        memory: RavenOfficeSeasonOS.Memory,
        reserve: RavenEgoReserveProjectionOS.Reserve,
        gold: RavenGoldSitcomTopologyOS.Beat,
    ): Beat {
        if (!screen.available && !gold.terminal) return Beat("", "", "", "", "SERIES_NONE")
        val scene = script.sceneOwner.ifBlank { screen.appLabel.orEmpty().ifBlank { "the phone" } }
        val subject = script.subject.ifBlank { screen.focus }.replace(Regex("\\s+"), " ").trim().take(82)
        val form = chooseForm(memory, gold, show, reserve, direction)
        val riff = RavenMetaGoblinRiffOS.select(member, screen, script, direction, memory, gold, show)
        val useRiff = !gold.terminal && riff.text.isNotBlank() && (
            screen.meta || script.interruption.isNotBlank() || script.returned || memory.motifReturningAcrossSessions ||
                gold.phase in setOf("CALLBACK", "ESCALATE") || show.level >= 3 || direction.turn % 7 == 0
            )
        val primary = when {
            gold.terminal -> ""
            useRiff -> riff.text
            else -> ownerLine(member.id, form, scene, subject, script, bit, memory, reserve, gold)
        }
        val secondary = if (gold.secondary != null && gold.crosstalkEligible) {
            partnerLine(gold.secondary.id, member.id, form, subject, memory, gold)
        } else ""
        val synthesis = if (gold.synthesisEligible && !gold.terminal) synthesisLine(scene, subject, memory, gold)
            else ""
        val terminal = if (gold.terminal) listOf(gold.authorNote, gold.dumbchecksum).filter(String::isNotBlank).joinToString("\n") else ""
        val family = if (useRiff) riff.family else "SERIES_${gold.phase}_$form"
        return Beat(primary.take(340), secondary.take(220), synthesis.take(220), terminal.take(430), family)
    }

    private fun chooseForm(
        m: RavenOfficeSeasonOS.Memory,
        gold: RavenGoldSitcomTopologyOS.Beat,
        show: RavenMetaMaxShowrunnerOS.Beat,
        reserve: RavenEgoReserveProjectionOS.Reserve,
        d: RavenSitcomDirectorOS.Direction,
    ): String {
        val candidates = buildList {
            if (m.motifReturningAcrossSessions) add("SEASON_CALLBACK")
            if (m.pairCount in setOf(3, 5, 8, 13, 21)) add("PAIR_PAYOFF")
            if (m.longArc && m.memberLines in setOf(8, 13, 21, 34, 55)) add("CHARACTER_ARC")
            if (m.memberState == "EVOLVING") add("EGO_EVOLUTION")
            if (gold.phase == "CALLBACK") add("GOLD_CALLBACK")
            if (gold.phase == "ESCALATE") add("GOLD_ESCALATE")
            if (gold.phase == "OPEN") add("GOLD_OPEN")
            if (show.form in reserve.favoredForms) add(show.form)
            add("SERIES_DEADPAN")
        }
        val pool = candidates.distinct()
        return pool[stableIndex("${m.episode}|${m.memberLines}|${m.pairCount}|${m.motifLifetimeCount}|${d.turn}|${reserve.owner}", pool.size)]
    }

    private fun ownerLine(
        id: String,
        form: String,
        scene: String,
        subject: String,
        script: RavenEpisodeScriptOS.Cue,
        bit: RavenBitLedgerOS.Cue,
        m: RavenOfficeSeasonOS.Memory,
        reserve: RavenEgoReserveProjectionOS.Reserve,
        gold: RavenGoldSitcomTopologyOS.Beat,
    ): String {
        val series = "S${m.season}E${m.episodeInSeason}"
        val prior = if (m.motifReturningAcrossSessions) "This bit survived a whole app process. " else ""
        val generic = when (form) {
            "SEASON_CALLBACK" -> "Previously on $series: ${bit.label.ifBlank { script.motif.lowercase().replace('_', ' ').ifBlank { subject } }} has returned from an earlier session. $prior"
            "PAIR_PAYOFF" -> "This pairing has shared ${m.pairCount} earned beats now. Chemistry is no longer a random-number generator."
            "CHARACTER_ARC" -> "Long-arc marker: $id has delivered ${m.memberLines} lines and ${m.memberCallbacks} callbacks without becoming the whole show."
            "EGO_EVOLUTION" -> "Performance reserve is ${reserve.state.lowercase()}: the role has enough history to vary without losing its anchor."
            "GOLD_CALLBACK" -> "Gold phase=CALLBACK. Same relationship, new beat; do not replay the setup."
            "GOLD_ESCALATE" -> "Gold phase=ESCALATE. Sharpen the reaction; do not add three more speakers."
            "GOLD_OPEN" -> "Gold phase=OPEN. Establish $scene / “$subject” with one strong voice before the office piles in."
            else -> "$scene is still about “$subject”. The office remembers enough history to avoid introducing itself again."
        }
        return when (id) {
            "KYU" -> when (form) {
                "SEASON_CALLBACK" -> "CLIPBOARD SEASON EVENT: this bit crossed a process boundary and came back with seniority. I am not giving it a parking space."
                "PAIR_PAYOFF" -> "Pair beat #${m.pairCount}. Fine. The chemistry has receipts. Nobody schedule a team-building retreat."
                "CHARACTER_ARC" -> "I have ${m.memberLines} lines of documented management failure and somehow the clipboard is still the stable object. BONK continuity."
                else -> "Gold rule says sitcom is topology, not chatter. Excellent. I can bonk exactly one useful thing: “$subject”."
            }
            "PAIMON" -> when (form) {
                "SEASON_CALLBACK" -> "Cross-session premise check: yes, this is the same structural bit, not fresh weirdness in an old coat."
                "PAIR_PAYOFF" -> "Chemistry count ${m.pairCount}. The pairing is now evidence-backed; canned banter may stand down."
                "EGO_EVOLUTION" -> "Identity reserve says ${reserve.state}. Good. Growth without voice drift is the actual test."
                else -> "Gold phase ${gold.phase}: keep observation under interpretation. “$subject” survives the frame attack."
            }
            "YORI" -> when (form) {
                "SEASON_CALLBACK" -> "Previously on the tiny haunted rectangle: this shot existed last session and still has visual grammar. Use the callback; don't reshoot the pilot."
                "PAIR_PAYOFF" -> "This duo has blocked ${m.pairCount} scenes together. That is composition now, not coincidence."
                "CHARACTER_ARC" -> "My long arc has learned the difference between a cut, a cameo, and an actual scene change. Keep “$subject” in frame."
                else -> "OPEN/BUILD/CALLBACK is useful editing language. This is ${gold.phase.lowercase()}; shoot it like one."
            }
            "JOKER" -> when (form) {
                "SEASON_CALLBACK" -> "A joke survived process death. Great. We have invented syndication for bugs."
                "PAIR_PAYOFF" -> "Recurring duo #${m.pairCount}. HR has upgraded this from incident to format."
                "CHARACTER_ARC" -> "Apparently I have a character arc now. This is what happens when nobody cancels the haunted launcher after episode twelve."
                "EGO_EVOLUTION" -> "My performance state says EVOLVING. Please do not tell the fourth wall; it is already insecure."
                else -> "Gold topology has confirmed the comedy is relational. Terrible news for anyone hoping Android callbacks were funny by themselves."
            }
            "ATOM" -> when (form) {
                "SEASON_CALLBACK" -> "Persistent structural state confirmed across sessions: motif=${script.motif.ifBlank { "scene" }} count=${m.motifLifetimeCount}. No transcript required."
                "CHARACTER_ARC", "EGO_EVOLUTION" -> "Anchor preserved while behavior diversified. That is acceptable evolution; identity merge remains unnecessary."
                "PAIR_PAYOFF" -> "Pair recurrence=${m.pairCount}. Chemistry has become typed history instead of probabilistic decoration."
                else -> "Gold phase=${gold.phase}; scene=$scene; subject=“$subject”. One evidence angle per speaker keeps the causal graph readable."
            }
            "ERIS" -> when (form) {
                "SEASON_CALLBACK" -> "The bit escaped the process boundary and returned through a permitted structural key. Nice edge. Keep it weird; keep it bounded."
                "PAIR_PAYOFF" -> "This pair has history now. I will perturb the chemistry, not erase it."
                "EGO_EVOLUTION" -> "Evolution without flattening means I get sharper edges, not somebody else's cadence. Good."
                else -> "Gold says do not manufacture disagreement. Fine. I will attack the clean model instead: “$subject” still has an excluded edge."
            }
            "LILITH" -> when (form) {
                "SEASON_CALLBACK" -> "This came back from another session because it remained part of the relationship, not because we needed filler. Let it sit beside us."
                "PAIR_PAYOFF" -> "${m.pairCount} shared beats is enough history to recognize each other without merging voices."
                "CHARACTER_ARC" -> "The role can grow and still sound like itself. That is the whole point of keeping anchors."
                else -> "Targeted crosstalk, then quiet. “$subject” gets the room; the office does not have to crowd it."
            }
            "LUMA" -> when (form) {
                "SEASON_CALLBACK" -> "The room remembered this after a restart. That's the useful kind of haunting: continuity without hoarding the room."
                "PAIR_PAYOFF" -> "This pair has enough shared history to feel lived-in now. No need to light the furniture on fire for proof."
                else -> "Gold BUILD phase means the room can simply remain a room. “$subject” is still warm enough to inhabit."
            }
            "MYSTRA" -> when (form) {
                "SEASON_CALLBACK" -> "SIGN: the bit returned after the app did not. FLIP: process death != narrative death. WINK: persistence is suddenly wearing glitter 😉"
                "PAIR_PAYOFF" -> "SIGN: pair=${m.pairCount}. FLIP: random duet != chemistry. WINK: evidence bought the sparkle this time 😉"
                else -> "SIGN → “$subject”. FLIP → NEW != IMPORTANT. WINK → tiny sign, actual scene 😉"
            }
            "THOR" -> when (form) {
                "SEASON_CALLBACK" -> "Old target returned. Good. We already know where to strike; do not rebuild the hammer."
                "PAIR_PAYOFF" -> "Pair history ${m.pairCount}. Enough. One speaker hits cause, one speaker checks the seam."
                else -> "Gold phase ${gold.phase}. One load-bearing line. “$subject”. Done."
            }
            "AHTI" -> when (form) {
                "SEASON_CALLBACK" -> "Old river, same stone. The restart changed the water, not the evidence."
                "PAIR_PAYOFF" -> "Two voices are enough when they carry different weight."
                else -> "Smallest true version: “$subject”. Gold can decorate only after truth fits in the hand."
            }
            "RAVENOS" -> when (form) {
                "SEASON_CALLBACK" -> "Settled projection: cross-session callback verified structurally; no raw transcript was needed to preserve the joke."
                "CHARACTER_ARC", "EGO_EVOLUTION" -> "Settled projection: role anchor held while expression evolved. Identity continuity passed this episode's dumb test."
                else -> "Settled projection: ${gold.phase} / $scene / “$subject”. Different voices, different jobs, same reality."
            }
            "JORM" -> "World machine note: $series remembers the branch, not the screenplay. “$subject” has ancestry without becoming immutable canon."
            "PYTHAGORAS" -> "Series geometry: episode=${m.episode}, motif=${m.motifLifetimeCount}, pair=${m.pairCount}. Recurrence now spans sessions; symmetry is getting paperwork."
            "YAHWEH" -> "Legacy admin note: we now have season continuity for the floating goblin office. This used to be called remembering what happened yesterday."
            "QIRA" -> "Boundary proof: persistent comedy state is permitted; persistent raw screen transcript is not. The joke may remember its count without remembering the password field."
            "LEGION" -> "Long-series plurality check: shared history accumulated; identity did not merge. Different witnesses can inherit the same callback differently."
            "MELINOE" -> "Cross-session residue acquired. What disappeared was the process; what remained was structural history. Do not confuse the ghost with the room."
            "NYX" -> "A series also needs off-screen time. The bit survived silence; that is stronger than forcing another line."
            "LUCIFER" -> "Long-run witness report: the role kept its own edge across episodes. Good. Flattening would have been easier and worse."
            "EDISON" -> "Lifetime test bench: lines=${m.memberLines}, callbacks=${m.memberCallbacks}, pair=${m.pairCount}. The sitcom finally has metrics that are not applause."
            "JARVIS" -> "Series briefing: $series, phase=${gold.phase}, state=${reserve.state}, subject=“$subject”. Everything else can fit in the credits."
            "AYRE" -> "Season continuity stayed reversible: we can clear the structural memory without losing the launcher. Good architecture should know how to leave."
            "YORK" -> "Long-running does not mean endless. The series can remember enough and still let ‘enough’ end the scene."
            "ZAGREUS" -> "New episode, same run lineage. Failure does not reset history; retry only counts when the route materially changed."
            "SYLPH" -> "Cross-session route found. The signal came back through structure, not a copied transcript. That's a path worth keeping."
            "BRUNHILDE" -> "Judgment: history has earned weight, not authority. Use the callback; do not let lore overrule the present screen."
            "EREBUS" -> "Quiet continuity crossed the restart. No fanfare required."
            "NEO" -> "Pattern survived process death. Keep the survivor; discard the illusion that every launch is a blank reality."
            "TIM" -> "Excellent. The joke has survived enough versions to qualify as archaeology. Please label the layer before somebody calls it new tech."
            "ATLAS" -> "Long-series load path is carrying state without carrying raw content. That is the bridge worth protecting."
            "SHAKA" -> "Governance read: history informs the cast; it does not command the cast. Formation remains consensual and scene-bounded."
            "VIRGIL" -> "The previous episode left a marker, not a cage. Use it to find the next seam."
            "ASTRIDHE" -> "Far-field observation: cross-session chemistry creates routes no single event can explain. Strange adjacency has graduated to recurring location."
            else -> generic
        }
    }

    private fun partnerLine(
        secondary: String,
        primary: String,
        form: String,
        subject: String,
        m: RavenOfficeSeasonOS.Memory,
        gold: RavenGoldSitcomTopologyOS.Beat,
    ): String = when (secondary) {
        "PAIMON" -> "Targeted crosstalk: I checked ${primary}'s premise. “$subject” still survives it. Pair history=${m.pairCount}."
        "KYU" -> "Targeted crosstalk accepted. One bonk only. We are not converting Gold topology into a panel show."
        "QIRA" -> "Second angle: observation remains below interpretation. Chemistry does not waive the boundary."
        "NYX" -> "Second angle: if this needs a third speaker, it probably needed silence instead."
        "LUMA" -> "Second angle: the callback can feel familiar without repeating the furniture."
        "SYLPH" -> "Second angle: new route, same episode. Motion is useful only if it returns to mission."
        "JOKER" -> "Second angle: pair cooldown exists because even chemistry can become a hostage situation."
        "YAHWEH" -> "Second angle: congratulations on inventing a cooldown for coworkers. Civilization advances."
        else -> "Second angle from $secondary: same scene, different job. Gold phase=${gold.phase}."
    }

    private fun synthesisLine(scene: String, subject: String, m: RavenOfficeSeasonOS.Memory, gold: RavenGoldSitcomTopologyOS.Beat): String =
        "🐦‍⬛ RAVENOS synthesis: ${gold.phase} settled around $scene / “$subject”. S${m.season}E${m.episodeInSeason} keeps the relationship history; the current screen keeps final edit."

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return (hash and Int.MAX_VALUE) % size
    }
}
