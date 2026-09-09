package com.faeryware.launcher;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public final class FaerywareWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new EngineImpl(); }

    private final class EngineImpl extends Engine {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        private final Runnable draw = new Runnable(){@Override public void run(){drawFrame();}};
        private boolean visible;

        @Override public void onVisibilityChanged(boolean v){ visible=v; if(v) drawFrame(); else handler.removeCallbacks(draw); }
        @Override public void onSurfaceChanged(SurfaceHolder h,int f,int w,int he){super.onSurfaceChanged(h,f,w,he);drawFrame();}
        @Override public void onSurfaceDestroyed(SurfaceHolder h){super.onSurfaceDestroyed(h);visible=false;handler.removeCallbacks(draw);}

        private void drawFrame(){
            SurfaceHolder holder=getSurfaceHolder(); Canvas c=null;
            try{
                c=holder.lockCanvas(); if(c==null)return;
                long now=System.currentTimeMillis(); int w=c.getWidth(),h=c.getHeight();
                c.drawColor(Color.rgb(3,3,8));
                int[] colors={0xffff4e9d,0xff42dc76,0xfff6cd5c,0xff46d3ff,0xffcb57ff,0xff5a6ce6};
                int active=(int)((now/3_600_000L)%6); float cx=w/2f, cy=h*0.42f;
                paint.setColor(colors[active]); paint.setAlpha(110); c.drawCircle(cx,cy,Math.min(w,h)*0.28f,paint);
                for(int i=0;i<6;i++){
                    double a=now/4200.0+i*Math.PI*2/6; float r=Math.min(w,h)*0.34f;
                    paint.setColor(colors[i]); paint.setAlpha(i==active?255:155);
                    c.drawCircle(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r, i==active?22f:13f,paint);
                }
                paint.setColor(Color.WHITE); paint.setAlpha(235); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(36f);
                c.drawText("FAERYWARE",cx,h*0.78f,paint);
                paint.setTextSize(18f); paint.setAlpha(190); c.drawText("something lives in the wallpaper",cx,h*0.82f,paint);
            }finally{if(c!=null)holder.unlockCanvasAndPost(c);}
            handler.removeCallbacks(draw); if(visible)handler.postDelayed(draw,1000L);
        }
    }
}
