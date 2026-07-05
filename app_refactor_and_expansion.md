# App Refactor and Expansion Technical Spec

This spec defines the next major evolution of Minimalist Calorie Counter into a focused Add Meal, Meals, Goals, and Health Connect workflow. The app should remain local-first, private, dark-mode-first, and optimized for one primary user who logs nutrition through ChatGPT-generated meal blurbs.

The implementation must follow `AGENTS.md`, keep non-display implementation files under the 300-line cap where practical, preserve existing debug automation contracts unless deliberately versioned, and update atomic docs under `docs/` when behavior changes.

## Phased Rollout Guide

### Phase 0 - Baseline, Guardrails, and Verification

Purpose: establish a known-good baseline before high-risk refactors.

Scope:

- Confirm `master` is clean and create a feature branch for each implementation slice.
- Run the current baseline:
  - `./gradlew testDebugUnitTest --console=plain`
  - `./gradlew lintDebug --console=plain`
  - `cd tools/mcc-mcp && npm test`
  - `./gradlew assembleDebug assembleDebugAndroidTest --console=plain`
- Confirm the Fold emulator and debug automation bridge are usable.
- Record any emulator/device instability as a blocker before making behavior changes.
- Keep Health Connect route strings, debug endpoint names, and MCP tool names stable.

Definition of done:

- Baseline tests are green or documented with exact known failures.
- Emulator/device verification path is available.
- No user data migration is attempted in this phase.

### Phase 1 - Health Connect Write Reliability

Purpose: make Add Meal writes trustworthy and recoverable.

Scope:

- Add a local meal write outbox with explicit states:
  - `Draft`
  - `PendingHealthConnect`
  - `Synced`
  - `FailedHealthConnect`
  - `Retrying`
- Make Add Meal commit produce a durable local record before or during the write, then update sync state based on Health Connect result.
- Ensure retry does not duplicate local database/day backup entries or Health Connect records.
- Preserve local database/day backup behavior, but make partial success visible instead of ambiguous.
- Add a small Add Meal/Settings sync status surface showing failed or pending writes.
- Add retry actions from the UI and debug automation.

Definition of done:

- A Health Connect failure after Add Meal commit creates a visible failed/pending state.
- Retrying the same meal does not duplicate food rows locally or in Health Connect.
- Successful writes clear pending state and refresh the Meals tab.
- Tests cover success, failure, retry, duplicate prevention, and UI state mapping.

### Phase 2 - Safer Health Connect Export and Cleanup

Purpose: keep powerful Health Connect tools while reducing accidental privacy and deletion risk.

Scope:

- Add export modes:
  - `Nutrition only`
  - `Nutrition and goals`
  - `Full Health Connect export`
- Add a redacted export option for ChatGPT check-ins.
- Keep CSV export to Downloads as the default output path, but make the sensitivity explicit.
- Add delete preview for app-owned Nutrition records:
  - date range
  - total records found
  - split by historical import vs Add Meal if identifiable
- Add cleanup choices:
  - delete historical imports only
  - delete Add Meal records only
  - delete all app-owned Nutrition records
- Keep spreadsheet import insert-only unless the user explicitly starts cleanup.

Definition of done:

- Full export still exists, but narrow/redacted modes are easier to choose.
- Destructive delete flow shows a record count before final confirmation.
- Export and delete progress survives normal screen navigation.
- Tests cover CSV headers, redaction, delete filters, and range ordering.

### Phase 3 - Add Meal Dashboard Expansion

Purpose: turn Add Meal from a text-entry screen into the daily nutrition cockpit.

Scope:

- Keep the primary flow:
  - paste meal blurb
  - parse preview
  - adjust meal time/type
  - send/write
  - see success animation and reset
- Promote the day summary card above parsed meal details.
- Expand the day card to show:
  - total calories consumed
  - calories remaining
  - macro progress for carbs, protein, fat, and fiber
  - next meal target
  - snack reserve
  - Health Connect sync status
- Add "Today check-in" copy/export action:
  - meals logged today
  - goals
  - remaining targets
  - recent weight/body metrics when available
- Keep local DB destination controls tucked behind the existing top-right menu.

Definition of done:

- Add Meal opens directly into a useful daily status view before any text is pasted.
- Parsed meal preview visually matches Meals tab meal/food treatment.
- Primary action remains visually obvious and reachable on Fold inner and outer screens.
- User can understand what is left for the day without leaving Add Meal.

### Phase 4 - Meals Review Flow

