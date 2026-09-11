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
import kotlin.math.abs

/**
 * Goblin Vision = the cross-app body of the canonical Office state.
 *
 * It is intentionally screen-recorder-HUD-like without reading the pixels underneath it. Foreground
 * app / notification / power / media / Office signals are settled elsewhere, then this projection
 * meta-reacts to that same state using the same EmojiOS + KaomojiOS packet as the Office Bar.
 *
 * Overlay permission and Follow-Me enablement remain explicit/revocable. The view only intercepts
 * touches inside its own draggable bounds.
 */
object RavenGoblinVisionOverlay {
    private const val PREFS = "ravenos_goblin_vision_v1"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"

    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var lp: WindowManager.LayoutParams? = null
    private var statusView: TextView? = null
    private var ownerView: TextView? = null
    private var noteView: TextView? = null
    private var contextView: TextView? = null

    fun render(
        context: Context,
        member: RavenOfficeMember,
        signal: String,
        note: String,
        detail: String,
        hauntMode: RavenHauntMode,
    ) {
        val stateAt = RavenOfficeStateStore.read(context)?.updatedAt ?: 0L
        val enabled = RavenFollowMeOverlay.isEnabled(context)
        val permitted = Settings.canDrawOverlays(context)
        if (!hauntMode.followMe || !enabled || !permitted) {
            RavenSurfaceIntegrity.mark(
                context,
                RavenSurfaceIntegrity.FOLLOW_ME,
                "INACTIVE",
                stateAt,
                when {
                    !hauntMode.followMe -> "goblin_vision_suppressed:${hauntMode.label}"
                    !enabled -> "goblin_vision_disabled"
                    else -> "overlay_permission_missing"
                },
            )
            hide()
            return
        }

        ensureView(context.applicationContext)
        val box = root ?: run {
            RavenSurfaceIntegrity.mark(context, RavenSurfaceIntegrity.FOLLOW_ME, "BLOCKED", stateAt, "goblin_vision_unavailable")
            return
        }
        val packet = RavenEmployeePresentation.packet(member, signal, detail, note)
        val textColor = contrastText(packet.accent)
        val secondary = if (textColor == Color.BLACK) 0xAA000000.toInt() else 0xCCFFFFFF.toInt()
        val alpha = when (hauntMode) {
            RavenHauntMode.CALM -> 208
            RavenHauntMode.LIVED_IN -> 218
            RavenHauntMode.HAUNTED -> 230
            RavenHauntMode.FERAL -> 240
            RavenHauntMode.APOCALYPSE -> 248
        }
        box.background = GradientDrawable().apply {
            cornerRadius = dp(context, if (hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal) 24 else 18).toFloat()
            setColor(withAlpha(packet.accent, alpha))
            setStroke(dp(context, if (hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal) 2 else 1), withAlpha(textColor, 90))
        }
        statusView?.apply {
            text = "● GOBLIN VISION · ${prettySignal(signal).uppercase()}"
            setTextColor(secondary)
        }
        ownerView?.apply {
            text = packet.ownerLine
            textSize = if (hauntMode == RavenHauntMode.APOCALYPSE) 18f else 16f
            setTextColor(textColor)
        }
        noteView?.apply {
            text = packet.note
            maxLines = when (hauntMode) {
                RavenHauntMode.CALM, RavenHauntMode.LIVED_IN -> 1
                RavenHauntMode.HAUNTED -> 2
                RavenHauntMode.FERAL -> 3
                RavenHauntMode.APOCALYPSE -> 4
            }
            setTextColor(textColor)
        }
        contextView?.apply {
            text = "${packet.lane} · ${packet.context}"
            maxLines = if (hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal) 3 else 2
            setTextColor(secondary)
        }

        lp?.let { params ->
            val widthDp = when (hauntMode) {
                RavenHauntMode.CALM -> 238
                RavenHauntMode.LIVED_IN -> 250
                RavenHauntMode.HAUNTED -> 272
                RavenHauntMode.FERAL -> 300
                RavenHauntMode.APOCALYPSE -> 330
            }
            val width = dp(context, widthDp)
            if (params.width != width) {
                params.width = width
                try { wm?.updateViewLayout(box, params) } catch (_: Throwable) {}
            }
        }

        RavenSurfaceIntegrity.mark(context, RavenSurfaceIntegrity.FOLLOW_ME, "RENDERED", stateAt, "goblin_vision")
    }

    fun hide() {
        val view = root ?: return
        try { wm?.removeView(view) } catch (_: Throwable) {}
        root = null
        statusView = null
        ownerView = null
        noteView = null
        contextView = null
        lp = null
        wm = null
    }

    private fun ensureView(context: Context) {
        if (root != null) return
        val manager = context.getSystemService(WindowManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 13), dp(context, 9), dp(context, 13), dp(context, 10))
            isClickable = true
            elevation = dp(context, 10).toFloat()
        }
        statusView = TextView(context).apply {
            textSize = 9f
            setTypeface(typeface, Typeface.BOLD)
        }.also(box::addView)
        ownerView = TextView(context).apply {
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(context, 2), 0, 0)
        }.also(box::addView)
        noteView = TextView(context).apply {
            textSize = 12f
            setPadding(0, dp(context, 3), 0, 0)
        }.also(box::addView)
        contextView = TextView(context).apply {
            textSize = 9f
            setPadding(0, dp(context, 4), 0, 0)
        }.also(box::addView)

        val params = WindowManager.LayoutParams(
            dp(context, 272),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = prefs.getInt(KEY_X, dp(context, 10))
            y = prefs.getInt(KEY_Y, dp(context, 120))
        }
        wireDrag(context, box, params)
        try {
            manager.addView(box, params)
            wm = manager
            root = box
            lp = params
        } catch (_: Throwable) {
            wm = null
        }
    }

    private fun wireDrag(context: Context, view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > dp(context, 4) || abs(dy) > dp(context, 4)) moved = true
                    params.x = (startX - dx.toInt()).coerceAtLeast(0)
                    params.y = (startY + dy.toInt()).coerceAtLeast(0)
                    try { wm?.updateViewLayout(view, params) } catch (_: Throwable) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putInt(KEY_X, params.x).putInt(KEY_Y, params.y).apply()
                    if (!moved) {
                        try {
                            context.startActivity(
                                Intent(context, RavenHomeActivity::class.java)
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

    private fun prettySignal(signal: String): String = signal.trim().replace('_', ' ').lowercase()
    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    private fun contrastText(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        return if (perceived >= 175) Color.BLACK else Color.WHITE
    }
    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
