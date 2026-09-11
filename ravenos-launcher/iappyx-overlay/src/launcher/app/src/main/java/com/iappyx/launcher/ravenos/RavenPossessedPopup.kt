package com.iappyx.launcher.ravenos

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/** Niagara-inspired app micro-room: app, resident, shortcuts, and bounded actions in one popup. */
object RavenPossessedPopup {
    fun show(activity: Activity, entry: RavenAppCatalog.Entry) {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val detail = "package:${entry.packageName}"
        val member = RavenOfficeRegistry.route("FOREGROUND_APP", detail)
        val note = RavenOfficeRegistry.authorNote(member, "FOREGROUND_APP", detail)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(10))
        }
        val head = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(ImageView(activity).apply {
            setImageDrawable(RavenAppCatalog.icon(activity, entry))
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val labels = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        labels.addView(TextView(activity).apply {
            text = entry.label
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        labels.addView(TextView(activity).apply {
            text = "${member.emoji} ${member.id} · ${member.lane}"
            textSize = 11f
            setTextColor(0xFFD8D8E4.toInt())
        })
        head.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(head)
        root.addView(TextView(activity).apply {
            text = note
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
        })

        val shortcuts = shortcuts(activity, entry.packageName)
        if (shortcuts.isNotEmpty()) {
            root.addView(TextView(activity).apply {
                text = "APP SHORTCUTS"
                textSize = 10f
                setTextColor(0xFFBDB7C7.toInt())
            })
            shortcuts.forEach { shortcut ->
                root.addView(Button(activity).apply {
                    text = shortcut.shortLabel?.toString() ?: shortcut.id
                    isAllCaps = false
                    setOnClickListener {
                        try {
                            activity.getSystemService(LauncherApps::class.java).startShortcut(shortcut, null, null)
                            RavenOfficeBarService.signal(activity, "APP_LAUNCH", "$detail|shortcut:${shortcut.id}")
                        } catch (_: Throwable) {}
                    }
                })
            }
        }

        val actions = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        fun action(label: String, block: () -> Unit) = Button(activity).apply {
            text = label
            isAllCaps = false
            setOnClickListener { block() }
        }
        actions.addView(action("Open") { launch(activity, entry) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(action("Pin ${member.id}") { RavenOfficeBarService.pin(activity, member.id) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(action("Info") {
            try {
                activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${entry.packageName}")))
            } catch (_: Throwable) {}
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(actions)

        AlertDialog.Builder(activity)
            .setView(root)
            .setNegativeButton("Close", null)
            .show()
        RavenOfficeBarService.signal(activity, "FOREGROUND_APP", "$detail|popup:possessed")
        RavenTaskerBridge.emit(activity, "possessed_popup", entry.packageName)
    }

    private fun launch(activity: Activity, entry: RavenAppCatalog.Entry) {
        try {
            activity.startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(entry.packageName, entry.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            RavenOfficeBarService.signal(activity, "APP_LAUNCH", "package:${entry.packageName}")
        } catch (_: Throwable) {}
    }

    private fun shortcuts(activity: Activity, packageName: String): List<android.content.pm.ShortcutInfo> {
        return try {
            val launcherApps = activity.getSystemService(LauncherApps::class.java)
            val query = LauncherApps.ShortcutQuery()
                .setPackage(packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty().take(4)
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
