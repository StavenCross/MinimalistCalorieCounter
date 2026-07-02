# Health Connect Integration

The app writes Nutrition records to Health Connect for Quick Add and historical imports.

## Permissions

The app requests:

- Read Nutrition
- Write Nutrition
- Write Weight

Read Nutrition is needed so the app can display Meals and avoid historical-import duplicates. Health Connect permissions remain user controlled and cannot be silently granted by automation.

## Quick Add Writes

Quick Add writes one Health Connect `NutritionRecord` per parsed food.

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
