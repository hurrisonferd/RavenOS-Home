package com.faeryware.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public final class FaerywareWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new EngineImpl(); }
    private final class EngineImpl extends Engine {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        private final Runnable draw = new Runnable() { @Override public void run() { drawFrame(); } };
        private boolean visible; private float xOffset = 0.5f; private int cachedFae = -1; private Bitmap cachedPortrait;
        @Override public void onVisibilityChanged(boolean v) { visible = v; if (v) drawFrame(); else handler.removeCallbacks(draw); }
        @Override public void onSurfaceChanged(SurfaceHolder h, int f, int w, int he) { super.onSurfaceChanged(h, f, w, he); drawFrame(); }
        @Override public void onSurfaceDestroyed(SurfaceHolder h) { super.onSurfaceDestroyed(h); visible = false; handler.removeCallbacks(draw); }
        @Override public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) { this.xOffset = xOffset; if (visible) drawFrame(); }
        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder(); Canvas c = null;
            try {
                c = holder.lockCanvas(); if (c == null) return;
                long now = System.currentTimeMillis(); int w = c.getWidth(), h = c.getHeight();
                int fae = FaerywareMemoryStore.activeFaeIndex(FaerywareWallpaperService.this); int color = FaerywareMemoryStore.faeColor(fae);
                c.drawColor(Color.rgb(3, 3, 8));
                if (cachedFae != fae || cachedPortrait == null) { cachedPortrait = FaerywareChibiRenderer.render(FaerywareWallpaperService.this, fae, 512); cachedFae = fae; }
                if (cachedPortrait != null) {
                    float scale = Math.max(w / (float)cachedPortrait.getWidth(), h / (float)cachedPortrait.getHeight());
                    int dw = Math.round(cachedPortrait.getWidth() * scale), dh = Math.round(cachedPortrait.getHeight() * scale);
                    int parallax = Math.round((xOffset - 0.5f) * Math.max(0, dw - w)); int left = (w - dw) / 2 - parallax, top = (h - dh) / 2;
                    paint.setAlpha(185); c.drawBitmap(cachedPortrait, new Rect(0, 0, cachedPortrait.getWidth(), cachedPortrait.getHeight()), new Rect(left, top, left + dw, top + dh), paint);
                }
                double phase = now / 1800.0 + fae; paint.setColor(color); paint.setAlpha(95); float pulse = 0.20f + 0.03f * (float)Math.sin(phase); c.drawCircle(w * 0.5f, h * 0.38f, Math.min(w, h) * pulse, paint);
                paint.setColor(Color.argb(190, 0, 0, 0)); c.drawRect(0, h * 0.72f, w, h, paint);
                paint.setColor(Color.WHITE); paint.setAlpha(245); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(34f); c.drawText(FaerywareMemoryStore.faeStamp(fae), w / 2f, h * 0.80f, paint);
                paint.setTextSize(17f); paint.setAlpha(220); c.drawText(FaerywareMemoryStore.whisper(FaerywareWallpaperService.this, fae), w / 2f, h * 0.84f, paint);
                paint.setTextSize(12f); paint.setAlpha(170); c.drawText(FaerywareMemoryStore.summary(FaerywareWallpaperService.this), w / 2f, h * 0.88f, paint);
            } finally { if (c != null) holder.unlockCanvasAndPost(c); }
            handler.removeCallbacks(draw); if (visible) handler.postDelayed(draw, 1400L);
        }
    }
}
