package com.iappyx.launcher.ravenos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

/**
 * Small deterministic device-state lane for RavenOS.
 *
 * Uses ordinary system broadcasts only; no hidden polling and no new dangerous permission.
 */
class RavenAmbientReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val battery = batteryPercent(context)
        val suffix = battery?.let { "|battery:$it%" }.orEmpty()
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED ->
                RavenOfficeBarService.signal(context, "POWER", "connected$suffix")
            Intent.ACTION_POWER_DISCONNECTED ->
                RavenOfficeBarService.signal(context, "POWER", "disconnected$suffix")
            Intent.ACTION_BATTERY_LOW ->
                RavenOfficeBarService.signal(context, "BATTERY", "low$suffix")
            Intent.ACTION_BATTERY_OKAY ->
                RavenOfficeBarService.signal(context, "BATTERY", "okay$suffix")
        }
    }

    private fun batteryPercent(context: Context): Int? = try {
        val manager = context.getSystemService(BatteryManager::class.java) ?: return null
        manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .takeIf { it in 0..100 }
    } catch (_: Throwable) {
        null
    }
}
