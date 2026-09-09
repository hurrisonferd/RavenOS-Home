package com.faeryware.launcher;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

public final class FaerywareGoblinOverlayService extends Service {
    static final String ACTION_SUMMON="com.faeryware.launcher.SUMMON";
    static final String ACTION_BANISH="com.faeryware.launcher.BANISH";
    static final String ACTION_MORE_HAUNTED="com.faeryware.launcher.MORE_HAUNTED";
    static final String ACTION_REFRESH="com.faeryware.launcher.REFRESH_GOBLINS";
    static final String ACTION_CYCLE="com.faeryware.launcher.CYCLE_GOBLIN";
    private static final String CH="faeryware_overlay_colony_v5";
    private static final String POS="faeryware_colony_position_v5";
    private static final int ID=6606;
    private final Handler h=new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private ImageView main;
    private final ImageView[] extras=new ImageView[5];
    private TextView handle;
    private WindowManager.LayoutParams mainLp, handleLp;
    private final WindowManager.LayoutParams[] extraLp=new WindowManager.LayoutParams[5];
    private GestureDetector gd;
    private int manualFae=-1, manualState=-1;
    private long lastPerchBucket=-1;
    private final Runnable tick=new Runnable(){public void run(){if(main!=null){refresh();h.postDelayed(this,900);}}};

    @Override public void onCreate(){super.onCreate();wm=getSystemService(WindowManager.class);NotificationManager n=getSystemService(NotificationManager.class);if(n!=null){NotificationChannel c=new NotificationChannel(CH,"Faeryware overlay colony",NotificationManager.IMPORTANCE_LOW);c.setDescription("Visible user-enabled Digi Fae residents over the regular Android launcher and, optionally, other apps.");n.createNotificationChannel(c);}}
    @Override public int onStartCommand(Intent in,int flags,int id){String ac=in==null?ACTION_SUMMON:in.getAction();if(ACTION_BANISH.equals(ac)){prefs().edit().putBoolean(FaerywareMemoryStore.KEY_RESIDENT_ENABLED,false).apply();clear();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}prefs().edit().putBoolean(FaerywareMemoryStore.KEY_RESIDENT_ENABLED,true).apply();if(ACTION_MORE_HAUNTED.equals(ac))cycleHaunt();else if(ACTION_CYCLE.equals(ac)){manualFae=(fae()+1)%6;manualState=0;}startForeground(ID,note());if(main==null)show();refresh();return START_NOT_STICKY;}
    @Override public void onDestroy(){clear();h.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}

    private android.content.SharedPreferences prefs(){return getSharedPreferences(FaerywareMemoryStore.PUBLIC_PREFS,MODE_PRIVATE);}
    private android.content.SharedPreferences pos(){return getSharedPreferences(POS,MODE_PRIVATE);}

    private void show(){
        main=sprite(false);mainLp=overlay(dp(156),dp(156),false);float nx=pos().getFloat("nx",0.79f),ny=pos().getFloat("ny",0.16f);placeNormalized(mainLp,nx,ny,dp(156),dp(156));touchMain();wm.addView(main,mainLp);
        for(int i=0;i<extras.length;i++){extras[i]=sprite(true);extraLp[i]=overlay(dp(104),dp(104),true);wm.addView(extras[i],extraLp[i]);}
        handle=new TextView(this);handle.setText("🦋");handle.setTextSize(20);handle.setGravity(Gravity.CENTER);handle.setTextColor(Color.WHITE);handle.setBackgroundColor(Color.argb(110,8,8,18));handle.setOnClickListener(v->openConsole());handleLp=overlay(dp(42),dp(76),false);int sw=getResources().getDisplayMetrics().widthPixels;handleLp.x=sw-dp(34);handleLp.y=dp(330);wm.addView(handle,handleLp);
        h.post(tick);
    }

    private ImageView sprite(boolean passive){ImageView v=new ImageView(this);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setPadding(0,0,0,0);v.setElevation(dp(12));v.setAlpha(passive?0.92f:0.98f);return v;}
    private WindowManager.LayoutParams overlay(int w,int he,boolean passive){int f=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;if(passive)f|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,he,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,f,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.START;return p;}

