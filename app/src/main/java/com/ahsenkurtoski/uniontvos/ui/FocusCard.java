package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;

/**
 * The single focusable surface used across the whole interface.
 *
 * Draws a rounded fill, a hairline border and — when focused — an accent ring
 * plus a faint accent tint. Focus is also communicated by a short scale
 * animation (disabled when the user asks for reduced motion).
 *
 * No elevation, no ripple, no shadow: this stays cheap on a 720p ARMv7 GPU.
 */
public class FocusCard extends FrameLayout {

    public interface FocusListener {
        void onCardFocus(FocusCard card, boolean focused);
    }

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF inner = new RectF();

    private int fillTop = Theme.SURFACE;
    private int fillBottom = Theme.SURFACE;
    private float radius;
    private float focusScale = 1.05f;
    private boolean focused;
    private boolean active;
    private final ArrayList<FocusListener> focusListeners = new ArrayList<FocusListener>(2);

    public FocusCard(Context context) {
        super(context);
        radius = Theme.dpf(18f);
        setFocusable(true);
        setClickable(true);
        setWillNotDraw(false);
        setClipToPadding(false);
        setClipChildren(false);

        tintPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                focused = hasFocus;
                applyFocusState(hasFocus);
                invalidate();
                for (int i = 0; i < focusListeners.size(); i++) {
                    focusListeners.get(i).onCardFocus(FocusCard.this, hasFocus);
                }
            }
        });
    }

    public void setRadiusDp(float radiusDp) {
        radius = Theme.dpf(radiusDp);
        invalidate();
    }

    public void setFill(int color) {
        setFill(color, color);
    }

    public void setFill(int top, int bottom) {
        fillTop = top;
        fillBottom = bottom;
        fillPaint.setShader(null);
        if (top != bottom && getWidth() > 0) {
            fillPaint.setShader(new LinearGradient(0, 0, 0, getHeight(), top, bottom, Shader.TileMode.CLAMP));
        }
        invalidate();
    }

    public void setFocusScale(float scale) {
        focusScale = scale;
    }

    public void setFocusListener(FocusListener listener) {
        addFocusListener(listener);
    }

    public void addFocusListener(FocusListener listener) {
        if (listener != null) {
            focusListeners.add(listener);
        }
    }

    /** Persistent selected state (used by accent swatches, sort chips, toggles). */
    public void setActive(boolean isActive) {
        if (active != isActive) {
            active = isActive;
            invalidate();
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCardFocused() {
        return focused;
    }

    private void applyFocusState(boolean hasFocus) {
        if (Theme.reduceMotion) {
            setScaleX(1f);
            setScaleY(1f);
            return;
        }
        float target = hasFocus ? focusScale : 1f;
        animate()
                .scaleX(target)
                .scaleY(target)
                .setDuration(hasFocus ? 120 : 100)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (fillTop != fillBottom) {
            fillPaint.setShader(new LinearGradient(0, 0, 0, h, fillTop, fillBottom, Shader.TileMode.CLAMP));
        } else {
            fillPaint.setShader(null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        rect.set(0, 0, w, h);
        if (fillTop == fillBottom) {
            fillPaint.setShader(null);
            fillPaint.setColor(fillTop);
        }
        canvas.drawRoundRect(rect, radius, radius, fillPaint);

        if (focused) {
            tintPaint.setColor(Theme.alpha(Theme.accent, 0.14f));
            canvas.drawRoundRect(rect, radius, radius, tintPaint);
        } else if (active) {
            tintPaint.setColor(Theme.alpha(Theme.accent, 0.10f));
            canvas.drawRoundRect(rect, radius, radius, tintPaint);
        }

        float strokeWidth = Theme.dpf(focused ? 2.2f : (active ? 1.6f : 1f));
        inner.set(strokeWidth / 2f, strokeWidth / 2f, w - strokeWidth / 2f, h - strokeWidth / 2f);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(focused ? Theme.accent : (active ? Theme.alpha(Theme.accent, 0.75f) : Theme.BORDER_SOFT));
        canvas.drawRoundRect(inner, Math.max(0f, radius - strokeWidth / 2f), Math.max(0f, radius - strokeWidth / 2f), strokePaint);
    }
}
