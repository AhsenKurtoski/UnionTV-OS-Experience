package com.ahsenkurtoski.uniontvos;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ahsenkurtoski.uniontvos.core.AppRepository;
import com.ahsenkurtoski.uniontvos.core.Prefs;
import com.ahsenkurtoski.uniontvos.ui.AboutScreen;
import com.ahsenkurtoski.uniontvos.ui.AppsScreen;
import com.ahsenkurtoski.uniontvos.ui.GlowBackground;
import com.ahsenkurtoski.uniontvos.ui.HomeScreen;
import com.ahsenkurtoski.uniontvos.ui.NetworkScreen;
import com.ahsenkurtoski.uniontvos.ui.Screen;
import com.ahsenkurtoski.uniontvos.ui.SettingsScreen;
import com.ahsenkurtoski.uniontvos.ui.SystemScreen;
import com.ahsenkurtoski.uniontvos.ui.Theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single activity launcher.
 *
 * Screens are plain views kept in a small stack; there are no fragments, no
 * navigation library and no background services. The activity is registered for
 * ACTION_MAIN + CATEGORY_HOME and is only made the default home when the user
 * chooses it in the system settings.
 */
public class MainActivity extends Activity implements AppRepository.Listener {

    private FrameLayout root;
    private FrameLayout screenLayer;
    private FrameLayout overlayLayer;

    private Prefs prefs;
    private AppRepository repository;

    private final Map<Class<?>, Screen> screenCache = new HashMap<Class<?>, Screen>();
    private final List<Screen> stack = new ArrayList<Screen>();

    private BroadcastReceiver packageReceiver;
    private View overlayReturnFocus;

    private static final int REQUEST_SSID = 41;

