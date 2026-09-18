package com.faeryware.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import com.faeryware.launcher.resident.ResidentRuntime;
import com.faeryware.launcher.resident.ResidentSignalStore;

/**
 * Compatibility facade for v5 callers.
 * v6 separates local phone observations (ResidentSignalStore) from resident identity/state (ResidentRuntime).
 * This class intentionally does not own freeform resident dialogue.
 */
final class FaerywareMemoryStore {
    static final String PUBLIC_PREFS = "faeryware_public";
    static final String MEMORY_PREFS = ResidentSignalStore.PREFS;
    static final String KEY_CONTEXT_MODE = "context_mode";
    static final String KEY_RESIDENT_BOOT = "resident_boot";
    static final String KEY_RESIDENT_ENABLED = "resident_enabled";
    static final String KEY_OVERLAY_SCOPE = "overlay_scope";
    static final String SCOPE_HOME_ONLY = "HOME_ONLY";
    static final String SCOPE_FOLLOW_ME = "FOLLOW_ME";

    private FaerywareMemoryStore() {}

    static void recordForeground(Context context, String packageName) { ResidentSignalStore.recordForeground(context, packageName); }
    static void recordNotification(Context context, String packageName) { ResidentSignalStore.recordNotification(context, packageName); }

    static int activeFaeIndex(Context context) {
        boolean contextMode = context.getSharedPreferences(PUBLIC_PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CONTEXT_MODE, true);
        return ResidentRuntime.activeResident(context, contextMode);
    }

    static int reactionState(Context context, int fae) { return ResidentRuntime.visualState(context, fae); }

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

    static String currentAppPackage(Context context) { return ResidentSignalStore.foregroundPackage(context); }
    static String currentAppLabel(Context context) { return ResidentSignalStore.foregroundLabel(context); }
    static String faeName(int index) { return ResidentRuntime.name(index); }
    static String faeStamp(int index) { return ResidentRuntime.stamp(index); }
    static int faeColor(int index) { return ResidentRuntime.color(index); }
    static String stateLabel(int fae, int state) { return ResidentRuntime.lane(fae) + " · " + ResidentRuntime.visualStateName(state); }

    /** UI status label, not owner-native speech. */
    static String whisper(Context context, int fae) { return ResidentRuntime.surfaceLabel(context, fae); }

    static String history(Context context) { return ResidentSignalStore.history(context); }
    static void clear(Context context) { ResidentSignalStore.clear(context); }
    static String summary(Context context) { return ResidentSignalStore.summary(context); }
}
