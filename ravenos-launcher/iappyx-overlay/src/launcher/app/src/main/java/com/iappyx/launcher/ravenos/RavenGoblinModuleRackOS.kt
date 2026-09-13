package com.iappyx.launcher.ravenos

/**
 * Dependency-aware load plan for Goblin Brain organs.
 *
 * The systems registry says what exists. This rack says what is allowed to execute for the current
 * Omni RV pressure without leaving a module enabled after one of its dependencies was shed.
 * Future recovered organs should enter the launcher through the registry + this rack rather than
 * becoming an untracked side path.
 */
object RavenGoblinModuleRackOS {
    data class Plan(
        val pressure: RavenOmniRvExpressionBudget.Pressure,
        val enabled: List<RavenGoblinSystemsRegistryOS.Organ>,
        val shed: List<RavenGoblinSystemsRegistryOS.Organ>,
        val shedReasons: Map<String, String>,
        val issues: List<String>,
    ) {
        val enabledIds: Set<String> get() = enabled.mapTo(linkedSetOf()) { it.id }
        val shedIds: Set<String> get() = shed.mapTo(linkedSetOf()) { it.id }

        val receipt: String get() = buildString {
            append(pressure.name)
            append(':').append(enabled.size).append('/').append(RavenGoblinSystemsRegistryOS.organs.size)
            if (shed.isNotEmpty()) append(":shed=").append(shed.joinToString(",") { it.id })
            if (issues.isNotEmpty()) append(":issues=").append(issues.joinToString(","))
        }
    }

    fun pressureFor(pulse: RavenRVResilienceOS.Pulse): RavenOmniRvExpressionBudget.Pressure = when {
        pulse.mode == "COCKPIT_OFFLINE" || pulse.score < 25 -> RavenOmniRvExpressionBudget.Pressure.CRITICAL
        pulse.mode == "PHONE_LIMP_HOME" || pulse.score < 45 -> RavenOmniRvExpressionBudget.Pressure.HOT
        pulse.score < 70 -> RavenOmniRvExpressionBudget.Pressure.BUSY
        else -> RavenOmniRvExpressionBudget.Pressure.NOMINAL
    }

    fun plan(pressure: RavenOmniRvExpressionBudget.Pressure): Plan {
        val all = RavenGoblinSystemsRegistryOS.organs
        val mandatory = RavenGoblinSystemsRegistryOS.baseBrainMandatory
        val initiallyEnabled = RavenGoblinSystemsRegistryOS.enabledUnder(pressure).mapTo(linkedSetOf()) { it.id }
        val enabled = initiallyEnabled.toMutableSet()
        val reasons = linkedMapOf<String, String>()

        all.filterNot { it.id in initiallyEnabled }.forEach { reasons[it.id] = "pressure:${pressure.name}" }

        // Dependency-aware shedding. Optional modules cannot remain enabled when an upstream organ
        // is unavailable. Mandatory organs fail readable instead of being silently erased.
        var changed: Boolean
        do {
            changed = false
            for (organ in all) {
                if (organ.id !in enabled) continue
                val missing = organ.dependsOn.firstOrNull { it !in enabled } ?: continue
                if (organ.id in mandatory) continue
                enabled.remove(organ.id)
                reasons[organ.id] = "dependency:$missing"
                changed = true
            }
        } while (changed)

        val issues = mutableListOf<String>()
        issues += RavenGoblinSystemsRegistryOS.validateGraph()
        for (organ in all.filter { it.id in enabled && it.id in mandatory }) {
            organ.dependsOn.filterNot { it in enabled }.forEach {
                issues += "mandatory_dependency_shed:${organ.id}->$it"
            }
        }

        val enabledOrgans = all.filter { it.id in enabled }
        val shedOrgans = all.filterNot { it.id in enabled }
        return Plan(
            pressure = pressure,
            enabled = enabledOrgans,
            shed = shedOrgans,
            shedReasons = reasons.toMap(),
            issues = issues.distinct(),
        )
    }

    fun compact(pressure: RavenOmniRvExpressionBudget.Pressure): String {
        val p = plan(pressure)
        val stages = p.enabled.groupBy { it.stage }.entries.joinToString(",") { "${it.key.name}:${it.value.size}" }
        return "GOBLIN MODULE RACK · pressure=${p.pressure.name} · enabled=${p.enabled.size}/${RavenGoblinSystemsRegistryOS.organs.size} · stages=$stages · issues=${p.issues.size}"
    }
}
