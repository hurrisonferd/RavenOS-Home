package com.iappyx.launcher.ravenos

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.iappyx.launcher.LauncherActivity
import java.lang.ref.WeakReference

/**
 * Explicit launcher escape hatch.
 *
 * Gestures remain fast shortcuts, but Raven never has to remember them: the small edge tab
 * opens every primary destination and long-press opens gesture configuration directly.
 */
object RavenMenu {
    private const val TAG = "ravenos_edge_menu"
    private var tabRef: WeakReference<TextView>? = null

    fun attach(activity: LauncherActivity) {
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        val existing = content.findViewWithTag<View>(TAG) as? TextView
        if (existing != null) {
            tabRef = WeakReference(existing)
            return
        }
        val density = activity.resources.displayMetrics.density
        val tab = TextView(activity).apply {
            tag = TAG
            text = "R"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            elevation = 18f * density
            background = GradientDrawable().apply {
                cornerRadius = 18f * density
                setColor(0xC51A111B.toInt())
                setStroke((1.2f * density).toInt().coerceAtLeast(1), 0xFFFF4FA3.toInt())
            }
            contentDescription = "Open RavenOS menu"
            setOnClickListener { open(activity) }
            setOnLongClickListener {
                RavenGesturePrefs.showDialog(activity)
                true
            }
        }
        content.addView(
            tab,
            FrameLayout.LayoutParams((42 * density).toInt(), (48 * density).toInt(), Gravity.START or Gravity.CENTER_VERTICAL).apply {
                marginStart = (-6 * density).toInt()
            },
        )
        tabRef = WeakReference(tab)
    }

    fun open(activity: LauncherActivity) {
        val items = arrayOf(
            "⌂  Home",
            "▦  Apps",
            "⌕  Search",
            "⚙  System / Studio",
            "↕  Gesture controls",
            "✦  Cycle haunt intensity",
            "◎  Office auto",
            "☾  Sleep office",
        )
        AlertDialog.Builder(activity)
            .setTitle("RavenOS")
            .setMessage(RavenGesturePrefs.summary(activity))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> activity.ravenOpenHome()
                    1 -> activity.ravenOpenApps()
                    2 -> activity.ravenOpenSearch()
                    3 -> activity.ravenOpenSystemDeck()
                    4 -> RavenGesturePrefs.showDialog(activity)
                    5 -> {
                        val next = RavenHauntModeStore.cycle(activity)
                        RavenOfficeBarService.setHaunt(activity, next)
                    }
                    6 -> RavenOfficeBarService.setAuto(activity)
                    7 -> RavenOfficeBarService.disable(activity)
                }
            }
            .setNegativeButton("Close", null)
            .show()
        RavenOfficeBarService.signal(activity, "NAVIGATION", "raven-menu")
    }
}
