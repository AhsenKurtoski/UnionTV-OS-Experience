package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;

/**
 * Central design tokens for UnionTV OS Experience.
 *
 * The palette is deliberately narrow: one near-black canvas, two surface tones,
 * three text tones and a single accent that the user can switch at runtime.
 * Everything is expressed in dp so the 720p panel scales correctly on the
 * television (tvdpi) and on any denser TV panel.
 */
public final class Theme {

    private Theme() {
    }

    /** Accent presets: blue, violet, aqua, amber. */
    public static final int[] ACCENTS = {
            0xFF4C7DFF,
            0xFF9A6BFF,
            0xFF2FC8D4,
            0xFFFFB454
    };

    public static final int BG = 0xFF05070B;
    public static final int SURFACE = 0xFF0D121C;
    public static final int SURFACE_HI = 0xFF131A27;
    public static final int SURFACE_TOP = 0xFF111827;
    public static final int SURFACE_BOTTOM = 0xFF0A0E16;

    public static final int TEXT = 0xFFF2F5FA;
    public static final int TEXT_DIM = 0xFF9AA5BA;
    public static final int TEXT_FAINT = 0xFF67728A;

    /** Universal online indicator, independent of the chosen accent. */
    public static final int OK = 0xFF3DDC97;

    public static final int BORDER = 0x26FFFFFF;
    public static final int BORDER_SOFT = 0x12FFFFFF;
    public static final int SCRIM = 0xE003050A;

    /** Current accent colour, applied to focus rings, highlights and icons. */
    public static int accent = ACCENTS[0];

    /** When true, focus changes do not scale cards. */
    public static boolean reduceMotion = false;

    private static float density = 1f;
    private static float scaledDensity = 1f;

    public static void init(Context context, int accentColor, boolean reduceMotionEnabled) {
        accent = accentColor;
        reduceMotion = reduceMotionEnabled;
        density = context.getResources().getDisplayMetrics().density;
        scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
    }

    public static int dp(float value) {
        return Math.round(value * density);
    }

    public static float dpf(float value) {
        return value * density;
    }

    public static float spf(float value) {
        return value * scaledDensity;
    }

    /** Replaces the alpha channel of an opaque colour. */
    public static int alpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(255f * alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /** Blends two opaque colours, ratio 0 = a, 1 = b. */
    public static int blend(int a, int b, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int rr = Math.round(ar + (br - ar) * r);
        int rg = Math.round(ag + (bg - ag) * r);
        int rb = Math.round(ab + (bb - ab) * r);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }
}
