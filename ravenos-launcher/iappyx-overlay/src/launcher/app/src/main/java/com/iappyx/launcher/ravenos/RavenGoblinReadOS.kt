package com.iappyx.launcher.ravenos

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owner-armed local OCR for Goblin Eye frames.
 *
 * Frames remain transient. Only a short normalized visible-text snippet may enter the local
 * RavenOS event stream, and obvious credential/verification surfaces are suppressed wholesale.
 */
object RavenGoblinReadOS {
    data class Reading(val text: String, val capturedAt: Long)

    private const val PREFS = "ravenos_goblin_read_v1"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TEXT = "last_text"
    private const val KEY_AT = "last_at"
    private const val MIN_INTERVAL_MS = 4_000L
    private const val TTL_MS = 20_000L

    private val inFlight = AtomicBoolean(false)
    @Volatile private var lastSubmitAt = 0L
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

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
            "SCREEN_TEXT",
            if (enabled) "state:armed|local:true|cloud:false|raw_persist:false" else "state:disabled",
        )
    }

    fun compact(context: Context): String = buildString {
        append("GOBLIN_READ=").append(if (isEnabled(context)) "ON" else "OFF")
        latest(context)?.let { append(" · TEXT=RECENT") }
    }

    fun latest(context: Context, now: Long = System.currentTimeMillis()): Reading? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val at = prefs.getLong(KEY_AT, 0L)
        val text = prefs.getString(KEY_TEXT, "").orEmpty()
        if (text.isBlank() || at <= 0L || now - at > TTL_MS) return null
        return Reading(text, at)
    }

    /** Takes ownership of [bitmap] and always recycles it. */
    fun submit(context: Context, bitmap: Bitmap) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        if (!isEnabled(app) || now - lastSubmitAt < MIN_INTERVAL_MS || !inFlight.compareAndSet(false, true)) {
            bitmap.recycle()
            return
        }
        lastSubmitAt = now
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val normalized = normalize(result.text)
                when {
                    normalized.isBlank() -> Unit
                    looksSensitive(normalized) -> {
                        clearReading(app)
                        RavenOfficeBarService.signal(
                            app,
                            "SCREEN_TEXT",
                            "state:suppressed_sensitive|local:true|cloud:false|raw_persist:false",
                        )
                    }
                    else -> {
                        val prior = latest(app)?.text.orEmpty()
                        if (normalized != prior) {
                            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                .putString(KEY_TEXT, normalized)
                                .putLong(KEY_AT, System.currentTimeMillis())
                                .apply()
                            RavenOfficeBarService.signal(
                                app,
                                "SCREEN_TEXT",
                                "state:visible|text:${escape(normalized)}|chars:${normalized.length}|local:true|cloud:false|raw_persist:false",
                            )
                        }
                    }
                }
            }
            .addOnFailureListener {
                RavenOfficeBarService.signal(app, "SCREEN_TEXT", "state:ocr_unavailable|local:true|cloud:false")
            }
            .addOnCompleteListener {
                try { bitmap.recycle() } catch (_: Throwable) {}
                inFlight.set(false)
            }
    }

    private fun normalize(raw: String): String = raw.lineSequence()
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.length >= 2 }
        .distinct()
        .take(5)
        .joinToString(" · ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(180)

    private fun looksSensitive(text: String): Boolean {
        val t = text.lowercase()
        return listOf(
            "password", "passcode", "verification code", "one-time code", "one time code",
            "security code", "authentication code", "2fa", "otp", "credit card", "card number",
            "social security", "recovery code", "seed phrase", "private key",
        ).any(t::contains)
    }

    private fun escape(text: String): String = text
        .replace('|', '/')
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(180)

    private fun clearReading(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_TEXT).remove(KEY_AT).apply()
    }
}
