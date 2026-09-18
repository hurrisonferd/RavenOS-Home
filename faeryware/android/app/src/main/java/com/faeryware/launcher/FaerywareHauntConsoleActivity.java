package com.faeryware.launcher;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import com.faeryware.launcher.resident.ResidentEventBus;

public final class FaerywareHauntConsoleActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    @Override protected void onResume(){super.onResume();render();}

    private void render(){
        ScrollView s=new ScrollView(this);s.setBackgroundColor(Color.rgb(4,4,10));
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(28),dp(18),dp(36));s.addView(r);
        int f=FaerywareMemoryStore.activeFaeIndex(this);
        r.addView(t("GHOST HOUSE",28,Color.WHITE,true));
        r.addView(t("DUAL HABITAT • COLONY IN THE WALLS • HOUSE ON THE HOME SCREEN",11,FaerywareMemoryStore.faeColor(f),true),top(4));
        r.addView(t(status(),12,Color.LTGRAY,false),top(18));
        r.addView(b("SUMMON OVERLAY COLONY",v->{if(!Settings.canDrawOverlays(this)){overlaySettings();return;}prefs().edit().putBoolean(FaerywareMemoryStore.KEY_RESIDENT_ENABLED,true).apply();goblin(FaerywareGoblinOverlayService.ACTION_SUMMON);}),top(14));
        r.addView(b("BANISH COLONY",v->goblin(FaerywareGoblinOverlayService.ACTION_BANISH)),top(6));
        r.addView(b(scopeLabel(),v->{String q=FaerywareMemoryStore.overlayScope(this);String n=FaerywareMemoryStore.SCOPE_HOME_ONLY.equals(q)?FaerywareMemoryStore.SCOPE_FOLLOW_ME:FaerywareMemoryStore.SCOPE_HOME_ONLY;prefs().edit().putString(FaerywareMemoryStore.KEY_OVERLAY_SCOPE,n).apply();goblin(FaerywareGoblinOverlayService.ACTION_REFRESH);render();}),top(14));
        r.addView(b(hauntLabel(),v->{goblin(FaerywareGoblinOverlayService.ACTION_MORE_HAUNTED);new Handler(Looper.getMainLooper()).postDelayed(this::render,120);}),top(6));
        r.addView(b("NEXT FAE",v->goblin(FaerywareGoblinOverlayService.ACTION_CYCLE)),top(6));
        r.addView(b("PING HOUSE CELL",v->{ResidentEventBus.publish(this, ResidentEventBus.Type.MANUAL, "ghost_house");Toast.makeText(this,"Resident event sent.",Toast.LENGTH_SHORT).show();render();}),top(6));
        r.addView(b("DRAW OVER OTHER APPS",v->overlaySettings()),top(14));
        r.addView(b("APP AWARENESS (for Home-only + context)",v->safe(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))),top(6));
        r.addView(b("NOTIFICATION-SOURCE AWARENESS",v->safe(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))),top(6));
        r.addView(b(bootLabel(),v->{boolean n=!prefs().getBoolean(FaerywareMemoryStore.KEY_RESIDENT_BOOT,false);prefs().edit().putBoolean(FaerywareMemoryStore.KEY_RESIDENT_BOOT,n).apply();render();}),top(14));
        r.addView(b("LIVE FAE WALLPAPER (optional)",v->wallpaper()),top(6));
        r.addView(b("CLEAR LOCAL CONTEXT MEMORY",v->{FaerywareMemoryStore.clear(this);render();}),top(14));
        r.addView(t("COLONY MODE\n• keep Samsung One UI or another launcher as Home\n• Faeryware draws small resident windows, wallpaper and widgets\n\nHOUSE MODE\n• the pinned iappyx chassis owns Home/grid/dock/widgets\n• the Faeryware Resident cell reads only the bounded resident provider\n• PING HOUSE CELL emits one local event without Accessibility or notification access\n• verified owner-native speech appears only when Omni RV supplies it\n\nCALM 1 resident • HAUNTED 2 • FERAL 3 • APOCALYPSE 6",11,Color.rgb(195,195,215),false),top(20));
        setContentView(s);
    }

    private android.content.SharedPreferences prefs(){return getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS,MODE_PRIVATE);}
    private String status(){int f=FaerywareMemoryStore.activeFaeIndex(this);return "ACTIVE: "+FaerywareMemoryStore.faeStamp(f)+"\nSTATE: "+FaerywareMemoryStore.stateLabel(f,FaerywareMemoryStore.reactionState(this,f))+"\nSCOPE: "+FaerywareMemoryStore.overlayScope(this)+"\nHAUNT: "+prefs().getString("haunt","HAUNTED")+"\nCONTEXT: "+FaerywareMemoryStore.summary(this);}
    private String scopeLabel(){return "OVERLAY SCOPE: "+FaerywareMemoryStore.overlayScope(this);}
    private String hauntLabel(){return "MORE HAUNTED • "+prefs().getString("haunt","HAUNTED");}
    private String bootLabel(){return "START AFTER REBOOT: "+(prefs().getBoolean(FaerywareMemoryStore.KEY_RESIDENT_BOOT,false)?"ON":"OFF");}
    private void overlaySettings(){safe(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));}
    private void goblin(String action){Intent i=new Intent(this,FaerywareGoblinOverlayService.class).setAction(action);try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception e){Toast.makeText(this,"Android held the colony at the boundary.",Toast.LENGTH_SHORT).show();}}
    private void wallpaper(){try{Intent i=new Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);i.putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,FaerywareWallpaperService.class));startActivity(i);}catch(Exception e){safe(new Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}}
    private void safe(Intent i){try{startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    private Button b(String x,View.OnClickListener a){Button b=new Button(this);b.setAllCaps(false);b.setText(x);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setOnClickListener(a);return b;}
    private TextView t(String x,int z,int c,boolean bold){TextView v=new TextView(this);v.setText(x);v.setTextSize(z);v.setTextColor(c);if(bold)v.setTypeface(v.getTypeface(),1);return v;}
    private LinearLayout.LayoutParams top(int m){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(m);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
