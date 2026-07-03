# Architecture Audit

This audit tracks large files and the safest cleanup sequence for the current Android app, debug automation bridge, and local MCP server.

## Current Oversized Files After Cleanup

Generated output, Gradle build folders, `node_modules`, and IDE metadata are excluded.

| File | Approx. lines | Type | Cleanup priority |
| --- | ---: | --- | --- |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/App.kt` | 2021 | App shell / route wiring / UI | High |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/screens/ScreenQuickImport.kt` | 1566 | Compose screen and components | Medium, UI-file exception applies |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/screens/ScreenHealthConnectNutrition.kt` | 991 | Compose screen and components | Medium, UI-file exception applies |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/screens/ScreenGoals.kt` | 859 | Compose screen and components | Medium, UI-file exception applies |

All non-UI files touched in this cleanup are now under the 300-line cap:

- `HealthConnectManager.kt`: 268 lines after extracting export, nutrition, archive sync, mapper, and goal-profile reader services.
- `AutomationBootstrap.kt`: 298 lines after extracting HTTP helpers, JSON serializers, and route aliases.
- `tools/mcc-mcp/src/server.ts`: 23 lines after splitting tool registration groups.
- `AppViewModel.kt`: 282 lines after extracting feature action classes for Health Connect, Goals, Add Meal, persistence, database, archive/day, and UI chrome.
- `AppViewModelQuickImportActions.kt`: 269 lines after moving commit helpers into `AppViewModelQuickImportHelpers.kt` and centralizing outcome clearing.

## Cleanup Completed In This Pass

- Added root project instructions in `agents.md`.
- Split Quick Import domain code into models, parser, mapper, planner, and sanitizer files.
- Split Goals domain code into models, calculator, and CSV persistence files.
- Split MCP server tool registration into device, quick import, Health Connect, settings, and goals groups.
- Split Health Connect code into manager facade, exporter, nutrition service, archive sync service, mapper, and goal-profile reader.
- Split debug automation code into bootstrap, HTTP helpers, JSON serializers, and route aliases.
- Split `AppViewModel` into a facade plus focused action classes:
  - `AppViewModelHealthConnectActions.kt`
  - `AppViewModelHealthConnectMealActions.kt`
  - `AppViewModelHealthConnectExportActions.kt`
  - `AppViewModelGoalsActions.kt`
  - `AppViewModelQuickImportActions.kt`
  - `AppViewModelPersistenceActions.kt`
  - `AppViewModelDatabaseActions.kt`
  - `AppViewModelArchiveDayActions.kt`
  - `AppViewModelUiActions.kt`
- Extracted shared nutrition UI models from Add Meal and Meals:
  - `ui/model/NutritionUiModels.kt`
  - shared meal grouping, day totals, macro progress, detail rows, and macro hint support.
- Extracted navigation and settings sheet contracts from app-level UI code:
  - `ui/navigation/AppRoutes.kt`
  - `ui/settings/SettingsSheet.kt`
  - debug automation now serializes settings sheets by stable key.
- Collapsed the legacy options-sheet entry point into the Settings page path:
  - removed `optionsSheetVisible` / `optionsSheetPage` state and facade setters.
  - removed the duplicate language, theme, Health Connect, archive, database, and support bottom sheet.
- Extracted shared bottom-sheet title/note components:
  - `ui/reused/SheetContent.kt`
  - reused by Settings, Add Meal, and Goals drawers.
- Consolidated Quick Import and historical import Health Connect payload mapping:
  - `QuickImportMapper.toHealthPayload(...)`
  - one mapping for total carbs, fiber, energy-from-fat, macro fields, names, and optional client record ids.

## Recommended Extraction Order

1. Keep public route strings and debug bridge aliases stable before route refactors.
2. Extract route constants and destination metadata from `App.kt`, `NavControllerListener.kt`, and debug automation.
3. Extract `App.kt` leaf UI pieces: scaffold chrome, drawer, settings page, dialogs, and route host.
4. Keep `AppViewModel` as a facade while moving persistence and feature coordinators behind smaller classes.
5. Split `HealthConnectManager.kt` by service area: permissions/status, export, goal profile reads, nutrition meals, historical import/delete, and legacy archive sync.
6. Split debug automation into HTTP server plumbing, endpoint handlers, request appliers, and JSON serializers.
7. Split large Compose screens only where components can move without obscuring the workflow.
8. Continue consolidating UI primitives: stat grids, meal summary rows, progress arcs, selectable rows, and standard bottom sheets.

## Regression Gates

- Domain model splits: `./gradlew testDebugUnitTest --tests '*QuickImport*' --console=plain`
- Goals splits: `./gradlew testDebugUnitTest --tests '*GoalCalculatorTest' --console=plain`
- MCP splits: `cd tools/mcc-mcp && npm test`
- Full Android unit pass: `./gradlew testDebugUnitTest --console=plain`
- Connected UI pass after screen or automation changes: focused `ScreenQuickImportTest`, `ScreenHealthConnectNutritionTest`, and `ScreenGoalsTest` instrumentation runs on the Fold emulator.

## Refactor Risks

- `AppUiState` contains mutable lists inside a data class. Moving reducers without changing mutation style can silently break Compose updates.
- Navigation route strings are also automation API strings. Renames can break the MCP/debug bridge while app tests still pass.
- Health Connect writes and deletes are high-risk because historical duplicate detection depends on client record IDs and fingerprinting.
- Legacy archive sync deletes Health Connect records by time range before reinserting. Treat it separately from insert-only quick import work.
- The visible name is "Add Meal", but internal `QuickImport` symbols are intentionally still named that way for now.
