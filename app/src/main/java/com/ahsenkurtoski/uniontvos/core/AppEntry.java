package com.ahsenkurtoski.uniontvos.core;

import android.content.ComponentName;
import android.graphics.Bitmap;

/** One launchable application discovered on the device. */
public final class AppEntry {

    public String label;
    public final String packageName;
    public final ComponentName component;
    public final boolean systemApp;

    /** Always shown first, currently the live TV application. */
    public boolean pinned;
    public boolean favorite;
    public long lastUsed;
    public Bitmap icon;

    public AppEntry(String label, String packageName, ComponentName component, boolean systemApp) {
        this.label = label;
        this.packageName = packageName;
        this.component = component;
        this.systemApp = systemApp;
    }

    /** Stable key for favourites and usage tracking. */
    public String key() {
        return component.flattenToString();
    }

    @Override
    public String toString() {
        return label + " (" + packageName + ")";
    }
}
