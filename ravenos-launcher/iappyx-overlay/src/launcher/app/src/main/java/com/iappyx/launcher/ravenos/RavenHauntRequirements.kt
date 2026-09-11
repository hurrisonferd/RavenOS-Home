package com.iappyx.launcher.ravenos

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import java.time.LocalTime

/**
 * Cheap, deterministic context requirements for RavenOS ecology.
 *
 * Requirements never grant permissions and never mutate launcher layout directly. They surface
 * bounded context hints and may emit an existing Office signal after a cooldown.
 */
object RavenHauntRequirements {
    data class Match(
        val id: String,
        val owner: String,
        val signal: String,
        val detail: String,
        val title: String,
        val body: String,
        val priority: Int,
        val ttlMs: Long,
    )

    private const val PREFS = "ravenos_haunt_requirements_v1"
    private const val KEY_SIGNATURE = "last_signal_signature"
    private const val KEY_AT = "last_signal_at"

    fun evaluate(context: Context): List<Match> {
        val battery = batterySnapshot(context)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val hour = LocalTime.now().hour
        val mode = RavenHauntModeStore.get(context)
        val out = mutableListOf<Match>()

        if (battery.percent in 0..19 && !battery.charging) {
            out += Match(
                id = "battery-low",
                owner = "EDISON",
                signal = "BATTERY",
                detail = "low:${battery.percent}%",
                title = "🔧 POWER FLOOR",
                body = "Battery ${battery.percent}% · preserve the next useful move.",
                priority = 100,
                ttlMs = 10 * 60_000L,
            )
        }
        if (audio.isMusicActive) {
            out += Match(
                id = "music-active",
                owner = "YORI",
                signal = "MUSIC",
                detail = "music:active",
                title = "🪐 MUSIC FIELD",
                body = "Audio is active · let the surface rotate toward motion.",
                priority = 82,
                ttlMs = 90_000L,
            )
        }
        if (hour >= 23 || hour < 6) {
            out += Match(
                id = "night-watch",
                owner = "NYX",
                signal = "NIGHT",
                detail = "clock:$hour",
                title = "🌙 NIGHT WATCH",
                body = "Late-hour ecology · quiet signals get more weight.",
                priority = 74,
                ttlMs = 30 * 60_000L,
            )
        }
        if (battery.charging && battery.percent < 90) {
            out += Match(
                id = "charging",
                owner = "LUMA",
                signal = "POWER",
                detail = "charging:${battery.percent}%",
                title = "🤍 RECOVERY FIELD",
                body = "Charging ${battery.percent}% · restore before expanding.",
                priority = 58,
                ttlMs = 10 * 60_000L,
            )
        }
        if (mode == RavenHauntMode.FERAL || mode == RavenHauntMode.APOCALYPSE) {
            out += Match(
                id = "high-haunt",
                owner = "KYU",
                signal = "HOME",
                detail = "haunt:${mode.label}",
                title = "💗 GOBLIN PRESSURE",
                body = "${mode.label} is active · more surfaces may volunteer themselves.",
                priority = if (mode == RavenHauntMode.APOCALYPSE) 52 else 42,
                ttlMs = 5 * 60_000L,
            )
        }

        return out.sortedByDescending(Match::priority)
    }

    fun top(context: Context): Match? = evaluate(context).firstOrNull()

    fun evaluateAndSignal(context: Context) {
        val match = top(context) ?: return
        if (match.priority < 58) return
        val now = System.currentTimeMillis()
        val signature = "${match.id}|${match.detail}"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_SIGNATURE, null)
        val at = prefs.getLong(KEY_AT, 0L)
        val minGap = minOf(match.ttlMs, 90_000L)
        if (previous == signature && now - at < minGap) return

        prefs.edit().putString(KEY_SIGNATURE, signature).putLong(KEY_AT, now).apply()
        RavenTaskerBridge.emit(context, "requirement", "$signature|owner:${match.owner}")
        RavenOfficeBarService.signal(context, match.signal, "requirement:${match.detail}")
    }

    private fun batterySnapshot(context: Context): BatterySnapshot {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val percent = if (level < 0 || scale <= 0) -1 else ((level * 100f) / scale).toInt().coerceIn(0, 100)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            BatterySnapshot(percent, charging)
        } catch (_: Throwable) {
            BatterySnapshot(-1, false)
        }
    }

    private data class BatterySnapshot(val percent: Int, val charging: Boolean)
}
