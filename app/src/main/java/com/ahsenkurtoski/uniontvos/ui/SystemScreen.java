package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahsenkurtoski.uniontvos.MainActivity;
import com.ahsenkurtoski.uniontvos.R;
import com.ahsenkurtoski.uniontvos.core.DeviceInfo;

/** Read-only system information. */
public class SystemScreen extends Screen {

    private ScrollView scroller;
    private FocusCard backToTop;

    public SystemScreen(MainActivity host) {
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
        content.setPadding(Theme.dp(28), Theme.dp(22), Theme.dp(28), Theme.dp(34));
        content.setClipChildren(false);
        scroller.addView(content, new ScrollView.LayoutParams(Ui.MATCH, Ui.WRAP));
        addView(scroller, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        content.addView(Chrome.header(host, getString(R.string.system_title),
                DeviceInfo.manufacturer() + " " + DeviceInfo.model()), Ui.lp(Ui.MATCH, Theme.dp(50)));

        long[] storage = DeviceInfo.storage();
        long ram = DeviceInfo.ramTotal(c);

        content.addView(Chrome.sectionTitle(c, getString(R.string.sys_section_device)));
        LinearLayout device = Chrome.panel(c);
        addRow(device, c, R.string.sys_manufacturer, DeviceInfo.manufacturer());
        addRow(device, c, R.string.sys_model, DeviceInfo.model());
        addRow(device, c, R.string.sys_arch, DeviceInfo.abi());
        addRow(device, c, R.string.sys_cores, c.getString(R.string.value_cpu_format, DeviceInfo.cpuCores()));
        content.addView(device);

        content.addView(Chrome.sectionTitle(c, getString(R.string.sys_section_os)));
        LinearLayout software = Chrome.panel(c);
        addRow(software, c, R.string.sys_os, "Android " + DeviceInfo.androidRelease());
        addRow(software, c, R.string.sys_sdk, String.valueOf(DeviceInfo.sdk()));
        addRow(software, c, R.string.sys_gpu, DeviceInfo.gpu(c));
        addRow(software, c, R.string.sys_home, DeviceInfo.defaultHome(c));
        addRow(software, c, R.string.sys_apps, String.valueOf(host.repository().apps().size()));
        content.addView(software);

        content.addView(Chrome.sectionTitle(c, getString(R.string.sys_section_memory)));
        LinearLayout memory = Chrome.panel(c);
        addRow(memory, c, R.string.sys_memory, DeviceInfo.formatBytes(ram) + " · "
                + DeviceInfo.ramSummary(c));
        addRow(memory, c, R.string.sys_storage, c.getString(R.string.value_storage,
                DeviceInfo.formatBytes(storage[0]), DeviceInfo.formatBytes(storage[1])));
        addRow(memory, c, R.string.sys_uptime, DeviceInfo.uptime());
        content.addView(memory);

        content.addView(Chrome.sectionTitle(c, getString(R.string.sys_section_display)));
        LinearLayout display = Chrome.panel(c);
        addRow(display, c, R.string.sys_display, c.getString(R.string.value_display,
                DeviceInfo.displayWidth(c), DeviceInfo.displayHeight(c)));
        addRow(display, c, R.string.sys_density, c.getString(R.string.value_density, DeviceInfo.densityDpi(c)));
        content.addView(display);

        backToTop = new FocusCard(c);
        backToTop.setRadiusDp(14f);
        backToTop.setFill(Theme.alpha(0xFFFFFFFF, 0.04f));
        TextView refresh = Ui.medium(c, getString(R.string.refresh), 12.5f, Theme.TEXT);
        backToTop.addView(refresh, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        ((FrameLayout.LayoutParams) refresh.getLayoutParams()).gravity = android.view.Gravity.CENTER;
        backToTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        host.applyAppearance(SystemScreen.class);
                    }
                });
            }
        });
        LinearLayout.LayoutParams refreshParams = Ui.lp(Theme.dp(150), Theme.dp(44));
        refreshParams.topMargin = Theme.dp(24);
        content.addView(backToTop, refreshParams);

        initialFocus = backToTop;
    }

    private void addRow(LinearLayout panel, Context c, int labelRes, CharSequence value) {
        if (panel.getChildCount() > 0) {
            panel.addView(Chrome.divider(c), Ui.lp(Ui.MATCH, Math.max(1, Theme.dp(0.7f))));
        }
        panel.addView(Chrome.infoRow(c, c.getString(labelRes), value));
    }
}
