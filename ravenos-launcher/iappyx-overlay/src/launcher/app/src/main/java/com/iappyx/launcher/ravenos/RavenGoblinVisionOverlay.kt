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
 * Resident TYPE_APPLICATION_OVERLAY body: CHIP idle, OBSERVING screen-aware, COMMENT earned speech, FEED on tap.
 */
object RavenGoblinVisionOverlay {
    private const val PREFS = "ravenos_goblin_vision_v1"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"
    private const val KEY_MANUAL_UNTIL = "manual_until"
    private const val MANUAL_HOLD_MS = 120_000L

    private enum class Mode { CHIP, OBSERVING, COMMENT, FEED }

    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var lp: WindowManager.LayoutParams? = null
    private var statusView: TextView? = null
    private var ownerView: TextView? = null
    private var noteView: TextView? = null
    private var contextView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var collapseRunnable: Runnable? = null

    private var mode = Mode.CHIP
    private var appContext: Context? = null
    private var lastMember: RavenOfficeMember? = null
    private var lastOwnerLine: String = ""
    private var lastNote: String = ""
    private var lastObservation: String = ""
    private var lastSignal: String = ""
    private var lastDetail: String = ""
    private var lastHaunt: RavenHauntMode = RavenHauntMode.HAUNTED

    fun render(context: Context, member: RavenOfficeMember, signal: String, note: String, detail: String, hauntMode: RavenHauntMode) {
        val packet = RavenEmployeePresentation.packet(member, signal, detail, note)
        remember(context, member, packet.ownerLine, cleanVisible(packet.note), cleanVisible(packet.note), signal, detail, hauntMode)
        mode = when {
            lastNote.isNotBlank() -> Mode.COMMENT
            lastObservation.isNotBlank() -> Mode.OBSERVING
            else -> Mode.CHIP
        }
        renderCurrent(context)
    }

    fun renderReaction(context: Context, reaction: RavenReactionPacket, hauntMode: RavenHauntMode) {
        val member = RavenOfficeRegistry.member(reaction.owner) ?: return
        val spoken = cleanVisible(reaction.dialogue)
        val observation = cleanVisible(reaction.authorNote)
        remember(context, member, reaction.ownerLine, spoken, observation, reaction.signal, reaction.detail, hauntMode)
        mode = when {
            spoken.isNotBlank() -> Mode.COMMENT
            observation.isNotBlank() -> Mode.OBSERVING
            else -> Mode.CHIP
        }
        renderCurrent(context)
        if (spoken.isNotBlank()) scheduleCollapse(context, reaction, hauntMode)
    }

    private fun remember(context: Context, member: RavenOfficeMember, ownerLine: String, note: String, observation: String, signal: String, detail: String, hauntMode: RavenHauntMode) {
        appContext = context.applicationContext
        lastMember = member
        lastOwnerLine = ownerLine
        lastNote = note
        lastObservation = observation
        lastSignal = signal
        lastDetail = detail
        lastHaunt = hauntMode
    }

