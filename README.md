# Minimalist Calorie Counter

Minimalist Calorie Counter is a private, local-first Android nutrition app centered on a ChatGPT-assisted meal logging workflow. The current build focuses on two primary daily tabs plus settings:

- Meals: review Health Connect meals by day, add meals from ChatGPT blurbs, inspect foods, repeat meals, delete app-owned meal records, and export a day check-in.
- Goals: maintain calorie and macro targets from Health Connect metrics plus locked manual overrides.

Settings contains Health Connect permissions, CSV export, Health Connect cleanup, theme, debug status, and legacy maintenance tools.

## Daily Workflow

1. Describe a meal to ChatGPT and ask for the app's nutrition blurb format.
2. Open Meals, choose the target day, and tap the add action on the day card.
3. Paste the blurb into the Add Meal drawer.
4. Confirm the inferred meal type and timestamp.
5. Save the meal, then review the updated day in Meals or use Goals to adjust targets.

The Add Meal drawer writes one Health Connect Nutrition record per parsed food. Each food includes calories, carbohydrates, protein, fat, fiber, sugar, saturated fat, energy from fat, meal type, and timestamp when those values are available in the blurb.

Health Connect does not expose a serving-weight field for Nutrition records, so serving amount stays in the food name.

## Health Connect

Health Connect is the main integration target. The app can:

- write Add Meal drawer and historical import Nutrition records;
- read app-owned Nutrition records for Meals review and duplicate prevention;
- read authorized body metrics for Goals defaults;
- export authorized Health Connect data to CSV in Downloads;
- delete app-owned Nutrition records after an explicit preview.

Cleanup is intentionally separated from import. Historical spreadsheet import is insert-only unless you explicitly use the Settings cleanup tool.

CSV export supports:

- Nutrition only;
- Nutrition and goals;
- Full Health Connect export.

Redacted export is enabled by default for check-ins. Raw/full export is available when explicitly selected.

## Reliability

Add Meal keeps a local write outbox with deterministic Health Connect client record ids. If local backup succeeds but Health Connect fails, the meal appears as pending or failed and can be retried without duplicating records.

The app also stores local backups, goals, preferences, import/export job history, and outbox rows in Room while mirroring key data to legacy CSV files during the migration window. Android backup rules include the Room database and legacy CSV files so debug deploys and platform restore have the best chance of preserving settings and goals.

## Developer Setup

Build and unit test:

```bash
./gradlew testDebugUnitTest --console=plain
./gradlew lintDebug --console=plain
./gradlew assembleDebug assembleDebugAndroidTest --console=plain
```

Run MCP tests:

```bash
cd tools/mcc-mcp
npm test
```

Run connected tests or smoke scripts on an emulator:

```bash
./gradlew connectedDebugAndroidTest --console=plain
scripts/android/run-fold-smoke.sh
scripts/android/run-fold-health-smoke.sh
scripts/android/run-automation-smoke.sh
```

## Documentation

Atomic project docs live under `docs/`:

- `docs/healthconnectintegration.md`
- `docs/database.md`
- `docs/api.md`
- `docs/mcp.md`
- `docs/testing.md`
- `docs/android-test-bench.md`
- `docs/architecture-audit.md`

The broader implementation roadmap is tracked in `app_refactor_and_expansion.md`.