    private void touchMain(){gd=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(MotionEvent e){return true;}@Override public boolean onSingleTapConfirmed(MotionEvent e){manualState=(state()+1)%6;drawMain();return true;}@Override public boolean onDoubleTap(MotionEvent e){manualFae=(fae()+1)%6;manualState=0;drawMain();return true;}@Override public void onLongPress(MotionEvent e){openConsole();}});main.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int ox,oy;boolean moved;public boolean onTouch(View v,MotionEvent e){gd.onTouchEvent(e);switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:sx=e.getRawX();sy=e.getRawY();ox=mainLp.x;oy=mainLp.y;moved=false;main.animate().cancel();main.setTranslationX(0);main.setTranslationY(0);return true;case MotionEvent.ACTION_MOVE:float dx=e.getRawX()-sx,dy=e.getRawY()-sy;if(Math.abs(dx)>dp(5)||Math.abs(dy)>dp(5))moved=true;mainLp.x=ox+Math.round(dx);mainLp.y=oy+Math.round(dy);clamp(mainLp,dp(156),dp(156));safeUpdate(main,mainLp);return true;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:if(moved)snapToPerch();return true;}return true;}});}

    private void refresh(){boolean show=FaerywareMemoryStore.shouldShowColony(this);main.setVisibility(show?View.VISIBLE:View.GONE);handle.setVisibility(show?View.VISIBLE:View.GONE);for(ImageView v:extras)v.setVisibility(show?View.VISIBLE:View.GONE);if(!show)return;drawMain();String level=prefs().getString("haunt","HAUNTED");int count="CALM".equals(level)?0:("HAUNTED".equals(level)?1:("FERAL".equals(level)?2:5));for(int i=0;i<extras.length;i++)extras[i].setVisibility(i<count?View.VISIBLE:View.GONE);moveExtras(count);float amp="CALM".equals(level)?1f:("HAUNTED".equals(level)?4f:("FERAL".equals(level)?8f:13f));double t=System.currentTimeMillis()/1200.0;main.animate().translationY((float)Math.sin(t+fae())*dp((int)amp)).translationX((float)Math.sin(t*.61+fae())*dp((int)(amp*.45f))).setDuration(700).start();}
    private void drawMain(){int f=fae(),s=state();android.graphics.Bitmap b=FaerywareStickerAtlas.sticker(this,f,s);if(b!=null)main.setImageBitmap(b);}
    private int fae(){return manualFae>=0?manualFae:FaerywareMemoryStore.activeFaeIndex(this);}
    private int state(){return manualState>=0?manualState:FaerywareMemoryStore.reactionState(this,fae());}

    private void moveExtras(int count){long bucket=System.currentTimeMillis()/8000L;if(bucket==lastPerchBucket)return;lastPerchBucket=bucket;int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;float[][] p={{.03f,.12f},{.72f,.27f},{.06f,.54f},{.73f,.65f},{.38f,.77f},{.52f,.08f},{.16f,.72f},{.76f,.46f}};for(int i=0;i<count;i++){int f=(fae()+i+1)%6,s=Math.floorMod((int)bucket+i+f,6);android.graphics.Bitmap bm=FaerywareStickerAtlas.sticker(this,f,s);if(bm!=null)extras[i].setImageBitmap(bm);float[] a=p[Math.floorMod((int)bucket+i*2, p.length)];extraLp[i].x=Math.round(a[0]*Math.max(1,sw-dp(104)));extraLp[i].y=Math.round(a[1]*Math.max(1,sh-dp(104)));safeUpdate(extras[i],extraLp[i]);extras[i].setScaleX((i%2==0)?1f:.94f);extras[i].setScaleY((i%2==0)?1f:.94f);}}

    private void snapToPerch(){int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;float[][] p={{.03f,.08f},{.68f,.08f},{.03f,.34f},{.68f,.34f},{.03f,.62f},{.68f,.62f},{.34f,.73f},{.34f,.18f}};float cx=(float)mainLp.x/Math.max(1,sw-dp(156)),cy=(float)mainLp.y/Math.max(1,sh-dp(156));float best=Float.MAX_VALUE,bx=cx,by=cy;for(float[] a:p){float dx=cx-a[0],dy=cy-a[1],d=dx*dx+dy*dy;if(d<best){best=d;bx=a[0];by=a[1];}}placeNormalized(mainLp,bx,by,dp(156),dp(156));pos().edit().putFloat("nx",bx).putFloat("ny",by).apply();safeUpdate(main,mainLp);}
    private void placeNormalized(WindowManager.LayoutParams lp,float nx,float ny,int w,int he){int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;lp.x=Math.round(nx*Math.max(1,sw-w));lp.y=Math.round(ny*Math.max(1,sh-he));clamp(lp,w,he);}
    private void clamp(WindowManager.LayoutParams lp,int w,int he){int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;lp.x=Math.max(-dp(20),Math.min(sw-w+dp(20),lp.x));lp.y=Math.max(0,Math.min(sh-he,lp.y));}
    private void safeUpdate(View v,WindowManager.LayoutParams p){try{wm.updateViewLayout(v,p);}catch(Exception ignored){}}
    private void openConsole(){try{startActivity(new Intent(this,FaerywareHauntConsoleActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception ignored){}}

    private void cycleHaunt(){String q=prefs().getString("haunt","HAUNTED");String n="CALM".equals(q)?"HAUNTED":("HAUNTED".equals(q)?"FERAL":("FERAL".equals(q)?"APOCALYPSE":"CALM"));prefs().edit().putString("haunt",n).apply();}
    private Notification note(){PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,FaerywareHauntConsoleActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE),cycle=PendingIntent.getService(this,2,new Intent(this,getClass()).setAction(ACTION_CYCLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE),more=PendingIntent.getService(this,3,new Intent(this,getClass()).setAction(ACTION_MORE_HAUNTED),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE),ban=PendingIntent.getService(this,4,new Intent(this,getClass()).setAction(ACTION_BANISH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,CH).setSmallIcon(R.drawable.ic_faeryware).setContentTitle("Faeryware colony • "+FaerywareMemoryStore.faeName(fae())).setContentText(prefs().getString("haunt","HAUNTED")+" • "+FaerywareMemoryStore.overlayScope(this)).setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(Icon.createWithResource(this,R.drawable.ic_faeryware),"Next Fae",cycle).build()).addAction(new Notification.Action.Builder(Icon.createWithResource(this,R.drawable.ic_faeryware),"More Haunted",more).build()).addAction(new Notification.Action.Builder(Icon.createWithResource(this,R.drawable.ic_faeryware),"Banish",ban).build()).build();}
    private void clear(){h.removeCallbacks(tick);if(wm!=null){try{if(main!=null)wm.removeView(main);}catch(Exception ignored){}for(ImageView v:extras)try{if(v!=null)wm.removeView(v);}catch(Exception ignored){}try{if(handle!=null)wm.removeView(handle);}catch(Exception ignored){}}main=null;handle=null;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
