package com.iappyx.launcher.ravenos

import org.junit.Assert.assertTrue
import org.junit.Test

class RavenEmployeeExpressionBridgeTest {
    @Test fun sourceContractExposesBridge() {
        assertTrue(RavenExpressionMetaVersion.CORE_5120_PRESERVED)
        assertTrue(RavenKaomojiExpansionBank.totalForms() >= 120)
    }
}
