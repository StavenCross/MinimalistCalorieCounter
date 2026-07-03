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

The first Phase 6 slice added the schema, DAOs, domain mappers, CSV seed planner, schema export, backup rules, and unit coverage.

The second Phase 6 slice wires runtime storage for:

- Goals
- Add Meal Health Connect outbox
- app preferences
- Add Meal local meal backups
- Health Connect export/delete job history

On startup, each migrated surface reads Room first. If Room is empty or unavailable during the migration window, the app falls back to the existing CSV file and seeds Room from that CSV data. Writes are mirrored to CSV during the transition so rollback and manual troubleshooting remain possible.

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

Runtime CSV fallback currently applies to Goals, Add Meal outbox data, and app preferences. Add Meal now also writes local meal backup rows to Room for committed meal foods. Health Connect export and delete attempts write import/export job history rows to Room. Food database, day data, and archive data still need full Room runtime wiring.

## Food Database

Food database rows represent a food's nutrients per 100g plus quick-select metadata. Add Meal can create these rows from parsed food weights when local database writing is enabled.

## Day Data

The day file tracks foods added to the current daily working set. Add Meal can add parsed foods here when day writing is enabled.

## Archive Data

The archive feature stores daily totals. It remains available for troubleshooting and legacy data, but the primary workflow now uses Add Meal and Health Connect Nutrition records.
