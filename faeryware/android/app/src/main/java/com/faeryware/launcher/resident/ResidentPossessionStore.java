package com.faeryware.launcher.resident;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** Durable transaction record for future iappyx placement possession/restoration. */
public final class ResidentPossessionStore {
    private static final String PREFS = "faeryware_possession_v1";

    public static final class Possession {
        public final String targetId;
        public final String resident;
        public final String surface;
        public final String originalJson;
        public final long acquiredAt;

        Possession(String targetId, String resident, String surface, String originalJson, long acquiredAt) {
            this.targetId = targetId;
            this.resident = resident;
            this.surface = surface;
            this.originalJson = originalJson;
            this.acquiredAt = acquiredAt;
        }
    }

    private ResidentPossessionStore() {}

    public static boolean begin(Context context, String targetId, String resident, String surface, String originalPlacementJson) {
        if (blank(targetId) || blank(resident) || blank(surface) || blank(originalPlacementJson)) return false;
        try { new JSONObject(originalPlacementJson); } catch (Exception invalid) { return false; }
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.contains(targetId + ".original")) return false; // fail closed: never overwrite the restoration anchor.
        p.edit()
            .putString(targetId + ".resident", resident)
            .putString(targetId + ".surface", surface)
            .putString(targetId + ".original", originalPlacementJson)
            .putLong(targetId + ".at", System.currentTimeMillis())
            .apply();
        return true;
    }

    public static Possession get(Context context, String targetId) {
        if (blank(targetId)) return null;
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String original = p.getString(targetId + ".original", null);
        if (original == null) return null;
        return new Possession(
            targetId,
            p.getString(targetId + ".resident", ""),
            p.getString(targetId + ".surface", ""),
            original,
            p.getLong(targetId + ".at", 0L)
        );
    }

    /** Returns exact stored JSON and clears the transaction only after the caller confirms restoration. */
    public static String restorationPayload(Context context, String targetId) {
        Possession p = get(context, targetId);
        return p == null ? null : p.originalJson;
    }

    public static void confirmRestored(Context context, String targetId) {
        if (blank(targetId)) return;
        SharedPreferences.Editor e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        e.remove(targetId + ".resident");
        e.remove(targetId + ".surface");
        e.remove(targetId + ".original");
        e.remove(targetId + ".at");
        e.apply();
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
