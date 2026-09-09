package com.faeryware.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

final class FaerywareStickerAtlas {
    private static Bitmap atlas;
    private static final int CELLS = 6;
    private FaerywareStickerAtlas() {}

    static synchronized Bitmap sticker(Context context, int faeIndex, int stateIndex) {
        if (atlas == null || atlas.isRecycled()) {
            atlas = BitmapFactory.decodeResource(context.getResources(), R.drawable.faeryware_v5_sticker_atlas);
        }
        if (atlas == null) return null;
        int row = Math.floorMod(faeIndex, CELLS);
        int col = Math.floorMod(stateIndex, CELLS);
        int cw = atlas.getWidth() / CELLS;
        int ch = atlas.getHeight() / CELLS;
        return Bitmap.createBitmap(atlas, col * cw, row * ch, cw, ch);
    }
}
