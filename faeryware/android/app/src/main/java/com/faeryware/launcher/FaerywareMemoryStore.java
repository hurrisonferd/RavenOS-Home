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
    static final String KEY_OVERLAY_SCOPE = "overlay_scope";
    static final String SCOPE_HOME_ONLY = "HOME_ONLY";
    static final String SCOPE_FOLLOW_ME = "FOLLOW_ME";
    private static final String KEY_FOREGROUND_PACKAGE = "foreground_package";
    private static final String KEY_FOREGROUND_LABEL = "foreground_label";
    private static final String KEY_FOREGROUND_AT = "foreground_at";
    private static final String KEY_NOTIFICATION_PACKAGE = "notification_package";
    private static final String KEY_NOTIFICATION_LABEL = "notification_label";
    private static final String KEY_NOTIFICATION_AT = "notification_at";
    private static final String KEY_NOTIFICATION_COUNT = "notification_count";
    private static final String KEY_HISTORY = "history";
    private static final int HISTORY_MAX = 36;

    private static final String[][] STATES = {
        {"HI!","LET'S GO!","ON IT!","HEHE","BONK","SUS..."},
        {"HMM...","I SEE IT.","EXACTLY.","BIG BRAIN","SUS.","ALL GOOD."},
        {"GOOD MORNING","YOU GOT THIS","COMFY","IT'S OKAY","BEAUTIFUL","HOME. ♡"},
        {"LET'S EXPLORE!","SO COOL!","IDEA!","ZOOM!","CURIOUS...","NEW PATH!"},
        {"YES.","NO.","SAY IT.","BOUNDARIES.","EXCUSE ME?","REAL TALK."},
        {"...","WATCHING.","UNDERSTOOD.","REST.","NOTED.","LATER."}
    };

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

    static int reactionState(Context context, int fae) {
        String pkg = currentAppPackage(context);
        long bucket = System.currentTimeMillis() / 120_000L;
        int seed = pkg == null ? 0 : pkg.hashCode();
        int state = Math.floorMod(seed + (int) bucket + fae * 7, 6);
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (fae == 5 && (hour >= 23 || hour < 6)) return Math.floorMod((int) bucket, 3) + 3;
        return state;
    }

    static boolean shouldShowColony(Context context) {
        SharedPreferences p = context.getSharedPreferences(PUBLIC_PREFS, Context.MODE_PRIVATE);
        String scope = p.getString(KEY_OVERLAY_SCOPE, SCOPE_HOME_ONLY);
        if (SCOPE_FOLLOW_ME.equals(scope)) return true;
        String pkg = currentAppPackage(context);
        if (pkg == null || pkg.isEmpty()) return true;
        return pkg.equals("com.sec.android.app.launcher") || pkg.contains("launcher") || pkg.equals(context.getPackageName());
    }

    static String overlayScope(Context context) {
        return context.getSharedPreferences(PUBLIC_PREFS, Context.MODE_PRIVATE).getString(KEY_OVERLAY_SCOPE, SCOPE_HOME_ONLY);
    }
    static String currentAppPackage(Context context) { return context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE).getString(KEY_FOREGROUND_PACKAGE, ""); }
    static String currentAppLabel(Context context) { return context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE).getString(KEY_FOREGROUND_LABEL, "UNKNOWN"); }
    static String faeName(int index) { String[] names = {"KYU","PAIMON","LUMA","SYLPH","QIRA","NYX"}; return names[Math.floorMod(index, names.length)]; }
    static String faeStamp(int index) { String[] names = {"💗 KYU","🟢 PAIMON","🟡 LUMA","🔵 SYLPH","🟣 QIRA","🔷 NYX"}; return names[Math.floorMod(index, names.length)]; }
    static int faeColor(int index) { int[] colors = {0xffff4e9d,0xff42dc76,0xfff6cd5c,0xff46d3ff,0xffcb57ff,0xff5a6ce6}; return colors[Math.floorMod(index, colors.length)]; }
    static String stateLabel(int fae, int state) { return STATES[Math.floorMod(fae, 6)][Math.floorMod(state, 6)]; }
    static String whisper(Context context, int fae) { return stateLabel(fae, reactionState(context, fae)); }
    static String history(Context context) { return context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE).getString(KEY_HISTORY, ""); }
    static void clear(Context context) { context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE).edit().clear().apply(); }

    static String summary(Context context) {
        SharedPreferences p = context.getSharedPreferences(MEMORY_PREFS, Context.MODE_PRIVATE);
        String app = p.getString(KEY_FOREGROUND_LABEL, "UNKNOWN");
        String notification = p.getString(KEY_NOTIFICATION_LABEL, "");
        int count = p.getInt(KEY_NOTIFICATION_COUNT, 0);
        StringBuilder out = new StringBuilder();
        out.append(shortLabel(app));
        if (notification != null && !notification.isEmpty()) out.append(" • last ping ").append(shortLabel(notification));
        out.append(" • ").append(count).append(" pings");
        return out.toString();
    }

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
        try { PackageManager pm=context.getPackageManager(); ApplicationInfo info=pm.getApplicationInfo(pkg,0); CharSequence label=pm.getApplicationLabel(info); if(label!=null&&label.length()>0)return label.toString(); } catch(Exception ignored) {}
        int dot=pkg.lastIndexOf('.'); return dot>=0&&dot+1<pkg.length()?pkg.substring(dot+1):pkg;
    }
    private static boolean containsAny(String value, String... needles) { if(value==null)return false; for(String n:needles)if(value.contains(n))return true; return false; }
    private static String shortLabel(String value) { if(value==null||value.trim().isEmpty())return "the phone"; String s=value.trim(); return s.length()<=22?s:s.substring(0,21)+"…"; }
}
