package com.iappyx.launcher.ravenos

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Owner-armed semantic viewport model for Follow-Me Office.
 *
 * This does not add authority beyond RavenAccessibilityReadOS. It restructures already-visible,
 * non-editable Accessibility content into a bounded page/chat/task model so Goblin Vision can
 * understand what Raven is looking at instead of flattening the entire window into one sentence.
 * Password nodes abort the read. Editable values are never retained.
 */
object RavenViewportSemanticsOS {
    data class Viewport(
        val packageName: String,
        val title: String,
        val subject: String,
        val task: String,
        val roleSummary: String,
        val phrases: String,
        val capturedAt: Long,
        val meta: Boolean,
    )

    private data class Fact(
        val text: String,
        val role: String,
        val top: Int,
        val bottom: Int,
        val score: Int,
    )

    private const val PREFS = "ravenos_viewport_semantics_v1"
    private const val TTL_MS = 45_000L
    private const val MAX_NODES = 180
    private const val MAX_DEPTH = 12

    fun latest(context: Context, now: Long = System.currentTimeMillis()): Viewport? {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val at = p.getLong("at", 0L)
        if (at <= 0L || now - at > TTL_MS) return null
        val pkg = p.getString("package", "").orEmpty()
        val subject = p.getString("subject", "").orEmpty()
        if (pkg.isBlank() || subject.isBlank()) return null
        return Viewport(
            packageName = pkg,
            title = p.getString("title", "").orEmpty(),
            subject = subject,
            task = p.getString("task", "VIEWING").orEmpty(),
            roleSummary = p.getString("roles", "").orEmpty(),
            phrases = p.getString("phrases", "").orEmpty(),
            capturedAt = at,
            meta = p.getBoolean("meta", false),
        )
    }

    fun observe(context: Context, packageName: String, root: AccessibilityNodeInfo?): Viewport? {
        val app = context.applicationContext
        if (!RavenAccessibilityReadOS.isEnabled(app) || root == null || packageName == app.packageName) return null

        val facts = ArrayList<Fact>()
        val roles = linkedMapOf<String, Int>()
        var visited = 0
        var sensitive = false
        var editablePresent = false
        var scrollablePresent = false
        val rootBounds = Rect().also { runCatching { root.getBoundsInScreen(it) } }
        val screenHeight = rootBounds.height().takeIf { it > 0 } ?: 2400

        fun addRole(role: String) { roles[role] = (roles[role] ?: 0) + 1 }
        fun roleFor(node: AccessibilityNodeInfo, text: String): String {
            val cls = node.className?.toString().orEmpty().lowercase()
            val id = node.viewIdResourceName.orEmpty().lowercase()
            val lower = text.lowercase()
            val heading = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && runCatching { node.isHeading }.getOrDefault(false)
            return when {
                heading -> "HEADING"
                id.contains("tab") || lower.endsWith(" tab") || lower.startsWith("tab ") -> "TAB"
                cls.contains("button") || node.isClickable && text.length <= 48 -> "BUTTON"
                cls.contains("image") -> "IMAGE_LABEL"
                node.isClickable -> "LINK"
                else -> "TEXT"
            }
        }

        fun score(text: String, role: String, top: Int, bottom: Int): Int {
            val center = (top + bottom) / 2
            val centerBias = when {
                center in (screenHeight / 5)..(screenHeight * 4 / 5) -> 32
                center in 0..(screenHeight / 5) -> 10
                else -> 16
            }
            val roleWeight = when (role) {
                "HEADING" -> 62
                "TAB" -> 42
                "LINK" -> 22
                "BUTTON" -> 14
                "IMAGE_LABEL" -> 10
                else -> 28
            }
            val lengthWeight = when (text.length) {
                in 24..150 -> 38
                in 12..220 -> 24
                else -> 8
            }
            val verbWeight = Regex("\\b(is|are|was|were|need|want|build|make|show|look|read|watch|discuss|fix|working|add|change|reply|message|playing|search|download)\\b", RegexOption.IGNORE_CASE)
                .findAll(text).count() * 18
            val metaWeight = RavenMetaRecursionOS.score(text) * 28
            return centerBias + roleWeight + lengthWeight + verbWeight + metaWeight
        }

        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > MAX_DEPTH || visited >= MAX_NODES || sensitive) return
            visited++
            if (node.isPassword) { sensitive = true; return }
            if (node.isEditable) editablePresent = true
            if (node.isScrollable) scrollablePresent = true

