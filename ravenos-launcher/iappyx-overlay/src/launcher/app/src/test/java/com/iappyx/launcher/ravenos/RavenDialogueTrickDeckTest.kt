package com.iappyx.launcher.ravenos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavenDialogueTrickDeckTest {
    @Test fun callbackTrickRequiresCallback() {
        val noCallback = RavenDialogueTrickDeck.eligible(4, "", setOf("callback","meta"))
        val callback = RavenDialogueTrickDeck.eligible(4, "POWER_CRISIS_RESOLVED", setOf("callback","meta"))
        assertFalse(noCallback.any { it.id == "CALLBACK_PAYOFF" })
        assertTrue(callback.any { it.id == "CALLBACK_PAYOFF" })
    }

    @Test fun lateOccurrenceUnlocksInstitutionalTricks() {
        val early = RavenDialogueTrickDeck.eligible(1, "", setOf("office","motif","meta"))
        val late = RavenDialogueTrickDeck.eligible(8, "", setOf("office","motif","meta"))
        assertFalse(early.any { it.id == "TINY_INSTITUTION" })
        assertTrue(late.any { it.id == "TINY_INSTITUTION" })
    }
}
