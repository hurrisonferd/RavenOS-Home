package com.iappyx.launcher.ravenos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.iappyx.launcher.LauncherActivity
import com.iappyx.launcher.R

/**
 * Persistent, user-visible RavenOS Office Bar.
 *
 * It is intentionally an ongoing notification rather than an invisible monitor:
 * - silent / low-importance channel
 * - current owner, emoji, UI accent, signal and deterministic author's note
 * - NEXT / AUTO / QUIET controls are always available from the notification
 * - no model call is required to update it
 *
 * Deeper phone awareness is supplied by explicit RavenOS/Faeryware signals; this service
 * does not scrape other apps on its own.
 */
class RavenOfficeBarService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        when (intent?.action) {
            ACTION_SIGNAL -> {
                prefs.edit()
                    .putString(KEY_SIGNAL, intent.getStringExtra(EXTRA_SIGNAL) ?: "HOME")
                    .putString(KEY_DETAIL, intent.getStringExtra(EXTRA_DETAIL) ?: "")
                    .apply()
            }
            ACTION_PIN -> {
                val requested = intent.getStringExtra(EXTRA_OWNER)
                val member = RavenOfficeRegistry.member(requested)
                if (member?.routable == true) {
                    prefs.edit().putString(KEY_MANUAL_OWNER, member.id).putBoolean(KEY_QUIET, false).apply()
                }
            }
            ACTION_NEXT -> {
                val signal = prefs.getString(KEY_SIGNAL, "HOME") ?: "HOME"
                val detail = prefs.getString(KEY_DETAIL, "") ?: ""
                val current = RavenOfficeRegistry.route(signal, detail, prefs.getString(KEY_MANUAL_OWNER, null))
                val list = RavenOfficeRegistry.routableMembers
                val idx = list.indexOfFirst { it.id == current.id }.let { if (it < 0) 0 else it }
                val next = list[(idx + 1) % list.size]
                prefs.edit().putString(KEY_MANUAL_OWNER, next.id).putBoolean(KEY_QUIET, false).apply()
            }
            ACTION_AUTO -> prefs.edit().remove(KEY_MANUAL_OWNER).putBoolean(KEY_QUIET, false).apply()
            ACTION_QUIET -> prefs.edit().putBoolean(KEY_QUIET, !prefs.getBoolean(KEY_QUIET, false)).apply()
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val signal = prefs.getString(KEY_SIGNAL, "HOME") ?: "HOME"
        val detail = prefs.getString(KEY_DETAIL, "") ?: ""
        val manual = prefs.getString(KEY_MANUAL_OWNER, null)
        val quiet = prefs.getBoolean(KEY_QUIET, false)
        val member = if (quiet) RavenOfficeRegistry.member("NYX")!! else RavenOfficeRegistry.route(signal, detail, manual)
        val note = if (quiet) "Quiet watch. The office is still here; only material signals break silence."
                   else RavenOfficeRegistry.authorNote(member, signal, detail)

        val openHome = PendingIntent.getActivity(
            this,
            10,
            Intent(this, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val next = serviceAction(ACTION_NEXT, 11)
        val auto = serviceAction(ACTION_AUTO, 12)
        val quietAction = serviceAction(ACTION_QUIET, 13)

        val mode = if (manual == null) "AUTO" else "PINNED"
        val title = "${member.emoji} ${member.id} · ${prettySignal(signal)}"
        val body = "AUTHOR'S NOTE: $note"
        val big = buildString {
            append(body)
            append("\n\n")
            append(member.lane)
            append(" · ")
            append(mode)
            if (detail.isNotBlank()) {
                append("\n")
                append(detail.take(180))
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setColor(member.accent)
            .setColorized(true)
            .setContentIntent(openHome)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, "NEXT", next)
            .addAction(0, "AUTO", auto)
            .addAction(0, if (quiet) "WAKE" else "QUIET", quietAction)
            .build()
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, RavenOfficeBarService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RavenOS Office Bar",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Reactive RavenOS office-member presence and deterministic author's notes"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun prettySignal(signal: String): String = signal
        .trim()
        .replace('_', ' ')
        .lowercase()
        .split(' ')
        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    companion object {
        private const val PREFS = "ravenos_office_bar_v1"
        private const val KEY_SIGNAL = "signal"
        private const val KEY_DETAIL = "detail"
        private const val KEY_MANUAL_OWNER = "manual_owner"
        private const val KEY_QUIET = "quiet"
        private const val EXTRA_SIGNAL = "signal"
        private const val EXTRA_DETAIL = "detail"
        private const val EXTRA_OWNER = "owner"

        const val CHANNEL_ID = "ravenos_office_bar"
        const val NOTIFICATION_ID = 0x524156
        const val ACTION_SIGNAL = "com.ravenos.launcher.office.SIGNAL"
        const val ACTION_PIN = "com.ravenos.launcher.office.PIN"
        const val ACTION_NEXT = "com.ravenos.launcher.office.NEXT"
        const val ACTION_AUTO = "com.ravenos.launcher.office.AUTO"
        const val ACTION_QUIET = "com.ravenos.launcher.office.QUIET"

        fun signal(context: Context, signal: String, detail: String = "") = start(
            context,
            Intent(context, RavenOfficeBarService::class.java)
                .setAction(ACTION_SIGNAL)
                .putExtra(EXTRA_SIGNAL, signal)
                .putExtra(EXTRA_DETAIL, detail),
        )

        fun pin(context: Context, owner: String) = start(
            context,
            Intent(context, RavenOfficeBarService::class.java)
                .setAction(ACTION_PIN)
                .putExtra(EXTRA_OWNER, owner),
        )

        fun next(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_NEXT))
        fun auto(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_AUTO))
        fun toggleQuiet(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_QUIET))

        private fun start(context: Context, intent: Intent) {
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Throwable) {
                // The launcher must remain usable even if Android refuses an FGS start.
            }
        }
    }
}
