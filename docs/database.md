# Local Data Files

The app stores its local data as CSV files under the app external files directory.

## Files

- `database.csv`
  - Food database entries.
- `day.csv`
  - Current day food list.
- `archive.csv`
  - Historical daily totals.
- Options file
  - App preferences and Health Connect sync flags.

Bundled defaults live under `app/src/main/res/raw`.

## Food Database

Food database rows represent a food's nutrients per 100g plus quick-select metadata. Add Meal can create these rows from parsed food weights when local database writing is enabled.

## Day Data

The day file tracks foods added to the current daily working set. Add Meal can add parsed foods here when day writing is enabled.

## Archive Data

The archive feature stores daily totals. It remains available for troubleshooting and legacy data, but the primary workflow now uses Add Meal and Health Connect Nutrition records.
