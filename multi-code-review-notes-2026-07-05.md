# Multi Code Review Notes - 2026-07-05

Scope: all `master` changes from the last two days, using base `ce39f9309b61a8cc92660d97ebc2e0051eae71b7` through current `HEAD`.

Available tools and skills:
- Skill: `/Users/cmiller/.codex/skills/multi-code-review/SKILL.md`.
- Repo instructions: `AGENTS.md`.
- Docs: `README.md`, `docs/testing.md`, `docs/healthconnectintegration.md`, `docs/database.md`, `docs/architecture-audit.md`, `docs/api.md`, `docs/mcp.md`, `app_refactor_and_expansion.md`.
- Checks: Gradle compile/unit/lint/connected tests, MCP npm tests, `git diff --check`, `rg`, line-count scans, subagent sidecar reviews.

Original intent reviewed each pass: Add Meal food-level Health Connect writes, editable parsed meals, Meals review/edit/delete/export, Goals profile/target guidance, Health Connect export/delete safety, Room/local backup reliability, debug automation/MCP control, docs, tests, and maintainability.

## Review 1

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: README and `docs/*.md`
- Diff/worktree reviewed: yes
- Checks planned: full compile/unit, static scans, line-count scan

### Findings
- Add Meal formatter used default locale.
  - Severity: Major
  - Evidence: `QuickImportFormatter.format`
  - Why it matters: comma-decimal locales could rewrite parsed blurbs into text the parser rejects.
  - Fix plan: force `Locale.US`, add locale regression.
- Add Meal edited amount did not update grams.
  - Severity: Major
  - Evidence: `QuickImportFoodDetailSheet` only changed `amountText`.
  - Why it matters: local DB per-100g values could be wrong after amount edits.
  - Fix plan: share amount parser and update `grams`.
- Non-UI line cap drift.
  - Severity: Minor
  - Evidence: `AppViewModel.kt`, `GoalCalculatorTest.kt`.
  - Why it matters: violates repo maintainability rules.
  - Fix plan: compact facade and split tests.

### Changes Made
- Added `QuickImportAmountParser`; updated parser/edit drawer.
- Forced formatter to `Locale.US`.
- Split goal status and meal allocation tests.

