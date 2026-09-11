package com.iappyx.launcher.ravenos

import android.app.Activity
import android.content.Context
import android.media.AudioManager

/**
 * Deterministic Raven Search command grammar.
 *
 * Exact/local commands are consumed before any AI/provider path. Unknown text returns false
 * and remains ordinary universal search input.
 */
object RavenCommandRouter {
    data class Result(val handled: Boolean, val message: String = "")

    fun execute(context: Context, raw: String): Result {
        val q = raw.trim()
        if (q.isBlank()) return Result(false)
        val normalized = q.lowercase().replace(Regex("\\s+"), " ")

        parsePercent(normalized, "media", "volume")?.let {
            return Result(RavenSystemDeck.setMediaPercent(context, it), "media $it%")
        }
        parsePercent(normalized, "ring", "ringer")?.let {
            return Result(RavenSystemDeck.setRingPercent(context, it), "ring $it%")
        }
        parsePercent(normalized, "alarm")?.let {
            return Result(RavenSystemDeck.setAlarmPercent(context, it), "alarm $it%")
        }
        parsePercent(normalized, "notification", "notifications")?.let {
            return Result(RavenSystemDeck.setNotificationPercent(context, it), "notifications $it%")
        }

        return when (normalized) {
            "normal", "ringer normal" -> Result(
                RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_NORMAL),
                "ringer normal",
            )
            "vibrate", "ringer vibrate" -> Result(
                RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_VIBRATE),
                "ringer vibrate",
            )
            "silent", "ringer silent" -> Result(
                RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_SILENT),
                "ringer silent",
            )
            "office next", "next office", "next member" -> {
                RavenOfficeBarService.next(context)
                Result(true, "office next")
            }
            "office auto", "auto office" -> {
                RavenOfficeBarService.auto(context)
                Result(true, "office auto")
            }
            "office quiet", "quiet office", "quiet" -> {
                RavenOfficeBarService.toggleQuiet(context)
                Result(true, "office quiet")
            }
            "office wake", "wake office", "wake bar" -> {
                RavenOfficeBarService.enable(context)
                RavenOfficeBarService.signal(context, "SEARCH", "command:office-wake")
                Result(true, "office wake")
            }
            "office sleep", "sleep office", "sleep bar" -> {
                RavenOfficeBarService.disable(context)
                Result(true, "office sleep")
            }
            "raven status", "launcher status", "awareness status" -> {
                Result(true, RavenAwarenessStatus.snapshot(context).compact())
            }
            "default home", "launcher home", "make ravenos home" -> {
                val activity = context as? Activity ?: return Result(false)
                RavenPermissionDeck.requestDefaultHome(activity)
                Result(true, "default HOME requested")
            }
            "battery survival", "background survival" -> {
                val activity = context as? Activity ?: return Result(false)
                RavenPermissionDeck.openBatteryOptimization(activity)
                Result(true, "battery survival settings")
            }
            else -> {
                if (normalized.startsWith("office ")) {
                    val owner = normalized.removePrefix("office ").trim().uppercase()
                    val member = RavenOfficeRegistry.member(owner)
                    if (member?.routable == true) {
                        RavenOfficeBarService.pin(context, member.id)
                        Result(true, "office ${member.id}")
                    } else Result(false)
                } else Result(false)
            }
        }
    }

    private fun parsePercent(input: String, vararg labels: String): Int? {
        for (label in labels) {
            val match = Regex("^${Regex.escape(label)}(?:\\s+|\\s*=\\s*)(\\d{1,3})%?$").matchEntire(input)
                ?: continue
            return match.groupValues[1].toIntOrNull()?.coerceIn(0, 100)
        }
        return null
    }
}
