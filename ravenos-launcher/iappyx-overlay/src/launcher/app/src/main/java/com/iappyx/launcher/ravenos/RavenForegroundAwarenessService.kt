package com.iappyx.launcher.ravenos

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

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

        // Web/chat content can mutate or scroll without a new Android window. Re-read a bounded
        // semantic viewport. If the IME / screenshot UI / System UI is active, prefer the visible
        // ordinary app underneath it rather than promoting a transient layer into scene ownership.
        if (RavenAccessibilityReadOS.isEnabled(this) && now - lastSemanticAt >= 550L) {
            observeVisibleSemantics(packageName, force = false)
        }
    }

    private data class WindowRoot(
        val root: AccessibilityNodeInfo,
        val pkg: String,
        val active: Boolean,
        val focused: Boolean,
        val type: Int,
    )

    private fun observeVisibleSemantics(packageHint: String?, force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSemanticAt < 550L) return

        val candidates = ArrayList<WindowRoot>()
        val visibleWindows = try { windows.orEmpty() } catch (_: Throwable) { emptyList<AccessibilityWindowInfo>() }
        visibleWindows.forEach { window ->
            val root = try { window.root } catch (_: Throwable) { null } ?: return@forEach
            val pkg = root.packageName?.toString()?.trim().orEmpty()
            if (pkg.isBlank() || pkg == applicationContext.packageName) return@forEach
            candidates += WindowRoot(root, pkg, window.isActive, window.isFocused, window.type)
        }

        if (candidates.isEmpty()) {
            val root = try { rootInActiveWindow } catch (_: Throwable) { null } ?: return
            val pkg = root.packageName?.toString()?.trim().orEmpty()
            if (pkg.isBlank() || pkg == applicationContext.packageName) return
            candidates += WindowRoot(root, pkg, true, true, AccessibilityWindowInfo.TYPE_APPLICATION)
        }

        fun isInput(pkg: String): Boolean {
            val p = pkg.lowercase()
            return p.contains("honeyboard") || p.contains("inputmethod") || p.contains("keyboard")
        }
        fun isTransientSystemLayer(pkg: String): Boolean {
            val p = pkg.lowercase()
            return p == "com.android.systemui" ||
                p.contains("smartcapture") ||
                p.contains("screenshot") ||
                p.contains("capture") && p.contains("samsung") ||
                isInput(pkg)
        }

        val ordinaryApps = candidates.filterNot { isTransientSystemLayer(it.pkg) }
        val nonInput = candidates.filterNot { isInput(it.pkg) }
        val preferredPool = ordinaryApps.ifEmpty { nonInput.ifEmpty { candidates } }
        val hintIsTransient = packageHint?.let(::isTransientSystemLayer) == true
        val stableHint = packageHint.takeUnless { hintIsTransient }

        val selected = preferredPool.firstOrNull { it.active && stableHint != null && it.pkg == stableHint }
            ?: preferredPool.firstOrNull { it.active && it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            ?: preferredPool.firstOrNull { it.active }
            ?: preferredPool.firstOrNull { it.focused && stableHint != null && it.pkg == stableHint }
            ?: preferredPool.firstOrNull { it.focused }
            ?: preferredPool.firstOrNull { stableHint != null && it.pkg == stableHint }
            ?: preferredPool.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            ?: preferredPool.firstOrNull()
            ?: return

        lastSemanticAt = now
        RavenAccessibilityReadOS.observe(this, selected.pkg, selected.root)
    }

    override fun onInterrupt() {
        RavenOfficeBarService.signal(this, "HOME", "foreground-awareness:interrupted")
    }
}
