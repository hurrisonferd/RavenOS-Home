package com.faeryware.launcher;

import android.app.Activity;
import android.app.WallpaperManager;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class FaerywareHauntConsoleActivity extends Activity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); render(); }
    @Override protected void onResume() { super.onResume(); render(); }

    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Color.rgb(4, 4, 10));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18), dp(28), dp(18), dp(36)); scroll.addView(root);
        int fae = FaerywareMemoryStore.activeFaeIndex(this);
        root.addView(text("FAERYWARE HAUNT CONSOLE", 25, Color.WHITE, true));
        root.addView(text("resident presence • explicit permissions • local-only context memory", 11, FaerywareMemoryStore.faeColor(fae), true), top(4));
        root.addView(text(statusText(), 12, Color.LTGRAY, false), top(18));
        root.addView(button("SUMMON RESIDENT FAE", v -> { if (!Settings.canDrawOverlays(this)) { openOverlay(); return; } getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, MODE_PRIVATE).edit().putBoolean(FaerywareMemoryStore.KEY_RESIDENT_ENABLED, true).apply(); startGoblin(FaerywareGoblinOverlayService.ACTION_SUMMON); }), top(14));
        root.addView(button("BANISH RESIDENT FAE", v -> startGoblin(FaerywareGoblinOverlayService.ACTION_BANISH)), top(6));
        root.addView(button("DRAW OVER OTHER APPS", v -> openOverlay()), top(14));
        root.addView(button("ENABLE APP AWARENESS", v -> safeStart(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))), top(6));
        root.addView(button("ENABLE NOTIFICATION AWARENESS", v -> safeStart(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))), top(6));
        root.addView(button(contextLabel(), v -> { android.content.SharedPreferences p = getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, MODE_PRIVATE); boolean next = !p.getBoolean(FaerywareMemoryStore.KEY_CONTEXT_MODE, true); p.edit().putBoolean(FaerywareMemoryStore.KEY_CONTEXT_MODE, next).apply(); Toast.makeText(this, "Context mode " + (next ? "ON" : "OFF"), Toast.LENGTH_SHORT).show(); render(); }), top(14));
        root.addView(button(bootLabel(), v -> { android.content.SharedPreferences p = getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, MODE_PRIVATE); boolean next = !p.getBoolean(FaerywareMemoryStore.KEY_RESIDENT_BOOT, false); p.edit().putBoolean(FaerywareMemoryStore.KEY_RESIDENT_BOOT, next).apply(); Toast.makeText(this, "Reboot residency " + (next ? "ON" : "OFF"), Toast.LENGTH_SHORT).show(); render(); }), top(6));
        root.addView(button("BECOME DEFAULT HOME", v -> becomeHome()), top(14));
        root.addView(button("LIVE WALLPAPER", v -> chooseWallpaper()), top(6));
        root.addView(button("RETURN TO ONE UI / HOME SETTINGS", v -> safeStart(new Intent(Settings.ACTION_HOME_SETTINGS))), top(6));
        root.addView(button("CLEAR LOCAL FAERY MEMORY", v -> { FaerywareMemoryStore.clear(this); Toast.makeText(this, "Local context memory cleared.", Toast.LENGTH_SHORT).show(); render(); }), top(14));
        root.addView(text("PRIVACY CONTRACT\n• app awareness records foreground package/app label only\n• notification awareness records source app + time only\n• no message bodies, keystrokes, passwords, or view text are copied\n• no network sync is used by this memory layer\n• overlay remains visible, draggable, and banishable\n• all special access is granted or revoked in Android Settings", 11, Color.rgb(190,190,210), false), top(20));
        root.addView(text("RECENT LOCAL FOOTPRINT\n" + historyPreview(), 10, Color.rgb(150,150,175), false), top(18));
        setContentView(scroll);
    }

    private String statusText() {
        boolean overlay = Settings.canDrawOverlays(this);
        boolean context = getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, MODE_PRIVATE).getBoolean(FaerywareMemoryStore.KEY_CONTEXT_MODE, true);
        boolean boot = getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, MODE_PRIVATE).getBoolean(FaerywareMemoryStore.KEY_RESIDENT_BOOT, false);
        String enabledAccessibility = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean awareness = enabledAccessibility != null && enabledAccessibility.contains(getPackageName());
        String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean notification = listeners != null && listeners.contains(getPackageName());
        return "ACTIVE FAE: " + FaerywareMemoryStore.faeStamp(FaerywareMemoryStore.activeFaeIndex(this)) + "\n" + FaerywareMemoryStore.summary(this) + "\n\nOVERLAY: " + yesNo(overlay) + "\nAPP AWARENESS: " + yesNo(awareness) + "\nNOTIFICATION AWARENESS: " + yesNo(notification) + "\nCONTEXT MODE: " + yesNo(context) + "\nSTART AFTER REBOOT: " + yesNo(boot);
    }

    private String historyPreview() {
        String raw = FaerywareMemoryStore.history(this);
        if (raw == null || raw.trim().isEmpty()) return "(empty)";
        String[] lines = raw.split("\\n"); StringBuilder out = new StringBuilder(); int start = Math.max(0, lines.length - 8);
        for (int i = start; i < lines.length; i++) { String[] parts = lines[i].split("\\|"); if (parts.length >= 2) { if (out.length() > 0) out.append('\n'); out.append(parts[0]).append(" • ").append(parts[1]); } }
        return out.toString();
    }

    private String contextLabel() { boolean on = getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, MODE_PRIVATE).getBoolean(FaerywareMemoryStore.KEY_CONTEXT_MODE, true); return "CONTEXT MODE: " + (on ? "ON" : "OFF"); }
    private String bootLabel() { boolean on = getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS, MODE_PRIVATE).getBoolean(FaerywareMemoryStore.KEY_RESIDENT_BOOT, false); return "START AFTER REBOOT: " + (on ? "ON" : "OFF"); }
    private String yesNo(boolean value) { return value ? "ON" : "OFF"; }
    private void openOverlay() { safeStart(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
    private void startGoblin(String action) { Intent i = new Intent(this, FaerywareGoblinOverlayService.class).setAction(action); try { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); } catch (Exception e) { Toast.makeText(this, "Android held the goblin at the boundary.", Toast.LENGTH_SHORT).show(); } }
    private void becomeHome() { if (Build.VERSION.SDK_INT >= 29) { RoleManager rm = getSystemService(RoleManager.class); if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) { safeStart(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)); return; } } safeStart(new Intent(Settings.ACTION_HOME_SETTINGS)); }
    private void chooseWallpaper() { try { Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER); i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(this, FaerywareWallpaperService.class)); startActivity(i); } catch (Exception e) { safeStart(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)); } }
    private void safeStart(Intent intent) { try { startActivity(intent); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); } }
    private Button button(String label, View.OnClickListener action) { Button b = new Button(this); b.setAllCaps(false); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(12f); b.setOnClickListener(action); return b; }
    private TextView text(String value, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD); return t; }
    private LinearLayout.LayoutParams top(int margin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(margin); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
