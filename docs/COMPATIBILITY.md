# Compatibility report

## Target device (audited)

| Property | Value |
| --- | --- |
| Model | UnionTV |
| Brand | TCL |
| Android | 9 (API 28) |
| UnionTV app | 1.1.4 (6) |
| Build fingerprint | `TCL/UnionTV/UnionTV:9/PPR2.180905.006.A1/builder.20201203.103024:user/release-keys` |
| CPU | 4× ARMv7, Realtek rtd284x |
| ABI | armeabi-v7a |
| Display | 1280 × 720, 213 dpi, 60 Hz |
| RAM | 1,437,396 kB, `ro.config.low_ram=true` |
| Default home before testing | `com.google.android.tvlauncher/.MainActivity` |
| Default home after setup | UnionTV OS Experience |
| Root | not available |
| Locale | system `it-IT`, product `en-GB` |

## Verified on the device

| Check | Result |
| --- | --- |
| `adb install -r` | Success |
| App launches from ADB and from the app list | Yes |
| Home renders at 1280 × 720 without clipping | Yes |
| Initial focus lands on the first application card | Yes |
| D-pad right / down / up move focus predictably | Yes |
| Visible focus ring on every focused control | Yes |
| `MENU` opens Settings | Yes |
| `BACK` returns to the previous screen and restores focus | Yes |
| Application discovery | 27 launchable apps, real labels and icons |
| Icon loading | Real icons before cards; no letter placeholders; cache in `filesDir` |
| Home checker | `tools/home.sh check` passes; System screen shows Default Home |
| Cleanup | Netflix, Kodi, Pluto TV and AirScreen removed; Google TV launcher removed for user 0 |
| Wi-Fi SSID in top bar and Network | Real SSID with green connected dot; redacted in published captures |
| Network details | IP/gateway values redacted in the published capture; interface wlan0 |
| Applications grid initial focus | Canali TV first; RIGHT → BrowseHere; DOWN → Impostazioni |
| Quick access navigation | DOWN → Settings; RIGHT → All apps → Network; no crash |
| Home top bar while scrolling | Legible; scrim removes content overlap |
| Launching a real app (BrowseHere, Downloader) | Yes |
| Long-press app actions menu | Yes (Open, favourite, info, close) |
| Add / remove favourite updates the Home rail | Yes |
| Accent change applies immediately and stays on Settings | Yes |
| Settings persist across screens | Yes |
| English date (“Friday 6 November”) in an English UI | Yes |
| Default home after explicit setup | UnionTV OS Experience (only HOME candidate for user 0) |
| Default home after full reboot | UnionTV (verified) |
| Google TV launcher | System APK retained; removed for user 0, restorable with `cmd package install-existing` |
| Crashes / ANRs during the test session | None |
| Post-OTA check | `bash tools/home.sh check` (read-only; exit 0 = UnionTV HOME) |

## Supported versions

| Item | Value |
| --- | --- |
| `minSdkVersion` | 21 (Android 5.0) |
| `targetSdkVersion` | 28 (Android 9) |
| `compileSdkVersion` | 33 |
| ABIs | all — no native code is packaged |
| Leanback required | no (`android.software.leanback` is optional) |
| Touchscreen required | no |

The interface uses only framework classes available since API 21
(`View`, `LinearLayout`, `ScrollView`, `LruCache`, `ConnectivityManager`,
`PackageManager`, vector drawables via `VectorDrawableCompat`-free framework
support). No AndroidX, Kotlin, Compose or Leanback dependency is present.

## Registration

The activity declares:

```
ACTION_MAIN
CATEGORY_HOME
CATEGORY_DEFAULT
CATEGORY_LEANBACK_LAUNCHER
```

with `launchMode="singleTask"`, `stateNotNeeded="true"`,
`excludeFromRecents="true"` and a fixed landscape orientation.

Android therefore offers it as a Home app and as a Leanback launcher entry. The
app never selects itself; the default was chosen explicitly.

## Platform behaviour and caveats

- **Menu key**: opens Settings. Remotes that send a different key code for their
  menu button may not trigger it; all other navigation uses standard D-pad
  codes.
- **Long press**: implemented through `View.OnLongClickListener`, so it works
  with a held OK button on the remote and with a touch hold.
- **Touch mode**: opening the actions menu by touch keeps the app in touch mode;
  action rows are focusable in touch mode so remote navigation still works
  immediately.
- **Application labels** are shown exactly as each app declares them; system
  apps on an Italian television therefore appear with Italian names.
- **Featured** uses favourites first, then recently used apps; it hides itself
  when neither exists.
- The app runs as a normal user application. It cannot replace system
  components, change firmware or modify other apps.

## Limitations

- English-only interface copy (no `values-it` yet).
- The SSID is hidden if the TV's Location toggle is off (Android requires it).
- The **Canali TV** pin is specific to the TCL `com.tcl.tv` input.
- On this TV the Google TV launcher is removed for user 0; a firmware/OTA update
  may restore it. The system APK is retained and can be restored.
- No search, voice assistant integration, widgets or app folders.
- No drag-and-drop reordering of rails.
- Four built-in procedural backgrounds; no custom wallpaper import.
- No multi-user profile support beyond each user's own app data.
- Long lists scroll with the D-pad; there is no “page up/down” shortcut.

## Disclaimer

Independent project, **not affiliated with TCL, Google or any application
vendor**. Names, icons and trademarks belong to their owners.
