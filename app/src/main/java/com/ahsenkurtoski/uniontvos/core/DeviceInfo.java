package com.ahsenkurtoski.uniontvos.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import java.util.Locale;

/** Device facts for the System and About screens. All reads are cheap and local. */
public final class DeviceInfo {

    private DeviceInfo() {
    }

    public static String manufacturer() {
        return capitalize(Build.MANUFACTURER);
    }

    public static String model() {
        return Build.MODEL;
    }

    public static String androidRelease() {
        return Build.VERSION.RELEASE;
    }

    public static int sdk() {
        return Build.VERSION.SDK_INT;
    }

    public static String abi() {
        String[] abis = Build.SUPPORTED_ABIS;
        if (abis != null && abis.length > 0) {
            return abis[0];
        }
        return Build.CPU_ABI == null ? "—" : Build.CPU_ABI;
    }

    public static int cpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static int displayWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int displayHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int densityDpi(Context context) {
        return context.getResources().getDisplayMetrics().densityDpi;
    }

    public static String gpu(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                String version = am.getDeviceConfigurationInfo().getGlEsVersion();
                return "OpenGL ES " + version;
            }
        } catch (Throwable ignored) {
        }
        return "—";
    }

    public static String ramSummary(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            long used = info.totalMem - info.availMem;
            return formatBytes(used) + " / " + formatBytes(info.totalMem);
        } catch (Throwable ignored) {
            return "—";
        }
    }

    public static long ramTotal(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            return info.totalMem;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static long[] storage() {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            return new long[]{stat.getAvailableBytes(), stat.getTotalBytes()};
        } catch (Throwable ignored) {
            return new long[]{0, 0};
        }
    }

    public static String uptime() {
        long totalMinutes = SystemClock.elapsedRealtime() / 60000L;
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes % (60 * 24)) / 60;
        long minutes = totalMinutes % 60;
        StringBuilder out = new StringBuilder();
        if (days > 0) {
            out.append(days).append("d ");
        }
        if (days > 0 || hours > 0) {
            out.append(hours).append("h ");
        }
        out.append(minutes).append("m");
        return out.toString();
    }

    public static String defaultHome(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo info = pm.resolveActivity(intent, 0);
            if (info != null) {
                if (info.activityInfo != null && context.getPackageName().equals(info.activityInfo.packageName)) {
                    return context.getString(com.ahsenkurtoski.uniontvos.R.string.app_name);
                }
                CharSequence label = info.loadLabel(pm);
                if (label != null && label.length() > 0) {
                    return label.toString();
                }
            }
        } catch (Throwable ignored) {
        }
        return "—";
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 MB";
        }
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gb >= 1.0) {
            return String.format(Locale.US, "%.1f GB", gb);
        }
        double mb = bytes / (1024.0 * 1024.0);
        return String.format(Locale.US, "%.0f MB", mb);
    }

    private static String capitalize(String value) {
        if (value == null || value.length() == 0) {
            return "—";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.getDefault());
    }
}
