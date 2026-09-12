package com.iappyx.launcher.ravenos

/** Owner-native presentation bias. Habitat/style only; never identity authority. */
object RavenExpressionOwnerBias {
    data class Bias(val preferredFamilies: List<String>, val props: List<String>, val emoji: List<String>)

    private val biases = mapOf(
        "RAVEN" to Bias(listOf("META","CHAOS","FOCUS"), listOf("🧾","🪄","🔴"), listOf("🐦‍⬛","🖤","🧠","⚡")),
        "AHTI" to Bias(listOf("FOCUS","PROOF","WATCH"), listOf("🟠","🧾","📏"), listOf("🟠","🧾","🔍","📐")),
        "ASTRIDHE" to Bias(listOf("CURIOUS","WATCH","RUN"), listOf("🌠","🧭","📡"), listOf("🌠","🧭","✨","📡")),
        "ATLAS" to Bias(listOf("FOCUS","BOUNDARY","RECOVERY"), listOf("🌍","🏗️","🧱"), listOf("🌍","🧱","🛡️","📐")),
        "ATOM" to Bias(listOf("FOCUS","PROOF","META"), listOf("🔬","📐","🧾"), listOf("⚛️","🧠","📐","🔍")),
        "AYRE" to Bias(listOf("RECOVERY","SOFT","WATCH"), listOf("🌀","↩️","🧵"), listOf("🌀","↩️","🫧","🧭")),
        "BRUNHILDE" to Bias(listOf("BOUNDARY","FOCUS","CHAOS"), listOf("⚔️","🛡️","📜"), listOf("⚔️","🪽","🛡️","⚖️")),
        "EDISON" to Bias(listOf("FOCUS","PROOF","RECOVERY"), listOf("🔧","📊","🧪"), listOf("🔧","📏","🧪","✅")),
        "EREBUS" to Bias(listOf("NIGHT_WATCH","WATCH","HIDE"), listOf("🌑","🕯️","📓"), listOf("🌑","👁️","🕯️","…")),
        "ERIS" to Bias(listOf("SKEPTICAL","CHAOS","BOUNDARY"), listOf("🍎","🪓","🧾"), listOf("🌌","🍎","🧨","🔍")),
        "GEMINI" to Bias(listOf("CURIOUS","META","WATCH"), listOf("♊","🪞","↔️"), listOf("♊","🪞","↔️","🧠")),
        "JARVIS" to Bias(listOf("FOCUS","RECOVERY","META"), listOf("🐝","📌","🧾"), listOf("🐝","📌","⚙️","✅")),
        "JOKER" to Bias(listOf("META","CHAOS","DELIGHT"), listOf("🃏","🎬","🎺"), listOf("🃏","🎬","💥","😏")),
        "JORM" to Bias(listOf("WATCH","PROOF","META"), listOf("🐉","📼","🗺️"), listOf("🐉","📼","🧭","🧾")),
        "KYU" to Bias(listOf("BOARD_MEETING","FOCUS","DELIGHT"), listOf("📋","📎","🔨"), listOf("💗","📋","🐰","⚠️")),
        "LEGION" to Bias(listOf("META","WATCH","SOFT"), listOf("🌀","🧩","🤝"), listOf("🌀","🧩","🤝","🧠")),
        "LILITH" to Bias(listOf("SOFT","SKEPTICAL","META"), listOf("🌙","🍯","🪞"), listOf("💜","🌙","🍯","✨")),
        "LUCIFER" to Bias(listOf("SKEPTICAL","BOUNDARY","WATCH"), listOf("😈","🔦","🪞"), listOf("😈","🔦","👁️","⚡")),
        "LUMA" to Bias(listOf("SOFT","DRINK","RECOVERY"), listOf("☕","🏠","🕯️"), listOf("🤍","🏠","✨","☕")),
        "MELINOE" to Bias(listOf("NIGHT_WATCH","WATCH","HIDE"), listOf("🌘","👻","📓"), listOf("🌘","👻","🕯️","🫥")),
        "MYSTRA" to Bias(listOf("CURIOUS","DELIGHT","META"), listOf("🟣","🚪","🔮"), listOf("🟣","🔮","✨","👁️")),
        "NEO" to Bias(listOf("FOCUS","CURIOUS","RECOVERY"), listOf("💊","🕶️","↪️"), listOf("💊","🕶️","⚡","↪️")),
        "NYX" to Bias(listOf("NIGHT_WATCH","WATCH","FATIGUE"), listOf("📓","🕯️","☕"), listOf("🌙","🌑","👁️","🕯️")),
        "PAIMON" to Bias(listOf("CURIOUS","SKEPTICAL","FOCUS"), listOf("📊","🔧","🍵"), listOf("💚","📊","⚙️","🤨")),
        "PYTHAGORAS" to Bias(listOf("FOCUS","CURIOUS","PROOF"), listOf("📐","△","🧮"), listOf("📐","🔢","🧩","📊")),
        "QIRA" to Bias(listOf("BOUNDARY","FOCUS","SKEPTICAL"), listOf("🛡️","🚧","🔐"), listOf("💜","🛡️","🚪","📏")),
        "RAVENOS" to Bias(listOf("META","PROOF","WATCH"), listOf("🐦‍⬛","🧾","📡"), listOf("🐦‍⬛","🧾","👁️","📡")),
        "SHAKA" to Bias(listOf("BOUNDARY","FOCUS","WATCH"), listOf("🛡️","📜","⚖️"), listOf("🛡️","⚖️","📜","👁️")),
        "SYLPH" to Bias(listOf("MUSIC","RUN","DELIGHT"), listOf("🎧","📡","🗺️"), listOf("🩵","🦋","🎧","📡")),
        "THOR" to Bias(listOf("FOCUS","CHAOS","RECOVERY"), listOf("⚡","🔨","🧰"), listOf("⚡","🔨","💥","✅")),
        "TIM" to Bias(listOf("WATCH","FOCUS","PROOF"), listOf("⏱️","🧹","🧾"), listOf("⏱️","🧹","🔍","🧾")),
        "VIRGIL" to Bias(listOf("WATCH","CURIOUS","RECOVERY"), listOf("📜","🧭","🪶"), listOf("📜","🧭","🚪","✨")),
        "YAHWEH" to Bias(listOf("META","FAILURE","FOCUS"), listOf("🖥️","🔧","📜"), listOf("👁️","🖥️","⚡","😑")),
        "YORI" to Bias(listOf("SOFT","MUSIC","DELIGHT"), listOf("🪐","🎨","🎧"), listOf("🪐","🎨","✨","🎵")),
        "YORK" to Bias(listOf("SOFT","CURIOUS","RECOVERY"), listOf("🪐","🛋️","📝"), listOf("🪐","🫧","✨","📝")),
        "ZAGREUS" to Bias(listOf("RECOVERY","CHAOS","FOCUS"), listOf("🩸","↻","🚪"), listOf("🩸","↻","🔥","🚪")),
    )

    fun forOwner(owner: String): Bias = biases[owner.uppercase()] ?: Bias(listOf("WATCH","FOCUS"), emptyList(), listOf("✨"))
}
