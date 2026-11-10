package com.ahsenkurtoski.uniontvos.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.LruCache;

import com.ahsenkurtoski.uniontvos.R;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Discovers launchable applications, decodes their icons once and keeps the
 * result in memory and in a small on-disk cache. Package queries run on one
 * low-priority background thread; icon decoding uses a small daemon pool so
 * the first list is published with real icons instead of letter placeholders.
 */
public final class AppRepository {

    public interface Listener {
        void onAppsChanged();
    }

    private static AppRepository instance;

    public static synchronized AppRepository get(Context context) {
        if (instance == null) {
            instance = new AppRepository(context.getApplicationContext());
        }
        return instance;
    }

    private static final long CACHE_TTL_MS = 20_000L;
    private static final String TV_PACKAGE = "com.tcl.tv";

    private final Context context;
    private final Prefs prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<AppEntry> apps = new ArrayList<AppEntry>();
    private final LruCache<String, Bitmap> iconCache;
    private final Set<String> pendingIcons = Collections.synchronizedSet(new HashSet<String>());
    private final ExecutorService executor;
    private final File iconDir;
    private final ExecutorService iconExecutor;

    private Listener listener;
    private volatile boolean loading;
    private volatile boolean loadedOnce;
    private long loadedAt;

    private final Runnable notifyRunnable = new Runnable() {
        @Override
        public void run() {
            if (listener != null) {
                listener.onAppsChanged();
            }
        }
    };

