# Health Connect Integration

The app writes Nutrition records to Health Connect for Add Meal and historical imports.

## Permissions

Core meal logging requests:

- Read Nutrition
- Write Nutrition
- Read Weight
- Write Weight
- Read Height
- Read Body Fat
- Read Lean Body Mass

The export workflow also requests broad Health Connect read permissions for supported record types, plus historical-data access, so it can produce a review CSV for external analysis.

Read Nutrition is needed so the app can display Meals and avoid historical-import duplicates. Weight, height, body-fat, and lean-mass reads support Goals. Health Connect permissions remain user controlled and cannot be silently granted by automation.

## Add Meal Writes

Add Meal writes one Health Connect `NutritionRecord` per parsed food.

Add Meal also records each Health Connect write intent in `quick_import_outbox.csv`. The outbox tracks deterministic client record ids, intended meal timestamp, meal type, attempt count, last error, and whether the write is pending, retrying, synced, or failed. This keeps local backup state visible when local database/day writes succeed but Health Connect is unavailable or missing permissions.

Each food record includes:

- Name
- Start/end time
- Meal type
- Calories
- Energy from fat
- Total carbohydrate
- Sugar
- Protein
- Total fat
- Saturated fat
- Dietary fiber

Health Connect does not provide a serving weight field on Nutrition records, so the serving amount is included in the food name.

## Meal Type

The app infers meal type from the selected timestamp unless snack override is enabled:

- 1:00 AM through 10:59 AM: breakfast
- 11:00 AM through 2:59 PM: lunch
- 3:00 PM through 10:59 PM: dinner
- Otherwise: snack

## Historical Import

Historical import writes one Nutrition record per food row. Duplicate detection checks both client record ids and content fingerprints.

## Cleanup

The Settings page can remove app-owned Health Connect Nutrition records by inclusive date range. Spreadsheet import does not auto-clear Health Connect data.
