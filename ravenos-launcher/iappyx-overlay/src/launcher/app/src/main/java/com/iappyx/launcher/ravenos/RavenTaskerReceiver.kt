package com.iappyx.launcher.ravenos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Explicit inbound automation gate for Tasker/automation apps.
 *
 * Exported by design, but every command requires RavenOS's locally-generated token. Only the
 * deterministic RavenCommandRouter is reachable from here. No arbitrary intents, shell, files,
 * clipboard, or private Office state are exposed through this receiver.
 */
class RavenTaskerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RavenTaskerBridge.ACTION_COMMAND) return

        val supplied = intent.getStringExtra(RavenTaskerBridge.EXTRA_TOKEN)
        if (!RavenTaskerBridge.validToken(context, supplied)) {
            RavenTaskerBridge.emit(context, "command_rejected", "bad_token")
            return
        }

        val command = intent.getStringExtra(RavenTaskerBridge.EXTRA_COMMAND)
            .orEmpty()
            .trim()
            .take(200)
        if (command.isBlank()) {
            RavenTaskerBridge.emit(context, "command_rejected", "empty_command")
            return
        }

        val result = RavenCommandRouter.execute(context, command)
        RavenTaskerBridge.emit(
            context,
            "command_result",
            "handled:${result.handled}|${result.message.take(160)}",
        )
    }
}
