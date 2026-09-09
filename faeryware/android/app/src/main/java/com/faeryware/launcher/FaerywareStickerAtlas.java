package com.faeryware.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

final class FaerywareStickerAtlas {
    private static final Bitmap[] CACHE = new Bitmap[6];
    private static final int[] RES = {
        R.drawable.fae_kyu_224,
        R.drawable.fae_paimon_224,
        R.drawable.fae_luma_224,
        R.drawable.fae_sylph_224,
        R.drawable.fae_qira_224,
        R.drawable.fae_nyx_224
    };

    private FaerywareStickerAtlas() {}

    static synchronized Bitmap sticker(Context context, int faeIndex, int stateIndex) {
        int i = Math.floorMod(faeIndex, RES.length);
        Bitmap b = CACHE[i];
        if (b == null || b.isRecycled()) {
            b = BitmapFactory.decodeResource(context.getResources(), RES[i]);
            CACHE[i] = b;
        }
        return b;
    }
}
