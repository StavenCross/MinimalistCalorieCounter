#!/usr/bin/env bash
set -euo pipefail

AVD_NAME="${MCC_AVD_NAME:-mcc_fold_api34}"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
ADB="$SDK_ROOT/platform-tools/adb"
EMULATOR="$SDK_ROOT/emulator/emulator"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT_DIR/.android"
LOG_FILE="$LOG_DIR/$AVD_NAME.log"

mkdir -p "$LOG_DIR"
"$ROOT_DIR/scripts/android/ensure-fold-avd.sh" >/dev/null

running_serial_for_avd() {
  "$ADB" devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ {print $1}' | while read -r serial; do
    if "$ADB" -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | grep -qx "$AVD_NAME"; then
      echo "$serial"
      return 0
    fi
  done
}

serial="$(running_serial_for_avd || true)"
if [[ -z "$serial" ]]; then
  nohup "$EMULATOR" -avd "$AVD_NAME" -no-snapshot-save -no-boot-anim "$@" >"$LOG_FILE" 2>&1 &
fi

deadline=$((SECONDS + 180))
while [[ $SECONDS -lt $deadline ]]; do
  serial="$(running_serial_for_avd || true)"
  if [[ -n "$serial" ]]; then
    booted="$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "$booted" == "1" ]]; then
      "$ADB" -s "$serial" shell input keyevent 82 >/dev/null 2>&1 || true
      "$ADB" -s "$serial" shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
      "$ADB" -s "$serial" shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
      "$ADB" -s "$serial" shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
      echo "$serial"
      exit 0
    fi
  fi
  sleep 2
done

echo "Timed out waiting for $AVD_NAME to boot. Emulator log: $LOG_FILE" >&2
exit 1
