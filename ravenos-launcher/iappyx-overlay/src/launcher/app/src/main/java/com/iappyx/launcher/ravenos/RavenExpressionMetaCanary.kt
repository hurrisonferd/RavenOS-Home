package com.iappyx.launcher.ravenos

/** Pure runtime self-check usable from diagnostics without Android permissions. */
object RavenExpressionMetaCanary {
    data class Result(val pass: Boolean, val checks: Int, val forms: Int, val families: Int, val detail: String)

    fun run(): Result {
        var checks = 0
        val families = RavenKaomojiExpansionBank.families()
        checks++
        if (families.size < 20) return Result(false, checks, RavenKaomojiExpansionBank.totalForms(), families.size, "family breadth")
        checks++
        if (RavenKaomojiExpansionBank.totalForms() < 120) return Result(false, checks, RavenKaomojiExpansionBank.totalForms(), families.size, "form breadth")
        val a = RavenMetaDialogueEngine.project(RavenMetaDialogueEngine.Input("KYU","USER_PRESENT","RETURN",occurrence=5,surface="DESK"))
        val b = RavenMetaDialogueEngine.project(RavenMetaDialogueEngine.Input("KYU","USER_PRESENT","RETURN",occurrence=5,surface="DESK"))
        checks++
        if (a != b) return Result(false, checks, RavenKaomojiExpansionBank.totalForms(), families.size, "nondeterminism")
        val critical = RavenMetaDialogueEngine.project(RavenMetaDialogueEngine.Input("KYU","USER_PRESENT","RETURN",pressure=RavenOmniRvExpressionBudget.Pressure.CRITICAL))
        checks++
        if (critical.kaomoji.isNotEmpty() || critical.metaTail.isNotEmpty()) return Result(false, checks, RavenKaomojiExpansionBank.totalForms(), families.size, "critical shedding")
        return Result(true, checks, RavenKaomojiExpansionBank.totalForms(), families.size, "pass")
    }
}
