#!/usr/bin/env bash
set -euo pipefail

AVD_NAME="${MCC_AVD_NAME:-mcc_fold_api34}"
DEVICE_ID="${MCC_AVD_DEVICE:-7.6in Foldable}"
SYSTEM_IMAGE="${MCC_AVD_IMAGE:-system-images;android-34;google_apis;arm64-v8a}"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"

SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/avdmanager"

if [[ ! -x "$SDKMANAGER" || ! -x "$AVDMANAGER" ]]; then
  echo "Android command line tools were not found under $SDK_ROOT" >&2
  exit 1
fi

if "$AVDMANAGER" list avd | grep -q "Name: $AVD_NAME$"; then
  echo "$AVD_NAME already exists"
  exit 0
fi

set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null
license_status=${PIPESTATUS[1]}
set -o pipefail
if [[ $license_status -ne 0 ]]; then
  echo "Failed to accept Android SDK licenses" >&2
  exit "$license_status"
fi
"$SDKMANAGER" --sdk_root="$SDK_ROOT" "$SYSTEM_IMAGE" "platform-tools" "emulator"

echo "no" | "$AVDMANAGER" create avd \
  --name "$AVD_NAME" \
  --package "$SYSTEM_IMAGE" \
  --device "$DEVICE_ID"

CONFIG="$HOME/.android/avd/$AVD_NAME.avd/config.ini"
{
  echo "hw.keyboard=yes"
  echo "disk.dataPartition.size=4096M"
  echo "showDeviceFrame=yes"
} >> "$CONFIG"

echo "Created $AVD_NAME using $DEVICE_ID / $SYSTEM_IMAGE"
