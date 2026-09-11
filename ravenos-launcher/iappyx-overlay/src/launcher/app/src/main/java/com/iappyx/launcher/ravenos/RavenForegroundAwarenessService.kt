package com.iappyx.launcher.ravenos

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Optional high-awareness RavenOS lane.
 *
 * User must explicitly enable this Accessibility service in Android Settings.
 * The paired XML keeps canRetrieveWindowContent=false. We consume only package,
 * window/class identity and event type from window transitions; no nodes, typed text,
 * passwords, page contents, view hierarchy, or touch stream are retrieved.
 */
class RavenForegroundAwarenessService : AccessibilityService() {
    private var lastPackage: String? = null
    private var lastClass: String? = null
    private var lastWindowId: Int = -1
    private var lastAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString()?.trim().orEmpty()
        if (packageName.isBlank() || packageName == applicationContext.packageName) return

        val className = event.className?.toString()?.trim().orEmpty().take(120)
        val windowId = event.windowId
        val eventType = event.eventType
        val now = System.currentTimeMillis()

        val packageChanged = packageName != lastPackage
        val surfaceChanged = packageChanged || className != lastClass || windowId != lastWindowId
        if (!surfaceChanged && now - lastAt < 900L) return

        val signal = if (packageChanged) "FOREGROUND_APP" else "FOREGROUND_WINDOW"
        val detail = buildString {
            append("package:").append(packageName)
            if (className.isNotBlank()) append("|class:").append(className)
            if (windowId >= 0) append("|window:").append(windowId)
            append("|event:").append(eventType)
            append("|source:accessibility-window")
        }

        lastPackage = packageName
        lastClass = className
        lastWindowId = windowId
        lastAt = now

        RavenOfficeBarService.signal(this, signal, detail)
    }

    override fun onInterrupt() {
        RavenOfficeBarService.signal(this, "HOME", "foreground-awareness:interrupted")
    }
}
