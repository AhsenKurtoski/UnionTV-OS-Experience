# Performance report

All figures were measured on the physical TCL UnionTV over ADB, not on an
emulator.

**Release:** [UnionTV OS Experience 1.1.4 (6)](https://github.com/AhsenKurtoski/UnionTV-OS-Experience/releases/tag/v1.1.4)

## 1.1.4 icon-loading update

`1.1.4` resolves every icon before publishing the application list, using a
three-thread pool and a PNG cache in `filesDir/icons`. Measured on the
reference TV: first frame cold **1,195 ms**, warm **543 ms**; the loading state
contains no letter placeholders. The rendering and memory tables below are the
1.1.2 baseline for the unchanged drawing path; version, size and storage rows
are current for 1.1.4.

## Test device

| Property | Value |
| --- | --- |
| Model / brand | UnionTV / TCL |
| Android | 9 (API 28) |
| ABI | armeabi-v7a (32-bit) |
| SoC | Realtek rtd284x, 4× ARMv7 |
| Display | 1280 × 720, 213 dpi (tvdpi) |
| RAM | 1,437,396 kB total, low-RAM device (`ro.config.low_ram=true`) |
| Installed apps | 27 launchable applications |

## Build

| Property | Value |
| --- | --- |
| Version | 1.1.4 (6) |
| APK size | 80,399 bytes (78.5 KB) |
| Installed APK | 80,399 bytes |
| Third-party libraries | none |
| Native libraries | none |
| Release build | R8 minification + resource shrinking |

The APK contains only the app's own bytecode, resources and four small PNG
launcher assets. All icon rendering, layout and drawing use the Android
framework.

## Startup

`adb shell am start -W -n com.ahsenkurtoski.uniontvos/.MainActivity`

| Run | `ThisTime` | `TotalTime` |
| --- | ---: | ---: |
| Cold 1.1.4 (loading frame) | 1,195 ms | 1,195 ms |
| Warm 1.1.4 (disk icon cache) | 543 ms | 1,118 ms |
| Cold 1.1.2 baseline | 538 ms | 2,131 ms |
| Cold 2 (1.1.2 baseline) | 554 ms | 1,992 ms |
| Cold 3 (1.1.2 baseline) | 554 ms | 2,087 ms |
| Warm (Home → app, 1.1.2 baseline) | 177 ms | 177 ms |
| Warm (Home → app, 1.1.2 baseline) | 194 ms | 194 ms |
| Warm (Home → app, 1.1.2 baseline) | 181 ms | 181 ms |

`ThisTime` is the time to the first drawn frame. For the 1.1.2 baseline,
`TotalTime` additionally waited for the app to become idle (including icon
decoding); 1.1.4 starts icon decoding after the first loading frame, so its
`TotalTime` is not an icon-ready measurement. Cold figures vary with ART and
package-manager state.

The 1.1.2 warm baseline was 0.18–0.19 s; the 1.1.4 warm start is 543 ms with
the disk icon cache. The activity remains `singleTask`.

## Memory

`adb shell dumpsys meminfo com.ahsenkurtoski.uniontvos`

| Category | KB |
| --- | ---: |
| Total PSS | 34,353 |
| Java heap | 1,820 |
| Native heap | 12,712 |
| Code (dex, oat, apk mapping) | 10,944 |
| Stack | 316 |
| Graphics | 0 |
| Private Other | 876 |
| System | 7,685 |
| Swap PSS | 58 |

The memory table above is the 1.1.2 baseline; 1.1.4 adds a three-thread icon
pool and a small disk cache and was not re-profiled with the same PSS harness.
The in-memory icon cache is capped at 3 MB (`LruCache`), icons are decoded once
at 72 dp and the wallpaper is a single 640 × 360 bitmap. There is no database
or service.

## Rendering

`adb shell dumpsys gfxinfo com.ahsenkurtoski.uniontvos reset`, followed by ten
D-pad moves across the horizontal application rail:

```
Total frames rendered: 88
Janky frames: 0 (0.00%)
50th percentile: 12ms
90th percentile: 15ms
95th percentile: 15ms
99th percentile: 16ms
```

The one-time full-page jump from the application rail to Quick access (DOWN)
is the only measurable hitch on the 1.4 GB panel:

```
Total frames rendered: 17
Janky frames: 5 (29.41%)
50th percentile: 13ms
90th percentile: 22ms
95th percentile: 22ms
99th percentile: 22ms
```

At 60 Hz the frame budget is 16.7 ms. The horizontal rails stay within 16 ms at
p99; the vertical jump spends five 22 ms frames on the focus/scroll transition,
then the screen is static again. A mixed ten-step run (five RIGHT + five DOWN)
measured 78 frames with 4–7 janky frames (5–9%), p50 12 ms, p90 16 ms and p99
23–28 ms. The panel runs at 60 fps.

## Storage

| Item | Value |
| --- | --- |
| App APK | 80,399 bytes |
| `/data` volume | 4,428,696 kB total, 1,888,328 kB free (58 % used overall) |

The app writes one small `SharedPreferences` file for settings, favourites and
last-used timestamps, plus up to 27 icon PNGs in `filesDir/icons`. The icon
cache is disposable and is rebuilt automatically.

## Cleanup performed on the reference TV

| Step | Result |
| --- | --- |
| Before cleanup | 882 MB free on `/data` (80% used), 31 launchable apps |
| Removed Netflix | `pm uninstall --user 0 com.netflix.ninja` (42.7 MB, system app) |
| Removed Kodi | `pm uninstall org.xbmc.kodi` (64.7 MB, user app; data/addons deleted) |
| Removed Pluto TV | `pm uninstall tv.pluto.android` (22.2 MB, user app) |
| Removed AirScreen | `pm uninstall com.ionitech.airscreen` (128.2 MB, user app) |
| Google TV launcher | `pm uninstall --user 0 com.google.android.tvlauncher` (system APK retained; makes UnionTV the only Home) |
| Cache/background trim | `pm trim-caches` + `am kill-all` |
| After cleanup | 1,888,328 kB free (58% used), 27 launchable apps |

RAM after the full cleanup: `MemFree` 481,772 kB and `MemAvailable` 761,096 kB.
After the earlier Netflix/Kodi pass the figures were 457,456 / 743,688 kB;
later passes reached 522,784 / 750,044 kB. Android reuses free RAM for cache,
so `MemAvailable` is the useful figure.

Netflix can be restored with `cmd package install-existing com.netflix.ninja`.
Kodi, Pluto TV and AirScreen were user apps; reinstall them from Google Play or
their official APKs. Their old data and addons are gone.

## Why it stays light

- No RecyclerView or support libraries: rails and the grid are custom
  `LinearLayout`/`FrameLayout` containers with manual recycling of views on
  refresh.
- Icons are decoded once and reused from memory and `filesDir`; a three-thread
  pool handles the first pass without touching the UI thread.
- The background is generated procedurally once per appearance change.
- The clock updates once per minute, aligned to the minute boundary.
- Network state uses `registerDefaultNetworkCallback` only while the activity is
  resumed; no polling and no wakelocks.
- No services, no broadcast receivers beyond a runtime-registered package-change
  receiver while resumed, no ads and no analytics.

## How to reproduce

```bash
adb shell am force-stop com.ahsenkurtoski.uniontvos
adb shell am start -W -n com.ahsenkurtoski.uniontvos/.MainActivity
adb shell dumpsys meminfo com.ahsenkurtoski.uniontvos
adb shell dumpsys gfxinfo com.ahsenkurtoski.uniontvos reset
# ten RIGHT presses on the Home app rail, then repeat with DOWN to Quick access
adb shell dumpsys gfxinfo com.ahsenkurtoski.uniontvos
```
