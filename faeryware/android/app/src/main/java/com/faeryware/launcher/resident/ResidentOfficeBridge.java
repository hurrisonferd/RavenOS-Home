package com.faeryware.launcher.resident;

import android.content.Context;
import android.content.SharedPreferences;

/** Provider-neutral envelope for verified Omni RV owner state. No model/provider dependency lives here. */
public final class ResidentOfficeBridge {
    public enum SoakState { DEEP_SOAKED, SOAKED, SOAK_READY, THIN, CONFLICT, RESERVED, NON_IDENTITY, OFFLINE }
    public enum SpeechMode { OWNER_NATIVE, BOUNDED_VERIFIED_SOURCE, MINIMAL_VERIFIED_SOURCE, HOLD }

    private static final String PREFS = "faeryware_office_bridge_v1";

    private ResidentOfficeBridge() {}

    public static SpeechMode modeFor(SoakState state) {
        switch (state) {
            case DEEP_SOAKED:
            case SOAKED:
                return SpeechMode.OWNER_NATIVE;
            case SOAK_READY:
                return SpeechMode.BOUNDED_VERIFIED_SOURCE;
            case THIN:
                return SpeechMode.MINIMAL_VERIFIED_SOURCE;
            case CONFLICT:
            case RESERVED:
            case NON_IDENTITY:
            case OFFLINE:
            default:
                return SpeechMode.HOLD;
        }
    }

    /** Called only by an explicit trusted RavenOS/Omni RV adapter. */
    public static void applyEnvelope(Context context, String owner, SoakState state, boolean verifiedSource, String verifiedText, String sourceSha) {
        if (owner == null || owner.trim().isEmpty()) return;
        String key = owner.trim().toUpperCase(java.util.Locale.US);
        SharedPreferences.Editor e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        e.putString(key + ".state", state.name());
        e.putBoolean(key + ".verified", verifiedSource);
        e.putString(key + ".text", verifiedText == null ? "" : verifiedText);
        e.putString(key + ".sha", sourceSha == null ? "" : sourceSha);
        e.putLong(key + ".at", System.currentTimeMillis());
        e.apply();
        ResidentEventBus.publish(context, ResidentEventBus.Type.OFFICE_STATE, key);
    }

    public static SoakState state(Context context, String owner) {
        if (owner == null) return SoakState.OFFLINE;
        String key = owner.trim().toUpperCase(java.util.Locale.US);
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key + ".state", SoakState.OFFLINE.name());
        try { return SoakState.valueOf(raw); } catch (Exception ignored) { return SoakState.OFFLINE; }
    }

    public static String verifiedSpeech(Context context, String owner) {
        if (owner == null) return "";
        String key = owner.trim().toUpperCase(java.util.Locale.US);
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean(key + ".verified", false)) return "";
        SpeechMode mode = modeFor(state(context, key));
        if (mode == SpeechMode.HOLD) return "";
        return p.getString(key + ".text", "");
    }

    public static String sourceSha(Context context, String owner) {
        if (owner == null) return "";
        String key = owner.trim().toUpperCase(java.util.Locale.US);
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key + ".sha", "");
    }
}
