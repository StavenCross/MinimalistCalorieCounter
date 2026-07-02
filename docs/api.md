# App API Surface

Minimalist Calorie Counter is a local-first Android app. The production app does not expose a network API.

The debug build exposes a localhost-only automation API for development. It is compiled from `app/src/debug` and is not present in release builds.

## Debug Automation API

Default device port: `8765`

Use adb forwarding from the host:

```bash
adb -s emulator-5554 forward tcp:18765 tcp:8765
```

Then call the bridge from the Mac:

```bash
curl http://127.0.0.1:18765/health
```

## Endpoints

- `GET /health`
  - Confirms the debug bridge is running.
- `GET /state`
  - Returns UI state, Health Connect status, Quick Add parse state, selected Meals date, and loaded Health Connect meals.
- `POST /navigate`
  - Body: `{ "screen": "quick_add" | "meals" | "settings" | "database" | "day" }`
- `POST /settings/open`
  - Body: `{ "sheet": "health_data" | "import_tools" | "maintenance" | "support" | null }`
- `POST /quick-import/preview`
  - Parses Quick Add text without writing.
- `POST /quick-import/commit`
  - Commits through the same ViewModel path as the Quick Add button.
- `POST /meals/select-date`
  - Body: `{ "date": "YYYY-MM-DD" }`
- `POST /health-connect/read-day`
  - Starts a Health Connect read for one date.
- `POST /health-connect/delete-range`
  - Starts deletion of app-owned Nutrition records in an inclusive date range.
- `POST /reset-debug-state`
  - Clears transient debug-visible UI state.

## Safety

This API is debug-only and binds inside the device to `127.0.0.1`. It is intended to be accessed through adb port forwarding.