Purpose: make logged data easy to inspect, trust, reuse, and export.

Scope:

- Keep compact meal cards grouped by meal type and timestamp.
- Add inline collapse/expand per meal.
- Add meal-level actions:
  - copy meal summary
  - repeat meal
  - delete app-owned Health Connect records for that meal
  - open full detail drawer
- Add day-level actions:
  - copy day summary
  - export day check-in
  - refresh through pull-to-refresh
- Preserve macro icon and progress treatment from the current design.

Definition of done:

- A day with many foods is scannable without excessive vertical scrolling.
- Meal totals, foods, and goal progress are visible without opening every detail drawer.
- Detail drawers are consistent with Add Meal and Goals sheets.
- Repeat/delete/copy actions are available but visually secondary.

### Phase 5 - Goals History and Recommendations

Purpose: make Goals a living recommendation system instead of a static settings page.

Scope:

- Keep Health Connect profile hydration for weight, height, body fat, lean mass, and related metrics.
- Preserve manual field locks; Health Connect can update only unlocked fields.
- Add weekly recommendation history:
  - recommendation date
  - source measurements used
  - calorie target
  - macro targets
  - weight-loss setting
  - applied/not applied state
- Keep goal recalculation quiet and state-driven: Health Connect values refresh on app load, and the Goals page surfaces a review prompt only when a recommendation materially differs from the current unlocked targets.
- Add trend cards:
  - weight trend
  - body fat trend
  - lean mass trend
  - average calorie adherence
  - macro adherence

Definition of done:

- Goals explains current targets and why they exist.
- Health Connect-derived fields are visibly different from locked manual fields.
- Past target decisions are reviewable.
- Add Meal and Meals both consume the same active goal model.

### Phase 6 - Local Persistence Upgrade

Purpose: replace brittle app-state CSV persistence with durable structured local storage while preserving CSV import/export.

Scope:

- Introduce Room for:
  - local meal backups
  - outbox writes
  - goal profile and goal history
  - app preferences
  - import/export job history
- Keep CSV as an import/export format, not the primary app state store.
- Add migrations from existing CSV-backed data.
- Preserve Android backup rules for settings, goals, and local backup records.

Definition of done:

- Uninstall/reinstall behavior is as good as Android allows through backup restore.
- Debug pushes do not unnecessarily wipe goals/settings/local backup state.
- CSV import/export remains available for troubleshooting and portability.
- Persistence tests cover migration, rollback, and corrupt input handling.

### Phase 7 - App Shell and Architecture Refactor

Purpose: reduce risk and improve maintainability without changing user behavior.

Scope:

- Split `App.kt` into focused files:
  - app entry/composition root
  - scaffold and navigation chrome
  - route host
  - top app bar
  - bottom navigation
  - hamburger drawer
  - settings screen
  - dialog host
  - file launchers
  - legacy maintenance routes
- Centralize destination metadata:
  - route
  - title
  - nav icon
  - drawer visibility
  - bottom-bar visibility
  - automation alias
- Keep `AppViewModel` as a facade only if it remains thin.
- Move feature coordination into small action/coordinator classes.
- Split large screen files only where extraction improves clarity:
  - shared cards
  - drawers
  - pure display components
  - state mappers

Definition of done:

- Non-display implementation files stay under the project line cap.
- Navigation metadata has one source of truth.
- Debug automation aliases remain stable or are deliberately versioned.
- No user-visible behavior changes occur without matching tests.

### Phase 8 - Full Regression, Device Polish, and Release Hardening

Purpose: prove the completed system works across the real target workflow.

Scope:

- Run complete JVM, lint, MCP, assemble, and instrumentation suites.
- Run Fold emulator inner and outer display smoke passes.
- Install and smoke test on the physical Fold when available.
- Verify Health Connect permissions, write, read, export, delete, goals hydration, and retry.
- Review all drawers and screens for consistent color layering, icon use, spacing, and touch targets.
- Update `README.md`, `PRIVACY_POLICY.md`, and relevant `docs/` files.

Definition of done:

- The app is shippable for the user's daily nutrition workflow.
- No known high-risk data loss path remains unguarded.
- All critical flows have automated tests and manual smoke evidence.

## Final Definition of Done

When this spec is complete, the application should be in the following state.

### Product State

- The app is primarily a three-tab nutrition tool:
  - Add Meal
  - Meals
  - Goals
