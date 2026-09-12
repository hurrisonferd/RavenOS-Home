package com.iappyx.launcher.ravenos

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owner-armed local OCR for Goblin Eye frames.
 * Retains bounded text plus coarse layout and normalized block geometry; raw frames are discarded.
 */
object RavenGoblinReadOS {
    data class Reading(
        val text: String,
        val top: String,
        val middle: String,
        val bottom: String,
        val blockCount: Int,
        val capturedAt: Long,
    ) {
        fun leastBusyZone(): String {
            val sizes = listOf("top" to top.length, "middle" to middle.length, "bottom" to bottom.length)
            return sizes.minByOrNull { it.second }?.first ?: "top"
        }
    }

    private const val PREFS = "ravenos_goblin_read_v1"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TEXT = "last_text"
    private const val KEY_TOP = "top"
    private const val KEY_MIDDLE = "middle"
    private const val KEY_BOTTOM = "bottom"
    private const val KEY_BLOCKS = "blocks"
    private const val KEY_AT = "last_at"
    private const val MIN_INTERVAL_MS = 3_200L
    private const val TTL_MS = 20_000L

    private val inFlight = AtomicBoolean(false)
    @Volatile private var lastSubmitAt = 0L
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    fun isEnabled(context: Context): Boolean = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) { clearReading(app); RavenScreenMapOS.clear(app) }
        RavenOfficeBarService.signal(
            app, "SCREEN_TEXT",
            if (enabled) "state:armed|layout:geometry|local:true|cloud:false|raw_persist:false" else "state:disabled",
        )
    }

    fun compact(context: Context): String = buildString {
        append("GOBLIN_READ=").append(if (isEnabled(context)) "ON" else "OFF")
        latest(context)?.let { append(" · OCR=").append(it.blockCount).append(" blocks · QUIET_ZONE=").append(it.leastBusyZone().uppercase()) }
    }

    fun latest(context: Context, now: Long = System.currentTimeMillis()): Reading? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val at = prefs.getLong(KEY_AT, 0L)
        val text = prefs.getString(KEY_TEXT, "").orEmpty()
        if (text.isBlank() || at <= 0L || now - at > TTL_MS) return null
        return Reading(
            text, prefs.getString(KEY_TOP, "").orEmpty(), prefs.getString(KEY_MIDDLE, "").orEmpty(),
            prefs.getString(KEY_BOTTOM, "").orEmpty(), prefs.getInt(KEY_BLOCKS, 0), at,
        )
    }

    /** Takes ownership of [bitmap] and always recycles it. */
    fun submit(context: Context, bitmap: Bitmap) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        if (!isEnabled(app) || now - lastSubmitAt < MIN_INTERVAL_MS || !inFlight.compareAndSet(false, true)) {
            bitmap.recycle(); return
        }
        lastSubmitAt = now
        val frameWidth = bitmap.width.coerceAtLeast(1)
        val frameHeight = bitmap.height.coerceAtLeast(1)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val all = mutableListOf<String>()
                val top = mutableListOf<String>()
                val middle = mutableListOf<String>()
                val bottom = mutableListOf<String>()
                val geometry = mutableListOf<RavenScreenMapOS.Block>()

                result.textBlocks.take(24).forEach { block ->
                    val clean = normalizePiece(block.text)
                    if (clean.isBlank()) return@forEach
                    all += clean
                    val rect = block.boundingBox
                    val centerY = rect?.centerY() ?: frameHeight / 2
                    when {
                        centerY < frameHeight / 3 -> top += clean
                        centerY < frameHeight * 2 / 3 -> middle += clean
                        else -> bottom += clean
                    }
                    if (rect != null) geometry += RavenScreenMapOS.Block(clean, rect.left, rect.top, rect.right, rect.bottom)
                }

                val normalized = normalize(all.joinToString(" · "))
                when {
                    normalized.isBlank() -> Unit
                    looksSensitive(normalized) -> {
                        clearReading(app); RavenScreenMapOS.clear(app)
                        RavenOfficeBarService.signal(app, "SCREEN_TEXT", "state:suppressed_sensitive|local:true|cloud:false|raw_persist:false")
                    }
                    else -> {
                        RavenScreenMapOS.record(app, frameWidth, frameHeight, geometry)
                        val topText = normalize(top.joinToString(" · ")).take(120)
                        val middleText = normalize(middle.joinToString(" · ")).take(120)
                        val bottomText = normalize(bottom.joinToString(" · ")).take(120)
                        val prior = latest(app)?.text.orEmpty()
                        if (normalized != prior) {
                            val captured = System.currentTimeMillis()
                            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                .putString(KEY_TEXT, normalized).putString(KEY_TOP, topText).putString(KEY_MIDDLE, middleText)
                                .putString(KEY_BOTTOM, bottomText).putInt(KEY_BLOCKS, result.textBlocks.size).putLong(KEY_AT, captured).apply()
                            val meta = RavenMetaRecursionOS.detect(normalized)
                            val map = RavenScreenMapOS.latest(app)
                            RavenOfficeBarService.signal(
                                app, "SCREEN_TEXT",
                                buildString {
                                    append("state:visible|text:").append(escape(normalized.take(180)))
                                    append("|top:").append(escape(topText.take(60)))
                                    append("|middle:").append(escape(middleText.take(60)))
                                    append("|bottom:").append(escape(bottomText.take(60)))
                                    append("|blocks:").append(result.textBlocks.size)
                                    append("|quiet_zone:").append(map?.quietZone() ?: leastBusy(topText, middleText, bottomText))
                                    append("|meta:").append(meta)
                                    append("|local:true|cloud:false|raw_persist:false")
                                },
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { RavenOfficeBarService.signal(app, "SCREEN_TEXT", "state:ocr_unavailable|local:true|cloud:false") }
            .addOnCompleteListener { try { bitmap.recycle() } catch (_: Throwable) {}; inFlight.set(false) }
    }

    private fun leastBusy(top: String, middle: String, bottom: String): String = listOf(
        "top" to top.length, "middle" to middle.length, "bottom" to bottom.length,
    ).minByOrNull { it.second }?.first ?: "top"

    private fun normalizePiece(raw: String): String = raw.lineSequence()
        .map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.length >= 2 }.take(3).joinToString(" ").take(120)

    private fun normalize(raw: String): String = raw.replace(Regex("\\s+"), " ").trim().take(300)

    private fun looksSensitive(text: String): Boolean {
        val t = text.lowercase()
        return listOf("password", "passcode", "verification code", "one-time code", "one time code", "security code",
            "authentication code", "2fa", "otp", "credit card", "card number", "social security", "recovery code",
            "seed phrase", "private key").any(t::contains)
    }

    private fun escape(text: String): String = text.replace('|', '/').replace('\n', ' ').replace('\r', ' ')
        .replace(Regex("\\s+"), " ").trim()

    private fun clearReading(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_TEXT).remove(KEY_TOP).remove(KEY_MIDDLE).remove(KEY_BOTTOM).remove(KEY_BLOCKS).remove(KEY_AT).apply()
    }
}
