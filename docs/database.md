# Local Persistence

The app is moving from CSV-backed app state to Room-backed structured local storage. CSV remains supported for import, export, bundled defaults, and troubleshooting.

## Room Foundation

The Room database is `mcc.db`. Schema exports live under:

```text
app/schemas/com.makstuff.minimalistcaloriecounter.persistence.room.AppDatabase/
```

Version 1 includes tables for:

- app preferences
- goal profile, targets, recommendation, and history
- Add Meal Health Connect outbox items and payloads
- local meal backup records
- import/export job history

The first Phase 6 slice adds the schema, DAOs, domain mappers, CSV seed planner, schema export, backup rules, and unit coverage. Runtime reads/writes still use the existing CSV store until migration wiring is completed.

## Backup

Android backup and device-transfer rules include both the legacy CSV files and the Room database files:

- `mcc.db`
- `mcc.db-shm`
- `mcc.db-wal`

This is best-effort Android backup behavior; Health Connect permissions are still controlled by Android/Health Connect and may require user reauthorization after reinstall or restore.

## Files

The legacy CSV files are stored under the app external files directory:

- `database.csv`
  - Food database entries.
- `day.csv`
  - Current day food list.
- `archive.csv`
  - Historical daily totals.
- Options file
  - App preferences and Health Connect sync flags.

Bundled defaults live under `app/src/main/res/raw`.

## CSV Migration Seeds

`CsvRoomSeedPlanner` converts existing Goals and Add Meal outbox CSV rows into Room seed objects before any database write occurs. Corrupt outbox CSV input is rejected before Room insertion so migration wiring can fail without partially mutating the database.

## Food Database

Food database rows represent a food's nutrients per 100g plus quick-select metadata. Add Meal can create these rows from parsed food weights when local database writing is enabled.

## Day Data

The day file tracks foods added to the current daily working set. Add Meal can add parsed foods here when day writing is enabled.

## Archive Data

The archive feature stores daily totals. It remains available for troubleshooting and legacy data, but the primary workflow now uses Add Meal and Health Connect Nutrition records.