    /**
     * The interface is authored in English; pinning the app locale keeps dates,
     * times and collation consistent instead of mixing an English UI with the
     * system language.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.setLocale(Locale.ENGLISH);
        super.attachBaseContext(newBase.createConfigurationContext(configuration));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Locale.setDefault(Locale.ENGLISH);
        prefs = new Prefs(this);
        Theme.init(this, Theme.ACCENTS[prefs.accentIndex()], prefs.reduceMotion());

        repository = AppRepository.get(this);
        repository.setListener(this);

        root = new FrameLayout(this);
        root.setClipChildren(false);
        screenLayer = new FrameLayout(this);
        overlayLayer = new FrameLayout(this);
        overlayLayer.setVisibility(View.GONE);

        root.addView(screenLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(overlayLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        applyBackground();

        push(screen(HomeScreen.class));
        requestSsidPermission();
    }

    /**
     * Android 8+ hides the Wi-Fi SSID behind location access. Some TV firmwares
     * only honour the precise permission, so both are requested once, with no
     * repeated nagging, to let the top bar show the real network name.
     */
    private void requestSsidPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (prefs.ssidFinePrompted()) {
            return;
        }
        prefs.setSsidFinePrompted(true);
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQUEST_SSID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SSID) {
            Screen top = current();
            if (top != null) {
                top.onHostResume();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        deferRefresh();
        registerPackageReceiver();
        Screen top = current();
        if (top != null) {
            top.onHostResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Screen top = current();
        if (top != null) {
            top.onHostPause();
        }
        unregisterPackageReceiver();
    }

    @Override
    protected void onDestroy() {
        repository.setListener(null);
        unregisterPackageReceiver();
        super.onDestroy();
    }

    /** Starts discovery after the first frame, so the loading UI paints fast. */
    private void deferRefresh() {
        if (root == null) {
            repository.refresh(false);
            return;
        }
        root.post(new Runnable() {
            @Override
            public void run() {
                repository.refresh(false);
            }
        });
    }

    // ------------------------------------------------------------- navigation

    public Prefs prefs() {
        return prefs;
    }

    public AppRepository repository() {
        return repository;
    }

    public Screen current() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    public void open(Class<? extends Screen> type) {
        push(screen(type));
    }

    public void openHome() {
        if (overlayLayer.getVisibility() == View.VISIBLE) {
            hideOverlay();
        }
        while (stack.size() > 1) {
            pop();
        }
    }

    private void push(Screen target) {
        Screen top = current();
        if (top == target) {
            return;
        }
        if (top != null) {
            top.onHidden();
            top.setVisibility(View.GONE);
        }
        target.setVisibility(View.VISIBLE);
        stack.add(target);
        target.onShown();
    }

    private void pop() {
        if (stack.size() <= 1) {
            return;
        }
        Screen leaving = stack.remove(stack.size() - 1);
        leaving.onHidden();
        leaving.onHostPause();
        leaving.setVisibility(View.GONE);

        Screen next = current();
        if (next != null) {
            next.setVisibility(View.VISIBLE);
            next.onShown();
            next.onHostResume();
        }
    }

    private Screen screen(Class<? extends Screen> type) {
        Screen cached = screenCache.get(type);
        if (cached != null) {
            return cached;
        }
        Screen created;
        if (type == HomeScreen.class) {
            created = new HomeScreen(this);
        } else if (type == AppsScreen.class) {
            created = new AppsScreen(this);
        } else if (type == SettingsScreen.class) {
            created = new SettingsScreen(this);
        } else if (type == NetworkScreen.class) {
            created = new NetworkScreen(this);
        } else if (type == SystemScreen.class) {
            created = new SystemScreen(this);
        } else if (type == AboutScreen.class) {
            created = new AboutScreen(this);
        } else {
            throw new IllegalArgumentException("Unknown screen: " + type);
        }
        screenLayer.addView(created, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screenCache.put(type, created);
        return created;
    }

    // ---------------------------------------------------------------- overlay

    public void showOverlay(View content, View returnFocus) {
        overlayReturnFocus = returnFocus;
        overlayLayer.removeAllViews();
        overlayLayer.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlayLayer.setVisibility(View.VISIBLE);
        screenLayer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    public void hideOverlay() {
        overlayLayer.removeAllViews();
        overlayLayer.setVisibility(View.GONE);
        screenLayer.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        final View target = overlayReturnFocus;
        overlayReturnFocus = null;
        if (target != null && target.getParent() != null) {
            target.post(new Runnable() {
                @Override
                public void run() {
                    target.requestFocus();
                }
            });
        }
    }

    public boolean isOverlayOpen() {
        return overlayLayer != null && overlayLayer.getVisibility() == View.VISIBLE;
    }

    // ------------------------------------------------------------- appearance

    /** Re-applies accent/wallpaper/motion and rebuilds the visible screen. */
    public void applyAppearance(Class<? extends Screen> keep) {
        Theme.init(this, Theme.ACCENTS[prefs.accentIndex()], prefs.reduceMotion());
        applyBackground();

        screenLayer.removeAllViews();
        screenCache.clear();
        stack.clear();
        push(screen(keep));
    }

    private void applyBackground() {
        Bitmap bitmap = GlowBackground.create(prefs.wallpaperIndex(), 640, 360);
        BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
        drawable.setFilterBitmap(true);
        drawable.setAntiAlias(true);
        root.setBackground(drawable);
    }

    // ------------------------------------------------------------- lifecycle

    @Override
    public void onAppsChanged() {
        for (Screen screen : screenCache.values()) {
            screen.onAppsChanged();
        }
    }

    public void notifyAppsChanged() {
        onAppsChanged();
    }

    private void registerPackageReceiver() {
        if (packageReceiver != null) {
            return;
        }
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                repository.refresh(true);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        try {
            registerReceiver(packageReceiver, filter);
        } catch (Throwable t) {
            packageReceiver = null;
        }
    }

    private void unregisterPackageReceiver() {
        if (packageReceiver == null) {
            return;
        }
        try {
            unregisterReceiver(packageReceiver);
        } catch (Throwable ignored) {
            // Already gone; nothing to do.
        }
        packageReceiver = null;
    }

    // ----------------------------------------------------------------- keys

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus || isOverlayOpen()) {
            return;
        }
        View focused = getWindow().getDecorView().findFocus();
        Screen top = current();
        if (focused == null && top != null) {
            top.onShown();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!isOverlayOpen()) {
                open(SettingsScreen.class);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (isOverlayOpen()) {
            hideOverlay();
            return;
        }
        Screen top = current();
        if (top != null && top.handleBack()) {
            return;
        }
        if (stack.size() > 1) {
            pop();
            return;
        }
        // Already on Home: stay here instead of leaving the launcher blank.
    }
}
