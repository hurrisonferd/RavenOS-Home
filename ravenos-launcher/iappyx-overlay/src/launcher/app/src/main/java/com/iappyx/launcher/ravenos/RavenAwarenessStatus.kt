package com.iappyx.launcher.ravenos

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.iappyx.launcher.notify.NotificationBadgeListener

/**
 * Runtime readiness snapshot for the invasive/haunted parts of RavenOS.
 *
 * This reads only Android permission/role state. It does not grant anything itself.
 */
data class RavenAwarenessSnapshot(
    val applicationId: String,
    val defaultHome: Boolean,
    val notificationPermission: Boolean,
    val foregroundAwareness: Boolean,
    val notificationAwareness: Boolean,
    val overlayAccess: Boolean,
    val officeBarEnabled: Boolean,
) {
    val passed: Int get() = listOf(
        defaultHome,
        notificationPermission,
        foregroundAwareness,
        notificationAwareness,
        overlayAccess,
        officeBarEnabled,
    ).count { it }
    val total: Int get() = 6

    fun compact(): String = buildString {
        append("RavenOS readiness $passed/$total")
        append(" · HOME=").append(flag(defaultHome))
        append(" · BAR=").append(flag(officeBarEnabled))
        append(" · FG=").append(flag(foregroundAwareness))
        append(" · NOTIF=").append(flag(notificationAwareness))
        append(" · OVERLAY=").append(flag(overlayAccess))
    }

    private fun flag(value: Boolean) = if (value) "ON" else "OFF"
}

object RavenAwarenessStatus {
    fun snapshot(context: Context): RavenAwarenessSnapshot = RavenAwarenessSnapshot(
        applicationId = context.packageName,
        defaultHome = isDefaultHome(context),
        notificationPermission = hasNotificationPermission(context),
        foregroundAwareness = serviceEnabled(context, RavenForegroundAwarenessService::class.java),
        notificationAwareness = notificationListenerEnabled(context),
        overlayAccess = Settings.canDrawOverlays(context),
        officeBarEnabled = RavenOfficeBarService.isEnabled(context),
    )

    fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun notificationListenerEnabled(context: Context): Boolean {
        val expected = ComponentName(context, NotificationBadgeListener::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == expected
        }
    }

    private fun serviceEnabled(context: Context, clazz: Class<*>): Boolean {
        val expected = ComponentName(context, clazz)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == expected
        }
    }
}
