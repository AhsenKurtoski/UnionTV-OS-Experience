#!/usr/bin/env bash
set -euo pipefail

# UnionTV Home switch and post-OTA check.
#
# Usage:
#   bash tools/home.sh status
#   bash tools/home.sh check
#   bash tools/home.sh uniontv [--remove-google-tv]
#   bash tools/home.sh googletv
#
# Environment:
#   ADB     path to adb (defaults to the bundled platform-tools/adb.exe)
#   SERIAL  device serial when more than one device is connected
#
# `status` and `check` are read-only. `check` exits 0 only when UnionTV is the
# resolved HOME activity, so it can be run after a firmware update.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${ADB:-}" ]]; then
    if [[ -x "$ROOT/.tools/sdk/platform-tools/adb.exe" ]]; then
        ADB="$ROOT/.tools/sdk/platform-tools/adb.exe"
    elif command -v adb >/dev/null 2>&1; then
        ADB="adb"
    else
        echo "adb non trovato. Imposta ADB=/percorso/di/adb." >&2
        exit 2
    fi
fi

SERIAL="${SERIAL:-}"
ADB_ARGS=()
if [[ -n "$SERIAL" ]]; then
    ADB_ARGS=(-s "$SERIAL")
fi

PKG="com.ahsenkurtoski.uniontvos"
UNIONTV_COMPONENT="$PKG/com.ahsenkurtoski.uniontvos.MainActivity"
GOOGLE_PKG="com.google.android.tvlauncher"
GOOGLE_COMPONENT="$GOOGLE_PKG/com.google.android.tvlauncher.MainActivity"

shell() {
    "$ADB" "${ADB_ARGS[@]}" shell "$@" | tr -d '\r'
}

home_activity() {
    shell cmd package resolve-activity --brief \
        -a android.intent.action.MAIN \
        -c android.intent.category.HOME | tail -n 1
}

top_activity() {
    shell dumpsys activity activities \
        | grep -m 1 -E 'mResumedActivity|topResumedActivity' \
        | sed -E 's/^.* (com\.[^ ]+\/[^ ]+) .*$/\1/' || true
}

version_name() {
    shell dumpsys package "$PKG" \
        | sed -n 's/.*versionName=//p' \
        | head -n 1 || true
}

google_installed() {
    shell pm list packages | grep -qx "$GOOGLE_PKG"
}

require_device() {
    if ! "$ADB" "${ADB_ARGS[@]}" get-state >/dev/null 2>&1; then
        echo "Nessuna TV ADB raggiungibile. Controlla la connessione e imposta SERIAL." >&2
        exit 2
    fi
}

is_uniontv_home() {
    local current
    current="$(home_activity)"
    # resolve-activity --brief may print .MainActivity instead of the full class.
    [[ "$current" == "$UNIONTV_COMPONENT" || "$current" == "$PKG/.MainActivity" ]]
}

status() {
    require_device
    echo "UnionTV version : $(version_name)"
    echo "HOME activity   : $(home_activity)"
    echo "Top activity    : $(top_activity)"
    if google_installed; then
        echo "Google TV       : installed for user 0"
    else
        echo "Google TV       : removed for user 0 (system APK retained)"
    fi
}

check() {
    require_device
    local current
    current="$(home_activity)"
    if [[ "$current" == "$UNIONTV_COMPONENT" || "$current" == "$PKG/.MainActivity" ]]; then
        echo "PASS: UnionTV is the resolved HOME activity."
        echo "      $current"
        return 0
    fi
    echo "FAIL: current HOME is $current" >&2
    echo "Run: bash tools/home.sh uniontv" >&2
    return 1
}

uniontv() {
    require_device
    if [[ "${1:-}" == "--remove-google-tv" ]]; then
        shell pm uninstall --user 0 "$GOOGLE_PKG" || true
    fi
    shell cmd package set-home-activity --user 0 "$UNIONTV_COMPONENT"
    shell am start -n "$UNIONTV_COMPONENT" >/dev/null 2>&1 || true
    if is_uniontv_home; then
        echo "UnionTV is the HOME activity."
    else
        echo "UnionTV was set, but the firmware still resolves another HOME." >&2
        echo "If Google TV is present, re-run with --remove-google-tv." >&2
        echo "The Google TV system APK is retained and can be restored." >&2
        return 1
    fi
}

googletv() {
    require_device
    shell cmd package install-existing "$GOOGLE_PKG" || true
    shell cmd package set-home-activity --user 0 "$GOOGLE_COMPONENT"
    shell am start -n "$GOOGLE_COMPONENT" >/dev/null 2>&1 || true
    echo "Google TV restored and set as HOME."
}

usage() {
    cat >&2 <<'EOF'
Usage: bash tools/home.sh <command>

  status   Show UnionTV, current HOME, Google TV and top activity.
  check    Read-only post-OTA check; exit 0 only if UnionTV is HOME.
  uniontv  Set UnionTV as HOME. Add --remove-google-tv to remove the Google
           TV launcher for user 0 first (the system APK remains restorable).
  googletv Restore the Google TV launcher for user 0 and set it as HOME.
EOF
}

case "${1:-status}" in
    status)   status ;;
    check)    check ;;
    uniontv)  uniontv "${2:-}" ;;
    googletv) googletv ;;
    -h|--help|help) usage ;;
    *) usage; exit 2 ;;
esac
