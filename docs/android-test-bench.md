# Android Test Bench

This repo includes a local Fold-style Android emulator bench so UI and import work can be tested without a physical phone.

## Device

- AVD name: `mcc_fold_api34`
- Profile: `7.6" Fold-in with outer display`
- Image: `system-images;android-34;google_apis;arm64-v8a`

The profile is a close local stand-in for a Samsung/Pixel Fold inner display. It is intentionally Android 14 because that image is already installed on this machine and supports fast ARM emulation.

## Commands

Create the AVD if needed:

```bash
scripts/android/ensure-fold-avd.sh
```

Boot it and print the emulator serial:

```bash
scripts/android/start-fold-emulator.sh
```

Build, install, grant best-effort Health Connect permissions, and run the emulator-safe smoke suite:

```bash
scripts/android/run-fold-smoke.sh
```

Run a focused Health Connect write/read smoke test using the embedded July 1 meal fixture:

```bash
scripts/android/run-fold-health-smoke.sh
```

The smoke runner pushes `~/Downloads/meal-log-health-connect-import-2026-07-02-cleaned.csv` when it exists. Override that fixture with:

```bash
MCC_HISTORICAL_MEAL_CSV=/path/to/file.csv scripts/android/run-fold-smoke.sh
```

## Notes

- The full Health Connect historical import instrumentation test skips when its local CSV fixture is absent from the app external files directory.
- A real device is still useful for validating Google/Samsung Health Connect app UI behavior, but the Fold AVD is enough for repeatable Compose UI checks, parser tests, APK install checks, and Health Connect client write/read instrumentation.
