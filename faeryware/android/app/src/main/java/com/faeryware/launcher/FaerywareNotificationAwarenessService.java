package com.faeryware.launcher;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class FaerywareNotificationAwarenessService extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        FaerywareMemoryStore.recordNotification(this, sbn.getPackageName());
        FaerywareWidgetProvider.refreshAll(this);
    }
}
