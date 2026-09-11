package com.iappyx.launcher.ravenos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Explicit/revocable Android special-access entry points for high-awareness RavenOS modes. */
object RavenPermissionDeck {
    const val REQUEST_POST_NOTIFICATIONS = 0x5241

    fun ensureOfficeBarNotifications(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) return true
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS,
        )
        return false
    }

    fun openForegroundAwareness(activity: Activity) {
        safeStart(activity, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openNotificationAwareness(activity: Activity) {
        safeStart(activity, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openOverlayAccess(activity: Activity) {
        safeStart(
            activity,
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}"),
            ),
        )
    }

    fun openAppDetails(activity: Activity) {
        safeStart(
            activity,
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${activity.packageName}"),
            ),
        )
    }

    private fun safeStart(activity: Activity, intent: Intent) {
        try { activity.startActivity(intent) }
        catch (_: Throwable) {
            try { activity.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            catch (_: Throwable) {}
        }
    }
}
