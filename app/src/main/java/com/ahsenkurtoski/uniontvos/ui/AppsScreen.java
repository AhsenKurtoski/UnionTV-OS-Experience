package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahsenkurtoski.uniontvos.MainActivity;
import com.ahsenkurtoski.uniontvos.R;
import com.ahsenkurtoski.uniontvos.core.AppEntry;
import com.ahsenkurtoski.uniontvos.core.AppRepository;
import com.ahsenkurtoski.uniontvos.core.Prefs;

import java.util.ArrayList;
import java.util.List;

/** The complete, sortable application grid. */
public class AppsScreen extends Screen {

    private ScrollView scroller;
    private SimpleGrid grid;
    private TextView subtitleView;
    private TextView emptyView;

    private final List<AppCard> cards = new ArrayList<AppCard>();
    private final List<Chip> chips = new ArrayList<Chip>();

    private int sortMode;
    private String signature = "";

    public AppsScreen(MainActivity host) {
        super(host);
        build();
    }

    private void build() {
        Context c = getContext();
        removeAllViews();
        sortMode = host.prefs().sortMode();

        scroller = new ScrollView(c);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setClipToPadding(false);
        scroller.setClipChildren(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setFocusable(false);

        LinearLayout content = Ui.column(c);
        content.setPadding(Theme.dp(28), Theme.dp(22), Theme.dp(28), Theme.dp(30));
        content.setClipChildren(false);
        scroller.addView(content, new ScrollView.LayoutParams(Ui.MATCH, Ui.WRAP));
        addView(scroller, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        content.addView(Chrome.header(host, getString(R.string.apps_title), getString(R.string.apps_hint)),
                Ui.lp(Ui.MATCH, Theme.dp(50)));

        LinearLayout sortRow = Ui.row(c);
        sortRow.addView(Chrome.sectionTitle(c, "Sort"), Ui.lp(Ui.WRAP, Ui.WRAP));
        addChip(sortRow, Prefs.SORT_AZ, getString(R.string.sort_az));
        addChip(sortRow, Prefs.SORT_RECENT, getString(R.string.sort_recent));
        addChip(sortRow, Prefs.SORT_FAVORITES, getString(R.string.sort_favorites));
        LinearLayout.LayoutParams sortParams = Ui.lp(Ui.MATCH, Ui.WRAP);
        sortParams.topMargin = Theme.dp(6);
        content.addView(sortRow, sortParams);

        subtitleView = Ui.text(c, "", 11.5f, Theme.TEXT_FAINT);
        LinearLayout.LayoutParams subtitleParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        subtitleParams.topMargin = Theme.dp(8);
        content.addView(subtitleView, subtitleParams);

        emptyView = Ui.text(c, getString(R.string.apps_empty), 13f, Theme.TEXT_FAINT);
        emptyView.setVisibility(View.GONE);
        LinearLayout.LayoutParams emptyParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        emptyParams.topMargin = Theme.dp(26);
        content.addView(emptyView, emptyParams);

        grid = new SimpleGrid(c);
        grid.setColumns(columns());
        LinearLayout.LayoutParams gridParams = Ui.lp(Ui.MATCH, Ui.WRAP);
        gridParams.topMargin = Theme.dp(14);
        content.addView(grid, gridParams);

        rebuildGrid();
        initialFocus = cards.isEmpty() ? chips.get(0) : cards.get(0);
        // Record the first signature so onShown does not rebuild the grid and
        // leave the focus pointing at a card that no longer exists.
        signature = signature(host.repository().sorted(sortMode));
    }

    private void addChip(LinearLayout parent, final int mode, String label) {
        Chip chip = new Chip(mode, label);
        chips.add(chip);
        LinearLayout.LayoutParams params = Ui.lp(Theme.dp(96), Theme.dp(36));
        params.leftMargin = Theme.dp(8);
        parent.addView(chip, params);
    }

    private int columns() {
        int width = getResources().getDisplayMetrics().widthPixels - Theme.dp(56);
        int gap = Theme.dp(12);
        int target = Theme.dp(112);
        return Math.max(4, (width + gap) / (target + gap));
    }

    @Override
    public void onShown() {
        refresh(false);
        super.onShown();
    }

    @Override
    public void onAppsChanged() {
        refresh(false);
    }

    private void refresh(boolean force) {
        List<AppEntry> apps = host.repository().sorted(sortMode);
        String current = signature(apps);
        if (!force && current.equals(signature)) {
            for (int i = 0; i < cards.size(); i++) {
                cards.get(i).updateIcon();
            }
            return;
        }
        signature = current;
        rebuildGrid();
    }

    private String signature(List<AppEntry> apps) {
        StringBuilder builder = new StringBuilder(apps.size() * 28);
        builder.append(sortMode).append('|');
        for (int i = 0; i < apps.size(); i++) {
            builder.append(apps.get(i).key()).append(':')
                    .append(apps.get(i).favorite ? '1' : '0').append(';');
        }
        return builder.toString();
    }

    private void rebuildGrid() {
        Context c = getContext();
        List<AppEntry> apps = host.repository().sorted(sortMode);
        AppEntry focused = focusedEntry();

        if (apps.isEmpty() && (host.repository().isLoading() || !host.repository().isLoadedOnce())) {
            grid.removeAllViews();
            cards.clear();
            subtitleView.setText(getString(R.string.apps_loading));
            emptyView.setText(getString(R.string.apps_loading));
            emptyView.setVisibility(View.VISIBLE);
            grid.setVisibility(View.GONE);
            initialFocus = chips.isEmpty() ? null : chips.get(0);
            return;
        }

        grid.removeAllViews();
        cards.clear();

        for (int i = 0; i < apps.size(); i++) {
            final AppEntry entry = apps.get(i);
            AppCard card = new AppCard(c);
            card.bind(entry);
            if (entry.icon == null) {
                host.repository().ensureIcon(entry);
            }
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    host.repository().markUsed(entry);
                    if (!AppRepository.launch(host, entry)) {
                        Ui.toast(c, c.getString(R.string.launch_failed, entry.label));
                    }
                }
            });
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ActionSheet.show(host, entry);
                    return true;
                }
            });
            card.addFocusListener(new FocusCard.FocusListener() {
                @Override
                public void onCardFocus(FocusCard focusedCard, boolean hasFocus) {
                    if (hasFocus) {
                        Ui.scrollIntoView(scroller, focusedCard, Theme.dp(8), Theme.dp(20));
                    }
                }
            });
            grid.addView(card);
            cards.add(card);
        }

        subtitleView.setText(getString(R.string.apps_subtitle_format, apps.size()));
        emptyView.setText(getString(R.string.apps_empty));
        emptyView.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        grid.setVisibility(apps.isEmpty() ? View.GONE : View.VISIBLE);

        initialFocus = cards.isEmpty() ? chips.get(0) : cards.get(0);

        if (isShown()) {
            if (focused != null) {
                restoreFocus(focused);
            } else if (findFocus() == null && initialFocus != null) {
                final View target = initialFocus;
                target.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isShown()) {
                            target.requestFocus();
                        }
                    }
                });
            }
        }
    }

    private AppEntry focusedEntry() {
        View focused = findFocus();
        if (focused instanceof AppCard) {
            return ((AppCard) focused).entry();
        }
        return null;
    }

    private void restoreFocus(AppEntry entry) {
        for (int i = 0; i < cards.size(); i++) {
            final AppCard card = cards.get(i);
            if (card.entry() != null && card.entry().key().equals(entry.key())) {
                card.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isShown()) {
                            card.requestFocus();
                        }
                    }
                });
                return;
            }
        }
    }

    private void selectSort(int mode) {
        if (sortMode == mode) {
            return;
        }
        sortMode = mode;
        host.prefs().setSortMode(mode);
        for (int i = 0; i < chips.size(); i++) {
            Chip chip = chips.get(i);
            chip.setActive(chip.mode == mode);
            chip.applyColors();
        }
        rebuildGrid();
    }

    /** Sort selector pill. */
    private final class Chip extends FocusCard {

        final int mode;
        private final TextView label;

        Chip(int sortModeValue, String text) {
            super(AppsScreen.this.getContext());
            mode = sortModeValue;
            setRadiusDp(999f);
            setFocusScale(1.05f);
            setFill(Theme.alpha(0xFFFFFFFF, 0.05f));

            label = Ui.medium(getContext(), text, 11.5f, Theme.TEXT_DIM);
            label.setGravity(Gravity.CENTER);
            addView(label, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

            setActive(mode == sortMode);
            addFocusListener(new FocusListener() {
                @Override
                public void onCardFocus(FocusCard card, boolean focused) {
                    applyColors();
                }
            });
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectSort(mode);
                }
            });
            applyColors();
        }

        void applyColors() {
            label.setTextColor(isActive() || isCardFocused() ? Theme.TEXT : Theme.TEXT_DIM);
        }
    }
}
