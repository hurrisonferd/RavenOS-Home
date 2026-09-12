package com.iappyx.launcher.ravenos

/** Detects when visible text is talking about RavenOS / the resident office itself. */
object RavenMetaRecursionOS {
    private val terms = listOf(
        "ravenos", "goblin", "follow-me", "follow me", "office feed", "meta goblin",
        "emojios", "kaomojios", "screen recorder", "overlay", "screen watch", "goblin eye",
        "kyu", "paimon", "joker", "atom", "lilith", "yori", "jorm", "neo",
    )

    fun detect(text: String): Boolean {
        val t = text.lowercase()
        return terms.any(t::contains)
    }

    fun focus(text: String): String? {
        val parts = text.replace('\n', '·').split(" · ", "·")
            .map { it.trim() }
            .filter { it.length >= 3 }
        return parts.firstOrNull { detect(it) }?.take(90)
            ?: parts.firstOrNull()?.take(90)
    }
}
