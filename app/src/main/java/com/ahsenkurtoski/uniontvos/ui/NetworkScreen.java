package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahsenkurtoski.uniontvos.MainActivity;
import com.ahsenkurtoski.uniontvos.R;
import com.ahsenkurtoski.uniontvos.core.NetworkMonitor;

/** Read-only network overview. */
public class NetworkScreen extends Screen {

    private ScrollView scroller;
    private TextView statusValue;
    private TextView statusDetail;
    private LinearLayout detailPanel;
    private TextView hintView;
    private FocusCard wifiButton;

    public NetworkScreen(MainActivity host) {
        super(host);
        build();
    }

    private void build() {
        Context c = getContext();
        removeAllViews();

        scroller = new ScrollView(c);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setClipToPadding(false);
        scroller.setClipChildren(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setFocusable(false);

        LinearLayout content = Ui.column(c);
        content.setPadding(Theme.dp(28), Theme.dp(22), Theme.dp(28), Theme.dp(32));
        content.setClipChildren(false);
        scroller.addView(content, new ScrollView.LayoutParams(Ui.MATCH, Ui.WRAP));
        addView(scroller, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        View header = Chrome.header(host, getString(R.string.network_title),
                getString(R.string.network_hint));
        content.addView(header, Ui.lp(Ui.MATCH, Theme.dp(50)));

        FrameLayout status = new FrameLayout(c);
        status.setBackground(Ui.roundStroke(Theme.alpha(0xFFFFFFFF, 0.04f), 20f, Theme.BORDER_SOFT, 1f));
        status.setPadding(Theme.dp(24), Theme.dp(20), Theme.dp(24), Theme.dp(20));

        LinearLayout statusText = Ui.column(c);
        TextView caption = Ui.text(c, getString(R.string.network_status), 9.5f, Theme.TEXT_FAINT);
        caption.setLetterSpacing(0.22f);
        statusText.addView(caption);
        statusValue = Ui.light(c, "", 24f, Theme.TEXT);
        LinearLayout.LayoutParams valueParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        valueParams.topMargin = Theme.dp(6);
        statusText.addView(statusValue, valueParams);
        statusDetail = Ui.text(c, "", 12f, Theme.TEXT_DIM);
        LinearLayout.LayoutParams detailParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        detailParams.topMargin = Theme.dp(6);
        statusText.addView(statusDetail, detailParams);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        textParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        status.addView(statusText, textParams);

        content.addView(status, Ui.lp(Ui.MATCH, Theme.dp(116)));

        content.addView(Chrome.sectionTitle(c, getString(R.string.network_title)));
        detailPanel = Chrome.panel(c);
        content.addView(detailPanel, Ui.lp(Ui.MATCH, Ui.WRAP));

        wifiButton = new FocusCard(c);
        wifiButton.setRadiusDp(14f);
        wifiButton.setFocusScale(1.02f);
        wifiButton.setFill(Theme.SURFACE_TOP, Theme.SURFACE_BOTTOM);
        TextView wifiLabel = Ui.medium(c, getString(R.string.network_open_settings), 13f, Theme.TEXT);
        wifiLabel.setGravity(Gravity.CENTER);
        wifiButton.addView(wifiLabel, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        wifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    host.startActivity(intent);
                } catch (Throwable t) {
                    Ui.toast(getContext(), getString(R.string.info_unavailable));
                }
            }
        });
        LinearLayout.LayoutParams buttonParams = Ui.lp(Theme.dp(260), Theme.dp(48));
        buttonParams.topMargin = Theme.dp(22);
        content.addView(wifiButton, buttonParams);

        // Start at the top: focusing the bottom button would scroll the header away.
        initialFocus = ((ViewGroup) header).getChildAt(0);
    }

    @Override
    public void onShown() {
        refresh();
        super.onShown();
    }

    @Override
    public void onHostResume() {
        refresh();
    }

    @Override
    public void onAppsChanged() {
        refresh();
    }

    private void refresh() {
        if (statusValue == null) {
            return;
        }
        Context c = getContext();
        NetworkMonitor.Snapshot snapshot = NetworkMonitor.snapshot(c);

        statusValue.setText(snapshot.connected ? R.string.network_connected : R.string.network_disconnected);
        String connection = null;
        if (snapshot.connected) {
            if (snapshot.ssid != null) {
                connection = snapshot.type + "  ·  " + snapshot.ssid;
            } else if (!"—".equals(snapshot.ip)) {
                connection = snapshot.type + "  ·  " + snapshot.ip;
            } else {
                connection = snapshot.type;
            }
        }
        statusDetail.setText(connection != null ? connection : c.getString(R.string.network_hint));

        detailPanel.removeAllViews();
        String unknown = c.getString(R.string.network_unknown);
        addRow(c, R.string.network_name,
                snapshot.connected && snapshot.ssid != null ? snapshot.ssid : unknown);
        addRow(c, R.string.network_type, snapshot.connected ? snapshot.type : unknown);
        addRow(c, R.string.network_ip, snapshot.connected ? snapshot.ip : unknown);
        addRow(c, R.string.network_gateway, snapshot.connected ? snapshot.gateway : unknown);
        addRow(c, R.string.network_dns, snapshot.connected ? snapshot.dns : unknown);
        addRow(c, R.string.network_speed, snapshot.connected && snapshot.downKbps > 0
                ? c.getString(R.string.speed_hint_format, snapshot.downKbps / 1000)
                : unknown);
        addRow(c, R.string.network_interface, snapshot.connected ? snapshot.iface : unknown);
        addRow(c, R.string.network_metered, c.getString(snapshot.metered ? R.string.on : R.string.off));
    }

    private void addRow(Context c, int labelRes, CharSequence value) {
        if (detailPanel.getChildCount() > 0) {
            detailPanel.addView(Chrome.divider(c), Ui.lp(Ui.MATCH, Math.max(1, Theme.dp(0.7f))));
        }
        detailPanel.addView(Chrome.infoRow(c, c.getString(labelRes), value));
    }
}