### Tests and Checks
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest --console=plain`
  - Result: passed before fixes, later rerun after fixes.
- `git diff --check`
  - Result: passed at this stage.

### Re-checks From Previous Reviews
- None.

### Recommendations From This Review
- Keep parser/formatter/edit tests together whenever Add Meal blurb shape changes.

## Review 2

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: README and Health Connect/database/testing docs
- Diff/worktree reviewed: yes
- Checks planned: Health Connect sidecar plus local service review

### Findings
- Meal edit delete-before-insert could lose records.
  - Severity: Major
  - Evidence: `HealthConnectNutritionService.replaceNutritionServings`
  - Why it matters: insert failure after delete would erase the original food group.
  - Fix plan: insert replacements before deleting originals and add operation-order test.

### Changes Made
- Added `HealthConnectReplaceOrder`.
- Changed replace operation to insert first, delete second.
- Added `HealthConnectReplaceOrderTest`.

### Tests and Checks
- Compile/unit rerun after the fix.
  - Result: passed.

### Re-checks From Previous Reviews
- Formatter and amount parser remained covered.

### Recommendations From This Review
- Future enhancement: prefer true Health Connect update semantics if the API provides a safe app-owned update path.

## Review 3

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: Health Connect and automation docs
- Diff/worktree reviewed: yes
- Checks planned: cleanup preview/delete path review

### Findings
- Automation cleanup delete could clear its preview and no-op while reporting started.
  - Severity: Major
  - Evidence: cleanup setters cleared preview before `removeNutritionRange`.
  - Why it matters: MCP/debug delete could appear to run without deleting.
  - Fix plan: store preview criteria and require delete criteria to match.

### Changes Made
- Added `startDate`, `endDate`, and `mode` to `HealthConnectCleanupPreview`.
- Added criteria checks before delete.
- Moved cleanup behavior into `AppViewModelHealthConnectCleanupActions`.

### Tests and Checks
- Compile/unit and connected tests.
  - Result: passed.

### Re-checks From Previous Reviews
- Replace order still insert-first after cleanup split.

### Recommendations From This Review
- Add a fake-manager ViewModel test harness later for direct cleanup preview/delete state assertions.

## Review 4

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: automation/MCP docs
- Diff/worktree reviewed: yes
- Checks planned: export option ordering review

### Findings
- Export mode update was async before export snapshot.
  - Severity: Major
  - Evidence: `updateExportMode` launched a coroutine, `exportRange` immediately read state.
  - Why it matters: MCP export could use stale mode/redaction-adjacent state.
  - Fix plan: update selected mode synchronously; refresh permission status asynchronously.

### Changes Made
- `updateExportMode` now writes mode immediately and only permission status asynchronously.

### Tests and Checks
- MCP tests include export-mode request coverage.
  - Result: passed.

### Re-checks From Previous Reviews
- Cleanup delete path still requires matching preview.

### Recommendations From This Review
- Keep automation endpoint mutations synchronous when the next line consumes state.

## Review 5

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: Health Connect docs and manifest
- Diff/worktree reviewed: yes
- Checks planned: permission/manifest lockstep review

### Findings
- Full export included cadence records without manifest declarations.
  - Severity: Major
  - Evidence: `HealthConnectExportRecords.kt`, `AndroidManifest.xml`.
  - Why it matters: permissions could be impossible to grant.
  - Fix plan: add cadence permissions and manifest lockstep test.
- Manual goal profile write lacked normal permission path.
  - Severity: Major
  - Evidence: `HealthConnectManager.permissions`.
  - Why it matters: manual height/weight write-back could never run after normal onboarding.
  - Fix plan: declare/request height/weight write permissions.

### Changes Made
- Added `WRITE_HEIGHT`, `READ_CYCLING_PEDALING_CADENCE`, and `READ_STEPS_CADENCE`.
- Included goal-profile write permissions in normal Health Connect permission set.
- Added `HealthConnectManifestPermissionsTest`.

### Tests and Checks
- Manifest/permission unit tests.
  - Result: passed.

### Re-checks From Previous Reviews
- Full unit suite passed after permission changes.

### Recommendations From This Review
- Any new export record type must land with manifest and docs updates.

## Review 6

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: database/backup docs
- Diff/worktree reviewed: yes
- Checks planned: Android backup rules review

### Findings
- Legacy outbox CSV was missing from backup rules.
  - Severity: Minor
  - Evidence: `backup_rules.xml`, `data_extraction_rules.xml`.
  - Why it matters: debug reinstall/restore could lose mirrored outbox state.
  - Fix plan: include `quick_import_outbox.csv` and add static XML test.

### Changes Made
- Added outbox CSV to backup and data extraction rules.
- Added `BackupRulesTest`.
- Updated `docs/database.md`.

### Tests and Checks
- `BackupRulesTest`.
  - Result: passed.

### Re-checks From Previous Reviews
- Manifest tests still passed with backup tests.

### Recommendations From This Review
- Treat backup XML as a persistence contract.

## Review 7

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: Add Meal/parser docs in README and Health Connect doc
- Diff/worktree reviewed: yes
- Checks planned: parser edge-case review

### Findings
- Record splitting assumed `Calories` came first.
  - Severity: Minor
  - Evidence: `QuickImportParser.splitRecords`
  - Why it matters: ChatGPT can emit macros first; subsequent records could merge.
  - Fix plan: split on any recognized nutrient label and test protein-first records.

### Changes Made
- Broadened split lookahead.
- Added `splitsRecordsWhenCaloriesAreNotTheFirstNutrient`.

### Tests and Checks
- Focused QuickImport tests.
  - Result: passed.

### Re-checks From Previous Reviews
- Locale formatter and edited-grams tests passed.

### Recommendations From This Review
- Keep parser permissive for order, strict for required fields.

## Review 8

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: Goals docs/spec
- Diff/worktree reviewed: yes
- Checks planned: Goals message/state review

### Findings
- Complete profile with no recommendation showed incomplete-profile copy.
  - Severity: Minor
  - Evidence: `AppViewModelGoalsActions.recalculateRecommendation`
  - Why it matters: Goals could tell the user to complete fields that are already complete.
  - Fix plan: show up-to-date copy when no meaningful recommendation exists.

### Changes Made
- Updated recalculation and Health Connect refresh messages for complete/no-change state.

### Tests and Checks
- Goal-focused unit tests.
  - Result: passed.

### Re-checks From Previous Reviews
- Goal status tests remained green after split.

### Recommendations From This Review
- Add ViewModel-level action tests when a fake environment is available.

## Review 9

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: architecture audit
- Diff/worktree reviewed: yes
- Checks planned: line-count and file-boundary scan

### Findings
- Architecture audit had stale line counts.
  - Severity: Minor
  - Evidence: `docs/architecture-audit.md`
  - Why it matters: cleanup decisions depend on honest file-size inventory.
  - Fix plan: refresh current oversized file table and note new cleanup split.

### Changes Made
- Updated architecture audit line counts and non-UI split notes.

### Tests and Checks
- Line-count scan.
  - Result: only UI/component/test files remain above cap.

### Re-checks From Previous Reviews
- `AppViewModel.kt`, `GoalCalculatorTest.kt`, and export actions are under cap.

### Recommendations From This Review
- Future UI-only split candidate: `GoalsDashboardComponents.kt`.

## Review 10

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: docs/testing.md
- Diff/worktree reviewed: yes
- Checks planned: broad JVM gate

### Findings
- Missing `assertTrue` import in new QuickImport test.
  - Severity: Major
  - Evidence: sidecar and compiler output for `QuickImportParserTest`.
  - Why it matters: unit test gate did not compile.
  - Fix plan: add import and rerun full unit compile.

### Changes Made
- Added `org.junit.Assert.assertTrue` import.

### Tests and Checks
- Full compile/unit.
  - Result: passed.

### Re-checks From Previous Reviews
- Parser and formatter tests compile and pass.

### Recommendations From This Review
- Keep compile running after every test edit batch.

## Review 11

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: README, testing, Health Connect docs
- Diff/worktree reviewed: yes
- Checks planned: sidecar finding reconciliation

### Findings
- Add Meal/Meals sidecar found invalid edit feedback gap.
  - Severity: Minor
  - Evidence: `HealthNutritionMealComponents` disables save on invalid values.
  - Why it matters: UX lacks inline explanation.
  - Fix plan: record as follow-up; current scope focused on correctness and safety.

### Changes Made
- No code change for this UX follow-up.

### Tests and Checks
- Connected Meals edit tests.
  - Result: passed.

### Re-checks From Previous Reviews
- Meal edit replacement safety fix rechecked by tests and source.

### Recommendations From This Review
- Add inline validation text to Meals edit drawer in a UI-focused pass.

## Review 12

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: persistence/database docs
- Diff/worktree reviewed: yes
- Checks planned: local backup reliability review

### Findings
- Local meal backup Room failure is swallowed.
  - Severity: Minor
  - Evidence: `AppViewModelQuickImportHelpers.writeLocalMealBackup`
  - Why it matters: backup guarantee is weaker if Room fails before Health Connect write.
  - Fix plan: record as follow-up; changing commit semantics needs a deliberate UX/error-state design.

### Changes Made
- No code change in this review.

### Tests and Checks
- Full unit and connected Add Meal tests.
  - Result: passed.

### Re-checks From Previous Reviews
- Backup XML coverage fixed separately.

### Recommendations From This Review
- Design a backup-failure state before blocking Health Connect writes on backup exceptions.

## Review 13

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: Health Connect export docs
- Diff/worktree reviewed: yes
- Checks planned: privacy/security scan

### Findings
- Full export remains intentionally broad.
  - Severity: Minor
  - Evidence: `HealthConnectExportRecords`, docs/export copy.
  - Why it matters: raw export can contain sensitive health data.
  - Fix plan: verify redaction default and docs; no change needed.

### Changes Made
- Health Connect docs updated with current permissions and cleanup safety.

### Tests and Checks
- `rg` for secrets/password/API key strings.
  - Result: no production secrets found.

### Re-checks From Previous Reviews
- Manifest/export permission lockstep test passed.

### Recommendations From This Review
- Keep redacted export default on.

## Review 14

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: MCP docs and tools tests
- Diff/worktree reviewed: yes
- Checks planned: MCP automation review

### Findings
- MCP health cleanup/export tools needed verification after state-order fixes.
  - Severity: Minor
  - Evidence: `tools/mcc-mcp/src/test/tools.test.ts`
  - Why it matters: local automation is part of the development/test workflow.
  - Fix plan: run MCP test suite.

### Changes Made
- No MCP code changes in this review.

### Tests and Checks
- `npm test` in `tools/mcc-mcp`.
  - Result: passed, 9 tests.

### Re-checks From Previous Reviews
- Export mode and cleanup request shape remain covered by MCP tests.

### Recommendations From This Review
- Add bridge-level response assertions for preview-required delete later.

## Review 15

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: Android test bench/testing docs
- Diff/worktree reviewed: yes
- Checks planned: connected emulator review

### Findings
- No new blocking issues found.
  - Severity: None
  - Evidence: connected Add Meal/Meals/Goals classes passed.
  - Why it matters: recent changes touched core screen workflows.
  - Fix plan: none.

### Changes Made
- No code changes.

### Tests and Checks
- `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest ...`
  - Result: passed, 36/36 tests.

### Re-checks From Previous Reviews
- Add Meal edit, Meals edit/quantity, and Goals tests passed.

### Recommendations From This Review
- Keep focused connected class list as the fast regression gate for this app.

## Review 16

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: docs/testing.md
- Diff/worktree reviewed: yes
- Checks planned: lint review

### Findings
- No lint regressions found.
  - Severity: None
  - Evidence: `lintDebug` report.
  - Why it matters: manifest/permission and Compose changes can surface lint issues.
  - Fix plan: none.

### Changes Made
- No code changes.

### Tests and Checks
- `./gradlew :app:lintDebug --console=plain`
  - Result: passed.

### Re-checks From Previous Reviews
- Manifest changes lint-clean.

### Recommendations From This Review
- Continue running lint after manifest permission edits.

## Review 17

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: architecture/database/Health Connect docs
- Diff/worktree reviewed: yes
- Checks planned: docs consistency review

### Findings
- Docs needed updates for write-height, cleanup criteria, backup CSV, and line counts.
  - Severity: Minor
  - Evidence: `docs/healthconnectintegration.md`, `docs/database.md`, `docs/architecture-audit.md`.
  - Why it matters: docs are operational source material for future app work.
  - Fix plan: patch docs.

### Changes Made
- Updated all three docs.

### Tests and Checks
- Docs reviewed manually against code and tests.
  - Result: aligned.

### Re-checks From Previous Reviews
- Backup and manifest tests enforce the most drift-prone pieces.

### Recommendations From This Review
- Add docs snapshot tooling later only if docs drift becomes frequent.

## Review 18

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: all relevant docs
- Diff/worktree reviewed: yes
- Checks planned: final static scan

### Findings
- Static scan only found expected `!!` assertions in tests.
  - Severity: None
  - Evidence: `rg` output limited to test assertions.
  - Why it matters: no obvious production TODO/secret/unsafe assertion leftovers.
  - Fix plan: none.

### Changes Made
- No code changes.

### Tests and Checks
- `rg -n "TODO|FIXME|println\\(|apiKey|password|clientSecret|secret|!!" ...`
  - Result: no production findings requiring action.

### Re-checks From Previous Reviews
- No new production hot spots introduced by review fixes.

### Recommendations From This Review
- Test `!!` cleanup is optional and not worth churn in this pass.

## Review 19

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: all relevant docs
- Diff/worktree reviewed: yes
- Checks planned: whitespace/status review

### Findings
- Existing notes file was initially overwritten despite skill requiring timestamped notes when default exists.
  - Severity: Minor
  - Evidence: `git diff multi-code-review-notes.md`.
  - Why it matters: prior review history should not be erased.
  - Fix plan: restore original file and create timestamped notes.

### Changes Made
- Restored `multi-code-review-notes.md` from `HEAD`.
- Created `multi-code-review-notes-2026-07-05.md`.

### Tests and Checks
- `git diff --check`
  - Result: passed after restore.

### Re-checks From Previous Reviews
- Notes now comply with the skill file naming rule.

### Recommendations From This Review
- Always check for existing notes before creating review artifacts.

## Review 20

### Re-ingested Context
- Skill instructions: yes
- Review notes: yes
- AGENTS.md files: `AGENTS.md`
- Repo docs: all relevant docs
- Diff/worktree reviewed: yes
- Checks planned: final comprehensive pass over fixes and checks

### Findings
- No new issues found in final review.
  - Severity: None
  - Evidence: green compile/unit/lint/MCP/connected checks and clean whitespace diff.
  - Why it matters: final review did not surface fresh defects requiring another review round.
  - Fix plan: none.

### Changes Made
- No additional code changes.

### Tests and Checks
- Full compile/unit: passed.
- Focused JVM: passed.
- Lint: passed.
- MCP tests: passed.
- Connected emulator tests: passed 36/36.
- `git diff --check`: passed.

### Re-checks From Previous Reviews
- Rechecked all fixed issues: formatter locale, amount grams, insert-before-delete, cleanup preview criteria, export mode state, manifest permissions, backup rules, parser split, Goals up-to-date message, line caps, docs, and notes-file compliance.

### Recommendations From This Review
- Ready with minor follow-ups: add Meals edit inline validation and deliberate backup-failure UX/state design.

