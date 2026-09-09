package com.faeryware.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/** Compact V5.1 visual anchor: one Raven-supplied portrait cell per Digi Fae. */
final class FaerywareStickerAtlas {
    private static Bitmap strip;
    private static final int CELLS = 6;

    private FaerywareStickerAtlas() {}

    static synchronized Bitmap sticker(Context context, int faeIndex, int stateIndex) {
        if (strip == null || strip.isRecycled()) {
            strip = BitmapFactory.decodeResource(context.getResources(), R.drawable.fae_six_strip);
        }
        if (strip == null) return null;
        int cell = strip.getWidth() / CELLS;
        int i = Math.floorMod(faeIndex, CELLS);
        return Bitmap.createBitmap(strip, i * cell, 0, cell, strip.getHeight());
    }
}
