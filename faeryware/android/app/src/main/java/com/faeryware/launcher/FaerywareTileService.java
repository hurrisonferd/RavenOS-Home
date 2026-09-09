package com.faeryware.launcher;

import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class FaerywareTileService extends TileService {
    @Override public void onStartListening(){
        super.onStartListening();
        Tile t=getQsTile(); if(t==null)return;
        String h=getSharedPreferences("faeryware_public",MODE_PRIVATE).getString("haunt","HAUNTED");
        t.setLabel("Haunt: "+h); t.setState(Tile.STATE_ACTIVE); t.updateTile();
    }
    @Override public void onClick(){
        super.onClick();
        String h=getSharedPreferences("faeryware_public",MODE_PRIVATE).getString("haunt","HAUNTED");
        String next="CALM".equals(h)?"HAUNTED":("HAUNTED".equals(h)?"FERAL":"CALM");
        getSharedPreferences("faeryware_public",MODE_PRIVATE).edit().putString("haunt",next).apply();
        if(Settings.canDrawOverlays(this)){
            Intent i=new Intent(this,FaerywareGoblinOverlayService.class).setAction(FaerywareGoblinOverlayService.ACTION_MORE_HAUNTED);
            try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception ignored){}
        }
        onStartListening();
    }
}
