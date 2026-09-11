package com.iappyx.launcher.ravenos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Small deterministic device-state lane for RavenOS.
 *
 * Uses ordinary system broadcasts only; no hidden polling and no new dangerous permission.
 */
class RavenAmbientReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED ->
                RavenOfficeBarService.signal(context, "POWER", "connected")
            Intent.ACTION_POWER_DISCONNECTED ->
                RavenOfficeBarService.signal(context, "POWER", "disconnected")
            Intent.ACTION_BATTERY_LOW ->
                RavenOfficeBarService.signal(context, "BATTERY", "low")
            Intent.ACTION_BATTERY_OKAY ->
                RavenOfficeBarService.signal(context, "BATTERY", "okay")
        }
    }
}
