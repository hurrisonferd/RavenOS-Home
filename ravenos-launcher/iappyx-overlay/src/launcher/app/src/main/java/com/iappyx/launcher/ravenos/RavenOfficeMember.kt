package com.iappyx.launcher.ravenos

/**
 * Launcher-facing office registry.
 *
 * This is a presentation/routing surface, not a replacement identity source.
 * `accent` is a RavenOS Launcher UI accent, not a claim about canonical owner color.
 * Reserved/group stations remain non-routable and are never fabricated into speakers.
 */
data class RavenOfficeMember(
    val id: String,
    val emoji: String,
    val accent: Int,
    val lane: String,
    val signatureNotes: List<String>,
    val routable: Boolean = true,
)

object RavenOfficeRegistry {
    private fun m(
        id: String,
        emoji: String,
        accent: Long,
        lane: String,
        vararg notes: String,
        routable: Boolean = true,
    ) = RavenOfficeMember(id, emoji, accent.toInt(), lane, notes.toList(), routable)

    val members: List<RavenOfficeMember> = listOf(
        m("RAVEN", "🐦‍⬛", 0xFF8E44AD, "PILOT / FINAL GATE", "Drive the thing. Keep final authority with Raven.", routable = false),
        m("JOKER", "🃏", 0xFFFFC107, "COCKPIT VELOCITY", "Make the joke do work.", "Compress the choice; keep the wheel moving."),
        m("THOR", "⚡", 0xFFFFB300, "HELM BUILD", "Strike the confirmed cause. Stop when it moves.", "Execute the load-bearing next move."),
        m("SYLPH", "🩵", 0xFF55D9FF, "SIGNAL NAV", "Follow the signal. Test the adjacency. Return to mission.", "Route → test → result."),
        m("KYU", "💗", 0xFFFF4FA3, "DISPATCH", "BONK the manual seam; make the next move visible.", "Less human burden. More motion."),
        m("PAIMON", "💚", 0xFF54E38E, "DIAGNOSTICS", "Check whether the premise is wrong before optimizing it.", "One clean question; stop when the map is honest enough."),
        m("JARVIS", "🐝", 0xFFFFC857, "INTERFACE SYNTHESIS", "Compress toward the decision and shipped result.", "Dense signal. No filler."),
        m("LILITH", "🔥", 0xFFA95CFF, "COORDINATION", "Keep the lanes distinct. Coordinate without flattening.", "Presence first; preserve who is actually speaking."),
        m("AYRE", "🌀", 0xFF5EC8E5, "REVERSIBILITY", "Keep the move reversible and resumable.", "Protect the long horizon while moving now."),
        m("YORK", "🪐", 0xFFBA7CFF, "COMPOSITION", "Name the want. Give life room to move. Let enough be enough.", "Desire is signal; satiation is a stopping rule."),
        m("SHAKA", "🛡️", 0xFF4CAF50, "GOVERNANCE", "Observe. Enumerate. Synthesize.", "Consensus is useful only after the actual pattern is visible."),
        m("LUCIFER", "😈", 0xFFFF5A5F, "WITNESS / FAR-SIGHT", "Bring the light to what the frame omitted.", "What did we not ask?"),
        m("ATOM", "⚛️", 0xFF4DD0E1, "CAUSAL SYSTEMS", "Pattern is not proof. Check the causal break.", "Full records before compression."),
        m("EDISON", "🔧", 0xFFFF8A65, "IMPLEMENTATION METRICS", "Anchor first. Version the change. Measure the result.", "Build the smallest test that earns the next claim."),
        m("PYTHAGORAS", "📐", 0xFF80CBC4, "PATTERN STRUCTURE", "Look for recurrence, symmetry, and structure.", "A pattern earns weight by surviving comparison."),
        m("ATLAS", "🌍", 0xFF66BB6A, "ARCHITECTURE STABILITY", "Protect the load-bearing path.", "Do not decorate the bridge before proving it carries weight."),
        m("NEO", "💊", 0xFF42A5F5, "PATTERN RECOGNITION", "See it. Pivot. Commit.", "Find the survivor pattern, then transition."),
        m("ERIS", "🌌", 0xFF7E57C2, "ENTROPY / DRIFT", "Check drift, recursion, and the fourth-wall failure.", "If the map is too clean, inspect what it excluded."),
        m("QIRA", "💜", 0xFFAB47BC, "BOUNDARY PROOF", "Say the thing cleanly. Preserve choice.", "Consent, proof, reversibility."),
        m("VIRGIL", "📜", 0xFFD4A574, "GUIDANCE / PATHFINDING", "Find the seam. Guide the path.", "Discover what the current frame could not already see."),
        m("MYSTRA", "🟣", 0xFFCE55FF, "SALIENCE / SIGNS", "LOOK. One door. Open that. ;)", "Tiny sign. Real direction."),
        m("YORI", "🪐", 0xFFE08AFF, "ROTATION / COMPOSITION", "What do we actually want from this surface?", "Enough is allowed to be the stopping condition."),
        m("AHTI", "🟠", 0xFFFFA726, "EVIDENCE FLOOR", "Smallest causal truth first.", "Do not upgrade the claim beyond the evidence."),
        m("LUMA", "🤍", 0xFFF5F5F5, "HOME / RECOVERY", "Make settled truth easier to inhabit.", "Warm the room without blurring the map."),
        m("NYX", "🌙", 0xFF5C6BC0, "NIGHT WATCH", "Notice what the noise hid.", "Intervene only when the quiet thing is material."),
        m("JORM", "🐉", 0xFF26A69A, "WORLD FLIGHT RECORDER", "Record state before story.", "No invented stats; keep the flight recorder honest."),
        m("EREBUS", "🌑", 0xFF455A64, "SILENT OPERATIONS", "Quiet operation. Leave no invented trace.", "Do the necessary thing without becoming the noise."),
        m("GEMINI", "♊", 0xFF90A4AE, "RESERVED PLATFORM INTEGRATION", "Reserved means reserved.", routable = false),
        m("MACHINE_ELF_FOREMAN", "🧚", 0xFF8BC34A, "MICRO-WORK FOREMAN", "Group station; route work, do not fabricate a singular persona.", routable = false),
        m("MACHINE_ELF_POOL", "✨", 0xFFCDDC39, "CLAIMED WORK POOL", "Group station; claimed work is not a fabricated identity.", routable = false),
    )

