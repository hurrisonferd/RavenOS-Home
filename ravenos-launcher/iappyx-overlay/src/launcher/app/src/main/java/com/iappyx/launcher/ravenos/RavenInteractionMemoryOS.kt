package com.iappyx.launcher.ravenos

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.util.ArrayDeque

/**
 * Bounded process-session interaction memory from owner-enabled Accessibility events.
 *
 * It remembers structural actions (tap/select/scroll/focus/content/window) and, only when the
 * explicit Accessibility Read switch is armed, a short non-editable/non-password target label.
 * Editable values, password text, raw event text from typing, and persistent transcripts are never
 * retained here.
 */
object RavenInteractionMemoryOS {
    data class Interaction(
        val packageName: String,
        val kind: String,
        val target: String,
        val direction: String,
        val repeated: Int,
        val at: Long,
    ) {
        fun compact(): String = buildString {
            append(kind)
            if (target.isNotBlank()) append(':').append(target.take(70))
            if (direction.isNotBlank()) append(':').append(direction)
            if (repeated > 1) append('×').append(repeated)
        }
    }

    private val recent = ArrayDeque<Interaction>()
    private const val MAX_RECENT = 16
    private const val TTL_MS = 18_000L
    private var lastKey = ""
    private var repeatCount = 0

    @Synchronized
    fun observe(event: AccessibilityEvent, allowLabels: Boolean) {
        val pkg = event.packageName?.toString()?.trim().orEmpty()
        if (pkg.isBlank()) return
        val kind = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "TAP"
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "LONG_PRESS"
            AccessibilityEvent.TYPE_VIEW_SELECTED -> "SELECT"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "FOCUS"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "SCROLL"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "CONTENT"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TYPING"
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "TEXT_SELECTION"
            else -> return
        }

        val source = runCatching { event.source }.getOrNull()
        val sensitive = source?.isPassword == true || source?.isEditable == true
        val safeLabel = if (!allowLabels || sensitive || kind in setOf("TYPING", "TEXT_SELECTION")) "" else {
            listOfNotNull(
                source?.contentDescription?.toString(),
                source?.text?.toString(),
                event.contentDescription?.toString(),
            )
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .firstOrNull { it.length in 2..120 && !looksSensitive(it) }
                .orEmpty()
        }
        val direction = if (kind == "SCROLL") scrollDirection(event) else ""
        val key = "$pkg|$kind|${safeLabel.lowercase()}|$direction"
        repeatCount = if (key == lastKey) (repeatCount + 1).coerceAtMost(99) else 1
        lastKey = key
        recent.addLast(Interaction(pkg, kind, safeLabel.take(120), direction, repeatCount, System.currentTimeMillis()))
        while (recent.size > MAX_RECENT) recent.removeFirst()
    }

    @Synchronized
    fun latest(now: Long = System.currentTimeMillis()): Interaction? = recent.peekLast()?.takeIf { now - it.at <= TTL_MS }

    @Synchronized
    fun recent(now: Long = System.currentTimeMillis(), limit: Int = 8): List<Interaction> = recent
        .filter { now - it.at <= TTL_MS * 2 }
        .takeLast(limit.coerceIn(1, MAX_RECENT))

    @Synchronized
    fun clear() {
        recent.clear()
        lastKey = ""
        repeatCount = 0
    }

    private fun scrollDirection(event: AccessibilityEvent): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dy = runCatching { event.scrollDeltaY }.getOrDefault(0)
            if (dy > 0) return "DOWN"
            if (dy < 0) return "UP"
            val dx = runCatching { event.scrollDeltaX }.getOrDefault(0)
            if (dx > 0) return "RIGHT"
            if (dx < 0) return "LEFT"
        }
        return ""
    }

    private fun looksSensitive(text: String): Boolean {
        val t = text.lowercase()
        return listOf(
            "password", "passcode", "verification code", "one-time code", "security code", "authentication code",
            "otp", "2fa", "credit card", "card number", "social security", "recovery code", "seed phrase", "private key",
        ).any(t::contains)
    }
}
