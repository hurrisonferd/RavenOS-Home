package com.iappyx.launcher.ravenos

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView

/**
 * Tiny, click-through visual embodiment for canonical Digi-Fae reaction art.
 *
 * This is presentation only: no touch capture, no activation, no screen reading, no effect authority.
 */
object RavenWatchletOS {
    private var manager: WindowManager? = null
    private var image: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    fun render(context: Context, packet: RavenReactionPacket, hauntMode: RavenHauntMode) {
        val app = context.applicationContext
        if (!RavenVisualAssetStore.hasVisual(packet.owner)) {
            hide()
            return
        }
        if (!hauntMode.followMe || !RavenFollowMeOverlay.isEnabled(app) || !Settings.canDrawOverlays(app)) {
            hide()
            return
        }
        if (!packet.interruptible && hauntMode.ordinal < RavenHauntMode.FERAL.ordinal) {
            hide()
            return
        }
        val bitmap = RavenVisualAssetStore.reaction(app, packet.owner, packet.visualState) ?: run {
            hide()
            return
        }
        ensureView(app)
        val view = image ?: return
        view.setImageBitmap(bitmap)
        view.visibility = View.VISIBLE
        position(app, packet.zone, hauntMode)

        hideRunnable?.let(handler::removeCallbacks)
        if (hauntMode != RavenHauntMode.APOCALYPSE) {
            val lifetime = when (hauntMode) {
                RavenHauntMode.CALM -> packet.lifetimeMs.coerceIn(900L, 2200L)
                RavenHauntMode.LIVED_IN -> packet.lifetimeMs.coerceIn(1200L, 3000L)
                RavenHauntMode.HAUNTED -> packet.lifetimeMs.coerceIn(1600L, 4200L)
                RavenHauntMode.FERAL -> packet.lifetimeMs.coerceIn(2500L, 6500L)
                RavenHauntMode.APOCALYPSE -> packet.lifetimeMs
            }
            val task = Runnable { view.visibility = View.GONE }
            hideRunnable = task
            handler.postDelayed(task, lifetime)
        }
    }

    fun hide() {
        hideRunnable?.let(handler::removeCallbacks)
        hideRunnable = null
        val view = image
        if (view != null) {
            try { manager?.removeView(view) } catch (_: Throwable) {}
        }
        image = null
        params = null
        manager = null
    }

    private fun ensureView(context: Context) {
        if (image != null) return
        val wm = context.getSystemService(WindowManager::class.java) ?: return
        val view = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            contentDescription = null
        }
        val lp = WindowManager.LayoutParams(
            dp(context, 154),
            dp(context, 171),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dp(context, 84)
        }
        try {
            wm.addView(view, lp)
            manager = wm
            image = view
            params = lp
        } catch (_: Throwable) {
            manager = null
            image = null
            params = null
        }
    }

    private fun position(context: Context, zone: String, hauntMode: RavenHauntMode) {
        val view = image ?: return
        val lp = params ?: return
        val metrics = context.resources.displayMetrics
        val width = if (hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal) dp(context, 184) else dp(context, 154)
        val height = (width * 1.11f).toInt()
        lp.width = width
        lp.height = height

        val maxX = (metrics.widthPixels - width).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - height).coerceAtLeast(0)
        val top = dp(context, 72).coerceAtMost(maxY)
        val bottom = (maxY - dp(context, 74)).coerceAtLeast(top)
        val centerX = maxX / 2
        val right = (maxX - dp(context, 8)).coerceAtLeast(0)
        val left = dp(context, 8).coerceAtMost(maxX)

        when (zone.uppercase()) {
            "EVIDENCE_DESK" -> { lp.x = right; lp.y = top }
            "QUIET_CORNER" -> { lp.x = right; lp.y = bottom }
            "TRANSIT_LANE" -> { lp.x = centerX; lp.y = top }
            "REPLAY_BAY" -> { lp.x = left; lp.y = bottom }
            "CLIPBOARD_COURT" -> { lp.x = centerX; lp.y = top }
            "JIM_CUBICLE" -> { lp.x = left; lp.y = (maxY * 0.34f).toInt() }
            "HOME_PERCH" -> { lp.x = left; lp.y = (maxY * 0.52f).toInt() }
            "NIGHT_DESK" -> { lp.x = right; lp.y = (maxY * 0.58f).toInt() }
            "PRODUCER_BOOTH" -> { lp.x = centerX; lp.y = bottom }
            else -> { lp.x = right; lp.y = top }
        }
        try { manager?.updateViewLayout(view, lp) } catch (_: Throwable) {}
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
