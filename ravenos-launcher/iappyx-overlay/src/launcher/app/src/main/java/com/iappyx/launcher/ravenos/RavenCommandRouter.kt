package com.iappyx.launcher.ravenos

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import com.iappyx.launcher.LauncherActivity

/** Deterministic Raven Search command grammar; exact/local commands run before any AI path. */
object RavenCommandRouter {
    data class Result(val handled: Boolean, val message: String = "")

    fun execute(context: Context, raw: String): Result {
        val q = raw.trim()
        if (q.isBlank()) return Result(false)
        val normalized = q.lowercase().replace(Regex("\\s+"), " ")

        parsePercent(normalized, "media", "volume")?.let { return Result(RavenSystemDeck.setMediaPercent(context, it), "media $it%") }
        parsePercent(normalized, "ring", "ringer")?.let { return Result(RavenSystemDeck.setRingPercent(context, it), "ring $it%") }
        parsePercent(normalized, "alarm")?.let { return Result(RavenSystemDeck.setAlarmPercent(context, it), "alarm $it%") }
        parsePercent(normalized, "notification", "notifications")?.let { return Result(RavenSystemDeck.setNotificationPercent(context, it), "notifications $it%") }

        parseGestureAssignment(normalized)?.let { (direction, target) ->
            RavenGesturePrefs.set(context, direction, target)
            return Result(true, "gesture ${direction.name.lowercase()} → ${target.label}")
        }

        if (normalized.startsWith("haunt ")) {
            val requested = normalized.removePrefix("haunt ").trim()
            if (requested == "next" || requested == "cycle") {
                val next = RavenHauntModeStore.cycle(context)
                RavenGoblinEngineOS.setHaunt(context, next)
                return Result(true, "haunt ${next.label}")
            }
            RavenHauntModeStore.parse(requested)?.let { mode ->
                RavenGoblinEngineOS.setHaunt(context, mode)
                return Result(true, "haunt ${mode.label}")
            }
        }

        if (normalized.startsWith("recall ") || normalized.startsWith("office recall ")) {
            val query = if (normalized.startsWith("office recall ")) q.substringAfter("office recall ", "") else q.substringAfter("recall ", "")
            return Result(true, RavenOfficeRecallOS.searchBlocking(context, query))
        }

        return when (normalized) {
            "why", "why?", "goblin why", "office why" -> Result(true, RavenEvidenceBoard.why(context))
            "evidence", "evidence board", "goblin evidence" -> Result(true, RavenEvidenceBoard.compact(context, 10))
            "clear evidence", "clear evidence board" -> { RavenEvidenceBoard.clear(context); Result(true, "evidence board cleared") }
            "save that shit", "save this", "bookmark moment", "pin current bit" -> Result(true, RavenReplayOS.saveCurrent(context))
            "replay", "replay os", "bookmarks" -> Result(true, RavenReplayOS.compact(context))
            "recall", "office recall", "recall help" -> Result(true, RavenOfficeRecallOS.searchBlocking(context, ""))
            "dialogue vault", "vault status", "writers room" -> Result(true, RavenDialogueVaultOS.compact(context))
            "scene graph", "screen graph", "what do you see" -> Result(true, RavenSceneGraphOS.compact(context))
            "clear dialogue usage", "clear writers room usage" -> {
                RavenDialogueVaultOS.clearUsage(context)
                RavenSceneGraphOS.clear()
                Result(true, "dialogue fingerprints, expression usage, and structural scene history cleared")
            }
            "next drop", "goblin next drop" -> {
                RavenGoblinEngineOS.signal(context, "NEXT_DROP", "owner:command")
                Result(true, "next drop")
            }
            "goblin status", "goblin brain", "brain status" -> {
                val why = RavenEvidenceBoard.why(context).replace('\n', ' ')
                Result(true, "GOBLIN BRAIN ACTIVE · $why · ${RavenDialogueVaultOS.compact(context)} · ${RavenOfficeRecallOS.compact(context)}")
            }
            "goblin modules", "brain modules", "engine status", "module rack", "goblin engine" ->
                Result(true, RavenGoblinEngineOS.status(context))
            "quick deck", "quick controls", "controls", "sound controls" -> {
                val activity = context as? LauncherActivity ?: return Result(false)
                RavenQuickControls.show(activity)
                Result(true, "Quick Deck")
            }
            "menu", "raven menu", "launcher menu" -> {
                val activity = context as? LauncherActivity ?: return Result(false)
                RavenMenu.open(activity)
                Result(true, "Raven Menu")
            }
            "gestures", "gesture status", "gesture controls", "swipe controls" -> Result(true, RavenGesturePrefs.summary(context))
            "gestures off", "gesture off", "disable gestures", "disable vertical gestures" -> {
                RavenGesturePrefs.set(context, RavenGestureDirection.UP, RavenGestureTarget.NONE)
                RavenGesturePrefs.set(context, RavenGestureDirection.DOWN, RavenGestureTarget.NONE)
                Result(true, RavenGesturePrefs.summary(context))
            }
            "gesture reset", "gestures reset", "reset gestures" -> {
                RavenGesturePrefs.reset(context)
                Result(true, RavenGesturePrefs.summary(context))
            }
            "haunt", "haunt next", "next haunt" -> {
                val next = RavenHauntModeStore.cycle(context)
                RavenGoblinEngineOS.setHaunt(context, next)
                Result(true, "haunt ${next.label}")
            }
            "haunt status", "haunting status" -> Result(true, "haunt ${RavenHauntModeStore.get(context).label}")
            "normal", "ringer normal" -> Result(RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_NORMAL), "ringer normal")
            "vibrate", "ringer vibrate" -> Result(RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_VIBRATE), "ringer vibrate")
            "silent", "ringer silent" -> Result(RavenSystemDeck.setRingerMode(context, AudioManager.RINGER_MODE_SILENT), "ringer silent")
            "office next", "next office", "next member" -> { RavenGoblinEngineOS.next(context); Result(true, "office next") }
            "office auto", "auto office" -> { RavenGoblinEngineOS.auto(context); Result(true, "office auto") }
            "office quiet", "quiet office", "quiet" -> { RavenGoblinEngineOS.toggleQuiet(context); Result(true, "office quiet") }
            "office wake", "wake office", "wake bar" -> {
                RavenGoblinEngineOS.enable(context)
                RavenGoblinEngineOS.signal(context, "SEARCH", "command:office-wake")
                Result(true, "office wake")
            }
            "office sleep", "sleep office", "sleep bar" -> { RavenGoblinEngineOS.disable(context); Result(true, "office sleep") }
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
                    RavenGoblinEngineOS.enable(context)
                    RavenGoblinEngineOS.signal(context, "SEARCH", "command:follow-me-on")
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
                RavenGoblinEngineOS.signal(context, "SEARCH", "command:follow-me-off")
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
                        RavenGoblinEngineOS.pin(context, member.id)
                        Result(true, "office ${member.id}")
                    } else Result(false)
                } else Result(false)
            }
        }
    }

    private fun parseGestureAssignment(input: String): Pair<RavenGestureDirection, RavenGestureTarget>? {
        val match = Regex("^(?:gesture|swipe)\\s+(up|down)\\s+(apps?|search|menu|none|off)$").matchEntire(input) ?: return null
        val direction = if (match.groupValues[1] == "up") RavenGestureDirection.UP else RavenGestureDirection.DOWN
        val target = when (match.groupValues[2]) {
            "app", "apps" -> RavenGestureTarget.APPS
            "search" -> RavenGestureTarget.SEARCH
            "menu" -> RavenGestureTarget.MENU
            "none", "off" -> RavenGestureTarget.NONE
            else -> return null
        }
        return direction to target
    }

    private fun parsePercent(input: String, vararg labels: String): Int? {
        for (label in labels) {
            val match = Regex("^${Regex.escape(label)}(?:\\s+|\\s*=\\s*)(\\d{1,3})%?$").matchEntire(input) ?: continue
            return match.groupValues[1].toIntOrNull()?.coerceIn(0, 100)
        }
        return null
    }
}
