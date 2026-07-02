#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERIAL="${ANDROID_SERIAL:-emulator-5554}"
PACKAGE_NAME="com.makstuff.minimalistcaloriecounter.debug"
HOST_PORT="${MCC_AUTOMATION_HOST_PORT:-18765}"
DEVICE_PORT="${MCC_AUTOMATION_DEVICE_PORT:-8765}"

cd "$ROOT_DIR"

./gradlew :app:installDebug --console=plain --quiet

ACTIVITY="$(adb -s "$SERIAL" shell cmd package resolve-activity --brief "$PACKAGE_NAME" | tail -n 1 | tr -d '\r')"
adb -s "$SERIAL" shell am start -n "$ACTIVITY" >/dev/null

adb -s "$SERIAL" forward "tcp:${HOST_PORT}" "tcp:${DEVICE_PORT}"

for _ in {1..20}; do
  if curl --silent --fail "http://127.0.0.1:${HOST_PORT}/health" >/tmp/mcc-automation-health.json; then
    break
  fi
  sleep 1
done

curl --silent --fail "http://127.0.0.1:${HOST_PORT}/health"
printf '\n'

curl --silent --fail \
  -H 'content-type: application/json' \
  -d '{"screen":"quick_add"}' \
  "http://127.0.0.1:${HOST_PORT}/navigate"
printf '\n'

curl --silent --fail \
  -H 'content-type: application/json' \
  -d '{"text":"100g test oats; Calories 389, Fat 6.9g, Sat Fat 1.2g, Trans Fat 0g, Cholesterol 0mg, Sodium 2mg, Carbs 66.3g, Fiber 10.6g, Sugar 0.9g, Added Sugar 0g, Protein 16.9g. Meal totals; Calories 389, Fat 6.9g, Sat Fat 1.2g, Trans Fat 0g, Cholesterol 0mg, Sodium 2mg, Carbs 66.3g, Fiber 10.6g, Sugar 0.9g, Added Sugar 0g, Protein 16.9g.","dateTime":"2026-07-02T12:00:00","snackOverride":false,"addDatabase":false,"addDay":false,"writeHealthConnect":false}' \
  "http://127.0.0.1:${HOST_PORT}/quick-import/preview"
printf '\n'

curl --silent --fail "http://127.0.0.1:${HOST_PORT}/state" >/tmp/mcc-automation-state.json
echo "Automation smoke passed. State saved to /tmp/mcc-automation-state.json"
