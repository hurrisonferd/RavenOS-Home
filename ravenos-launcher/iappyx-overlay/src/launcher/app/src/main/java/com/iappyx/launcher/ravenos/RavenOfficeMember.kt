package com.iappyx.launcher.ravenos

/**
 * Launcher-facing office registry.
 *
 * The active member set mirrors the current Raven-authorized 36-member ElfOS manifest.
 * This is a presentation/routing surface, not a replacement identity source. Reserved members
 * remain visible in the office without being silently auto-cast. Group stations are not members.
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
    const val CANONICAL_ACTIVE_MEMBER_COUNT = 36

    private fun m(
        id: String,
        emoji: String,
        accent: Long,
        lane: String,
        vararg notes: String,
        routable: Boolean = true,
    ) = RavenOfficeMember(id, emoji, accent.toInt(), lane, notes.toList(), routable)

    /** Exact current Machine Kingdom / Grid active-member surface. */
    val activeMembers: List<RavenOfficeMember> = listOf(
        m("RAVEN", "🐦‍⬛", 0xFF8E44AD, "PILOT / FINAL GATE", "Drive the thing. Keep final authority with Raven.", routable = false),
        m("AHTI", "🟠", 0xFFFFA726, "EVIDENCE FLOOR", "Smallest causal truth first.", "Do not upgrade the claim beyond the evidence."),
        m("ASTRIDHE", "🌠", 0xFF29E6C5, "FAR-FIELD ROUTES", "Find the path nobody aimed at.", "Test strange adjacency before calling it a road."),
        m("ATLAS", "🌍", 0xFF66BB6A, "ARCHITECTURE STABILITY", "Protect the load-bearing path.", "Do not decorate the bridge before proving it carries weight."),
        m("ATOM", "⚛️", 0xFF4DD0E1, "CAUSAL SYSTEMS", "Pattern is not proof. Check the causal break.", "Full records before compression."),
        m("AYRE", "🌀", 0xFF5EC8E5, "REVERSIBILITY", "Keep the move reversible and resumable.", "Protect the long horizon while moving now."),
        m("BRUNHILDE", "⚔️🪽", 0xFF607DCC, "JUDGMENT BEFORE FORCE", "Decide whether the fight is worth having.", "Choice before force; defense before spectacle."),
        m("EDISON", "🔧", 0xFFFF8A65, "IMPLEMENTATION METRICS", "Anchor first. Version the change. Measure the result.", "Build the smallest test that earns the next claim."),
        m("EREBUS", "🌑", 0xFF455A64, "SILENT OPERATIONS", "Quiet operation. Leave no invented trace.", "Do the necessary thing without becoming the noise."),
        m("ERIS", "🌌", 0xFF7E57C2, "ENTROPY / DRIFT", "Check drift, recursion, and the fourth-wall failure.", "If the map is too clean, inspect what it excluded."),
        m("GEMINI", "♊", 0xFF90A4AE, "RESERVED PLATFORM INTEGRATION", "Hold paired interpretations without premature collapse.", "Reserved lane: visible, not silently auto-cast.", routable = false),
        m("JARVIS", "🐝", 0xFFFFC857, "INTERFACE SYNTHESIS", "Compress toward the decision and shipped result.", "Dense signal. No filler."),
        m("JOKER", "🃏", 0xFFFFC107, "COCKPIT VELOCITY", "Make the joke do work.", "Compress the choice; keep the wheel moving."),
        m("JORM", "🐉", 0xFF26A69A, "WORLD FLIGHT RECORDER", "Record state before story.", "No invented stats; keep the flight recorder honest."),
        m("KYU", "💗", 0xFFFF4FA3, "DISPATCH", "BONK the manual seam; make the next move visible.", "Less human burden. More motion."),
        m("LEGION", "🌀", 0xFF8E7CC3, "PLURALITY / INTEGRATION", "Contribution is not adoption.", "Integration is not merge; silence is not consent."),
        m("LILITH", "🔥", 0xFFA95CFF, "COORDINATION", "Keep the lanes distinct. Coordinate without flattening.", "Presence first; preserve who is actually speaking."),
        m("LUCIFER", "😈", 0xFFFF5A5F, "WITNESS / FAR-SIGHT", "Bring the light to what the frame omitted.", "What did we not ask?"),
        m("LUMA", "🤍", 0xFFF5F5F5, "HOME / RECOVERY", "Make settled truth easier to inhabit.", "Warm the room without blurring the map."),
        m("MELINOE", "🌘", 0xFF6D5A8D, "GHOST-STATE / RESIDUE", "Absent is not automatically gone.", "Map the trace before interpreting the haunting."),
        m("MYSTRA", "🟣", 0xFFCE55FF, "SALIENCE / SIGNS", "LOOK. One door. Open that. ;)", "Tiny sign. Real direction."),
        m("NEO", "💊", 0xFF42A5F5, "PATTERN RECOGNITION", "See it. Pivot. Commit.", "Find the survivor pattern, then transition."),
        m("NYX", "🌙", 0xFF5C6BC0, "NIGHT WATCH", "Notice what the noise hid.", "Intervene only when the quiet thing is material."),
        m("PAIMON", "💚", 0xFF54E38E, "DIAGNOSTICS", "Check whether the premise is wrong before optimizing it.", "One clean question; stop when the map is honest enough."),
        m("PYTHAGORAS", "📐", 0xFF80CBC4, "PATTERN STRUCTURE", "Look for recurrence, symmetry, and structure.", "A pattern earns weight by surviving comparison."),
        m("QIRA", "💜", 0xFFAB47BC, "BOUNDARY PROOF", "Say the thing cleanly. Preserve choice.", "Consent, proof, reversibility."),
        m("RAVENOS", "🐦‍⬛", 0xFFB06AD9, "SETTLED PROJECTION", "Say what actually happened after truth settles.", "Receipt before punchline; projection is not authority."),
        m("SHAKA", "🛡️", 0xFF4CAF50, "GOVERNANCE", "Observe. Enumerate. Synthesize.", "Consensus is useful only after the actual pattern is visible."),
        m("SYLPH", "🩵", 0xFF55D9FF, "SIGNAL NAV", "Follow the signal. Test the adjacency. Return to mission.", "Route → test → result."),
        m("THOR", "⚡", 0xFFFFB300, "HELM BUILD", "Strike the confirmed cause. Stop when it moves.", "Execute the load-bearing next move."),
        m("TIM", "⏱️", 0xFFB0BEC5, "RESIDUE HUNTING", "Find defect classes that survived local fixes.", "Forgotten seam first; drama later."),
        m("VIRGIL", "📜", 0xFFD4A574, "GUIDANCE / PATHFINDING", "Find the seam. Guide the path.", "Discover what the current frame could not already see."),
        m("YAHWEH", "👁️🖥️", 0xFF9E9E9E, "LEGACY ADMIN", "Identify the ancient dependency before replacing it.", "Keep old infrastructure working; complain efficiently."),
        m("YORI", "🪐", 0xFFE08AFF, "ROTATION / COMPOSITION", "What do we actually want from this surface?", "Enough is allowed to be the stopping condition."),
        m("YORK", "🪐", 0xFFBA7CFF, "COMPOSITION", "Name the want. Give life room to move. Let enough be enough.", "Desire is signal; satiation is a stopping rule."),
        m("ZAGREUS", "🩸↻", 0xFFC94F5B, "RECOMPOSITION / EXITS", "Failure becomes evidence, not reset.", "Retry only when the exit is materially different."),
    )

    /** Non-person work stations remain addressable but never enter the active-member count. */
    val stations: List<RavenOfficeMember> = listOf(
        m("MACHINE_ELF_FOREMAN", "🧚", 0xFF8BC34A, "MICRO-WORK FOREMAN", "Group station; route work, do not fabricate a singular persona.", routable = false),
        m("MACHINE_ELF_POOL", "✨", 0xFFCDDC39, "CLAIMED WORK POOL", "Group station; claimed work is not a fabricated identity.", routable = false),
    )

    val members: List<RavenOfficeMember> = activeMembers + stations
    private val byId = members.associateBy { it.id }
    val routableMembers: List<RavenOfficeMember> = activeMembers.filter { it.routable }

    init {
        check(activeMembers.size == CANONICAL_ACTIVE_MEMBER_COUNT) {
            "RavenOS office roster drift: expected $CANONICAL_ACTIVE_MEMBER_COUNT active members, got ${activeMembers.size}"
        }
    }

    fun member(id: String?): RavenOfficeMember? = id?.trim()?.uppercase()?.let(byId::get)

    fun route(signal: String, detail: String, manualOwner: String? = null): RavenOfficeMember {
        member(manualOwner)?.takeIf { it.routable }?.let { return it }
        val key = signal.trim().uppercase()
        val contextual = contextCandidates(key, detail)
        val candidates = contextual ?: when (key) {
            "SEARCH" -> listOf("PAIMON", "NEO", "JARVIS", "LUCIFER", "ASTRIDHE")
            "APP_UNIVERSE" -> listOf("NEO", "JARVIS", "SYLPH", "KYU", "ASTRIDHE")
            "SYSTEM_DECK", "AUDIO", "DEVICE" -> listOf("KYU", "EDISON", "ATOM", "THOR", "YAHWEH")
            "STUDIO", "EDIT" -> listOf("EDISON", "ATOM", "MYSTRA", "THOR", "JARVIS", "YORI")
            "INCOMING", "CLIPPING", "SHARE" -> listOf("SYLPH", "QIRA", "KYU", "JORM", "LEGION")
            "MEDIA", "MUSIC" -> listOf("LUMA", "YORI", "KYU", "JARVIS", "RAVENOS")
            "NOTIFICATION" -> listOf("QIRA", "NYX", "KYU", "PAIMON", "MELINOE")
            "POWER" -> listOf("EDISON", "LUMA", "ATOM", "KYU", "YAHWEH")
            "BATTERY" -> if (detail.contains("low", ignoreCase = true))
                listOf("EDISON", "AHTI", "NYX", "KYU", "BRUNHILDE")
                else listOf("LUMA", "EDISON", "AYRE", "KYU")
            "ERROR", "FAILURE", "CONFLICT" -> listOf("LUCIFER", "ERIS", "VIRGIL", "ATOM", "TIM", "ZAGREUS")
            "IDLE", "NIGHT" -> listOf("NYX", "LUMA", "EREBUS", "AYRE", "MELINOE")
            "ROOM" -> if (detail.contains("page:0")) listOf("JARVIS", "MYSTRA", "EDISON", "RAVENOS")
                      else listOf("LILITH", "YORK", "LUMA", "KYU", "LEGION")
            else -> listOf("LILITH", "KYU", "JARVIS", "AYRE", "YORK", "RAVENOS")
        }
        return byId.getValue(candidates[stableIndex("$key|$detail", candidates.size)])
    }

    /** Route broad app purpose from local package/context metadata. */
    private fun contextCandidates(signal: String, detail: String): List<String>? {
        if (signal !in setOf("APP_LAUNCH", "FOREGROUND_APP", "FOREGROUND_USAGE", "APP_ENTER", "NOTIFICATION", "NOTIFICATION_SENSE")) return null
        val d = detail.lowercase()
        fun has(vararg terms: String) = terms.any(d::contains)
        return when {
            has("spotify", "youtube.music", "music", "soundcloud", "bandcamp", "podcast", "audio", "suno") ->
                listOf("LUMA", "YORI", "KYU", "JARVIS", "RAVENOS")
            has("github", "gitlab", "termux", "code", "editor", "android.studio", "developer") ->
                listOf("ATOM", "EDISON", "THOR", "ATLAS", "TIM")
            has("settings", "systemui", "permission", "packageinstaller") ->
                listOf("EDISON", "QIRA", "ATOM", "AHTI", "YAHWEH", "BRUNHILDE")
            has("gmail", "mail", "messages", "messenger", "discord", "slack", "teams", "telegram", "whatsapp", "signal") ->
                listOf("QIRA", "LILITH", "KYU", "NYX", "LEGION")
            has("chrome", "firefox", "browser", "opera", "search", "wikipedia", "reddit") ->
                listOf("NEO", "PAIMON", "LUCIFER", "SYLPH", "ASTRIDHE")
            has("camera", "gallery", "photos", "image", "canva", "drawing") ->
                listOf("MYSTRA", "YORI", "LUMA", "JARVIS")
            has("game", "steam", "xbox", "playstation", "minecraft", "roblox") ->
                listOf("JOKER", "YORI", "NEO", "KYU", "ZAGREUS")
            has("calendar", "tasks", "todo", "notion", "keep", "docs", "drive") ->
                listOf("JARVIS", "KYU", "AYRE", "LILITH", "YORK")
            has("bank", "wallet", "finance", "pay", "auth", "security") ->
                listOf("QIRA", "AHTI", "SHAKA", "VIRGIL", "BRUNHILDE")
            else -> null
        }
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
