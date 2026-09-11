package com.iappyx.launcher.ravenos

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

/**
 * Native, deterministic System Deck plumbing inspired by utility-first launchers.
 * No AI/provider dependency: audio control remains useful with every network/provider offline.
 */
object RavenSystemDeck {
    data class StreamState(val current: Int, val max: Int) {
        val percent: Int get() = if (max <= 0) 0 else ((current.toDouble() / max) * 100.0).roundToInt().coerceIn(0, 100)
    }

    data class AudioState(
        val media: StreamState,
        val ring: StreamState,
        val alarm: StreamState,
        val notification: StreamState,
        val ringerMode: Int,
    )

    fun snapshot(context: Context): AudioState {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return AudioState(
            media = stream(audio, AudioManager.STREAM_MUSIC),
            ring = stream(audio, AudioManager.STREAM_RING),
            alarm = stream(audio, AudioManager.STREAM_ALARM),
            notification = stream(audio, AudioManager.STREAM_NOTIFICATION),
            ringerMode = audio.ringerMode,
        )
    }

    fun setMediaPercent(context: Context, percent: Int) = setPercent(context, AudioManager.STREAM_MUSIC, percent, "media")
    fun setRingPercent(context: Context, percent: Int) = setPercent(context, AudioManager.STREAM_RING, percent, "ring")
    fun setAlarmPercent(context: Context, percent: Int) = setPercent(context, AudioManager.STREAM_ALARM, percent, "alarm")
    fun setNotificationPercent(context: Context, percent: Int) = setPercent(context, AudioManager.STREAM_NOTIFICATION, percent, "notification")

    fun setRingerMode(context: Context, mode: Int): Boolean {
        if (mode != AudioManager.RINGER_MODE_NORMAL &&
            mode != AudioManager.RINGER_MODE_VIBRATE &&
            mode != AudioManager.RINGER_MODE_SILENT) return false
        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.ringerMode = mode
            RavenOfficeBarService.signal(context, "SYSTEM_DECK", "ringer-mode:$mode")
            true
        } catch (_: Throwable) { false }
    }

    private fun setPercent(context: Context, stream: Int, percent: Int, label: String): Boolean {
        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audio.getStreamMaxVolume(stream)
            val value = ((percent.coerceIn(0, 100) / 100.0) * max).roundToInt().coerceIn(0, max)
            audio.setStreamVolume(stream, value, 0)
            RavenOfficeBarService.signal(context, "SYSTEM_DECK", "$label:${percent.coerceIn(0, 100)}%")
            true
        } catch (_: Throwable) { false }
    }

    private fun stream(audio: AudioManager, stream: Int): StreamState = StreamState(
        current = audio.getStreamVolume(stream),
        max = audio.getStreamMaxVolume(stream),
    )
}
