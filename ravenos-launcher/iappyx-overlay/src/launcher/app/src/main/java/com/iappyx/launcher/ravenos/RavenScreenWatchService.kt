package com.iappyx.launcher.ravenos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.iappyx.launcher.R
import kotlin.math.abs

/**
 * Owner-armed whole-screen Goblin Eye.
 *
 * Raw frames stay in memory and are immediately discarded. The default analyzer emits only coarse
 * deterministic visual deltas (motion/brightness/color/hash). No OCR, cloud upload, or model call.
 */
class RavenScreenWatchService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    private var worker: Handler? = null
    private var lastSample: IntArray? = null
    private var lastHash: Long = 0L
    private var lastAnalyzeAt = 0L
    private var lastEmitAt = 0L
    private var ignoreFrames = 0
    private var stopping = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            // Android may invoke this synchronously while our own stop() is already unwinding.
            // A single guard prevents recursive cleanup and duplicate stop commentary.
            if (stopping) return
            stopInternal(explicit = false, stopProjection = false, systemStop = true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopInternal(explicit = true)
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val code = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
                val data = intent.intentExtra(EXTRA_RESULT_DATA) ?: return START_NOT_STICKY
                if (code == Int.MIN_VALUE) return START_NOT_STICKY
                if (projection != null) return START_STICKY
                stopping = false
                startProjection(code, data)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopInternal(explicit = false)
        super.onDestroy()
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        ensureChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val manager = getSystemService(MediaProjectionManager::class.java) ?: run {
            stopInternal(explicit = false)
            return
        }
        val p = try { manager.getMediaProjection(resultCode, data) } catch (_: Throwable) { null } ?: run {
            stopInternal(explicit = false)
            return
        }
        projection = p
        p.registerCallback(projectionCallback, Handler(mainLooper))

        val metrics = resources.displayMetrics
        val width = 240
        val height = ((metrics.heightPixels.toFloat() / metrics.widthPixels.coerceAtLeast(1)) * width)
            .toInt().coerceIn(240, 560)
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader = imageReader
        val thread = HandlerThread("RavenGoblinEye").also { it.start() }
        workerThread = thread
        val handler = Handler(thread.looper)
        worker = handler
        imageReader.setOnImageAvailableListener({ source ->
            val image = try { source.acquireLatestImage() } catch (_: Throwable) { null } ?: return@setOnImageAvailableListener
            try { analyze(image) } finally { image.close() }
        }, handler)

        virtualDisplay = try {
            p.createVirtualDisplay(
                "RavenOS-GoblinEye",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler,
            )
        } catch (_: Throwable) {
            null
        }
        if (virtualDisplay == null) {
            stopInternal(explicit = false)
            return
        }
        setActive(this, true)
        RavenOfficeBarService.signal(this, "SCREEN_VISUAL", "state:armed|size:${width}x$height|raw_persist:false|cloud:false")
    }

    private fun analyze(image: Image) {
        if (stopping) return
        val now = System.currentTimeMillis()
        val interval = when (RavenHauntModeStore.get(this)) {
            RavenHauntMode.CALM -> 3000L
            RavenHauntMode.LIVED_IN -> 2200L
            RavenHauntMode.HAUNTED -> 1500L
            RavenHauntMode.FERAL -> 1000L
            RavenHauntMode.APOCALYPSE -> 750L
        }
        if (now - lastAnalyzeAt < interval) return
        lastAnalyzeAt = now
        if (ignoreFrames > 0) {
            ignoreFrames--
            return
        }
        val plane = image.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride <= 0 || rowStride <= 0) return

        val grid = 16
        val samples = IntArray(grid * grid)
        var sumLum = 0L
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var idx = 0
        for (gy in 0 until grid) {
            val y = ((gy + 0.5f) * image.height / grid).toInt().coerceIn(0, image.height - 1)
            for (gx in 0 until grid) {
                val x = ((gx + 0.5f) * image.width / grid).toInt().coerceIn(0, image.width - 1)
                val offset = y * rowStride + x * pixelStride
                if (offset + 2 >= buffer.limit()) continue
                val r = buffer.get(offset).toInt() and 0xFF
                val g = buffer.get(offset + 1).toInt() and 0xFF
                val b = buffer.get(offset + 2).toInt() and 0xFF
                val lum = (r * 299 + g * 587 + b * 114) / 1000
                samples[idx++] = lum
                sumLum += lum
                sumR += r
                sumG += g
                sumB += b
            }
        }
        if (idx < 32) return
        val avg = (sumLum / idx).toInt()
        val rgb = Triple((sumR / idx).toInt(), (sumG / idx).toInt(), (sumB / idx).toInt())
        val hash = perceptualHash(samples, idx, avg)
        val prior = lastSample
        var changed = 0
        var deltaSum = 0L
        if (prior != null) {
            val n = minOf(prior.size, samples.size, idx)
            for (i in 0 until n) {
                val d = abs(samples[i] - prior[i])
                deltaSum += d
                if (d >= 26) changed++
            }
            val motion = changed * 100 / n.coerceAtLeast(1)
            val delta = (deltaSum / n.coerceAtLeast(1)).toInt()
            val hashDistance = java.lang.Long.bitCount(hash xor lastHash)
            val meaningful = motion >= 18 || delta >= 20 || hashDistance >= 22
            if (meaningful && now - lastEmitAt >= 1200L) {
                lastEmitAt = now
                ignoreFrames = 2 // reduce feedback from RavenOS's own next overlay repaint
                RavenOfficeBarService.signal(
                    this,
                    "SCREEN_VISUAL",
                    "state:changed|motion:$motion|delta:$delta|hash_distance:$hashDistance|luma:$avg|rgb:${hex(rgb)}|raw_persist:false|cloud:false",
                )
            }
        }
        lastSample = samples
        lastHash = hash
    }

    private fun perceptualHash(samples: IntArray, count: Int, average: Int): Long {
        var hash = 0L
        val stride = (count / 64).coerceAtLeast(1)
        var bit = 0
        var i = 0
        while (bit < 64 && i < count) {
            if (samples[i] >= average) hash = hash or (1L shl bit)
            bit++
            i += stride
        }
        return hash
    }

    private fun hex(rgb: Triple<Int, Int, Int>): String = "%02X%02X%02X".format(rgb.first, rgb.second, rgb.third)

    private fun buildNotification(): android.app.Notification {
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, RavenScreenWatchService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("👁 Goblin Eye armed")
            .setContentText("Local visual deltas only · raw frames are not persisted")
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "STOP EYE", stop)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "RavenOS Goblin Eye", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Owner-armed local screen-change awareness"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            },
        )
    }

    private fun stopInternal(
        explicit: Boolean,
        stopProjection: Boolean = true,
        systemStop: Boolean = false,
    ) {
        if (stopping) return
        stopping = true
        val wasActive = projection != null || isActive(this)
        try { reader?.setOnImageAvailableListener(null, null) } catch (_: Throwable) {}
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        virtualDisplay = null
        try { reader?.close() } catch (_: Throwable) {}
        reader = null
        try { projection?.unregisterCallback(projectionCallback) } catch (_: Throwable) {}
        if (stopProjection) {
            try { projection?.stop() } catch (_: Throwable) {}
        }
        projection = null
        try { workerThread?.quitSafely() } catch (_: Throwable) {}
        workerThread = null
        worker = null
        lastSample = null
        lastHash = 0L
        setActive(this, false)
        if (wasActive) {
            val state = when {
                explicit -> "stopped_by_raven"
                systemStop -> "stopped_by_system"
                else -> "stopped"
            }
            RavenOfficeBarService.signal(this, "SCREEN_VISUAL", "state:$state")
        }
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) {}
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun Intent.intentExtra(name: String): Intent? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(name, Intent::class.java)
    } else {
        getParcelableExtra(name)
    }

    companion object {
        private const val PREFS = "ravenos_screen_watch_v1"
        private const val KEY_ACTIVE = "active"
        const val CHANNEL_ID = "ravenos_goblin_eye"
        const val NOTIFICATION_ID = 0x474F42
        const val ACTION_START = "com.ravenos.launcher.screenwatch.START"
        const val ACTION_STOP = "com.ravenos.launcher.screenwatch.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        fun isActive(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACTIVE, false)

        private fun setActive(context: Context, active: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, active).apply()
        }

        fun stop(context: Context) {
            try {
                context.startService(Intent(context, RavenScreenWatchService::class.java).setAction(ACTION_STOP))
            } catch (_: Throwable) {}
        }
    }
}
