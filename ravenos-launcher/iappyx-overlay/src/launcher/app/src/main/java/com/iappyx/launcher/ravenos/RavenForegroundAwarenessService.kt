package com.iappyx.launcher.ravenos

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Optional high-awareness RavenOS lane.
 *
 * User must explicitly enable this Accessibility service in Android Settings.
 * The paired XML sets canRetrieveWindowContent=false. We consume only the foreground
 * package name from window-state/window-set transitions and never inspect nodes, typed
 * text, passwords, page contents, or touch events.
 */
class RavenForegroundAwarenessService : AccessibilityService() {
    private var lastPackage: String? = null
    private var lastAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString()?.trim().orEmpty()
        if (packageName.isBlank() || packageName == applicationContext.packageName) return

        val now = System.currentTimeMillis()
        if (packageName == lastPackage && now - lastAt < 750L) return
        lastPackage = packageName
        lastAt = now

        RavenOfficeBarService.signal(
            this,
            "FOREGROUND_APP",
            "package:$packageName",
        )
    }

    override fun onInterrupt() {
        RavenOfficeBarService.signal(this, "HOME", "foreground-awareness:interrupted")
    }
}
