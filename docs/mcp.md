# Local MCP Server

The local MCP server wraps adb and the debug automation bridge so Codex can control the app during development.

Source:

`tools/mcc-mcp`

## Setup

```bash
cd tools/mcc-mcp
npm install
npm run build
```

## Run

```bash
cd tools/mcc-mcp
npm start
```

## Main Tools

- `mcc_list_devices`
  - Lists adb devices.
- `mcc_connect`
  - Runs adb port forwarding and checks `/health`.
- `mcc_state`
  - Reads app state.
- `mcc_navigate`
  - Navigates to Add Meal, Meals, Settings, Database, or Day.
- `mcc_quick_import_preview`
  - Parses a meal blurb without writing.
- `mcc_quick_import_commit`
  - Commits an Add Meal meal through the real app path.
- `mcc_quick_import_retry`
  - Retries a failed or pending Add Meal Health Connect write by outbox id.
- `mcc_select_meals_date`
  - Selects a Meals page date.
- `mcc_health_read_day`
  - Starts a Health Connect read for a date.
- `mcc_health_delete_range`
  - Deletes app-owned Health Connect Nutrition rows in a date range.
- `mcc_health_export_range`
  - Exports readable Health Connect records in a date range to device Downloads.
- `mcc_open_settings_panel`
  - Opens or closes a settings panel.
- `mcc_goals_state`
  - Reads Goals profile, target, and recommendation state.
- `mcc_goals_settings`
  - Opens or closes the Goals settings drawer.
- `mcc_goals_set_profile`
  - Updates the full Goals profile used for recommendations.
- `mcc_goals_set_measurement`
  - Updates one Goals measurement field.
- `mcc_goals_toggle_measurement_lock`
  - Toggles one Goals measurement lock.
- `mcc_goals_set_macro`
  - Updates one macro target.
- `mcc_goals_toggle_macro_lock`
  - Toggles one macro target lock.
- `mcc_goals_refresh_health_connect`
  - Refreshes Goals profile values from Health Connect.
- `mcc_goals_recalculate`
  - Recalculates the current recommendation.
- `mcc_goals_apply_recommendation`
  - Applies the current recommendation.
- `mcc_screenshot`
  - Captures a device screenshot.
- `mcc_logcat`
  - Reads app logcat.
- `mcc_run_smoke`
  - Runs local Android smoke scripts.

## Physical Phone Caution

Prefer the Fold emulator for destructive tests. If using a physical phone, pass the explicit `deviceSerial` and keep Health Connect delete ranges narrow.
