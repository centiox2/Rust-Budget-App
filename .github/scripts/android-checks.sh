#!/usr/bin/env bash
set -euo pipefail
mkdir -p artifacts/android-screenshots
collect_evidence() {
  adb logcat -d > artifacts/android-logcat.txt || true
  adb pull /sdcard/Download/verdant-test-screenshots/. artifacts/android-screenshots/ || true
}
trap collect_evidence EXIT

integration_status=0
./gradlew :androidApp:connectedDebugAndroidTest --max-workers=2 --stacktrace || integration_status=$?

# Clear the debug install, then exercise the exact signed production binary.
adb uninstall com.centiox.verdant || true
adb install artifacts/release/Verdant-Budget-Kotlin.apk
release_status=0
./gradlew :releaseDriver:connectedDebugAndroidTest --max-workers=2 --stacktrace || release_status=$?
if (( integration_status != 0 || release_status != 0 )); then exit 1; fi
