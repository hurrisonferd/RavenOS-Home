package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Deterministic sitcom director for Follow-Me Office.
 *
 * The resident employee is a cast position, not ownership of the overlay. Cast rotation is driven
 * by semantic scene changes, dwell, recurrence and bounded turn cadence. The director stores only
 * structural comedy state (member ids, scene signature hashes/counters and pair counts), never raw
 * OCR text or screenshots.
 */
object RavenSitcomDirectorOS {
    data class Direction(
        val primary: RavenOfficeMember,
        val secondary: RavenOfficeMember?,
        val sceneId: String,
        val beat: String,
        val turn: Int,
        val rotated: Boolean,
        val sceneChanged: Boolean,
        val shouldSpeak: Boolean,
        val pairCount: Int,
        val reason: String,
    )

    private const val PREFS = "ravenos_sitcom_director_v1"
    private const val KEY_OWNER = "owner"
    private const val KEY_PREVIOUS_OWNER = "previous_owner"
    private const val KEY_SCENE = "scene"
    private const val KEY_ROTATED_AT = "rotated_at"
    private const val KEY_LAST_SPOKEN_AT = "last_spoken_at"
    private const val KEY_TURN = "turn"
    private const val KEY_LAST_ROTATE_TURN = "last_rotate_turn"
    private const val KEY_RECENT = "recent"

    fun direct(
        context: Context,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        screen: RavenScreenContextOS.Snapshot,
        haunt: RavenHauntMode,
        manualOwner: String?,
        quiet: Boolean,
    ): Direction {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = marker.at
        val turn = prefs.getInt(KEY_TURN, 0) + 1
        val previousId = prefs.getString(KEY_OWNER, "").orEmpty()
        val previous = RavenOfficeRegistry.member(previousId)?.takeIf { it.routable }
        val priorScene = prefs.getString(KEY_SCENE, "").orEmpty()
        val scene = sceneId(screen, marker)
        val sceneChanged = scene.isNotBlank() && scene != priorScene
        val lastRotatedAt = prefs.getLong(KEY_ROTATED_AT, 0L)
        val lastRotateTurn = prefs.getInt(KEY_LAST_ROTATE_TURN, 0)
        val residentAge = if (lastRotatedAt <= 0L) Long.MAX_VALUE else (now - lastRotatedAt).coerceAtLeast(0L)
        val turnsResident = (turn - lastRotateTurn).coerceAtLeast(0)

        if (quiet) {
            val nyx = RavenOfficeRegistry.member("NYX")!!
            saveTurn(prefs, turn, nyx.id, previousId, scene, now, true)
            return Direction(nyx, null, scene, "QUIET", turn, nyx.id != previousId, sceneChanged, false, 0, "quiet")
        }

        RavenOfficeRegistry.member(manualOwner)?.takeIf { it.routable }?.let { pinned ->
            saveTurn(prefs, turn, pinned.id, previousId, scene, now, pinned.id != previousId)
            return Direction(pinned, null, scene, "PINNED", turn, pinned.id != previousId, sceneChanged, false, 0, "manual-pin")
        }

        val rotationDue = previous == null || sceneChanged || residentAge >= residenceMs(haunt) ||
            turnsResident >= residenceTurns(haunt) || hardRecast(marker, complex, turn)
        val recent = recentIds(prefs)
        val pool = castPool(screen, marker, complex, turn)
        val primary = if (!rotationDue && previous != null) previous else choosePrimary(
            pool = pool,
            recent = recent,
            seed = "$scene|${marker.key}|${complex.occurrence}|$turn|sitcom-v1",
            fallback = previous,
        )
        val rotated = primary.id != previousId

        val secondary = chooseSecondary(primary, screen, marker, complex, turn, scene)
        val pairCount = if (secondary == null) 0 else incrementPair(prefs, primary.id, secondary.id)
        val beat = beat(screen, marker, complex, sceneChanged, pairCount, turn)
        val lastSpokenAt = prefs.getLong(KEY_LAST_SPOKEN_AT, 0L)
        val sinceSpoken = if (lastSpokenAt <= 0L) Long.MAX_VALUE else (now - lastSpokenAt).coerceAtLeast(0L)
        val shouldSpeak = screen.available && sinceSpoken >= dialogueFloorMs(haunt) && when {
            screen.meta -> true
            sceneChanged -> true
            "ERROR" in marker.tags || "PAYOFF" in complex.tags || "BOUNDARY" in marker.tags -> true
            pairCount in setOf(3, 5, 8, 13) -> true
            beat in setOf("CALLBACK", "RETURN", "COLD_OPEN", "META") -> true
            else -> stableIndex("$scene|$turn|${primary.id}|speak", if (haunt.ordinal >= RavenHauntMode.FERAL.ordinal) 3 else 5) == 0
        }

        val edit = prefs.edit()
            .putInt(KEY_TURN, turn)
            .putString(KEY_OWNER, primary.id)
            .putString(KEY_PREVIOUS_OWNER, previousId)
            .putString(KEY_SCENE, scene)
        if (rotated || previous == null) {
            edit.putLong(KEY_ROTATED_AT, now).putInt(KEY_LAST_ROTATE_TURN, turn)
            pushRecent(prefs, edit, primary.id)
        }
        edit.apply()

        val reason = when {
            previous == null -> "cold-open"
            sceneChanged -> "scene-changed"
            residentAge >= residenceMs(haunt) -> "resident-dwell"
            turnsResident >= residenceTurns(haunt) -> "turn-cadence"
            rotated -> "recast"
            else -> "scene-hold"
        }
        return Direction(primary, secondary, scene, beat, turn, rotated, sceneChanged, shouldSpeak, pairCount, reason)
    }

