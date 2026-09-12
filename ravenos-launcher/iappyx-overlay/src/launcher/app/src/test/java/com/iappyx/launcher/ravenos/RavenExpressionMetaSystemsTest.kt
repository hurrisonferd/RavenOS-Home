package com.iappyx.launcher.ravenos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavenExpressionMetaSystemsTest {
    @Test fun kaomojiBankHasBreadthAndDeterminism() {
        assertTrue(RavenKaomojiExpansionBank.totalForms() >= 120)
        assertTrue(RavenKaomojiExpansionBank.families().size >= 20)
        assertEquals(
            RavenKaomojiExpansionBank.pick("WATCH", 42),
            RavenKaomojiExpansionBank.pick("WATCH", 42)
        )
    }

    @Test fun omniRvBudgetShedsDecorationBeforeCore() {
        val nominal = RavenOmniRvExpressionBudget.forPressure(RavenOmniRvExpressionBudget.Pressure.NOMINAL, false, false)
        val critical = RavenOmniRvExpressionBudget.forPressure(RavenOmniRvExpressionBudget.Pressure.CRITICAL, false, false)
        assertTrue(nominal.allowKaomoji)
        assertTrue(nominal.allowMetaTail)
        assertFalse(critical.allowKaomoji)
        assertFalse(critical.allowMetaTail)
    }

    @Test fun metaStageEscalatesDeterministically() {
        assertEquals("SEED", RavenMetaDialogueStateOS.stageFor(1))
        assertEquals("CALLBACK", RavenMetaDialogueStateOS.stageFor(2))
        assertEquals("RUNNING_BIT", RavenMetaDialogueStateOS.stageFor(3))
        assertEquals("INSTITUTIONAL", RavenMetaDialogueStateOS.stageFor(55))
    }

    @Test fun identicalFrameProducesIdenticalProjection() {
        val frame = RavenMetaDialogueRenderer.Frame(
            owner = "KYU", event = "USER_PRESENT", text = "x", evidence = "y",
            motif = "RETURN_DESK", occurrence = 5, previousOwner = "PAIMON"
        )
        assertEquals(RavenMetaDialogueRenderer.render(frame), RavenMetaDialogueRenderer.render(frame))
    }
}
