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
./gradlew assembleDebug assembleDebugAndroidTest

"$ADB" -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
"$ADB" -s "$SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null

"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.READ_NUTRITION >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell pm grant "$PACKAGE" android.permission.health.WRITE_NUTRITION >/dev/null 2>&1 || true

INSTRUMENT_OUTPUT="$(mktemp)"
"$ADB" -s "$SERIAL" shell am instrument -w \
  -e class com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectImportTest#writesJulyFirstHistoricalMealsToHealthConnect \
  "$TEST_PACKAGE/$RUNNER" | tee "$INSTRUMENT_OUTPUT"
if grep -Eq "FAILURES!!!|There (was|were) [0-9]+ failure" "$INSTRUMENT_OUTPUT"; then
  exit 1
fi

echo "Fold emulator Health Connect smoke passed on $SERIAL"
