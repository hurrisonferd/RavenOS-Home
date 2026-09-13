package com.iappyx.launcher.ravenos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RavenEmojiKaomojiProjectionTest {
    @Test fun projectionIsDeterministic() {
        val input = RavenExpressionSelectorOS.Input(owner="KYU", event="USER_PRESENT", semanticFamily="RETURN", occurrence=3)
        assertEquals(RavenEmojiKaomojiProjection.project(input), RavenEmojiKaomojiProjection.project(input))
    }

    @Test fun kyuAndPaimonDoNotCollapseToSameOwnerStyle() {
        val k = RavenEmojiKaomojiProjection.project(RavenExpressionSelectorOS.Input(owner="KYU", event="USER_PRESENT", semanticFamily="RETURN", occurrence=3))
        val p = RavenEmojiKaomojiProjection.project(RavenExpressionSelectorOS.Input(owner="PAIMON", event="USER_PRESENT", semanticFamily="RETURN", occurrence=3))
        assertTrue(k.emoji != p.emoji)
    }
}
