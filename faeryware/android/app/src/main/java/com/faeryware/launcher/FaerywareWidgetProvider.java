package com.faeryware.launcher;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class FaerywareWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { update(context, manager, ids); }
    static void refreshAll(Context context) { AppWidgetManager manager = AppWidgetManager.getInstance(context); int[] ids = manager.getAppWidgetIds(new ComponentName(context, FaerywareWidgetProvider.class)); update(context, manager, ids); }
    private static void update(Context context, AppWidgetManager manager, int[] ids) {
        int fae = FaerywareMemoryStore.activeFaeIndex(context);
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_faeryware);
            views.setImageViewBitmap(R.id.widget_portrait, FaerywareChibiRenderer.render(context, fae, 320));
            views.setTextViewText(R.id.widget_title, FaerywareMemoryStore.faeStamp(fae));
            views.setTextViewText(R.id.widget_line, FaerywareMemoryStore.whisper(context, fae));
            views.setTextViewText(R.id.widget_context, FaerywareMemoryStore.summary(context));
            PendingIntent pi = PendingIntent.getActivity(context, 91, new Intent(context, FaerywareLauncherActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pi);
            manager.updateAppWidget(id, views);
        }
    }
}
