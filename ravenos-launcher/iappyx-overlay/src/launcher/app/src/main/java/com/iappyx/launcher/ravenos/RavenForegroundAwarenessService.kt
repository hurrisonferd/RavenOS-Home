package com.iappyx.launcher.ravenos

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Optional RavenOS foreground + owner-armed visible-semantics lane.
 *
 * Package/window identity is always the baseline once this Android Accessibility service is enabled.
 * Visible node reading remains a second explicit Raven switch in RavenAccessibilityReadOS.
 */
class RavenForegroundAwarenessService : AccessibilityService() {
    private var lastPackage: String? = null
    private var lastClass: String? = null
    private var lastWindowId: Int = -1
    private var lastAt: Long = 0L
    private var lastSemanticAt: Long = 0L

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
        if (surfaceChanged || now - lastAt >= 900L) {
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

        // Content is deliberately slower than window identity. We only ask Android for the
        // visible hierarchy when Raven has separately armed Accessibility Read.
        if (RavenAccessibilityReadOS.isEnabled(this) && now - lastSemanticAt >= 1200L) {
            lastSemanticAt = now
            val root = try { rootInActiveWindow } catch (_: Throwable) { null }
            RavenAccessibilityReadOS.observe(this, packageName, root)
        }
    }

    override fun onInterrupt() {
        RavenOfficeBarService.signal(this, "HOME", "foreground-awareness:interrupted")
    }
}
