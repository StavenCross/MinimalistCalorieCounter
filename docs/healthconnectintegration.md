# Health Connect Integration

The app writes Nutrition records to Health Connect for Add Meal and historical imports.

## Permissions

Core meal logging requests:

- Read Nutrition
- Write Nutrition
- Read Weight
- Write Weight
- Read Height
- Write Height
- Read Body Fat
- Read Body Water Mass
- Read Bone Mass
- Read Lean Body Mass

The default Health Connect grant flow requests only core meal, archive, and Goals permissions. CSV and check-in exports request their additional read/history permissions when the user starts that capability. Optional broad export reads are reported separately and do not create a false missing-permissions state.

Read Nutrition is needed so the app can display Meals and avoid duplicate historical-import or Add Meal writes. Weight, height, body-fat, and lean-mass reads support Goals. Weight and height writes let manual Goals values be mirrored back to Health Connect when permission is granted. Health Connect permissions remain user controlled and cannot be silently granted by automation.

## Add Meal Writes

Add Meal writes one Health Connect `NutritionRecord` per parsed food.

Add Meal also records each Health Connect write intent in `quick_import_outbox.csv`. The outbox tracks deterministic client record ids, intended meal timestamp, meal type, stored Health Connect payloads, attempt count, last error, and whether the write is pending, retrying, synced, or failed. This keeps local backup state visible when local database/day writes succeed but Health Connect is unavailable or missing permissions.

Before writing, Add Meal reads app-owned Nutrition records for the affected date and skips payloads that already exist by deterministic client record id or matching nutrition fingerprint. Retry uses the stored payloads and the same deterministic client record ids, so an already-written retry reconciles to synced without duplicating Health Connect records.

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

Choosing a meal type manually changes only the label. It never rewrites the selected timestamp, so Breakfast, Lunch, Dinner, or Snack can be logged at any time of day. Default meal times are still used when preparing a repeated meal for another date.

## Historical Import

Historical import writes one Nutrition record per food row. Duplicate detection checks both client record ids and content fingerprints.

## Cleanup

The Settings page can remove app-owned Health Connect Nutrition records by inclusive date range. Spreadsheet import does not auto-clear Health Connect data.

The Meals page can delete a selected meal by forwarding that meal group's visible app-owned Nutrition record ids to Health Connect. This is narrower than Settings cleanup and does not delete by date range.

Repeating a meal from the Meals page does not write directly to Health Connect. It opens the Meals add drawer with copied nutrition values, then the normal Add Meal commit flow performs local backup, outbox tracking, duplicate prevention, and Health Connect write.

The Meals page can also export the selected day's check-in summary as a text file in Downloads. This uses the same generated day summary as the copy action, not a raw Health Connect CSV.

Goals history deletion removes the selected saved goal from local goal history and then attempts a narrow Health Connect weight cleanup. Weight cleanup only targets app-written weight records whose client record id uses the goal-weight prefix and whose value matches the deleted goal entry. The app does not delete arbitrary third-party Health Connect weight records because historical goal entries do not store external Health Connect record ids.

Cleanup modes are:

- Historical imports only
- Add Meal records only
- All app-owned nutrition

Cleanup must be previewed before deletion. Preview scans the selected date range and reports total matching records plus split counts for historical import records, Add Meal records, and legacy Daily Total records. Delete requires the selected start date, end date, and cleanup mode to match the preview, then uses the same classifier as preview so the confirmation count and actual deletion target stay aligned.

Debug automation and the MCP server expose cleanup preview separately from deletion. This keeps automated destructive tests aligned with the Settings UI rule that a preview must happen before delete.

## Export

Settings can export Health Connect CSV files to Downloads. Export modes are:

- Nutrition only
- Nutrition and goals
- Full Health Connect export

Redacted export is enabled by default for ChatGPT check-ins. It omits Health Connect record ids, client record ids, client record version, data-origin package, recording method, last-modified time, and the raw record text while keeping dates, times, calories, macros, meal type, and available body metrics. Raw/full export remains available by turning redaction off and selecting the full mode.

Goals includes a Check-ins card that creates a ChatGPT-ready XLSX workbook in Downloads. Weekly check-ins export the previous Monday-through-Sunday calendar week, monthly check-ins export the previous calendar month, and custom check-ins export once both dates are selected. The workbook currently includes Summary, Meals Foods, Daily Nutrition, Body Metrics, Sleep, Activity Burn, Heart Oxygen, and Exercise sheets.

Debug automation and the MCP server can set export mode and redaction before triggering an export so emulator smoke tests cover all export choices without manual drawer navigation.
