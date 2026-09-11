package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * Tiny wallpaper-space controls inspired by Total Launcher invisible regions.
 * They are explicit/revocable and default to faint visible sigils rather than unknowable gestures.
 */
object RavenGhostHotspots {
    private const val PREFS = "ravenos_ghost_hotspots_v1"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INITIALIZED = "initialized"
    private const val TAG_PREFIX = "ravenos_ghost_hotspot_"
    private var contentRef: WeakReference<ViewGroup>? = null

    fun attach(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        contentRef = WeakReference(content)
        val prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            prefs.edit().putBoolean(KEY_INITIALIZED, true).putBoolean(KEY_ENABLED, true).apply()
        }
        if (!prefs.getBoolean(KEY_ENABLED, true)) {
            remove(content)
            return
        }
        if (content.findViewWithTag<View>("${TAG_PREFIX}tl") != null) return

        add(activity, content, "tl", "⌕", Gravity.TOP or Gravity.START) { RavenCommandPalette.show(activity) }
        add(activity, content, "tr", "R", Gravity.TOP or Gravity.END) { RavenSummoningWheel.show(activity) }
        add(activity, content, "bl", "📜", Gravity.BOTTOM or Gravity.START) { RavenOfficeFeed.show(activity) }
        add(activity, content, "br", "◈", Gravity.BOTTOM or Gravity.END) { RavenOfficeBarService.cycleHaunt(activity) }
    }

    fun toggle(activity: Activity): Boolean {
        val prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
        val enabled = !prefs.getBoolean(KEY_ENABLED, true)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        if (content != null) {
            if (enabled) attach(activity) else remove(content)
        }
        RavenTaskerBridge.emit(activity, "ghost_hotspots", if (enabled) "on" else "off")
        return enabled
    }

    fun enabled(activity: Activity): Boolean = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, true)

    private fun add(activity: Activity, content: ViewGroup, id: String, glyph: String, gravity: Int, action: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val view = TextView(activity).apply {
            tag = "$TAG_PREFIX$id"
            text = glyph
            textSize = 12f
            setTextColor(0xAAFFFFFF.toInt())
            this.gravity = Gravity.CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = "RavenOS ghost hotspot $id"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x3621112D)
                setStroke(dp(1).coerceAtLeast(1), 0x44FFFFFF)
            }
            setOnClickListener { action() }
            setOnLongClickListener { RavenSummoningWheel.show(activity); true }
        }
        content.addView(view, FrameLayout.LayoutParams(dp(38), dp(38)).apply {
            this.gravity = gravity
            val edge = dp(8)
            setMargins(edge, dp(36), edge, dp(86))
        })
    }

    private fun remove(content: ViewGroup) {
        for (i in content.childCount - 1 downTo 0) {
            val child = content.getChildAt(i)
            if ((child.tag as? String)?.startsWith(TAG_PREFIX) == true) content.removeViewAt(i)
        }
    }
}
