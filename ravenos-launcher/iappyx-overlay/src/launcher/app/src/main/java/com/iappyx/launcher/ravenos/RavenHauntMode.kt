package com.iappyx.launcher.ravenos

import android.content.Context

/**
 * User-selected RavenOS presence intensity.
 *
 * This changes presentation/reactivity only. It NEVER grants Android permissions.
 * A mode may ask an already-authorized surface to do more, but missing permission
 * remains missing and every special-access lane is still independently revocable.
 */
enum class RavenHauntMode(
    val label: String,
    val officeBar: Boolean,
    val followMe: Boolean,
    val foregroundRouting: Boolean,
    val notificationRouting: Boolean,
    val overlayDetailLines: Int,
) {
    CALM(
        label = "CALM",
        officeBar = true,
        followMe = false,
        foregroundRouting = false,
        notificationRouting = false,
        overlayDetailLines = 0,
    ),
    LIVED_IN(
        label = "LIVED-IN",
        officeBar = true,
        followMe = false,
        foregroundRouting = true,
        notificationRouting = false,
        overlayDetailLines = 1,
    ),
    HAUNTED(
        label = "HAUNTED",
        officeBar = true,
        followMe = true,
        foregroundRouting = true,
        notificationRouting = true,
        overlayDetailLines = 2,
    ),
    FERAL(
        label = "FERAL",
        officeBar = true,
        followMe = true,
        foregroundRouting = true,
        notificationRouting = true,
        overlayDetailLines = 3,
    ),
    APOCALYPSE(
        label = "APOCALYPSE",
        officeBar = true,
        followMe = true,
        foregroundRouting = true,
        notificationRouting = true,
        overlayDetailLines = 4,
    );

    fun next(): RavenHauntMode {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}

object RavenHauntModeStore {
    private const val PREFS = "ravenos_haunt_mode_v1"
    private const val KEY_MODE = "mode"

    fun get(context: Context): RavenHauntMode {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, RavenHauntMode.HAUNTED.name)
        return try {
            RavenHauntMode.valueOf(raw ?: RavenHauntMode.HAUNTED.name)
        } catch (_: Throwable) {
            RavenHauntMode.HAUNTED
        }
    }

    fun set(context: Context, mode: RavenHauntMode): RavenHauntMode {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
        return mode
    }

    fun cycle(context: Context): RavenHauntMode = set(context, get(context).next())

    fun parse(raw: String): RavenHauntMode? {
        val normalized = raw.trim()
            .uppercase()
            .replace('-', '_')
            .replace(' ', '_')
        return when (normalized) {
            "LIVEDIN", "LIVED_IN" -> RavenHauntMode.LIVED_IN
            else -> RavenHauntMode.entries.firstOrNull { it.name == normalized }
        }
    }
}
