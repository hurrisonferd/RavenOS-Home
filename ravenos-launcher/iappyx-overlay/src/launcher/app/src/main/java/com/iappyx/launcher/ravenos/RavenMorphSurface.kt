package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference

/** Small state-morphing surface driven by the highest active haunt requirement. */
object RavenMorphSurface {
    private const val TAG = "ravenos_morph_surface"
    private var ref: WeakReference<LinearLayout>? = null

    fun attach(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val existing = content.findViewWithTag<View>(TAG) as? LinearLayout
        if (existing != null) {
            ref = WeakReference(existing)
            refresh(activity)
            return
        }

        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val box = LinearLayout(activity).apply {
            tag = TAG
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(9))
            elevation = dp(4).toFloat()
            visibility = View.GONE
            setOnClickListener { RavenOfficeFeed.show(activity) }
        }
        box.addView(TextView(activity).apply {
            tag = "title"
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        box.addView(TextView(activity).apply {
            tag = "body"
            textSize = 10f
            setTextColor(Color.WHITE)
            maxLines = 2
        })
        content.addView(box, FrameLayout.LayoutParams(dp(230), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            marginStart = dp(12)
        })
        ref = WeakReference(box)
        refresh(activity)
    }

    fun refresh(activity: Activity) {
        val box = ref?.get() ?: return
        val mode = RavenHauntModeStore.get(activity)
        val match = RavenHauntRequirements.top(activity)
        if (match == null || (mode == RavenHauntMode.CALM && match.priority < 90)) {
            box.visibility = View.GONE
            return
        }
        val member = RavenOfficeRegistry.member(match.owner)
        val accent = member?.accent ?: 0xFFFF4FA3.toInt()
        val alpha = when (mode) {
            RavenHauntMode.CALM -> 150
            RavenHauntMode.LIVED_IN -> 170
            RavenHauntMode.HAUNTED -> 190
            RavenHauntMode.FERAL -> 215
            RavenHauntMode.APOCALYPSE -> 235
        }
        box.background = GradientDrawable().apply {
            cornerRadius = 18f * activity.resources.displayMetrics.density
            setColor(Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent)))
            setStroke((activity.resources.displayMetrics.density).toInt().coerceAtLeast(1), 0x66FFFFFF)
        }
        (box.findViewWithTag<View>("title") as? TextView)?.text = match.title
        (box.findViewWithTag<View>("body") as? TextView)?.text = match.body
        box.visibility = View.VISIBLE
    }

    fun hide() {
        ref?.get()?.visibility = View.GONE
    }
}
