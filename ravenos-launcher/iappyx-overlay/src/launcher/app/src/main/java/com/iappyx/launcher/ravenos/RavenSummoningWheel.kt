package com.iappyx.launcher.ravenos

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.iappyx.launcher.LauncherActivity

/** Pie-launcher-inspired explicit muscle-memory surface. Built lazily only while summoned. */
object RavenSummoningWheel {
    fun show(activity: Activity) {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val size = dp(330)
        val cell = dp(82)
        val dialog = Dialog(activity)
        val root = FrameLayout(activity).apply {
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply {
                setColor(0xEA17131C.toInt())
                cornerRadius = dp(36).toFloat()
                setStroke(dp(1), 0x66FFFFFF)
            }
        }

        fun place(label: String, x: Int, y: Int, action: () -> Unit) {
            val b = Button(activity).apply {
                text = label
                isAllCaps = false
                textSize = 10f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xCC302538.toInt())
                    setStroke(dp(1), 0x66FFFFFF)
                }
                setOnClickListener { dialog.dismiss(); action() }
            }
            root.addView(b, FrameLayout.LayoutParams(cell, cell).apply {
                leftMargin = dp(x)
                topMargin = dp(y)
            })
        }

        place("⌕\nSearch", 124, 4) { RavenCommandPalette.show(activity) }
        place("▦\nApps", 232, 44) { RavenCommandPalette.show(activity, "/") }
        place("♫\nSound", 244, 154) { showSound(activity) }
        place("📜\nFeed", 202, 244) { RavenOfficeFeed.show(activity) }
        place("✦\nStudio", 92, 244) { openStudio(activity) }
        place("◈\nSigils", 4, 154) { RavenGhostHotspots.toggle(activity) }
        place("↻\nOffice", 14, 44) { RavenOfficeBarService.next(activity) }

        val center = Button(activity).apply {
            text = "R"
            textSize = 26f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE0FF3E9D.toInt())
                setStroke(dp(2), 0xAAFFFFFF.toInt())
            }
            setOnClickListener { dialog.dismiss() }
            setOnLongClickListener {
                RavenOfficeBarService.cycleHaunt(activity)
                true
            }
        }
        root.addView(center, FrameLayout.LayoutParams(dp(88), dp(88)).apply {
            leftMargin = dp(121)
            topMargin = dp(121)
        })

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout(size, size)
            setGravity(Gravity.CENTER)
        }
        dialog.setOnShowListener { dialog.window?.setLayout(size, size) }
        dialog.show()
        RavenTaskerBridge.emit(activity, "summoning_wheel", "opened")
    }

    private fun openStudio(activity: Activity) {
        try {
            activity.startActivity(Intent(activity, LauncherActivity::class.java)
                .putExtra(RavenHomeActivity.EXTRA_OPEN_STUDIO, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        } catch (_: Throwable) {}
    }

    private fun showSound(activity: Activity) {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, p / 2)
        }
        val state = RavenSystemDeck.snapshot(activity)
        fun slider(label: String, value: Int, setter: (Int) -> Unit) {
            val title = TextView(activity).apply { text = "$label  $value%"; setTextColor(Color.WHITE) }
            root.addView(title)
            root.addView(SeekBar(activity).apply {
                max = 100
                progress = value
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { title.text = "$label  $progress%" }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) { setter(progress) }
                })
            })
        }
        slider("MEDIA", state.media.percent) { RavenSystemDeck.setMediaPercent(activity, it) }
        slider("RING", state.ring.percent) { RavenSystemDeck.setRingPercent(activity, it) }
        slider("ALARM", state.alarm.percent) { RavenSystemDeck.setAlarmPercent(activity, it) }
        val modes = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        fun mode(label: String, value: Int) = Button(activity).apply {
            text = label
            isAllCaps = false
            setOnClickListener { RavenSystemDeck.setRingerMode(activity, value) }
        }
        modes.addView(mode("Normal", AudioManager.RINGER_MODE_NORMAL), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        modes.addView(mode("Vibrate", AudioManager.RINGER_MODE_VIBRATE), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        modes.addView(mode("Silent", AudioManager.RINGER_MODE_SILENT), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(modes)
        AlertDialog.Builder(activity).setTitle("Raven Sound").setView(root).setPositiveButton("Done", null).show()
    }
}
