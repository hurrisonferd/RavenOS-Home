package com.iappyx.launcher.ravenos

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.iappyx.launcher.LauncherActivity
import kotlin.math.abs

/**
 * Small visible Office presence that can follow Raven across apps.
 *
 * Requires the explicit SYSTEM_ALERT_WINDOW grant. It only occupies its own small bounds,
 * never reads the underlying app, and never intercepts touches outside those bounds.
 */
object RavenFollowMeOverlay {
    private const val PREFS = "ravenos_follow_me_v1"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"

    private var manager: WindowManager? = null
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var ownerView: TextView? = null
    private var noteView: TextView? = null
    private var contextView: TextView? = null

    fun isEnabled(context: Context): Boolean = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, false)

    fun enable(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, true).apply()
        return true
    }

    fun disable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, false).apply()
        hide()
    }

    fun render(
        context: Context,
        member: RavenOfficeMember,
        signal: String,
        note: String,
        detail: String,
    ) {
        if (!isEnabled(context) || !Settings.canDrawOverlays(context)) {
            hide()
            return
        }
        ensureView(context.applicationContext)
        val textColor = contrastText(member.accent)
        val secondary = if (textColor == Color.BLACK) 0xAA000000.toInt() else 0xCCFFFFFF.toInt()
        root?.background = GradientDrawable().apply {
            cornerRadius = dp(context, 18).toFloat()
            setColor(withAlpha(member.accent, 238))
            setStroke(dp(context, 1), withAlpha(textColor, 70))
        }
        ownerView?.apply {
            text = "${member.emoji} ${member.id}"
            setTextColor(textColor)
        }
        noteView?.apply {
            text = note
            setTextColor(textColor)
        }
        contextView?.apply {
            text = buildString {
                append(signal.replace('_', ' ').lowercase())
                append(" · ")
                append(member.lane)
                if (detail.isNotBlank()) {
                    append("\n")
                    append(detail.take(90))
                }
            }
            setTextColor(secondary)
        }
    }

    fun hide() {
        val r = root ?: return
        try { manager?.removeView(r) } catch (_: Throwable) {}
        root = null
        ownerView = null
        noteView = null
        contextView = null
        params = null
        manager = null
    }

    private fun ensureView(context: Context) {
        if (root != null) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wm = context.getSystemService(WindowManager::class.java) ?: return
        manager = wm

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10))
            isClickable = true
            elevation = dp(context, 8).toFloat()
        }
        ownerView = TextView(context).apply {
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        }.also(container::addView)
        noteView = TextView(context).apply {
            textSize = 12f
            maxLines = 3
            setPadding(0, dp(context, 3), 0, 0)
        }.also(container::addView)
        contextView = TextView(context).apply {
            textSize = 10f
            maxLines = 2
            setPadding(0, dp(context, 4), 0, 0)
        }.also(container::addView)

        val lp = WindowManager.LayoutParams(
            dp(context, 286),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = prefs.getInt(KEY_X, dp(context, 12))
            y = prefs.getInt(KEY_Y, dp(context, 150))
        }
        params = lp
        wireDragAndOpen(context, container, lp)
        try {
            wm.addView(container, lp)
            root = container
        } catch (_: Throwable) {
            manager = null
            params = null
        }
    }

    private fun wireDragAndOpen(
        context: Context,
        view: View,
        lp: WindowManager.LayoutParams,
    ) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = lp.x
                    startY = lp.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > dp(context, 4) || abs(dy) > dp(context, 4)) moved = true
                    // END gravity reverses horizontal intuition, so subtract dx.
                    lp.x = (startX - dx.toInt()).coerceAtLeast(0)
                    lp.y = (startY + dy.toInt()).coerceAtLeast(0)
                    try { manager?.updateViewLayout(view, lp) } catch (_: Throwable) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putInt(KEY_X, lp.x).putInt(KEY_Y, lp.y).apply()
                    if (!moved) {
                        try {
                            context.startActivity(
                                Intent(context, LauncherActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            )
                        } catch (_: Throwable) {}
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )

    private fun contrastText(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        return if (perceived >= 175) Color.BLACK else Color.WHITE
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
