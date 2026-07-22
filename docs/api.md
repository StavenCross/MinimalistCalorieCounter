# App API Surface

Minimalist Calorie Counter is a local-first Android app. The production app does not expose a network API.

The debug build exposes a localhost-only automation API for development. It is compiled from `app/src/debug` and is not present in release builds.

## Meal Import Links

The app accepts user-triggered meal imports through Android intents. This is a local handoff, not a remote write API. Incoming meals open the Meals page Add Meal drawer with confirmation enabled before Health Connect or local backup writes run.

Supported entry points:

- `foodlog://import?payload=<base64url-json>`
- `https://nutrition.dioem.cloud/import?payload=<base64url-json>`
- `ACTION_SEND` with `text/plain` containing either supported link
- `ACTION_SEND` with `application/json` or `text/plain` containing raw import JSON

The HTTPS route is an Android App Link. The backend at `nutrition.dioem.cloud` must serve `/.well-known/assetlinks.json` for the exact package and signing certificate. The current hosted association supports the debug package used for Fold deployment; a release build requires its own association before production rollout. No browser fallback page is currently guaranteed, so callers should retain the `foodlog://` form when direct App Link verification is unavailable.

Import nutrition is stored as reported. The app does not reject restaurant or packaged-food values merely because rounded `fiber_g`, `sugar_g`, or `sat_fat_g` figures do not form exact mathematical subsets of another macro.

The optional `totals` object is advisory. The app derives the meal preview and saved totals by summing the reported items, so a rounded or stale total cannot block the import or override what will actually be written.

`amount` is a display label and may use a weight (`211g`, `6 oz`) or a serving description (`1 venti`, `1 croissant`, `2 slices`). Weight-based meals can also populate the legacy per-100g local database. If any item in a default import is serving-based or otherwise cannot be represented faithfully in that database, the app skips legacy database/day conversion for the whole meal and writes every reported serving to Health Connect and the local meal backup instead.

To keep external intents bounded, encoded payloads may contain at most 64,000 characters, decoded JSON at most 48,000 UTF-8 bytes, 100 meal items, and 32 nested JSON levels.

Import JSON shape:

```json
{
  "source": "chatgpt",
  "action": "log_meal",
  "date": "2026-07-08",
  "time": "12:00",
  "meal": "lunch",
  "items": [
    {
      "name": "Chicken thigh, cooked",
      "amount": "250g",
      "calories": 520,
      "protein_g": 45,
      "carbs_g": 0,
      "fat_g": 32,
      "fiber_g": 0,
      "sugar_g": 0,
      "sat_fat_g": 9
    }
  ],
  "totals": {
    "calories": 520,
    "protein_g": 45,
    "carbs_g": 0,
    "fat_g": 32,
    "fiber_g": 0,
    "sugar_g": 0,
    "sat_fat_g": 9
  }
}
```

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
  - Returns UI state, Health Connect status, Add Meal parse state, Add Meal outbox state, selected Meals date, and loaded Health Connect meals.
- `POST /navigate`
  - Body: `{ "screen": "quick_add" | "quick_import" | "add_meal" | "meals" | "settings" | "database" | "day" }`
  - `quick_add`, `quick_import`, and `add_meal` are compatibility aliases for the Meals page, where the Add Meal drawer now lives.
- `POST /settings/open`
  - Body: `{ "sheet": "health_data" | "import_tools" | "theme" | "maintenance" | null }`
- `POST /quick-import/preview`
  - Parses Add Meal text without writing.
- `POST /quick-import/commit`
  - Commits through the same ViewModel path as the Add Meal button.
  - Health Connect write attempts are reflected in the Add Meal outbox section returned by `GET /state`.
- `POST /quick-import/retry`
  - Body: `{ "id": "<outbox-id>" }`
  - Retries one Add Meal Health Connect write from stored outbox payloads.
- `POST /quick-import/outbox/clear`
  - Body: `{ "id": "<optional-outbox-id>", "attentionOnly": true }`
  - Clears one outbox row or all pending/failed/retrying rows for debug test setup.
- `POST /meals/select-date`
  - Body: `{ "date": "YYYY-MM-DD" }`
- `POST /health-connect/read-day`
  - Starts a Health Connect read for one date.
- `POST /health-connect/preview-delete-range`
  - Body: `{ "startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD", "mode": "HistoricalImports" | "AddMeal" | "AllAppNutrition" }`
  - Starts cleanup preview for app-owned Nutrition records in an inclusive date range.
- `POST /health-connect/delete-range`
  - Starts deletion of previously previewed app-owned Nutrition records in an inclusive date range.
- `POST /health-connect/export-options`
  - Body: `{ "mode": "NutritionOnly" | "NutritionAndGoals" | "Full", "redacted": true }`
  - Updates export mode/redaction options for test automation.
- `POST /health-connect/export-range`
  - Starts CSV export of readable Health Connect records in an inclusive date range. The same body may include `mode` and `redacted`.
- `GET /goals/state`
  - Returns the current Goals profile, targets, active targets, and recommendation state.
- `POST /goals/settings`
  - Opens or closes the Goals settings drawer.
- `POST /goals/set-profile`
  - Updates profile fields used for calorie and macro recommendations.
- `POST /goals/set-measurement`
  - Updates one profile measurement.
- `POST /goals/toggle-measurement-lock`
  - Toggles whether one profile measurement is manual or Health Connect controlled.
- `POST /goals/set-macro`
  - Updates one macro target.
- `POST /goals/toggle-macro-lock`
  - Toggles whether one macro target is manual or generated by recommendation rules.
- `POST /goals/refresh-health-connect`
  - Refreshes Goals profile data from Health Connect where permissions and records are available.
- `POST /goals/recalculate`
  - Regenerates the calorie and macro recommendation from the current profile.
- `POST /goals/apply-recommendation`
  - Applies the current recommendation as active targets.
- `POST /reset-debug-state`
  - Clears transient debug-visible UI state.

## Safety

This API is debug-only and binds inside the device to `127.0.0.1`. It is intended to be accessed through adb port forwarding.
