package com.ahsenkurtoski.uniontvos.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** All persistent state of the launcher, in one small preferences file. */
public final class Prefs {

    public static final int SORT_AZ = 0;
    public static final int SORT_RECENT = 1;
    public static final int SORT_FAVORITES = 2;

    private static final String FILE = "uniontv_os";

    private static final String KEY_ACCENT = "accent";
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String KEY_REDUCE_MOTION = "reduce_motion";
    private static final String KEY_CLOCK_24 = "clock_24";
    private static final String KEY_FAVORITES_RAIL = "favorites_rail";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_SORT = "sort_mode";
    private static final String KEY_LAST_USED_PREFIX = "last_used:";
    private static final String KEY_SSID_PROMPTED = "ssid_prompted";
    private static final String KEY_SSID_FINE_PROMPTED = "ssid_fine_prompted";

    private final SharedPreferences prefs;

    public Prefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public int accentIndex() {
        return clamp(prefs.getInt(KEY_ACCENT, 0), 0, 3);
    }

    public void setAccentIndex(int index) {
        prefs.edit().putInt(KEY_ACCENT, clamp(index, 0, 3)).apply();
    }

    public int wallpaperIndex() {
        return clamp(prefs.getInt(KEY_WALLPAPER, 0), 0, 3);
    }

    public void setWallpaperIndex(int index) {
        prefs.edit().putInt(KEY_WALLPAPER, clamp(index, 0, 3)).apply();
    }

    public boolean reduceMotion() {
        return prefs.getBoolean(KEY_REDUCE_MOTION, false);
    }

    public void setReduceMotion(boolean value) {
        prefs.edit().putBoolean(KEY_REDUCE_MOTION, value).apply();
    }

    public boolean clock24() {
        return prefs.getBoolean(KEY_CLOCK_24, true);
    }

    public void setClock24(boolean value) {
        prefs.edit().putBoolean(KEY_CLOCK_24, value).apply();
    }

    public boolean favoritesRail() {
        return prefs.getBoolean(KEY_FAVORITES_RAIL, true);
    }

    public void setFavoritesRail(boolean value) {
        prefs.edit().putBoolean(KEY_FAVORITES_RAIL, value).apply();
    }

    public int sortMode() {
        return clamp(prefs.getInt(KEY_SORT, SORT_AZ), 0, 2);
    }

    public void setSortMode(int mode) {
        prefs.edit().putInt(KEY_SORT, clamp(mode, 0, 2)).apply();
    }

    public Set<String> favorites() {
        Set<String> stored = prefs.getStringSet(KEY_FAVORITES, null);
        return stored == null ? new HashSet<String>() : new HashSet<String>(stored);
    }

    public boolean isFavorite(String key) {
        Set<String> stored = prefs.getStringSet(KEY_FAVORITES, null);
        return stored != null && stored.contains(key);
    }

    /** @return true when the key is a favourite after the toggle. */
    public boolean toggleFavorite(String key) {
        Set<String> stored = new HashSet<String>(favorites());
        boolean nowFavorite;
        if (stored.contains(key)) {
            stored.remove(key);
            nowFavorite = false;
        } else {
            stored.add(key);
            nowFavorite = true;
        }
        prefs.edit().putStringSet(KEY_FAVORITES, stored).apply();
        return nowFavorite;
    }

    public boolean ssidPrompted() {
        return prefs.getBoolean(KEY_SSID_PROMPTED, false);
    }

    public void setSsidPrompted(boolean value) {
        prefs.edit().putBoolean(KEY_SSID_PROMPTED, value).apply();
    }

    public boolean ssidFinePrompted() {
        return prefs.getBoolean(KEY_SSID_FINE_PROMPTED, false);
    }

    public void setSsidFinePrompted(boolean value) {
        prefs.edit().putBoolean(KEY_SSID_FINE_PROMPTED, value).apply();
    }

    public long lastUsed(String key) {
        return prefs.getLong(KEY_LAST_USED_PREFIX + key, 0L);
    }

    public void markUsed(String key) {
        prefs.edit().putLong(KEY_LAST_USED_PREFIX + key, System.currentTimeMillis()).apply();
    }

    public void restoreDefaults() {
        Set<String> favorites = favorites();
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_ACCENT);
        editor.remove(KEY_WALLPAPER);
        editor.remove(KEY_REDUCE_MOTION);
        editor.remove(KEY_CLOCK_24);
        editor.remove(KEY_FAVORITES_RAIL);
        editor.remove(KEY_SORT);
        // Favourites are user content, not an appearance setting: keep them.
        editor.apply();
        if (!favorites.isEmpty()) {
            prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
        }
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }
}
