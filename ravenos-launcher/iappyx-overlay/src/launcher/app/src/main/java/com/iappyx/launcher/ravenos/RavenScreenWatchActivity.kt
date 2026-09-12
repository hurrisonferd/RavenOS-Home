package com.iappyx.launcher.ravenos

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.core.content.ContextCompat

/**
 * Tiny consent trampoline for Goblin Eye.
 * Android owns the screen-capture consent UI; RavenOS never caches or reuses a grant.
 */
class RavenScreenWatchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (RavenScreenWatchService.isActive(this)) {
            RavenScreenWatchService.stop(this)
            finish()
            return
        }
        val manager = getSystemService(MediaProjectionManager::class.java) ?: run {
            finish()
            return
        }
        try {
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
        } catch (_: Throwable) {
            finish()
        }
    }

    @Deprecated("Deprecated in Android SDK; retained for the system MediaProjection consent result.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK && data != null) {
            val start = Intent(this, RavenScreenWatchService::class.java)
                .setAction(RavenScreenWatchService.ACTION_START)
                .putExtra(RavenScreenWatchService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(RavenScreenWatchService.EXTRA_RESULT_DATA, data)
            try { ContextCompat.startForegroundService(this, start) } catch (_: Throwable) {}
        }
        finish()
    }

    companion object {
        private const val REQUEST_CAPTURE = 0x4756
    }
}
