package com.faeryware.launcher.resident;

import android.content.Context;

import java.util.Locale;

/** Resident selection + bounded visual state. Identity/cognition remains upstream in RavenOS/Omni RV. */
public final class ResidentRuntime {
    private static final String[] NAMES = {"KYU", "PAIMON", "LUMA", "SYLPH", "QIRA", "NYX"};
    private static final String[] STAMPS = {"💗 KYU", "🟢 PAIMON", "🟡 LUMA", "🔵 SYLPH", "🟣 QIRA", "🔷 NYX"};
    private static final int[] COLORS = {0xffff4e9d, 0xff42dc76, 0xfff6cd5c, 0xff46d3ff, 0xffcb57ff, 0xff5a6ce6};
    private static final String[] LANES = {"MOTION", "FRAME", "HOME", "SIGNAL", "BOUNDARY", "QUIET"};
    private static final String[] VISUAL_STATES = {"OBSERVE", "NOTICE", "MOVE", "SETTLE", "WAIT", "QUIET"};

    private ResidentRuntime() {}

    public static int activeResident(Context context, boolean contextMode) {
        if (!contextMode) return hourlyResident();
        String signal = (ResidentSignalStore.foregroundPackage(context) + " " + ResidentSignalStore.foregroundLabel(context)).toLowerCase(Locale.US);
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 23 || hour < 6) return 5;
        if (containsAny(signal, "message", "messenger", "discord", "slack", "telegram", "whatsapp", "gmail", "mail", "signal")) return 4;
        if (containsAny(signal, "chrome", "browser", "firefox", "internet", "search", "reddit", "wikipedia")) return 3;
        if (containsAny(signal, "notes", "keep", "docs", "document", "notion", "calculator", "settings")) return 1;
        if (containsAny(signal, "camera", "gallery", "photos", "photo", "video", "capcut")) return 0;
        if (containsAny(signal, "spotify", "music", "youtube", "podcast", "weather", "calendar", "clock")) return 2;

        long age = System.currentTimeMillis() - ResidentSignalStore.notificationAt(context);
        if (age >= 0 && age < 90_000L) {
            String notification = ResidentSignalStore.notificationPackage(context).toLowerCase(Locale.US);
            if (containsAny(notification, "message", "discord", "slack", "mail", "gmail")) return 4;
            return 0;
        }
        return hourlyResident();
    }

    public static int visualState(Context context, int resident) {
        String pkg = ResidentSignalStore.foregroundPackage(context);
        ResidentEventBus.Type event = ResidentEventBus.lastType(context);
        long bucket = System.currentTimeMillis() / 120_000L;
        int seed = (pkg == null ? 0 : pkg.hashCode()) + event.ordinal() * 17;
        int state = Math.floorMod(seed + (int) bucket + resident * 7, VISUAL_STATES.length);
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (resident == 5 && (hour >= 23 || hour < 6)) return 5;
        return state;
    }

    public static String name(int index) { return NAMES[Math.floorMod(index, NAMES.length)]; }
    public static String stamp(int index) { return STAMPS[Math.floorMod(index, STAMPS.length)]; }
    public static int color(int index) { return COLORS[Math.floorMod(index, COLORS.length)]; }
    public static String lane(int index) { return LANES[Math.floorMod(index, LANES.length)]; }
    public static String visualStateName(int state) { return VISUAL_STATES[Math.floorMod(state, VISUAL_STATES.length)]; }

    /** UI label only; never presented as owner-native dialogue. */
    public static String surfaceLabel(Context context, int index) {
        return lane(index) + " · " + visualStateName(visualState(context, index));
    }

    private static int hourlyResident() { return (int) ((System.currentTimeMillis() / 3_600_000L) % NAMES.length); }

    private static boolean containsAny(String value, String... needles) {
        if (value == null) return false;
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
