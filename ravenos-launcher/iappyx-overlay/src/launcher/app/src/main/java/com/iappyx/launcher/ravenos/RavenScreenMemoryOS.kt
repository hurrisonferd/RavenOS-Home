package com.iappyx.launcher.ravenos

import java.util.ArrayDeque

/**
 * Process-session memory for semantic screen subjects.
 *
 * This deliberately stays in-memory: it remembers a few short derived subject strings so the
 * Follow-Me Office can notice continuity/returns without persisting a transcript or raw OCR.
 */
object RavenScreenMemoryOS {
    data class Memory(
        val previousFocus: String,
        val previousApp: String,
        val changed: Boolean,
        val returning: Boolean,
        val dwellCount: Int,
        val returnCount: Int,
        val recentSubjects: List<String>,
    )

    private data class Entry(val app: String, val focus: String, val signature: String)

    private val recent = ArrayDeque<Entry>()
    private val returns = linkedMapOf<String, Int>()
    private var currentSignature = ""
    private var currentDwell = 0
    private const val MAX_RECENT = 6
    private const val MAX_RETURN_KEYS = 48

    @Synchronized
    fun observe(screen: RavenScreenContextOS.Snapshot): Memory {
        val previous = recent.peekLast()
        if (!screen.available || screen.signature.isBlank()) {
            return Memory(
                previousFocus = previous?.focus.orEmpty(),
                previousApp = previous?.app.orEmpty(),
                changed = false,
                returning = false,
                dwellCount = currentDwell,
                returnCount = 0,
                recentSubjects = recent.map { it.focus }.toList(),
            )
        }

        val app = screen.appLabel.orEmpty().ifBlank { screen.semanticKind }
        val focus = screen.focus.replace(Regex("\\s+"), " ").trim().take(150)
        val signature = screen.signature.take(220)
        val changed = signature != currentSignature

        if (!changed) {
            currentDwell++
            return Memory(
                previousFocus = previous?.focus.orEmpty(),
                previousApp = previous?.app.orEmpty(),
                changed = false,
                returning = false,
                dwellCount = currentDwell,
                returnCount = returns[signature] ?: 1,
                recentSubjects = recent.map { it.focus }.toList(),
            )
        }

        val seenBefore = recent.any { it.signature == signature }
        val count = (returns[signature] ?: 0) + 1
        returns.remove(signature)
        returns[signature] = count
        while (returns.size > MAX_RETURN_KEYS) {
            val first = returns.entries.firstOrNull()?.key ?: break
            returns.remove(first)
        }

        currentSignature = signature
        currentDwell = 1
        if (previous?.signature != signature) {
            recent.addLast(Entry(app, focus, signature))
            while (recent.size > MAX_RECENT) recent.removeFirst()
        }

        return Memory(
            previousFocus = previous?.focus.orEmpty(),
            previousApp = previous?.app.orEmpty(),
            changed = true,
            returning = seenBefore,
            dwellCount = currentDwell,
            returnCount = count,
            recentSubjects = recent.map { it.focus }.toList(),
        )
    }

    @Synchronized
    fun clear() {
        recent.clear()
        returns.clear()
        currentSignature = ""
        currentDwell = 0
    }
}