- Settings remains available for permissions, export, cleanup, theme, and troubleshooting.
- Database and archive functionality remains available only as maintenance/troubleshooting where still needed.
- Legacy recipe, language, and unused food/archive workflows are removed or hidden unless they directly support the modern workflow.
- Health Connect is the main integration target and the main source for Meals review.
- Local storage acts as a backup, retry queue, settings store, and import/export staging layer.

### Look and Feel

- The app keeps a dark, charcoal-first Material 3 Expressive direction.
- Background hierarchy is clear:
  - deepest app background is near-black/charcoal
  - cards and sheets get progressively lighter as they rise
  - borders are subtle and derived from surface colors, not pure white
- Color is used mostly through icons, progress indicators, and focused accents rather than large tinted surfaces.
- Add Meal, Meals, Goals, and Settings share the same surface, drawer, chip, and macro-card vocabulary.
- Macro indicators use recognizable colored icons and consistent tap hints.
- Primary actions are obvious, large enough for Fold outer screen use, and visually distinct.
- Destructive actions are visually separated and require confirmation.

### UX State

- Add Meal answers "what am I adding and what does it do to today?"
- Meals answers "what did I log, what is in Health Connect, and how am I doing today?"
- Goals answers "what are my current targets, where did they come from, and how are they changing?"
- Settings answers "what integrations and maintenance tools are available?"
- User-facing copy is short, action-oriented, and avoids explaining obvious UI behavior.
- Health Connect permission gaps are explicit and recoverable.
- Partial failures do not look like success.
- Long-running work shows progress and can be retried.

### Data and Reliability State

- Add Meal writes one Health Connect Nutrition record per parsed food.
- Each Health Connect write has a durable local sync state.
- Retrying writes is idempotent.
- Historical imports skip duplicates by client record id and content fingerprint.
- Deletes are previewed before execution and scoped by explicit user choice.
- Exports can be narrow, redacted, or full.
- Goals use Health Connect values only for unlocked fields.
- Local backup and settings survive normal debug deploys and Android backup restore where the platform permits.

### Testing State

- Unit tests cover:
  - Add Meal parser
  - Add Meal planner
  - Health Connect payload mapping
  - outbox state transitions
  - duplicate prevention
  - goals calculations
  - CSV export modes
  - delete filters
  - route metadata
- Instrumentation tests cover:
  - Add Meal parse/commit/failure/retry
  - Meals day review and detail drawers
  - Goals settings and picker drawers
  - Settings export/delete drawers
  - Fold inner and outer layout constraints
- MCP/debug tests cover:
  - navigation
  - Add Meal preview/commit
  - Meals date selection
  - Goals state and settings updates
  - export/delete commands
  - retry/outbox state
- Manual smoke covers:
  - Fold emulator inner display
  - Fold emulator outer display
  - physical Fold install when connected
  - Health Connect permission grant/read/write loop

## Current Architecture Baseline

The app already has useful separation after prior cleanup:

- Health Connect code is split into manager, nutrition service, archive sync service, exporter, mapper, goal-profile reader, retry helper, and permission scopes.
- Add Meal domain code is split into parser, models, mapper, planner, sanitizer, and ViewModel action helpers.
- Goals domain code is split into models, calculator, and CSV persistence.
- Debug automation and MCP tooling exist and should be preserved.
- Shared nutrition UI models exist for day totals, meal grouping, macro progress, and macro hints.

Remaining high-risk files:

- `App.kt` remains the largest architectural risk because it mixes route wiring, settings UI, dialogs, launchers, and legacy flows.
- `ScreenQuickImport.kt`, `ScreenHealthConnectNutrition.kt`, and `ScreenGoals.kt` are large display files. They can stay large only while they are mostly component/display code. Extract logic, state mapping, and shared components as work continues.

## Detailed Requirements

### Health Connect Outbox

Add a local model representing meal write intent and sync state.

Required fields:

- local id
- created timestamp
- intended meal timestamp
- meal type
- source text hash
- parsed meal summary
- food payloads
- Health Connect client record ids
- state
- attempt count
- last attempt timestamp
- last error message

Behavior:

- Commit creates or reuses an outbox item based on stable fingerprint.
- Health Connect write uses deterministic client record ids.
- Success marks the outbox item synced.
- Failure preserves the outbox item and exposes retry.
- Retry reuses the same client record ids.
- If Health Connect already contains matching records, retry reconciles to synced rather than failing.

Tests:

- successful write transitions pending to synced
- failed write transitions pending to failed
- retry after failure does not duplicate
- duplicate Health Connect records reconcile to synced
- local backup remains available after failure

