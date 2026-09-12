package com.iappyx.launcher.ravenos

/**
 * Privacy-safe structural interpretation of notification-shade events.
 *
 * This organ intentionally ignores package, channel, title, body, extras and notification keys.
 * It only exposes Android presentation geometry already present on the typed marker: category,
 * importance, conversation/ambient/filter state, plausible alerting, burst pressure and payoff.
 */
object RavenShadeSenseOS {
    enum class Salience { NONE, LOW, NORMAL, HIGH }

    data class Snapshot(
        val active: Boolean,
        val state: String,
        val category: String,
        val importance: Int,
        val conversation: Boolean,
        val ambient: Boolean,
        val matchesFilter: Boolean,
        val alerting: Boolean,
        val ongoing: Boolean,
        val burst: Int,
        val payoff: Boolean,
        val lifetimeMs: Long?,
        val salience: Salience,
    ) {
        val safeDetail: String get() = buildString {
            append("state:").append(state)
            append("|category:").append(category)
            append("|importance:").append(importance)
            append("|conversation:").append(conversation)
            append("|ambient:").append(ambient)
            append("|matches_filter:").append(matchesFilter)
            append("|alerting:").append(alerting)
            append("|ongoing:").append(ongoing)
            append("|burst:").append(burst)
            append("|payoff:").append(payoff)
            lifetimeMs?.let { append("|lifetime_ms:").append(it) }
            append("|salience:").append(salience.name)
        }
    }

    fun observe(marker: RavenMarkerBus.Marker): Snapshot {
        if (!marker.key.startsWith("NOTIFICATION")) return inactive()
        val d = marker.detail
        val state = field(d, "state")?.lowercase() ?: if (marker.key.endsWith("REMOVED")) "removed" else "posted"
        val category = field(d, "category")?.uppercase()?.ifBlank { "NONE" } ?: "NONE"
        val importance = field(d, "importance")?.toIntOrNull() ?: -1000
        val conversation = bool(d, "conversation")
        val ambient = bool(d, "ambient")
        val matches = field(d, "matches_filter")?.toBooleanStrictOrNull() ?: true
        val alerting = bool(d, "alerting")
        val ongoing = bool(d, "ongoing")
        val burst = (field(d, "burst")?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val lifetime = field(d, "lifetime_ms")?.toLongOrNull()?.takeIf { it >= 0L }
        val payoff = state == "removed" || bool(d, "payoff")
        val salience = when {
            payoff && lifetime != null && lifetime < 4_000L -> Salience.NORMAL
            alerting && (conversation || burst >= 3 || importance >= 4) -> Salience.HIGH
            importance >= 4 && !ambient -> Salience.HIGH
            alerting || importance >= 3 || burst >= 2 -> Salience.NORMAL
            else -> Salience.LOW
        }
        return Snapshot(true, state, category, importance, conversation, ambient, matches, alerting, ongoing, burst, payoff, lifetime, salience)
    }

    private fun inactive() = Snapshot(
        active = false,
        state = "none",
        category = "NONE",
        importance = -1000,
        conversation = false,
        ambient = false,
        matchesFilter = true,
        alerting = false,
        ongoing = false,
        burst = 0,
        payoff = false,
        lifetimeMs = null,
        salience = Salience.NONE,
    )

    private fun bool(detail: String, name: String): Boolean =
        field(detail, name)?.equals("true", ignoreCase = true) == true

    private fun field(detail: String, name: String): String? =
        Regex("(?:^|\\|)${Regex.escape(name)}:([^|]*)")
            .find(detail)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
}
