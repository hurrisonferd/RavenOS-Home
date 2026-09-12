package com.iappyx.launcher.ravenos

/** Non-authoritative examples used by diagnostics/tests. */
object RavenMetaDialogueExamples {
    fun examples(): List<RavenMetaDialoguePack> = listOf(
        RavenMetaDialogueEngine.project(RavenMetaDialogueEngine.Input("KYU","USER_PRESENT","RETURN",motif="RETURN_DESK",occurrence=5,previousOwner="PAIMON",surface="BOARD_MEETING")),
        RavenMetaDialogueEngine.project(RavenMetaDialogueEngine.Input("PAIMON","BATTERY_LOW","PROOF",motif="POWER_BUREAU",occurrence=3,previousOwner="KYU",surface="DESK")),
        RavenMetaDialogueEngine.project(RavenMetaDialogueEngine.Input("NYX","SCREEN_OFF","WATCH",motif="NIGHT_SHIFT",occurrence=8,surface="PEEK",noveltyLow=true)),
        RavenMetaDialogueEngine.project(RavenMetaDialogueEngine.Input("QIRA","PERMISSION_BLOCKED","BOUNDARY",motif="BOUNDARY_COURT",occurrence=2,surface="WIDGET"))
    )
}
