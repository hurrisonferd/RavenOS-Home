package com.iappyx.launcher.ravenos

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Explicitly owner-armed visible Accessibility semantics.
 *
 * The service may retrieve window content so this layer can inspect what Android exposes, but
 * nothing is read unless Raven enables this switch. Password nodes and editable values are never
 * ingested. Only a bounded local visible-text summary and semantic viewport are retained.
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
    private const val KEY_VIEWPORT_SIG = "viewport_sig"
    private const val TTL_MS = 45_000L
    private const val MAX_NODES = 180
    private const val MAX_DEPTH = 12

    fun isEnabled(context: Context): Boolean = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        if (!enabled) {
            clearReading(app)
            RavenViewportSemanticsOS.clear(app)
            RavenInteractionMemoryOS.clear()
        }
        RavenOfficeBarService.signal(
            app,
            "SCREEN_SEMANTIC",
            if (enabled) "state:armed|source:accessibility|viewport:true|interaction_memory:true|editable_values:false|passwords:false|local:true" else "state:disabled|source:accessibility",
        )
    }

    fun compact(context: Context): String = buildString {
        append("ACCESSIBILITY_READ=").append(if (isEnabled(context)) "ON" else "OFF")
        latest(context)?.let { append(" · SEMANTICS=RECENT") }
        RavenViewportSemanticsOS.latest(context)?.let {
            append(" · TASK=").append(it.task)
            if (it.selected.isNotBlank()) append(" · SELECTED=").append(it.selected.take(42))
        }
        RavenInteractionMemoryOS.latest()?.let { append(" · ACTION=").append(it.compact().take(70)) }
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
            if (node.isEditable || cls.contains("EditText", true) || cls.contains("Input", true)) keyboardLike = true

            // Labels and static visible text are useful scene evidence. Editable values are excluded.
            if (node.isVisibleToUser && !node.isEditable) {
                listOf(node.text, node.contentDescription)
                    .mapNotNull { it?.toString()?.replace(Regex("\\s+"), " ")?.trim() }
                    .filter { it.length in 2..220 }
                    .forEach { if (it !in collected && collected.size < 52) collected += it }
            }
            val children = node.childCount.coerceAtMost(40)
            for (i in 0 until children) walk(node.getChild(i), depth + 1)
        }
        walk(root, 0)

        if (sensitive) {
            clearReading(app)
            RavenViewportSemanticsOS.clear(app)
            RavenInteractionMemoryOS.clear()
            RavenOfficeBarService.signal(
                app,
                "SCREEN_SEMANTIC",
                "state:suppressed_password|package:${escape(packageName)}|source:accessibility|local:true",
            )
            return
        }

        val viewport = RavenViewportSemanticsOS.observe(app, packageName, root)
        val normalized = collected.take(28).joinToString(" · ").take(720).trim()
        if (normalized.isBlank() && viewport == null) return
        val textForReading = normalized.ifBlank { viewport?.phrases.orEmpty() }.take(720)
        if (textForReading.isBlank()) return

        val viewportSig = listOf(
            viewport?.title.orEmpty(), viewport?.subject.orEmpty(), viewport?.selected.orEmpty(), viewport?.focused.orEmpty(),
            viewport?.task.orEmpty(), viewport?.roleSummary.orEmpty(),
        ).joinToString("|")
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prior = "${prefs.getString(KEY_PACKAGE, "").orEmpty()}|${prefs.getString(KEY_TEXT, "").orEmpty()}|${prefs.getString(KEY_VIEWPORT_SIG, "").orEmpty()}"
        val current = "$packageName|$textForReading|$viewportSig"
        if (prior == current) {
            prefs.edit().putLong(KEY_AT, System.currentTimeMillis()).apply()
            return
        }

        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_PACKAGE, packageName)
            .putString(KEY_TEXT, textForReading)
            .putInt(KEY_NODES, visited)
            .putBoolean(KEY_KEYBOARD, keyboardLike)
            .putString(KEY_VIEWPORT_SIG, viewportSig)
            .putLong(KEY_AT, now)
            .apply()

        val metaCorpus = listOf(textForReading, viewport?.title.orEmpty(), viewport?.subject.orEmpty()).joinToString(" ")
        val meta = RavenMetaRecursionOS.detect(metaCorpus)
        RavenOfficeBarService.signal(
            app,
            "SCREEN_SEMANTIC",
            buildString {
                append("state:visible|package:").append(escape(packageName))
                append("|text:").append(escape(textForReading.take(300)))
                viewport?.let {
                    if (it.title.isNotBlank()) append("|viewport_title:").append(escape(it.title.take(100)))
                    append("|viewport_subject:").append(escape(it.subject.take(180)))
                    if (it.selected.isNotBlank()) append("|viewport_selected:").append(escape(it.selected.take(120)))
                    if (it.focused.isNotBlank()) append("|viewport_focused:").append(escape(it.focused.take(120)))
                    append("|task:").append(it.task)
                    append("|roles:").append(escape(it.roleSummary.take(100)))
                }
                RavenInteractionMemoryOS.latest(now)?.takeIf { it.packageName == packageName }?.let {
                    append("|interaction:").append(it.kind)
                    if (it.target.isNotBlank()) append("|interaction_target:").append(escape(it.target.take(100)))
                    if (it.direction.isNotBlank()) append("|interaction_direction:").append(it.direction)
                }
                append("|nodes:").append(visited)
                append("|keyboard:").append(keyboardLike)
                append("|meta:").append(meta)
                append("|meta_score:").append(RavenMetaRecursionOS.score(metaCorpus))
                append("|source:accessibility|viewport:true|interaction_memory:true|local:true|editable_values:false|passwords:false")
            },
        )
    }

    private fun clearReading(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_PACKAGE).remove(KEY_TEXT).remove(KEY_NODES).remove(KEY_KEYBOARD).remove(KEY_AT).remove(KEY_VIEWPORT_SIG)
            .apply()
    }

    private fun escape(text: String): String = text
        .replace('|', '/')
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}