    private fun renderCurrent(context: Context) {
        val member = lastMember ?: return
        val stateAt = RavenOfficeStateStore.read(context)?.updatedAt ?: 0L
        val enabled = RavenFollowMeOverlay.isEnabled(context)
        val permitted = Settings.canDrawOverlays(context)
        if (!lastHaunt.followMe || !enabled || !permitted) {
            RavenSurfaceIntegrity.mark(
                context, RavenSurfaceIntegrity.FOLLOW_ME,
                if (RavenFollowMeOverlay.isPending(context)) "PENDING" else "INACTIVE",
                stateAt,
                when {
                    !lastHaunt.followMe -> "goblin_vision_suppressed:${lastHaunt.label}"
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
            cornerRadius = dp(context, if (lastHaunt.ordinal >= RavenHauntMode.FERAL.ordinal) 24 else 18).toFloat()
            setColor(Color.argb(if (lastHaunt.ordinal >= RavenHauntMode.FERAL.ordinal) 247 else 238, 14, 14, 20))
            setStroke(dp(context, if (lastHaunt.ordinal >= RavenHauntMode.FERAL.ordinal) 2 else 1), withAlpha(accent, 235))
        }

        val eventGlyph = RavenEmployeePresentation.signalGlyph(lastSignal, lastDetail)
        when (mode) {
            Mode.CHIP -> {
                statusView?.visibility = View.GONE
                ownerView?.apply {
                    visibility = View.VISIBLE
                    text = lastOwnerLine
                    textSize = 14f
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(accent)
                }
                noteView?.visibility = View.GONE
                contextView?.visibility = View.GONE
            }
            Mode.OBSERVING -> {
                statusView?.apply {
                    visibility = View.VISIBLE
                    text = "👁 WATCHING THE GLASS"
                    setTextColor(0xFFBFC0CC.toInt())
                }
                ownerView?.apply {
                    visibility = View.VISIBLE
                    text = lastOwnerLine
                    textSize = 14.5f
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(accent)
                }
                noteView?.apply {
                    visibility = if (lastObservation.isBlank()) View.GONE else View.VISIBLE
                    text = lastObservation
                    textSize = 12.8f
                    maxLines = 3
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(0xFFE7E7EF.toInt())
                }
                contextView?.visibility = View.GONE
            }
            Mode.COMMENT -> {
                statusView?.apply {
                    visibility = View.VISIBLE
                    text = listOf("👁 META GOBLIN", eventGlyph).filter { it.isNotBlank() }.joinToString("  ")
                    setTextColor(0xFFBFC0CC.toInt())
                }
                ownerView?.apply {
                    visibility = View.VISIBLE
                    text = lastOwnerLine
                    textSize = if (lastHaunt == RavenHauntMode.APOCALYPSE) 17f else 15f
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(accent)
                }
                noteView?.apply {
                    visibility = if (lastNote.isBlank()) View.GONE else View.VISIBLE
                    text = lastNote
                    textSize = 13.5f
                    maxLines = if (lastHaunt == RavenHauntMode.APOCALYPSE) 5 else 4
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(0xFFF8F8FC.toInt())
                }
                contextView?.visibility = View.GONE
            }
            Mode.FEED -> {
                statusView?.apply {
                    visibility = View.VISIBLE
                    text = "👁 OFFICE · RECENT HAUNTINGS"
                    setTextColor(0xFFBFC0CC.toInt())
                }
                ownerView?.apply {
                    visibility = View.VISIBLE
                    text = lastOwnerLine
                    textSize = 14.5f
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(accent)
                }
                noteView?.visibility = View.GONE
                contextView?.apply {
                    val feed = miniFeed(context)
                    visibility = if (feed.isBlank()) View.GONE else View.VISIBLE
                    text = feed
                    textSize = 11.5f
                    maxLines = 12
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(0xFFF0F0F6.toInt())
                }
            }
        }

        lp?.let { params ->
            val widthDp = when (mode) {
                Mode.CHIP -> if (lastHaunt.ordinal >= RavenHauntMode.FERAL.ordinal) 220 else 196
                Mode.OBSERVING -> when (lastHaunt) {
                    RavenHauntMode.CALM -> 244
                    RavenHauntMode.LIVED_IN -> 268
                    RavenHauntMode.HAUNTED -> 292
                    RavenHauntMode.FERAL -> 316
                    RavenHauntMode.APOCALYPSE -> 332
                }
                Mode.COMMENT -> when (lastHaunt) {
                    RavenHauntMode.CALM -> 250
                    RavenHauntMode.LIVED_IN -> 276
                    RavenHauntMode.HAUNTED -> 308
                    RavenHauntMode.FERAL -> 332
                    RavenHauntMode.APOCALYPSE -> 356
                }
                Mode.FEED -> if (lastHaunt == RavenHauntMode.APOCALYPSE) 368 else 346
            }
            var changed = false
            val width = dp(context, widthDp)
            if (params.width != width) { params.width = width; changed = true }

            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val manualUntil = prefs.getLong(KEY_MANUAL_UNTIL, 0L)
            if (System.currentTimeMillis() >= manualUntil && mode != Mode.FEED) {
                val placement = RavenOverlayChoreographyOS.preferred(context)
                if (abs(params.y - placement.y) > dp(context, 8)) {
                    params.y = placement.y
                    prefs.edit().putInt(KEY_Y, params.y).apply()
                    changed = true
                }
            }
            if (changed) try { wm?.updateViewLayout(box, params) } catch (_: Throwable) {}
        }

        RavenSurfaceIntegrity.mark(
            context, RavenSurfaceIntegrity.FOLLOW_ME, "RENDERED", stateAt,
            "goblin_vision_meta_overlay_v7:${mode.name.lowercase()}",
        )
    }

    private fun miniFeed(context: Context): String = RavenOfficeTraceStore.recent(context, 8)
        .mapNotNull { entry ->
            val line = cleanVisible(entry.note)
            if (line.isBlank()) return@mapNotNull null
            val member = RavenOfficeRegistry.member(entry.owner)
            val presentation = member?.let { RavenEmployeePresentation.packet(it, entry.signal, entry.detail, line) }
            val who = presentation?.let { "${it.emojiSoup} ${entry.owner} ${it.kaomoji}" } ?: entry.owner
            val repeat = if (entry.repeats > 1) " ×${entry.repeats}" else ""
            "$who$repeat\n${line.take(100)}"
        }.take(4).joinToString("\n\n")

    private fun scheduleCollapse(context: Context, reaction: RavenReactionPacket, hauntMode: RavenHauntMode) {
        collapseRunnable?.let(handler::removeCallbacks)
        if (!reaction.interruptible || hauntMode == RavenHauntMode.APOCALYPSE) return
        val runnable = Runnable {
            if (mode == Mode.COMMENT) {
                mode = if (lastObservation.isNotBlank()) Mode.OBSERVING else Mode.CHIP
                renderCurrent(context.applicationContext)
            }
        }
        collapseRunnable = runnable
        handler.postDelayed(runnable, reaction.lifetimeMs.coerceIn(4_000L, 18_000L))
    }

    fun hide() {
        collapseRunnable?.let(handler::removeCallbacks)
        collapseRunnable = null
        val view = root ?: return
        try { wm?.removeView(view) } catch (_: Throwable) {}
        root = null; statusView = null; ownerView = null; noteView = null; contextView = null; lp = null; wm = null
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
        statusView = TextView(context).apply { textSize = 9.5f; setTypeface(typeface, Typeface.BOLD) }.also(box::addView)
        ownerView = TextView(context).apply {
            textSize = 15f; setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(context, 3), 0, 0)
        }.also(box::addView)
        noteView = TextView(context).apply {
            textSize = 13.5f; setLineSpacing(dp(context, 1).toFloat(), 1.03f); setPadding(0, dp(context, 5), 0, 0)
        }.also(box::addView)
        contextView = TextView(context).apply {
            textSize = 11.5f; setLineSpacing(dp(context, 1).toFloat(), 1.02f); setPadding(0, dp(context, 6), 0, 0)
        }.also(box::addView)

        val params = WindowManager.LayoutParams(
            dp(context, 302), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = prefs.getInt(KEY_X, dp(context, 10))
            y = prefs.getInt(KEY_Y, dp(context, 104))
        }
        wireDrag(context, box, params)
        try { manager.addView(box, params); wm = manager; root = box; lp = params } catch (_: Throwable) { wm = null }
    }

    private fun wireDrag(context: Context, view: View, params: WindowManager.LayoutParams) {
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false; var downAt = 0L
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y
                    downAt = System.currentTimeMillis(); moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (abs(dx) > dp(context, 4) || abs(dy) > dp(context, 4)) moved = true
                    params.x = (startX - dx.toInt()).coerceAtLeast(0)
                    params.y = (startY + dy.toInt()).coerceAtLeast(0)
                    try { wm?.updateViewLayout(view, params) } catch (_: Throwable) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    val edit = prefs.edit().putInt(KEY_X, params.x).putInt(KEY_Y, params.y)
                    if (moved) edit.putLong(KEY_MANUAL_UNTIL, System.currentTimeMillis() + MANUAL_HOLD_MS)
                    edit.apply()
                    if (!moved) {
                        val held = System.currentTimeMillis() - downAt
                        if (held >= 650L) {
                            try {
                                context.startActivity(Intent(context, RavenHomeActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                            } catch (_: Throwable) {}
                        } else {
                            collapseRunnable?.let(handler::removeCallbacks)
                            mode = when (mode) {
                                Mode.CHIP -> if (lastObservation.isBlank()) Mode.FEED else Mode.OBSERVING
                                Mode.OBSERVING -> if (lastNote.isBlank()) Mode.FEED else Mode.COMMENT
                                Mode.COMMENT -> Mode.FEED
                                Mode.FEED -> Mode.CHIP
                            }
                            appContext?.let(::renderCurrent)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun cleanVisible(raw: String): String {
        val clean = raw.replace(Regex("\\s+"), " ").trim()
        if (clean.isBlank() || clean.equals("Noted.", true)) return ""
        if (Regex("^[A-Z0-9_-]+\\s+is\\s+watching[.!]?$", RegexOption.IGNORE_CASE).matches(clean)) return ""
        return clean
    }

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun readableAccent(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        if (perceived >= 145) return color
        return Color.rgb((Color.red(color) + 255) / 2, (Color.green(color) + 255) / 2, (Color.blue(color) + 255) / 2)
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
