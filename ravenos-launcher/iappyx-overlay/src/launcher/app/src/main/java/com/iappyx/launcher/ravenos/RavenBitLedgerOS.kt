package com.iappyx.launcher.ravenos

import java.util.LinkedHashMap

/**
 * Process-session comedy memory for Machine Kingdom Office.
 *
 * Stores structural bit identity/counters/forms only. It never stores screenshots, raw OCR,
 * editable values or a dialogue transcript. The same running bit can therefore evolve from setup
 * to callback to escalation to a delayed brick joke without repeating the same sentence.
 */
object RavenBitLedgerOS {
    data class Cue(
        val id: String,
        val label: String,
        val count: Int,
        val tier: String,
        val returning: Boolean,
        val brick: Boolean,
        val previousOwner: String,
        val turnGap: Int,
        val lastForm: String,
        val shouldEscalate: Boolean,
    ) {
        val active: Boolean get() = id.isNotBlank()
    }

    private data class State(
        var count: Int,
        var lastTurn: Int,
        var lastOwner: String,
        var previousOwner: String,
        var lastForm: String,
        var lastAt: Long,
    )

    private val bits = LinkedHashMap<String, State>()
    private const val MAX_BITS = 48

    @Synchronized
    fun observe(
        screen: RavenScreenContextOS.Snapshot,
        script: RavenEpisodeScriptOS.Cue,
        direction: RavenSitcomDirectorOS.Direction,
        now: Long = System.currentTimeMillis(),
    ): Cue {
        val id = bitId(screen, script)
        if (id.isBlank()) return Cue("", "", 0, "NONE", false, false, "", 0, "", false)

        val label = labelFor(id, script)
        val old = bits[id]
        val gap = old?.let { (direction.turn - it.lastTurn).coerceAtLeast(0) } ?: 0
        val returning = old != null && gap >= 3
        val brick = old != null && old.count >= 2 && gap >= 10
        val nextCount = ((old?.count ?: 0) + 1).coerceAtMost(99)
        val tier = when {
            brick -> "BRICK_JOKE"
            nextCount >= 13 -> "MYTHOLOGY"
            nextCount >= 5 -> "ESCALATION"
            nextCount >= 2 -> "CALLBACK"
            else -> "SETUP"
        }
        val shouldEscalate = brick || nextCount in setOf(2, 3, 5, 8, 13, 21)
        val state = old ?: State(0, 0, "", "", "", 0L)
        state.count = nextCount
        state.previousOwner = state.lastOwner
        state.lastOwner = direction.primary.id
        state.lastTurn = direction.turn
        state.lastAt = now
        bits.remove(id)
        bits[id] = state
        trim()
        return Cue(
            id = id,
            label = label,
            count = nextCount,
            tier = tier,
            returning = returning,
            brick = brick,
            previousOwner = state.previousOwner,
            turnGap = gap,
            lastForm = state.lastForm,
            shouldEscalate = shouldEscalate,
        )
    }

    @Synchronized
    fun markUsed(cue: Cue, form: String, owner: String) {
        if (!cue.active) return
        bits[cue.id]?.let {
            it.lastForm = form
            it.previousOwner = it.lastOwner
            it.lastOwner = owner
        }
    }

    @Synchronized
    fun clear() = bits.clear()

    @Synchronized
    fun compact(): String {
        if (bits.isEmpty()) return "BITS=QUIET"
        val hottest = bits.entries.maxByOrNull { it.value.count } ?: return "BITS=QUIET"
        return "BITS=${bits.size} hot=${hottest.key.take(36)}×${hottest.value.count} form=${hottest.value.lastForm.ifBlank { "none" }}"
    }

    private fun bitId(screen: RavenScreenContextOS.Snapshot, script: RavenEpisodeScriptOS.Cue): String {
        return when {
            script.motif.isNotBlank() -> "MOTIF:${script.motif}"
            script.interaction == "SELECT" && script.interactionTarget.isNotBlank() ->
                "SELECT:${screen.semanticKind}:${normalize(script.interactionTarget).take(54)}"
            script.interaction == "SCROLL" && screen.semanticKind.isNotBlank() -> "SCROLL:${screen.semanticKind}"
            script.returned && script.sceneOwner.isNotBlank() -> "RETURN:${normalize(script.sceneOwner)}"
            screen.meta && screen.semanticKind.isNotBlank() -> "META:${screen.semanticKind}"
            script.interruption.isNotBlank() && script.sceneOwner.isNotBlank() ->
                "CAMEO:${normalize(script.sceneOwner)}:${normalize(script.interruption)}"
            else -> ""
        }
    }

    private fun labelFor(id: String, script: RavenEpisodeScriptOS.Cue): String = when {
        id.startsWith("MOTIF:") -> id.substringAfter(':').lowercase().replace('_', ' ')
        id.startsWith("SELECT:") -> "selected ${script.interactionTarget.take(64).ifBlank { "thing" }}"
        id.startsWith("SCROLL:") -> "scrolling ${script.sceneOwner.ifBlank { "the scene" }}"
        id.startsWith("RETURN:") -> "returning to ${script.sceneOwner.ifBlank { "the scene" }}"
        id.startsWith("META:") -> "the office noticing itself"
        id.startsWith("CAMEO:") -> "${script.interruption.ifBlank { "Android" }} interrupting ${script.sceneOwner.ifBlank { "the scene" }}"
        else -> id.lowercase().replace('_', ' ')
    }

    private fun normalize(text: String): String = text.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun trim() {
        while (bits.size > MAX_BITS) {
            val first = bits.entries.firstOrNull()?.key ?: break
            bits.remove(first)
        }
    }
}
