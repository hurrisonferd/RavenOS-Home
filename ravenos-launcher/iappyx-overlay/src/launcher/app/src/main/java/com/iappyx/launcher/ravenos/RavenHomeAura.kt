package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.lang.ref.WeakReference

/**
 * Non-interactive RavenOS Home aura.
 *
 * Draws only a thin/glowing edge treatment over the launcher surface. It never handles
 * touch, focus, accessibility actions, or content inspection. Member + haunt state are
 * supplied by the same deterministic Office route used by the Office Bar.
 */
object RavenHomeAura {
    private var viewRef: WeakReference<AuraView>? = null

    fun attach(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val existing = content.findViewWithTag<View>(TAG) as? AuraView
        if (existing != null) {
            viewRef = WeakReference(existing)
            return
        }
        val aura = AuraView(activity).apply {
            tag = TAG
            isClickable = false
            isLongClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        content.addView(
            aura,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        viewRef = WeakReference(aura)
    }

    fun render(member: RavenOfficeMember, mode: RavenHauntMode) {
        val view = viewRef?.get() ?: return
        view.post { view.setState(member.accent, mode) }
    }

    private const val TAG = "ravenos_home_aura"

    private class AuraView(activity: Activity) : View(activity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private var accent: Int = 0xFFFF4FA3.toInt()
        private var mode: RavenHauntMode = RavenHauntMode.HAUNTED
        private val density = resources.displayMetrics.density

        fun setState(nextAccent: Int, nextMode: RavenHauntMode) {
            if (accent == nextAccent && mode == nextMode) return
            accent = nextAccent
            mode = nextMode
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val spec = spec(mode)
            if (spec.alpha <= 0 || width <= 0 || height <= 0) return

            // Multiple nested strokes create a cheap glow without allocating bitmaps or
            // invoking blur/shadow layers. The center remains completely transparent.
            val base = spec.widthDp * density
            repeat(spec.layers) { index ->
                val fraction = (index + 1).toFloat() / spec.layers.toFloat()
                val stroke = base * (1f + index * 0.85f)
                val alpha = (spec.alpha * (1f - fraction * 0.62f)).toInt().coerceIn(8, 255)
                paint.strokeWidth = stroke
                paint.color = Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent))
                val inset = stroke / 2f
                canvas.drawRect(inset, inset, width - inset, height - inset, paint)
            }
        }

        private fun spec(mode: RavenHauntMode): AuraSpec = when (mode) {
            RavenHauntMode.CALM -> AuraSpec(alpha = 18, widthDp = 1.2f, layers = 1)
            RavenHauntMode.LIVED_IN -> AuraSpec(alpha = 34, widthDp = 1.7f, layers = 2)
            RavenHauntMode.HAUNTED -> AuraSpec(alpha = 58, widthDp = 2.2f, layers = 3)
            RavenHauntMode.FERAL -> AuraSpec(alpha = 86, widthDp = 3.0f, layers = 4)
            RavenHauntMode.APOCALYPSE -> AuraSpec(alpha = 118, widthDp = 4.0f, layers = 5)
        }
    }

    private data class AuraSpec(
        val alpha: Int,
        val widthDp: Float,
        val layers: Int,
    )
}
