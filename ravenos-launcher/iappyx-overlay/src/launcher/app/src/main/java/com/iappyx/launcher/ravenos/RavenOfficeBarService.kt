package com.iappyx.launcher.ravenos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
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
 * - NEXT / AUTO / QUIET / HAUNT controls are available from the notification
 * - no model call is required to update it
 * - explicit sleep survives ordinary launcher signals; explicit wake restores it
 */
class RavenOfficeBarService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        RavenScreenMonitor.start(this)
    }

    override fun onDestroy() {
        RavenScreenMonitor.stop(this)
        RavenFollowMeOverlay.hide()
        RavenHomeAura.hide()
        RavenHomeWhisper.hide()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val action = intent?.action

        if (action == ACTION_DISABLE) {
            prefs.edit()
                .putBoolean(KEY_ENABLED, false)
                .putBoolean(KEY_EXPLICIT_DISABLED, true)
                .apply()
            val stateAt = RavenOfficeStateStore.read(this)?.updatedAt ?: 0L
            RavenSurfaceIntegrity.mark(
                this,
                RavenSurfaceIntegrity.OFFICE_BAR,
                "INACTIVE",
                stateAt,
                "explicit_sleep",
            )
            RavenFollowMeOverlay.hide()
            RavenHomeAura.hide()
            RavenHomeWhisper.hide()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Ordinary context signals must not silently undo Raven's explicit sleep choice.
        if (action == ACTION_SIGNAL && prefs.getBoolean(KEY_EXPLICIT_DISABLED, false)) {
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_ENABLE -> {
                prefs.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putBoolean(KEY_EXPLICIT_DISABLED, false)
                    .apply()
            }
            ACTION_RESTORE -> {
                if (!prefs.getBoolean(KEY_ENABLED, false)) return START_NOT_STICKY
                val reason = intent.getStringExtra(EXTRA_DETAIL).orEmpty()
                if (reason.isNotBlank()) prefs.edit().putString(KEY_DETAIL, "restored:$reason").apply()
            }
            ACTION_SIGNAL -> prefs.edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_SIGNAL, intent.getStringExtra(EXTRA_SIGNAL) ?: "HOME")
                .putString(KEY_DETAIL, intent.getStringExtra(EXTRA_DETAIL) ?: "")
                .apply()
            ACTION_PIN -> {
                val requested = intent.getStringExtra(EXTRA_OWNER)
                val member = RavenOfficeRegistry.member(requested)
                if (member?.routable == true) {
                    prefs.edit()
                        .putBoolean(KEY_ENABLED, true)
                        .putBoolean(KEY_EXPLICIT_DISABLED, false)
                        .putString(KEY_MANUAL_OWNER, member.id)
                        .putBoolean(KEY_QUIET, false)
                        .apply()
                }
            }
            ACTION_NEXT -> {
                val signal = prefs.getString(KEY_SIGNAL, "HOME") ?: "HOME"
                val detail = prefs.getString(KEY_DETAIL, "") ?: ""
                val current = RavenOfficeRegistry.route(signal, detail, prefs.getString(KEY_MANUAL_OWNER, null))
                val list = RavenOfficeRegistry.routableMembers
                val idx = list.indexOfFirst { it.id == current.id }.let { if (it < 0) 0 else it }
                val next = list[(idx + 1) % list.size]
                prefs.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putBoolean(KEY_EXPLICIT_DISABLED, false)
                    .putString(KEY_MANUAL_OWNER, next.id)
                    .putBoolean(KEY_QUIET, false)
                    .apply()
            }
            ACTION_AUTO -> prefs.edit()
                .putBoolean(KEY_ENABLED, true)
                .putBoolean(KEY_EXPLICIT_DISABLED, false)
                .remove(KEY_MANUAL_OWNER)
                .putBoolean(KEY_QUIET, false)
                .apply()
            ACTION_QUIET -> prefs.edit()
                .putBoolean(KEY_ENABLED, true)
                .putBoolean(KEY_EXPLICIT_DISABLED, false)
                .putBoolean(KEY_QUIET, !prefs.getBoolean(KEY_QUIET, false))
                .apply()
            ACTION_HAUNT_CYCLE -> {
                val next = RavenHauntModeStore.cycle(this)
                prefs.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putBoolean(KEY_EXPLICIT_DISABLED, false)
                    .putString(KEY_DETAIL, "haunt:${next.label}")
                    .apply()
            }
        }

        if (!prefs.getBoolean(KEY_ENABLED, false)) return START_NOT_STICKY
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        RavenOfficeStateStore.read(this)?.let { snapshot ->
            RavenSurfaceIntegrity.mark(
                this,
                RavenSurfaceIntegrity.OFFICE_BAR,
                "POSTED",
                snapshot.updatedAt,
                "foreground_notification",
            )
        }
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val signal = prefs.getString(KEY_SIGNAL, "HOME") ?: "HOME"
        val detail = prefs.getString(KEY_DETAIL, "") ?: ""
        val manual = prefs.getString(KEY_MANUAL_OWNER, null)
        val quiet = prefs.getBoolean(KEY_QUIET, false)
        val hauntMode = RavenHauntModeStore.get(this)
        val member = if (quiet) RavenOfficeRegistry.member("NYX")!! else RavenOfficeRegistry.route(signal, detail, manual)
        val note = if (quiet) "Quiet watch. The office is still here; only material signals break silence."
                   else RavenOfficeRegistry.authorNote(member, signal, detail)

        // One canonical local snapshot, then every projection consumes the same decision.
        RavenOfficeStateStore.write(
            this,
            member = member,
            signal = signal,
            detail = detail,
            note = note,
            hauntMode = hauntMode,
            manual = manual != null,
            quiet = quiet,
        )
        RavenOfficeTraceStore.record(this, member, signal, detail, note, hauntMode)

        RavenHomeAura.render(member, hauntMode)
        RavenHomeWhisper.render(member, note, signal, detail, hauntMode)
        RavenFollowMeOverlay.render(this, member, signal, note, detail, hauntMode)

        val openHome = PendingIntent.getActivity(
            this,
            10,
            Intent(this, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val next = serviceAction(ACTION_NEXT, 11)
        val auto = serviceAction(ACTION_AUTO, 12)
        val quietAction = serviceAction(ACTION_QUIET, 13)
        val haunt = serviceAction(ACTION_HAUNT_CYCLE, 14)

        val mode = if (manual == null) "AUTO" else "PINNED"
        val title = "${member.emoji} ${member.id} · ${prettySignal(signal)}"
        val body = "AUTHOR'S NOTE: $note"
        val contextLine = buildString {
            append(member.lane)
            append(" · ").append(mode)
            append(" · ").append(hauntMode.label)
            if (detail.isNotBlank()) append(" · ").append(detail.take(120))
        }
        val big = "$body\n\n$contextLine"

        val custom = RemoteViews(packageName, R.layout.ravenos_office_bar).apply {
            val textColor = contrastText(member.accent)
            val secondary = if (textColor == Color.BLACK) 0xCC000000.toInt() else 0xDDFFFFFF.toInt()
            setInt(R.id.raven_office_root, "setBackgroundColor", member.accent)
            setTextViewText(R.id.raven_office_owner, title)
            setTextViewText(R.id.raven_office_note, body)
            setTextViewText(R.id.raven_office_context, contextLine)
            setTextColor(R.id.raven_office_owner, textColor)
            setTextColor(R.id.raven_office_note, textColor)
            setTextColor(R.id.raven_office_context, secondary)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setCustomContentView(custom)
            .setCustomBigContentView(custom)
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
            .addAction(0, "HAUNT", haunt)
            .build()
    }

    private fun contrastText(color: Int): Int {
        val perceived = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        return if (perceived >= 175) Color.BLACK else Color.WHITE
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this, requestCode, Intent(this, RavenOfficeBarService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "RavenOS Office Bar", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Reactive RavenOS office-member presence and deterministic author's notes"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun prettySignal(signal: String): String = signal.trim().replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    companion object {
        private const val PREFS = "ravenos_office_bar_v1"
        private const val KEY_SIGNAL = "signal"
        private const val KEY_DETAIL = "detail"
        private const val KEY_MANUAL_OWNER = "manual_owner"
        private const val KEY_QUIET = "quiet"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_EXPLICIT_DISABLED = "explicit_disabled"
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
        const val ACTION_ENABLE = "com.ravenos.launcher.office.ENABLE"
        const val ACTION_DISABLE = "com.ravenos.launcher.office.DISABLE"
        const val ACTION_RESTORE = "com.ravenos.launcher.office.RESTORE"
        const val ACTION_HAUNT_CYCLE = "com.ravenos.launcher.office.HAUNT_CYCLE"

        fun isEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

        fun signal(context: Context, signal: String, detail: String = "") {
            val mode = RavenHauntModeStore.get(context)
            val normalizedSignal = signal.trim().uppercase()
            when (normalizedSignal) {
                "FOREGROUND_APP" -> if (!mode.foregroundRouting) return
                "NOTIFICATION" -> if (!mode.notificationRouting) return
            }
            val enriched = enrichDetail(context, normalizedSignal, detail)
            if (!RavenOfficeGovernor.accept(context, normalizedSignal, enriched, mode)) return
            start(context, Intent(context, RavenOfficeBarService::class.java)
                .setAction(ACTION_SIGNAL).putExtra(EXTRA_SIGNAL, signal).putExtra(EXTRA_DETAIL, enriched))
        }

        fun pin(context: Context, owner: String) = start(context, Intent(context, RavenOfficeBarService::class.java)
            .setAction(ACTION_PIN).putExtra(EXTRA_OWNER, owner))
        fun next(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_NEXT))
        fun auto(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_AUTO))
        fun toggleQuiet(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_QUIET))
        fun enable(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_ENABLE))
        fun disable(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_DISABLE))
        fun cycleHaunt(context: Context) = start(context, Intent(context, RavenOfficeBarService::class.java).setAction(ACTION_HAUNT_CYCLE))
        fun setHaunt(context: Context, mode: RavenHauntMode) {
            RavenHauntModeStore.set(context, mode)
            enable(context)
            signal(context, "SYSTEM_DECK", "haunt:${mode.label}")
        }
        fun restore(context: Context, reason: String) = start(context, Intent(context, RavenOfficeBarService::class.java)
            .setAction(ACTION_RESTORE).putExtra(EXTRA_DETAIL, reason))

        private fun enrichDetail(context: Context, signal: String, detail: String): String {
            if (signal != "APP_LAUNCH" && signal != "FOREGROUND_APP" && signal != "NOTIFICATION") return detail
            val marker = "package:"
            val start = detail.indexOf(marker)
            if (start < 0) return detail
            val pkg = detail.substring(start + marker.length).substringBefore('|').trim()
            if (pkg.isBlank()) return detail
            return try {
                val info = context.packageManager.getApplicationInfo(pkg, 0)
                val label = context.packageManager.getApplicationLabel(info).toString().trim()
                if (label.isBlank()) detail else "app:${label.take(80)}|package:$pkg"
            } catch (_: Throwable) { detail }
        }

        private fun start(context: Context, intent: Intent) {
            try { ContextCompat.startForegroundService(context, intent) }
            catch (_: Throwable) { /* Launcher remains usable if Android refuses an FGS start. */ }
        }
    }
}