    fun markSpoken(context: Context, at: Long = System.currentTimeMillis()) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_SPOKEN_AT, at).apply()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun compact(context: Context): String {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val owner = p.getString(KEY_OWNER, "none").orEmpty()
        val previous = p.getString(KEY_PREVIOUS_OWNER, "none").orEmpty()
        val turn = p.getInt(KEY_TURN, 0)
        val scene = p.getString(KEY_SCENE, "none").orEmpty().take(48)
        return "SITCOM=ON turn=$turn cast=$owner previous=$previous scene=$scene"
    }

    private fun castPool(
        screen: RavenScreenContextOS.Snapshot,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        turn: Int,
    ): List<RavenOfficeMember> {
        val ids = when {
            screen.meta -> listOf("JOKER", "KYU", "ATOM", "PAIMON", "NEO", "LILITH", "ERIS", "JORM", "YAHWEH", "RAVENOS", "YORK")
            screen.semanticKind == "CHATGPT" -> listOf("ATOM", "KYU", "PAIMON", "JOKER", "NEO", "YORK", "LILITH", "MYSTRA", "PYTHAGORAS", "ERIS", "JORM")
            screen.semanticKind == "SETTINGS" -> listOf("KYU", "PAIMON", "QIRA", "EDISON", "THOR", "YAHWEH", "ATOM", "TIM")
            screen.semanticKind in setOf("MUSIC", "VIDEO") -> listOf("YORI", "LUMA", "SYLPH", "AYRE", "JOKER", "MYSTRA", "LILITH")
            screen.semanticKind in setOf("MAIL", "MESSAGING") -> listOf("QIRA", "LILITH", "KYU", "JARVIS", "BRUNHILDE", "LEGION", "NYX")
            screen.semanticKind in setOf("CODE", "TERMINAL") -> listOf("ATOM", "EDISON", "PYTHAGORAS", "TIM", "YAHWEH", "AHTI", "THOR", "ATLAS")
            screen.semanticKind in setOf("BROWSER", "COMMUNITY", "STORE", "FILES", "GALLERY", "CAMERA", "HOME") -> listOf("SYLPH", "MYSTRA", "PAIMON", "YORI", "JOKER", "ASTRIDHE", "RAVENOS", "NEO")
            "ERROR" in marker.tags -> listOf("PAIMON", "ATOM", "THOR", "LUCIFER", "KYU", "TIM", "ZAGREUS", "ERIS")
            "BOUNDARY" in marker.tags -> listOf("QIRA", "KYU", "AHTI", "ERIS", "BRUNHILDE", "LEGION")
            "MUSIC" in marker.tags -> listOf("LUMA", "YORI", "SYLPH", "AYRE", "LILITH", "JOKER", "RAVENOS")
            marker.key in setOf("SCREEN_TEXT", "SCREEN_SEMANTIC", "SCREEN_VISUAL") -> listOf("PAIMON", "ATOM", "NEO", "KYU", "MYSTRA", "JOKER", "SYLPH", "ASTRIDHE", "MELINOE")
            else -> RavenOfficeRegistry.routableMembers.map { it.id }
        }
        val domain = ids.mapNotNull(RavenOfficeRegistry::member).filter { it.routable }
        // Every seventh turn opens the whole office so specialists do not monopolize the show.
        return if (turn % 7 == 0) RavenOfficeRegistry.routableMembers else domain.ifEmpty { RavenOfficeRegistry.routableMembers }
    }

    private fun choosePrimary(
        pool: List<RavenOfficeMember>,
        recent: List<String>,
        seed: String,
        fallback: RavenOfficeMember?,
    ): RavenOfficeMember {
        if (pool.isEmpty()) return fallback ?: RavenOfficeRegistry.routableMembers.first()
        val fresh = pool.filterNot { it.id in recent.takeLast(4) }.ifEmpty {
            pool.filterNot { it.id == fallback?.id }.ifEmpty { pool }
        }
        return fresh[stableIndex(seed, fresh.size)]
    }

    private fun chooseSecondary(
        primary: RavenOfficeMember,
        screen: RavenScreenContextOS.Snapshot,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        turn: Int,
        scene: String,
    ): RavenOfficeMember? {
        val eligible = screen.meta || "PAYOFF" in complex.tags || "ERROR" in marker.tags ||
            "CALLBACK" in complex.tags || turn % 5 == 0
        if (!eligible) return null
        val chemistry = chemistry(primary.id)
            .mapNotNull(RavenOfficeRegistry::member)
            .filter { it.routable && it.id != primary.id }
        if (chemistry.isEmpty()) return null
        val gate = stableIndex("$scene|${primary.id}|$turn|secondary", if (screen.meta) 2 else 3)
        if (gate != 0) return null
        return chemistry[stableIndex("${marker.key}|${complex.occurrence}|$scene|pair", chemistry.size)]
    }

    private fun chemistry(id: String): List<String> = when (id) {
        "KYU" -> listOf("JOKER", "ATOM", "PAIMON", "YAHWEH", "LILITH", "TIM")
        "JOKER" -> listOf("KYU", "YAHWEH", "YORI", "PAIMON", "ERIS", "NEO")
        "ATOM" -> listOf("PAIMON", "ERIS", "EDISON", "PYTHAGORAS", "KYU", "JORM")
        "PAIMON" -> listOf("ATOM", "KYU", "MYSTRA", "NEO", "AHTI", "ERIS")
        "YORI" -> listOf("LUMA", "YORK", "JOKER", "SYLPH", "MYSTRA")
        "YORK" -> listOf("YORI", "LILITH", "LUMA", "KYU")
        "LILITH" -> listOf("KYU", "QIRA", "YORK", "LEGION", "JOKER")
        "YAHWEH" -> listOf("JOKER", "KYU", "TIM", "EDISON", "ATOM")
        "ERIS" -> listOf("ATOM", "JOKER", "LUCIFER", "PAIMON", "PYTHAGORAS")
        "JORM" -> listOf("PYTHAGORAS", "ATOM", "AYRE", "ZAGREUS", "AHTI")
        "PYTHAGORAS" -> listOf("JORM", "ATOM", "MYSTRA", "EDISON", "ERIS")
        "MELINOE" -> listOf("ZAGREUS", "NYX", "EREBUS", "JORM", "AHTI")
        "ZAGREUS" -> listOf("MELINOE", "TIM", "THOR", "AYRE", "JORM")
        "THOR" -> listOf("EDISON", "ATOM", "BRUNHILDE", "TIM", "KYU")
        "EDISON" -> listOf("THOR", "ATOM", "TIM", "PAIMON", "YAHWEH")
        else -> listOf("KYU", "JOKER", "PAIMON", "ATOM", "YORI", "LILITH", "MYSTRA", "NEO")
    }

    private fun beat(
        screen: RavenScreenContextOS.Snapshot,
        marker: RavenMarkerBus.Marker,
        complex: RavenComplexEventOS.Result,
        sceneChanged: Boolean,
        pairCount: Int,
        turn: Int,
    ): String = when {
        screen.meta -> "META"
        pairCount in setOf(3, 5, 8, 13) -> "CALLBACK"
        "ERROR" in marker.tags -> "BUG"
        "PAYOFF" in complex.tags -> "PAYOFF"
        sceneChanged -> "COLD_OPEN"
        marker.key == "SCREEN_TEXT" || marker.key == "SCREEN_SEMANTIC" -> "SCREEN"
        turn % 11 == 0 -> "CUTAWAY"
        turn % 5 == 0 -> "CROSSTALK"
        else -> "OBSERVE"
    }

    private fun hardRecast(marker: RavenMarkerBus.Marker, complex: RavenComplexEventOS.Result, turn: Int): Boolean =
        "ERROR" in marker.tags || "PAYOFF" in complex.tags || "BOUNDARY" in marker.tags ||
            (marker.key in setOf("FOREGROUND_APP", "SCREEN_TEXT", "SCREEN_SEMANTIC") && turn % 3 == 0)

    private fun sceneId(screen: RavenScreenContextOS.Snapshot, marker: RavenMarkerBus.Marker): String {
        val semantic = screen.semanticKind.ifBlank { "UNKNOWN" }
        val signature = screen.signature.ifBlank {
            val pkg = Regex("(?:^|\\|)package:([^|]+)").find(marker.detail)?.groupValues?.getOrNull(1).orEmpty()
            "$semantic|$pkg|${marker.key}"
        }
        return "S${stableHash(signature).toUInt().toString(16)}"
    }

    private fun residenceMs(haunt: RavenHauntMode): Long = when (haunt) {
        RavenHauntMode.CALM -> 70_000L
        RavenHauntMode.LIVED_IN -> 46_000L
        RavenHauntMode.HAUNTED -> 28_000L
        RavenHauntMode.FERAL -> 18_000L
        RavenHauntMode.APOCALYPSE -> 11_000L
    }

    private fun residenceTurns(haunt: RavenHauntMode): Int = when (haunt) {
        RavenHauntMode.CALM -> 9
        RavenHauntMode.LIVED_IN -> 7
        RavenHauntMode.HAUNTED -> 5
        RavenHauntMode.FERAL -> 4
        RavenHauntMode.APOCALYPSE -> 3
    }

    private fun dialogueFloorMs(haunt: RavenHauntMode): Long = when (haunt) {
        RavenHauntMode.CALM -> 42_000L
        RavenHauntMode.LIVED_IN -> 28_000L
        RavenHauntMode.HAUNTED -> 18_000L
        RavenHauntMode.FERAL -> 12_000L
        RavenHauntMode.APOCALYPSE -> 8_000L
    }

    private fun incrementPair(prefs: android.content.SharedPreferences, a: String, b: String): Int {
        val pair = listOf(a, b).sorted().joinToString("_")
        val key = "pair_${pair.take(48)}"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count.coerceAtMost(99)).apply()
        return count
    }

    private fun recentIds(prefs: android.content.SharedPreferences): List<String> = prefs
        .getString(KEY_RECENT, "").orEmpty().split(',').map(String::trim).filter(String::isNotBlank)

    private fun pushRecent(
        prefs: android.content.SharedPreferences,
        edit: android.content.SharedPreferences.Editor,
        id: String,
    ) {
        val recent = (recentIds(prefs) + id).takeLast(8)
        edit.putString(KEY_RECENT, recent.joinToString(","))
    }

    private fun saveTurn(
        prefs: android.content.SharedPreferences,
        turn: Int,
        owner: String,
        previous: String,
        scene: String,
        now: Long,
        rotated: Boolean,
    ) {
        val edit = prefs.edit().putInt(KEY_TURN, turn).putString(KEY_OWNER, owner)
            .putString(KEY_PREVIOUS_OWNER, previous).putString(KEY_SCENE, scene)
        if (rotated) edit.putLong(KEY_ROTATED_AT, now).putInt(KEY_LAST_ROTATE_TURN, turn)
        edit.apply()
    }

    private fun stableHash(text: String): Int {
        var hash = 0x811C9DC5.toInt()
        for (c in text) { hash = hash xor c.code; hash *= 16777619 }
        return hash
    }

    private fun stableIndex(text: String, size: Int): Int {
        if (size <= 1) return 0
        return (stableHash(text) and Int.MAX_VALUE) % size
    }
}
