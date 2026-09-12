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

    override fun onServiceConnected() {
        super.onServiceConnected()
        RavenOfficeBarService.signal(this, "FOREGROUND_WINDOW", "state:accessibility_connected|source:accessibility-window")
        if (RavenAccessibilityReadOS.isEnabled(this)) observeVisibleSemantics(null, force = true)
    }

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

        // Web content can mutate or scroll without a new Android window. Re-read the active root on
        // bounded content/focus/scroll/click events so browser surfaces do not leave the office blind.
        if (RavenAccessibilityReadOS.isEnabled(this) && now - lastSemanticAt >= 550L) {
            observeVisibleSemantics(packageName, force = false)
        }
    }

    private fun observeVisibleSemantics(packageHint: String?, force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSemanticAt < 550L) return
        val root = try { rootInActiveWindow } catch (_: Throwable) { null } ?: return
        val pkg = packageHint?.takeIf { it.isNotBlank() }
            ?: root.packageName?.toString()?.trim().orEmpty()
        if (pkg.isBlank() || pkg == applicationContext.packageName) return
        lastSemanticAt = now
        RavenAccessibilityReadOS.observe(this, pkg, root)
    }

    override fun onInterrupt() {
        RavenOfficeBarService.signal(this, "HOME", "foreground-awareness:interrupted")
    }
}
