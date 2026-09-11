package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
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
            FrameLayout.LayoutParams((268 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
                topMargin = (82 * density).toInt()
                marginEnd = (10 * density).toInt()
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
                "${mode.label}:readable_glass_v2",
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
            setPadding(dp(15), dp(12), dp(15), dp(13))
            elevation = dp(10).toFloat()

            owner.textSize = 16f
            owner.setTypeface(owner.typeface, Typeface.BOLD)
            owner.maxLines = 1
            owner.ellipsize = TextUtils.TruncateAt.END
            addView(owner, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

            note.textSize = 13.5f
            note.setLineSpacing(dp(1).toFloat(), 1.03f)
            note.setPadding(0, dp(5), 0, 0)
            addView(note, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

            context.textSize = 10f
            context.setLineSpacing(dp(1).toFloat(), 1.0f)
            context.setPadding(0, dp(7), 0, 0)
            context.ellipsize = TextUtils.TruncateAt.END
            addView(context, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        fun render(member: RavenOfficeMember, authorNote: String, signal: String, detail: String, mode: RavenHauntMode) {
            if (mode == RavenHauntMode.CALM) {
                visibility = View.GONE
                return
            }
            applyModeGeometry(mode)
            visibility = View.VISIBLE

            // Raven feedback: employee colors are identity accents, not the reading surface.
            // Keep the card dark/opaque enough for deterministic readability over any wallpaper.
            val accent = readableAccent(member.accent)
            background = GradientDrawable().apply {
                cornerRadius = dp(if (mode == RavenHauntMode.APOCALYPSE) 24 else 18).toFloat()
                setColor(Color.argb(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 244 else 232, 16, 16, 23))
                setStroke(dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 2 else 1), withAlpha(accent, 225))
            }

            owner.text = "${member.emoji} ${member.id}"
            owner.setTextColor(accent)

            note.text = authorNote
            note.setTextColor(0xFFF8F8FC.toInt())
            note.maxLines = when (mode) {
                RavenHauntMode.LIVED_IN -> 2
                RavenHauntMode.HAUNTED -> 5
                RavenHauntMode.FERAL -> 8
                RavenHauntMode.APOCALYPSE -> 10
                RavenHauntMode.CALM -> 1
            }
            note.ellipsize = if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) null else TextUtils.TruncateAt.END

            context.visibility = if (mode.ordinal >= RavenHauntMode.HAUNTED.ordinal) View.VISIBLE else View.GONE
            context.setTextColor(0xFFD4D4DE.toInt())
            context.maxLines = if (mode == RavenHauntMode.APOCALYPSE) 4 else 3
            context.text = buildString {
                append(signal.replace('_', ' ').lowercase()).append(" · ").append(member.lane)
                if (detail.isNotBlank() && mode.ordinal >= RavenHauntMode.FERAL.ordinal) {
                    append("\n").append(detail.take(if (mode == RavenHauntMode.APOCALYPSE) 190 else 140))
                }
            }
        }

        private fun applyModeGeometry(mode: RavenHauntMode) {
            val lp = layoutParams as? FrameLayout.LayoutParams ?: return
            lp.width = dp(when (mode) {
                RavenHauntMode.CALM -> 220
                RavenHauntMode.LIVED_IN -> 248
                RavenHauntMode.HAUNTED -> 286
                RavenHauntMode.FERAL -> 312
                RavenHauntMode.APOCALYPSE -> 336
            })
            lp.topMargin = dp(when (mode) {
                RavenHauntMode.APOCALYPSE -> 72
                RavenHauntMode.FERAL -> 76
                else -> 82
            })
            lp.marginEnd = dp(if (mode == RavenHauntMode.APOCALYPSE) 6 else 10)
            layoutParams = lp
            setPadding(
                dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 16 else 14),
                dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 13 else 11),
                dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 16 else 14),
                dp(if (mode.ordinal >= RavenHauntMode.FERAL.ordinal) 14 else 12),
            )
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

        private fun dp(value: Int): Int = (value * density).toInt()
    }
}
