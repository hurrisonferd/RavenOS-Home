package com.faeryware.launcher;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class FaerywareWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_faeryware);
            int fae=(int)((System.currentTimeMillis()/3_600_000L)%6);
            String[] names={"💗 KYU","🟢 PAIMON","🟡 LUMA","🔵 SYLPH","🟣 QIRA","🔷 NYX"};
            String[] lines={"hehe. still here.","pattern found.","home. lights on.","new path. zoom.","boundary held.","☾ watching."};
            views.setTextViewText(R.id.widget_title,names[fae]);
            views.setTextViewText(R.id.widget_line,lines[fae]);
            Intent open=new Intent(context,FaerywareLauncherActivity.class);
            PendingIntent pi=PendingIntent.getActivity(context,91,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root,pi);
            manager.updateAppWidget(id,views);
        }
    }
}
