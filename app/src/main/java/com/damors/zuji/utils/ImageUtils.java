package com.damors.zuji.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

public class ImageUtils {
    public static Bitmap getBitmap(Context context, int vectorDrawableId) {
        final VectorDrawableCompat drawable = VectorDrawableCompat.create(context.getResources(), vectorDrawableId, null);
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
