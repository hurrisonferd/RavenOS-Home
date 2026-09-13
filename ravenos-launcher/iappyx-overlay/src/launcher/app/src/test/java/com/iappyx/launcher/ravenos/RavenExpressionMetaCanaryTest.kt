package com.iappyx.launcher.ravenos

import org.junit.Assert.assertTrue
import org.junit.Test

class RavenExpressionMetaCanaryTest {
    @Test fun canaryPasses() {
        val result = RavenExpressionMetaCanary.run()
        assertTrue(result.detail, result.pass)
    }
}
