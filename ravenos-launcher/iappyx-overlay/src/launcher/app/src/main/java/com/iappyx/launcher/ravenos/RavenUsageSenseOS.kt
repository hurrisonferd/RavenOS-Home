package com.iappyx.launcher.ravenos

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process

/**
 * Optional app-level foreground ledger using Android Usage Access.
 *
 * The user explicitly grants PACKAGE_USAGE_STATS in Settings. We read only usage-event
 * package/activity-resume transitions. No app content, text, view hierarchy, screenshots,
 * notification bodies, or interaction authority are obtained from this lane.
 *
 * V14 continuity: when no competing app has resumed, a bounded polling heartbeat may refresh the
 * already-known foreground session. It does not emit a new Office event and cannot resurrect a
 * paused session. This keeps a static Suno/ChatGPT/Chrome page from becoming "Home" just because
 * Raven read or listened without touching the screen for ten minutes.
 */
object RavenUsageSenseOS {
    private const val PREFS = "ravenos_usage_sense_v1"
    private const val KEY_LAST_PACKAGE = "last_package"
    private const val KEY_LAST_EVENT_TIME = "last_event_time"
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            val app = appContext ?: return
            try { sample(app) } catch (_: Throwable) {}
            if (running) handler.postDelayed(this, interval(app))
        }
    }

    @Volatile
    private var appContext: Context? = null

    fun start(context: Context) {
        appContext = context.applicationContext
        if (running) return
        running = true
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        appContext = null
    }

    fun hasAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        return try {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            ) == AppOpsManager.MODE_ALLOWED
        } catch (_: Throwable) {
            false
        }
    }

    fun compact(context: Context): String = if (hasAccess(context)) {
        "USAGE ACCESS=ON · app-resume fallback + same-app continuity heartbeat"
    } else {
        "USAGE ACCESS=OFF · optional foreground fallback"
    }

    private fun interval(context: Context): Long = when (RavenHauntModeStore.get(context)) {
        RavenHauntMode.CALM -> 10_000L
        RavenHauntMode.LIVED_IN -> 7_000L
        RavenHauntMode.HAUNTED -> 5_000L
        RavenHauntMode.FERAL -> 3_500L
        RavenHauntMode.APOCALYPSE -> 2_500L
    }

    private fun sample(context: Context) {
        if (!hasAccess(context)) return
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val since = prefs.getLong(KEY_LAST_EVENT_TIME, now - 15_000L)
            .coerceIn(now - 60_000L, now)
        val events = manager.queryEvents(since, now)
        val event = UsageEvents.Event()
        var newestPackage: String? = null
        var newestAt = since

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val foreground = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
            if (!foreground) continue
            val pkg = event.packageName?.trim().orEmpty()
            if (pkg.isBlank() || pkg == context.packageName || isTransientPackage(pkg)) continue
            if (event.timeStamp >= newestAt) {
                newestAt = event.timeStamp
                newestPackage = pkg
            }
        }

        prefs.edit().putLong(KEY_LAST_EVENT_TIME, now).apply()
        val previous = prefs.getString(KEY_LAST_PACKAGE, null)
        val pkg = newestPackage

        if (pkg.isNullOrBlank()) {
            // No competing ordinary app resumed since the last poll. Refresh only an existing,
            // unpaused session that agrees with our last Usage Access foreground witness.
            val current = RavenAppSessionOS.current(context, now, allowStale = true)
            if (!previous.isNullOrBlank() && current != null && !current.paused && current.packageName == previous) {
                RavenAppSessionOS.touch(context, previous, "usage-heartbeat", at = now)
            }
            return
        }

        if (pkg == previous) {
            val current = RavenAppSessionOS.current(context, now, allowStale = true)
            if (current != null && !current.paused && current.packageName == pkg) {
                RavenAppSessionOS.touch(context, pkg, "usage-heartbeat", at = now)
            } else {
                RavenAppSessionOS.observe(context, pkg, "usage-access", at = newestAt)
            }
            return
        }

        prefs.edit().putString(KEY_LAST_PACKAGE, pkg).apply()
        RavenAppSessionOS.observe(context, pkg, "usage-access", at = newestAt)
        RavenOfficeBarService.signal(
            context,
            "FOREGROUND_USAGE",
            "package:$pkg|source:usage-access",
        )
    }

    private fun isTransientPackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p == "com.android.systemui" || p.contains("honeyboard") || p.contains("inputmethod") ||
            p.contains("keyboard") || p.contains("smartcapture") || p.contains("screenshot") ||
            (p.contains("capture") && p.contains("samsung"))
    }
}
