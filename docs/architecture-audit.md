# Architecture Audit

This audit tracks large files and the safest cleanup sequence for the current Android app, debug automation bridge, and local MCP server.

## Current Oversized Files After Cleanup

Generated output, Gradle build folders, `node_modules`, and IDE metadata are excluded.

| File | Approx. lines | Type | Cleanup priority |
| --- | ---: | --- | --- |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/screens/ScreenQuickImport.kt` | 1417 | Compose screen and components | Medium, UI-file exception applies |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/screens/HealthNutritionMealComponents.kt` | 601 | Compose meal display components | Low, UI-file exception applies |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/screens/ScreenHealthConnectNutrition.kt` | 576 | Compose screen and components | Low, UI-file exception applies |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/screens/GoalsSettingsComponents.kt` | 500 | Compose goals settings components | Low, UI-file exception applies |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/settings/AppSettingsPage.kt` | 461 | Compose settings page | Low, UI-file exception applies |
| `app/src/main/java/com/makstuff/minimalistcaloriecounter/ui/screens/GoalsDashboardComponents.kt` | 449 | Compose goals dashboard display components | Low, UI-file exception applies |

All non-UI files touched in this cleanup are now under the 300-line cap:

- `HealthConnectManager.kt`: 300 lines after extracting export, nutrition, archive sync, mapper, goal-profile reader, and permission-scope policy helpers.
- `AutomationBootstrap.kt`: 298 lines after extracting HTTP helpers, JSON serializers, and route aliases.
- `tools/mcc-mcp/src/server.ts`: 23 lines after splitting tool registration groups.
- `AppViewModel.kt`: 297 lines after extracting feature action classes for Health Connect, Goals, Add Meal, persistence, database, archive/day, and UI chrome.
- `AppViewModelQuickImportActions.kt`: 300 lines after moving outbox persistence helpers into `AppViewModelQuickImportHelpers.kt`.

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
- Consolidated screen panel styling:
  - `ui/reused/SurfacePanel.kt`
  - reused by Add Meal, Meals, and Goals instead of three private duplicate implementations.
- Consolidated Health Connect quota retry behavior:
  - `health/HealthConnectRetry.kt`
  - archive sync and historical meal import now use the same retry/backoff helper.
- Added Health Connect permission scopes:
  - `health/HealthConnectPermissionScope.kt`
  - `HealthConnectManager` now gates each operation by required capability instead of duplicating raw permission-set checks.
  - archive sync is gated by write permissions only, while historical import/delete still require write plus nutrition read.
- Centralized top-level app navigation:
  - `ui/navigation/AppNavigation.kt`
  - top-level routes use `launchSingleTop`, `restoreState`, and a stable route set from `AppRoutes`.
- Centralized first destination metadata:
  - `ui/navigation/AppDestinations.kt`
  - bottom-bar order, drawer visibility, nav highlight key, icon id, label, and accent color now share one metadata source.
- Extracted app chrome components:
  - `ui/navigation/AppChrome.kt`
  - bottom navigation and hamburger drawer now live outside `App.kt` while preserving destination metadata and route behavior.
- Extracted top app bar:
  - `ui/navigation/AppTopBar.kt`
  - title, hamburger action, and page-specific top-right settings actions now live outside `App.kt`.
- Extracted startup effects:
  - `AppStartupEffects.kt`
  - automation navigation, Health Connect permission refresh, CSV bootstrap, and loading completion now live outside `App.kt`.
- Extracted route host:
  - `ui/navigation/AppRouteHost.kt`
  - modern Add Meal, Meals, Goals, Settings routes and legacy route registration now live outside `App.kt`.
- Extracted Health Connect sync dialogs:
  - `AppHealthConnectDialogs.kt`
  - progress, keep-screen-on behavior, and sync error confirmation now live outside `App.kt`.
- Extracted Settings display components:
  - `ui/settings/SettingsComponents.kt`
  - settings cards, option rows, selectable rows, and section headers now live outside `App.kt`.
- Extracted app file launchers:
  - `AppFileLaunchers.kt`
  - database/archive import-export launchers and historical meal import parsing now live outside `App.kt`.
- Extracted confirmation dialog host:
  - `AppConfirmationDialogs.kt`
  - reset, import, delete, and Health Connect confirmation prompts now live outside `App.kt`.
- Extracted Settings page:
  - `ui/settings/AppSettingsPage.kt`
  - settings cards, sheets, date pickers, and Health Connect settings actions now live outside `App.kt`.
- Extracted legacy maintenance routes:
  - `ui/navigation/legacy/LegacyArchiveRoutes.kt`
  - `ui/navigation/legacy/LegacyDatabaseRoutes.kt`
  - `ui/navigation/legacy/LegacyDayRoutes.kt`
  - old archive, database, and day-builder routes are isolated from the modern Add Meal/Meals/Goals route host.
- Extracted Add Meal destination dialog host:
  - `ui/screens/QuickImportDestinationDialogHost.kt`
  - Add Meal destination settings now live outside `App.kt`.
  - the Add Meal destination bottom sheet implementation now lives with the host instead of inside `ScreenQuickImport.kt`.
- Extracted Add Meal nutrient detail components:
  - `ui/screens/QuickImportNutrientDetails.kt`
  - food detail sheet and shared nutrient detail pills now live outside `ScreenQuickImport.kt`.
- Extracted Health Connect Meals display components:
  - `ui/screens/HealthNutritionMealComponents.kt`
  - meal cards, food rows, detail drawers, macro grids, and status/section primitives now live outside `ScreenHealthConnectNutrition.kt`.
- Extracted Goals dashboard display components:
  - `ui/screens/GoalsDashboardComponents.kt`
  - hero, recommendation, recalculation, history, macro target, profile snapshot, and trend cards now live outside `ScreenGoals.kt`.
- Extracted Goals settings components:
  - `ui/screens/GoalsSettingsComponents.kt`
  - settings drawer, profile picker drawers, measurement inputs, macro inputs, and lock controls now live outside `ScreenGoals.kt`.
- Added a single settings-sheet automation intent:
  - `AppViewModelUiActions.openSettingsSheet(...)`
  - `/settings/open` now navigates to Settings and opens the requested drawer as one command.
- Added the first Add Meal Health Connect outbox slice:
  - `classes/QuickImportOutbox.kt`
  - `classes/QuickImportOutboxCsv.kt`
  - `AppViewModelQuickImportRetryActions.kt`
  - Add Meal writes now use deterministic client record ids and persist pending/synced/failed write state plus retry payloads.
  - Add Meal Health Connect writes now pre-read app-owned Nutrition records and skip existing records by client record id or nutrition fingerprint.
  - debug automation and MCP expose outbox rows and retry actions for verification.
- Split Health Connect export code into exporter orchestration, CSV schema/redaction, row mapping, mode definitions, and record-type permission lists.
- Added Health Connect cleanup preview and mode classification so Settings can preview destructive deletes before removing app-owned Nutrition records.
- Added Add Meal today check-in summary generation as a testable UI model helper; the screen only handles clipboard copy state.
- Added Meals review day/meal summary generation as testable UI model helpers; the screen owns only clipboard copy state.
- Added Meals review collapse helpers so long meals start compact and expand inline without moving that logic into the screen.
- Added meal-level Health Connect deletes by forwarding the selected meal group's app-owned record ids through the existing delete path.
- Added meal repeat preparation through Add Meal so repeat writes reuse the existing parser, outbox, and Health Connect commit flow.
- Added day check-in text export to Downloads and extracted a shared Downloads text writer for Health Connect CSV and check-in exports.
- Added richer Goals recommendation history metadata and a compact history card so applied targets show their BMR/TDEE and measurement context.
- Added explicit Goals Sunday recalculation status with a testable schedule helper and a visible recalculation card.
- Added Goals trend/adherence cards backed by recommendation-history body metrics and the currently loaded Health Connect meals day.
- Added the Phase 6 Room persistence foundation:
  - Room/KSP dependencies and schema export.
  - `persistence/room/AppDatabase.kt`
  - Room entities and DAOs for preferences, goals, Add Meal outbox, local meal backups, and import/export jobs.
  - Room seed mappers for Goals and Add Meal outbox data.
  - CSV seed planner tests that reject corrupt outbox input before database writes.
  - Android backup/device-transfer rules for `mcc.db`, `mcc.db-shm`, and `mcc.db-wal`.
- Wired the first Phase 6 Room runtime surfaces:
  - Goals reads Room first, falls back to CSV, and seeds Room during migration.
  - Add Meal outbox reads Room first, falls back to CSV, and seeds Room during migration.
  - App preferences read Room first, fall back to CSV, and seed Room during migration.
  - Goals, Add Meal outbox, and app preference writes are mirrored to CSV while Room migration continues.
  - Add Meal writes local meal backup rows to Room for every committed meal food.
  - Health Connect export and delete attempts write import/export job history rows to Room.
  - Added a real instrumentation round-trip test for `AppRoomStore`.

## Recommended Extraction Order

1. Keep public route strings and debug bridge aliases stable before route refactors.
2. Finish destination metadata extraction from `App.kt`, `NavControllerListener.kt`, and debug automation. Route constants, automation aliases, and top-level route membership are centralized; titles, icons, and drawer/bottom-bar metadata are the remaining split.
3. Extract `App.kt` leaf UI pieces: scaffold chrome, drawer, settings page, dialogs, and route host.
4. Keep `AppViewModel` as a facade while moving persistence and feature coordinators behind smaller classes.
5. Continue splitting `HealthConnectManager.kt` by service area only when it would keep the facade under 300 lines or make permissions easier to test. The current permission-scope boundary is the preferred entry point for new Health Connect gates.
6. Split debug automation into HTTP server plumbing, endpoint handlers, request appliers, and JSON serializers.
7. Split large Compose screens only where components can move without obscuring the workflow.
8. Continue consolidating UI primitives: stat grids, meal summary rows, progress arcs, selectable rows, and standard bottom sheets.

## Regression Gates

- Domain model splits: `./gradlew testDebugUnitTest --tests '*QuickImport*' --console=plain`
- Goals splits: `./gradlew testDebugUnitTest --tests '*GoalCalculatorTest' --console=plain`
- MCP splits: `cd tools/mcc-mcp && npm test`
- Full Android unit pass: `./gradlew testDebugUnitTest --console=plain`
- Room foundation pass: `./gradlew testDebugUnitTest --tests '*persistence.room*' --console=plain`
- Connected UI pass after screen or automation changes: focused `ScreenQuickImportTest`, `ScreenHealthConnectNutritionTest`, and `ScreenGoalsTest` instrumentation runs on the Fold emulator.

## Refactor Risks

- `AppUiState` contains mutable lists inside a data class. Moving reducers without changing mutation style can silently break Compose updates.
- Navigation route strings are also automation API strings. Renames can break the MCP/debug bridge while app tests still pass.
- Health Connect writes and deletes are high-risk because historical duplicate detection depends on client record IDs and fingerprinting.
- Legacy archive sync deletes Health Connect records by time range before reinserting. Treat it separately from insert-only quick import work.
- The visible name is "Add Meal", but internal `QuickImport` symbols are intentionally still named that way for now.
