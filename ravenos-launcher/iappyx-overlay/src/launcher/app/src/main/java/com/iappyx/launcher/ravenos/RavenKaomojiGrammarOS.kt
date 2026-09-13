package com.iappyx.launcher.ravenos

/** Compositional owner/family kaomoji grammar layered over the authored bank. */
object RavenKaomojiGrammarOS {
    private val familyProps = mapOf(
        "WATCH" to listOf("", " 👁", " 📎", " ☕"),
        "FOCUS" to listOf("", " 🔧", " 📋", " 🧾"),
        "META" to listOf("", " 🪞", " 📺", " ⌐■-■"),
        "BOARD_MEETING" to listOf(" 📋", " 📊", " ☕", " 🧾"),
        "FAILURE" to listOf(" 🧯", " 🪦", " 📋", " ⚠️"),
        "RECOVERY" to listOf(" 🔧", " ✅", " ☕", " 🧾"),
        "MUSIC" to listOf(" 🎧", " ♪", " ♫", " 🎵"),
        "BOUNDARY" to listOf(" 🛡️", " 🔐", " 🚧", " ✋"),
        "NIGHT_WATCH" to listOf(" 🌙", " 🕯", " ☕", ""),
        "DELIGHT" to listOf(" ✨", " 🎉", " 🦋", ""),
        "CHAOS" to listOf(" 💥", " 🧨", " 📋", " 🔥"),
    )

    private val motions = mapOf(
        "WATCH" to listOf("", " …", " ｼﾞｰｯ", " |"),
        "FOCUS" to listOf("", " و", " つ", " !!"),
        "META" to listOf("", " .exe", " //", " ↻"),
        "BOARD_MEETING" to listOf("", " ＿〆", " ﾊﾞﾝ", " …"),
        "FAILURE" to listOf("", " ぐぬぬ", " …", " ＿|￣|○"),
        "RECOVERY" to listOf("", " ｽｯ", " ✓", " ﾖｼ"),
        "MUSIC" to listOf("", " ♪", " ♫", " ~"),
        "BOUNDARY" to listOf("", " STOP", " ⊘", " …"),
        "NIGHT_WATCH" to listOf("", " …", " zZ", " ｽｯ"),
    )

    fun pick(owner: String, family: String, seed: Int): String {
        val normalized = family.uppercase()
        val core = RavenKaomojiExpansionBank.pick(normalized, seed)
        val bias = RavenExpressionOwnerBias.forOwner(owner)
        val props = familyProps[normalized].orEmpty() + bias.props.take(2).map { " $it" }
        val motion = motions[normalized].orEmpty()
        val addProp = props.isNotEmpty() && Math.floorMod(seed / 7, 4) == 0
        val addMotion = motion.isNotEmpty() && Math.floorMod(seed / 11, 5) == 0
        val prop = if (addProp) props[Math.floorMod(seed / 13, props.size)] else ""
        val move = if (addMotion) motion[Math.floorMod(seed / 17, motion.size)] else ""
        return (core + move + prop).trim()
    }
}
