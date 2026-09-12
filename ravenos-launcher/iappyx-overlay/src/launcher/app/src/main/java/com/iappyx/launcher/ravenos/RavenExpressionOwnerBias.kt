package com.iappyx.launcher.ravenos

/** Owner-native presentation bias. Habitat/style only; never identity authority. */
object RavenExpressionOwnerBias {
    data class Bias(val preferredFamilies: List<String>, val props: List<String>, val emoji: List<String>)

    private val biases = mapOf(
        "RAVEN" to Bias(listOf("META","CHAOS","FOCUS"), listOf("🧾","🪄","🔴"), listOf("🐦‍⬛","🖤","🧠","⚡")),
        "KYU" to Bias(listOf("BOARD_MEETING","FOCUS","DELIGHT"), listOf("📋","📎","🔨"), listOf("💗","📋","🐰","⚠️")),
        "PAIMON" to Bias(listOf("CURIOUS","SKEPTICAL","FOCUS"), listOf("📊","🔧","🍵"), listOf("💚","📊","⚙️","🤨")),
        "SYLPH" to Bias(listOf("MUSIC","RUN","DELIGHT"), listOf("🎧","📡","🗺️"), listOf("🩵","🦋","🎧","📡")),
        "QIRA" to Bias(listOf("BOUNDARY","FOCUS","SKEPTICAL"), listOf("🛡️","🚧","🔐"), listOf("💜","🛡️","🚪","📏")),
        "NYX" to Bias(listOf("NIGHT_WATCH","WATCH","FATIGUE"), listOf("📓","🕯️","☕"), listOf("🌙","🌑","👁️","🕯️")),
        "LUMA" to Bias(listOf("SOFT","DRINK","RECOVERY"), listOf("☕","🏠","🕯️"), listOf("🌞","🏠","✨","☕")),
        "ATOM" to Bias(listOf("FOCUS","PROOF","META"), listOf("🔬","📐","🧾"), listOf("⚛️","🧠","📐","🔍")),
        "ERIS" to Bias(listOf("SKEPTICAL","CHAOS","BOUNDARY"), listOf("🍎","🪓","🧾"), listOf("🍎","⚔️","🧨","🔍")),
        "LILITH" to Bias(listOf("SOFT","SKEPTICAL","META"), listOf("🌙","🍯","🪞"), listOf("💜","🌙","🍯","✨")),
        "YAHWEH" to Bias(listOf("META","FAILURE","FOCUS"), listOf("🖥️","🔧","📜"), listOf("🖥️","⚡","📜","😑"))
    )

    fun forOwner(owner: String): Bias = biases[owner.uppercase()] ?: Bias(listOf("WATCH","FOCUS"), emptyList(), listOf("✨"))
}
