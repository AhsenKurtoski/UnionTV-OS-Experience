package com.ahsenkurtoski.uniontvos.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * Draws the four ambient backgrounds procedurally.
 *
 * Rendering at half resolution and letting the GPU scale the bitmap up keeps
 * memory around 0.9 MB for the entire background while producing smooth
 * gradients — no large image assets ship with the app.
 */
public final class GlowBackground {

    private GlowBackground() {
    }

    public static final int COUNT = 4;

    private static final int[] BASE = {
            0xFF04060B,
            0xFF07050E,
            0xFF06080C,
            0xFF03080A
    };

    private static final int[][] GLOWS = {
            {0xFF1D3A8A, 0xFF4C1D95},
            {0xFF4C1D95, 0xFF1E40AF},
            {0xFF243244, 0xFF131C2B},
            {0xFF0F6B63, 0xFF1E3A8A}
    };

    public static Bitmap create(int index, int width, int height) {
        int safeIndex = Math.max(0, Math.min(BASE.length - 1, index));
        int w = Math.max(2, width);
        int h = Math.max(2, height);

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setShader(null);
        paint.setColor(BASE[safeIndex]);
        canvas.drawRect(0, 0, w, h, paint);

        int[] glows = GLOWS[safeIndex];

        paint.setShader(new RadialGradient(
                w * 0.16f, h * 0.02f, Math.max(w, h) * 0.72f,
                new int[]{Theme.alpha(glows[0], 0.42f), Theme.alpha(glows[0], 0.10f), 0x00000000},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setShader(new RadialGradient(
                w * 0.94f, h * 0.92f, Math.max(w, h) * 0.62f,
                new int[]{Theme.alpha(glows[1], 0.36f), Theme.alpha(glows[1], 0.06f), 0x00000000},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        // Bottom vignette keeps text and cards readable above the glow.
        paint.setShader(new LinearGradient(0, h * 0.35f, 0, h,
                new int[]{0x00000000, 0x9903050A, 0xE605070B},
                new float[]{0f, 0.72f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        return bitmap;
    }
}
