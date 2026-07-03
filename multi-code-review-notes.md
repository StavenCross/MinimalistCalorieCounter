# Multi Code Review Notes

Scope: full `MinimalistCalorieCounter` codebase review requested on 2026-07-03.

Requested review count: 30 full reviews.

Baseline at start:
- Branch: `master`
- Head: `b56149b Deepen architecture boundaries`
- Intent: clean project boundaries, remove duplication, verify correctness across Android app, Health Connect integration, debug automation bridge, local MCP tooling, docs, and tests.

Available tools and checks selected:
- Repo search/read: `rg`, `find`, `sed`, `git diff`, `git status`, `git log`
- Android build/checks: `./gradlew compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`
- Android connected checks: `adb`, focused Compose instrumentation tests, Fold emulator smoke
- MCP/tooling checks: `cd tools/mcc-mcp && npm test`
- Runtime automation smoke: `scripts/android/run-automation-smoke.sh`
- Review support: subagents for bug, spec, maintainability, security/perf review lanes; main reviewer reconciles findings.

Applicable instructions:
- `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- `/Users/cmiller/.codex/skills/multi-code-review/SKILL.md`

Relevant repo docs:
- `README.md`
- `docs/android-test-bench.md`
- `docs/api.md`
- `docs/architecture-audit.md`
- `docs/automation.md`
- `docs/database.md`
- `docs/healthconnectintegration.md`
- `docs/mcp.md`
- `docs/testing.md`

## Review 1

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `docs/*.md`
- Diff/worktree reviewed: pending
- Checks planned: `git status`, repo inventory, targeted static search, Gradle unit/lint/build, MCP tests, emulator connected tests

### Findings
- Debug automation/MCP docs were stale.
  - Severity: Minor
  - Evidence: `app/src/debug/java/com/makstuff/minimalistcaloriecounter/automation/AutomationBootstrap.kt` exposes `/health-connect/export-range` and `/goals/*` endpoints; `tools/mcc-mcp/src/healthTools.ts` exposes `mcc_health_export_range`; `tools/mcc-mcp/src/goalsTools.ts` exposes Goals tools; `docs/api.md` and `docs/mcp.md` omitted them.
  - Why it matters: future automation/control work depends on docs matching the debug bridge and local MCP server. Missing endpoints cause operators to bypass existing tooling or assume features are unsupported.
  - Fix plan: update atomic API/MCP docs to include implemented endpoints and settings sheet keys.
- Manifest duplicate attribute concern was a false positive.
  - Severity: None
  - Evidence: `nl -ba app/src/main/AndroidManifest.xml` shows one `android:name` on the `activity-alias` at line 90.
  - Why it matters: invalid manifest XML would block builds; verified no issue.
  - Fix plan: no code change.

### Changes Made
- `docs/api.md`:
  - What changed: documented `theme` and `language` settings sheet keys, Health Connect export endpoint, and Goals automation endpoints.
  - Why: align docs with actual debug automation contract.
- `docs/mcp.md`:
  - What changed: documented `mcc_health_export_range` and Goals MCP tools.
  - Why: align MCP docs with registered tools.

### Tests and Checks
- `git status --short --branch`
  - Result: only review notes and docs changes are dirty.
  - Notes: baseline was clean before notes file.
- Source/docs inventory and static risk searches with `find`, `rg`, `wc -l`
  - Result: completed; initial broad search included generated directories, then narrowed to maintained source/docs.
  - Notes: line-cap candidates remain known in `docs/architecture-audit.md`.
- `./gradlew testDebugUnitTest lintDebug --console=plain`
  - Result: passed.
  - Notes: unit tests were up to date; lint succeeded.
- `cd tools/mcc-mcp && npm test`
  - Result: passed, 6 tests.
- `git diff --check`
  - Result: passed.

### Re-checks From Previous Reviews
- None.

### Recommendations From This Review
- Add an automated docs/contract snapshot test for debug endpoints and MCP tool names so docs drift is caught by CI/local checks.

### Remaining Risks
- Health Connect export remains privacy-heavy by design because it exports all readable record types and includes `raw_record`; later reviews should re-check whether UI copy and docs make that sufficiently explicit.

## Review 2

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `docs/*.md`
- Diff/worktree reviewed: yes
- Checks planned: re-check docs fix, Health Connect export/delete review, route/debug API review, source-only static searches

### Findings
- Health Connect integration docs understated current permissions.
  - Severity: Minor
  - Evidence: `app/src/main/AndroidManifest.xml` declares broad Health Connect read permissions plus nutrition/weight writes; `docs/healthconnectintegration.md` only listed Read Nutrition, Write Nutrition, and Write Weight.
  - Why it matters: this app handles sensitive health data. Documentation should accurately explain why broad permissions are requested, especially export and Goals profile reads.
  - Fix plan: update Health Connect docs to distinguish core meal logging permissions from broad export permissions.

### Changes Made
- `docs/healthconnectintegration.md`:
  - What changed: documented nutrition, weight, height, body-fat, lean-mass, export, and historical read permission purposes.
  - Why: align sensitive permission docs with manifest and app behavior.

### Tests and Checks
- Re-read `docs/api.md`, `docs/mcp.md`, `docs/healthconnectintegration.md`, `AutomationBootstrap.kt`, MCP tool registration, and `AndroidManifest.xml`.
  - Result: docs now match implemented debug endpoints and broad Health Connect permission purpose.
  - Notes: no code behavior changed in Review 2.

### Re-checks From Previous Reviews
- Docs endpoint drift:
  - Re-check result: fixed; `docs/api.md` and `docs/mcp.md` include the previously missing endpoints/tools.

### Recommendations From This Review
- Keep permission-purpose docs adjacent to future Health Connect permission changes.

### Remaining Risks
- Export is intentionally broad and writes to public Downloads. Later reviews should continue to assess whether additional in-app warnings or export scoping are needed.

## Review 3

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `docs/*.md`
- Diff/worktree reviewed: yes
- Checks planned: Quick Import commit/persistence review, Goals CSV review, source-only static search

### Findings
- Goals CSV read path used manual comma splitting.
  - Severity: Major
  - Evidence: `AppCsvStore.readGoals()` split saved `goals.csv` lines with `split(",")`.
  - Why it matters: valid quoted commas in recommendation warnings corrupt persisted Goals state.
  - Fix plan: parse Goals rows with the CSV parser and add regression coverage.
- Health Connect quick meal writes logged food names and macro values.
  - Severity: Major
  - Evidence: `HealthConnectNutritionService.insertQuickMealNutrition()` emitted each payload to logcat.
  - Why it matters: meal names and nutrients are health data.
  - Fix plan: remove production payload logs.
- Debug automation HTTP parsing used unsafe character-count body reads.
  - Severity: Major
  - Evidence: `AutomationHttp.readRequest()` allocated `CharArray(contentLength)` from HTTP `Content-Length`.
  - Why it matters: non-ASCII JSON could be truncated and large bodies could allocate excessively.
  - Fix plan: read bounded UTF-8 bytes from `InputStream`.

### Changes Made
- `AppCsvStore.kt`, `AppCsvStoreTest.kt`: added CSV parser-based Goals row reading and quoted-comma test.
- `HealthConnectNutritionService.kt`: removed meal payload logging.
- `AutomationHttp.kt`, `AutomationBootstrap.kt`, `AutomationHttpTest.kt`: moved to bounded byte-correct request parsing.

### Tests and Checks
- Focused tests initially exposed parser/test-fixture issues; iterated until green.
- `./gradlew testDebugUnitTest --tests '*AppCsvStoreTest' --tests '*AutomationHttpTest' --tests '*HealthConnectPagingTest' --console=plain`: passed after fixes.

### Re-checks From Previous Reviews
- Endpoint docs:
  - Re-check result: still documented in `docs/api.md` and `docs/mcp.md`.
- Health Connect permission docs:
  - Re-check result: still documented in `docs/healthconnectintegration.md`.

### Recommendations From This Review
- Add socket-level debug bridge tests later if the bridge grows beyond local automation.

### Remaining Risks
- Debug bridge returns HTTP 500 for malformed debug-only requests; acceptable for current local scope but not ideal.

## Review 4

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `docs/*.md`
- Diff/worktree reviewed: yes
- Checks planned: Health Connect Nutrition read/delete/import pagination review

### Findings
- App-owned Nutrition reads were not paginated.
  - Severity: Major
  - Evidence: `HealthConnectNutritionService.readNutritionRecords()` called `readRecords()` once with no `pageSize`/`pageToken`.
  - Why it matters: Meals display, duplicate detection, cleanup, and delete range could miss records.
  - Fix plan: add a shared Health Connect paging helper and use it for Nutrition reads.

### Changes Made
- `HealthConnectPaging.kt`, `HealthConnectPagingTest.kt`, `HealthConnectNutritionService.kt`: added `readAllHealthConnectPages()` and applied it with page size 500.

### Tests and Checks
- `./gradlew testDebugUnitTest --tests '*HealthConnectPagingTest' --console=plain`: passed.

### Re-checks From Previous Reviews
- Goals CSV parser and debug HTTP parser tests still passed in the focused test run.

### Recommendations From This Review
- Reuse `HealthConnectPaging.kt` for future non-export Health Connect readers.

## Review 5

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `PRIVACY_POLICY.md`, `docs/*.md`
- Diff/worktree reviewed: yes
- Checks planned: privacy and permissions disclosure review

### Findings
- Privacy policy contradicted implemented Health Connect reads.
  - Severity: Major
  - Evidence: `PRIVACY_POLICY.md` said the app does not read Health Connect; code reads Nutrition/body metrics/export record types.
  - Why it matters: permission disclosure must match Health Connect behavior.
  - Fix plan: update policy to disclose local authorized reads and CSV export.

### Changes Made
- `PRIVACY_POLICY.md`: documented local Health Connect reads, goals refresh, cleanup, duplicate checks, and requested CSV export.

### Tests and Checks
- Static policy/code comparison: passed after update.

### Re-checks From Previous Reviews
- Health Connect permission docs and privacy policy now agree.

### Recommendations From This Review
- Update policy and Health Connect docs together for future permission changes.

## Review 6

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `docs/database.md`, `docs/testing.md`
- Diff/worktree reviewed: yes
- Checks planned: CSV import failure-path review

### Findings
- Database and Archive imports overwrote live CSVs before validation.
  - Severity: Major
  - Evidence: `App.kt` copied selected streams directly to `database.csv`/`archive.csv`, then parsed them.
  - Why it matters: malformed imports could destroy valid local data.
  - Fix plan: validate first in `AppCsvStore`, then replace through a temp file.

### Changes Made
- `AppCsvStore.kt`, `AppViewModelPersistenceActions.kt`, `AppViewModel.kt`, `App.kt`: added validated import entry points.
- `AppCsvStoreTest.kt`: added failed-validation-does-not-overwrite regression test.
- `strings.xml`: updated database/archive import warnings.

### Tests and Checks
- `./gradlew testDebugUnitTest --tests '*AppCsvStoreTest' --console=plain`: passed.

### Re-checks From Previous Reviews
- Goals CSV parser still passed after persistence refactor.

### Recommendations From This Review
- Add document-picker instrumentation coverage if legacy database/archive import becomes primary again.

## Review 7

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `docs/*.md`
- Diff/worktree reviewed: yes
- Checks planned: stale visible copy and docs scan

### Findings
- Stale visible wording remained.
  - Severity: Minor
  - Evidence: README referenced removed Recipes and old Health Connect limits; sanitizer fallback returned `Quick Import Food`; import dialogs described invalid overwrite behavior.
  - Why it matters: visible docs and generated names should match the current Add Meal workflow.
  - Fix plan: update README/copy/tests.

### Changes Made
- `README.md`, `QuickImportSanitizer.kt`, `QuickImportParserTest.kt`, `strings.xml`: updated stale wording.

### Tests and Checks
- Targeted `rg` stale-language scan: clean after updates.

### Re-checks From Previous Reviews
- Import warning text now matches validated import behavior.

### Recommendations From This Review
- Keep README fork-specific and current.

## Review 8

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/mcp.md`, `docs/api.md`, `docs/automation.md`
- Diff/worktree reviewed: yes
- Checks planned: MCP contract review

### Findings
- Settings-sheet MCP action lacked unit coverage.
  - Severity: Minor
  - Evidence: `openSettingsPanel()` existed and docs listed `theme`/`language`, but tests did not call it.
  - Why it matters: MCP/debug route drift has been recurring.
  - Fix plan: add MCP unit test.

### Changes Made
- `tools/mcc-mcp/src/test/tools.test.ts`: added settings panel bridge-post test.

### Tests and Checks
- `cd tools/mcc-mcp && npm test`: passed, 7 tests.

### Re-checks From Previous Reviews
- MCP docs and test coverage now align for the settings sheet action.

### Recommendations From This Review
- Add a route/tool registry snapshot if the debug bridge continues growing.

## Review 9

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/architecture-audit.md`, `docs/testing.md`
- Diff/worktree reviewed: yes
- Checks planned: line-limit scan

### Findings
- `App.kt` remains oversized.
  - Severity: Medium
  - Evidence: `wc -l` reports `App.kt` at 2022 lines.
  - Why it matters: UI launcher/dialog code remains expensive to review.
  - Fix plan: no broad extraction in this review; keep as follow-up because changed non-display files remain under cap.

### Changes Made
- None.

### Tests and Checks
- `wc -l` scan: changed non-display files remain under 300 lines.

### Re-checks From Previous Reviews
- `AutomationBootstrap.kt` remains below the 300-line cap after parser extraction.

### Recommendations From This Review
- Extract `App.kt` launchers/dialogs in a separate tested slice.

## Review 10

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/healthconnectintegration.md`, `docs/database.md`
- Diff/worktree reviewed: yes
- Checks planned: legacy Archive Health Connect sync review

### Findings
- Legacy Archive full sync can still delete Nutrition by day range before writing daily totals.
  - Severity: Medium
  - Evidence: `HealthConnectArchiveSyncService` deletes `NutritionRecord` by time range.
  - Why it matters: risky if used on days with per-food Add Meal records.
  - Fix plan: leave unchanged because it is legacy/manual and documented; recommend future gating/hiding.

### Changes Made
- None.

### Tests and Checks
- Static review of `HealthConnectArchiveSyncService.kt`: risk confirmed.

### Re-checks From Previous Reviews
- Nutrition pagination now protects read/list/delete paths that use `HealthConnectNutritionService`.

### Recommendations From This Review
- Hide or rework legacy Archive full sync if Add Meal remains the only intended workflow.

## Review 11

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/testing.md`, `docs/android-test-bench.md`
- Diff/worktree reviewed: yes
- Checks planned: focused regression rerun

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- `./gradlew testDebugUnitTest --tests '*AppCsvStoreTest' --tests '*AutomationHttpTest' --tests '*HealthConnectPagingTest' --console=plain`: passed.

### Re-checks From Previous Reviews
- CSV import atomicity, automation UTF-8 parsing, and Health Connect paging focused tests passed.

### Recommendations From This Review
- Keep focused tests near low-level helpers for fast regression feedback.

## Review 12

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `docs/*.md`
- Diff/worktree reviewed: yes
- Checks planned: compiler warning review

### Findings
- Kotlin warnings surfaced redundant null fallback code.
  - Severity: Minor
  - Evidence: compiler warned Elvis operators always returned left operand in `App.kt`.
  - Why it matters: warning noise hides real issues.
  - Fix plan: remove redundant Elvis operators.

### Changes Made
- `App.kt`: removed stale Elvis operators around non-null Health Connect sync state.

### Tests and Checks
- `./gradlew testDebugUnitTest lintDebug --console=plain`: passed.

### Re-checks From Previous Reviews
- Broad unit/lint includes prior focused tests.

### Recommendations From This Review
- Treat new compiler warnings as review findings.

## Review 13

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/api.md`, `docs/mcp.md`, `docs/automation.md`
- Diff/worktree reviewed: yes
- Checks planned: docs/tool stale phrase scan

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- Static stale-language `rg` scan: clean.

### Re-checks From Previous Reviews
- Visible docs/tool titles use Add Meal where appropriate.

### Recommendations From This Review
- Keep internal `QuickImport` names stable for now to avoid rename-only churn.

## Review 14

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/healthconnectintegration.md`, `PRIVACY_POLICY.md`
- Diff/worktree reviewed: yes
- Checks planned: Health Connect export sensitivity review

### Findings
- Health Connect export remains broad and writes to Downloads.
  - Severity: Medium
  - Evidence: `HealthConnectExporter` exports configured readable record types and raw record text.
  - Why it matters: exported CSV is sensitive.
  - Fix plan: no behavior change because it matches the requested check-in workflow; docs/policy now disclose it.

### Changes Made
- None.

### Tests and Checks
- Static exporter/docs review: disclosure accurate.

### Re-checks From Previous Reviews
- Privacy policy now discloses requested CSV exports.

### Recommendations From This Review
- Add nutrition-only export mode if broader users ever use this fork.

## Review 15

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/database.md`
- Diff/worktree reviewed: yes
- Checks planned: persistence boundary review

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- Static review confirmed import validation happens before temp-file write/rename.

### Re-checks From Previous Reviews
- `App.kt` picker path no longer writes selected CSV streams directly.

### Recommendations From This Review
- Consider making `replaceCsvIfValid()` private after higher-level import tests are added.

## Review 16

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/mcp.md`
- Diff/worktree reviewed: yes
- Checks planned: MCP test rerun

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- `cd tools/mcc-mcp && npm test`: passed, 7 tests.

### Re-checks From Previous Reviews
- Settings panel MCP regression passes.

### Recommendations From This Review
- Keep MCP tests pure and use emulator smoke for bridge integration.

## Review 17

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/android-test-bench.md`, `docs/testing.md`
- Diff/worktree reviewed: yes
- Checks planned: APK build

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- `./gradlew assembleDebug assembleDebugAndroidTest --console=plain`: passed.

### Re-checks From Previous Reviews
- Debug and androidTest APKs compile.

### Recommendations From This Review
- Keep androidTest assembly in closeout checks.

## Review 18

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/android-test-bench.md`
- Diff/worktree reviewed: yes
- Checks planned: Fold emulator automation smoke

### Findings
- Emulator smoke could not complete because the Fold AVD disappeared from ADB after boot.
  - Severity: Environment blocker for connected verification
  - Evidence: `run-automation-smoke.sh` failed in `:app:installDebug` with `No connected devices`; direct `adb devices -l` became empty after `start-fold-emulator.sh` reported `emulator-5554`.
  - Why it matters: runtime smoke is expected for this app after debug automation changes.
  - Fix plan: record blocker; rely on green unit/lint/build/MCP checks for this pass.

### Changes Made
- None.

### Tests and Checks
- `scripts/android/start-fold-emulator.sh`: reported `emulator-5554`.
- `ANDROID_SERIAL=emulator-5554 scripts/android/run-automation-smoke.sh`: blocked by no connected devices.

### Re-checks From Previous Reviews
- Debug bridge parser remains unit-tested; runtime socket smoke blocked by environment.

### Recommendations From This Review
- Investigate AVD lifecycle/ADB visibility separately before relying on connected checks.

## Review 19

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/architecture-audit.md`, `docs/testing.md`
- Diff/worktree reviewed: yes
- Checks planned: subagent reconciliation

### Findings
- Subagent findings aligned with main review fixes.
  - Severity: Informational
  - Evidence: bug, spec, maintainability, and security/perf lanes flagged CSV import safety, pagination, privacy disclosure, logging, MCP/docs drift, and large files.
  - Why it matters: independent review confirmed highest-risk fixes.
  - Fix plan: keep remaining large-file and legacy archive-sync items as recommendations.

### Changes Made
- None.

### Tests and Checks
- Subagent findings reconciled against current diff.

### Re-checks From Previous Reviews
- Fixed findings are represented in code/docs/tests.

### Recommendations From This Review
- Continue using subagents for Health Connect and persistence changes.

## Review 20

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/api.md`, `docs/database.md`, `docs/healthconnectintegration.md`
- Diff/worktree reviewed: yes
- Checks planned: documentation consistency pass

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- Static docs/source comparison: API, MCP, database, testing, and Health Connect docs match implemented additions.

### Re-checks From Previous Reviews
- Permission-purpose drift resolved across docs and privacy policy.

### Recommendations From This Review
- Add a docs checklist to future Health Connect permission changes.

## Review 21

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/testing.md`
- Diff/worktree reviewed: yes
- Checks planned: whitespace/diff hygiene

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- `git diff --check`: passed.

### Re-checks From Previous Reviews
- All touched files have no whitespace errors.

### Recommendations From This Review
- Keep `git diff --check` in closeout checks.

## Review 22

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/database.md`
- Diff/worktree reviewed: yes
- Checks planned: ViewModel/persistence boundary review

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- Static review: UI delegates imports to ViewModel/persistence instead of writing files directly.

### Re-checks From Previous Reviews
- Import logic moved out of the document-picker callbacks.

### Recommendations From This Review
- Move document launchers out of `App.kt` in a future UI extraction.

## Review 23

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/healthconnectintegration.md`
- Diff/worktree reviewed: yes
- Checks planned: duplicate detection path review

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- Static review: historical import duplicate detection now sees all paginated records for each date.

### Re-checks From Previous Reviews
- `writeHistoricalMealFoods()`, cleanup, range delete, and Meals display share paginated `readNutritionRecords()`.

### Recommendations From This Review
- Add fake-client service tests if Health Connect interfaces become practical to mock.

## Review 24

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`
- Diff/worktree reviewed: yes
- Checks planned: README fork-state review

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- Static README scan: obvious stale Recipe and Health Connect contradictions removed.

### Re-checks From Previous Reviews
- Targeted stale-text scan is clean.

### Recommendations From This Review
- Consider a fuller README rewrite later; this pass fixed contradictions only.

## Review 25

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/testing.md`
- Diff/worktree reviewed: yes
- Checks planned: full local validation

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- `./gradlew testDebugUnitTest lintDebug --console=plain`: passed.
- `cd tools/mcc-mcp && npm test`: passed.
- `git diff --check`: passed.

### Re-checks From Previous Reviews
- Broad local validation covers all code/doc/test changes except connected emulator smoke.

### Recommendations From This Review
- Connected smoke remains the only validation gap.

## Review 26

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/architecture-audit.md`
- Diff/worktree reviewed: yes
- Checks planned: large-file follow-up review

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- `wc -l` on changed source files: changed non-display files remain under 300 lines.

### Re-checks From Previous Reviews
- `AutomationBootstrap.kt` is 296 lines after parser extraction.

### Recommendations From This Review
- Continue extracting logic from `App.kt` and large screens in separate tested slices.

## Review 27

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/api.md`, `docs/mcp.md`
- Diff/worktree reviewed: yes
- Checks planned: route/tool naming review

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- Static review: MCP route wrappers and debug endpoint names align.

### Re-checks From Previous Reviews
- Visible docs/tool titles use Add Meal; internal quick-import route names intentionally remain stable.

### Recommendations From This Review
- Avoid renaming debug endpoints unless MCP clients and docs migrate together.

## Review 28

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `docs/testing.md`, `docs/android-test-bench.md`
- Diff/worktree reviewed: yes
- Checks planned: build artifact review

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- `./gradlew assembleDebug assembleDebugAndroidTest --console=plain`: passed.

### Re-checks From Previous Reviews
- Debug and test APKs build.

### Recommendations From This Review
- Run connected Compose tests once emulator stability is restored.

## Review 29

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: all relevant docs listed in baseline
- Diff/worktree reviewed: yes
- Checks planned: final source and stale-text audit

### Findings
- No new blocking issue found.

### Changes Made
- None.

### Tests and Checks
- `git status --short --branch`: only intended review branch changes are dirty.
- Targeted `rg` stale-language scan: clean.

### Re-checks From Previous Reviews
- README/privacy/docs consistency is aligned.

### Recommendations From This Review
- Commit review branch only after final diff review.

## Review 30

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `/Users/cmiller/Documents/Projects/MinimalistCalorieCounter/AGENTS.md`
- Repo docs: `README.md`, `PRIVACY_POLICY.md`, `docs/*.md`
- Diff/worktree reviewed: yes
- Checks planned: final risk call

### Findings
- No new blocking issue found in final review.

### Changes Made
- None in final review.

### Tests and Checks
- `./gradlew testDebugUnitTest lintDebug --console=plain`: passed.
- `cd tools/mcc-mcp && npm test`: passed.
- `git diff --check`: passed.
- `./gradlew assembleDebug assembleDebugAndroidTest --console=plain`: passed.
- `ANDROID_SERIAL=emulator-5554 scripts/android/run-automation-smoke.sh`: blocked by disappearing emulator/no connected devices.

### Re-checks From Previous Reviews
- Major fixed issues:
  - Re-check result: Goals CSV parsing, Health Connect payload logging, debug HTTP parsing, Nutrition pagination, import validation, privacy docs, stale wording, and MCP settings coverage are addressed and locally tested.

### Recommendations From This Review
- Investigate the Fold AVD instability and rerun connected automation/Compose smoke.
- Decide whether legacy Archive Health Connect full sync should be hidden or changed to avoid deleting per-food Add Meal records.
- Continue extracting `App.kt` launcher/dialog logic and shared UI helpers in separate, tested slices.

### Remaining Risks
- Connected emulator smoke did not complete because the AVD exited or disappeared from ADB after boot.
- Legacy Archive full sync remains risky if used on dates that also have Add Meal per-food Health Connect records.
