package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.ahsenkurtoski.uniontvos.core.AppEntry;

/** One application tile: icon, label and a small favourite marker. */
public class AppCard extends FocusCard {

    public static final int WIDTH_DP = 106;
    public static final int HEIGHT_DP = 100;

    private final ImageView iconView;
    private final TextView labelView;
    private final TextView badgeView;
    private final int iconSizePx;

    private AppEntry entry;

    public AppCard(Context context) {
        super(context);
        setRadiusDp(18f);
        setFocusScale(1.07f);
        setFill(Theme.SURFACE_TOP, Theme.SURFACE_BOTTOM);

        iconSizePx = Theme.dp(56);

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams iconParams = new LayoutParams(iconSizePx, iconSizePx);
        iconParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        iconParams.topMargin = Theme.dp(13);
        addView(iconView, iconParams);

        labelView = Ui.text(context, "", 12f, Theme.TEXT_DIM);
        labelView.setGravity(Gravity.CENTER);
        labelView.setMaxLines(1);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams labelParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        labelParams.gravity = Gravity.BOTTOM;
        labelParams.bottomMargin = Theme.dp(11);
        labelParams.leftMargin = Theme.dp(7);
        labelParams.rightMargin = Theme.dp(7);
        addView(labelView, labelParams);

        badgeView = new TextView(context);
        badgeView.setBackground(Ui.round(Theme.accent, 999f));
        LayoutParams badgeParams = new LayoutParams(Theme.dp(7), Theme.dp(7));
        badgeParams.gravity = Gravity.TOP | Gravity.END;
        badgeParams.topMargin = Theme.dp(12);
        badgeParams.rightMargin = Theme.dp(12);
        badgeView.setVisibility(GONE);
        addView(badgeView, badgeParams);

        addFocusListener(new FocusListener() {
            @Override
            public void onCardFocus(FocusCard card, boolean focused) {
                labelView.setTextColor(focused ? Theme.TEXT : Theme.TEXT_DIM);
            }
        });
    }

    public void bind(AppEntry appEntry) {
        entry = appEntry;
        labelView.setText(appEntry.label);
        labelView.setTextColor(isCardFocused() ? Theme.TEXT : Theme.TEXT_DIM);
        Bitmap icon = appEntry.icon;
        if (icon != null) {
            iconView.setImageBitmap(icon);
        } else {
            iconView.setImageBitmap(Ui.letterAvatar(getContext(), appEntry.label, iconSizePx));
        }
        badgeView.setBackground(Ui.round(Theme.accent, 999f));
        badgeView.setVisibility(appEntry.favorite ? VISIBLE : GONE);
    }

    /** Refreshes icon/badge after the repository finishes decoding. */
    public void updateIcon() {
        if (entry == null) {
            return;
        }
        if (entry.icon != null) {
            iconView.setImageBitmap(entry.icon);
        }
        badgeView.setVisibility(entry.favorite ? VISIBLE : GONE);
    }

    public AppEntry entry() {
        return entry;
    }
}
