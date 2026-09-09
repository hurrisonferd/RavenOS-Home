package com.faeryware.launcher;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public final class FaerywareAwarenessService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        FaerywareMemoryStore.recordForeground(this, pkg.toString());
        FaerywareWidgetProvider.refreshAll(this);
    }
    @Override public void onInterrupt() {}
}