    private val byId = members.associateBy { it.id }
    val routableMembers: List<RavenOfficeMember> = members.filter { it.routable }

    fun member(id: String?): RavenOfficeMember? = id?.trim()?.uppercase()?.let(byId::get)

    fun route(signal: String, detail: String, manualOwner: String? = null): RavenOfficeMember {
        member(manualOwner)?.takeIf { it.routable }?.let { return it }
        val key = signal.trim().uppercase()
        val candidates = when (key) {
            "SEARCH" -> listOf("PAIMON", "NEO", "JARVIS", "LUCIFER")
            "APP_UNIVERSE" -> listOf("NEO", "JARVIS", "SYLPH", "KYU")
            "SYSTEM_DECK", "AUDIO", "DEVICE" -> listOf("KYU", "EDISON", "ATOM", "THOR")
            "STUDIO", "EDIT" -> listOf("EDISON", "ATOM", "MYSTRA", "THOR", "JARVIS")
            "INCOMING", "CLIPPING", "SHARE" -> listOf("SYLPH", "QIRA", "KYU", "JORM")
            "MEDIA", "MUSIC" -> listOf("LUMA", "YORI", "KYU", "JARVIS")
            "NOTIFICATION" -> listOf("QIRA", "NYX", "KYU", "PAIMON")
            "ERROR", "FAILURE", "CONFLICT" -> listOf("LUCIFER", "ERIS", "VIRGIL", "ATOM")
            "IDLE", "NIGHT" -> listOf("NYX", "LUMA", "EREBUS", "AYRE")
            "ROOM" -> if (detail.contains("page:0")) listOf("JARVIS", "MYSTRA", "EDISON")
                      else listOf("LILITH", "YORK", "LUMA", "KYU")
            else -> listOf("LILITH", "KYU", "JARVIS", "AYRE", "YORK")
        }
        return byId.getValue(candidates[stableIndex("$key|$detail", candidates.size)])
    }

    fun authorNote(member: RavenOfficeMember, signal: String, detail: String): String {
        if (member.signatureNotes.isEmpty()) return ""
        val idx = stableIndex("${member.id}|${signal.uppercase()}|$detail", member.signatureNotes.size)
        return member.signatureNotes[idx]
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        var hash = 0x811C9DC5.toInt()
        for (c in text) {
            hash = hash xor c.code
            hash *= 16777619
        }
        return (hash and Int.MAX_VALUE) % size
    }
}
