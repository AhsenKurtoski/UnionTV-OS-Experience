package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Small, allocation-conscious UI helpers used by every screen. */
public final class Ui {

    private Ui() {
    }

    public static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    public static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    public static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    public static TextView text(Context c, CharSequence value, float sizeSp, int color) {
        return styled(c, value, sizeSp, color, REGULAR);
    }

    public static TextView light(Context c, CharSequence value, float sizeSp, int color) {
        return styled(c, value, sizeSp, color, LIGHT);
    }

    public static TextView medium(Context c, CharSequence value, float sizeSp, int color) {
        return styled(c, value, sizeSp, color, MEDIUM);
    }

    private static TextView styled(Context c, CharSequence value, float sizeSp, int color, Typeface face) {
        TextView view = new TextView(c);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(face);
        view.setIncludeFontPadding(false);
        return view;
    }

    public static LinearLayout row(Context c) {
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public static LinearLayout column(Context c) {
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    public static LinearLayout.LayoutParams lpw(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }

    public static ViewGroup.MarginLayoutParams margins(View view, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof ViewGroup.MarginLayoutParams)) {
            params = new ViewGroup.MarginLayoutParams(MATCH, WRAP);
        }
        ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) params;
        m.leftMargin = left;
        m.topMargin = top;
        m.rightMargin = right;
        m.bottomMargin = bottom;
        view.setLayoutParams(m);
        return m;
    }

    public static void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            m.leftMargin = left;
            m.topMargin = top;
            m.rightMargin = right;
            m.bottomMargin = bottom;
            view.setLayoutParams(m);
        }
    }

    public static GradientDrawable round(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(Theme.dpf(radiusDp));
        return d;
    }

    public static GradientDrawable roundStroke(int color, float radiusDp, int strokeColor, float strokeDp) {
        GradientDrawable d = round(color, radiusDp);
        d.setStroke(Math.max(1, Theme.dp(strokeDp)), strokeColor);
        return d;
    }

    public static GradientDrawable pill(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(Theme.dpf(999));
        return d;
    }

    /** Renders a vector drawable into a bitmap once, so lists draw bitmaps only. */
    public static Bitmap vectorBitmap(Context c, int resId, int sizePx, int tint) {
        try {
            Drawable d = c.getDrawable(resId);
            if (d == null) {
                return null;
            }
            d = d.mutate();
            d.setTint(tint);
            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            d.setBounds(0, 0, sizePx, sizePx);
            d.draw(canvas);
            d.setTint(-1);
            return bmp;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Simple monogram used while an application icon is loading or unavailable. */
    public static Bitmap letterAvatar(Context c, String label, int sizePx) {
        int[] palette = {0xFF2B3A5C, 0xFF3A2B5C, 0xFF1F4A47, 0xFF4A3B1F};
        int color = palette[Math.abs((label == null ? "" : label).hashCode()) % palette.length];

        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        RectF rect = new RectF(0, 0, sizePx, sizePx);
        canvas.drawRoundRect(rect, sizePx * 0.26f, sizePx * 0.26f, paint);

        String letter = (label == null || label.trim().isEmpty())
                ? "?"
                : label.trim().substring(0, 1).toUpperCase();
        paint.setColor(0xFFEDF1F8);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(MEDIUM);
        paint.setTextSize(sizePx * 0.44f);
        float baseline = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f;
        canvas.drawText(letter, sizePx / 2f, baseline, paint);
        return bmp;
    }

    public static void toast(Context c, CharSequence message) {
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Scrolls a vertical container just enough to make the focused child fully
     * visible. Called explicitly on focus instead of relying on implicit
     * scrolling, which is unreliable inside nested scroll containers.
     */
    public static void scrollIntoView(ScrollView scroller, View child, int topInset, int bottomInset) {
        if (scroller == null || child == null || scroller.getHeight() == 0) {
            return;
        }
        Rect r = new Rect(0, 0, child.getWidth(), child.getHeight());
        try {
            scroller.offsetDescendantRectToMyCoords(child, r);
        } catch (IllegalArgumentException e) {
            return;
        }
        int viewport = scroller.getHeight();
        int current = scroller.getScrollY();
        int target = current;
        if (r.top - topInset < current) {
            target = r.top - topInset;
        } else if (r.bottom + bottomInset > current + viewport) {
            target = r.bottom + bottomInset - viewport;
        }
        if (target != current) {
            View content = scroller.getChildAt(0);
            int max = content == null ? 0 : Math.max(0, content.getHeight() - viewport);
            scroller.smoothScrollTo(0, Math.max(0, Math.min(max, target)));
        }
    }

    /** Horizontal counterpart used by the home rails. */
    public static void scrollIntoView(HorizontalScrollView scroller, View child, int leftInset, int rightInset) {
        if (scroller == null || child == null || scroller.getWidth() == 0) {
            return;
        }
        Rect r = new Rect(0, 0, child.getWidth(), child.getHeight());
        try {
            scroller.offsetDescendantRectToMyCoords(child, r);
        } catch (IllegalArgumentException e) {
            return;
        }
        int viewport = scroller.getWidth();
        int current = scroller.getScrollX();
        int target = current;
        if (r.left - leftInset < current) {
            target = r.left - leftInset;
        } else if (r.right + rightInset > current + viewport) {
            target = r.right + rightInset - viewport;
        }
        if (target != current) {
            View content = scroller.getChildAt(0);
            int max = content == null ? 0 : Math.max(0, content.getWidth() - viewport);
            scroller.smoothScrollTo(Math.max(0, Math.min(max, target)), 0);
        }
    }

    public static void divider(Context c, LinearLayout parent) {
        View line = new View(c);
        line.setBackgroundColor(Theme.alpha(0xFFFFFFFF, 0.06f));
        parent.addView(line, lp(MATCH, Math.max(1, Theme.dp(0.7f))));
    }
}
