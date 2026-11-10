package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahsenkurtoski.uniontvos.BuildConfig;
import com.ahsenkurtoski.uniontvos.MainActivity;
import com.ahsenkurtoski.uniontvos.R;

/** Project identity and credits. */
public class AboutScreen extends Screen {

    private FocusCard closeButton;

    public AboutScreen(MainActivity host) {
        super(host);
        build();
    }

    private void build() {
        Context c = getContext();
        removeAllViews();

        ScrollView scroller = new ScrollView(c);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setClipToPadding(false);
        scroller.setClipChildren(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setFocusable(false);

        LinearLayout content = Ui.column(c);
        content.setPadding(Theme.dp(28), Theme.dp(22), Theme.dp(28), Theme.dp(30));
        scroller.addView(content, new ScrollView.LayoutParams(Ui.MATCH, Ui.WRAP));
        addView(scroller, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        content.addView(Chrome.header(host, getString(R.string.about_title),
                getString(R.string.about_role)), Ui.lp(Ui.MATCH, Theme.dp(50)));

        // Brand lockup.
        LinearLayout brand = Ui.column(c);
        brand.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView mark = new ImageView(c);
        Bitmap icon = Ui.vectorBitmap(c, R.drawable.ic_info, Theme.dp(26), Theme.accent);
        if (icon != null) {
            mark.setImageBitmap(icon);
        }
        brand.addView(mark, Ui.lp(Theme.dp(26), Theme.dp(26)));

        LinearLayout wordmark = Ui.row(c);
        wordmark.setGravity(Gravity.CENTER);
        TextView union = Ui.medium(c, getString(R.string.brand_union), 22f, Theme.TEXT);
        union.setLetterSpacing(0.18f);
        TextView tv = Ui.medium(c, getString(R.string.brand_tv), 22f, Theme.accent);
        tv.setLetterSpacing(0.18f);
        wordmark.addView(union);
        wordmark.addView(tv);
        LinearLayout.LayoutParams wordParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        wordParams.topMargin = Theme.dp(10);
        brand.addView(wordmark, wordParams);

        TextView sub = Ui.text(c, getString(R.string.brand_sub), 9f, Theme.TEXT_FAINT);
        sub.setLetterSpacing(0.5f);
        LinearLayout.LayoutParams subParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        subParams.topMargin = Theme.dp(6);
        brand.addView(sub, subParams);

        View rule = new View(c);
        rule.setBackground(Ui.round(Theme.accent, 999f));
        LinearLayout.LayoutParams ruleParams = Ui.lp(Theme.dp(44), Theme.dp(2.5f));
        ruleParams.topMargin = Theme.dp(20);
        brand.addView(rule, ruleParams);

        LinearLayout.LayoutParams brandParams = Ui.lp(Ui.MATCH, Ui.WRAP);
        brandParams.topMargin = Theme.dp(22);
        content.addView(brand, brandParams);

        // Creator credit.
        LinearLayout credit = Ui.column(c);
        credit.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView creditLabel = Ui.text(c, getString(R.string.credit_label), 10.5f, Theme.TEXT_DIM);
        creditLabel.setLetterSpacing(0.14f);
        credit.addView(creditLabel);
        TextView creditName = Ui.medium(c, getString(R.string.brand_name_caps), 17f, Theme.TEXT);
        creditName.setLetterSpacing(0.24f);
        LinearLayout.LayoutParams nameParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        nameParams.topMargin = Theme.dp(8);
        credit.addView(creditName, nameParams);

        LinearLayout.LayoutParams creditParams = Ui.lp(Ui.MATCH, Ui.WRAP);
        creditParams.topMargin = Theme.dp(24);
        content.addView(credit, creditParams);

        TextView description = Ui.text(c, getString(R.string.about_description), 12.5f, Theme.TEXT_DIM);
        description.setLineSpacing(Theme.dpf(5), 1f);
        description.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams descParams = Ui.lp(Theme.dp(620), Ui.WRAP);
        descParams.gravity = Gravity.CENTER_HORIZONTAL;
        descParams.topMargin = Theme.dp(24);
        content.addView(description, descParams);

        LinearLayout meta = Chrome.panel(c);
        LinearLayout.LayoutParams metaParams = Ui.lp(Theme.dp(620), Ui.WRAP);
        metaParams.gravity = Gravity.CENTER_HORIZONTAL;
        metaParams.topMargin = Theme.dp(22);
        addRow(meta, c, R.string.about_version_label, BuildConfig.VERSION_NAME);
        addRow(meta, c, R.string.about_compat_label, c.getString(R.string.about_compat_value));
        addRow(meta, c, R.string.about_build_label, BuildConfig.BUILD_TYPE);
        addRow(meta, c, R.string.about_package_label, c.getPackageName());
        content.addView(meta, metaParams);

        TextView disclaimer = Ui.text(c, getString(R.string.about_disclaimer), 10.5f, Theme.TEXT_FAINT);
        disclaimer.setLineSpacing(Theme.dpf(4), 1f);
        disclaimer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams disclaimerParams = Ui.lp(Theme.dp(600), Ui.WRAP);
        disclaimerParams.gravity = Gravity.CENTER_HORIZONTAL;
        disclaimerParams.topMargin = Theme.dp(20);
        content.addView(disclaimer, disclaimerParams);

        TextView footer = Ui.text(c, getString(R.string.credit_line), 10f, Theme.TEXT_FAINT);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = Ui.lp(Ui.MATCH, Ui.WRAP);
        footerParams.topMargin = Theme.dp(24);
        content.addView(footer, footerParams);

        closeButton = new FocusCard(c);
        closeButton.setRadiusDp(12f);
        closeButton.setFill(Theme.alpha(0xFFFFFFFF, 0.05f));
        TextView close = Ui.medium(c, getString(R.string.action_close), 12.5f, Theme.TEXT);
        close.setGravity(Gravity.CENTER);
        closeButton.addView(close, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.onBackPressed();
            }
        });
        LinearLayout.LayoutParams closeParams = Ui.lp(Theme.dp(160), Theme.dp(44));
        closeParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeParams.topMargin = Theme.dp(26);
        content.addView(closeButton, closeParams);

        initialFocus = closeButton;
    }

    private void addRow(LinearLayout panel, Context c, int labelRes, CharSequence value) {
        if (panel.getChildCount() > 0) {
            panel.addView(Chrome.divider(c), Ui.lp(Ui.MATCH, Math.max(1, Theme.dp(0.7f))));
        }
        panel.addView(Chrome.infoRow(c, c.getString(labelRes), value));
    }
}
