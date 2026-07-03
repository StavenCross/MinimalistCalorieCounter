#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
ADB="$SDK_ROOT/platform-tools/adb"
PACKAGE="com.makstuff.minimalistcaloriecounter.debug"
TEST_PACKAGE="$PACKAGE.test"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
SERIAL="$("$ROOT_DIR/scripts/android/start-fold-emulator.sh")"

cd "$ROOT_DIR"
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest

"$ADB" -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.READ_NUTRITION >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.WRITE_NUTRITION >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.READ_WEIGHT >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.WRITE_WEIGHT >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.READ_HEIGHT >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.READ_BODY_FAT >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.READ_LEAN_BODY_MASS >/dev/null 2>&1 || true

CSV="${MCC_HISTORICAL_MEAL_CSV:-$HOME/Downloads/meal-log-health-connect-import-2026-07-02-cleaned.csv}"
if [[ -f "$CSV" ]]; then
  if ! "$ADB" -s "$SERIAL" push "$CSV" "/sdcard/Android/data/$PACKAGE/files/$(basename "$CSV")" >/dev/null 2>&1; then
    echo "Optional historical CSV fixture could not be pushed into app external files; continuing smoke without it." >&2
  fi
fi

INSTRUMENT_OUTPUT="$(mktemp)"
"$ADB" -s "$SERIAL" shell am instrument -w \
  -e class com.makstuff.minimalistcaloriecounter.ui.screens.ScreenQuickImportTest \
  "$TEST_PACKAGE/$RUNNER" | tee "$INSTRUMENT_OUTPUT"
if grep -Eq "FAILURES!!!|There (was|were) [0-9]+ failure" "$INSTRUMENT_OUTPUT"; then
  exit 1
fi

echo "Fold emulator smoke passed on $SERIAL"
