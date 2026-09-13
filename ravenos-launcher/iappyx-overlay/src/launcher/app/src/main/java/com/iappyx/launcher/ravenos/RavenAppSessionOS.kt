package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * Structural foreground continuity for Goblin Vision.
 *
 * This does not read app content. Accessibility/Usage lanes report package identity they already
 * possess; this organ keeps the last ordinary foreground app stable across notification/media/
 * overlay callbacks so those events become supporting context instead of stealing scene ownership.
 */
object RavenAppSessionOS {
    private const val PREFS = "ravenos_app_session_v1"
    private const val ACTIVE_LEASE_MS = 10 * 60_000L
    private const val RECENT_MAX = 8

    data class Session(
        val packageName: String,
        val label: String,
        val kind: String,
        val summary: String,
        val enteredAt: Long,
        val lastSeenAt: Long,
        val previousPackage: String,
        val previousLabel: String,
        val switchCount: Int,
        val returnCount: Int,
        val source: String,
        val confidence: Int,
        val active: Boolean,
    ) {
        val dwellMs: Long get() = (lastSeenAt - enteredAt).coerceAtLeast(0L)
        fun compact(now: Long = System.currentTimeMillis()): String = buildString {
            append("APP_SESSION=").append(label.ifBlank { packageName.substringAfterLast('.') })
            append("/").append(kind.ifBlank { "APP" })
            append(" · AGE=").append(((now - lastSeenAt).coerceAtLeast(0L) / 1000L)).append("s")
            append(" · DWELL=").append((dwellMs / 1000L)).append("s")
            if (previousLabel.isNotBlank()) append(" · PREV=").append(previousLabel)
            if (returnCount > 0) append(" · RETURNS=").append(returnCount)
            append(" · SOURCE=").append(source)
            append(" · CONF=").append(confidence)
            append(" · ").append(if (active) "ACTIVE" else "STALE")
        }
    }

    fun observe(
        context: Context,
        packageName: String,
        source: String,
        visibleText: String? = null,
        keyboardLike: Boolean = false,
        at: Long = System.currentTimeMillis(),
    ): Session? {
        val app = context.applicationContext
        val pkg = packageName.trim()
        if (pkg.isBlank() || pkg == app.packageName || isTransientPackage(pkg)) return current(app, at)

        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldPkg = prefs.getString("package", "").orEmpty()
        val oldLabel = prefs.getString("label", "").orEmpty()
        val changed = oldPkg.isNotBlank() && oldPkg != pkg
        val label = packageLabel(app, pkg)
        val semantics = RavenAppSemanticsOS.interpret(app, pkg, label, visibleText, keyboardLike)
        val recent = readRecent(prefs.getString("recent", "").orEmpty()).toMutableList()
        val returning = changed && recent.any { it == pkg }
        if (changed) {
            recent.remove(pkg)
            recent.add(0, oldPkg)
            while (recent.size > RECENT_MAX) recent.removeAt(recent.lastIndex)
        }

        val enteredAt = if (oldPkg == pkg) prefs.getLong("entered_at", at).takeIf { it > 0L } ?: at else at
        val switchCount = prefs.getInt("switch_count", 0) + if (changed) 1 else 0
        val returnCount = prefs.getInt("return_count", 0) + if (returning) 1 else 0
        val confidence = sourceConfidence(source)
        prefs.edit()
            .putString("package", pkg)
            .putString("label", semantics.label.ifBlank { label })
            .putString("kind", semantics.kind)
            .putString("summary", semantics.summary)
            .putLong("entered_at", enteredAt)
            .putLong("last_seen_at", at)
            .putString("previous_package", if (changed) oldPkg else prefs.getString("previous_package", "").orEmpty())
            .putString("previous_label", if (changed) oldLabel else prefs.getString("previous_label", "").orEmpty())
            .putInt("switch_count", switchCount)
            .putInt("return_count", returnCount)
            .putString("source", source.take(40))
            .putInt("confidence", confidence)
            .putString("recent", recent.filter(String::isNotBlank).distinct().take(RECENT_MAX).joinToString(","))
            .apply()
        return current(app, at)
    }

    /** Touches the current app without changing ownership; useful for repeated same-app windows. */
    fun touch(context: Context, packageName: String, source: String, at: Long = System.currentTimeMillis()): Session? {
        val current = current(context, at, allowStale = true)
        if (current == null || current.packageName != packageName || isTransientPackage(packageName)) {
            return observe(context, packageName, source, at = at)
        }
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("last_seen_at", at)
            .putString("source", source.take(40))
            .putInt("confidence", maxOf(current.confidence, sourceConfidence(source)))
            .apply()
        return current(context, at)
    }

    fun current(context: Context, now: Long = System.currentTimeMillis(), allowStale: Boolean = false): Session? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pkg = prefs.getString("package", "").orEmpty()
        if (pkg.isBlank()) return null
        val lastSeen = prefs.getLong("last_seen_at", 0L)
        val active = lastSeen > 0L && now - lastSeen <= ACTIVE_LEASE_MS
        if (!active && !allowStale) return null
        return Session(
            packageName = pkg,
            label = prefs.getString("label", "").orEmpty(),
            kind = prefs.getString("kind", "APP").orEmpty(),
            summary = prefs.getString("summary", "app surface").orEmpty(),
            enteredAt = prefs.getLong("entered_at", lastSeen),
            lastSeenAt = lastSeen,
            previousPackage = prefs.getString("previous_package", "").orEmpty(),
            previousLabel = prefs.getString("previous_label", "").orEmpty(),
            switchCount = prefs.getInt("switch_count", 0),
            returnCount = prefs.getInt("return_count", 0),
            source = prefs.getString("source", "UNKNOWN").orEmpty(),
            confidence = prefs.getInt("confidence", 0),
            active = active,
        )
    }

    fun clear(context: Context, reason: String = "") {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        if (reason.isNotBlank()) RavenSurfaceIntegrity.mark(
            context,
            RavenSurfaceIntegrity.FOLLOW_ME,
            "CONTEXT_RESET",
            RavenOfficeStateStore.read(context)?.updatedAt ?: 0L,
            "app_session:${reason.take(80)}",
        )
    }

    private fun packageLabel(context: Context, pkg: String): String = try {
        val info = context.packageManager.getApplicationInfo(pkg, 0)
        context.packageManager.getApplicationLabel(info).toString().trim().take(80)
    } catch (_: Throwable) {
        pkg.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }.take(80)
    }

    private fun sourceConfidence(source: String): Int = when {
        source.contains("accessibility", ignoreCase = true) -> 100
        source.contains("usage", ignoreCase = true) -> 84
        source.contains("launch", ignoreCase = true) -> 78
        else -> 68
    }

    private fun isTransientPackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p == "com.android.systemui" || p.contains("honeyboard") || p.contains("inputmethod") ||
            p.contains("keyboard") || p.contains("smartcapture") || p.contains("screenshot") ||
            (p.contains("capture") && p.contains("samsung"))
    }

    private fun readRecent(raw: String): List<String> = raw.split(',').map(String::trim).filter(String::isNotBlank)
}
