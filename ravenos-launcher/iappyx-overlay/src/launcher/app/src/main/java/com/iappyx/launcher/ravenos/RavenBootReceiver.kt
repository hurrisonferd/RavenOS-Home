package com.iappyx.launcher.ravenos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restores the user-visible Office Bar after normal reboot/package replacement when it was enabled. */
class RavenBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                if (RavenOfficeBarService.isEnabled(context)) {
                    RavenOfficeBarService.restore(context, intent.action ?: "boot")
                }
            }
        }
    }
}
