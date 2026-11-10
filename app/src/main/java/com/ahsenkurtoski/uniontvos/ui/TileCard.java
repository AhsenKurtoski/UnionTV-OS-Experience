package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

/** Focusable action tile (Settings, Network, System…) matching the app tile rhythm. */
public class TileCard extends FocusCard {

    private final ImageView iconView;
    private final TextView labelView;
    private final TextView valueView;

    public TileCard(Context context, int iconRes, CharSequence label) {
        super(context);
        setRadiusDp(18f);
        setFocusScale(1.07f);
        setFill(Theme.SURFACE_TOP, Theme.SURFACE_BOTTOM);

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap icon = Ui.vectorBitmap(context, iconRes, Theme.dp(30), Theme.TEXT_DIM);
        if (icon != null) {
            iconView.setImageBitmap(icon);
        }
        LayoutParams iconParams = new LayoutParams(Theme.dp(30), Theme.dp(30));
        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.topMargin = Theme.dp(16);
        iconParams.leftMargin = Theme.dp(16);
        addView(iconView, iconParams);

        labelView = Ui.text(context, label, 12f, Theme.TEXT_DIM);
        labelView.setMaxLines(1);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams labelParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        labelParams.gravity = Gravity.BOTTOM;
        labelParams.bottomMargin = Theme.dp(12);
        labelParams.leftMargin = Theme.dp(15);
        labelParams.rightMargin = Theme.dp(12);
        addView(labelView, labelParams);

        valueView = Ui.text(context, "", 10f, Theme.TEXT_FAINT);
        LayoutParams valueParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        valueParams.gravity = Gravity.TOP;
        valueParams.topMargin = Theme.dp(18);
        valueParams.leftMargin = Theme.dp(52);
        valueParams.rightMargin = Theme.dp(12);
        valueView.setVisibility(GONE);
        addView(valueView, valueParams);

        addFocusListener(new FocusListener() {
            @Override
            public void onCardFocus(FocusCard card, boolean focused) {
                labelView.setTextColor(focused ? Theme.TEXT : Theme.TEXT_DIM);
            }
        });
    }

    public void setValue(CharSequence value) {
        if (value == null || value.length() == 0) {
            valueView.setVisibility(GONE);
            return;
        }
        valueView.setText(value);
        valueView.setVisibility(VISIBLE);
    }

    public void setLabel(CharSequence label) {
        labelView.setText(label);
    }
}
