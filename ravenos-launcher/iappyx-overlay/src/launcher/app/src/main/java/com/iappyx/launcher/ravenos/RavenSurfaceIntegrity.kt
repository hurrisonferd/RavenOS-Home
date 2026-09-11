package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Local proof ledger for RavenOS Office projections.
 *
 * Deliberately distinguishes RENDERED from DISPATCHED. Native launcher surfaces can report
 * that their actual View was updated. WebView / cross-process paths are only called DISPATCHED
 * until a future device-side acknowledgement exists. No optimistic "everything synced" claim.
 */
object RavenSurfaceIntegrity {
    private const val PREFS = "ravenos_surface_integrity_v1"

    const val CANONICAL = "CANONICAL"
    const val OFFICE_BAR = "OFFICE_BAR"
    const val HOME_AURA = "HOME_AURA"
    const val HOME_WHISPER = "HOME_WHISPER"
    const val FOLLOW_ME = "FOLLOW_ME"
    const val WIDGETS = "WIDGETS"
    const val WALLPAPER_CHANNEL = "WALLPAPER_CHANNEL"

    private val surfaces = listOf(
        CANONICAL,
        OFFICE_BAR,
        HOME_AURA,
        HOME_WHISPER,
        FOLLOW_ME,
        WIDGETS,
        WALLPAPER_CHANNEL,
    )

    fun mark(
        context: Context,
        surface: String,
        status: String,
        stateUpdatedAt: Long,
        detail: String = "",
    ) {
        val key = surface.trim().uppercase()
        if (key !in surfaces) return
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("$key.status", status.trim().uppercase())
            .putLong("$key.state_at", stateUpdatedAt)
            .putLong("$key.observed_at", System.currentTimeMillis())
            .putString("$key.detail", detail.take(180))
            .apply()
    }

    fun compact(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val canonical = RavenOfficeStateStore.read(context)
        val target = canonical?.updatedAt ?: 0L
        val now = System.currentTimeMillis()
        return surfaces.joinToString(" | ") { surface ->
            val status = prefs.getString("$surface.status", "UNSEEN").orEmpty()
            val stateAt = prefs.getLong("$surface.state_at", 0L)
            val observedAt = prefs.getLong("$surface.observed_at", 0L)
            val detail = prefs.getString("$surface.detail", "").orEmpty()
            val sync = when {
                stateAt <= 0L -> "NO_STATE"
                target > 0L && stateAt == target -> "CURRENT"
                target > 0L && stateAt < target -> "STALE"
                else -> "STATE"
            }
            val age = if (observedAt <= 0L) -1L else (now - observedAt).coerceAtLeast(0L)
            buildString {
                append(surface).append('=').append(status).append('/').append(sync)
                if (age >= 0L) append('@').append(age).append("ms")
                if (detail.isNotBlank()) append('(').append(detail).append(')')
            }
        }
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
