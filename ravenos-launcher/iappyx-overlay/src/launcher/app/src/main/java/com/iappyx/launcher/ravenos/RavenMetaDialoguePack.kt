package com.iappyx.launcher.ravenos

/** Small return object for surfaces that want one already-composed expression/meta packet. */
data class RavenMetaDialoguePack(
    val emoji: String,
    val kaomoji: String,
    val ensemble: String,
    val metaTail: String,
    val family: String,
    val stage: String,
    val budgetReason: String
)
