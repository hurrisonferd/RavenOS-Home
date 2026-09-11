package com.iappyx.launcher.ravenos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.DateFormat
import java.util.Date

/** Bounded RavenOS moment feed. Evidence stays underneath; this surface reads like conversation. */
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
        root.addView(t("short deterministic phone commentary · evidence stays underneath", 10f, false, 0xFFBDB7C7.toInt()))

        val scene = RavenPhoneSceneOS.snapshot(activity)
        root.addView(t("RIGHT NOW", 11f, true, 0xFFD8D8E4.toInt()))
        root.addView(t(sceneLine(scene), 12f))

        RavenEvidenceBoard.last(activity)?.let { latest ->
            val owner = latest.optString("owner", "?")
            val dialogue = latest.optString("dialogue").ifBlank { "Noted." }
            val author = latest.optString("authorNote")
            root.addView(t("CURRENT COMMENT", 11f, true, 0xFFD8D8E4.toInt()))
            root.addView(t(buildString {
                append(owner).append("\n").append(dialogue)
                if (author.isNotBlank()) append("\nAUTHOR'S NOTE: ").append(author)
            }, 12f))
        }

        val trace = RavenOfficeTraceStore.recent(activity, 12)
        root.addView(t("RECENT", 11f, true, 0xFFD8D8E4.toInt()))
        if (trace.isEmpty()) {
            root.addView(t("No reactions yet.", 12f))
        } else {
            trace.forEach { entry ->
                val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.at))
                val repeated = if (entry.repeats > 1) " · ×${entry.repeats}" else ""
                root.addView(t("$time · ${entry.owner}$repeated\n${entry.note.take(230)}", 11.5f))
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

    private fun sceneLine(scene: RavenPhoneSceneOS.Scene): String = buildString {
        append(scene.activeApp ?: "No foreground app")
        if (scene.mediaHot) {
            append(" · ")
            if (!scene.mediaTitle.isNullOrBlank()) append('“').append(scene.mediaTitle.take(42)).append("” playing")
            else append("music playing")
        } else {
            append(" · music quiet")
        }
        if (scene.recentSwitches >= 2) append(" · ").append(scene.recentSwitches).append(" app changes recently")
        if (scene.notificationBurst >= 2) append(" · ").append(scene.notificationBurst).append(" recent pings")
        append(" · Goblin Eye ").append(if (scene.goblinEyeActive) "on" else "off")
    }
}
