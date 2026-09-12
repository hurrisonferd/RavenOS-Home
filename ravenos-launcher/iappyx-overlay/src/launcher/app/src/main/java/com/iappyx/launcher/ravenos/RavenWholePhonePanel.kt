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

        section("SCREEN-FIRST OFFICE 👁🧠", "The resident separates sensing, screen understanding, interaction, observation, and earned character dialogue. Current visible meaning outranks package/window chatter; observations can stay visible while the office waits for a line worth saying.")
        button("REFRESH SCREEN INTERPRETATION") {
            RavenOfficeBarService.signal(activity, "SCREEN_SEMANTIC", "state:manual_refresh|source:control_panel")
        }
        button("CLEAR SCREEN/CALLBACK MEMORY") {
            RavenScreenMemoryOS.clear()
            RavenCallbackMemoryOS.clear()
            RavenEpisodeScriptOS.clear()
            RavenInteractionMemoryOS.clear()
            RavenBitLedgerOS.clear()
            RavenBackstageOS.clear()
            RavenGoldEpisodeStatsOS.clear()
            RavenInterruptibilityOS.clear(activity)
            RavenOfficeGovernor.clearStats(activity)
        }

        section("LONG-RUNNING MACHINE KINGDOM 📺📚🧬", "Bounded multi-session structural continuity tracks episode number, employee appearances, spoken/callback/meta counts, pair chemistry history and canonical Goblin Vision motifs. It persists across launcher restarts without storing screenshots, raw OCR, editable values, viewport phrases or dialogue transcripts. EgoOS identity remains source authority; this is a derived performance/relationship reserve only.")
        button("RESET LONG-RUNNING SERIES HISTORY") {
            RavenOfficeSeasonOS.clear(activity)
            RavenGoldSitcomTopologyOS.clear(activity)
            RavenBackstageOS.clear()
            RavenGoldEpisodeStatsOS.clear()
            RavenOfficeBarService.signal(activity, "SCREEN_SEMANTIC", "state:series_reset|source:control_panel")
        }

        section("GOLD ELF SITCOM TOPOLOGY 🟠🎭", "Gold grammar is now a runtime topology: OPEN → BUILD → CALLBACK / ESCALATE → CLOSE. Targeted crosstalk beats parallel monologue; max two active speakers; pair and partner cooldowns preserve ensemble space; silence remains valid; terminal author note and dumbchecksum only happen after an actual closing event. Silent backstage relevance can earn the one crosstalk slot instead of canned pair roulette.")
        button("RESET GOLD CHEMISTRY + EPISODE STATS") {
            RavenGoldSitcomTopologyOS.clear(activity)
            RavenBackstageOS.clear()
            RavenGoldEpisodeStatsOS.clear()
            RavenOfficeBarService.signal(activity, "SCREEN_SEMANTIC", "state:gold_cooldown_reset|source:control_panel")
        }

        section("META-MAX MACHINE KINGDOM 🎭🪞👑", "Earned dialogue can use a structural running-bit ledger and Meta-Max showrunner: setup, callback, escalation, delayed brick joke, title card, cutaway, role reversal, continuity roast and fourth-wall emergency. It shapes lines after the existing cadence gate; it does not make the office chatter faster.")
        button("RESET META-MAX BITS") {
            RavenBitLedgerOS.clear()
            RavenOfficeBarService.signal(activity, "SCREEN_SEMANTIC", "state:metamax_reset|source:control_panel")
        }

        section("OFFICE SITCOM 🎬🧚", "The Follow-Me employee is a cast position, not the owner of the widget. Scene change, dwell, recurrence, pair chemistry, selected controls and deterministic turn cadence rotate the full routable office. Dialogue is screen-grounded; callbacks evolve without turning every sensor event into chatter.")
        button("RESET SITCOM CAST + CURRENT BITS") {
            RavenSitcomDirectorOS.clear(activity)
            RavenEpisodeScriptOS.clear()
            RavenBitLedgerOS.clear()
            RavenGoldSitcomTopologyOS.clear(activity)
            RavenBackstageOS.clear()
            RavenGoldEpisodeStatsOS.clear()
            RavenOfficeBarService.signal(activity, "SCREEN_SEMANTIC", "state:sitcom_reset|source:control_panel")
        }

        section("EPISODE SCRIPT MEMORY 📺🧠", "Process-session structural memory tracks scene owner, task, short derived subject, interactions, interruptions, returns, motifs and earned callbacks. It does not store screenshots, raw OCR frames, editable field values or a persistent transcript. Smart Capture/System UI/keyboard can become cameos while the real app keeps scene ownership.")
        button("RESET CURRENT EPISODE") {
            RavenEpisodeScriptOS.clear()
            RavenScreenMemoryOS.clear()
            RavenInteractionMemoryOS.clear()
            RavenBitLedgerOS.clear()
            RavenGoldSitcomTopologyOS.clear(activity)
            RavenBackstageOS.clear()
            RavenGoldEpisodeStatsOS.clear()
            RavenOfficeBarService.signal(activity, "SCREEN_SEMANTIC", "state:episode_reset|source:control_panel")
        }

        section("OMNI RV RESILIENCE MESH 🚐⚙️", "Follow-Me is the cockpit; senses remain modular underneath. The local heartbeat mesh reports FULL_VISION / SEMANTIC / OCR / PHONE_LIMP_HOME and explicit faults. It never grants permissions or silently re-arms MediaProjection. This borrows Omni RV's heartbeat, fault-router, backpressure and limp-home philosophy without making the overlay class a literal monolith.")
        button("REFRESH RESILIENCE MESH") {
            RavenOfficeBarService.signal(activity, "SCREEN_DIAGNOSTIC", "state:rv_mesh_refresh|source:control_panel")
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

        section("ACCESSIBILITY READ 🧭🔤", "Owner-armed visible semantics include bounded roles, selected/focused non-editable controls and structural interaction memory for tap/select/scroll/focus. Editable values and password nodes are excluded. Accessibility and OCR remain two witnesses to one semantic screen; transient keyboard/SystemUI/screenshot layers are demoted when a real application window is visible underneath.")
        button("ENABLE ACCESSIBILITY READ") { RavenAccessibilityReadOS.setEnabled(activity, true) }
        button("DISABLE ACCESSIBILITY READ") { RavenAccessibilityReadOS.setEnabled(activity, false) }
        button("OPEN ACCESSIBILITY AWARENESS") { RavenPermissionDeck.openForegroundAwareness(activity) }

        section("CROSS-APP META GOBLIN", "Follow-Me is the persistent TYPE_APPLICATION_OVERLAY body over ordinary apps: widened CHIP identity, OBSERVING when the screen is understood, COMMENT when an employee earns dialogue, tap for recent hauntings. Secure/lock/protected surfaces remain Android-controlled.")
        button("OPEN APPEAR-ON-TOP ACCESS") { RavenPermissionDeck.openOverlayAccess(activity) }
        button("ENABLE FOLLOW-ME") {
            if (!RavenFollowMeOverlay.enable(activity)) RavenPermissionDeck.openOverlayAccess(activity)
            else RavenOfficeBarService.signal(activity, "SYSTEM_DECK", "follow-me:enabled")
        }
        button("HIDE FOLLOW-ME") { RavenFollowMeOverlay.disable(activity) }

        section("FOREGROUND LEDGER", "Optional Android Usage Access adds a second app-resume signal when One UI Accessibility events are incomplete. App identity/timing only; no content.")
        button("OPEN USAGE ACCESS") { RavenPermissionDeck.openUsageAwareness(activity) }

        section("CALLBACK + SUBJECT MEMORY", "Callback memory tracks loops/returns; screen memory tracks only a few short derived semantic subjects in process memory. Episode Script and Meta-Max Bit Ledger add structural continuity without storing a raw screen/dialogue transcript.")
        button("CLEAR CURRENT CALLBACK BITS") {
            RavenCallbackMemoryOS.clear()
            RavenEpisodeScriptOS.clear()
            RavenInteractionMemoryOS.clear()
            RavenBitLedgerOS.clear()
            RavenGoldSitcomTopologyOS.clear(activity)
            RavenBackstageOS.clear()
            RavenGoldEpisodeStatsOS.clear()
        }

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
        val resilience = RavenRVResilienceOS.snapshot(activity)
        view.text = buildString {
            append(awareness.compact())
            append("\n").append(resilience.compact())
            append("\n").append(RavenOfficeSeasonOS.compact(activity))
            append("\n").append(RavenBitLedgerOS.compact())
            append("\n").append(RavenBackstageOS.compact())
            append("\n").append(RavenGoldEpisodeStatsOS.snapshot().compact())
            append("\n").append(RavenUsageSenseOS.compact(activity))
            append("\n").append(RavenNotificationSenseOS.summary(activity))
            append("\n").append(media.compact())
            append("\n").append(RavenGoblinReadOS.compact(activity))
            append("\n").append(RavenAccessibilityReadOS.compact(activity))
            append("\n").append(RavenFollowMeOverlay.status(activity))
            append("\n").append(galaxy.compact())
            append("\n").append(RavenOfficeGovernor.compact(activity))
            append("\n").append(RavenSitcomDirectorOS.compact(activity))
            append("\n").append(RavenEpisodeScriptOS.compact())
            append("\nSCREEN=").append(if (screen.available) "READABLE" else "UNKNOWN")
            append(" source=").append(screen.source)
            append(" confidence=").append(screen.confidence)
            append(" kind=").append(screen.semanticKind)
            append(" meta=").append(screen.meta)
            if (screen.focus.isNotBlank()) append("\nSUBJECT=").append(screen.focus.take(170))
        }
    }
}
