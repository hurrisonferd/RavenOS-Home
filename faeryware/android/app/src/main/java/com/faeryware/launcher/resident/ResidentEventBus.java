package com.faeryware.launcher.resident;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.faeryware.launcher.FaerywareWidgetProvider;

/** Local bounded event bus. It routes facts; it does not invent resident speech. */
public final class ResidentEventBus {
    public enum Type { APP_FOREGROUND, NOTIFICATION_SOURCE, TIMER_TICK, MANUAL, OFFICE_STATE }

    public static final String ACTION_RESIDENT_EVENT = "com.faeryware.launcher.RESIDENT_EVENT";
    private static final String PREFS = "faeryware_resident_event_bus_v1";
    private static final String KEY_TYPE = "type";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_AT = "at";

    private ResidentEventBus() {}

    public static void publish(Context context, Type type, String source) {
        long now = System.currentTimeMillis();
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(KEY_TYPE, type.name()).putString(KEY_SOURCE, source == null ? "" : source).putLong(KEY_AT, now).apply();

        Intent event = new Intent(ACTION_RESIDENT_EVENT).setPackage(context.getPackageName());
        event.putExtra(KEY_TYPE, type.name());
        event.putExtra(KEY_SOURCE, source == null ? "" : source);
        event.putExtra(KEY_AT, now);
        context.sendBroadcast(event);

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, FaerywareWidgetProvider.class));
        if (ids != null && ids.length > 0) {
            Intent update = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(new ComponentName(context, FaerywareWidgetProvider.class))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(update);
        }
    }

    public static Type lastType(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TYPE, Type.TIMER_TICK.name());
        try { return Type.valueOf(raw); } catch (Exception ignored) { return Type.TIMER_TICK; }
    }

    public static String lastSource(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SOURCE, ""); }
    public static long lastAt(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_AT, 0L); }
}
