package com.faeryware.launcher;

import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class FaerywareTileService extends TileService {
    @Override public void onStartListening(){super.onStartListening();Tile t=getQsTile();if(t==null)return;String h=getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS,MODE_PRIVATE).getString("haunt","HAUNTED");t.setLabel("Fae • "+h);t.setState(Tile.STATE_ACTIVE);t.updateTile();}
    @Override public void onClick(){super.onClick();android.content.SharedPreferences p=getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS,MODE_PRIVATE);String h=p.getString("haunt","HAUNTED");String n="CALM".equals(h)?"HAUNTED":("HAUNTED".equals(h)?"FERAL":("FERAL".equals(h)?"APOCALYPSE":"CALM"));p.edit().putString("haunt",n).apply();if(Settings.canDrawOverlays(this)&&p.getBoolean(FaerywareMemoryStore.KEY_RESIDENT_ENABLED,false)){Intent i=new Intent(this,FaerywareGoblinOverlayService.class).setAction(FaerywareGoblinOverlayService.ACTION_REFRESH);try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception ignored){}}FaerywareWidgetProvider.refreshAll(this);onStartListening();}
}
