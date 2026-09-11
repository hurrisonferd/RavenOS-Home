package com.iappyx.launcher.ravenos

import android.content.Context
import android.os.Build
import android.os.PowerManager

/** Samsung/Android survival diagnostics. No vendor-private API dependency. */
object RavenGalaxyHauntOS {
    data class Snapshot(
        val samsung: Boolean,
        val model: String,
        val batteryOptimizationExempt: Boolean,
        val notificationAwareness: Boolean,
        val overlayAccess: Boolean,
        val foregroundAwareness: Boolean,
        val followMe: Boolean,
        val goblinEye: Boolean,
        val mediaSessionReady: Boolean,
    ) {
        fun compact(): String = buildString {
            append(if (samsung) "GALAXY" else "ANDROID")
            if (model.isNotBlank()) append(" ").append(model)
            append(" · BATTERY_EXEMPT=").append(onOff(batteryOptimizationExempt))
            append(" · NOTIF=").append(onOff(notificationAwareness))
            append(" · FG=").append(onOff(foregroundAwareness))
            append(" · OVERLAY=").append(onOff(overlayAccess))
            append(" · FOLLOW=").append(onOff(followMe))
            append(" · EYE=").append(onOff(goblinEye))
            append(" · MEDIA=").append(onOff(mediaSessionReady))
        }

        private fun onOff(value: Boolean) = if (value) "ON" else "OFF"
    }

    fun snapshot(context: Context): Snapshot {
        val awareness = RavenAwarenessStatus.snapshot(context)
        val power = context.getSystemService(PowerManager::class.java)
        val exempt = runCatching { power?.isIgnoringBatteryOptimizations(context.packageName) == true }.getOrDefault(false)
        return Snapshot(
            samsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true),
            model = Build.MODEL.orEmpty(),
            batteryOptimizationExempt = exempt,
            notificationAwareness = awareness.notificationAwareness,
            overlayAccess = awareness.overlayAccess,
            foregroundAwareness = awareness.foregroundAwareness,
            followMe = awareness.followMeEnabled,
            goblinEye = RavenScreenWatchService.isActive(context),
            mediaSessionReady = awareness.notificationAwareness,
        )
    }

    fun samsungInstructions(context: Context): String {
        val s = snapshot(context)
        return if (s.samsung) {
            "Samsung survival: Settings → Battery → Background usage limits → Never sleeping apps → RavenOS Launcher. Battery optimization exemption is ${if (s.batteryOptimizationExempt) "already ON" else "not confirmed"}."
        } else {
            "Background survival varies by manufacturer. Keep RavenOS out of deep-sleep/battery-restriction lists when persistent Goblin Vision is desired."
        }
    }
}
