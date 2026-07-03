# Testing

## JVM Tests

Run:

```bash
./gradlew testDebugUnitTest --console=plain
```

Coverage includes Add Meal parsing, Add Meal planning, Add Meal outbox state/CSV/retry payload behavior, Add Meal repeat preparation, Add Meal local backup mapping, historical import parsing, Health Connect nutrition mapping, duplicate reconciliation helpers, Health Connect export mode/redaction behavior, Health Connect export/delete job mapping, day check-in export naming, cleanup mode classification, Goals recommendation history metadata, Goals Sunday recalculation scheduling, Goals trend/adherence summaries, Add Meal check-in summary formatting, Meals review summary formatting, Meals collapse/expand visibility rules, and Room foundation seed/mapping behavior.

Room-focused tests can be run with:

```bash
./gradlew testDebugUnitTest --tests '*persistence.room*' --console=plain
```

## Android Instrumentation Tests

Run on an emulator:

```bash
./gradlew connectedDebugAndroidTest --console=plain
```

Room runtime round-trip coverage is in `AppRoomStoreTest`. If connected Compose tests are unstable, `assembleDebugAndroidTest` still verifies this test compiles into the instrumentation APK.

## Fold Emulator Smoke

Existing scripts:

```bash
scripts/android/run-fold-smoke.sh
scripts/android/run-fold-health-smoke.sh
```

Automation bridge smoke:

```bash
scripts/android/run-automation-smoke.sh
```

## MCP Tests

Run:

```bash
cd tools/mcc-mcp
npm test
```

These tests validate adb device parsing, input defaults, and bridge tool calls with injected runners.
