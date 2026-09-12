package com.iappyx.launcher.ravenos

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.iappyx.launcher.notify.NotificationBadgeListener

/** Reads Android's active media-session state when notification-listener access permits it. */
object RavenMediaSessionSenseOS {
    data class Snapshot(
        val available: Boolean,
        val packageName: String,
        val state: Int,
        val title: String,
        val artist: String,
        val album: String,
        val positionMs: Long,
        val durationMs: Long,
    ) {
        val playing: Boolean get() = state == PlaybackState.STATE_PLAYING
        val stateName: String get() = when (state) {
            PlaybackState.STATE_PLAYING -> "PLAYING"
            PlaybackState.STATE_PAUSED -> "PAUSED"
            PlaybackState.STATE_STOPPED -> "STOPPED"
            PlaybackState.STATE_BUFFERING -> "BUFFERING"
            PlaybackState.STATE_FAST_FORWARDING -> "FAST_FORWARDING"
            PlaybackState.STATE_REWINDING -> "REWINDING"
            PlaybackState.STATE_SKIPPING_TO_NEXT -> "SKIP_NEXT"
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> "SKIP_PREVIOUS"
            PlaybackState.STATE_CONNECTING -> "CONNECTING"
            PlaybackState.STATE_ERROR -> "ERROR"
            else -> "NONE"
        }

        fun semanticKey(): String = listOf(packageName, stateName, title, artist, durationMs).joinToString("|")
        fun compact(): String = if (!available) "MEDIA SESSION OFFLINE" else buildString {
            append(stateName)
            if (packageName.isNotBlank()) append(" · ").append(packageName.substringAfterLast('.'))
            if (title.isNotBlank()) append(" · ").append(title.take(44))
            if (artist.isNotBlank()) append(" · ").append(artist.take(32))
        }
    }

    fun snapshot(context: Context): Snapshot {
        val manager = context.getSystemService(MediaSessionManager::class.java)
            ?: return offline()
        val component = ComponentName(context, NotificationBadgeListener::class.java)
        val controllers = try { manager.getActiveSessions(component) } catch (_: Throwable) { return offline() }
        if (controllers.isEmpty()) return Snapshot(true, "", PlaybackState.STATE_NONE, "", "", "", 0L, 0L)

        val controller = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.first()
        return fromController(controller)
    }

    fun signalDetail(snapshot: Snapshot): String = buildString {
        append("package:").append(snapshot.packageName)
        append("|state:").append(snapshot.stateName)
        if (snapshot.title.isNotBlank()) append("|title:").append(clean(snapshot.title, 100))
        if (snapshot.artist.isNotBlank()) append("|artist:").append(clean(snapshot.artist, 80))
        if (snapshot.album.isNotBlank()) append("|album:").append(clean(snapshot.album, 80))
        append("|position_ms:").append(snapshot.positionMs.coerceAtLeast(0L))
        append("|duration_ms:").append(snapshot.durationMs.coerceAtLeast(0L))
    }

    private fun fromController(controller: MediaController): Snapshot {
        val metadata = controller.metadata
        val playback = controller.playbackState
        val title = text(metadata, MediaMetadata.METADATA_KEY_TITLE)
            .ifBlank { text(metadata, MediaMetadata.METADATA_KEY_DISPLAY_TITLE) }
        val artist = text(metadata, MediaMetadata.METADATA_KEY_ARTIST)
            .ifBlank { text(metadata, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) }
        val album = text(metadata, MediaMetadata.METADATA_KEY_ALBUM)
        return Snapshot(
            available = true,
            packageName = controller.packageName.orEmpty(),
            state = playback?.state ?: PlaybackState.STATE_NONE,
            title = title,
            artist = artist,
            album = album,
            positionMs = playback?.position ?: 0L,
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
        )
    }

    private fun text(metadata: MediaMetadata?, key: String): String =
        metadata?.getText(key)?.toString()?.trim().orEmpty()

    private fun offline() = Snapshot(false, "", PlaybackState.STATE_NONE, "", "", "", 0L, 0L)

    private fun clean(raw: String, max: Int): String = raw.replace('|', ' ').replace('\n', ' ').trim().take(max)
}
