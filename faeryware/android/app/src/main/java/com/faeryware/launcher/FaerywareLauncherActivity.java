package com.faeryware.launcher;

import android.app.Activity;
import android.app.WallpaperManager;
import android.app.role.RoleManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FaerywareLauncherActivity extends Activity {
    private static final int HOME = 0, FEED = 1, HOUSE = 2, APPS = 3;
    private static final String[] FAE = {"KYU", "PAIMON", "LUMA", "SYLPH", "QIRA", "NYX"};
    private static final int[] COLORS = {
            Color.rgb(255,78,157), Color.rgb(66,220,118), Color.rgb(246,205,92),
            Color.rgb(70,211,255), Color.rgb(203,87,255), Color.rgb(90,108,230)
    };

    private int page = HOME;
    private LinearLayout content;
    private GestureDetector gestures;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        prefs = getSharedPreferences("faeryware_public", MODE_PRIVATE);
        gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                if (a == null || b == null) return false;
                float dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
                if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > dp(80)) page = dx > 0 ? FEED : HOUSE;
                else if (Math.abs(dy) > dp(90)) page = dy < 0 ? APPS : HOME;
                else return false;
                render();
                return true;
            }
        });
        render();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if (gestures != null) gestures.onTouchEvent(e);
        return super.dispatchTouchEvent(e);
    }

    @Override protected void onResume() { super.onResume(); render(); }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.argb(210, 4, 4, 10));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(28), dp(16), dp(42));
        scroll.addView(content);
        addHeader();
        if (page == HOME) addHome();
        else if (page == FEED) addFeed();
        else if (page == HOUSE) addHouse();
        else addApps();
        content.addView(text("→ HAUNT FEED   ← HOUSE   ↑ APPS   ↓ HOME", 10, Color.LTGRAY, true), top(26));
        setContentView(scroll);
    }

    private void addHeader() {
        int i = activeFaeIndex();
        TextView title = text("FAERYWARE", 30, Color.WHITE, true);
        content.addView(title);
        content.addView(text("ANDROID HAS DEVELOPED A SMALL TENANT PROBLEM", 11, COLORS[i], true), top(2));
    }

    private void addHome() {
        int i = activeFaeIndex();
        TextView hero = text("🧚  " + FAE[i] + "\n" + whisper(i) + "\n\nHAUNT LEVEL: " + prefs.getString("haunt", "HAUNTED"), 24, Color.WHITE, true);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(16), dp(54), dp(16), dp(54));
        hero.setBackgroundColor(Color.argb(150, Color.red(COLORS[i]), Color.green(COLORS[i]), Color.blue(COLORS[i])));
        content.addView(hero, top(18));
        addSearch(false);
        GridLayout grid = grid();
        grid.addView(button("HAUNT FEED", v -> { page = FEED; render(); }));
        grid.addView(button("GHOST HOUSE", v -> { page = HOUSE; render(); }));
        grid.addView(button("APPS", v -> { page = APPS; render(); }));
        grid.addView(button("SUMMON GOBLIN", v -> summonGoblin()));
        grid.addView(button("MORE HAUNTED", v -> moreHaunted()));
        grid.addView(button("BECOME HOME", v -> becomeHome()));
        content.addView(grid, top(14));
        addDock();
    }

    private void addFeed() {
        int i = activeFaeIndex();
        content.addView(text("HAUNT FEED", 25, COLORS[i], true), top(18));
        content.addView(text("One swipe from Home: Fae presence, scratchpad, search, phone talismans.", 12, Color.LTGRAY, false), top(4));
        EditText note = new EditText(this);
        note.setHint("leave a note for whatever lives in the phone...");
        note.setHintTextColor(Color.GRAY);
        note.setTextColor(Color.WHITE);
        note.setText(prefs.getString("scratchpad", ""));
        note.setMinLines(5);
        note.setBackgroundColor(Color.argb(100, 255, 255, 255));
        note.setOnFocusChangeListener((v, focus) -> { if (!focus) prefs.edit().putString("scratchpad", note.getText().toString()).apply(); });
        content.addView(note, top(12));
        content.addView(button("SAVE SCRATCHPAD", v -> {
            prefs.edit().putString("scratchpad", note.getText().toString()).apply();
            Toast.makeText(this, "The house remembers.", Toast.LENGTH_SHORT).show();
        }), top(6));
        addSearch(false);
        GridLayout talismans = grid();
        talismans.addView(button("CAMERA", v -> launchAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)));
        talismans.addView(button("CALENDAR", v -> launchPackage("com.google.android.calendar")));
        talismans.addView(button("CLOCK", v -> launchAction(android.provider.AlarmClock.ACTION_SHOW_ALARMS)));
        talismans.addView(button("SETTINGS", v -> startActivity(new Intent(Settings.ACTION_SETTINGS))));
        content.addView(talismans, top(10));
        content.addView(text("WHISPER: " + whisper(i), 13, COLORS[i], true), top(14));
        addDock();
    }

    private void addHouse() {
        content.addView(text("GHOST HOUSE", 25, Color.WHITE, true), top(18));
        content.addView(text("Everything weird is still user-triggered. Overlay permission is explicit; One UI escape stays visible.", 12, Color.LTGRAY, false), top(4));
        GridLayout grid = grid();
        grid.addView(button("SUMMON GOBLIN", v -> summonGoblin()));
        grid.addView(button("BANISH GOBLIN", v -> goblinAction(FaerywareGoblinOverlayService.ACTION_BANISH)));
        grid.addView(button("MORE HAUNTED", v -> moreHaunted()));
        grid.addView(button("LIVE WALLPAPER", v -> chooseWallpaper()));
        grid.addView(button("PIN WIDGET", v -> pinWidget()));
        grid.addView(button("OVERLAY PERMISSION", v -> openOverlayPermission()));
        grid.addView(button("RETURN TO ONE UI", v -> openHomeSettings()));
        grid.addView(button("ANDROID SETTINGS", v -> startActivity(new Intent(Settings.ACTION_SETTINGS))));
        content.addView(grid, top(12));
    }

    private void addApps() {
        content.addView(text("APP CATACOMBS", 25, Color.WHITE, true), top(18));
        EditText query = addSearch(true);
        GridLayout apps = new GridLayout(this);
        apps.setColumnCount(3);
        content.addView(apps, top(8));
        List<ResolveInfo> all = launcherApps();
        Runnable repaint = () -> {
            apps.removeAllViews();
            String q = query.getText().toString().trim().toLowerCase();
            for (ResolveInfo info : all) {
                String label = String.valueOf(info.loadLabel(getPackageManager()));
                if (!q.isEmpty() && !label.toLowerCase().contains(q)) continue;
                apps.addView(button(label, v -> launch(info)));
            }
        };
        query.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ repaint.run(); }
            public void afterTextChanged(android.text.Editable e){}
        });
        repaint.run();
    }

    private EditText addSearch(boolean localOnly) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        EditText query = new EditText(this);
        query.setHint(localOnly ? "filter installed apps" : "search apps or the web");
        query.setTextColor(Color.WHITE); query.setHintTextColor(Color.GRAY);
        row.addView(query, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (!localOnly) row.addView(button("WEB", v -> {
            String q = query.getText().toString().trim();
            if (!q.isEmpty()) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))));
        }));
        content.addView(row, top(12));
        return query;
    }

    private void addDock() {
        GridLayout dock = grid();
        dock.addView(button("☎ PHONE", v -> launchAction(Intent.ACTION_DIAL)));
        dock.addView(button("✉ MESSAGES", v -> launchAction(Intent.ACTION_SENDTO, Uri.parse("smsto:"))));
        dock.addView(button("◉ CAMERA", v -> launchAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)));
        dock.addView(button("◎ BROWSER", v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))));
        content.addView(dock, top(16));
    }

    private void summonGoblin() {
        if (!Settings.canDrawOverlays(this)) { openOverlayPermission(); return; }
        goblinAction(FaerywareGoblinOverlayService.ACTION_SUMMON);
    }

    private void moreHaunted() {
        String h = prefs.getString("haunt", "HAUNTED");
        String next = "CALM".equals(h) ? "HAUNTED" : ("HAUNTED".equals(h) ? "FERAL" : "CALM");
        prefs.edit().putString("haunt", next).apply();
        goblinAction(FaerywareGoblinOverlayService.ACTION_MORE_HAUNTED);
        Toast.makeText(this, "HAUNT LEVEL → " + next, Toast.LENGTH_SHORT).show();
        render();
    }

    private void goblinAction(String action) {
        Intent i = new Intent(this, FaerywareGoblinOverlayService.class).setAction(action);
        try { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); }
        catch (Exception e) { Toast.makeText(this, "Android held the goblin at the boundary.", Toast.LENGTH_SHORT).show(); }
    }

    private void openOverlayPermission() {
        try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
        catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void becomeHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) { startActivity(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)); return; }
        }
        openHomeSettings();
    }

    private void openHomeSettings() {
        try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
        catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void chooseWallpaper() {
        try {
            Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(this, FaerywareWallpaperService.class));
            startActivity(i);
        } catch (Exception e) { startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)); }
    }

    private void pinWidget() {
        AppWidgetManager m = AppWidgetManager.getInstance(this);
        if (m.isRequestPinAppWidgetSupported()) m.requestPinAppWidget(new ComponentName(this, FaerywareWidgetProvider.class), null, null);
    }

    private List<ResolveInfo> launcherApps() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> r = new ArrayList<>(getPackageManager().queryIntentActivities(i, 0));
        r.sort(Comparator.comparing(a -> String.valueOf(a.loadLabel(getPackageManager())), String.CASE_INSENSITIVE_ORDER));
        return r;
    }

    private void launch(ResolveInfo info) {
        if (info.activityInfo == null) return;
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ignored) {}
    }

    private void launchPackage(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i); else Toast.makeText(this, "App not installed.", Toast.LENGTH_SHORT).show();
    }

    private void launchAction(String action) { launchAction(action, null); }
    private void launchAction(String action, Uri data) {
        Intent i = new Intent(action); if (data != null) i.setData(data);
        try { startActivity(i); } catch (Exception e) { Toast.makeText(this, "No handler found.", Toast.LENGTH_SHORT).show(); }
    }

    private int activeFaeIndex() { return (int)((System.currentTimeMillis() / 3_600_000L) % FAE.length); }
    private String whisper(int i) {
        String[] w = {"hehe. still here.","hmm... pattern found.","home. lights on.","new path. zoom.","boundary held. nope.","☾ watching."};
        return w[i];
    }

    private GridLayout grid() { GridLayout g = new GridLayout(this); g.setColumnCount(2); g.setUseDefaultMargins(true); return g; }
    private Button button(String label, View.OnClickListener action) {
        Button b = new Button(this); b.setAllCaps(false); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(10.5f); b.setOnClickListener(action); return b;
    }
    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD); return t;
    }
    private LinearLayout.LayoutParams top(int dp) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.topMargin = dp(dp); return p; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
