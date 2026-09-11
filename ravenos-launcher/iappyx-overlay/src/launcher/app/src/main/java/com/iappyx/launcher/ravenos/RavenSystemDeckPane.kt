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
    private lateinit var readinessView: TextView
    private lateinit var hauntView: TextView
    private lateinit var traceView: TextView
    private lateinit var integrityView: TextView

    init {
        isFillViewport = true
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(px(22), px(24), px(22), px(40))
        addView(root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        root.addView(text("RAVENOS SYSTEM DECK", 27f, Color.WHITE, true))
        root.addView(text("native controls first · intelligence optional", 12f, Color.LTGRAY, false), top(3))
        root.addView(text("Swipe right from Home lands here. AI / Widgets / Wallpapers / Transitions / Icons stay in the tabs above.", 12f, 0xFFB8B8C8.toInt(), false), top(12))

        root.addView(section("ACTIVATION / SURVIVAL"), top(26))
        readinessView = text(readinessText(), 12f, 0xFFD5D5DF.toInt(), false)
        root.addView(readinessView, top(8))
        root.addView(button("MAKE RAVENOS DEFAULT HOME") {
            RavenPermissionDeck.requestDefaultHome(activity)
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "default-home-requested")
        }, top(10))
        root.addView(button("WAKE OFFICE BAR") {
            RavenPermissionDeck.ensureOfficeBarNotifications(activity)
            RavenOfficeBarService.enable(activity)
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "office-bar-wake")
            updateReadiness()
            updateIntegrity()
        }, top(6))
        root.addView(button("SLEEP OFFICE BAR") {
            RavenOfficeBarService.disable(activity)
            updateReadiness()
            updateIntegrity()
        }, top(6))
        root.addView(button("BACKGROUND / BATTERY SURVIVAL") {
            RavenPermissionDeck.openBatteryOptimization(activity)
        }, top(6))
        root.addView(button("RUN LOCAL RAVENOS CANARY") {
            val snap = RavenAwarenessStatus.snapshot(activity)
            readinessView.text = readinessText(snap) + "\n\nLOCAL CANARY: ${snap.compact()}"
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "canary:${snap.passed}/${snap.total}")
            updateIntegrity()
        }, top(6))

        root.addView(section("HAUNT INTENSITY"), top(28))
        hauntView = text(hauntText(), 13f, 0xFFD5D5DF.toInt(), true)
        root.addView(hauntView, top(8))
        root.addView(text("Presentation intensity never grants permissions. CALM/LIVED-IN suppress Follow-Me; HAUNTED and above may project it only if Raven separately enabled overlay access and Follow-Me Office.", 11f, 0xFFAAAABA.toInt(), false), top(4))
        val hauntRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        hauntRow.addView(button("CALM") { setHaunt(RavenHauntMode.CALM) }, weight())
        hauntRow.addView(button("HAUNTED") { setHaunt(RavenHauntMode.HAUNTED) }, weight())
        hauntRow.addView(button("FERAL") { setHaunt(RavenHauntMode.FERAL) }, weight())
        root.addView(hauntRow, top(10))
        val hauntRow2 = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        hauntRow2.addView(button("LIVED-IN") { setHaunt(RavenHauntMode.LIVED_IN) }, weight())
        hauntRow2.addView(button("APOCALYPSE") { setHaunt(RavenHauntMode.APOCALYPSE) }, weight())
        hauntRow2.addView(button("CYCLE") {
            val next = RavenHauntModeStore.cycle(activity)
            RavenOfficeBarService.setHaunt(activity, next)
            updateHaunt()
            updateIntegrity()
        }, weight())
        root.addView(hauntRow2, top(6))

        root.addView(section("OFFICE FLIGHT RECORDER"), top(28))
        traceView = text(RavenOfficeTraceStore.compact(activity), 11f, 0xFFC8C8D4.toInt(), false)
        root.addView(traceView, top(8))
        root.addView(text("Bounded local routing receipts only. No cloud sync, no app body text, no keystrokes.", 10f, 0xFF9292A4.toInt(), false), top(4))
        root.addView(button("REFRESH OFFICE TRACE") { updateTrace() }, top(8))
        root.addView(button("CLEAR OFFICE TRACE") {
            RavenOfficeTraceStore.clear(activity)
            updateTrace()
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "office-trace-cleared")
        }, top(6))

        root.addView(section("SURFACE INTEGRITY"), top(28))
        integrityView = text(RavenSurfaceIntegrity.compact(activity), 10f, 0xFFC8C8D4.toInt(), false)
        root.addView(integrityView, top(8))
        root.addView(text("RENDERED = RavenOS updated an owned native View. POSTED = Android accepted the Office Bar foreground notification. DISPATCHED = state was sent to a WebView/cross-process channel but is not yet device-acknowledged. No optimistic sync claims.", 10f, 0xFF9292A4.toInt(), false), top(4))
        root.addView(button("REFRESH SURFACE INTEGRITY") { updateIntegrity() }, top(8))
        root.addView(button("CLEAR SURFACE INTEGRITY") {
            RavenSurfaceIntegrity.clear(activity)
            updateIntegrity()
        }, top(6))

        root.addView(section("AUDIO"), top(28))
        rebuildAudio()

        root.addView(section("OFFICE BAR"), top(28))
        root.addView(text("The always-on Office Bar changes owner, emoji, accent and deterministic author's note as RavenOS context changes.", 13f, 0xFFD5D5DF.toInt(), false), top(8))
        root.addView(button("ENABLE OFFICE BAR NOTIFICATIONS") {
            RavenPermissionDeck.ensureOfficeBarNotifications(activity)
            RavenOfficeBarService.enable(activity)
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "notification-permission-requested")
            updateIntegrity()
        }, top(10))
        root.addView(button("PING SYSTEM DECK") {
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "manual-ping")
            updateIntegrity()
        }, top(6))

        root.addView(section("MAX AWARENESS · EXPLICIT AND REVOCABLE"), top(28))
        root.addView(text("RavenOS can become aggressively context-aware without hiding what it can see. Each Android special-access lane is granted separately and can be revoked in Settings.", 13f, 0xFFD5D5DF.toInt(), false), top(8))
        root.addView(button("FOREGROUND APP AWARENESS") {
            RavenPermissionDeck.openForegroundAwareness(activity)
        }, top(10))
        root.addView(text("Package/window transitions only. The RavenOS service is configured with canRetrieveWindowContent=false; it does not read page text or typed input.", 11f, 0xFFAAAABA.toInt(), false), top(3))
        root.addView(button("NOTIFICATION SOURCE AWARENESS") {
            RavenPermissionDeck.openNotificationAwareness(activity)
        }, top(8))
        root.addView(text("Lets RavenOS route from notification source metadata. Office Bar does not consume title/body text in this path.", 11f, 0xFFAAAABA.toInt(), false), top(3))
        root.addView(button("OVERLAY / FOLLOW-ME ACCESS") {
            RavenPermissionDeck.openOverlayAccess(activity)
        }, top(8))
        root.addView(button("ENABLE FOLLOW-ME OFFICE") {
            if (RavenFollowMeOverlay.enable(activity)) {
                RavenOfficeBarService.enable(activity)
                RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "follow-me-enabled")
                updateReadiness()
                updateIntegrity()
            } else {
                RavenPermissionDeck.openOverlayAccess(activity)
            }
        }, top(6))
        root.addView(text("Projects the routed office member as a small draggable overlay across apps. It sees only the already-routed Office state; it does not read the app underneath it.", 11f, 0xFFAAAABA.toInt(), false), top(3))
        root.addView(button("HIDE FOLLOW-ME OFFICE") {
            RavenFollowMeOverlay.disable(activity)
            RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "follow-me-disabled")
            updateReadiness()
            updateIntegrity()
        }, top(6))
        root.addView(button("APP PERMISSION DETAILS") {
            RavenPermissionDeck.openAppDetails(activity)
        }, top(8))
    }

    fun refresh() {
        updateReadiness()
        updateHaunt()
        updateTrace()
        updateIntegrity()
        RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "refresh")
    }

    private fun setHaunt(mode: RavenHauntMode) {
        RavenOfficeBarService.setHaunt(activity, mode)
        updateHaunt()
        updateReadiness()
        updateIntegrity()
    }

    private fun updateHaunt() {
        hauntView.text = hauntText()
    }

    private fun updateTrace() {
        traceView.text = RavenOfficeTraceStore.compact(activity)
    }

    private fun updateIntegrity() {
        integrityView.text = RavenSurfaceIntegrity.compact(activity)
    }

    private fun hauntText(): String {
        val mode = RavenHauntModeStore.get(activity)
        return "CURRENT  ${mode.label} · FOREGROUND=${onOff(mode.foregroundRouting)} · NOTIFICATION=${onOff(mode.notificationRouting)} · FOLLOW=${onOff(mode.followMe)}"
    }

    private fun updateReadiness() {
        readinessView.text = readinessText()
    }

    private fun readinessText(snapshot: RavenAwarenessSnapshot = RavenAwarenessStatus.snapshot(activity)): String = buildString {
        append("MAX AWARENESS READINESS  ${snapshot.passed}/${snapshot.total}\n")
        append("APP  ").append(snapshot.applicationId).append('\n')
        append("DEFAULT HOME  ").append(onOff(snapshot.defaultHome)).append('\n')
        append("OFFICE BAR  ").append(onOff(snapshot.officeBarEnabled)).append('\n')
        append("NOTIFICATION PERMISSION  ").append(onOff(snapshot.notificationPermission)).append('\n')
        append("FOREGROUND AWARENESS  ").append(onOff(snapshot.foregroundAwareness)).append('\n')
        append("NOTIFICATION AWARENESS  ").append(onOff(snapshot.notificationAwareness)).append('\n')
        append("OVERLAY ACCESS  ").append(onOff(snapshot.overlayAccess)).append('\n')
        append("FOLLOW-ME OFFICE  ").append(onOff(snapshot.followMeEnabled)).append('\n')
        append("HAUNT MODE  ").append(RavenHauntModeStore.get(activity).label)
    }

    private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"

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
