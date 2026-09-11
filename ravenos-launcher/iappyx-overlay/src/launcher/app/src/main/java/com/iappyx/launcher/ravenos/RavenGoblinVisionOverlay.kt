package com.iappyx.launcher.ravenos

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * GOBLIN VISION cross-app presentation contract.
 * Persistent speech bubble for ordinary Android app surfaces; secure/system surfaces
 * remain governed by Android rather than being falsely claimed by RavenOS.
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
    private val handler = Handler(Looper.getMainLooper())
    private var collapseRunnable: Runnable? = null

    fun render(
        context: Context,
        member: RavenOfficeMember,
        signal: String,
        note: String,
        detail: String,
        hauntMode: RavenHauntMode,
    ) {
        val packet = RavenEmployeePresentation.packet(member, signal, detail, note)
        renderBase(context, member, packet.ownerLine, packet.note, packet.context, hauntMode)
    }

    fun renderReaction(context: Context, reaction: RavenReactionPacket, hauntMode: RavenHauntMode) {
        val member = RavenOfficeRegistry.member(reaction.owner) ?: return
        val spoken = reaction.dialogue.ifBlank { "${reaction.owner} is watching." }
        val glyph = RavenEmployeePresentation.signalGlyph(reaction.signal, reaction.detail)
        renderBase(
            context = context,
            member = member,
            ownerLine = reaction.ownerLine,
            note = spoken,
            contextLine = glyph,
            hauntMode = hauntMode,
        )
        scheduleCollapse(context, reaction, hauntMode)
    }

    private fun renderBase(
        context: Context,
        member: RavenOfficeMember,
        ownerLine: String,
        note: String,
        contextLine: String,
        hauntMode: RavenHauntMode,
    ) {
        val stateAt = RavenOfficeStateStore.read(context)?.updatedAt ?: 0L
        val enabled = RavenFollowMeOverlay.isEnabled(context)
        val permitted = Settings.canDrawOverlays(context)
        if (!hauntMode.followMe || !enabled || !permitted) {
            RavenSurfaceIntegrity.mark(
                context,
                RavenSurfaceIntegrity.FOLLOW_ME,
                if (RavenFollowMeOverlay.isPending(context)) "PENDING" else "INACTIVE",
                stateAt,
                when {
                    !hauntMode.followMe -> "goblin_vision_suppressed:${hauntMode.label}"
                    RavenFollowMeOverlay.isPending(context) -> "goblin_vision_waiting_for_overlay_permission"
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

        val accent = readableAccent(member.accent)
        box.background = GradientDrawable().apply {
            cornerRadius = dp(context, if (hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal) 24 else 18).toFloat()
            setColor(Color.argb(if (hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal) 247 else 238, 14, 14, 20))
            setStroke(dp(context, if (hauntMode.ordinal >= RavenHauntMode.FERAL.ordinal) 2 else 1), withAlpha(accent, 235))
        }
        statusView?.apply {
            visibility = View.VISIBLE
            text = "👁 GOBLIN"
            setTextColor(0xFFBFC0CC.toInt())
        }
        ownerView?.apply {
            visibility = View.VISIBLE
            text = ownerLine
            textSize = if (hauntMode == RavenHauntMode.APOCALYPSE) 17f else 15f
            setTextColor(accent)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        noteView?.apply {
            visibility = if (note.isBlank()) View.GONE else View.VISIBLE
            text = note
            textSize = 13.5f
            maxLines = when (hauntMode) {
                RavenHauntMode.CALM -> 2
                RavenHauntMode.LIVED_IN -> 3
                RavenHauntMode.HAUNTED -> 4
                RavenHauntMode.FERAL -> 4
                RavenHauntMode.APOCALYPSE -> 5
            }
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(0xFFF8F8FC.toInt())
        }
        contextView?.apply {
            visibility = if (contextLine.isBlank()) View.GONE else View.VISIBLE
            text = contextLine
            textSize = 10f
            maxLines = 1
            setTextColor(0xFFD4D4DE.toInt())
        }

        lp?.let { params ->
            val widthDp = when (hauntMode) {
                RavenHauntMode.CALM -> 238
                RavenHauntMode.LIVED_IN -> 268
                RavenHauntMode.HAUNTED -> 302
                RavenHauntMode.FERAL -> 326
                RavenHauntMode.APOCALYPSE -> 350
            }
            val width = dp(context, widthDp)
            if (params.width != width) {
                params.width = width
                try { wm?.updateViewLayout(box, params) } catch (_: Throwable) {}
            }
        }

        RavenSurfaceIntegrity.mark(context, RavenSurfaceIntegrity.FOLLOW_ME, "RENDERED", stateAt, "goblin_vision_persistent_meta_v4")
    }

    private fun scheduleCollapse(context: Context, reaction: RavenReactionPacket, hauntMode: RavenHauntMode) {
        collapseRunnable?.let(handler::removeCallbacks)
        if (!reaction.interruptible || hauntMode == RavenHauntMode.APOCALYPSE) return
        val runnable = Runnable {
            val box = root ?: return@Runnable
            statusView?.visibility = View.GONE
            noteView?.visibility = View.GONE
            contextView?.visibility = View.GONE
            lp?.let { params ->
                params.width = dp(context, if (hauntMode == RavenHauntMode.FERAL) 190 else 164)
                try { wm?.updateViewLayout(box, params) } catch (_: Throwable) {}
            }
        }
        collapseRunnable = runnable
        handler.postDelayed(runnable, reaction.lifetimeMs.coerceIn(2400L, 15_000L))
    }

    fun hide() {
        collapseRunnable?.let(handler::removeCallbacks)
        collapseRunnable = null
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
            setPadding(dp(context, 15), dp(context, 10), dp(context, 15), dp(context, 12))
            isClickable = true
            elevation = dp(context, 12).toFloat()
        }
        statusView = TextView(context).apply {
            textSize = 9.5f
            setTypeface(typeface, Typeface.BOLD)
        }.also(box::addView)
        ownerView = TextView(context).apply {
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(context, 3), 0, 0)
        }.also(box::addView)
        noteView = TextView(context).apply {
            textSize = 13.5f
            setLineSpacing(dp(context, 1).toFloat(), 1.03f)
            setPadding(0, dp(context, 5), 0, 0)
        }.also(box::addView)
        contextView = TextView(context).apply {
            textSize = 10f
            setPadding(0, dp(context, 5), 0, 0)
        }.also(box::addView)

        val params = WindowManager.LayoutParams(
            dp(context, 302),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = prefs.getInt(KEY_X, dp(context, 10))
            y = prefs.getInt(KEY_Y, dp(context, 104))
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
        var downAt = 0L
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    downAt = System.currentTimeMillis()
                    moved = false
                    true
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
                    if (!moved && System.currentTimeMillis() - downAt >= 650L) {
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

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun readableAccent(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        if (perceived >= 145) return color
        return Color.rgb(
            (Color.red(color) + 255) / 2,
            (Color.green(color) + 255) / 2,
            (Color.blue(color) + 255) / 2,
        )
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
