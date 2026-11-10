# Recovery guide

The Google TV system APK is never modified or deleted. On the reference TV its
launcher was removed for user 0 to make UnionTV the only Home; the app itself
does **not** change the default home. These steps restore Google TV or remove
UnionTV.

## 1. Restore the original home screen

Restore the Google TV launcher for user 0 if it was removed:

```bash
adb shell cmd package install-existing com.google.android.tvlauncher
```

Then:

1. Open **Settings**.
2. Go to **Apps → Default apps → Home** (the exact path varies slightly by
   firmware).
3. Select **Google TV** / **Home**.

The original launcher starts immediately.

## 2. Restore from ADB

```bash
adb connect <tv-ip>:5555

# Restore the system launcher for user 0 if it was removed
adb shell cmd package install-existing com.google.android.tvlauncher

# Make the original launcher the default again
adb shell cmd package set-home-activity \
  com.google.android.tvlauncher/com.google.android.tvlauncher.MainActivity

# Or simply open it once, without changing the default
adb shell am start -n com.google.android.tvlauncher/.MainActivity
```

If `cmd package set-home-activity` is not available on the firmware, use the
Settings path in section 1.

## 3. Uninstall UnionTV OS Experience

```bash
adb uninstall com.ahsenkurtoski.uniontvos
```

Or on the TV: **Settings → Apps → UnionTV OS Experience → Uninstall**.

Uninstalling removes the app, its preferences, favourites and usage history.
If the Google TV launcher is still removed for user 0, restore it first
(section 1); otherwise the TV falls back to the TCL fallback home.

## 4. If the TV starts with a blank screen

This should not happen: the app is a normal launcher candidate, and Android
falls back to the previous home app if it is uninstalled. If the screen is
blank:

1. Press **Home** on the remote; Android shows the home selection screen when
   more than one home app is installed.
2. Choose **Google TV**. If Google TV is not listed, restore it with section 1
   first.
3. If the remote does not respond, reconnect ADB and run the command from
   section 2, then uninstall the app.

## 5. No root, no firmware changes

No step in this project requires root, fastboot, recovery mode or a firmware
flash. The app is installed and removed like any other user application.

## 6. Restore applications removed during cleanup

The reference TV had Netflix, Kodi, Pluto TV and AirScreen removed to free
storage.

Netflix is a system app removed only for user 0. Restore it with:

```bash
adb shell cmd package install-existing com.netflix.ninja
```

Kodi, Pluto TV and AirScreen were user apps. Reinstall them from Google Play or
their official APKs. Their previous data, settings and addons were deleted with
the uninstall and cannot be restored from the TV.

## 7. Persistence

UnionTV stores settings and favourites in `SharedPreferences`; it has no
scheduled reset and no Internet permission. On the reference TV the Google TV
launcher was removed for user 0, HOME opened UnionTV, and the setup survived a
full reboot.

A firmware or Google TV update can restore the system launcher. The Google TV
APK is still on the system partition: use section 1 to restore it, then select
UnionTV again if you want it back as the only Home. The app itself never changes
the system default.

## 8. Home switch and post-update check

Use the safe wrapper for all Home operations:

```bash
bash tools/home.sh status
bash tools/home.sh check
bash tools/home.sh uniontv
bash tools/home.sh googletv
```

`check` is read-only and exits 0 only when UnionTV is the resolved HOME
activity; run it after a firmware update. The System screen also shows the
current **Default Home** value. `uniontv` can be given `--remove-google-tv` if
the update restored Google TV as the default; the system APK remains restorable
with `googletv`.
