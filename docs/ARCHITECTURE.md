# Architecture

UnionTV OS Experience is a small, dependency-free Android application. The
entire visible interface is drawn with the Android framework: no Compose, no
Leanback, no RecyclerView, no support libraries and no native code.

**Release:** [UnionTV OS Experience 1.1.4 (6)](https://github.com/AhsenKurtoski/UnionTV-OS-Experience/releases/tag/v1.1.4)

## High-level shape

```
MainActivity (singleTask)
  └─ screen stack (FrameLayout)
       ├─ HomeScreen ───────┐
       ├─ AppsScreen ───────┤
       ├─ SettingsScreen ───┤──> AppRepository ──> PackageManager
       ├─ NetworkScreen ────┤       ├─ app discovery
       ├─ SystemScreen ─────┤       ├─ favourites + recents
       └─ AboutScreen ──────┘       ├─ icon memory cache
                                     └─ filesDir/icons PNG cache
       Prefs (SharedPreferences)
       NetworkMonitor (while resumed)
       DeviceInfo (framework facts)
```

There is one activity and no exported service. Screens are plain
`FrameLayout`-based views pushed on an internal stack. Back and Menu are handled
by `MainActivity`; the D-pad focus model is provided by Android's
`View`/`LinearLayout` focus handling plus the app's custom focus ring.

## Source map

| Path | Responsibility |
| --- | --- |
| `MainActivity.java` | Activity lifecycle, screen stack, key handling, first-frame refresh deferral |
| `core/AppRepository.java` | Launchable-app discovery, sorting, favourites, recents, launch, icon pool and caches |
| `core/AppEntry.java` | Immutable application record: package, activity, label, icon, favourite flag |
| `core/Prefs.java` | `SharedPreferences`-backed settings and favourites |
| `core/NetworkMonitor.java` | Connectivity/SSID/IP snapshot and resumed-lifecycle updates |
| `core/DeviceInfo.java` | Android, display, memory and storage facts |
| `ui/HomeScreen.java` | Home layout, status bar, hero, rails, focus and refresh |
| `ui/AppsScreen.java` | Full application grid, sorting and app actions |
| `ui/SettingsScreen.java` | Appearance and interface settings |
| `ui/NetworkScreen.java` | Network details |
| `ui/SystemScreen.java` | Platform and device details |
| `ui/AboutScreen.java` | Project identity and disclaimer |
| `ui/` (remaining) | Cards, rails, focus ring, theme/colour helpers, custom drawing |

The exact package root is `com.ahsenkurtoski.uniontvos`.

## Discovery and launch

`AppRepository` queries `PackageManager` for activities with
`CATEGORY_LEANBACK_LAUNCHER` and `CATEGORY_LAUNCHER`, then filters and
deduplicates them into `AppEntry` values. It does not contain a hardcoded app
list. The TCL `com.tcl.tv` entry is a display pin only: if the package is not
installed, it is absent.

Launching an app resolves the stored component and starts it through the normal
Android launch intent. A local last-used timestamp feeds the **Recent** sort;
this data never leaves the device.

## Icons and first frame

Icon loading is the main 1.1.4 change:

1. `MainActivity` creates the Home frame and shows the loading state.
2. After the first frame, `deferRefresh()` asks the repository to refresh.
3. Discovery runs off the UI thread.
4. Icons are decoded by a small pool. A memory `LruCache` prevents repeated
   decoding; decoded PNGs are also stored under `filesDir/icons`.
5. `loadedOnce` prevents a second empty-list flash when Home is re-entered.
6. The rail is published only when icons are available; there are no letter
   placeholders in 1.1.4.

The disk location is deliberately `filesDir/icons`, not `cacheDir`. System cache
maintenance can remove `cacheDir` content; the icon cache should remain useful
across ordinary cache trims and reboots. It is still disposable: if a PNG is
missing or fails to decode, the repository regenerates it.

Icons are decoded once at the display size (72 dp). The in-memory cache is capped
at 3 MB. The icon pool is bounded at three threads to respect the 1.4 GB RAM
panel.

## State and persistence

`Prefs` wraps one `SharedPreferences` file for:

- accent colour and background style;
- reduced motion;
- 24-hour clock;
- whether the favourite rail is shown;
- favourite package/component identities;
- last-used timestamps.

The default Home itself is an Android package-manager state, not an app
preference. Installing UnionTV does not change it automatically.

## Network and device facts

`NetworkMonitor` uses `ConnectivityManager` and `WifiManager` to read the active
network and, when available, the SSID. It registers a default-network callback
only while the activity is resumed and unregisters it when paused. There is no
polling timer, no wakelock, no service and no Internet socket.

`DeviceInfo` queries `Build`, `DisplayMetrics`, `ActivityManager` memory info and
`StatFs` for the System screen. These values are derived from the framework and
are formatted for display.

## Build and release

- Java 8 source/target compatibility, compiled and tested with JDK 17.
- Android Gradle Plugin; compile SDK 33, min SDK 21, target SDK 28.
- Release builds use R8 minification and resource shrinking.
- The release variant is signed when `keystore.properties` exists; otherwise it
  builds unsigned. Keystore material is git-ignored.
- No third-party dependencies are declared. The release APK is only the app's
  own dex, resources and four small launcher PNGs.

Build command used for this release:

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot"
sh .tools/gradle-7.6.1/bin/gradle :app:assembleRelease --no-daemon --console=plain
```

## Home registration and recovery

The manifest declares the Home and leanback categories. The app itself does not
change the Home setting. On the reference TV the selection was made explicitly;
`tools/home.sh` wraps the ADB operations for verification and recovery. The
Google TV system package is retained (`install-existing` restores it) and the
recovery path is documented in [RECOVERY.md](RECOVERY.md).

## Design constraints

The target is a 1.4 GB, ARMv7, 720p television. The architecture therefore
favours:

- one activity and a flat view hierarchy;
- framework views and custom drawing over a component library;
- one background discovery thread and a small icon pool;
- a bounded memory cache plus a persistent on-disk icon cache;
- minute-aligned clock updates and resumed-only network callbacks;
- no services, ads, analytics, telemetry or Internet permission.

The result is an APK of **80,399 bytes** with no native libraries and no runtime
third-party code. See [PERFORMANCE.md](PERFORMANCE.md) for measured figures and
methodology.
