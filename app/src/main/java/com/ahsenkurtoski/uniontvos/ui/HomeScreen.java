package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import com.ahsenkurtoski.uniontvos.core.AppEntry;
import com.ahsenkurtoski.uniontvos.core.AppRepository;
import com.ahsenkurtoski.uniontvos.core.NetworkMonitor;
import com.ahsenkurtoski.uniontvos.core.Prefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** The UnionTV home experience. */
public class HomeScreen extends Screen {

    private ScrollView scroller;
    private FrameLayout hero;
    private TextView greetingView;
    private TextView timeView;
    private TextView dateView;
    private TextView networkView;
    private TextView statView;
    private TextView hintView;
    private View networkDot;
    private boolean shownOnce;
    private boolean awaitingInitialFocus;
    private FocusCard settingsButton;
    private LinearLayout featuredColumn;
    private RailView favoritesRail;
    private RailView appsRail;
    private RailView quickRail;

    private final List<AppCard> allCards = new ArrayList<AppCard>();
    private final List<FeaturePill> featuredPills = new ArrayList<FeaturePill>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final SimpleDateFormat time24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat time12 = new SimpleDateFormat("h:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault());

    private String signature = "";
    private String lastNetworkText = "";
    private boolean resumed;
    private boolean networkCallbackRegistered;
    private ConnectivityManager connectivityManager;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!resumed) {
                return;
            }
            updateClock();
            scheduleTick();
        }
    };

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            postNetworkUpdate();
        }

        @Override
        public void onLost(Network network) {
            postNetworkUpdate();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
            postNetworkUpdate();
        }
    };

    public HomeScreen(MainActivity host) {
        super(host);
        build();
    }

    // ---------------------------------------------------------------- layout

    private void build() {
        removeAllViews();
        Context c = getContext();

        scroller = new ScrollView(c);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setClipToPadding(false);
        scroller.setClipChildren(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setFocusable(false);

        LinearLayout content = Ui.column(c);
        content.setPadding(Theme.dp(28), Theme.dp(96), Theme.dp(28), Theme.dp(18));
        content.setClipChildren(false);
        scroller.addView(content, new ScrollView.LayoutParams(Ui.MATCH, Ui.WRAP));
        addView(scroller, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        // The bar is an overlay so the brand, clock and status stay pinned
        // while the rails scroll underneath it.
        FrameLayout headerArea = new FrameLayout(c);
        headerArea.setBackground(headerScrim());
        headerArea.setClipChildren(false);
        LinearLayout headerInner = Ui.column(c);
        headerInner.setPadding(Theme.dp(28), Theme.dp(20), Theme.dp(28), 0);
        headerInner.addView(buildTopBar(), Ui.lp(Ui.MATCH, Theme.dp(58)));
        headerArea.addView(headerInner, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(Ui.MATCH, Theme.dp(96));
        headerParams.gravity = Gravity.TOP;
        addView(headerArea, headerParams);

        content.addView(buildHero(), heroParams());
        content.addView(buildFavoritesHint(c));
        content.addView(buildRails(c));
        content.addView(buildFooter(c), footerParams());
    }

    private android.graphics.drawable.Drawable headerScrim() {
        return new android.graphics.drawable.Drawable() {
            private final Paint paint = new Paint();
            private LinearGradient shader;

            @Override
            protected void onBoundsChange(android.graphics.Rect bounds) {
                shader = new LinearGradient(0, bounds.top, 0, bounds.bottom,
                        new int[]{0xFF05070B, 0xFF05070B, 0xF205070B, 0x0005070B},
                        new float[]{0f, 0.76f, 0.92f, 1f}, Shader.TileMode.CLAMP);
            }

            @Override
            public void draw(Canvas canvas) {
                if (shader == null) {
                    return;
                }
                paint.setShader(shader);
                canvas.drawRect(getBounds(), paint);
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return android.graphics.PixelFormat.TRANSLUCENT;
            }
        };
    }

    private FrameLayout.LayoutParams heroParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(Ui.MATCH, Theme.dp(162));
        params.topMargin = 0;
        return params;
    }

    private View buildFavoritesHint(Context c) {
        hintView = Ui.text(c, getString(R.string.home_empty_favorites), 11.5f, Theme.TEXT_FAINT);
        hintView.setVisibility(View.GONE);
        LinearLayout.LayoutParams params = Ui.lp(Ui.WRAP, Ui.WRAP);
        params.topMargin = Theme.dp(14);
        params.leftMargin = Theme.dp(4);
        hintView.setLayoutParams(params);
        return hintView;
    }

    private LinearLayout.LayoutParams footerParams() {
        LinearLayout.LayoutParams params = Ui.lp(Ui.MATCH, Theme.dp(34));
        params.topMargin = Theme.dp(20);
        return params;
    }

    private View buildTopBar() {
        Context c = getContext();
        FrameLayout bar = new FrameLayout(c);

        LinearLayout brand = Ui.column(c);
        LinearLayout nameRow = Ui.row(c);
        nameRow.setGravity(Gravity.BOTTOM);

        TextView union = Ui.medium(c, getString(R.string.brand_union), 19f, Theme.TEXT);
        union.setLetterSpacing(0.14f);
        TextView tv = Ui.medium(c, getString(R.string.brand_tv), 19f, Theme.accent);
        tv.setLetterSpacing(0.14f);
        nameRow.addView(union);
        nameRow.addView(tv);

        TextView sub = Ui.text(c, getString(R.string.brand_sub), 7.5f, Theme.TEXT_FAINT);
        sub.setLetterSpacing(0.4f);

        brand.addView(nameRow);
        LinearLayout.LayoutParams subParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        subParams.topMargin = Theme.dp(5);
        brand.addView(sub, subParams);

        FrameLayout.LayoutParams brandParams = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        brandParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        bar.addView(brand, brandParams);

        LinearLayout right = Ui.row(c);

        LinearLayout pill = Ui.row(c);
        pill.setBackground(Ui.pill(Theme.alpha(0xFFFFFFFF, 0.05f)));
        pill.setPadding(Theme.dp(11), Theme.dp(6), Theme.dp(12), Theme.dp(6));
        networkDot = new View(c);
        networkDot.setBackground(Ui.round(Theme.accent, 999f));
        pill.addView(networkDot, Ui.lp(Theme.dp(6), Theme.dp(6)));
        networkView = Ui.text(c, "", 11f, Theme.TEXT_DIM);
        networkView.setMaxLines(1);
        networkView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        networkView.setMaxWidth(Theme.dp(150));
        LinearLayout.LayoutParams netParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        netParams.leftMargin = Theme.dp(8);
        pill.addView(networkView, netParams);
        right.addView(pill);

        LinearLayout clock = Ui.column(c);
        clock.setGravity(Gravity.END);
        timeView = Ui.medium(c, "--:--", 24f, Theme.TEXT);
        timeView.setLetterSpacing(0.02f);
        dateView = Ui.text(c, "", 10.5f, Theme.TEXT_DIM);
        clock.addView(timeView);
        clock.addView(dateView);
        LinearLayout.LayoutParams clockParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        clockParams.leftMargin = Theme.dp(20);
        right.addView(clock, clockParams);

        settingsButton = new FocusCard(c);
        settingsButton.setRadiusDp(13f);
        settingsButton.setFocusScale(1.09f);
        settingsButton.setFill(Theme.alpha(0xFFFFFFFF, 0.06f));
        ImageView gear = new ImageView(c);
        gear.setImageBitmap(Ui.vectorBitmap(c, R.drawable.ic_settings, Theme.dp(19), Theme.TEXT_DIM));
        FrameLayout.LayoutParams gearParams = new FrameLayout.LayoutParams(Theme.dp(19), Theme.dp(19));
        gearParams.gravity = Gravity.CENTER;
        settingsButton.addView(gear, gearParams);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.open(SettingsScreen.class);
            }
        });
        LinearLayout.LayoutParams gearButtonParams = Ui.lp(Theme.dp(42), Theme.dp(42));
        gearButtonParams.leftMargin = Theme.dp(16);
        right.addView(settingsButton, gearButtonParams);

        FrameLayout.LayoutParams rightParams = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        rightParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        bar.addView(right, rightParams);

        return bar;
    }

    private View buildHero() {
        Context c = getContext();
        hero = new HeroPanel(c);

        LinearLayout left = Ui.column(c);
        left.setGravity(Gravity.CENTER_VERTICAL);
        left.setPadding(Theme.dp(30), Theme.dp(20), Theme.dp(320), Theme.dp(20));

        greetingView = Ui.light(c, "", 31f, Theme.TEXT);
        greetingView.setLetterSpacing(0.005f);

        TextView tagline = Ui.text(c, getString(R.string.home_tagline), 13f, Theme.TEXT_DIM);

        statView = Ui.text(c, "", 9.5f, Theme.TEXT_FAINT);
        statView.setLetterSpacing(0.2f);

        left.addView(greetingView);
        LinearLayout.LayoutParams taglineParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        taglineParams.topMargin = Theme.dp(6);
        left.addView(tagline, taglineParams);
        LinearLayout.LayoutParams statParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        statParams.topMargin = Theme.dp(13);
        left.addView(statView, statParams);

        FrameLayout.LayoutParams leftParams = new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH);
        leftParams.gravity = Gravity.START;
        hero.addView(left, leftParams);

        featuredColumn = Ui.column(c);
        featuredColumn.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

        TextView featuredLabel = Ui.text(c, getString(R.string.rail_featured).toUpperCase(Locale.getDefault()),
                8.5f, Theme.TEXT_FAINT);
        featuredLabel.setLetterSpacing(0.3f);
        featuredLabel.setGravity(Gravity.END);
        featuredColumn.addView(featuredLabel);

        FrameLayout.LayoutParams featuredParams = new FrameLayout.LayoutParams(Theme.dp(252), Ui.MATCH);
        featuredParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        featuredParams.rightMargin = Theme.dp(24);
        hero.addView(featuredColumn, featuredParams);

        return hero;
    }

    private View buildRails(Context c) {
        LinearLayout rails = Ui.column(c);
        rails.setClipChildren(false);

        favoritesRail = new RailView(c, getString(R.string.rail_favorites));
        favoritesRail.setEmptyText(getString(R.string.home_empty_favorites));
        rails.addView(favoritesRail, RailView.layoutParams(c));

        appsRail = new RailView(c, getString(R.string.rail_apps));
        appsRail.setEmptyText(getString(R.string.home_empty_apps));
        rails.addView(appsRail, RailView.layoutParams(c));

        quickRail = new RailView(c, getString(R.string.rail_quick));
        rails.addView(quickRail, RailView.layoutParams(c));

        return rails;
    }

    private View buildFooter(Context c) {
        FrameLayout footer = new FrameLayout(c);
        TextView credit = Ui.text(c, getString(R.string.credit_line), 10f, Theme.TEXT_FAINT);
        FrameLayout.LayoutParams creditParams = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        creditParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        footer.addView(credit, creditParams);

        TextView version = Ui.text(c, "v" + BuildConfig.VERSION_NAME, 9.5f,
                Theme.alpha(Theme.TEXT_FAINT, 0.75f));
        FrameLayout.LayoutParams versionParams = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        versionParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        footer.addView(version, versionParams);
        return footer;
    }

    // ------------------------------------------------------------- behaviour

    @Override
    public void onShown() {
        // The first load publishes cards only after their icons are ready, so
        // hold the initial focus until the real cards exist; never land on a
        // letter placeholder.
        if (!shownOnce) {
            shownOnce = true;
            awaitingInitialFocus = true;
        }
        refreshRails(false);
        updateClock();
        updateNetwork();
        super.onShown();
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (awaitingInitialFocus && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            if (code >= android.view.KeyEvent.KEYCODE_DPAD_UP
                    && code <= android.view.KeyEvent.KEYCODE_DPAD_CENTER) {
                awaitingInitialFocus = false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onHostResume() {
        resumed = true;
        updateClock();
        scheduleTick();
        updateNetwork();
        registerNetwork();
        refreshRails(false);
    }

    @Override
    public void onHostPause() {
        resumed = false;
        handler.removeCallbacks(ticker);
        unregisterNetwork();
    }

    @Override
    public void onAppsChanged() {
        refreshRails(false);
    }

    private void registerNetwork() {
        if (networkCallbackRegistered) {
            return;
        }
        connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
            networkCallbackRegistered = true;
        } catch (Throwable ignored) {
            networkCallbackRegistered = false;
        }
    }

    private void unregisterNetwork() {
        if (!networkCallbackRegistered || connectivityManager == null) {
            return;
        }
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Throwable ignored) {
            // ignored
        }
        networkCallbackRegistered = false;
    }

    private void postNetworkUpdate() {
        post(new Runnable() {
            @Override
            public void run() {
                updateNetwork();
            }
        });
    }

    private void updateNetwork() {
        if (networkView == null) {
            return;
        }
        NetworkMonitor.Snapshot snapshot = NetworkMonitor.snapshot(getContext());
        String text;
        if (snapshot.connected) {
            String detail = snapshot.ssid;
            if (detail == null || detail.length() == 0) {
                detail = "—".equals(snapshot.ip) ? null : snapshot.ip;
            }
            text = detail == null ? snapshot.type : snapshot.type + "  ·  " + detail;
        } else {
            text = getString(R.string.network_disconnected);
        }
        networkDot.setBackground(Ui.round(snapshot.connected ? Theme.OK : Theme.TEXT_FAINT, 999f));
        if (!text.equals(lastNetworkText)) {
            networkView.setText(text);
            lastNetworkText = text;
        }
    }

    private void scheduleTick() {
        handler.removeCallbacks(ticker);
        long now = System.currentTimeMillis();
        long delay = 60_000L - (now % 60_000L) + 250L;
        handler.postDelayed(ticker, delay);
    }

    private void updateClock() {
        if (timeView == null) {
            return;
        }
        Date now = new Date();
        timeView.setText(host.prefs().clock24() ? time24.format(now) : time12.format(now));
        String date = dateFormat.format(now);
        if (date.length() > 0) {
            date = Character.toUpperCase(date.charAt(0)) + date.substring(1);
        }
        dateView.setText(date);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greeting;
        if (hour >= 5 && hour < 12) {
            greeting = R.string.home_greeting_morning;
        } else if (hour >= 12 && hour < 18) {
            greeting = R.string.home_greeting_afternoon;
        } else if (hour >= 18 && hour < 23) {
            greeting = R.string.home_greeting_evening;
        } else {
            greeting = R.string.home_greeting_night;
        }
        String greetingText = getString(greeting);
        if (!greetingText.equals(greetingView.getText().toString())) {
            greetingView.setText(greetingText);
        }
    }

    // ------------------------------------------------------------- app rails

    private void refreshRails(boolean force) {
        if (favoritesRail == null) {
            return;
        }
        AppRepository repo = host.repository();
        List<AppEntry> all = repo.sorted(host.prefs().sortMode());
        List<AppEntry> favorites = repo.favorites();

        if (all.isEmpty() && (repo.isLoading() || !repo.isLoadedOnce())) {
            if (!"loading".equals(signature)) {
                showLoadingState();
            }
            return;
        }
        appsRail.setEmptyText(getString(R.string.home_empty_apps));

        String current = buildSignature(all);
        if (!force && current.equals(signature)) {
            updateCards();
            return;
        }
        signature = current;

        AppEntry focused = focusedEntry();

        allCards.clear();
        featuredPills.clear();

        List<View> favoriteCards = new ArrayList<View>();
        for (int i = 0; i < favorites.size(); i++) {
            favoriteCards.add(appCard(favorites.get(i)));
        }

        List<View> appCards = new ArrayList<View>();
        for (int i = 0; i < all.size(); i++) {
            appCards.add(appCard(all.get(i)));
        }

        boolean favoritesEnabled = host.prefs().favoritesRail();
        boolean showFavorites = favoritesEnabled && !favoriteCards.isEmpty();
        favoritesRail.setVisibility(showFavorites ? VISIBLE : GONE);
        favoritesRail.setCards(showFavorites ? favoriteCards : new ArrayList<View>());
        hintView.setVisibility(favoritesEnabled && favoriteCards.isEmpty() ? VISIBLE : GONE);

        appsRail.setCards(appCards);
        quickRail.setCards(buildQuickAccess());

        updateFeatured(favorites, all);

        statView.setText(statLine(all.size(), favorites.size()));

        initialFocus = !favoriteCards.isEmpty()
                ? favoriteCards.get(0)
                : (!appCards.isEmpty() ? appCards.get(0) : settingsButton);

        restoreFocus(focused);

        if (awaitingInitialFocus && !appCards.isEmpty()) {
            final View target = initialFocus;
            post(new Runnable() {
                @Override
                public void run() {
                    if (isShown()) {
                        awaitingInitialFocus = false;
                        target.requestFocus();
                    }
                }
            });
        }
    }

    private void showLoadingState() {
        signature = "loading";
        allCards.clear();
        featuredPills.clear();
        favoritesRail.setVisibility(GONE);
        hintView.setVisibility(GONE);
        appsRail.setEmptyText(getString(R.string.apps_loading));
        appsRail.setCards(new ArrayList<View>());
        quickRail.setCards(buildQuickAccess());
        statView.setText(R.string.apps_loading);
        initialFocus = settingsButton;
    }

    private String statLine(int appCount, int favoriteCount) {
        android.content.res.Resources resources = getContext().getResources();
        String apps = resources.getQuantityString(R.plurals.home_apps_count, appCount, appCount);
        String favorites = resources.getQuantityString(
                R.plurals.home_favourites_count, favoriteCount, favoriteCount);
        return apps + "  ·  " + favorites;
    }

    private String buildSignature(List<AppEntry> all) {
        StringBuilder builder = new StringBuilder(all.size() * 32);
        builder.append(host.prefs().favoritesRail() ? '1' : '0').append('|');
        for (int i = 0; i < all.size(); i++) {
            AppEntry entry = all.get(i);
            builder.append(entry.key()).append(':').append(entry.favorite ? '1' : '0').append(';');
        }
        return builder.toString();
    }

    private void updateCards() {
        for (int i = 0; i < allCards.size(); i++) {
            allCards.get(i).updateIcon();
        }
        for (int i = 0; i < featuredPills.size(); i++) {
            featuredPills.get(i).updateIcon();
        }
    }

    private AppCard appCard(final AppEntry entry) {
        AppCard card = new AppCard(getContext());
        card.bind(entry);
        if (entry.icon == null) {
            host.repository().ensureIcon(entry);
        }
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openApp(entry);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ActionSheet.show(host, entry);
                return true;
            }
        });
        verticalFollow(card);
        allCards.add(card);
        return card;
    }

    private List<View> buildQuickAccess() {
        List<View> tiles = new ArrayList<View>();

        tiles.add(actionTile(R.drawable.ic_settings, getString(R.string.quick_settings),
                SettingsScreen.class));

        TileCard apps = new TileCard(getContext(), R.drawable.ic_apps, getString(R.string.quick_apps));
        apps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.open(AppsScreen.class);
            }
        });
        verticalFollow(apps);
        tiles.add(apps);

        tiles.add(actionTile(R.drawable.ic_wifi, getString(R.string.quick_network), NetworkScreen.class));
        tiles.add(actionTile(R.drawable.ic_system, getString(R.string.quick_system), SystemScreen.class));
        tiles.add(actionTile(R.drawable.ic_info, getString(R.string.quick_about), AboutScreen.class));
        return tiles;
    }

    private TileCard actionTile(int iconRes, String label, final Class<? extends Screen> target) {
        TileCard tile = new TileCard(getContext(), iconRes, label);
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.open(target);
            }
        });
        verticalFollow(tile);
        return tile;
    }

    private void updateFeatured(List<AppEntry> favorites, List<AppEntry> all) {
        List<AppEntry> picks = new ArrayList<AppEntry>(2);
        for (int i = 0; i < favorites.size() && picks.size() < 2; i++) {
            picks.add(favorites.get(i));
        }
        if (picks.isEmpty() && !all.isEmpty()) {
            List<AppEntry> recent = host.repository().sorted(Prefs.SORT_RECENT);
            for (int i = 0; i < recent.size() && picks.size() < 2; i++) {
                if (recent.get(i).lastUsed > 0) {
                    picks.add(recent.get(i));
                }
            }
        }

        while (featuredColumn.getChildCount() > 1) {
            featuredColumn.removeViewAt(1);
        }
        featuredColumn.setVisibility(picks.isEmpty() ? GONE : VISIBLE);

        for (int i = 0; i < picks.size(); i++) {
            FeaturePill pill = new FeaturePill(getContext(), picks.get(i));
            LinearLayout.LayoutParams params = Ui.lp(Ui.MATCH, Theme.dp(44));
            if (i > 0) {
                params.topMargin = Theme.dp(8);
            }
            featuredColumn.addView(pill, params);
            featuredPills.add(pill);
        }
    }

    private <T extends FocusCard> T verticalFollow(final T card) {
        card.addFocusListener(new FocusCard.FocusListener() {
            @Override
            public void onCardFocus(FocusCard focusedCard, boolean hasFocus) {
                if (hasFocus) {
                    Ui.scrollIntoView(scroller, focusedCard, Theme.dp(96), Theme.dp(16));
                }
            }
        });
        return card;
    }

    private AppEntry focusedEntry() {
        View focused = findFocus();
        if (focused instanceof AppCard) {
            return ((AppCard) focused).entry();
        }
        return null;
    }

    private void restoreFocus(AppEntry entry) {
        if (entry == null || !isShown()) {
            return;
        }
        for (int i = 0; i < allCards.size(); i++) {
            final AppCard card = allCards.get(i);
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

    private void openApp(AppEntry entry) {
        host.repository().markUsed(entry);
        if (!AppRepository.launch(host, entry)) {
            Ui.toast(getContext(), getString(R.string.launch_failed, entry.label));
        }
    }

    // ------------------------------------------------------------ components

    /** Featured shortcut pill inside the hero. */
    private final class FeaturePill extends FocusCard {

        private final AppEntry entry;
        private final ImageView iconView;
        private final int iconSize;

        FeaturePill(Context context, AppEntry appEntry) {
            super(context);
            entry = appEntry;
            iconSize = Theme.dp(22);
            setRadiusDp(12f);
            setFocusScale(1.03f);
            setFill(Theme.alpha(0xFFFFFFFF, 0.05f));

            LinearLayout content = Ui.row(context);
            content.setPadding(Theme.dp(11), 0, Theme.dp(12), 0);

            iconView = new ImageView(context);
            content.addView(iconView, Ui.lp(iconSize, iconSize));

            TextView label = Ui.text(context, appEntry.label, 12f, Theme.TEXT_DIM);
            label.setMaxLines(1);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams labelParams = Ui.lpw(0, Ui.WRAP, 1f);
            labelParams.leftMargin = Theme.dp(10);
            content.addView(label, labelParams);

            addView(content, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

            addFocusListener(new FocusListener() {
                @Override
                public void onCardFocus(FocusCard card, boolean focused) {
                    label.setTextColor(focused ? Theme.TEXT : Theme.TEXT_DIM);
                }
            });

            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openApp(entry);
                }
            });

            updateIcon();
        }

        void updateIcon() {
            Bitmap bitmap = entry.icon;
            if (bitmap != null) {
                iconView.setImageBitmap(bitmap);
            } else {
                iconView.setImageBitmap(Ui.letterAvatar(getContext(), entry.label, iconSize));
                host.repository().ensureIcon(entry);
            }
        }
    }

    /** Hero panel: accent-tinted gradient, soft glow and a thin accent rule. */
    private static final class HeroPanel extends FrameLayout {

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final RectF ruleRect = new RectF();
        private final float radius = Theme.dpf(24f);

        HeroPanel(Context context) {
            super(context);
            setWillNotDraw(false);
            setClipChildren(true);
            setClipToPadding(true);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w <= 0 || h <= 0) {
                return;
            }
            int top = Theme.blend(0xFF0C1220, Theme.accent, 0.18f);
            int bottom = 0xFF080C14;
            fill.setShader(new LinearGradient(0, 0, w * 0.6f, h, top, bottom, Shader.TileMode.CLAMP));
            glow.setShader(new RadialGradient(w * 0.78f, h * 0.86f, w * 0.55f,
                    new int[]{Theme.alpha(Theme.accent, 0.30f), Theme.alpha(Theme.accent, 0.05f), 0x00000000},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            rule.setColor(Theme.accent);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, radius, radius, fill);
            canvas.drawRoundRect(rect, radius, radius, glow);

            // Faint monogram watermark, visible when no featured shortcuts exist.
            mark.setTypeface(Ui.LIGHT);
            mark.setTextSize(Theme.dpf(150f));
            mark.setColor(Theme.alpha(0xFFFFFFFF, 0.045f));
            mark.setTextAlign(Paint.Align.CENTER);
            float baseline = h / 2f - (mark.descent() + mark.ascent()) / 2f;
            canvas.drawText("U", w - Theme.dpf(72f), baseline, mark);

            ruleRect.set(0, Theme.dpf(26f), Theme.dpf(3f), h - Theme.dpf(26f));
            canvas.drawRoundRect(ruleRect, Theme.dpf(2f), Theme.dpf(2f), rule);
        }
    }
}
