package com.iappyx.launcher.ravenos

import android.content.Context
import kotlin.math.abs

/**
 * Bounded, local, screen-first context for Meta Goblin dialogue.
 *
 * This organ does not create new sensing authority. It composes already owner-authorized
 * Accessibility semantics, local Goblin Read OCR, ScreenMap geometry, and the current phone scene.
 * Raw frames are never retained here. Missing trustworthy reads remain missing rather than guessed.
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
        val confidence: Int,
    )

    fun snapshot(context: Context, now: Long = System.currentTimeMillis()): Snapshot {
        val scene = RavenPhoneSceneOS.snapshot(context, now)
        val access = RavenAccessibilityReadOS.latest(context, now)
        val ocr = RavenGoblinReadOS.latest(context, now)
        val map = RavenScreenMapOS.latest(context, now)

        val accessFresh = access?.takeIf { abs(now - it.capturedAt) <= 45_000L }
        val ocrFresh = ocr?.takeIf { abs(now - it.capturedAt) <= 45_000L }
        val accessText = accessFresh?.text.orEmpty()
        val ocrText = ocrFresh?.text.orEmpty()
        val rawText = mergeEvidence(accessText, ocrText)

        val source = when {
            accessText.isNotBlank() && ocrText.isNotBlank() -> "ACCESSIBILITY+OCR"
            accessText.isNotBlank() -> "ACCESSIBILITY"
            ocrText.isNotBlank() -> "OCR"
            else -> "NONE"
        }
        val capturedAt = maxOf(accessFresh?.capturedAt ?: 0L, ocrFresh?.capturedAt ?: 0L)
        val age = if (capturedAt > 0L) (now - capturedAt).coerceAtLeast(0L) else Long.MAX_VALUE
        val pkg = accessFresh?.packageName
        val app = scene.activeApp?.take(48)
        val keyboard = accessFresh?.keyboardLike == true
        val semantic = RavenAppSemanticsOS.interpret(context, pkg, app, rawText.takeIf { it.isNotBlank() }, keyboard)

        val top = ocrFresh?.top.orEmpty().cleanZone()
        val middle = ocrFresh?.middle.orEmpty().cleanZone()
        val bottom = ocrFresh?.bottom.orEmpty().cleanZone()
        val focus = chooseFocus(rawText)
        val meta = rawText.isNotBlank() && RavenMetaRecursionOS.detect(rawText)
        val quiet = map?.quietZone(keyboard) ?: ocrFresh?.leastBusyZone() ?: "top"
        val blocks = (map?.blocks?.size ?: 0).coerceAtLeast(ocrFresh?.blockCount ?: 0).coerceAtLeast(accessFresh?.nodeCount ?: 0)
        val confidence = when {
            rawText.isBlank() || focus.isBlank() -> 0
            accessText.isNotBlank() && ocrText.isNotBlank() && age <= 6_000L -> 99
            accessText.isNotBlank() && ocrText.isNotBlank() -> 96
            accessText.isNotBlank() && age <= 6_000L -> 95
            accessText.isNotBlank() -> 91
            ocrText.isNotBlank() && age <= 8_000L && (ocrFresh?.blockCount ?: 0) >= 2 -> 88
            ocrText.isNotBlank() && (ocrFresh?.blockCount ?: 0) >= 1 -> 80
            else -> 64
        }

        // Do not include the evidence source in novelty identity. Accessibility and OCR are two
        // witnesses to one screen subject, not two separate topics deserving duplicate dialogue.
        val signature = if (focus.isBlank()) "" else listOf(
            semantic.kind,
            semantic.label,
            focus.lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").take(128),
        ).joinToString("|")

        return Snapshot(
            available = rawText.isNotBlank() && focus.isNotBlank() && confidence >= 60,
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
            confidence = confidence,
        )
    }

    private fun mergeEvidence(access: String, ocr: String): String {
        val pieces = ArrayList<String>()
        fun add(raw: String) {
            raw.split(" · ", "\n")
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { it.length >= 2 }
                .forEach { piece ->
                    val key = piece.lowercase()
                    if (pieces.none { it.lowercase() == key }) pieces += piece
                }
        }
        add(access)
        add(ocr)
        return pieces.joinToString(" · ").take(620)
    }

    private fun chooseFocus(text: String): String {
        if (text.isBlank()) return ""
        val chrome = setOf(
            "back", "home", "search", "more", "share", "copy", "cancel", "done", "ok", "close",
            "settings", "menu", "edit", "send", "next", "previous", "open", "notifications",
            "new chat", "voice", "attach", "tools", "regenerate", "ask chatgpt",
        )
        val phrases = text
            .split(" · ", "\n", ". ", "! ", "? ")
            .map { it.replace(Regex("\\s+"), " ").trim().trim('·', '-', '|', ':') }
            .filter { it.length in 3..220 }
            .distinct()
            .filterNot { it.lowercase() in chrome }
            .filterNot { it.matches(Regex("^[0-9:./ -]+$")) }
            .filterNot { it.equals("ChatGPT can make mistakes", ignoreCase = true) }

        val metaFocus = RavenMetaRecursionOS.focus(text)?.takeIf { RavenMetaRecursionOS.detect(it) }
        val chosen = metaFocus ?: phrases.maxByOrNull { phrase ->
            val letters = phrase.count(Char::isLetter)
            val words = phrase.split(' ').count { it.length >= 3 }
            val verbs = Regex(
                "\\b(is|are|was|were|need|needs|want|wants|build|make|improve|show|says|saying|discuss|discussing|fix|working|react|reacting|add|change|compile|read|watch|seeing|aware)\\b",
                RegexOption.IGNORE_CASE,
            ).findAll(phrase).count()
            val meta = RavenMetaRecursionOS.score(phrase)
            (letters * 2) + (words * 8) + (verbs * 18) + (meta * 24) + phrase.length.coerceAtMost(110)
        } ?: text
        return chosen.replace(Regex("\\s+"), " ").trim().take(170)
    }

    private fun String.cleanZone(): String = replace(Regex("\\s+"), " ").trim().take(150)
}
