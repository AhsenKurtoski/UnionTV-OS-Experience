package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahsenkurtoski.uniontvos.MainActivity;
import com.ahsenkurtoski.uniontvos.R;
import com.ahsenkurtoski.uniontvos.core.Prefs;

/** All user preferences, applied without restarting the process. */
public class SettingsScreen extends Screen {

    private ScrollView scroller;
    private LinearLayout content;

    public SettingsScreen(MainActivity host) {
        super(host);
        build();
    }

    private void build() {
        Context c = getContext();
        removeAllViews();

        scroller = new ScrollView(c);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setClipToPadding(false);
        scroller.setClipChildren(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setFocusable(false);

        content = Ui.column(c);
        content.setPadding(Theme.dp(28), Theme.dp(22), Theme.dp(28), Theme.dp(34));
        content.setClipChildren(false);
        scroller.addView(content, new ScrollView.LayoutParams(Ui.MATCH, Ui.WRAP));
        addView(scroller, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        content.addView(Chrome.header(host, getString(R.string.settings_title),
                getString(R.string.app_name) + " · v" + com.ahsenkurtoski.uniontvos.BuildConfig.VERSION_NAME),
                Ui.lp(Ui.MATCH, Theme.dp(50)));

        buildAppearance(c);
        buildInterface(c);
        buildApplications(c);
        buildProject(c);

        initialFocus = accentSwatches.get(0);
    }

    private final java.util.List<FocusCard> accentSwatches = new java.util.ArrayList<FocusCard>();

    private static final int[] ACCENT_NAMES = {
            R.string.accent_blue, R.string.accent_violet, R.string.accent_aqua, R.string.accent_amber
    };

    private static final int[] WALLPAPER_NAMES = {
            R.string.wall_midnight, R.string.wall_nebula, R.string.wall_slate, R.string.wall_aurora
    };

    private void buildAppearance(Context c) {
        content.addView(Chrome.sectionTitle(c, getString(R.string.settings_appearance)));

        TextView accentLabel = Ui.text(c, getString(R.string.settings_accent), 12.5f, Theme.TEXT_DIM);
        content.addView(accentLabel);

        LinearLayout swatches = Ui.row(c);
        int currentAccent = host.prefs().accentIndex();
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            Swatch swatch = new Swatch(c, i, Theme.ACCENTS[i]);
            swatch.setActive(i == currentAccent);
            LinearLayout.LayoutParams params = Ui.lp(Theme.dp(56), Theme.dp(52));
            params.leftMargin = i == 0 ? 0 : Theme.dp(10);
            params.topMargin = Theme.dp(10);
            swatches.addView(swatch, params);
            accentSwatches.add(swatch);
        }
        content.addView(swatches);

        TextView wallpaperLabel = Ui.text(c, getString(R.string.settings_wallpaper), 12.5f, Theme.TEXT_DIM);
        LinearLayout.LayoutParams labelParams = Ui.lp(Ui.WRAP, Ui.WRAP);
        labelParams.topMargin = Theme.dp(20);
        content.addView(wallpaperLabel, labelParams);

        LinearLayout wallpapers = Ui.row(c);
        int currentWall = host.prefs().wallpaperIndex();
        for (int i = 0; i < GlowBackground.COUNT; i++) {
            WallpaperSwatch swatch = new WallpaperSwatch(c, i);
            swatch.setActive(i == currentWall);
            LinearLayout.LayoutParams params = Ui.lp(Theme.dp(108), Theme.dp(64));
            params.leftMargin = i == 0 ? 0 : Theme.dp(10);
            params.topMargin = Theme.dp(10);
            wallpapers.addView(swatch, params);
        }
        content.addView(wallpapers);
    }

    private void buildInterface(Context c) {
        content.addView(Chrome.sectionTitle(c, getString(R.string.settings_interface)));

        final Prefs prefs = host.prefs();

        Chrome.OptionRow motion = Chrome.optionRow(c,
                getString(R.string.settings_reduce_motion),
                getString(R.string.settings_reduce_motion_desc),
                prefs.reduceMotion() ? getString(R.string.on) : getString(R.string.off),
                prefs.reduceMotion(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prefs.setReduceMotion(!prefs.reduceMotion());
                        rebuildLater(SettingsScreen.class);
                    }
                });
        content.addView(motion);

        Chrome.OptionRow clock = Chrome.optionRow(c,
                getString(R.string.settings_clock24),
                getString(R.string.settings_clock24_desc),
                prefs.clock24() ? getString(R.string.on) : getString(R.string.off),
                prefs.clock24(),
                null);
        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setClock24(!prefs.clock24());
                ((Chrome.OptionRow) v).setValue(prefs.clock24() ? getString(R.string.on) : getString(R.string.off));
                ((Chrome.OptionRow) v).setValueHighlighted(prefs.clock24());
                host.notifyAppsChanged();
            }
        });
        content.addView(clock);

        Chrome.OptionRow favorites = Chrome.optionRow(c,
                getString(R.string.settings_favorites_rail),
                getString(R.string.settings_favorites_rail_desc),
                prefs.favoritesRail() ? getString(R.string.on) : getString(R.string.off),
                prefs.favoritesRail(),
                null);
        favorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setFavoritesRail(!prefs.favoritesRail());
                ((Chrome.OptionRow) v).setValue(prefs.favoritesRail() ? getString(R.string.on) : getString(R.string.off));
                ((Chrome.OptionRow) v).setValueHighlighted(prefs.favoritesRail());
                host.notifyAppsChanged();
            }
        });
        content.addView(favorites);
    }

    private void buildApplications(Context c) {
        content.addView(Chrome.sectionTitle(c, getString(R.string.settings_apps_section)));

        Chrome.OptionRow manage = Chrome.optionRow(c,
                getString(R.string.settings_manage_favorites),
                getString(R.string.settings_manage_favorites_desc),
                null, false,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        host.open(AppsScreen.class);
                    }
                });
        content.addView(manage);
    }

    private void buildProject(Context c) {
        content.addView(Chrome.sectionTitle(c, getString(R.string.settings_about_section)));

        Chrome.OptionRow about = Chrome.optionRow(c,
                getString(R.string.settings_about),
                getString(R.string.settings_about_desc),
                null, false,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        host.open(AboutScreen.class);
                    }
                });
        content.addView(about);

        Chrome.OptionRow reset = Chrome.optionRow(c,
                getString(R.string.settings_reset),
                getString(R.string.settings_reset_desc),
                null, false,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        host.prefs().restoreDefaults();
                        Ui.toast(getContext(), getString(R.string.settings_reset_done));
                        rebuildLater(SettingsScreen.class);
                    }
                });
        content.addView(reset);
    }

    private void rebuildLater(final Class<? extends Screen> keep) {
        post(new Runnable() {
            @Override
            public void run() {
                host.applyAppearance(keep);
            }
        });
    }

    /** Accent colour swatch. */
    private final class Swatch extends FocusCard {

        Swatch(Context c, final int index, int color) {
            super(c);
            setRadiusDp(15f);
            setFocusScale(1.06f);
            setFill(Theme.alpha(color, 0.16f));
            setContentDescription(c.getString(ACCENT_NAMES[index]));

            View dot = new View(c);
            dot.setBackground(Ui.round(color, 999f));
            FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(Theme.dp(26), Theme.dp(26));
            dotParams.gravity = Gravity.CENTER;
            addView(dot, dotParams);

            ImageView check = new ImageView(c);
            check.setImageBitmap(Ui.vectorBitmap(c, R.drawable.ic_check, Theme.dp(14), 0xFF05070B));
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(Theme.dp(14), Theme.dp(14));
            checkParams.gravity = Gravity.CENTER;
            addView(check, checkParams);

            addFocusListener(new FocusListener() {
                @Override
                public void onCardFocus(FocusCard card, boolean focused) {
                    setFill(Theme.alpha(Theme.ACCENTS[index], focused ? 0.28f : 0.16f));
                }
            });
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (host.prefs().accentIndex() == index) {
                        return;
                    }
                    host.prefs().setAccentIndex(index);
                    rebuildLater(SettingsScreen.class);
                }
            });
        }
    }

    /** Background preview tile. */
    private final class WallpaperSwatch extends FocusCard {

        WallpaperSwatch(Context c, final int index) {
            super(c);
            setRadiusDp(14f);
            setFocusScale(1.05f);
            setFill(Theme.SURFACE);
            setContentDescription(c.getString(WALLPAPER_NAMES[index]));

            final int width = Theme.dp(108);
            final int height = Theme.dp(64);
            ImageView preview = new ImageView(c);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bitmap = GlowBackground.create(index, width, height);
            preview.setImageBitmap(bitmap);

            addView(preview, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Theme.dpf(14f));
                }
            });

            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (host.prefs().wallpaperIndex() == index) {
                        return;
                    }
                    host.prefs().setWallpaperIndex(index);
                    rebuildLater(SettingsScreen.class);
                }
            });
        }
    }
}
