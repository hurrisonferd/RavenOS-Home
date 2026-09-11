package com.iappyx.launcher.ravenos

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Notification-awareness layer for RavenOS.
 *
 * Default SOURCE mode never routes title/body text. SEMANTIC may expose the title locally;
 * FULL_LOCAL may expose title/body locally. None of these modes upload data or grant effect authority.
 */
object RavenNotificationSenseOS {
    enum class PrivacyMode { SOURCE, SEMANTIC, FULL_LOCAL }

    private const val PREFS = "ravenos_notification_sense_v1"
    private const val KEY_MODE = "privacy_mode"
    private const val KEY_LAST_PACKAGE = "last_package"
    private const val KEY_LAST_AT = "last_at"
    private const val KEY_BURST = "burst"
    private const val BURST_MS = 12_000L

    fun mode(context: Context): PrivacyMode = runCatching {
        PrivacyMode.valueOf(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MODE, PrivacyMode.SOURCE.name) ?: PrivacyMode.SOURCE.name,
        )
    }.getOrDefault(PrivacyMode.SOURCE)

    fun setMode(context: Context, mode: PrivacyMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
        RavenOfficeBarService.signal(context, "SYSTEM_DECK", "notification-sense:${mode.name.lowercase()}")
    }

    fun cycle(context: Context): PrivacyMode {
        val values = PrivacyMode.entries
        val next = values[(mode(context).ordinal + 1) % values.size]
        setMode(context, next)
        return next
    }

    fun onPosted(
        context: Context,
        sbn: StatusBarNotification,
        rankingMap: NotificationListenerService.RankingMap?,
        interruptionFilter: Int,
    ) {
        if (!RavenHauntModeStore.get(context).notificationRouting) return
        val n = sbn.notification ?: return
        val ranking = NotificationListenerService.Ranking()
        val ranked = runCatching { rankingMap?.getRanking(sbn.key, ranking) == true }.getOrDefault(false)
        val importance = if (ranked) ranking.importance else NotificationManager.IMPORTANCE_UNSPECIFIED
        val conversation = ranked && ranking.isConversation
        val ambient = ranked && ranking.isAmbient
        val matchesFilter = !ranked || ranking.matchesInterruptionFilter()
        val channel = if (ranked) ranking.channel?.id.orEmpty() else ""
        val ongoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val category = n.category.orEmpty()
        val alerting = matchesFilter && !ambient && importance >= NotificationManager.IMPORTANCE_DEFAULT && !ongoing
        val burst = nextBurst(context, sbn.packageName, sbn.postTime)
        val privacy = mode(context)
        val extras = n.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val body = (
            extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            )?.toString().orEmpty().trim()

        val detail = buildString {
            append("package:").append(sbn.packageName)
            append("|state:posted")
            append("|category:").append(category.ifBlank { "none" })
            append("|importance:").append(importance)
            append("|conversation:").append(conversation)
            append("|ambient:").append(ambient)
            append("|matches_filter:").append(matchesFilter)
            append("|alerting:").append(alerting)
            append("|ongoing:").append(ongoing)
            append("|burst:").append(burst)
            append("|interruption_filter:").append(interruptionFilter)
            if (channel.isNotBlank()) append("|channel:").append(clean(channel, 64))
            when (privacy) {
                PrivacyMode.SOURCE -> Unit
                PrivacyMode.SEMANTIC -> if (title.isNotBlank()) append("|title:").append(clean(title, 100))
                PrivacyMode.FULL_LOCAL -> {
                    if (title.isNotBlank()) append("|title:").append(clean(title, 100))
                    if (body.isNotBlank()) append("|body:").append(clean(body, 180))
                }
            }
            append("|privacy:").append(privacy.name)
        }
        // Dedicated signal avoids the legacy app-label enricher truncating notification metadata.
        RavenOfficeBarService.signal(context, "NOTIFICATION_SENSE", detail)
    }

    fun onRemoved(context: Context, sbn: StatusBarNotification) {
        if (!RavenHauntModeStore.get(context).notificationRouting) return
        RavenOfficeBarService.signal(
            context,
            "NOTIFICATION_SENSE",
            "package:${sbn.packageName}|state:removed|privacy:${mode(context).name}",
        )
    }

    fun summary(context: Context): String = "NOTIFICATION SENSE ${mode(context).name}"

    private fun nextBurst(context: Context, pkg: String, at: Long): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastPkg = prefs.getString(KEY_LAST_PACKAGE, "") ?: ""
        val lastAt = prefs.getLong(KEY_LAST_AT, 0L)
        val old = prefs.getInt(KEY_BURST, 0)
        val burst = if (pkg == lastPkg && at - lastAt in 0..BURST_MS) old + 1 else 1
        prefs.edit().putString(KEY_LAST_PACKAGE, pkg).putLong(KEY_LAST_AT, at).putInt(KEY_BURST, burst).apply()
        return burst
    }

    private fun clean(raw: String, max: Int): String = raw
        .replace('|', ' ')
        .replace('\n', ' ')
        .replace('\r', ' ')
        .trim()
        .take(max)
}
