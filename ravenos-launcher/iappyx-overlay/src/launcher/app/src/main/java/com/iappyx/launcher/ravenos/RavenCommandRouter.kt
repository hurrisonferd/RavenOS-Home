package com.iappyx.launcher.ravenos

import android.app.Activity
import android.content.Context
import android.media.AudioManager

/** Deterministic Raven Search command grammar; exact/local commands run before any AI path. */
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

        if (normalized.startsWith("haunt ")) {
            val requested = normalized.removePrefix("haunt ").trim()
            if (requested == "next" || requested == "cycle") {
                val next = RavenHauntModeStore.cycle(context)
                RavenOfficeBarService.setHaunt(context, next)
                return Result(true, "haunt ${next.label}")
            }
            RavenHauntModeStore.parse(requested)?.let { mode ->
                RavenOfficeBarService.setHaunt(context, mode)
                return Result(true, "haunt ${mode.label}")
            }
        }

        return when (normalized) {
            "haunt", "haunt next", "next haunt" -> {
                val next = RavenHauntModeStore.cycle(context)
                RavenOfficeBarService.setHaunt(context, next)
                Result(true, "haunt ${next.label}")
            }
            "haunt status", "haunting status" -> Result(true, "haunt ${RavenHauntModeStore.get(context).label}")
            "normal", "ringer normal" -> Result(RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_NORMAL), "ringer normal")
            "vibrate", "ringer vibrate" -> Result(RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_VIBRATE), "ringer vibrate")
            "silent", "ringer silent" -> Result(RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_SILENT), "ringer silent")
            "office next", "next office", "next member" -> { RavenOfficeBarService.next(context); Result(true, "office next") }
            "office auto", "auto office" -> { RavenOfficeBarService.auto(context); Result(true, "office auto") }
            "office quiet", "quiet office", "quiet" -> { RavenOfficeBarService.toggleQuiet(context); Result(true, "office quiet") }
            "office wake", "wake office", "wake bar" -> {
                RavenOfficeBarService.enable(context)
                RavenOfficeBarService.signal(context, "SEARCH", "command:office-wake")
                Result(true, "office wake")
            }
            "office sleep", "sleep office", "sleep bar" -> { RavenOfficeBarService.disable(context); Result(true, "office sleep") }
            "office trace", "trace office", "routing trace" -> Result(true, RavenOfficeTraceStore.compact(context, 8))
            "clear office trace", "office trace clear" -> { RavenOfficeTraceStore.clear(context); Result(true, "office trace cleared") }
            "office cadence", "cadence office", "office governor" -> Result(true, RavenOfficeGovernor.compact(context))
            "clear office cadence", "office cadence clear", "clear office governor" -> {
                RavenOfficeGovernor.clearStats(context); Result(true, "office cadence stats cleared")
            }
            "office integrity", "surface integrity", "office surfaces" -> Result(true, RavenSurfaceIntegrity.compact(context))
            "clear office integrity", "office integrity clear" -> {
                RavenSurfaceIntegrity.clear(context); Result(true, "office surface integrity cleared")
            }
            "follow me", "follow me on", "overlay on", "office overlay" -> {
                val enabled = RavenFollowMeOverlay.enable(context)
                if (enabled) {
                    RavenOfficeBarService.enable(context)
                    RavenOfficeBarService.signal(context, "SEARCH", "command:follow-me-on")
                    Result(true, "follow-me on")
                } else {
                    val activity = context as? Activity
                    if (activity != null) {
                        RavenPermissionDeck.openOverlayAccess(activity)
                        Result(true, "overlay permission requested")
                    } else Result(false)
                }
            }
            "follow me off", "overlay off", "hide office" -> {
                RavenFollowMeOverlay.disable(context)
                RavenOfficeBarService.signal(context, "SEARCH", "command:follow-me-off")
                Result(true, "follow-me off")
            }
            "raven status", "launcher status", "awareness status" -> {
                val readiness = RavenAwarenessStatus.snapshot(context).compact()
                val haunt = RavenHauntModeStore.get(context).label
                Result(true, "$readiness · HAUNT=$haunt")
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
            val match = Regex("^${Regex.escape(label)}(?:\\s+|\\s*=\\s*)(\\d{1,3})%?$").matchEntire(input) ?: continue
            return match.groupValues[1].toIntOrNull()?.coerceIn(0, 100)
        }
        return null
    }
}
