package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/** Owner-facing WHY / replay / telemetry / screen-context / capture-recipe console. */
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

        content.addView(button("WHAT DOES THE OFFICE SEE?") { screenSnapshot(activity) })
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
            RavenCallbackMemoryOS.clear()
            RavenScreenMemoryOS.clear()
            RavenInterruptibilityOS.clear(activity)
            RavenOfficeGovernor.clearStats(activity)
            "Goblin evidence, marker ring, recurrence, screen-subject memory, cadence state, and governor stats cleared."
        })

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            addView(content)
        }
        AlertDialog.Builder(activity)
            .setTitle("Goblin Vision Control")
            .setMessage("SEEING != UNDERSTANDING != SPEAKING != DOING · screen meaning outranks callback noise")
            .setView(scroll)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun screenSnapshot(activity: Activity): String {
        val s = RavenScreenContextOS.snapshot(activity)
        val reaction = RavenReactionStateStore.readJson(activity)
        return buildString {
            append("SCREEN-FIRST OFFICE\n")
            append("AVAILABLE=").append(s.available)
            append(" · SOURCE=").append(s.source)
            append(" · CONFIDENCE=").append(s.confidence)
            append(" · AGE=").append(if (s.ageMs == Long.MAX_VALUE) "NONE" else "${s.ageMs}ms")
            append("\nAPP=").append(s.appLabel ?: "UNKNOWN")
            append(" · KIND=").append(s.semanticKind)
            append(" · META=").append(s.meta)
            append(" · KEYBOARD=").append(s.keyboardLike)
            append("\nSUMMARY=").append(s.semanticSummary.ifBlank { "UNKNOWN" })
            append("\nSUBJECT=").append(s.focus.ifBlank { "UNKNOWN" })
            append("\nQUIET_ZONE=").append(s.quietZone)
            append(" · BLOCKS=").append(s.blockCount)
            append("\n").append(RavenOfficeGovernor.compact(activity))
            append("\n\nREACTION=").append(reaction?.take(1000) ?: "NONE")
        }
    }

    private fun snapshot(activity: Activity): String = buildString {
        append("GOBLIN VISION\n")
        append("Telemetry: ").append(RavenTelemetryPackOS.compact(activity)).append("\n")
        append("Cadence: ").append(RavenOfficeGovernor.compact(activity)).append("\n\n")
        append(screenSnapshot(activity)).append("\n\n")
        append(RavenEvidenceBoard.why(activity)).append("\n\n")
        append("Replay:\n").append(RavenReplayOS.compact(activity, 5))
    }
}
