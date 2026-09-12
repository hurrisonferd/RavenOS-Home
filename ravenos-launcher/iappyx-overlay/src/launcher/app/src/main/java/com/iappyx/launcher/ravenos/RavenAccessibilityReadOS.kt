package com.iappyx.launcher.ravenos

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Explicitly owner-armed visible Accessibility semantics.
 *
 * The service may retrieve window content so this layer can inspect what Android exposes, but
 * nothing is read unless Raven enables this switch. Password nodes and editable values are never
 * ingested. Only a short, local, bounded visible-text summary is retained.
 */
object RavenAccessibilityReadOS {
    data class Reading(
        val packageName: String,
        val text: String,
        val nodeCount: Int,
        val keyboardLike: Boolean,
        val capturedAt: Long,
    )

    private const val PREFS = "ravenos_accessibility_read_v1"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PACKAGE = "package"
    private const val KEY_TEXT = "text"
    private const val KEY_NODES = "nodes"
    private const val KEY_KEYBOARD = "keyboard"
    private const val KEY_AT = "at"
    private const val TTL_MS = 15_000L
    private const val MAX_NODES = 96
    private const val MAX_DEPTH = 8

    fun isEnabled(context: Context): Boolean = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        if (!enabled) clearReading(app)
        RavenOfficeBarService.signal(
            app,
            "SCREEN_SEMANTIC",
            if (enabled) "state:armed|source:accessibility|editable_values:false|passwords:false|local:true" else "state:disabled|source:accessibility",
        )
    }

    fun compact(context: Context): String = buildString {
        append("ACCESSIBILITY_READ=").append(if (isEnabled(context)) "ON" else "OFF")
        latest(context)?.let { append(" · SEMANTICS=RECENT") }
    }

    fun latest(context: Context, now: Long = System.currentTimeMillis()): Reading? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val at = prefs.getLong(KEY_AT, 0L)
        if (at <= 0L || now - at > TTL_MS) return null
        val text = prefs.getString(KEY_TEXT, "").orEmpty()
        val pkg = prefs.getString(KEY_PACKAGE, "").orEmpty()
        if (pkg.isBlank() || text.isBlank()) return null
        return Reading(
            packageName = pkg,
            text = text,
            nodeCount = prefs.getInt(KEY_NODES, 0),
            keyboardLike = prefs.getBoolean(KEY_KEYBOARD, false),
            capturedAt = at,
        )
    }

    fun observe(context: Context, packageName: String, root: AccessibilityNodeInfo?) {
        val app = context.applicationContext
        if (!isEnabled(app) || root == null || packageName == app.packageName) return

        val collected = ArrayList<String>()
        var visited = 0
        var sensitive = false
        var keyboardLike = packageName.contains("honeyboard", true) || packageName.contains("inputmethod", true)

        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > MAX_DEPTH || visited >= MAX_NODES || sensitive) return
            visited++
            if (node.isPassword) {
                sensitive = true
                return
            }
            val cls = node.className?.toString().orEmpty()
            if (cls.contains("EditText", true) || cls.contains("Input", true)) keyboardLike = true

            // Labels and static visible text are useful scene evidence. Editable values are excluded.
            if (node.isVisibleToUser && !node.isEditable) {
                listOf(node.text, node.contentDescription)
                    .mapNotNull { it?.toString()?.replace(Regex("\\s+"), " ")?.trim() }
                    .filter { it.length in 2..120 }
                    .forEach { if (it !in collected && collected.size < 24) collected += it }
            }
            val children = node.childCount.coerceAtMost(24)
            for (i in 0 until children) walk(node.getChild(i), depth + 1)
        }
        walk(root, 0)

        if (sensitive) {
            clearReading(app)
            RavenOfficeBarService.signal(
                app,
                "SCREEN_SEMANTIC",
                "state:suppressed_password|package:${escape(packageName)}|source:accessibility|local:true",
            )
            return
        }

        val normalized = collected.take(10).joinToString(" · ").take(260).trim()
        if (normalized.isBlank()) return
        val prior = latest(app)?.let { "${it.packageName}|${it.text}" }.orEmpty()
        val current = "$packageName|$normalized"
        if (prior == current) return

        val now = System.currentTimeMillis()
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PACKAGE, packageName)
            .putString(KEY_TEXT, normalized)
            .putInt(KEY_NODES, visited)
            .putBoolean(KEY_KEYBOARD, keyboardLike)
            .putLong(KEY_AT, now)
            .apply()

        val meta = RavenMetaRecursionOS.detect(normalized)
        RavenOfficeBarService.signal(
            app,
            "SCREEN_SEMANTIC",
            buildString {
                append("state:visible|package:").append(escape(packageName))
                append("|text:").append(escape(normalized.take(180)))
                append("|nodes:").append(visited)
                append("|keyboard:").append(keyboardLike)
                append("|meta:").append(meta)
                append("|source:accessibility|local:true|editable_values:false|passwords:false")
            },
        )
    }

    private fun clearReading(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_PACKAGE).remove(KEY_TEXT).remove(KEY_NODES).remove(KEY_KEYBOARD).remove(KEY_AT)
            .apply()
    }

    private fun escape(text: String): String = text
        .replace('|', '/')
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}
