#!/usr/bin/env bash
# Builds the app, starts an emulator if no phone or emulator is connected, installs the app and opens it.
# Another emulator than the first one: AVD=Pixel_8 ./scripts/run-android.sh
set -euo pipefail

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"

cd "$(dirname "$0")/../android"

# A device counts only when adb sees it as "device" (not "offline" or "unauthorized").
has_device() { "$ADB" devices | awk 'NR > 1 && $2 == "device"' | grep -q .; }

if has_device; then
    echo "Using the connected device."
    started_emulator=false
else
    AVD="${AVD:-$("$EMULATOR" -list-avds | head -n 1)}"
    if [ -z "$AVD" ]; then
        echo "No emulator found. Create one in Android Studio → Device Manager." >&2
        exit 1
    fi
    echo "Starting the emulator $AVD…"
    # Its own session (setsid), so the emulator stays open when this script or the terminal ends.
    # macOS has no setsid command, so perl does it.
    perl -MPOSIX -e 'POSIX::setsid(); exec @ARGV' -- "$EMULATOR" -avd "$AVD" > /dev/null 2>&1 &
    started_emulator=true
fi

# Builds while the emulator boots, so we don't wait for both one after the other.
./gradlew assembleDebug

if [ "$started_emulator" = true ]; then
    echo "Waiting for the emulator to boot…"
    "$ADB" wait-for-device
    until [ "$("$ADB" shell getprop sys.boot_completed 2> /dev/null | tr -d '\r')" = "1" ]; do
        sleep 2
    done
fi

./gradlew installDebug
"$ADB" shell am start -n com.bookworms.app/.MainActivity
