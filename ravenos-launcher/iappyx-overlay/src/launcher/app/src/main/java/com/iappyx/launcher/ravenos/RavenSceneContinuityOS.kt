package com.iappyx.launcher.ravenos

import android.content.Context
import java.security.MessageDigest

/**
 * Bounded continuity coordinate for the scene inside the current foreground app.
 * AppSession owns the room; this organ only tracks the beat inside that room.
 */
object RavenSceneContinuityOS {
    private const val PREFS = "ravenos_scene_continuity_v1"
    private const val KEY_PACKAGE = "package"
    private const val KEY_SIGNATURE = "signature"
    private const val KEY_FOCUS = "focus"
    private const val KEY_TITLE = "title"
    private const val KEY_ENTERED_AT = "entered_at"
    private const val KEY_LAST_SEEN_AT = "last_seen_at"
    private const val KEY_CHANGE_COUNT = "change_count"
    private const val KEY_RETURN_COUNT = "return_count"
    private const val KEY_PREVIOUS_SIGNATURE = "previous_signature"
    private const val KEY_PREVIOUS_FOCUS = "previous_focus"

    data class Beat(
        val packageName: String,
        val beat: String,
        val focus: String,
        val title: String,
        val dwellMs: Long,
        val changeCount: Int,
        val returnCount: Int,
        val previousFocus: String,
        val signatureHash: String,
    ) {
        fun compact(): String = buildString {
            append("SCENE_BEAT=").append(beat)
            if (dwellMs > 0L) append("/dwell=").append(dwellMs / 1000L).append('s')
            if (changeCount > 0) append("/changes=").append(changeCount)
            if (returnCount > 0) append("/returns=").append(returnCount)
            if (focus.isNotBlank()) append("/focus=").append(focus.take(72))
        }
    }

    fun observe(
        context: Context,
        packageName: String?,
        semanticSignature: String,
        focus: String,
        title: String,
        interactionKind: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Beat? {
        val pkg = packageName?.trim().orEmpty()
        val sig = semanticSignature.trim()
        if (pkg.isBlank() || sig.isBlank()) return current(context, pkg.takeIf(String::isNotBlank), now)
        val hash = hash(sig)
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldPkg = p.getString(KEY_PACKAGE, "").orEmpty()
        val oldHash = p.getString(KEY_SIGNATURE, "").orEmpty()
        val oldFocus = p.getString(KEY_FOCUS, "").orEmpty()
        val oldTitle = p.getString(KEY_TITLE, "").orEmpty()
        val previousHash = p.getString(KEY_PREVIOUS_SIGNATURE, "").orEmpty()
        val enteredAt = p.getLong(KEY_ENTERED_AT, now)
        val oldChanges = p.getInt(KEY_CHANGE_COUNT, 0)
        val oldReturns = p.getInt(KEY_RETURN_COUNT, 0)
        val priorFocus = p.getString(KEY_PREVIOUS_FOCUS, "").orEmpty()

        val sameApp = oldPkg == pkg
        val sameScene = sameApp && oldHash == hash
        val returned = sameApp && previousHash.isNotBlank() && previousHash == hash && oldHash != hash
        val focusChanged = sameApp && !sameScene && oldFocus.isNotBlank() && focus.isNotBlank() && oldTitle == title
        val interaction = interactionKind.orEmpty().uppercase()
        val beat = when {
            !sameApp -> "ROOM_ENTER"
            sameScene && interaction.contains("SCROLL") -> "SCROLL"
            sameScene && interaction.contains("FOCUS") -> "FOCUS_STAY"
            sameScene -> "STAY"
            returned -> "PAGE_RETURN"
            focusChanged -> "FOCUS_SHIFT"
            else -> "PAGE_CHANGE"
        }
        val nextEntered = if (!sameApp || !sameScene) now else enteredAt
        val changes = if (!sameApp) 0 else if (!sameScene) oldChanges + 1 else oldChanges
        val returns = if (returned) oldReturns + 1 else oldReturns

        p.edit()
            .putString(KEY_PACKAGE, pkg)
            .putString(KEY_PREVIOUS_SIGNATURE, if (!sameScene && oldHash.isNotBlank()) oldHash else previousHash)
            .putString(KEY_PREVIOUS_FOCUS, if (!sameScene && oldFocus.isNotBlank()) oldFocus.take(140) else priorFocus)
            .putString(KEY_SIGNATURE, hash)
            .putString(KEY_FOCUS, focus.clean(140))
            .putString(KEY_TITLE, title.clean(120))
            .putLong(KEY_ENTERED_AT, nextEntered)
            .putLong(KEY_LAST_SEEN_AT, now)
            .putInt(KEY_CHANGE_COUNT, changes)
            .putInt(KEY_RETURN_COUNT, returns)
            .apply()

        return Beat(
            pkg,
            beat,
            focus.clean(140),
            title.clean(120),
            (now - nextEntered).coerceAtLeast(0L),
            changes,
            returns,
            (if (!sameScene && oldFocus.isNotBlank()) oldFocus else priorFocus).clean(140),
            hash,
        )
    }

    fun current(context: Context, expectedPackage: String? = null, now: Long = System.currentTimeMillis()): Beat? {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pkg = p.getString(KEY_PACKAGE, "").orEmpty()
        if (pkg.isBlank() || (expectedPackage != null && expectedPackage != pkg)) return null
        val seen = p.getLong(KEY_LAST_SEEN_AT, 0L)
        if (seen <= 0L || now - seen > 10 * 60_000L) return null
        val entered = p.getLong(KEY_ENTERED_AT, seen)
        return Beat(
            packageName = pkg,
            beat = "STAY",
            focus = p.getString(KEY_FOCUS, "").orEmpty().clean(140),
            title = p.getString(KEY_TITLE, "").orEmpty().clean(120),
            dwellMs = (now - entered).coerceAtLeast(0L),
            changeCount = p.getInt(KEY_CHANGE_COUNT, 0),
            returnCount = p.getInt(KEY_RETURN_COUNT, 0),
            previousFocus = p.getString(KEY_PREVIOUS_FOCUS, "").orEmpty().clean(140),
            signatureHash = p.getString(KEY_SIGNATURE, "").orEmpty(),
        )
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).take(8).joinToString("") { "%02x".format(it) }

    private fun String.clean(max: Int): String = replace('|', '/').replace('\n', ' ').replace('\r', ' ')
        .replace(Regex("\\s+"), " ").trim().take(max)
}
