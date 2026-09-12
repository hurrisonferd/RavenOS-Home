package com.iappyx.launcher.ravenos

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.iappyx.launcher.notify.NotificationBadgeListener

/** Runtime readiness snapshot. Reads Android state only; it grants nothing. */
data class RavenAwarenessSnapshot(
    val applicationId: String,
    val defaultHome: Boolean,
    val notificationPermission: Boolean,
    val foregroundAwareness: Boolean,
    val notificationAwareness: Boolean,
    val overlayAccess: Boolean,
    val followMeEnabled: Boolean,
    val officeBarEnabled: Boolean,
    val mediaSessionReady: Boolean,
    val goblinEyeActive: Boolean,
    val batteryOptimizationExempt: Boolean,
) {
    val passed: Int get() = listOf(
        defaultHome,
        notificationPermission,
        foregroundAwareness,
        notificationAwareness,
        overlayAccess,
        followMeEnabled,
        officeBarEnabled,
        mediaSessionReady,
        goblinEyeActive,
        batteryOptimizationExempt,
    ).count { it }
    val total: Int get() = 10

    fun compact(): String = buildString {
        append("RavenOS readiness $passed/$total")
        append(" · HOME=").append(flag(defaultHome))
        append(" · BAR=").append(flag(officeBarEnabled))
        append(" · FG=").append(flag(foregroundAwareness))
        append(" · NOTIF=").append(flag(notificationAwareness))
        append(" · MEDIA=").append(flag(mediaSessionReady))
        append(" · OVERLAY=").append(flag(overlayAccess))
        append(" · FOLLOW=").append(flag(followMeEnabled))
        append(" · EYE=").append(flag(goblinEyeActive))
        append(" · BATTERY=").append(flag(batteryOptimizationExempt))
    }

    private fun flag(value: Boolean) = if (value) "ON" else "OFF"
}

object RavenAwarenessStatus {
    fun snapshot(context: Context): RavenAwarenessSnapshot {
        val notificationAware = notificationListenerEnabled(context)
        val power = context.getSystemService(PowerManager::class.java)
        val batteryExempt = runCatching { power?.isIgnoringBatteryOptimizations(context.packageName) == true }.getOrDefault(false)
        return RavenAwarenessSnapshot(
            applicationId = context.packageName,
            defaultHome = isDefaultHome(context),
            notificationPermission = hasNotificationPermission(context),
            foregroundAwareness = serviceEnabled(context, RavenForegroundAwarenessService::class.java),
            notificationAwareness = notificationAware,
            overlayAccess = Settings.canDrawOverlays(context),
            followMeEnabled = RavenFollowMeOverlay.isEnabled(context),
            officeBarEnabled = RavenOfficeBarService.isEnabled(context),
            mediaSessionReady = notificationAware,
            goblinEyeActive = RavenScreenWatchService.isActive(context),
            batteryOptimizationExempt = batteryExempt,
        )
    }

    fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationListenerEnabled(context: Context): Boolean {
        val expected = ComponentName(context, NotificationBadgeListener::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private fun serviceEnabled(context: Context, clazz: Class<*>): Boolean {
        val expected = ComponentName(context, clazz)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}
