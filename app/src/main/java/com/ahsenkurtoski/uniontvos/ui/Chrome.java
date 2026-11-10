package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahsenkurtoski.uniontvos.MainActivity;
import com.ahsenkurtoski.uniontvos.R;

/** Reusable chrome so every sub-screen shares one visual rhythm. */
public final class Chrome {

    private Chrome() {
    }

    public static View header(final MainActivity host, CharSequence title, CharSequence subtitle) {
        Context c = host;
        LinearLayout bar = Ui.row(c);

        FocusCard back = new FocusCard(c);
        back.setRadiusDp(13f);
        back.setFocusScale(1.08f);
        back.setFill(Theme.alpha(0xFFFFFFFF, 0.06f));
        ImageView chevron = new ImageView(c);
        chevron.setImageBitmap(Ui.vectorBitmap(c, R.drawable.ic_back, Theme.dp(18), Theme.TEXT_DIM));
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(Theme.dp(18), Theme.dp(18));
        iconParams.gravity = Gravity.CENTER;
        back.addView(chevron, iconParams);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.onBackPressed();
            }
        });
        back.setContentDescription(c.getString(R.string.back));
        bar.addView(back, Ui.lp(Theme.dp(40), Theme.dp(40)));

        LinearLayout titles = Ui.column(c);
        TextView titleView = Ui.light(c, title, 25f, Theme.TEXT);
        titles.addView(titleView);
        if (subtitle != null && subtitle.length() > 0) {
            TextView subtitleView = Ui.text(c, subtitle, 11f, Theme.TEXT_FAINT);
            LinearLayout.LayoutParams subParams = Ui.lp(Ui.WRAP, Ui.WRAP);
            subParams.topMargin = Theme.dp(4);
            titles.addView(subtitleView, subParams);
        }
        LinearLayout.LayoutParams titleParams = Ui.lpw(0, Ui.WRAP, 1f);
        titleParams.leftMargin = Theme.dp(14);
        bar.addView(titles, titleParams);

        return bar;
    }

    public static TextView sectionTitle(Context c, CharSequence text) {
        TextView view = Ui.medium(c, text, 9.5f, Theme.TEXT_FAINT);
        view.setLetterSpacing(0.22f);
        view.setAllCaps(true);
        LinearLayout.LayoutParams params = Ui.lp(Ui.WRAP, Ui.WRAP);
        params.topMargin = Theme.dp(24);
        params.bottomMargin = Theme.dp(10);
        params.leftMargin = Theme.dp(2);
        view.setLayoutParams(params);
        return view;
    }

    /** Rounded, non-focusable group container. */
    public static LinearLayout panel(Context c) {
        LinearLayout panel = Ui.column(c);
        panel.setBackground(Ui.roundStroke(Theme.alpha(0xFFFFFFFF, 0.035f), 18f, Theme.BORDER_SOFT, 1f));
        panel.setPadding(Theme.dp(18), Theme.dp(4), Theme.dp(18), Theme.dp(4));
        return panel;
    }

    public static View infoRow(Context c, CharSequence label, CharSequence value) {
        LinearLayout row = Ui.row(c);
        row.setPadding(0, Theme.dp(13), 0, Theme.dp(13));

        TextView labelView = Ui.text(c, label, 12.5f, Theme.TEXT_DIM);
        row.addView(labelView, Ui.lpw(0, Ui.WRAP, 1f));

        TextView valueView = Ui.medium(c, value, 13f, Theme.TEXT);
        valueView.setGravity(Gravity.END);
        valueView.setMaxLines(2);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(valueView, Ui.lp(Theme.dp(330), Ui.WRAP));

        return row;
    }

    public static View divider(Context c) {
        View line = new View(c);
        line.setBackgroundColor(Theme.alpha(0xFFFFFFFF, 0.055f));
        return line;
    }

    /** Full-width focusable row with title, description and a trailing value. */
    public static class OptionRow extends FocusCard {

        private final TextView titleView;
        private final TextView valueView;

        public OptionRow(Context c, CharSequence title, CharSequence description) {
            super(c);
            setRadiusDp(14f);
            setFocusScale(1.012f);
            setFill(Theme.SURFACE_TOP, Theme.SURFACE_BOTTOM);

            LinearLayout content = Ui.row(c);
            content.setPadding(Theme.dp(18), Theme.dp(13), Theme.dp(18), Theme.dp(13));

            LinearLayout text = Ui.column(c);
            titleView = Ui.text(c, title, 14f, Theme.TEXT);
            text.addView(titleView);
            if (description != null && description.length() > 0) {
                TextView desc = Ui.text(c, description, 11f, Theme.TEXT_FAINT);
                LinearLayout.LayoutParams descParams = Ui.lp(Ui.WRAP, Ui.WRAP);
                descParams.topMargin = Theme.dp(4);
                text.addView(desc, descParams);
            }
            content.addView(text, Ui.lpw(0, Ui.WRAP, 1f));

            valueView = Ui.medium(c, "", 12f, Theme.TEXT_DIM);
            valueView.setGravity(Gravity.END);
            content.addView(valueView, Ui.lp(Theme.dp(90), Ui.WRAP));

            addView(content, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }

        public void setValue(CharSequence value) {
            valueView.setText(value);
        }

        public void setValueHighlighted(boolean highlighted) {
            valueView.setTextColor(highlighted ? Theme.accent : Theme.TEXT_DIM);
        }

        public TextView titleView() {
            return titleView;
        }
    }

    public static OptionRow optionRow(Context c, CharSequence title, CharSequence description,
                                      CharSequence value, boolean highlighted,
                                      View.OnClickListener listener) {
        OptionRow row = new OptionRow(c, title, description);
        if (value != null) {
            row.setValue(value);
            row.setValueHighlighted(highlighted);
        }
        row.setOnClickListener(listener);
        LinearLayout.LayoutParams params = Ui.lp(Ui.MATCH, Ui.WRAP);
        params.topMargin = Theme.dp(8);
        row.setLayoutParams(params);
        return row;
    }
}
