# Debug Automation

The debug automation bridge lets local tools drive the app without fragile tap coordinates.

## Android Bridge

Bridge source:

`app/src/debug/java/com/makstuff/minimalistcaloriecounter/automation/AutomationBootstrap.kt`

Startup happens from `MainActivity` through reflection and only when `BuildConfig.DEBUG` is true.

Default device port:

`8765`

Forward it to the host:

```bash
adb -s emulator-5554 forward tcp:18765 tcp:8765
```

## Smoke Test

Run:

```bash
scripts/android/run-automation-smoke.sh
```

The script installs the debug APK, launches the app, forwards the port, checks `/health`, navigates to Add Meal, previews a sample meal, and writes state to `/tmp/mcc-automation-state.json`.

## Design Rules

- Debug source set only.
- No release dependency on automation server code.
- Bind to device localhost only.
- Use app ViewModel and repository methods instead of UI node pokes.
- Keep destructive actions explicit and date-scoped.
