package com.iappyx.launcher.ravenos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/** Dynamic screen/user-presence monitor owned by the visible Office Bar service. */
object RavenScreenMonitor {
    private var receiver: BroadcastReceiver? = null

    fun start(context: Context) {
        if (receiver != null) return
        val app = context.applicationContext
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF ->
                        RavenOfficeBarService.signal(ctx, "NIGHT", "screen:off")
                    Intent.ACTION_SCREEN_ON ->
                        RavenOfficeBarService.signal(ctx, "DEVICE", "screen:on")
                    Intent.ACTION_USER_PRESENT ->
                        RavenOfficeBarService.signal(ctx, "HOME", "user:present")
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        try {
            app.registerReceiver(r, filter)
            receiver = r
        } catch (_: Throwable) {
            receiver = null
        }
    }

    fun stop(context: Context) {
        val r = receiver ?: return
        receiver = null
        try { context.applicationContext.unregisterReceiver(r) } catch (_: Throwable) {}
    }
}
