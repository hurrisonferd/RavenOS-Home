package com.iappyx.launcher.ravenos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RavenMetaDialogueEngineTest {
    @Test fun deterministicProjection() {
        val i = RavenMetaDialogueEngine.Input("KYU","USER_PRESENT","RETURN",motif="RETURN_DESK",occurrence=5,previousOwner="PAIMON",surface="DESK")
        assertEquals(RavenMetaDialogueEngine.project(i), RavenMetaDialogueEngine.project(i))
    }

    @Test fun criticalPressureRemovesDecoration() {
        val o = RavenMetaDialogueEngine.project(RavenMetaDialogueEngine.Input("KYU","USER_PRESENT","RETURN",pressure=RavenOmniRvExpressionBudget.Pressure.CRITICAL))
        assertTrue(o.kaomoji.isEmpty())
        assertTrue(o.ensemble.isEmpty())
        assertTrue(o.metaTail.isEmpty())
    }
}
