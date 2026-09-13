package com.iappyx.launcher.ravenos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavenGoblinModuleRackTest {
    @Test fun nominalPlanHasNoGraphOrExtensionIssues() {
        val plan = RavenGoblinModuleRackOS.plan(RavenOmniRvExpressionBudget.Pressure.NOMINAL)
        assertTrue(plan.issues.isEmpty())
        assertTrue(RavenGoblinExtensionRackOS.validate().isEmpty())
        assertTrue("GOBLIN_ENGINE" in plan.enabledIds)
        assertTrue("MODULE_RACK" in plan.enabledIds)
        assertTrue("LATE_EXTENSION_RACK" in plan.enabledIds)
        assertTrue("SURFACE_ROUTER" in plan.enabledIds)
    }

    @Test fun hotPressureShedsKnowledgeBrokerAndItsDependentCache() {
        val plan = RavenGoblinModuleRackOS.plan(RavenOmniRvExpressionBudget.Pressure.HOT)
        assertFalse("KNOWLEDGE_BROKER" in plan.enabledIds)
        assertFalse("KNOWLEDGE_CACHE" in plan.enabledIds)
        assertEquals("dependency:KNOWLEDGE_BROKER", plan.shedReasons["KNOWLEDGE_CACHE"])
    }

    @Test fun criticalPressurePreservesEngineSpine() {
        val plan = RavenGoblinModuleRackOS.plan(RavenOmniRvExpressionBudget.Pressure.CRITICAL)
        listOf(
            "MODULE_RACK",
            "LATE_EXTENSION_RACK",
            "GOBLIN_ENGINE",
            "SURFACE_ROUTER",
            "REACTION_STATE",
            "OFFICE_STATE",
            "OFFICE_BAR",
        ).forEach { assertTrue("missing mandatory $it", it in plan.enabledIds) }
        assertFalse("KNOWLEDGE_BROKER" in plan.enabledIds)
        assertFalse("META_GRAMMAR" in plan.enabledIds)
    }

    @Test fun extensionRackOnlyExecutesRegisteredOrgans() {
        assertEquals(setOf("META_GRAMMAR", "KNOWLEDGE_BROKER"), RavenGoblinExtensionRackOS.ids().toSet())
        assertTrue(RavenGoblinExtensionRackOS.validate().isEmpty())
    }
}
