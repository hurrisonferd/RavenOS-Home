package com.iappyx.launcher.ravenos

import android.graphics.Color

/**
 * Color grammar for Follow-Me. Expression moves into accent, gradient and border instead of emoji spam.
 */
object RavenOverlayPaletteOS {
    data class Palette(
        val start: Int,
        val end: Int,
        val stroke: Int,
        val status: Int,
        val owner: Int,
        val body: Int,
        val context: Int,
    )

    fun resolve(accentRaw: Int, mode: String, signal: String, detail: String): Palette {
        val accent = readable(accentRaw)
        val semantic = semanticTint(signal, detail)
        val modeBias = when (mode.uppercase()) {
            "COMMENT" -> 0.58f
            "OBSERVING" -> 0.40f
            "FEED" -> 0.34f
            else -> 0.28f
        }
        val active = mix(accent, semantic, if (semantic == 0) 0f else 0.34f)
        val darkA = mix(Color.rgb(10, 10, 16), active, modeBias * 0.34f)
        val darkB = mix(Color.rgb(18, 16, 26), active, modeBias * 0.18f)
        val stroke = mix(active, Color.WHITE, if (mode.equals("COMMENT", true)) 0.12f else 0.02f)
        val status = mix(active, Color.WHITE, 0.42f)
        val owner = mix(active, Color.WHITE, 0.20f)
        val body = mix(Color.rgb(236, 236, 246), active, 0.07f)
        val context = mix(Color.rgb(190, 190, 205), active, 0.10f)
        return Palette(darkA, darkB, stroke, status, owner, body, context)
    }

    private fun semanticTint(signal: String, detail: String): Int {
        val s = signal.uppercase()
        return when {
            detail.contains("meta:true", true) || detail.contains("meta_level:4", true) || detail.contains("meta_level:5", true) -> Color.rgb(190, 105, 255)
            detail.contains("gold_phase:CALLBACK", true) || detail.contains("callback", true) -> Color.rgb(255, 191, 58)
            detail.contains("gold_phase:ESCALATE", true) || detail.contains("ERROR", true) -> Color.rgb(255, 92, 108)
            detail.contains("gold_phase:CLOSE", true) || detail.contains("PAYOFF", true) -> Color.rgb(91, 224, 148)
            s.contains("MEDIA") || detail.contains("track:", true) -> Color.rgb(255, 83, 174)
            s.contains("SCREEN") || s.contains("EYE") -> Color.rgb(92, 203, 255)
            s.contains("NOTIFICATION") -> Color.rgb(255, 177, 67)
            s.contains("BATTERY") || s.contains("POWER") -> Color.rgb(115, 225, 128)
            s.contains("SYSTEM") || s.contains("SETTING") -> Color.rgb(138, 151, 255)
            else -> 0
        }
    }

    private fun readable(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        if (perceived >= 138) return color
        return Color.rgb((Color.red(color) + 255) / 2, (Color.green(color) + 255) / 2, (Color.blue(color) + 255) / 2)
    }

    private fun mix(a: Int, b: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        fun c(x: Int, y: Int): Int = (x + (y - x) * t).toInt().coerceIn(0, 255)
        return Color.rgb(c(Color.red(a), Color.red(b)), c(Color.green(a), Color.green(b)), c(Color.blue(a), Color.blue(b)))
    }
}