### Health Connect Export

Modes:

- Nutrition only:
  - Nutrition records
  - goal summary for the selected range if needed for check-ins
- Nutrition and goals:
  - Nutrition records
  - weight, height, body fat, lean mass
  - active goal targets
  - weekly recommendation history
- Full:
  - all currently supported readable Health Connect record types
  - raw record column retained

Redaction:

- Remove record ids, client record ids, package names, recording method, and raw record by default.
- Keep date, time, record type, calories, macros, meal type, body metrics, and goal context.
- Require explicit opt-in for raw/full export.

Tests:

- each export mode has stable headers
- redacted export omits sensitive fields
- date ranges are inclusive
- empty ranges generate a valid CSV with headers

### Health Connect Cleanup

Required controls:

- start date
- end date
- cleanup mode
- preview count
- final confirmation

Cleanup modes:

- historical imports only
- Add Meal records only
- all app-owned Nutrition records

Behavior:

- Preview reads matching records and shows count before delete.
- Delete only removes app-owned records.
- Progress updates while scanning and deleting.
- Delete can be retried safely if interrupted.

Tests:

- preview filters correctly
- delete does not remove non-app records
- historical-only mode ignores Add Meal records
- Add Meal-only mode ignores historical records

### Add Meal Dashboard

Day card requirements:

- selected date
- calories consumed
- calories remaining
- progress indicators for calories, carbs, protein, fat, and fiber
- current meal target
- snack reserve
- Health Connect sync status
- tap/copy check-in affordance

Parsed meal requirements:

- meal header with meal type, time, calories, and macro chips
- compact food rows
- detail drawer for each food
- detail drawer for meal totals
- send action as floating primary action
- clear action as secondary icon

Success behavior:

- show transient success animation
- reset pasted text
- keep selected date/time logic sensible
- refresh Meals data
- update day card totals
- update outbox/sync status

Tests:

- day card renders before parse
- parse preview renders foods and totals
- send success resets text and refreshes meals
- send failure leaves text and shows retry/error state
- outer Fold width keeps calorie and macro chips on intended rows

### Meals Page

Day header requirements:

- selected date
- previous/next date controls
- tap date opens date picker drawer
- pull-to-refresh instead of a refresh icon
- day goal progress

Meal card requirements:

- meal type icon
- meal title/time
- calories chip
- macro chips
- compact food rows with name and calories
- tap food opens food detail drawer
- tap meal opens meal detail drawer
- overflow/actions drawer for copy, repeat, and delete when available

Tests:

- grouped meals render by date
- multi-food day remains compact
- goal progress appears when goals are configured
- over-goal progress uses overage treatment
- details and actions drawers open and dismiss

### Goals Page

Profile fields:

- birthday
- sex
- height
- weight
- body fat percentage
- lean mass
- lifestyle
- weight-loss target

Macro goals:

- calories
- protein
- carbs
- fat
- fiber

Behavior:

- Required fields validate clearly.
- Pickers use bottom drawers.
- Manual values lock fields.
- Unlocking allows Health Connect to update the field.
- Recommendation card explains current target calculation.
- Weekly recommendation history persists.

Tests:

- required-field validation identifies missing fields
- Health Connect refresh updates unlocked fields only
- locked fields survive refresh
- date picker drawer updates birthday without save button
- recommendation calculation uses current profile

### Settings

Sections:

- Health Connect permissions
- Export from Health Connect
- Remove Health Connect meals and nutrition
- Theme
- Troubleshooting
- Debug automation status in debug builds

Behavior:

- Settings should be a hub, not an accordion list.
- Each major tool opens a bottom drawer.
- Destructive tools are visually separated.
- Export and delete tools show progress and result.

Tests:

- each drawer opens from Settings and can be dismissed
- export mode selection persists for the operation
- delete preview must occur before delete confirmation
- debug-only controls are absent from release builds

### Architecture

Route metadata should be centralized in one model that includes:

- stable route string
- visible title
- content description
- selected/unselected icon
- bottom nav eligibility
- drawer eligibility
- debug automation aliases

`App.kt` should become a composition root, not an implementation container.

Target file groups:

- `ui/app/`
  - app scaffold
  - app chrome
  - dialog host
  - launcher host
- `ui/navigation/`
  - routes
  - metadata
  - navigation helpers
- `ui/settings/`
  - settings screen
  - settings sheets
  - export/delete UI
- `features/addmeal/`
  - UI components
  - state mapper
  - outbox coordinator
