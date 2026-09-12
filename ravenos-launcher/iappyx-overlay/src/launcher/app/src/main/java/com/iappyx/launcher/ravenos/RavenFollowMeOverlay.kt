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
 * Requires the explicit SYSTEM_ALERT_WINDOW grant. It only occupies its own bounds,
 * never reads the underlying app, and never intercepts touches outside those bounds.
 */
object RavenFollowMeOverlay {
    private const val PREFS = "ravenos_follow_me_v1"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PENDING = "pending_permission_enable"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"

    private var manager: WindowManager? = null
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var ownerView: TextView? = null
    private var noteView: TextView? = null
    private var contextView: TextView? = null

    /**
     * Reconciles the Android overlay grant with Raven's prior ENABLE FOLLOW-ME request.
     * This fixes the old two-tap trap: Raven can request Follow-Me, grant Android access,
     * and the next real phone event arms the overlay without requiring a second button press.
     */
    fun isEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ENABLED, false)) return true
        val pending = prefs.getBoolean(KEY_PENDING, false)
        if (pending && Settings.canDrawOverlays(context)) {
            prefs.edit().putBoolean(KEY_ENABLED, true).putBoolean(KEY_PENDING, false).apply()
            RavenSurfaceIntegrity.mark(
                context,
                RavenSurfaceIntegrity.FOLLOW_ME,
                "ARMED",
                RavenOfficeStateStore.read(context)?.updatedAt ?: 0L,
                "overlay_permission_grant_reconciled",
            )
            return true
        }
        return false
    }

    fun isPending(context: Context): Boolean = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_PENDING, false)

    fun status(context: Context): String = when {
        isEnabled(context) && Settings.canDrawOverlays(context) -> "FOLLOW-ME=ON · OVERLAY=GRANTED"
        isPending(context) && !Settings.canDrawOverlays(context) -> "FOLLOW-ME=PENDING · OVERLAY=NEEDS GRANT"
        Settings.canDrawOverlays(context) -> "FOLLOW-ME=OFF · OVERLAY=GRANTED"
        else -> "FOLLOW-ME=OFF · OVERLAY=NOT GRANTED"
    }

    fun enable(context: Context): Boolean {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!Settings.canDrawOverlays(context)) {
            prefs.edit().putBoolean(KEY_ENABLED, false).putBoolean(KEY_PENDING, true).apply()
            RavenSurfaceIntegrity.mark(
                context,
                RavenSurfaceIntegrity.FOLLOW_ME,
                "PENDING",
                RavenOfficeStateStore.read(context)?.updatedAt ?: 0L,
                "raven_requested_follow_me_waiting_for_android_overlay_grant",
            )
            return false
        }
        prefs.edit().putBoolean(KEY_ENABLED, true).putBoolean(KEY_PENDING, false).apply()
        RavenSurfaceIntegrity.mark(
            context,
            RavenSurfaceIntegrity.FOLLOW_ME,
            "ARMED",
            RavenOfficeStateStore.read(context)?.updatedAt ?: 0L,
            "enabled_by_raven",
        )
        return true
    }

    fun disable(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, false).putBoolean(KEY_PENDING, false).apply()
        RavenSurfaceIntegrity.mark(
            context,
            RavenSurfaceIntegrity.FOLLOW_ME,
            "INACTIVE",
            RavenOfficeStateStore.read(context)?.updatedAt ?: 0L,
            "disabled_by_raven",
        )
        hide()
    }

    fun render(
        context: Context,
        member: RavenOfficeMember,
        signal: String,
        note: String,
        detail: String,
        hauntMode: RavenHauntMode = RavenHauntModeStore.get(context),
    ) {
        val stateAt = RavenOfficeStateStore.read(context)?.updatedAt ?: 0L
        val enabled = isEnabled(context)
        val permitted = Settings.canDrawOverlays(context)
        if (!hauntMode.followMe || !enabled || !permitted) {
            val reason = when {
                !hauntMode.followMe -> "suppressed:${hauntMode.label}"
                !enabled && isPending(context) -> "pending_overlay_permission"
                !enabled -> "disabled"
                else -> "overlay_permission_missing"
            }
            RavenSurfaceIntegrity.mark(
                context,
                RavenSurfaceIntegrity.FOLLOW_ME,
                if (isPending(context)) "PENDING" else "INACTIVE",
                stateAt,
                reason,
            )
            hide()
            return
        }

        ensureView(context.applicationContext)
        if (root == null) {
            RavenSurfaceIntegrity.mark(
                context,
                RavenSurfaceIntegrity.FOLLOW_ME,
                "BLOCKED",
                stateAt,
                "overlay_view_unavailable",
            )
            return
        }

        val accent = readableAccent(member.accent)
        root?.background = GradientDrawable().apply {
            cornerRadius = dp(context, if (hauntMode == RavenHauntMode.APOCALYPSE) 24 else 18).toFloat()
            setColor(Color.argb(if (hauntMode == RavenHauntMode.APOCALYPSE) 248 else 240, 16, 16, 23))
            setStroke(dp(context, if (hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal) 2 else 1), withAlpha(accent, 225))
        }
        ownerView?.apply {
            text = "${member.emoji} ${member.id} · ${hauntMode.label}"
            setTextColor(accent)
            textSize = if (hauntMode == RavenHauntMode.APOCALYPSE) 17f else 15f
        }
        noteView?.apply {
            text = note
            setTextColor(0xFFF8F8FC.toInt())
            maxLines = when (hauntMode) {
                RavenHauntMode.HAUNTED -> 4
                RavenHauntMode.FERAL -> 6
                RavenHauntMode.APOCALYPSE -> 8
                else -> 3
            }
        }
        contextView?.apply {
            text = buildString {
                append(signal.replace('_', ' ').lowercase())
                append(" · ")
                append(member.lane)
                if (detail.isNotBlank() && hauntMode.overlayDetailLines >= 2) {
                    append("\n")
                    append(detail.take(if (hauntMode == RavenHauntMode.APOCALYPSE) 190 else 130))
                }
            }
            maxLines = if (hauntMode == RavenHauntMode.APOCALYPSE) 5 else 3
            setTextColor(0xFFD4D4DE.toInt())
        }

        params?.let { lp ->
            val desiredWidth = when (hauntMode) {
                RavenHauntMode.HAUNTED -> 300
                RavenHauntMode.FERAL -> 330
                RavenHauntMode.APOCALYPSE -> 356
                else -> 286
            }
            val pxWidth = dp(context, desiredWidth)
            if (lp.width != pxWidth) {
                lp.width = pxWidth
                try { root?.let { manager?.updateViewLayout(it, lp) } } catch (_: Throwable) {}
            }
        }

        RavenSurfaceIntegrity.mark(
            context,
            RavenSurfaceIntegrity.FOLLOW_ME,
            "RENDERED",
            stateAt,
            "overlay_view_cross_app",
        )
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
            setPadding(dp(context, 15), dp(context, 12), dp(context, 15), dp(context, 13))
            isClickable = true
            elevation = dp(context, 10).toFloat()
        }
        ownerView = TextView(context).apply {
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        }.also(container::addView)
        noteView = TextView(context).apply {
            textSize = 13f
            maxLines = 6
            setPadding(0, dp(context, 4), 0, 0)
        }.also(container::addView)
        contextView = TextView(context).apply {
            textSize = 10f
            maxLines = 3
            setPadding(0, dp(context, 6), 0, 0)
        }.also(container::addView)

        val lp = WindowManager.LayoutParams(
            dp(context, 300),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = prefs.getInt(KEY_X, dp(context, 10))
            y = prefs.getInt(KEY_Y, dp(context, 112))
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

    private fun readableAccent(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        if (perceived >= 145) return color
        return Color.rgb(
            (Color.red(color) + 255) / 2,
            (Color.green(color) + 255) / 2,
            (Color.blue(color) + 255) / 2,
        )
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
