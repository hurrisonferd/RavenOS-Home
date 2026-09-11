package com.iappyx.launcher.ravenos

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import java.security.SecureRandom

/**
 * Small Tasker/automation bridge.
 *
 * Outbound: RavenOS emits non-sensitive state events on ACTION_EVENT.
 * Inbound: an exported receiver accepts ACTION_COMMAND only when the caller supplies RavenOS's
 * locally-generated 128-bit token. The payload is routed through RavenCommandRouter only; there is
 * no shell, arbitrary Intent execution, file access, or private-state export.
 */
object RavenTaskerBridge {
    const val ACTION_EVENT = "com.ravenos.launcher.TASKER_EVENT"
    const val ACTION_COMMAND = "com.ravenos.launcher.TASKER_COMMAND"
    const val EXTRA_TOKEN = "token"
    const val EXTRA_COMMAND = "command"
    private const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"
    private const val PREFS = "ravenos_tasker_bridge_v2"
    private const val KEY_TOKEN = "token"

    fun emit(context: Context, event: String, detail: String = "") {
        try {
            context.applicationContext.sendBroadcast(
                Intent(ACTION_EVENT)
                    .putExtra("event", event.trim().take(64))
                    .putExtra("detail", detail.trim().take(240))
                    .putExtra("at", System.currentTimeMillis()),
            )
        } catch (_: Throwable) {
            // Automation is optional; launcher operation never depends on a receiver existing.
        }
    }

    fun token(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_TOKEN, null)?.takeIf { it.length >= 32 }?.let { return it }
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val generated = bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        prefs.edit().putString(KEY_TOKEN, generated).apply()
        return generated
    }

    fun rotateToken(context: Context): String {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_TOKEN).apply()
        return token(context)
    }

    fun validToken(context: Context, supplied: String?): Boolean {
        if (supplied.isNullOrBlank()) return false
        val expected = token(context)
        if (supplied.length != expected.length) return false
        var diff = 0
        for (i in expected.indices) diff = diff or (expected[i].code xor supplied[i].code)
        return diff == 0
    }

    fun isTaskerInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(TASKER_PACKAGE, 0)
        true
    } catch (_: Throwable) {
        false
    }

    fun openTasker(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(TASKER_PACKAGE) ?: return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun showSetup(activity: Activity) {
        val current = token(activity)
        val body = """
            OUTBOUND ACTION
            $ACTION_EVENT

            INBOUND ACTION
            $ACTION_COMMAND

            Include extras:
            token = $current
            command = office kyu   (or any local Raven command)

            Inbound commands are token-gated and use RavenOS's deterministic command router only.
        """.trimIndent()
        AlertDialog.Builder(activity)
            .setTitle("RavenOS ↔ Tasker")
            .setMessage(body)
            .setPositiveButton("Copy token") { _, _ ->
                val clipboard = activity.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("RavenOS Tasker token", current))
            }
            .setNeutralButton("Open Tasker") { _, _ -> openTasker(activity) }
            .setNegativeButton("Rotate token") { _, _ -> rotateToken(activity) }
            .show()
    }

    fun summary(context: Context): String {
        val state = if (isTaskerInstalled(context)) "TASKER=INSTALLED" else "TASKER=NOT_INSTALLED"
        return "$state · outbound events + token-gated inbound commands ready"
    }
}
