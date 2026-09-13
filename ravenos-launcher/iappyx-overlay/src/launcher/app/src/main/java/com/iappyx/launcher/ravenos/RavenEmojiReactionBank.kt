package com.iappyx.launcher.ravenos

/** Deterministic EmojiOS-style reaction accents for launcher meta presentation. */
object RavenEmojiReactionBank {
    private val semantic = mapOf(
        "WATCH" to listOf("👁️","🔍","📡","🛰️"),
        "PROOF" to listOf("🧾","✅","📋","🔎"),
        "ACTION" to listOf("⚡","🔧","🛠️","🚀"),
        "CHAOS" to listOf("🧨","💥","🌀","🚨"),
        "SOFT" to listOf("✨","🌙","☕","🫧"),
        "FAILURE" to listOf("🧯","⚠️","🪦","🫠"),
        "META" to listOf("🎭","🪞","🧠","📺"),
        "OFFICE" to listOf("📋","🗂️","📎","🪑"),
        "RETURN" to listOf("🏠","🔓","👋","🛬"),
        "BOUNDARY" to listOf("🛡️","🚧","🔐","✋"),
        "MUSIC" to listOf("🎧","🎵","📡","🦋")
    )

    fun pick(owner: String, family: String, seed: Int): String {
        val ownerPool = RavenExpressionOwnerBias.forOwner(owner).emoji
        val semanticPool = semantic[family.uppercase()] ?: semantic["WATCH"]!!
        val a = ownerPool[Math.floorMod(seed, ownerPool.size)]
        val b = semanticPool[Math.floorMod(seed / 7, semanticPool.size)]
        return if (a == b) a else "$a$b"
    }
}
