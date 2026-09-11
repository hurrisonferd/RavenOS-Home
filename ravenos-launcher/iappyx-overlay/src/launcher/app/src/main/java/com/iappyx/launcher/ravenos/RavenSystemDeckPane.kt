package com.iappyx.launcher.ravenos

import android.graphics.Color
import android.media.AudioManager
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.iappyx.launcher.LauncherActivity

/** Native utility-first first surface inside the RavenOS Studio/command page. */
class RavenSystemDeckPane(
    private val activity: LauncherActivity,
) : ScrollView(activity) {
    private val root = LinearLayout(activity)
    private val dp = resources.displayMetrics.density

    init {
        isFillViewport = true
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(px(22), px(24), px(22), px(40))
        addView(root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        root.addView(text("RAVENOS SYSTEM DECK", 27f, Color.WHITE, true))
        root.addView(text("native controls first · intelligence optional", 12f, Color.LTGRAY, false), top(3))
        root.addView(text("Swipe right from Home lands here. AI / Widgets / Wallpapers / Transitions / Icons stay in the tabs above.", 12f, 0xFFB8B8C8.toInt(), false), top(12))

        root.addView(section("AUDIO"), top(26))
        rebuildAudio()

        root.addView(section("OFFICE BAR"), top(28))
        root.addView(text("The always-on Office Bar changes owner, emoji, accent and deterministic author's note as RavenOS context changes.", 13f, 0xFFD5D5DF.toInt(), false), top(8))
        root.addView(button("ENABLE OFFICE BAR NOTIFICATIONS") {
            RavenPermissionDeck.ensureOfficeBarNotifications(activity)
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "notification-permission-requested")
        }, top(10))
        root.addView(button("PING SYSTEM DECK") {
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "manual-ping")
        }, top(6))

        root.addView(section("MAX AWARENESS · EXPLICIT AND REVOCABLE"), top(28))
        root.addView(text("RavenOS can become aggressively context-aware without hiding what it can see. Each Android special-access lane is granted separately and can be revoked in Settings.", 13f, 0xFFD5D5DF.toInt(), false), top(8))
        root.addView(button("FOREGROUND APP AWARENESS") {
            RavenPermissionDeck.openForegroundAwareness(activity)
        }, top(10))
        root.addView(text("Package/window transitions only. The RavenOS service is configured with canRetrieveWindowContent=false; it does not read page text or typed input.", 11f, 0xFFAAAAba.toInt(), false), top(3))
        root.addView(button("NOTIFICATION SOURCE AWARENESS") {
            RavenPermissionDeck.openNotificationAwareness(activity)
        }, top(8))
        root.addView(text("Lets RavenOS route from notification source metadata. Office Bar does not consume title/body text in this path.", 11f, 0xFFAAAAba.toInt(), false), top(3))
        root.addView(button("OVERLAY / FOLLOW-ME ACCESS") {
            RavenPermissionDeck.openOverlayAccess(activity)
        }, top(8))
        root.addView(button("APP PERMISSION DETAILS") {
            RavenPermissionDeck.openAppDetails(activity)
        }, top(8))
    }

    fun refresh() {
        // Audio controls are self-updating after writes; a future pass can replace this
        // with a small state observer when external volume changes need live slider motion.
        RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "refresh")
    }

    private fun rebuildAudio() {
        val state = RavenSystemDeck.snapshot(activity)
        addSlider("MEDIA", state.media.percent) { RavenSystemDeck.setMediaPercent(activity, it) }
        addSlider("RING", state.ring.percent) { RavenSystemDeck.setRingPercent(activity, it) }
        addSlider("ALARM", state.alarm.percent) { RavenSystemDeck.setAlarmPercent(activity, it) }
        addSlider("NOTIFICATIONS", state.notification.percent) { RavenSystemDeck.setNotificationPercent(activity, it) }

        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(modeButton("NORMAL", AudioManager.RINGER_MODE_NORMAL), weight())
        row.addView(modeButton("VIBRATE", AudioManager.RINGER_MODE_VIBRATE), weight())
        row.addView(modeButton("SILENT", AudioManager.RINGER_MODE_SILENT), weight())
        root.addView(row, top(14))
    }

    private fun addSlider(label: String, initial: Int, setter: (Int) -> Unit) {
        val labelView = text("$label  $initial%", 13f, Color.WHITE, true)
        root.addView(labelView, top(13))
        val seek = SeekBar(activity).apply {
            max = 100
            progress = initial
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    labelView.text = "$label  $progress%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    setter(progress)
                }
            })
        }
        root.addView(seek, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun modeButton(label: String, mode: Int): Button = button(label) {
        RavenSystemDeck.setRingerMode(activity, mode)
    }

    private fun section(label: String): TextView = text(label, 15f, 0xFFFF5AAE.toInt(), true)

    private fun button(label: String, click: (View) -> Unit): Button = Button(activity).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        setOnClickListener(click)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView = TextView(activity).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun top(value: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = px(value) }

    private fun weight(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
    private fun px(value: Int): Int = (value * dp).toInt()
}
