# Testing

## JVM Tests

Run:

```bash
./gradlew testDebugUnitTest --console=plain
```

Coverage includes Add Meal parsing, Add Meal planning, Add Meal outbox state/CSV/retry payload behavior, Add Meal repeat preparation, historical import parsing, Health Connect nutrition mapping, duplicate reconciliation helpers, Health Connect export mode/redaction behavior, day check-in export naming, cleanup mode classification, Goals recommendation history metadata, Goals Sunday recalculation scheduling, Goals trend/adherence summaries, Add Meal check-in summary formatting, Meals review summary formatting, and Meals collapse/expand visibility rules.

## Android Instrumentation Tests

Run on an emulator:

```bash
./gradlew connectedDebugAndroidTest --console=plain
```

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
