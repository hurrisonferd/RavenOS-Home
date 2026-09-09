package com.faeryware.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class FaerywareMemoryStore {
    static final String PUBLIC_PREFS = "faeryware_public";
    static final String MEMORY_PREFS = "faeryware_memory_v1";
    static final String KEY_CONTEXT_MODE = "context_mode";
    static final String KEY_RESIDENT_BOOT = "resident_boot";
    static final String KEY_RESIDENT_ENABLED = "resident_enabled";
    private static final String KEY_FOREGROUND_PACKAGE = "foreground_package";
    private static final String KEY_FOREGROUND_LABEL = "foreground_label";
    private static final String KEY_FOREGROUND_AT = "foreground_at";
    private static final String KEY_NOTIFICATION_PACKAGE = "notification_package";
    private static final String KEY_NOTIFICATION_LABEL = "notification_label";
    private static final String KEY_NOTIFICATION_AT = "notification_at";
    private static final String KEY_NOTIFICATION_COUNT = "notification_count";
    private static final String KEY_HISTORY = "history";
    private static final int HISTORY_MAX = 36;

    private FaerywareMemoryStore() {}

    static void recordForeground(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        String pkg = packageName.trim();
        if (pkg.equals(context.getPackageName())) return;
        String label = appLabel(context, pkg);
        long now = System.currentTimeMillis();
        SharedPreferences p = context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE);
        String previous = p.getString(KEY_FOREGROUND_PACKAGE, "");
        p.edit().putString(KEY_FOREGROUND_PACKAGE, pkg).putString(KEY_FOREGROUND_LABEL, label).putLong(KEY_FOREGROUND_AT, now).apply();
        if (!pkg.equals(previous)) appendHistory(context, "APP", pkg, now);
    }

    static void recordNotification(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        String pkg = packageName.trim();
        if (pkg.equals(context.getPackageName())) return;
        String label = appLabel(context, pkg);
        long now = System.currentTimeMillis();
        SharedPreferences p = context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE);
        int count = Math.min(9999, p.getInt(KEY_NOTIFICATION_COUNT, 0) + 1);
        p.edit().putString(KEY_NOTIFICATION_PACKAGE, pkg).putString(KEY_NOTIFICATION_LABEL, label).putLong(KEY_NOTIFICATION_AT, now).putInt(KEY_NOTIFICATION_COUNT, count).apply();
        appendHistory(context, "NOTIFY", pkg, now);
    }

    static int activeFaeIndex(Context context) {
        SharedPreferences publicPrefs = context.getSharedPreferences(PUBLIC_PREFS, Context.MODE_PRIVATE);
        if (!publicPrefs.getBoolean(KEY_CONTEXT_MODE, true)) return hourlyFae();
        SharedPreferences p = context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE);
        String signal = (p.getString(KEY_FOREGROUND_PACKAGE, "") + " " + p.getString(KEY_FOREGROUND_LABEL, "")).toLowerCase(Locale.US);
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 23 || hour < 6) return 5;
        if (containsAny(signal, "message","messenger","discord","slack","telegram","whatsapp","gmail","mail","signal")) return 4;
        if (containsAny(signal, "chrome","browser","firefox","internet","search","reddit","wikipedia")) return 3;
        if (containsAny(signal, "notes","keep","docs","document","notion","calculator","settings")) return 1;
        if (containsAny(signal, "camera","gallery","photos","photo","video","capcut")) return 0;
        if (containsAny(signal, "spotify","music","youtube","podcast","weather","calendar","clock")) return 2;
        String notification = p.getString(KEY_NOTIFICATION_PACKAGE, "").toLowerCase(Locale.US);
        long notificationAt = p.getLong(KEY_NOTIFICATION_AT, 0L);
        if (System.currentTimeMillis() - notificationAt < 90_000L) {
            if (containsAny(notification, "message","discord","slack","mail","gmail")) return 4;
            return 0;
        }
        return hourlyFae();
    }

    static String faeName(int index) { String[] names = {"KYU","PAIMON","LUMA","SYLPH","QIRA","NYX"}; return names[Math.floorMod(index, names.length)]; }
    static String faeStamp(int index) { String[] names = {"💗 KYU","🟢 PAIMON","🟡 LUMA","🔵 SYLPH","🟣 QIRA","🔷 NYX"}; return names[Math.floorMod(index, names.length)]; }
    static int faeColor(int index) { int[] colors = {0xffff4e9d,0xff42dc76,0xfff6cd5c,0xff46d3ff,0xffcb57ff,0xff5a6ce6}; return colors[Math.floorMod(index, colors.length)]; }

    static String whisper(Context context, int index) {
        String app = currentAppLabel(context);
        String[] base = {"hehe. still here.","hmm... pattern found.","home. lights on.","new path. zoom.","boundary held. nope.","☾ watching."};
        String line = base[Math.floorMod(index, base.length)];
        if (app == null || app.isEmpty() || "UNKNOWN".equals(app)) return line;
        switch (Math.floorMod(index, 6)) {
            case 0: return line + " " + shortLabel(app) + "?";
            case 1: return "noted: " + shortLabel(app) + ".";
            case 2: return "hanging out by " + shortLabel(app) + ".";
            case 3: return "found you in " + shortLabel(app) + ".";
            case 4: return shortLabel(app) + " boundary held.";
            default: return "☾ " + shortLabel(app) + " is open.";
        }
    }

    static String summary(Context context) {
        SharedPreferences p = context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE);
        String app = p.getString(KEY_FOREGROUND_LABEL, "UNKNOWN");
        String notification = p.getString(KEY_NOTIFICATION_LABEL, "");
        int count = p.getInt(KEY_NOTIFICATION_COUNT, 0);
        long appAt = p.getLong(KEY_FOREGROUND_AT, 0L);
        String age = appAt <= 0 ? "no app memory yet" : age(System.currentTimeMillis() - appAt);
        StringBuilder out = new StringBuilder();
        out.append("with ").append(shortLabel(app)).append(" • ").append(age);
        if (notification != null && !notification.isEmpty()) out.append(" • last ping ").append(shortLabel(notification));
        out.append(" • pings ").append(count);
        return out.toString();
    }

    static String currentAppLabel(Context context) { return context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE).getString(KEY_FOREGROUND_LABEL, "UNKNOWN"); }
    static String history(Context context) { return context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE).getString(KEY_HISTORY, ""); }
    static void clear(Context context) { context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE).edit().clear().apply(); }
    private static int hourlyFae() { return (int)((System.currentTimeMillis() / 3_600_000L) % 6L); }

    private static void appendHistory(Context context, String kind, String pkg, long now) {
        SharedPreferences p = context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE);
        String existing = p.getString(KEY_HISTORY, "");
        List<String> lines = new ArrayList<>();
        if (existing != null && !existing.isEmpty()) for (String line : existing.split("\\n")) if (!line.trim().isEmpty()) lines.add(line);
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

    private static boolean containsAny(String value, String... needles) { if (value == null) return false; for (String n : needles) if (value.contains(n)) return true; return false; }
    private static String shortLabel(String value) { if (value == null || value.trim().isEmpty()) return "the phone"; String s = value.trim(); return s.length() <= 22 ? s : s.substring(0, 21) + "…"; }
    private static String age(long ms) { if (ms < 0) return "now"; long seconds = ms / 1000L; if (seconds < 45) return "now"; long minutes = seconds / 60L; if (minutes < 60) return minutes + "m ago"; return (minutes / 60L) + "h ago"; }
}
