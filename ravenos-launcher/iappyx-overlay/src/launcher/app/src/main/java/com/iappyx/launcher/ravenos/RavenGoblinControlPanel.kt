package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/** Owner-facing WHY / replay / telemetry / capture-recipe console. */
object RavenGoblinControlPanel {
    fun show(activity: Activity) {
        val d = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(20))
        }
        val status = TextView(activity).apply {
            textSize = 11f
            setTextColor(0xFFD8D8E4.toInt())
            typeface = Typeface.MONOSPACE
            text = snapshot(activity)
            setTextIsSelectable(true)
        }
        content.addView(status)

        fun button(label: String, action: () -> String): Button = Button(activity).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setOnClickListener { status.text = action() }
        }

        content.addView(button("WHY DID THEY SAY THAT?") { RavenEvidenceBoard.why(activity) })
        content.addView(button("SAVE THAT SHIT") { RavenReplayOS.saveCurrent(activity) })
        content.addView(button("REPLAY / BOOKMARKS") { RavenReplayOS.compact(activity, 10) })
        content.addView(button("EVIDENCE BOARD") { RavenEvidenceBoard.compact(activity, 12) })
        content.addView(button("EVIDENCE CARD RECIPE") { RavenCaptureRecipeOS.execute(activity, RavenCaptureRecipeOS.Recipe.EVIDENCE_CARD).message })
        content.addView(button("PIN REGION RECIPE") { RavenCaptureRecipeOS.execute(activity, RavenCaptureRecipeOS.Recipe.PIN_REGION).message })
        content.addView(button("DIAGNOSTIC SNAPSHOT RECIPE") { RavenCaptureRecipeOS.execute(activity, RavenCaptureRecipeOS.Recipe.DIAGNOSTIC_SNAPSHOT).message })
        content.addView(button("REFRESH TELEMETRY") { snapshot(activity) })
        content.addView(button("NEXT DROP") {
            RavenOfficeBarService.signal(activity, "NEXT_DROP", "owner:goblin-control-panel")
            "NEXT DROP requested through deterministic Office signal."
        })
        content.addView(button("CLEAR EVIDENCE + RECURRENCE") {
            RavenEvidenceBoard.clear(activity)
            RavenMarkerBus.clear(activity)
            RavenComplexEventOS.clear(activity)
            "Goblin evidence, marker ring, and recurrence counters cleared."
        })

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            addView(content)
        }
        AlertDialog.Builder(activity)
            .setTitle("Goblin Vision Control")
            .setMessage("SEEING != SUGGESTING != DOING · deterministic signal outranks inference")
            .setView(scroll)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun snapshot(activity: Activity): String = buildString {
        append("GOBLIN VISION\n")
        append("Telemetry: ").append(RavenTelemetryPackOS.compact(activity)).append("\n\n")
        append(RavenEvidenceBoard.why(activity)).append("\n\n")
        append("Replay:\n").append(RavenReplayOS.compact(activity, 5)).append("\n\n")
        append("Reaction JSON: ").append(RavenReactionStateStore.readJson(activity)?.take(900) ?: "NONE")
    }
}
