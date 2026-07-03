# Testing

## JVM Tests

Run:

```bash
./gradlew testDebugUnitTest --console=plain
```

Coverage includes Add Meal parsing, Add Meal planning, Add Meal outbox state/CSV/retry payload behavior, historical import parsing, Health Connect nutrition mapping, duplicate reconciliation helpers, Health Connect export mode/redaction behavior, cleanup mode classification, Add Meal check-in summary formatting, and Meals review summary formatting.

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
