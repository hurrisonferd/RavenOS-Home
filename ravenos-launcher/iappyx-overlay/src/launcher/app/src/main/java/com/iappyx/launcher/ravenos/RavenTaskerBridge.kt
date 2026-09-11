package com.iappyx.launcher.ravenos

import android.content.Context
import android.content.Intent

/**
 * Minimal, low-risk Tasker/automation bridge.
 *
 * RavenOS emits non-sensitive state events as ordinary broadcasts. Tasker can receive the action
 * `com.ravenos.launcher.TASKER_EVENT` with extras `event`, `detail`, and `at`. No inbound remote
 * command receiver is exported here, so another app cannot silently drive RavenOS through this
 * bridge.
 */
object RavenTaskerBridge {
    const val ACTION_EVENT = "com.ravenos.launcher.TASKER_EVENT"
    private const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"

    fun emit(context: Context, event: String, detail: String = "") {
        try {
            val safeEvent = event.trim().take(64)
            val safeDetail = detail.trim().take(240)
            context.applicationContext.sendBroadcast(
                Intent(ACTION_EVENT)
                    .putExtra("event", safeEvent)
                    .putExtra("detail", safeDetail)
                    .putExtra("at", System.currentTimeMillis()),
            )
        } catch (_: Throwable) {
            // Automation is optional; launcher operation never depends on a receiver existing.
        }
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

    fun summary(context: Context): String = if (isTaskerInstalled(context)) {
        "TASKER=INSTALLED · outbound event bridge ready"
    } else {
        "TASKER=NOT_INSTALLED · RavenOS event broadcasts still available"
    }
}
