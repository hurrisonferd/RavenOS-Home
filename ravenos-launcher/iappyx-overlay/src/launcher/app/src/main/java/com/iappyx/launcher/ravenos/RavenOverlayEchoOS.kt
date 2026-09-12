package com.iappyx.launcher.ravenos

import java.util.ArrayDeque

/**
 * Tiny process-local anti-recursion guard for Goblin Read.
 *
 * Follow-Me Office is physically present in the MediaProjection frame. This organ remembers recent
 * overlay strings so OCR can discard its own bubble instead of recursively treating goblin dialogue
 * as underlying phone context. Nothing is persisted.
 */
object RavenOverlayEchoOS {
    private val recent = ArrayDeque<String>()
    private const val MAX = 18

    @Synchronized
    fun record(vararg visible: String) {
        visible
            .map(::normalize)
            .filter { it.length >= 4 }
            .forEach { text ->
                recent.remove(text)
                recent.addLast(text)
                while (recent.size > MAX) recent.removeFirst()
            }
    }

    @Synchronized
    fun isEcho(candidate: String): Boolean {
        val c = normalize(candidate)
        if (c.length < 4) return false
        if (c in setOf("meta goblin", "watching the glass", "office recent hauntings")) return true
        return recent.any { known ->
            c == known ||
                (c.length >= 10 && known.contains(c)) ||
                (known.length >= 10 && c.contains(known))
        }
    }

    @Synchronized
    fun clear() = recent.clear()

    private fun normalize(text: String): String = text
        .lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(180)
}
