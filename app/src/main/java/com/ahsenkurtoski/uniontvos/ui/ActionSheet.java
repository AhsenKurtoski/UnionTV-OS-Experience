package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahsenkurtoski.uniontvos.MainActivity;
import com.ahsenkurtoski.uniontvos.R;
import com.ahsenkurtoski.uniontvos.core.AppEntry;
import com.ahsenkurtoski.uniontvos.core.AppRepository;

import java.util.ArrayList;
import java.util.List;

/** Long-press menu for an application tile. */
public final class ActionSheet extends FrameLayout {

    private final List<FocusCard> actions = new ArrayList<FocusCard>();

    public static void show(MainActivity host, AppEntry entry) {
        View returnFocus = host.getWindow().getDecorView().findFocus();
        ActionSheet sheet = new ActionSheet(host, entry);
        host.showOverlay(sheet, returnFocus);
    }

    private ActionSheet(final MainActivity host, final AppEntry entry) {
        super(host);
        setBackgroundColor(Theme.SCRIM);
        setClickable(true);
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.hideOverlay();
            }
        });

        Context c = host;
        LinearLayout card = Ui.column(c);
        card.setBackground(Ui.roundStroke(0xFF10151F, 22f, Theme.BORDER, 1f));
        card.setPadding(Theme.dp(20), Theme.dp(20), Theme.dp(20), Theme.dp(14));
        card.setClickable(true);

        LinearLayout titleRow = Ui.row(c);
        ImageView icon = new ImageView(c);
        Bitmap bitmap = entry.icon != null ? entry.icon
                : Ui.letterAvatar(c, entry.label, Theme.dp(38));
        icon.setImageBitmap(bitmap);
        titleRow.addView(icon, Ui.lp(Theme.dp(38), Theme.dp(38)));

        LinearLayout titles = Ui.column(c);
        TextView name = Ui.medium(c, entry.label, 15f, Theme.TEXT);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView packageName = Ui.text(c, entry.packageName, 10f, Theme.TEXT_FAINT);
        titles.addView(name);
        LinearLayout.LayoutParams packageParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        packageParams.topMargin = Theme.dp(3);
        titles.addView(packageName, packageParams);
        LinearLayout.LayoutParams titleParams = Ui.lpw(0, Ui.WRAP, 1f);
        titleParams.leftMargin = Theme.dp(12);
        titleRow.addView(titles, titleParams);
        card.addView(titleRow, Ui.lp(Ui.MATCH, Ui.WRAP));

        addAction(host, card, c.getString(R.string.action_open), new Runnable() {
            @Override
            public void run() {
                host.hideOverlay();
                host.repository().markUsed(entry);
                if (!AppRepository.launch(host, entry)) {
                    Ui.toast(host, host.getString(R.string.launch_failed, entry.label));
                }
            }
        }, true);

        String favoriteLabel = entry.favorite
                ? c.getString(R.string.action_remove_favorite)
                : c.getString(R.string.action_add_favorite);
        addAction(host, card, favoriteLabel, new Runnable() {
            @Override
            public void run() {
                host.prefs().toggleFavorite(entry.key());
                host.repository().syncFavorites();
                host.notifyAppsChanged();
                host.hideOverlay();
            }
        }, false);

        addAction(host, card, c.getString(R.string.action_info), new Runnable() {
            @Override
            public void run() {
                host.hideOverlay();
                openAppInfo(host, entry);
            }
        }, false);

        addAction(host, card, c.getString(R.string.action_close), new Runnable() {
            @Override
            public void run() {
                host.hideOverlay();
            }
        }, false);

        // Keep focus cycling inside the sheet.
        for (int i = 0; i < actions.size(); i++) {
            FocusCard current = actions.get(i);
            FocusCard next = actions.get((i + 1) % actions.size());
            FocusCard previous = actions.get((i - 1 + actions.size()) % actions.size());
            current.setNextFocusDownId(next.getId());
            current.setNextFocusUpId(previous.getId());
        }

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(Theme.dp(430), Ui.WRAP);
        cardParams.gravity = Gravity.CENTER;
        addView(card, cardParams);

        actions.get(0).post(new Runnable() {
            @Override
            public void run() {
                actions.get(0).requestFocus();
            }
        });
    }

    private void addAction(final MainActivity host, LinearLayout card, String label,
                           final Runnable action, boolean first) {
        FocusCard row = new FocusCard(host);
        row.setId(View.generateViewId());
        // The sheet can also be opened by touch; focus must still work there.
        row.setFocusableInTouchMode(true);
        row.setRadiusDp(12f);
        row.setFocusScale(1.015f);
        row.setFill(Theme.alpha(0xFFFFFFFF, first ? 0.06f : 0.02f));

        TextView text = Ui.text(host, label, 13.5f, Theme.TEXT_DIM);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setPadding(Theme.dp(16), 0, Theme.dp(16), 0);
        row.addView(text, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        row.addFocusListener(new FocusCard.FocusListener() {
            @Override
            public void onCardFocus(FocusCard focusedCard, boolean hasFocus) {
                text.setTextColor(hasFocus ? Theme.TEXT : Theme.TEXT_DIM);
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });

        LinearLayout.LayoutParams params = Ui.lp(Ui.MATCH, Theme.dp(46));
        params.topMargin = Theme.dp(4);
        card.addView(row, params);
        actions.add(row);
    }

    private static void openAppInfo(MainActivity host, AppEntry entry) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + entry.packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            host.startActivity(intent);
        } catch (Throwable t) {
            Ui.toast(host, host.getString(R.string.info_unavailable));
        }
    }
}
