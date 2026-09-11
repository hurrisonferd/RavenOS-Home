package com.iappyx.launcher.ravenos

import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.iappyx.launcher.LauncherActivity

/**
 * Utility-first overlay inspired by the strongest part of sound-control launchers:
 * one explicit tap from Home gives Raven useful phone controls immediately.
 * No model, network, or Studio navigation is required.
 */
object RavenQuickControls {
    fun show(activity: LauncherActivity) {
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(10))
        }
        fun text(value: String, size: Float = 12f, bold: Boolean = false): TextView = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        fun button(label: String, action: () -> Unit): Button = Button(activity).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
        }
        fun row(vararg buttons: Button): LinearLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            buttons.forEach { addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)) }
        }

        root.addView(text("RAVENOS QUICK DECK", 18f, true))
        root.addView(text("useful first · haunted second · AI optional", 11f, false))

        root.addView(row(
            button("HOME") { activity.ravenOpenHome() },
            button("APPS") { activity.ravenOpenApps() },
            button("SEARCH") { activity.ravenOpenSearch() },
            button("SYSTEM") { activity.ravenOpenSystemDeck() },
        ))

        val state = RavenSystemDeck.snapshot(activity)
        addSlider(root, activity, "MEDIA", state.media.percent) { RavenSystemDeck.setMediaPercent(activity, it) }
        addSlider(root, activity, "RING", state.ring.percent) { RavenSystemDeck.setRingPercent(activity, it) }
        addSlider(root, activity, "ALARM", state.alarm.percent) { RavenSystemDeck.setAlarmPercent(activity, it) }

        root.addView(row(
            button("NORMAL") { RavenSystemDeck.setRingerMode(activity, AudioManager.RINGER_MODE_NORMAL) },
            button("VIBRATE") { RavenSystemDeck.setRingerMode(activity, AudioManager.RINGER_MODE_VIBRATE) },
            button("SILENT") { RavenSystemDeck.setRingerMode(activity, AudioManager.RINGER_MODE_SILENT) },
        ))

        root.addView(text(RavenGesturePrefs.summary(activity), 10f, false))
        root.addView(button("GESTURE CONTROLS") { RavenGesturePrefs.showDialog(activity) })

        AlertDialog.Builder(activity)
            .setView(root)
            .setNegativeButton("Close", null)
            .show()
        RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "quick-deck")
    }

    private fun addSlider(
        root: LinearLayout,
        activity: LauncherActivity,
        label: String,
        initial: Int,
        setter: (Int) -> Unit,
    ) {
        val density = activity.resources.displayMetrics.density
        val title = TextView(activity).apply {
            text = "$label  $initial%"
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        root.addView(title)
        root.addView(SeekBar(activity).apply {
            max = 100
            progress = initial
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    title.text = "$label  $progress%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    setter(progress)
                }
            })
        })
    }
}
