package com.iappyx.launcher.ravenos

import android.content.Context
import kotlin.math.abs

/**
 * Bounded, local, screen-first context for Meta Goblin dialogue.
 *
 * This organ does not create new sensing authority. It only composes already owner-authorized
 * Accessibility semantics, local Goblin Read OCR, ScreenMap geometry, and the current phone scene.
 * Raw frames are never retained here. A missing trustworthy read is represented as missing context.
 */
object RavenScreenContextOS {
    data class Snapshot(
        val available: Boolean,
        val appLabel: String?,
        val packageName: String?,
        val semanticKind: String,
        val semanticSummary: String,
        val text: String,
        val focus: String,
        val top: String,
        val middle: String,
        val bottom: String,
        val source: String,
        val capturedAt: Long,
        val ageMs: Long,
        val meta: Boolean,
        val keyboardLike: Boolean,
        val blockCount: Int,
        val quietZone: String,
        val signature: String,
    )

    fun snapshot(context: Context, now: Long = System.currentTimeMillis()): Snapshot {
        val scene = RavenPhoneSceneOS.snapshot(context, now)
        val access = RavenAccessibilityReadOS.latest(context, now)
        val ocr = RavenGoblinReadOS.latest(context, now)
        val map = RavenScreenMapOS.latest(context, now)

        val accessFresh = access?.takeIf { abs(now - it.capturedAt) <= 12_000L }
        val ocrFresh = ocr?.takeIf { abs(now - it.capturedAt) <= 18_000L }
        val useAccess = accessFresh != null

        val rawText = when {
            useAccess -> accessFresh!!.text
            ocrFresh != null -> ocrFresh.text
            else -> ""
        }.replace(Regex("\\s+"), " ").trim().take(300)

        val source = when {
            useAccess -> "ACCESSIBILITY"
            ocrFresh != null -> "OCR"
            else -> "NONE"
        }
        val capturedAt = when {
            useAccess -> accessFresh!!.capturedAt
            ocrFresh != null -> ocrFresh.capturedAt
            else -> 0L
        }
        val age = if (capturedAt > 0L) (now - capturedAt).coerceAtLeast(0L) else Long.MAX_VALUE
        val pkg = accessFresh?.packageName
        val app = scene.activeApp?.take(48)
        val keyboard = accessFresh?.keyboardLike == true
        val semantic = RavenAppSemanticsOS.interpret(context, pkg, app, rawText.takeIf { it.isNotBlank() }, keyboard)

        val top = if (useAccess) "" else ocrFresh?.top.orEmpty().cleanZone()
        val middle = if (useAccess) "" else ocrFresh?.middle.orEmpty().cleanZone()
        val bottom = if (useAccess) "" else ocrFresh?.bottom.orEmpty().cleanZone()
        val focus = chooseFocus(rawText)
        val meta = rawText.isNotBlank() && RavenMetaRecursionOS.detect(rawText)
        val quiet = map?.quietZone(keyboard) ?: ocrFresh?.leastBusyZone() ?: "top"
        val blocks = map?.blocks?.size ?: ocrFresh?.blockCount ?: accessFresh?.nodeCount ?: 0
        val signature = if (focus.isBlank()) "" else listOf(
            semantic.kind,
            semantic.label,
            focus.lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").take(96),
            source,
        ).joinToString("|")

        return Snapshot(
            available = rawText.isNotBlank() && focus.isNotBlank(),
            appLabel = semantic.label.takeIf { it.isNotBlank() },
            packageName = pkg,
            semanticKind = semantic.kind,
            semanticSummary = semantic.summary,
            text = rawText,
            focus = focus,
            top = top,
            middle = middle,
            bottom = bottom,
            source = source,
            capturedAt = capturedAt,
            ageMs = age,
            meta = meta,
            keyboardLike = keyboard,
            blockCount = blocks,
            quietZone = quiet,
            signature = signature,
        )
    }

    private fun chooseFocus(text: String): String {
        if (text.isBlank()) return ""
        val chrome = setOf(
            "back", "home", "search", "more", "share", "copy", "cancel", "done", "ok", "close",
            "settings", "menu", "edit", "send", "next", "previous", "open", "notifications",
        )
        val phrases = text
            .split(" · ", "\n")
            .map { it.replace(Regex("\\s+"), " ").trim().trim('·', '-', '|') }
            .filter { it.length in 3..140 }
            .distinct()
            .filterNot { it.lowercase() in chrome }
            .filterNot { it.matches(Regex("^[0-9:./ -]+$")) }
            .filterNot { it.equals("ChatGPT can make mistakes", ignoreCase = true) }
        val chosen = phrases.maxByOrNull { phrase ->
            val letters = phrase.count(Char::isLetter)
            val words = phrase.split(' ').count { it.length >= 3 }
            (letters * 2) + (words * 8) + phrase.length.coerceAtMost(72)
        } ?: text
        return chosen.replace(Regex("\\s+"), " ").trim().take(112)
    }

    private fun String.cleanZone(): String = replace(Regex("\\s+"), " ").trim().take(96)
}
