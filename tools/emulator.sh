#!/usr/bin/env bash
# Drives the headless emulator for local development and screenshots.
#
# Toolchain is expected inside the repo (.jdk, .android-sdk, .android/avd); anything already on
# PATH is used instead. Everything the script creates stays under the repo.
#
#   tools/emulator.sh boot          start headless and wait for boot
#   tools/emulator.sh install       build and install the debug APK
#   tools/emulator.sh run           launch MainActivity
#   tools/emulator.sh shot NAME     screenshot into .screenshots/NAME.png
#   tools/emulator.sh tap X Y       tap at device pixels
#   tools/emulator.sh dark on|off   switch night mode
#   tools/emulator.sh stop          shut the emulator down
#   tools/emulator.sh status        is it running, and booted?
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AVD_NAME="${AVD_NAME:-even36g}"
PORT="${PORT:-5554}"
PKG="dev.barter.app.debug"
ACTIVITY="dev.barter.app.MainActivity"
SHOTS="$ROOT/.screenshots"

export JAVA_HOME="${JAVA_HOME:-$ROOT/.jdk}"
export ANDROID_HOME="${ANDROID_HOME:-$ROOT/.android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$ROOT/.android/avd}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

adb_() { adb -s "emulator-$PORT" "$@"; }

wait_for_boot() {
    adb_ wait-for-device
    for _ in $(seq 1 120); do
        if [ "$(adb_ shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
            return 0
        fi
        sleep 2
    done
    echo "emulator did not report boot_completed in time" >&2
    return 1
}

case "${1:-}" in
boot)
    adb_ emu kill 2>/dev/null || true
    # even36g is the stock google_apis image. The ATD image (even36) renders correctly but its
    # screencap and screenrecord both return black on this emulator, so it cannot be used for review.
    # A window is also required here: with -no-window the guest never composites a frame.
    window=()
    [ "${HEADLESS:-0}" = "1" ] && window=(-no-window)
    nohup emulator -avd "$AVD_NAME" -port "$PORT" \
        "${window[@]}" -no-audio -no-boot-anim -no-snapshot \
        -gpu swiftshader_indirect >"$ROOT/.emulator.log" 2>&1 &
    wait_for_boot
    # Keeps a SystemUI ANR dialog from covering the app in screenshots.
    adb_ shell settings put global hide_error_dialogs 1 >/dev/null
    echo "booted emulator-$PORT"
    ;;
install)
    "$ROOT/gradlew" --project-dir "$ROOT" :app:installDebug
    ;;
run)
    adb_ shell am start -n "$PKG/$ACTIVITY" >/dev/null
    sleep 2
    echo "started $ACTIVITY"
    ;;
shot)
    mkdir -p "$SHOTS"
    adb_ exec-out screencap -p >"$SHOTS/${2:-shot}.png"
    echo "$SHOTS/${2:-shot}.png"
    ;;
tap)
    adb_ shell input tap "$2" "$3"
    sleep "${4:-1}"
    ;;
dark)
    adb_ shell cmd uimode night "$2" >/dev/null
    echo "night mode: $2"
    ;;
stop)
    adb_ emu kill
    ;;
status)
    if adb_ shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q 1; then
        echo "emulator-$PORT booted"
    else
        echo "emulator-$PORT not booted"
        exit 1
    fi
    ;;
*)
    sed -n '2,14p' "${BASH_SOURCE[0]}"
    exit 1
    ;;
esac
