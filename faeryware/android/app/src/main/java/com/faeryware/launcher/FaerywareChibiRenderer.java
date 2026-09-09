package com.faeryware.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

final class FaerywareChibiRenderer {
    private FaerywareChibiRenderer() {}

    static Bitmap render(Context context, int index, int sizePx) {
        int size = Math.max(96, sizePx);
        int i = Math.floorMod(index, 6);
        int accent = FaerywareMemoryStore.faeColor(i);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float s = size / 256f;
        p.setColor(Color.argb(72, Color.red(accent), Color.green(accent), Color.blue(accent)));
        c.drawCircle(128*s, 128*s, 118*s, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(165, 235, 245, 255));
        Path leftWing = new Path();
        leftWing.moveTo(75*s, 118*s); leftWing.cubicTo(28*s, 60*s, 10*s, 92*s, 58*s, 146*s); leftWing.cubicTo(18*s, 158*s, 30*s, 186*s, 78*s, 155*s); leftWing.close(); c.drawPath(leftWing, p);
        Path rightWing = new Path();
        rightWing.moveTo(181*s, 118*s); rightWing.cubicTo(228*s, 60*s, 246*s, 92*s, 198*s, 146*s); rightWing.cubicTo(238*s, 158*s, 226*s, 186*s, 178*s, 155*s); rightWing.close(); c.drawPath(rightWing, p);
        p.setColor(darken(accent, 0.72f)); c.drawCircle(128*s, 124*s, 82*s, p);
        p.setColor(Color.rgb(246, 207, 214));
        Path le = new Path(); le.moveTo(67*s, 108*s); le.lineTo(24*s, 91*s); le.lineTo(67*s, 138*s); le.close(); c.drawPath(le, p);
        Path re = new Path(); re.moveTo(189*s, 108*s); re.lineTo(232*s, 91*s); re.lineTo(189*s, 138*s); re.close(); c.drawPath(re, p);
        p.setColor(Color.rgb(255, 222, 224)); c.drawOval(new RectF(67*s, 70*s, 189*s, 194*s), p);
        p.setColor(accent); c.drawArc(new RectF(65*s, 51*s, 191*s, 154*s), 180, 180, true, p);
        Path bangs = new Path(); bangs.moveTo(74*s, 91*s); bangs.quadTo(94*s, 56*s, 110*s, 100*s); bangs.quadTo(130*s, 57*s, 143*s, 104*s); bangs.quadTo(166*s, 67*s, 184*s, 104*s); bangs.lineTo(184*s, 79*s); bangs.close(); c.drawPath(bangs, p);
        p.setStrokeWidth(5*s); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(Color.rgb(44, 25, 48)); p.setStyle(Paint.Style.STROKE);
        if (i == 0) { c.drawLine(91*s, 129*s, 108*s, 129*s, p); p.setStyle(Paint.Style.FILL); c.drawCircle(156*s, 128*s, 7*s, p); }
        else if (i == 2 || i == 5) { c.drawArc(new RectF(84*s, 120*s, 111*s, 139*s), 200, 140, false, p); c.drawArc(new RectF(145*s, 120*s, 172*s, 139*s), 200, 140, false, p); }
        else { p.setStyle(Paint.Style.FILL); c.drawCircle(100*s, 129*s, i == 3 ? 8*s : 6*s, p); c.drawCircle(156*s, 129*s, i == 3 ? 8*s : 6*s, p); p.setColor(Color.WHITE); c.drawCircle(98*s, 126*s, 2*s, p); c.drawCircle(154*s, 126*s, 2*s, p); }
        p.setColor(Color.argb(90, 255, 80, 140)); c.drawCircle(91*s, 151*s, 8*s, p); c.drawCircle(165*s, 151*s, 8*s, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4*s); p.setColor(Color.rgb(110, 35, 70));
        if (i == 4) c.drawLine(119*s, 159*s, 137*s, 159*s, p); else c.drawArc(new RectF(115*s, 151*s, 141*s, 174*s), 12, 156, false, p);
        p.setStyle(Paint.Style.FILL); p.setColor(darken(accent, 0.63f));
        Path body = new Path(); body.moveTo(93*s, 185*s); body.quadTo(128*s, 211*s, 163*s, 185*s); body.lineTo(183*s, 236*s); body.lineTo(73*s, 236*s); body.close(); c.drawPath(body, p);
        String[] sigils = {"♥","⬡","✦","⌁","◇","☾"}; p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(28*s); c.drawText(sigils[i], 128*s, 221*s, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5*s); p.setColor(Color.argb(230, 255, 255, 255)); c.drawCircle(128*s, 128*s, 115*s, p);
        return b;
    }

    private static int darken(int color, float factor) {
        return Color.rgb(Math.max(0, Math.min(255, Math.round(Color.red(color) * factor))), Math.max(0, Math.min(255, Math.round(Color.green(color) * factor))), Math.max(0, Math.min(255, Math.round(Color.blue(color) * factor))));
    }
}
