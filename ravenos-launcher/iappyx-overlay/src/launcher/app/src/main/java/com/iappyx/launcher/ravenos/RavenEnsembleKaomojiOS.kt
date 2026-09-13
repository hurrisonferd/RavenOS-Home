package com.iappyx.launcher.ravenos

/** Pair/group posture vocabulary for office reactions. Presentation only. */
object RavenEnsembleKaomojiOS {
    private val pairs = mapOf(
        "AGREE" to listOf("(•̀ᴗ•́)و ̑̑  (•̀ᴗ•́)و ̑̑", "(ง •̀_•́)ง ٩(•̀ᴗ•́)و", "(￣▽￣)ノ  ヾ(￣▽￣)"),
        "SIDE_EYE" to listOf("(¬_¬)   (¬_¬)", "(눈_눈)  (¬‿¬)", "(￢_￢)  (ಠ_ಠ)"),
        "ARGUE" to listOf("(งಠ_ಠ)ง  vs  ᕦ(ò_óˇ)ᕤ", "(╬ಠ益ಠ)  ⇄  (ಠ_ಠ)", "☞(ﾟヮﾟ☞)  (☜ﾟヮﾟ)☜"),
        "HANDOFF" to listOf("(•̀ᴗ•́)و📋 → (._.)φ", "(¬‿¬)🧾 → ( •̀ω•́ )و", "(ง •̀_•́)ง🔧 → (￣▽￣)ノ"),
        "COMFORT" to listOf("(づ｡◕‿‿◕｡)づ  (´• ω •`)", "(っ˘▽˘)っ  (｡•́‿•̀｡)", "(つ≧▽≦)つ  (⌒‿⌒)"),
        "BOARD" to listOf("📋(ಠ_ಠ) ┬─┬ (¬‿¬)☕", "( •̀ω•́ )و📊  ┬─┬  (눈_눈)", "┬─┬ノ( º _ ºノ)  📋  (•̀ᴗ•́)و"),
        "PANIC" to listOf("ヽ(°〇°)ﾉ  ヽ(°〇°)ﾉ", "ε=ε=┌( >_<)┘  ε=ε=┌( >_<)┘", "┻━┻ ︵ヽ(`Д´)ﾉ︵ ┻━┻"),
        "SILENT" to listOf("|ω･)    |ω･)", "(._.)   (._.)", "(￢_￢)   (￢_￢)")
    )

    fun pick(kind: String, seed: Int): String {
        val options = pairs[kind.uppercase()] ?: pairs["SILENT"]!!
        return options[Math.floorMod(seed, options.size)]
    }
}
