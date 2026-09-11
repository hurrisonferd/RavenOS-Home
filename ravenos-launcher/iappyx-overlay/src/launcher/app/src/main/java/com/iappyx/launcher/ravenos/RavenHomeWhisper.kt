package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference

/** Native, non-interactive resident card mounted directly on RavenOS Home. */
object RavenHomeWhisper {
    private var viewRef: WeakReference<WhisperView>? = null

    fun attach(activity: Activity) {
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        val existing = content.findViewWithTag<View>(TAG) as? WhisperView
        if (existing != null) {
            viewRef = WeakReference(existing)
            return
        }
        val card = WhisperView(activity).apply {
            tag = TAG
            isClickable = false
            isLongClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val density = activity.resources.displayMetrics.density
        content.addView(
            card,
            FrameLayout.LayoutParams((220 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
                topMargin = (86 * density).toInt()
                marginEnd = (12 * density).toInt()
            },
        )
        viewRef = WeakReference(card)
    }

    fun render(member: RavenOfficeMember, note: String, signal: String, detail: String, mode: RavenHauntMode) {
        val view = viewRef?.get() ?: return
        view.post {
            view.render(member, note, signal, detail, mode)
            val stateAt = RavenOfficeStateStore.read(view.context)?.updatedAt ?: 0L
            RavenSurfaceIntegrity.mark(
                view.context,
                RavenSurfaceIntegrity.HOME_WHISPER,
                if (mode == RavenHauntMode.CALM) "INACTIVE" else "RENDERED",
                stateAt,
                mode.label,
            )
        }
    }

    fun hide() {
        val view = viewRef?.get() ?: return
        view.post {
            view.visibility = View.GONE
            val stateAt = RavenOfficeStateStore.read(view.context)?.updatedAt ?: 0L
            RavenSurfaceIntegrity.mark(view.context, RavenSurfaceIntegrity.HOME_WHISPER, "INACTIVE", stateAt, "hidden")
        }
    }

    private const val TAG = "ravenos_home_whisper"

    private class WhisperView(activity: Activity) : LinearLayout(activity) {
        private val owner = TextView(activity)
        private val note = TextView(activity)
        private val context = TextView(activity)
        private val density = resources.displayMetrics.density

        init {
            orientation = VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            elevation = dp(7).toFloat()
            owner.textSize = 15f
            owner.setTypeface(owner.typeface, Typeface.BOLD)
            addView(owner, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            note.textSize = 12f
            note.setPadding(0, dp(4), 0, 0)
            addView(note, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            context.textSize = 9f
            context.setPadding(0, dp(5), 0, 0)
            addView(context, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        fun render(member: RavenOfficeMember, authorNote: String, signal: String, detail: String, mode: RavenHauntMode) {
            if (mode == RavenHauntMode.CALM) {
                visibility = View.GONE
                return
            }
            applyModeGeometry(mode)
            visibility = View.VISIBLE
            val textColor = contrastText(member.accent)
            val secondary = if (textColor == Color.BLACK) 0xA8000000.toInt() else 0xC8FFFFFF.toInt()
            background = GradientDrawable().apply {
                cornerRadius = dp(if (mode == RavenHauntMode.APOCALYPSE) 24 else 16).toFloat()
                setColor(withAlpha(member.accent, when (mode) {
                    RavenHauntMode.LIVED_IN -> 150
                    RavenHauntMode.HAUNTED -> 184
                    RavenHauntMode.FERAL -> 212
                    RavenHauntMode.APOCALYPSE -> 232
                    RavenHauntMode.CALM -> 0
                }))
                setStroke(dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 2 else 1), withAlpha(textColor, 58))
            }
            owner.text = "${member.emoji} ${member.id}"
            owner.setTextColor(textColor)
            note.text = authorNote
            note.setTextColor(textColor)
            note.maxLines = when (mode) {
                RavenHauntMode.LIVED_IN -> 1
                RavenHauntMode.HAUNTED -> 2
                RavenHauntMode.FERAL -> 3
                RavenHauntMode.APOCALYPSE -> 4
                RavenHauntMode.CALM -> 1
            }
            context.visibility = if (mode.ordinal >= RavenHauntMode.HAUNTED.ordinal) View.VISIBLE else View.GONE
            context.setTextColor(secondary)
            context.text = buildString {
                append(signal.replace('_', ' ').lowercase()).append(" · ").append(member.lane)
                if (detail.isNotBlank() && mode.ordinal >= RavenHauntMode.FERAL.ordinal) {
                    append("\n").append(detail.take(if (mode == RavenHauntMode.APOCALYPSE) 130 else 85))
                }
            }
        }

        private fun applyModeGeometry(mode: RavenHauntMode) {
            val lp = layoutParams as? FrameLayout.LayoutParams ?: return
            lp.width = dp(when (mode) {
                RavenHauntMode.CALM -> 180
                RavenHauntMode.LIVED_IN -> 188
                RavenHauntMode.HAUNTED -> 220
                RavenHauntMode.FERAL -> 248
                RavenHauntMode.APOCALYPSE -> 278
            })
            lp.topMargin = dp(when (mode) {
                RavenHauntMode.APOCALYPSE -> 76
                RavenHauntMode.FERAL -> 80
                else -> 86
            })
            lp.marginEnd = dp(if (mode == RavenHauntMode.APOCALYPSE) 8 else 12)
            layoutParams = lp
            setPadding(
                dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 14 else 12),
                dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 12 else 10),
                dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 14 else 12),
                dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 12 else 10),
            )
        }

        private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
        private fun contrastText(color: Int): Int {
            val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
            return if (perceived >= 175) Color.BLACK else Color.WHITE
        }
        private fun dp(value: Int): Int = (value * density).toInt()
    }
}
