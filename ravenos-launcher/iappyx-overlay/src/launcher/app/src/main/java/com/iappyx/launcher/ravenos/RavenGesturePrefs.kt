package com.iappyx.launcher.ravenos

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.iappyx.launcher.LauncherActivity

enum class RavenGestureDirection { UP, DOWN }
enum class RavenGestureTarget(val label: String) {
    APPS("Apps"),
    SEARCH("Search"),
    MENU("Raven Menu"),
    NONE("None"),
}

/**
 * User-owned launcher gesture routing.
 *
 * Gestures are shortcuts, not mandatory navigation. Raven Menu remains available as the
 * explicit escape hatch even when both vertical gestures are disabled.
 */
object RavenGesturePrefs {
    private const val PREFS = "ravenos_gesture_prefs_v1"

    fun get(context: Context, direction: RavenGestureDirection): RavenGestureTarget {
        val fallback = default(direction)
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(direction.name, fallback.name)
        return runCatching { RavenGestureTarget.valueOf(raw ?: fallback.name) }.getOrDefault(fallback)
    }

    fun set(context: Context, direction: RavenGestureDirection, target: RavenGestureTarget) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(direction.name, target.name).apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun summary(context: Context): String =
        "SWIPE UP → ${get(context, RavenGestureDirection.UP).label.uppercase()} · " +
            "SWIPE DOWN → ${get(context, RavenGestureDirection.DOWN).label.uppercase()}"

    /** Returns true only when RavenOS actually handled the gesture. */
    fun fire(activity: LauncherActivity, direction: RavenGestureDirection): Boolean {
        val target = get(activity, direction)
        val handled = when (target) {
            RavenGestureTarget.APPS -> { activity.ravenOpenApps(); true }
            RavenGestureTarget.SEARCH -> { activity.ravenOpenSearch(); true }
            RavenGestureTarget.MENU -> { RavenMenu.open(activity); true }
            RavenGestureTarget.NONE -> false
        }
        if (handled) {
            RavenOfficeBarService.signal(
                activity,
                "NAVIGATION",
                "gesture:${direction.name.lowercase()}->${target.name.lowercase()}",
            )
        }
        return handled
    }

    fun showDialog(activity: LauncherActivity) {
        val items = arrayOf(
            "Swipe up → ${get(activity, RavenGestureDirection.UP).label}",
            "Swipe down → ${get(activity, RavenGestureDirection.DOWN).label}",
            "Disable vertical gestures",
            "Reset defaults (Up=Apps, Down=Search)",
        )
        AlertDialog.Builder(activity)
            .setTitle("RavenOS Gesture Controls")
            .setMessage("Gestures are optional shortcuts. The Raven edge tab always remains available.")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> choose(activity, RavenGestureDirection.UP)
                    1 -> choose(activity, RavenGestureDirection.DOWN)
                    2 -> {
                        set(activity, RavenGestureDirection.UP, RavenGestureTarget.NONE)
                        set(activity, RavenGestureDirection.DOWN, RavenGestureTarget.NONE)
                        RavenOfficeBarService.signal(activity, "NAVIGATION", "vertical-gestures-disabled")
                    }
                    3 -> {
                        reset(activity)
                        RavenOfficeBarService.signal(activity, "NAVIGATION", "gesture-defaults-restored")
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun choose(activity: LauncherActivity, direction: RavenGestureDirection) {
        val targets = RavenGestureTarget.entries.toTypedArray()
        val labels = targets.map { it.label }.toTypedArray()
        val current = targets.indexOf(get(activity, direction)).coerceAtLeast(0)
        AlertDialog.Builder(activity)
            .setTitle("Swipe ${direction.name.lowercase()}")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                set(activity, direction, targets[which])
                RavenOfficeBarService.signal(
                    activity,
                    "NAVIGATION",
                    "gesture:${direction.name.lowercase()}=${targets[which].name.lowercase()}",
                )
                dialog.dismiss()
                showDialog(activity)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun default(direction: RavenGestureDirection): RavenGestureTarget = when (direction) {
        RavenGestureDirection.UP -> RavenGestureTarget.APPS
        RavenGestureDirection.DOWN -> RavenGestureTarget.SEARCH
    }
}
