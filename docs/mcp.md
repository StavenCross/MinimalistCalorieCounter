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
  - Navigates to Quick Add, Meals, Settings, Database, or Day.
- `mcc_quick_import_preview`
  - Parses a meal blurb without writing.
- `mcc_quick_import_commit`
  - Commits a Quick Add meal through the real app path.
- `mcc_select_meals_date`
  - Selects a Meals page date.
- `mcc_health_read_day`
  - Starts a Health Connect read for a date.
- `mcc_health_delete_range`
  - Deletes app-owned Health Connect Nutrition rows in a date range.
- `mcc_open_settings_panel`
  - Opens or closes a settings panel.
- `mcc_screenshot`
  - Captures a device screenshot.
- `mcc_logcat`
  - Reads app logcat.
- `mcc_run_smoke`
  - Runs local Android smoke scripts.

## Physical Phone Caution

Prefer the Fold emulator for destructive tests. If using a physical phone, pass the explicit `deviceSerial` and keep Health Connect delete ranges narrow.
