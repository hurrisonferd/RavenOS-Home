package com.iappyx.launcher.ravenos

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper

/**
 * Cheap deterministic heartbeat for coarse phone state plus Android media-session semantics.
 * No screen pixels, keystrokes, microphone, or network content are read here.
 */
object RavenPhonePulseMonitor {
    private val handler = Handler(Looper.getMainLooper())
    private var app: Context? = null
    private var last: Snapshot? = null
    private var running = false

    data class Snapshot(
        val music: Boolean,
        val media: Int,
        val ring: Int,
        val alarm: Int,
        val ringer: Int,
        val battery: Int,
        val charging: Boolean,
        val mediaSession: RavenMediaSessionSenseOS.Snapshot,
    )

    private val tick = object : Runnable {
        override fun run() {
            val context = app ?: return
            if (!running) return
            val now = snapshot(context)
            val before = last
            last = now
            if (before != null) emitChanges(context, before, now)
            handler.postDelayed(this, interval(RavenHauntModeStore.get(context)))
        }
    }

    @Synchronized
    fun start(context: Context) {
        if (running) return
        app = context.applicationContext
        last = snapshot(context.applicationContext)
        running = true
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 1200L)
    }

    @Synchronized
    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        app = null
        last = null
    }

    fun compact(context: Context): String {
        val s = snapshot(context.applicationContext)
        return buildString {
            append("MUSIC=").append(if (s.music) "ON" else "OFF")
            append(" · MEDIA=").append(s.media).append('%')
            append(" · RING=").append(s.ring).append('%')
            append(" · ALARM=").append(s.alarm).append('%')
            append(" · BAT=").append(s.battery).append('%')
            if (s.charging) append('⚡')
            append("\n").append(s.mediaSession.compact())
        }
    }

    private fun emitChanges(context: Context, old: Snapshot, new: Snapshot) {
        if (old.music != new.music) {
            RavenOfficeBarService.signal(context, "MUSIC", if (new.music) "music:active" else "music:inactive")
        }
        if (old.media != new.media || old.ring != new.ring || old.alarm != new.alarm || old.ringer != new.ringer) {
            RavenOfficeBarService.signal(
                context,
                "AUDIO",
                "media:${new.media}|ring:${new.ring}|alarm:${new.alarm}|ringer:${ringerName(new.ringer)}",
            )
        }
        if (old.charging != new.charging) {
            RavenOfficeBarService.signal(context, "POWER", if (new.charging) "charging:${new.battery}%" else "unplugged:${new.battery}%")
        }
        val crossedLow = old.battery >= 20 && new.battery in 0..19
        val recoveredLow = old.battery in 0..19 && new.battery >= 20
        if (crossedLow || recoveredLow) {
            RavenOfficeBarService.signal(context, "BATTERY", "${if (crossedLow) "low" else "recovered"}:${new.battery}%")
        }

        val oldSession = old.mediaSession
        val newSession = new.mediaSession
        if (newSession.available && oldSession.semanticKey() != newSession.semanticKey()) {
            RavenOfficeBarService.signal(context, "MEDIA_SESSION", RavenMediaSessionSenseOS.signalDetail(newSession))
        }
    }

    private fun snapshot(context: Context): Snapshot {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val batteryIntent = try { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) } catch (_: Throwable) { null }
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val battery = if (level < 0 || scale <= 0) -1 else ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return Snapshot(
            music = audio.isMusicActive,
            media = percent(audio, AudioManager.STREAM_MUSIC),
            ring = percent(audio, AudioManager.STREAM_RING),
            alarm = percent(audio, AudioManager.STREAM_ALARM),
            ringer = audio.ringerMode,
            battery = battery,
            charging = charging,
            mediaSession = RavenMediaSessionSenseOS.snapshot(context),
        )
    }

    private fun percent(audio: AudioManager, stream: Int): Int = try {
        val max = audio.getStreamMaxVolume(stream).coerceAtLeast(1)
        ((audio.getStreamVolume(stream) * 100f) / max).toInt().coerceIn(0, 100)
    } catch (_: Throwable) { -1 }

    private fun ringerName(mode: Int): String = when (mode) {
        AudioManager.RINGER_MODE_SILENT -> "silent"
        AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
        else -> "normal"
    }

    private fun interval(mode: RavenHauntMode): Long = when (mode) {
        RavenHauntMode.CALM -> 15_000L
        RavenHauntMode.LIVED_IN -> 8_000L
        RavenHauntMode.HAUNTED -> 5_000L
        RavenHauntMode.FERAL -> 3_000L
        RavenHauntMode.APOCALYPSE -> 2_000L
    }
}