            if (node.isVisibleToUser && !node.isEditable) {
                val candidates = listOf(node.text, node.contentDescription)
                    .mapNotNull { it?.toString()?.replace(Regex("\\s+"), " ")?.trim() }
                    .filter { it.length in 2..220 }
                    .distinct()
                val rect = Rect().also { runCatching { node.getBoundsInScreen(it) } }
                for (text in candidates) {
                    if (facts.any { it.text.equals(text, true) }) continue
                    val role = roleFor(node, text)
                    addRole(role)
                    facts += Fact(text, role, rect.top, rect.bottom, score(text, role, rect.top, rect.bottom))
                    if (facts.size >= 64) break
                }
            }
            val count = node.childCount.coerceAtMost(40)
            for (i in 0 until count) walk(node.getChild(i), depth + 1)
        }
        walk(root, 0)

        if (sensitive) { clear(app); return null }
        if (facts.isEmpty()) return null

        val chrome = setOf(
            "back", "home", "more", "menu", "share", "copy", "close", "cancel", "done", "open",
            "previous", "next", "settings", "notifications", "search", "refresh", "new tab", "new chat",
            "ask chatgpt", "voice", "attach", "tools", "send",
        )
        val meaningful = facts
            .filterNot { it.text.lowercase() in chrome }
            .filterNot { it.text.matches(Regex("^[0-9:./% -]+$")) }
            .sortedByDescending { it.score }
        if (meaningful.isEmpty()) return null

        val title = meaningful.firstOrNull { it.role == "HEADING" }?.text
            ?: meaningful.firstOrNull { it.role == "TAB" && it.text.length > 3 }?.text
            ?: meaningful.firstOrNull { it.top in 0..(screenHeight / 3) && it.text.length in 4..100 }?.text
            ?: ""
        val subject = (meaningful.firstOrNull { it.text != title } ?: meaningful.first()).text.take(190)
        val phrasePack = meaningful.take(12).map { it.text }.distinct().joinToString(" · ").take(720)
        val allText = meaningful.take(24).joinToString(" ") { it.text }.lowercase()
        val task = inferTask(packageName, allText, editablePresent, scrollablePresent, roles.keys)
        val roleSummary = roles.entries.sortedByDescending { it.value }.take(5)
            .joinToString(",") { "${it.key}:${it.value}" }
        val meta = RavenMetaRecursionOS.detect("$title $subject $phrasePack")
        val now = System.currentTimeMillis()
        val viewport = Viewport(packageName, title.take(120), subject, task, roleSummary, phrasePack, now, meta)

        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("package", viewport.packageName)
            .putString("title", viewport.title)
            .putString("subject", viewport.subject)
            .putString("task", viewport.task)
            .putString("roles", viewport.roleSummary)
            .putString("phrases", viewport.phrases)
            .putLong("at", viewport.capturedAt)
            .putBoolean("meta", viewport.meta)
            .apply()
        return viewport
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun inferTask(
        packageName: String,
        text: String,
        editablePresent: Boolean,
        scrollablePresent: Boolean,
        roles: Set<String>,
    ): String {
        val pkg = packageName.lowercase()
        fun has(vararg terms: String) = terms.any(text::contains)
        return when {
            editablePresent && has("send", "reply", "message", "chat", "ask chatgpt") -> "COMPOSING"
            editablePresent && has("search", "find", "address") -> "SEARCHING"
            pkg.contains("settings") || has("permission", "accessibility", "appear on top") -> "CONFIGURING"
            has("pull request", "commit", "workflow", "kotlin", "build", "github") -> "DEBUGGING"
            has("playing", "pause", "album", "track", "song") -> "LISTENING"
            has("video", "comments", "youtube") -> "WATCHING"
            "TAB" in roles || pkg.contains("chrome") || pkg.contains("browser") -> if (scrollablePresent) "BROWSING" else "WEB"
            scrollablePresent && (has("chat", "conversation", "reply") || pkg.contains("openai")) -> "READING_CHAT"
            scrollablePresent -> "READING"
            editablePresent -> "TYPING"
            else -> "VIEWING"
        }
    }
}
