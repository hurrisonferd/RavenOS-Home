package com.faeryware.launcher.resident;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/** Narrow local phone observations. No message bodies, page text, keys, or network transport. */
public final class ResidentSignalStore {
    public static final String PREFS = "faeryware_memory_v1";
    private static final String KEY_FOREGROUND_PACKAGE = "foreground_package";
    private static final String KEY_FOREGROUND_LABEL = "foreground_label";
    private static final String KEY_FOREGROUND_AT = "foreground_at";
    private static final String KEY_NOTIFICATION_PACKAGE = "notification_package";
    private static final String KEY_NOTIFICATION_LABEL = "notification_label";
    private static final String KEY_NOTIFICATION_AT = "notification_at";
    private static final String KEY_NOTIFICATION_COUNT = "notification_count";
    private static final String KEY_HISTORY = "history";
    private static final int HISTORY_MAX = 36;

    private ResidentSignalStore() {}

    public static void recordForeground(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        String pkg = packageName.trim();
        if (pkg.equals(context.getPackageName())) return;
        SharedPreferences p = prefs(context);
        String previous = p.getString(KEY_FOREGROUND_PACKAGE, "");
        long now = System.currentTimeMillis();
        p.edit()
            .putString(KEY_FOREGROUND_PACKAGE, pkg)
            .putString(KEY_FOREGROUND_LABEL, appLabel(context, pkg))
            .putLong(KEY_FOREGROUND_AT, now)
            .apply();
        if (!pkg.equals(previous)) appendHistory(context, "APP", pkg, now);
        ResidentEventBus.publish(context, ResidentEventBus.Type.APP_FOREGROUND, pkg);
    }

    public static void recordNotification(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        String pkg = packageName.trim();
        if (pkg.equals(context.getPackageName())) return;
        SharedPreferences p = prefs(context);
        long now = System.currentTimeMillis();
        int count = Math.min(9999, p.getInt(KEY_NOTIFICATION_COUNT, 0) + 1);
        p.edit()
            .putString(KEY_NOTIFICATION_PACKAGE, pkg)
            .putString(KEY_NOTIFICATION_LABEL, appLabel(context, pkg))
            .putLong(KEY_NOTIFICATION_AT, now)
            .putInt(KEY_NOTIFICATION_COUNT, count)
            .apply();
        appendHistory(context, "NOTIFY", pkg, now);
        ResidentEventBus.publish(context, ResidentEventBus.Type.NOTIFICATION_SOURCE, pkg);
    }

    public static String foregroundPackage(Context context) { return prefs(context).getString(KEY_FOREGROUND_PACKAGE, ""); }
    public static String foregroundLabel(Context context) { return prefs(context).getString(KEY_FOREGROUND_LABEL, "UNKNOWN"); }
    public static long foregroundAt(Context context) { return prefs(context).getLong(KEY_FOREGROUND_AT, 0L); }
    public static String notificationPackage(Context context) { return prefs(context).getString(KEY_NOTIFICATION_PACKAGE, ""); }
    public static String notificationLabel(Context context) { return prefs(context).getString(KEY_NOTIFICATION_LABEL, ""); }
    public static long notificationAt(Context context) { return prefs(context).getLong(KEY_NOTIFICATION_AT, 0L); }
    public static int notificationCount(Context context) { return prefs(context).getInt(KEY_NOTIFICATION_COUNT, 0); }
    public static String history(Context context) { return prefs(context).getString(KEY_HISTORY, ""); }

    public static String summary(Context context) {
        String app = shortLabel(foregroundLabel(context));
        String notification = notificationLabel(context);
        StringBuilder out = new StringBuilder(app);
        if (notification != null && !notification.trim().isEmpty()) out.append(" • last ping ").append(shortLabel(notification));
        out.append(" • ").append(notificationCount(context)).append(" pings");
        return out.toString();
    }

    public static void clear(Context context) { prefs(context).edit().clear().apply(); }

    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private static void appendHistory(Context context, String kind, String pkg, long now) {
        SharedPreferences p = prefs(context);
        String existing = p.getString(KEY_HISTORY, "");
        List<String> lines = new ArrayList<>();
        if (existing != null && !existing.isEmpty()) {
            for (String line : existing.split("\\n")) if (!line.trim().isEmpty()) lines.add(line);
        }
        lines.add(kind + "|" + pkg + "|" + now);
        while (lines.size() > HISTORY_MAX) lines.remove(0);
        StringBuilder out = new StringBuilder();
        for (String line : lines) { if (out.length() > 0) out.append('\n'); out.append(line); }
        p.edit().putString(KEY_HISTORY, out.toString()).apply();
    }

    private static String appLabel(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(info);
            if (label != null && label.length() > 0) return label.toString();
        } catch (Exception ignored) {}
        int dot = pkg.lastIndexOf('.');
        return dot >= 0 && dot + 1 < pkg.length() ? pkg.substring(dot + 1) : pkg;
    }

    private static String shortLabel(String value) {
        if (value == null || value.trim().isEmpty()) return "the phone";
        String s = value.trim();
        return s.length() <= 22 ? s : s.substring(0, 21) + "…";
    }
}
