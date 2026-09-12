package com.iappyx.launcher.ravenos

import android.content.Context
import kotlin.math.abs

/**
 * Bounded, local, screen-first context for Meta Goblin dialogue.
 *
 * This organ does not create new sensing authority. It composes already owner-authorized
 * Accessibility semantics, semantic viewport roles, local Goblin Read OCR, ScreenMap geometry,
 * and the current phone scene. Raw frames are never retained here.
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
        val viewport = RavenViewportSemanticsOS.latest(context, now)
        val ocr = RavenGoblinReadOS.latest(context, now)
        val map = RavenScreenMapOS.latest(context, now)

        val accessFresh = access?.takeIf { abs(now - it.capturedAt) <= 45_000L }
        val viewportFresh = viewport?.takeIf {
            abs(now - it.capturedAt) <= 45_000L && (accessFresh == null || accessFresh.packageName == it.packageName)
        }
        val ocrFresh = ocr?.takeIf { abs(now - it.capturedAt) <= 45_000L }
        val accessText = accessFresh?.text.orEmpty()
        val viewportText = viewportFresh?.phrases.orEmpty()
        val ocrText = ocrFresh?.text.orEmpty()
        val rawText = mergeEvidence(viewportText, accessText, ocrText)

        val source = buildList {
            if (viewportText.isNotBlank()) add("VIEWPORT")
            if (accessText.isNotBlank()) add("ACCESSIBILITY")
            if (ocrText.isNotBlank()) add("OCR")
        }.joinToString("+").ifBlank { "NONE" }
        val capturedAt = maxOf(
            viewportFresh?.capturedAt ?: 0L,
            accessFresh?.capturedAt ?: 0L,
            ocrFresh?.capturedAt ?: 0L,
        )
        val age = if (capturedAt > 0L) (now - capturedAt).coerceAtLeast(0L) else Long.MAX_VALUE
        val pkg = viewportFresh?.packageName ?: accessFresh?.packageName
        val app = scene.activeApp?.take(48)
        val keyboard = accessFresh?.keyboardLike == true
        val semantic = RavenAppSemanticsOS.interpret(context, pkg, app, rawText.takeIf { it.isNotBlank() }, keyboard)

        val top = listOf(viewportFresh?.title.orEmpty(), ocrFresh?.top.orEmpty())
            .filter(String::isNotBlank).joinToString(" · ").cleanZone()
        val middle = ocrFresh?.middle.orEmpty().cleanZone()
        val bottom = ocrFresh?.bottom.orEmpty().cleanZone()
        val metaCorpus = listOf(rawText, viewportFresh?.title.orEmpty(), viewportFresh?.subject.orEmpty()).joinToString(" ")
        val meta = metaCorpus.isNotBlank() && RavenMetaRecursionOS.detect(metaCorpus)
        val focus = chooseFocus(rawText, viewportFresh?.subject.orEmpty(), meta)
        val quiet = map?.quietZone(keyboard) ?: ocrFresh?.leastBusyZone() ?: "top"
        val blocks = (map?.blocks?.size ?: 0).coerceAtLeast(ocrFresh?.blockCount ?: 0).coerceAtLeast(accessFresh?.nodeCount ?: 0)
        val confidence = when {
            rawText.isBlank() || focus.isBlank() -> 0
            viewportText.isNotBlank() && accessText.isNotBlank() && ocrText.isNotBlank() && age <= 6_000L -> 99
            viewportText.isNotBlank() && accessText.isNotBlank() && age <= 8_000L -> 98
            accessText.isNotBlank() && ocrText.isNotBlank() && age <= 6_000L -> 97
            viewportText.isNotBlank() -> 95
            accessText.isNotBlank() -> if (age <= 6_000L) 94 else 90
            ocrText.isNotBlank() && age <= 8_000L && (ocrFresh?.blockCount ?: 0) >= 2 -> 88
            ocrText.isNotBlank() && (ocrFresh?.blockCount ?: 0) >= 1 -> 80
            else -> 64
        }
        val semanticSummary = buildString {
            append(semantic.summary)
            viewportFresh?.task?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append(" · ")
                append(it.lowercase().replace('_', ' '))
            }
            viewportFresh?.title?.takeIf { it.isNotBlank() && !it.equals(focus, true) }?.let {
                if (isNotEmpty()) append(" · ")
                append(it.take(80))
            }
        }.take(180)

        // Sensor source is not part of novelty identity. Viewport task is: reading a thread and
        // composing in the same app are different scenes even when some visible words overlap.
        val signature = if (focus.isBlank()) "" else listOf(
            semantic.kind,
            semantic.label,
            viewportFresh?.task.orEmpty(),
            viewportFresh?.title.orEmpty().lowercase().take(64),
            focus.lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").take(128),
        ).joinToString("|")

        return Snapshot(
            available = rawText.isNotBlank() && focus.isNotBlank() && confidence >= 60,
            appLabel = semantic.label.takeIf { it.isNotBlank() },
            packageName = pkg,
            semanticKind = semantic.kind,
            semanticSummary = semanticSummary,
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

    private fun mergeEvidence(vararg sources: String): String {
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
        sources.forEach(::add)
        return pieces.joinToString(" · ").take(900)
    }

    private fun chooseFocus(text: String, viewportSubject: String, meta: Boolean): String {
        if (text.isBlank() && viewportSubject.isBlank()) return ""
        val corpus = listOf(text, viewportSubject).filter(String::isNotBlank).joinToString(" · ")
        val metaFocus = RavenMetaRecursionOS.focus(corpus)?.takeIf { RavenMetaRecursionOS.detect(it) }
        if (meta && !metaFocus.isNullOrBlank()) return metaFocus.replace(Regex("\\s+"), " ").trim().take(190)
        if (viewportSubject.length in 8..220 && !isChrome(viewportSubject)) {
            return viewportSubject.replace(Regex("\\s+"), " ").trim().take(190)
        }

        val phrases = corpus
            .split(" · ", "\n", ". ", "! ", "? ")
            .map { it.replace(Regex("\\s+"), " ").trim().trim('·', '-', '|', ':') }
            .filter { it.length in 3..220 }
            .distinct()
            .filterNot(::isChrome)
            .filterNot { it.matches(Regex("^[0-9:./ -]+$")) }
            .filterNot { it.equals("ChatGPT can make mistakes", ignoreCase = true) }

        val chosen = phrases.maxByOrNull { phrase ->
            val letters = phrase.count(Char::isLetter)
            val words = phrase.split(' ').count { it.length >= 3 }
            val verbs = Regex(
                "\\b(is|are|was|were|need|needs|want|wants|build|make|improve|show|says|saying|discuss|discussing|fix|working|react|reacting|add|change|compile|read|watch|seeing|aware|reply|review|playing|searching)\\b",
                RegexOption.IGNORE_CASE,
            ).findAll(phrase).count()
            val metaScore = RavenMetaRecursionOS.score(phrase)
            (letters * 2) + (words * 8) + (verbs * 18) + (metaScore * 24) + phrase.length.coerceAtMost(110)
        } ?: corpus
        return chosen.replace(Regex("\\s+"), " ").trim().take(190)
    }

    private fun isChrome(value: String): Boolean {
        val chrome = setOf(
            "back", "home", "search", "more", "share", "copy", "cancel", "done", "ok", "close",
            "settings", "menu", "edit", "send", "next", "previous", "open", "notifications",
            "new chat", "voice", "attach", "tools", "regenerate", "ask chatgpt", "new tab", "refresh",
        )
        return value.trim().lowercase() in chrome
    }

    private fun String.cleanZone(): String = replace(Regex("\\s+"), " ").trim().take(170)
}