- `features/meals/`
  - UI components
  - meal actions
  - day summary mapper
- `features/goals/`
  - UI components
  - profile editor
  - recommendation history
- `data/local/`
  - Room database
  - DAOs
  - migrations
- `health/`
  - Health Connect facades and services

Constraints:

- Do not duplicate route strings across UI, MCP, and debug automation.
- Do not duplicate macro mapping between Add Meal, historical import, and Health Connect.
- Do not duplicate goal progress UI between Add Meal and Meals.
- Preserve public debug API behavior unless the API version is explicitly updated.

### Automation and MCP

Required debug API additions:

- get outbox state
- retry failed write
- clear failed write for test setup
- create seeded goal profile
- set active tab
- open each major drawer
- run export preview/delete preview
- trigger export mode

Required MCP additions:

- expose the new debug API methods as tools
- keep existing tool names stable
- add tests with injected runners

Tests:

- unit tests for request parsing
- MCP tests for tool payloads
- automation smoke for Add Meal, Meals, Goals, Settings, export, delete, and retry

## Regression Test Matrix

Run the relevant subset during each phase and the full matrix before completion.

Core:

```bash
./gradlew testDebugUnitTest --console=plain
./gradlew lintDebug --console=plain
./gradlew assembleDebug assembleDebugAndroidTest --console=plain
cd tools/mcc-mcp && npm test
```

Connected:

```bash
./gradlew connectedDebugAndroidTest --console=plain
scripts/android/run-fold-smoke.sh
scripts/android/run-fold-health-smoke.sh
scripts/android/run-automation-smoke.sh
```

Focused connected suites:

```bash
./gradlew connectedDebugAndroidTest --tests '*ScreenQuickImportTest' --console=plain
./gradlew connectedDebugAndroidTest --tests '*ScreenHealthConnectNutritionTest' --console=plain
./gradlew connectedDebugAndroidTest --tests '*ScreenGoalsTest' --console=plain
```

Manual smoke:

- Add Meal paste, parse, send, success animation, reset.
- Health Connect permission grant/read/write.
- Meals refresh and date navigation.
- Goals Health Connect refresh and manual lock behavior.
- Settings export and delete preview.
- Fold emulator outer screen review.
- Fold emulator inner screen review.
- Physical Fold install when connected.

## Risks and Mitigations

### Health Connect API Behavior

Risk: Health Connect permissions and record behavior can differ across device state, Android version, and granted scopes.

Mitigation:

- Gate every operation by explicit permission scope.
- Keep read/write errors visible.
- Use emulator and physical device smoke tests.
- Avoid assuming adb can directly inspect Health Connect storage.

### Partial Writes

Risk: local backup succeeds while Health Connect fails.

Mitigation:

- Add outbox state.
- Expose retry.
- Reconcile duplicate Health Connect records by deterministic client ids.

### Destructive Cleanup

Risk: user deletes more Health Connect data than intended.

Mitigation:

- Preview counts before delete.
- Split delete modes.
- Keep spreadsheet import insert-only.

### Refactor Drift

Risk: route or automation refactors break MCP/debug control without breaking app UI tests.

Mitigation:

- Centralize route metadata.
- Add route metadata tests.
- Keep MCP tests green through each route change.

### Visual Regression

Risk: Fold inner/outer screens regress in chip wrapping, progress arcs, or drawer spacing.

Mitigation:

- Add connected UI tests with stable test tags.
- Run outer and inner Fold smoke.
- Capture screenshots for layout changes.

## Documentation Updates Required During Implementation

Update these docs as behavior changes:

- `docs/healthconnectintegration.md`
  - outbox behavior
  - export modes
  - cleanup modes
  - permission scope changes
- `docs/api.md`
  - debug automation endpoint additions
- `docs/mcp.md`
  - MCP tool additions
- `docs/database.md`
  - Room/local backup behavior and CSV import/export role
- `docs/testing.md`
  - new test commands and required smoke matrix
- `docs/architecture-audit.md`
  - completed extractions and remaining large files
- `README.md`
  - user-facing workflow changes
- `PRIVACY_POLICY.md`
  - export and local storage behavior changes

## Non-Goals

- Do not build a cloud API for Health Connect writes. Health Connect is device-local.
- Do not restore legacy recipe functionality.
- Do not invest in a full food database product beyond local backup/troubleshooting.
- Do not make historical archive totals the primary workflow again.
- Do not remove local backup behavior from Add Meal.
- Do not make broad Health Connect export the default ChatGPT check-in path.
