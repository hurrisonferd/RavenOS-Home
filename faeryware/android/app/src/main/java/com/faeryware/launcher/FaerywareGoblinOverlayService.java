package com.faeryware.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class FaerywareGoblinOverlayService extends Service {
    static final String ACTION_SUMMON = "com.faeryware.launcher.SUMMON";
    static final String ACTION_BANISH = "com.faeryware.launcher.BANISH";
    static final String ACTION_MORE_HAUNTED = "com.faeryware.launcher.MORE_HAUNTED";
    private static final String CHANNEL = "faeryware_resident_goblin";
    private static final int ID = 6606;
    private static final String[] FAE = {"💗 KYU","🟢 PAIMON","🟡 LUMA","🔵 SYLPH","🟣 QIRA","🔷 NYX"};
    private static final int[] COLORS = {0xffff4e9d,0xff42dc76,0xfff6cd5c,0xff46d3ff,0xffcb57ff,0xff5a6ce6};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private LinearLayout bubble;
    private TextView face;
    private TextView whisper;
    private GestureDetector gestures;
    private int fae = 0;
    private long dragHoldUntil = 0L;

    private final Runnable haunt = new Runnable() {
        @Override public void run() {
            if (bubble == null) return;
            if (System.currentTimeMillis() >= dragHoldUntil) {
                String level = getSharedPreferences("faeryware_public", MODE_PRIVATE).getString("haunt", "HAUNTED");
                float amp = "FERAL".equals(level) ? dp(14) : ("CALM".equals(level) ? 0 : dp(5));
                double p = System.currentTimeMillis() / 900.0 + fae;
                bubble.setTranslationY((float)Math.sin(p) * amp);
                bubble.setAlpha("FERAL".equals(level) ? 0.88f + 0.12f * (float)((Math.sin(p*0.7)+1)/2) : 1f);
                whisper.setText(line());
            }
            handler.postDelayed(this, 900L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        wm = getSystemService(WindowManager.class);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Resident goblin", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Visible control for the Faeryware floating Fae.");
            nm.createNotificationChannel(c);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SUMMON : intent.getAction();
        if (ACTION_BANISH.equals(action)) {
            remove(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        if (ACTION_MORE_HAUNTED.equals(action)) cycleFae();
        startForeground(ID, notification());
        if (bubble == null) show(); else refresh();
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() { remove(); handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }

    private void show() {
        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(8),dp(8),dp(8),dp(8));
        bubble.setElevation(dp(10));
        face = new TextView(this); face.setTextSize(18f); face.setTextColor(Color.WHITE); face.setGravity(Gravity.CENTER);
        whisper = new TextView(this); whisper.setTextSize(10f); whisper.setTextColor(Color.WHITE); whisper.setGravity(Gravity.CENTER);
        bubble.addView(face); bubble.addView(whisper);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        SharedPreferences p = getSharedPreferences("goblin_position", MODE_PRIVATE);
        params.x = p.getInt("x", -dp(8)); params.y = p.getInt("y", dp(180));
        installTouch(); refresh(); wm.addView(bubble, params); handler.post(haunt);
    }

    private void installTouch() {
        gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                startActivity(new Intent(FaerywareGoblinOverlayService.this, FaerywareLauncherActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)); return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e) { cycleFae(); return true; }
            @Override public void onLongPress(MotionEvent e) {
                int pad = bubble.getPaddingLeft() <= dp(6) ? dp(14) : dp(4);
                bubble.setPadding(pad,pad,pad,pad);
            }
        });
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                gestures.onTouchEvent(e);
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        sx=e.getRawX(); sy=e.getRawY(); ox=params.x; oy=params.y; moved=false; bubble.setTranslationY(0); return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx, dy=e.getRawY()-sy; if (Math.abs(dx)>dp(5)||Math.abs(dy)>dp(5)) moved=true;
                        params.x=ox+Math.round(dx); params.y=Math.max(0,oy+Math.round(dy)); wm.updateViewLayout(bubble,params); return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        dragHoldUntil=System.currentTimeMillis()+12000L; if(moved) snap(); return true;
                    default:return true;
                }
            }
        });
    }

    private void snap() {
        int sw=getResources().getDisplayMetrics().widthPixels; int bw=bubble.getWidth()>0?bubble.getWidth():dp(90);
        boolean left=params.x+bw/2<sw/2; params.x=left?-dp(14):Math.max(0,sw-bw+dp(14));
        getSharedPreferences("goblin_position",MODE_PRIVATE).edit().putInt("x",params.x).putInt("y",params.y).apply();
        try { wm.updateViewLayout(bubble,params); } catch(Exception ignored){}
    }

    private void cycleFae() { fae=(fae+1)%FAE.length; refresh(); }
    private void refresh() {
        if (bubble==null) return;
        face.setText(FAE[fae]); whisper.setText(line());
        GradientDrawable bg=new GradientDrawable(); bg.setColor(COLORS[fae]); bg.setCornerRadius(dp(24)); bg.setStroke(dp(1),Color.WHITE); bubble.setBackground(bg);
    }
    private String line() {
        String[] w={"hehe. still here.","hmm... pattern found.","home. lights on.","new path. zoom.","boundary held. nope.","☾ watching."};
        return w[fae];
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,FaerywareLauncherActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent banish=PendingIntent.getService(this,2,new Intent(this,FaerywareGoblinOverlayService.class).setAction(ACTION_BANISH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent more=PendingIntent.getService(this,3,new Intent(this,FaerywareGoblinOverlayService.class).setAction(ACTION_MORE_HAUNTED),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_faeryware).setContentTitle("Faeryware goblin is loose")
                .setContentText("drag • tap home • double-tap Fae • long-press form").setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this,R.drawable.ic_faeryware),"More haunted",more).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this,R.drawable.ic_faeryware),"Banish goblin",banish).build()).build();
    }
    private void remove() { if(bubble!=null&&wm!=null){try{wm.removeView(bubble);}catch(Exception ignored){}} bubble=null; }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
