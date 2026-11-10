# Changelog

All notable changes to the UnionTV OS Experience launcher are recorded here.
The app version is the Android `versionName`; the number in parentheses is the
`versionCode`.

## [1.1.4] — 2026-11-10

### Changed

- Reworked first-load presentation: the application rail is published only
  after real icons are resolved, with no letter placeholders.
- Icon decoding now uses a three-thread pool and a persistent PNG cache under
  `filesDir/icons` (not `cacheDir`), plus the existing bounded memory cache.
- Startup defers discovery until after the first frame; the cold frame shows a
  clean **Loading applications…** state.
- Added `loadedOnce` so returning Home does not flash an empty list a second
  time.
- Added `tools/home.sh` with read-only `status`/`check` and explicit
  `uniontv`/`googletv` Home switching. `check` handles the abbreviated
  `.MainActivity` resolver form.
- Expanded GitHub documentation: feature guide, architecture, QA matrix,
  screenshot gallery, recovery, performance and this changelog.
- Added the 15-second launch film, poster, deterministic HTML scene stage and
  original score generator under `docs/launch/`.

### Release facts

| Property | Value |
| --- | --- |
| Version | **1.1.4 (6)** |
| APK | `dist/UnionTV-1.1.4.apk` |
| Size | 80,399 bytes (78.5 KiB) |
| SHA-256 | `3008043f871dac8b9648b60b4ff3084632a39c5888a7348edc46131e19475e5b` |
| Tested device | TCL UnionTV, Android 9 (API 28), ARMv7, 1280×720 |
| Launchable apps | 27 |
| Cold first frame | 1,195 ms |
| Warm first frame | 543 ms |
| Third-party libraries | none |
| Internet permission | none |

### Validation

See [docs/QA.md](docs/QA.md). No known errors were observed on the recorded
1.1.4 matrix; this is not a lifetime guarantee for future firmware or hardware.

## [1.1.3] — earlier iteration

- Intermediate visual/audit pass. The screenshots are archived as
  `docs/audit/v113-*.png` and superseded by the 1.1.4 capture set.
- No separately recorded release notes or APK checksum are available for this
  internal iteration.

## [1.1.2] — earlier iteration

- Rendering baseline documented in [docs/PERFORMANCE.md](docs/PERFORMANCE.md):
  0/88 janky frames across the horizontal rail test; 5/17 on the one-time
  vertical jump; p50/p90/p99 12/15/16 ms for the rail.
- This rendering path is unchanged in 1.1.4; the 1.1.4 work targets icon loading
  and startup.

## [1.0.0] — initial release

- First native launcher: Home, Applications, Settings, Network, System and About
  screens; D-pad focus; runtime app discovery; local favourites; no third-party
  dependencies and no Internet permission.
- The project identity and the **Designed & Developed by Ahsen Kurtoski** credit
  date from this first release.

## Release assets

| Asset | Path |
| --- | --- |
| APK | `dist/UnionTV-1.1.4.apk` |
| Launch film | `docs/launch/uniontv-launch-15s.mp4` |
| Launch poster | `docs/launch/uniontv-launch-poster.png` |
| Screenshot gallery | `docs/SCREENSHOTS.md` |
| QA report | `docs/QA.md` |

## Notes on versioning

- `versionCode` must increase for every published APK.
- Settings and favourites live in `SharedPreferences`; the icon cache is
  disposable and may be rebuilt.
- A firmware/OTA can restore another Home launcher. This is a platform state
  change, not an app update; verify with `bash tools/home.sh check` and follow
  [docs/RECOVERY.md](docs/RECOVERY.md).