    private AppRepository(Context context) {
        this.context = context;
        this.prefs = new Prefs(context);
        this.iconCache = new LruCache<String, Bitmap>(3 * 1024) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return Math.max(1, value.getByteCount() / 1024);
            }
        };
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "uniontv-apps");
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        });
        this.iconDir = new File(context.getFilesDir(), "icons");
        if (!iconDir.exists()) {
            iconDir.mkdirs();
        }
        this.iconExecutor = Executors.newFixedThreadPool(3, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "uniontv-icons");
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        });
    }

    public Prefs prefs() {
        return prefs;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Current (unsorted) snapshot. Do not mutate. */
    public List<AppEntry> apps() {
        return apps;
    }

    public boolean isLoading() {
        return loading;
    }

    /** True after the first list-and-icon pass has completed (even if empty). */
    public boolean isLoadedOnce() {
        return loadedOnce;
    }

    public List<AppEntry> favorites() {
        List<AppEntry> result = new ArrayList<AppEntry>();
        for (AppEntry entry : apps) {
            if (entry.favorite) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Returns a sorted copy for a given {@link Prefs} sort mode. */
    public List<AppEntry> sorted(int mode) {
        List<AppEntry> copy = new ArrayList<AppEntry>(apps);
        final Collator collator = Collator.getInstance(Locale.getDefault());
        Comparator<AppEntry> byLabel = new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return collator.compare(a.label, b.label);
            }
        };
        if (mode == Prefs.SORT_RECENT) {
            Collections.sort(copy, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    if (a.lastUsed != b.lastUsed) {
                        return a.lastUsed > b.lastUsed ? -1 : 1;
                    }
                    return byLabel.compare(a, b);
                }
            });
        } else if (mode == Prefs.SORT_FAVORITES) {
            Collections.sort(copy, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    if (a.favorite != b.favorite) {
                        return a.favorite ? -1 : 1;
                    }
                    return byLabel.compare(a, b);
                }
            });
        } else {
            Collections.sort(copy, byLabel);
        }

        // The live TV application is always the first entry, in every sort mode.
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i).pinned) {
                AppEntry pinned = copy.remove(i);
                copy.add(0, pinned);
                break;
            }
        }
        return copy;
    }

    /** Reloads the application list. Cheap no-op while a load is in flight. */
    public void refresh(boolean force) {
        if (loading) {
            return;
        }
        if (!force && !apps.isEmpty() && SystemClock.elapsedRealtime() - loadedAt < CACHE_TTL_MS) {
            return;
        }
        loading = true;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<AppEntry> loaded;
                try {
                    loaded = queryApps();
                    // Resolve every icon, from the disk cache or PackageManager,
                    // before publishing the list. The Home screen can therefore
                    // never render a letter placeholder for a first load.
                    decodeIcons(loaded);
                } catch (Throwable t) {
                    loaded = new ArrayList<AppEntry>();
                }
                final List<AppEntry> result = loaded;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        apps.clear();
                        apps.addAll(result);
                        loading = false;
                        loadedOnce = true;
                        loadedAt = SystemClock.elapsedRealtime();
                        if (listener != null) {
                            listener.onAppsChanged();
                        }
                    }
                });
            }
        });
    }

    /** Decodes missing icons in parallel, checking the disk cache first. */
    private void decodeIcons(List<AppEntry> found) {
        if (found.isEmpty()) {
            return;
        }
        final CountDownLatch done = new CountDownLatch(found.size());
        for (final AppEntry entry : found) {
            iconExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap icon = decodeIcon(entry);
                        if (icon != null) {
                            entry.icon = icon;
                        }
                    } finally {
                        done.countDown();
                    }
                }
            });
        }
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Decodes a single missing icon without blocking the caller. */
    public void ensureIcon(final AppEntry entry) {
        if (entry == null || entry.icon != null) {
            return;
        }
        if (!pendingIcons.add(entry.key())) {
            return;
        }
        iconExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = decodeIcon(entry);
                if (bitmap != null) {
                    entry.icon = bitmap;
                }
                pendingIcons.remove(entry.key());
                if (bitmap != null) {
                    main.post(notifyRunnable);
                }
            }
        });
    }

    public void syncFavorites() {
        for (AppEntry entry : apps) {
            entry.favorite = prefs.isFavorite(entry.key());
            entry.lastUsed = prefs.lastUsed(entry.key());
        }
    }

    public void markUsed(AppEntry entry) {
        prefs.markUsed(entry.key());
        entry.lastUsed = System.currentTimeMillis();
    }

    private Bitmap decodeIcon(AppEntry entry) {
        Bitmap cached = iconCache.get(entry.key());
        if (cached != null) {
            return cached;
        }
        File file = iconFile(entry);
        if (file.isFile()) {
            Bitmap disk = null;
            try {
                disk = BitmapFactory.decodeFile(file.getAbsolutePath());
            } catch (Throwable ignored) {
                disk = null;
            }
            if (disk != null) {
                iconCache.put(entry.key(), disk);
                return disk;
            }
            file.delete();
        }
        int sizePx = Math.max(48, Math.round(72f * context.getResources().getDisplayMetrics().density));
        Bitmap bitmap = null;
        try {
            Drawable drawable = context.getPackageManager().getActivityIcon(entry.component);
            if (drawable != null) {
                drawable = drawable.mutate();
                bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, sizePx, sizePx);
                drawable.draw(canvas);
            }
        } catch (Throwable ignored) {
            bitmap = null;
        }
        if (bitmap != null) {
            iconCache.put(entry.key(), bitmap);
            writeIconCache(file, bitmap);
        }
        return bitmap;
    }

    private File iconFile(AppEntry entry) {
        String name = UUID.nameUUIDFromBytes(
                entry.key().getBytes(StandardCharsets.UTF_8)).toString();
        return new File(iconDir, name + ".png");
    }

    /** Writes atomically so a killed process never leaves a half PNG. */
    private void writeIconCache(File file, Bitmap bitmap) {
        File temp = new File(file.getPath() + ".tmp");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(temp);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                temp.delete();
                return;
            }
            out.flush();
            out.close();
            out = null;
            if (file.exists()) {
                file.delete();
            }
            if (!temp.renameTo(file)) {
                temp.delete();
            }
        } catch (Throwable ignored) {
            temp.delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                    // Nothing else to close.
                }
            }
        }
    }

    private List<AppEntry> queryApps() {
        PackageManager pm = context.getPackageManager();
        Map<String, AppEntry> found = new LinkedHashMap<String, AppEntry>();
        collect(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, found);
        collect(pm, Intent.CATEGORY_LAUNCHER, found);

        List<AppEntry> result = new ArrayList<AppEntry>(found.values());
        applyPinnedTv(result);
        final Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(result, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return collator.compare(a.label, b.label);
            }
        });
        return result;
    }

    /**
     * Keeps the television's own live TV application first, labelled
     * "Canali TV". Discovery stays dynamic: if the package is absent the list
     * is simply left untouched.
     */
    private void applyPinnedTv(List<AppEntry> list) {
        AppEntry best = null;
        for (int i = 0; i < list.size(); i++) {
            AppEntry entry = list.get(i);
            if (!TV_PACKAGE.equals(entry.packageName)) {
                continue;
            }
            if (best == null || entry.component.getClassName().endsWith("TVActivity")) {
                best = entry;
            }
        }
        if (best == null) {
            try {
                Intent launch = context.getPackageManager().getLaunchIntentForPackage(TV_PACKAGE);
                if (launch != null && launch.getComponent() != null) {
                    best = new AppEntry(context.getString(R.string.tv_channels_label), TV_PACKAGE,
                            launch.getComponent(), true);
                    best.favorite = prefs.isFavorite(best.key());
                    best.lastUsed = prefs.lastUsed(best.key());
                }
            } catch (Throwable ignored) {
                best = null;
            }
        }
        if (best == null) {
            return;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            if (TV_PACKAGE.equals(list.get(i).packageName) && list.get(i) != best) {
                list.remove(i);
            }
        }
        if (!list.contains(best)) {
            list.add(best);
        }
        best.label = context.getString(R.string.tv_channels_label);
        best.pinned = true;
    }

    private void collect(PackageManager pm, String category, Map<String, AppEntry> out) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ResolveInfo> resolved;
        try {
            resolved = pm.queryIntentActivities(intent, 0);
        } catch (Throwable t) {
            return;
        }
        if (resolved == null) {
            return;
        }
        String self = context.getPackageName();
        for (int i = 0; i < resolved.size(); i++) {
            ResolveInfo info = resolved.get(i);
            ActivityInfo activity = info.activityInfo;
            if (activity == null || activity.packageName == null || activity.name == null) {
                continue;
            }
            if (self.equals(activity.packageName)) {
                continue;
            }
            if (!activity.enabled) {
                continue;
            }
            if (activity.applicationInfo != null && !activity.applicationInfo.enabled) {
                continue;
            }
            String bufferKey = activity.packageName + "/" + activity.name;
            if (out.containsKey(bufferKey)) {
                continue;
            }
            String label;
            try {
                label = String.valueOf(info.loadLabel(pm)).trim();
            } catch (Throwable t) {
                label = activity.packageName;
            }
            if (label.isEmpty()) {
                label = activity.packageName;
            }
            boolean system = activity.applicationInfo != null
                    && (activity.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            AppEntry entry = new AppEntry(label, activity.packageName,
                    new ComponentName(activity.packageName, activity.name), system);
            entry.favorite = prefs.isFavorite(entry.key());
            entry.lastUsed = prefs.lastUsed(entry.key());
            out.put(bufferKey, entry);
        }
    }

    /** Tries the launcher intent, then the package launch intent. */
    public static boolean launch(Context context, AppEntry entry) {
        if (entry == null) {
            return false;
        }
        Intent direct = new Intent(Intent.ACTION_MAIN);
        direct.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        direct.setComponent(entry.component);
        direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            context.startActivity(direct);
            return true;
        } catch (Throwable ignored) {
            // fall through
        }
        Intent fallback = null;
        try {
            fallback = context.getPackageManager().getLaunchIntentForPackage(entry.packageName);
        } catch (Throwable ignored) {
            fallback = null;
        }
        if (fallback != null) {
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                context.startActivity(fallback);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }
}
