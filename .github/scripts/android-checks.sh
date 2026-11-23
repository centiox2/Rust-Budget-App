#!/usr/bin/env bash
set -euo pipefail
mkdir -p artifacts/android-screenshots
collect_evidence() {
  adb logcat -d > artifacts/android-logcat.txt || true
  adb pull /sdcard/Download/verdant-test-screenshots/. artifacts/android-screenshots/ || true
}
trap collect_evidence EXIT

# The Google emulator launcher can ANR while its first-run setup competes with Gradle.
# Tests launch explicit activities and do not need a home launcher or its overlays.
for launcher in com.google.android.apps.nexuslauncher com.android.launcher3; do
  if adb shell pm path "$launcher" | grep -q '^package:'; then
    adb shell am force-stop "$launcher"
    adb shell pm disable-user --user 0 "$launcher"
  fi
done
adb shell wm dismiss-keyguard

integration_status=0
./gradlew :androidApp:connectedDebugAndroidTest --max-workers=2 --stacktrace || integration_status=$?

# Clear the debug install, then exercise the exact signed production binary.
adb uninstall com.centiox.verdant || true
adb install artifacts/release/Verdant-Budget-Kotlin.apk
release_status=0
./gradlew :releaseDriver:connectedDebugAndroidTest --max-workers=2 --stacktrace || release_status=$?
if (( integration_status != 0 || release_status != 0 )); then exit 1; fi
