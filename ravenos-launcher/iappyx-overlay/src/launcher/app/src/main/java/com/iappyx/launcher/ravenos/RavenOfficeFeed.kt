package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.DateFormat
import java.util.Date

/** AIO-inspired, bounded RavenOS state feed. No infinite timeline and no cloud dependency. */
object RavenOfficeFeed {
    fun show(activity: Activity) {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        fun t(value: String, size: Float = 12f, bold: Boolean = false, color: Int = Color.WHITE) = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(16))
        }
        root.addView(t("OFFICE FEED", 20f, true, 0xFFFF64B4.toInt()))
        root.addView(t("bounded local context · newest first", 10f, false, 0xFFBDB7C7.toInt()))

        val requirements = RavenHauntRequirements.evaluate(activity).take(4)
        if (requirements.isNotEmpty()) {
            root.addView(t("ACTIVE REQUIREMENTS", 11f, true, 0xFFD8D8E4.toInt()))
            requirements.forEach { m ->
                root.addView(t("${m.title}\n${m.body}\n${m.owner} · priority ${m.priority}", 12f))
            }
        }

        val audio = RavenSystemDeck.snapshot(activity)
        root.addView(t("SYSTEM", 11f, true, 0xFFD8D8E4.toInt()))
        root.addView(t("Media ${audio.media.percent}% · Ring ${audio.ring.percent}% · Alarm ${audio.alarm.percent}%", 12f))
        root.addView(t(RavenTaskerBridge.summary(activity), 11f, false, 0xFFBDB7C7.toInt()))

        val trace = RavenOfficeTraceStore.recent(activity, 10)
        root.addView(t("RECENT OFFICE", 11f, true, 0xFFD8D8E4.toInt()))
        if (trace.isEmpty()) {
            root.addView(t("No routing receipts yet.", 12f))
        } else {
            trace.forEach { entry ->
                val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.at))
                val detail = if (entry.detail.isBlank()) "" else "\n${entry.detail.take(110)}"
                root.addView(t("$time · ${entry.owner} · ${entry.signal} · ${entry.haunt}$detail\n${entry.note.take(140)}", 11f))
            }
        }

        val scroll = ScrollView(activity).apply { addView(root) }
        AlertDialog.Builder(activity)
            .setView(scroll)
            .setNegativeButton("Close", null)
            .setPositiveButton("Clear trace") { _, _ -> RavenOfficeTraceStore.clear(activity) }
            .show()
        RavenTaskerBridge.emit(activity, "office_feed", "opened")
    }
}
