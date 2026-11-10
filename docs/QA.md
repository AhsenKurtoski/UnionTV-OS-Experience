# QA and tested limits

**Release:** UnionTV OS Experience 1.1.4 (6)  
**Test device:** TCL UnionTV, Android 9 / API 28, ARMv7, 1280×720, 1.4 GB RAM  
**Test date:** 10 November 2026  
**Test method:** physical television over ADB, visual inspection of TV screenshots, framework counters and read-only package checks.

> **Important:** “No known errors” is not a claim that no defect can ever exist.
> This document records what was actually tested and what was not. There is no
> software that can honestly guarantee “zero bugs forever” on every current and
> future firmware. The release gate below is the repeatable evidence for this
> build.

## Release checks

| Check | Result | Evidence |
| --- | --- | --- |
| Release build completes | **PASS** | `build-1.1.4.log` |
| APK installed | **PASS** | `versionName=1.1.4`, `versionCode=6` |
| APK size | **PASS** | 80,399 bytes |
| APK SHA-256 | **PASS** | `3008043f871dac8b9648b60b4ff3084632a39c5888a7348edc46131e19475e5b` |
| UnionTV resolves as HOME | **PASS** | `cmd package resolve-activity` returns `com.ahsenkurtoski.uniontvos/.MainActivity` |
| `tools/home.sh check` | **PASS**, exit 0 | read-only post-OTA check |
| Google TV removed for user 0 | **PASS** | system APK retained and restorable |
| Cold start, first frame | **PASS** | 1,195 ms |
| Warm start, first frame | **PASS** | 543 ms |
| Loading state without letter placeholders | **PASS** | `v114-loading.png` |
| Home with real icons | **PASS** | 27 applications, `v114-home.png` |
| Applications grid | **PASS** | real labels/icons, sort controls, `v114-apps.png` |
| Settings screen | **PASS** | version 1.1.4, `v114-settings.png` |
| Network screen | **PASS** | Wi-Fi connected, SSID `G403-276D`, IP 192.168.0.154, `v114-network.png` |
| System screen | **PASS** | model/platform/storage facts, `v114-system.png` |
| About screen | **PASS** | 1.1.4, package and compatibility, `v114-about.png` |
| Crash/ANR during capture | **PASS** | no crash or ANR observed |
| Home persistence after reboot | **PASS** | resolved Home remained UnionTV in the reference test |
| TV left on Canali TV | **PASS** | top activity `com.tcl.tv/.TVActivity` |
| 15 s launch video | **PASS** | 1280×720, 30 fps, 15.000 s, H.264/AAC, 1.4 MB |

## Functional test matrix

### Home

| Case | Result |
| --- | --- |
| Default Home opens UnionTV | PASS |
| Brand, clock, date and WiFi status render | PASS |
| Favourites rail appears only when favourites exist | PASS |
| Application rail shows 27 apps on reference TV | PASS |
| Canali TV is first where present | PASS |
| Quick access opens Settings / Apps / Network / System / About | PASS |
| D-pad focus is visible and does not jump off-screen | PASS |
| Warm re-entry does not flash a second loading screen | PASS |
| Home does not change the Android default Home itself | PASS |

### Applications

| Case | Result |
| --- | --- |
| Grid lists launchable apps | PASS |
| Icons and labels are real | PASS |
| A–Z order | PASS |
| Recent order after launch bookkeeping | PASS |
| Favourites sort | PASS |
| OK opens the app | PASS |
| Hold OK opens the action menu | PASS |
| Add/remove favourite updates Home | PASS |
| App info and Close actions | PASS |

### Settings

| Case | Result |
| --- | --- |
| Four accent colours apply immediately | PASS |
| Four backgrounds apply immediately | PASS |
| Reduce motion can be toggled | PASS |
| 24-hour clock changes Home presentation | PASS |
| Favourites-on-Home can be hidden | PASS |
| Restore defaults resets the appearance settings | PASS |
| Settings survive a normal app restart/reboot | PASS |

### Network

| Case | Result |
| --- | --- |
| Connected state shown | PASS |
| SSID shown when Location is enabled | PASS |
| IP, gateway, DNS, link speed and interface shown | PASS |
| Read-only screen makes no network request | PASS |
| No Internet permission | PASS |

### System / About

| Case | Result |
| --- | --- |
| Model/manufacturer/ABI/platform facts present | PASS |
| Memory/storage/display figures present | PASS |
| About version matches installed package | PASS |
| Package and disclaimer present | PASS |
| Back returns to previous screen | PASS |

## Commands used

```bash
# Version and install state
adb shell dumpsys package com.ahsenkurtoski.uniontvos | grep -E 'versionName|versionCode'

# Read-only Home verification
bash tools/home.sh check

# Resolved HOME (brief)
adb shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN \
  -c android.intent.category.HOME | tail -n 1

# Launch timing (reference measurement)
adb shell am force-stop com.ahsenkurtoski.uniontvos
adb shell am start -W -n com.ahsenkurtoski.uniontvos/.MainActivity

# Last visible activity
adb shell dumpsys activity activities \
  | grep -m1 -E 'mResumedActivity|topResumedActivity'
```

Screenshot capture used `adb exec-out screencap -p` and is archived in
[docs/audit](audit/). The release video is a separate rendered asset and is
documented in [launch/README.md](launch/README.md).

## Known limitations and deliberately untested paths

These are not hidden failures; they are scope limits of this QA pass:

1. **Destructive Home switching was not executed.** `bash tools/home.sh uniontv`
   and `bash tools/home.sh googletv` alter the live Home screen. They are
   implemented and syntax-checked, but this pass used only read-only `status`
   and `check` to avoid interrupting the reference TV.
2. **No OTA was performed.** The Google TV APK is retained and the recovery
   procedure is documented, but a real firmware update was not simulated.
3. **One physical device.** Results do not prove identical behaviour on every
   Android TV, every ABI, every launcher firmware or every screen size.
4. **No automated test suite.** The repository has no JVM/instrumentation tests;
   QA is device-level and manual. `lint.abortOnError` is disabled in the
   Android build configuration, so lint is not a release gate.
5. **Not an exhaustive interaction fuzz.** Every screen and normal path was
   inspected; rare key sequences, prolonged use, low-memory pressure and
   non-standard remotes were not fuzzed.
6. **English copy.** Android system/app labels may appear in their own locale.

## What would be needed for a stronger guarantee

- Instrumented tests for discovery, sorting, favourites and preference
  persistence.
- Macrobenchmark or repeated cold/warm startup runs with variance statistics.
- A second physical Android TV with a different ABI and firmware.
- A non-destructive Home-switch harness, or a disposable test device, for
  `uniontv` / `googletv`.
- An OTA/recovery drill and a low-storage test.
- Long-running D-pad and memory-pressure tests.

Until those are executed, the accurate statement is: **no known errors on the
recorded 1.1.4 test matrix; 1.1.4 is the validated reference release.**

## Release gate

Before publishing a new APK:

1. Build with the pinned JDK and inspect the build log.
2. Install on the reference TV and confirm versionCode/versionName.
3. Run `bash tools/home.sh check`.
4. Capture Home, loading, Apps, Settings, Network, System and About.
5. Verify no crash/ANR and that the TV still opens Canali TV.
6. Record the APK SHA-256 and update this document.
7. Re-render the launch video only if the visual story changes.
