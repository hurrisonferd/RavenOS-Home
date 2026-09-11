package com.iappyx.launcher.ravenos

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.util.concurrent.Executors

/**
 * Process-local launchable app cache for Raven Home.
 *
 * PackageManager queries and label sorting happen off the UI thread. Icons are loaded lazily and
 * cached because loading hundreds of drawables on every HOME resume was the largest native-shell
 * jank source observed in the first device build.
 */
object RavenAppCatalog {
    data class Entry(
        val label: String,
        val packageName: String,
        val activityName: String,
    ) {
        val key: String get() = "$packageName/$activityName"
    }

    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "RavenAppCatalog").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())
    private val iconCache = object : LruCache<String, Drawable>(72) {}

    @Volatile private var cached: List<Entry>? = null
    @Volatile private var loading = false
    private val pending = mutableListOf<(List<Entry>) -> Unit>()

    fun current(): List<Entry>? = cached

    fun load(context: Context, callback: (List<Entry>) -> Unit) {
        cached?.let {
            callback(it)
            return
        }
        val shouldStart: Boolean
        synchronized(pending) {
            cached?.let {
                callback(it)
                return
            }
            pending += callback
            shouldStart = !loading
            if (shouldStart) loading = true
        }
        if (!shouldStart) return

        val app = context.applicationContext
        worker.execute {
            val result = try {
                val pm = app.packageManager
                val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(query, 0)
                    .asSequence()
                    .mapNotNull { info ->
                        val ai = info.activityInfo ?: return@mapNotNull null
                        // RavenOS Studio is intentionally not an app-drawer duplicate; it has an
                        // explicit Home button and menu entry.
                        if (ai.packageName == app.packageName) return@mapNotNull null
                        val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                            .ifBlank { ai.packageName }
                        Entry(label, ai.packageName, ai.name)
                    }
                    .distinctBy(Entry::key)
                    .sortedBy { it.label.lowercase() }
                    .toList()
            } catch (_: Throwable) {
                emptyList()
            }

            main.post {
                val callbacks: List<(List<Entry>) -> Unit>
                synchronized(pending) {
                    cached = result
                    loading = false
                    callbacks = pending.toList()
                    pending.clear()
                }
                callbacks.forEach { callback -> callback(result) }
            }
        }
    }

    fun icon(context: Context, entry: Entry): Drawable? {
        iconCache.get(entry.key)?.let { return it }
        return try {
            val icon = context.packageManager.getActivityIcon(ComponentName(entry.packageName, entry.activityName))
            iconCache.put(entry.key, icon)
            icon
        } catch (_: Throwable) {
            try { context.packageManager.getApplicationIcon(entry.packageName) }
            catch (_: Throwable) { null }
        }
    }

    fun invalidate() {
        cached = null
        iconCache.evictAll()
    }
}
