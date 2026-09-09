package com.faeryware.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public final class FaerywareBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        boolean enabled = context.getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, Context.MODE_PRIVATE).getBoolean(FaerywareMemoryStore.KEY_RESIDENT_BOOT, false);
        if (!enabled || !Settings.canDrawOverlays(context)) return;
        Intent summon = new Intent(context, FaerywareGoblinOverlayService.class).setAction(FaerywareGoblinOverlayService.ACTION_SUMMON);
        try { if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(summon); else context.startService(summon); } catch (Exception ignored) {}
    }
}
