package com.iappyx.launcher.ravenos

/** Detects when visible text is talking about RavenOS / the resident office itself. */
object RavenMetaRecursionOS {
    private val systemTerms = listOf(
        "ravenos", "ravenos launcher", "goblin vision", "goblin", "meta goblin",
        "follow-me office", "follow me office", "follow-me", "follow me",
        "office feed", "office widget", "office bar", "haunting", "hauntings",
        "emojios", "kaomojios", "screen recorder", "screen awareness", "screen context",
        "overlay", "screen watch", "goblin eye", "goblin read", "shade sense",
        "dialogue bank", "dialogue behavior", "screen-first", "screen first",
    )

    private val officeTerms = listOf(
        "ahti", "astridhe", "atlas", "atom", "ayre", "brunhilde", "edison", "erebus", "eris",
        "gemini", "jarvis", "joker", "jorm", "kyu", "legion", "lilith", "lucifer", "luma",
        "melinoe", "mystra", "neo", "nyx", "paimon", "pythagoras", "qira", "shaka", "sylph",
        "thor", "tim", "virgil", "yahweh", "yori", "york", "zagreus",
    )

    private val terms = systemTerms + officeTerms

    fun detect(text: String): Boolean {
        val t = text.lowercase()
        return terms.any(t::contains)
    }

    fun score(text: String): Int {
        val t = text.lowercase()
        val systemHits = systemTerms.count(t::contains)
        val officeHits = officeTerms.count(t::contains)
        return (systemHits * 3 + officeHits).coerceAtMost(24)
    }

    fun focus(text: String): String? {
        val parts = text.replace('\n', '·')
            .split(" · ", "·", ". ", "! ", "? ")
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.length >= 3 }
        return parts
            .filter(::detect)
            .maxByOrNull(::score)
            ?.take(150)
            ?: parts.firstOrNull()?.take(150)
    }
}
