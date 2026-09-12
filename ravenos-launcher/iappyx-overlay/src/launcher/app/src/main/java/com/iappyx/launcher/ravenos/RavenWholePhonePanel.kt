package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/** Owner-facing controls for RavenOS whole-phone awareness. */
object RavenWholePhonePanel {
    fun show(activity: Activity) {
        val density = activity.resources.displayMetrics.density
        fun px(v: Int) = (v * density).toInt()
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(20), px(12), px(20), px(24))
        }
        val status = TextView(activity).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
        }
        root.addView(status)

        fun section(title: String, body: String) {
            root.addView(TextView(activity).apply {
                text = title
                textSize = 15f
                setTextColor(0xFFFF5AAE.toInt())
                setPadding(0, px(18), 0, px(4))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            root.addView(TextView(activity).apply {
                text = body
                textSize = 11f
                setTextColor(0xFFCCCCD8.toInt())
            })
        }

        fun button(label: String, action: () -> Unit) {
            root.addView(Button(activity).apply {
                text = label
                isAllCaps = false
                setOnClickListener { action(); refresh(activity, status) }
            })
        }

        section("SCREEN-FIRST OFFICE 👁🧠", "The resident separates sensing, screen understanding, observation, and earned character dialogue. Current visible meaning outranks package/window chatter; observations can stay visible while the office waits for a line worth saying.")
        button("REFRESH SCREEN INTERPRETATION") {
            RavenOfficeBarService.signal(activity, "SCREEN_SEMANTIC", "state:manual_refresh|source:control_panel")
        }
        button("CLEAR SCREEN/CALLBACK MEMORY") {
            RavenScreenMemoryOS.clear()
            RavenCallbackMemoryOS.clear()
            RavenInterruptibilityOS.clear(activity)
            RavenOfficeGovernor.clearStats(activity)
        }

        section("NOTIFICATION SENSE", "SOURCE = package/category/ranking only. SEMANTIC may route title locally. FULL_LOCAL may ingest title/body locally. Nothing is uploaded by this layer.")
        button("SOURCE ONLY") { RavenNotificationSenseOS.setMode(activity, RavenNotificationSenseOS.PrivacyMode.SOURCE) }
        button("SEMANTIC LOCAL") { RavenNotificationSenseOS.setMode(activity, RavenNotificationSenseOS.PrivacyMode.SEMANTIC) }
        button("FULL LOCAL") { RavenNotificationSenseOS.setMode(activity, RavenNotificationSenseOS.PrivacyMode.FULL_LOCAL) }
        button("OPEN NOTIFICATION ACCESS") { RavenPermissionDeck.openNotificationAwareness(activity) }

        section("MEDIA SESSION SENSE", "Uses Android active MediaSession state when notification-listener access permits it. No microphone capture.")
        button("REFRESH MEDIA STATE") {
            RavenOfficeBarService.signal(activity, "MEDIA_SESSION", RavenMediaSessionSenseOS.signalDetail(RavenMediaSessionSenseOS.snapshot(activity)))
        }

        section("GOBLIN EYE 👁", "Owner-armed MediaProjection. Local frames feed motion/color/hash and optionally text-layout awareness. Raw frames are not persisted by ScreenWatchOS. Stable screens receive periodic OCR refreshes so context does not expire just because Raven stopped scrolling.")
        button("👁 ARM GOBLIN EYE") { RavenPermissionDeck.armGoblinEye(activity) }
        button("STOP GOBLIN EYE") { RavenPermissionDeck.stopGoblinEye(activity) }

        section("GOBLIN READ 👁🔤", "Optional local OCR layered on Goblin Eye. It retains bounded text/layout evidence, filters Follow-Me Office's own visible dialogue, and suppresses credential-looking surfaces. Raw frames are discarded.")
        button("ENABLE LOCAL GOBLIN READ") { RavenGoblinReadOS.setEnabled(activity, true) }
        button("DISABLE GOBLIN READ") { RavenGoblinReadOS.setEnabled(activity, false) }

        section("ACCESSIBILITY READ 🧭🔤", "Separate owner switch for Android-exposed visible labels and static text. Editable values and password nodes are excluded. Accessibility and OCR are fused as two witnesses to one semantic screen rather than competing sources.")
        button("ENABLE ACCESSIBILITY READ") { RavenAccessibilityReadOS.setEnabled(activity, true) }
        button("DISABLE ACCESSIBILITY READ") { RavenAccessibilityReadOS.setEnabled(activity, false) }
        button("OPEN ACCESSIBILITY AWARENESS") { RavenPermissionDeck.openForegroundAwareness(activity) }

        section("CROSS-APP META GOBLIN", "Follow-Me is the persistent TYPE_APPLICATION_OVERLAY body over ordinary apps: CHIP idle, OBSERVING when the screen is understood, COMMENT when an employee earns dialogue, tap for recent hauntings. Secure/lock/protected surfaces remain Android-controlled.")
        button("OPEN APPEAR-ON-TOP ACCESS") { RavenPermissionDeck.openOverlayAccess(activity) }
        button("ENABLE FOLLOW-ME") {
            if (!RavenFollowMeOverlay.enable(activity)) RavenPermissionDeck.openOverlayAccess(activity)
            else RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "follow-me:enabled")
        }
        button("HIDE FOLLOW-ME") { RavenFollowMeOverlay.disable(activity) }

        section("FOREGROUND LEDGER", "Optional Android Usage Access adds a second app-resume signal when One UI Accessibility events are incomplete. App identity/timing only; no content.")
        button("OPEN USAGE ACCESS") { RavenPermissionDeck.openUsageAwareness(activity) }

        section("CALLBACK + SUBJECT MEMORY", "Callback memory tracks loops/returns; screen memory tracks only a few short derived semantic subjects in process memory. Neither organ stores raw screenshots or a persistent OCR transcript.")
        button("CLEAR CALLBACK BITS") { RavenCallbackMemoryOS.clear() }

        section("GALAXY / BACKGROUND SURVIVAL", RavenGalaxyHauntOS.samsungInstructions(activity))
        button("BATTERY OPTIMIZATION") { RavenPermissionDeck.openBatteryOptimization(activity) }
        button("BATTERY SETTINGS") { RavenPermissionDeck.openBatterySettings(activity) }
        button("APP DETAILS") { RavenPermissionDeck.openAppDetails(activity) }

        val scroll = ScrollView(activity).apply { addView(root) }
        refresh(activity, status)
        AlertDialog.Builder(activity)
            .setTitle("Goblin Vision · Whole-phone senses")
            .setView(scroll)
            .setNegativeButton("Close", null)
            .show()
        RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "whole-phone-senses")
    }

    private fun refresh(activity: Activity, view: TextView) {
        val awareness = RavenAwarenessStatus.snapshot(activity)
        val media = RavenMediaSessionSenseOS.snapshot(activity)
        val galaxy = RavenGalaxyHauntOS.snapshot(activity)
        val screen = RavenScreenContextOS.snapshot(activity)
        view.text = buildString {
            append(awareness.compact())
            append("\n").append(RavenUsageSenseOS.compact(activity))
            append("\n").append(RavenNotificationSenseOS.summary(activity))
            append("\n").append(media.compact())
            append("\n").append(RavenGoblinReadOS.compact(activity))
            append("\n").append(RavenAccessibilityReadOS.compact(activity))
            append("\n").append(RavenFollowMeOverlay.status(activity))
            append("\n").append(galaxy.compact())
            append("\n").append(RavenOfficeGovernor.compact(activity))
            append("\nSCREEN=").append(if (screen.available) "READABLE" else "UNKNOWN")
            append(" source=").append(screen.source)
            append(" confidence=").append(screen.confidence)
            append(" kind=").append(screen.semanticKind)
            append(" meta=").append(screen.meta)
            if (screen.focus.isNotBlank()) append("\nSUBJECT=").append(screen.focus.take(170))
        }
    }
}
