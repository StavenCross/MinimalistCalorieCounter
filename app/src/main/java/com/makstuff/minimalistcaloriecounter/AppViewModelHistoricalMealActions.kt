package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectResult
import com.makstuff.minimalistcaloriecounter.widget.NutritionWidgetUpdater
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns CSV historical-meal preview, write, and cleanup orchestration.
 *
 * Keeping batch-import state separate from interactive meal actions makes permission and progress
 * behavior easier to review without changing the public `AppViewModel` entry points.
 */
internal class AppViewModelHistoricalMealActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    /** Parses imported rows into a reviewable preview without writing Health Connect data. */
    fun preview(rows: List<List<String>>) {
        val preview = HistoricalMealImporter.parseCsv(rows)
        env.state.update {
            it.copy(
                historicalMealImportPreview = preview,
                historicalMealImportMessage = "Preview: ${preview.validRows} foods, ${preview.mealCount} meals, ${preview.skippedRows} skipped rows.",
            )
        }
    }

    /** Writes the accepted preview while publishing durable progress and a terminal result. */
    fun write() {
        val preview = env.uiState.historicalMealImportPreview ?: return
        if (preview.foods.isEmpty()) {
            env.state.update { it.copy(historicalMealImportMessage = "No valid historical foods to write.") }
            return
        }
        env.state.update {
            it.copy(
                historicalMealImportInProgress = true,
                historicalMealImportMessage = "Writing historical meals to Health Connect.",
                healthConnectSyncProgress = 0f,
                healthConnectSyncCurrentCount = 0,
                healthConnectSyncTotalCount = preview.foods.size,
            )
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.writeHistoricalMealFoods(
                foods = preview.foods,
                onProgress = { progress, current, total ->
                    env.state.update {
                        it.copy(
                            healthConnectSyncProgress = progress,
                            healthConnectSyncCurrentCount = current,
                            healthConnectSyncTotalCount = total,
                        )
                    }
                },
            )) {
                is HistoricalMealHealthConnectResult.Success -> finishWrite(
                    message = "Historical import complete: ${result.written} written, ${result.skippedDuplicates} duplicates skipped.",
                    refreshMeals = true,
                )
                HistoricalMealHealthConnectResult.HealthConnectUnavailable -> finishWrite(
                    env.application.getString(R.string.toast_hc_not_available),
                )
                HistoricalMealHealthConnectResult.PermissionsMissing -> finishWrite(
                    env.application.getString(R.string.health_connect_permissions_missing),
                )
                is HistoricalMealHealthConnectResult.Failed -> finishWrite(result.message)
            }
        }
    }

    /** Removes only records associated with the currently previewed historical import dates. */
    fun cleanup() {
        val preview = env.uiState.historicalMealImportPreview ?: run {
            env.state.update { it.copy(historicalMealImportMessage = "Import a historical meal CSV first so cleanup knows the date range.") }
            return
        }
        env.state.update {
            it.copy(
                historicalMealImportInProgress = true,
                historicalMealImportMessage = "Removing historical import and legacy Daily Total rows.",
            )
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.cleanupHistoricalMealRecords(preview.dates)) {
                is HistoricalMealHealthConnectResult.Success -> finishCleanup(
                    message = "Cleanup complete: ${result.deleted} Health Connect records removed.",
                    refreshMeals = true,
                )
                HistoricalMealHealthConnectResult.HealthConnectUnavailable -> finishCleanup(
                    env.application.getString(R.string.toast_hc_not_available),
                )
                HistoricalMealHealthConnectResult.PermissionsMissing -> finishCleanup(
                    env.application.getString(R.string.health_connect_permissions_missing),
                )
                is HistoricalMealHealthConnectResult.Failed -> finishCleanup(result.message)
            }
        }
    }

    private suspend fun finishWrite(message: String, refreshMeals: Boolean = false) {
        env.state.update {
            it.copy(
                historicalMealImportInProgress = false,
                healthConnectSyncProgress = null,
                historicalMealImportMessage = message,
            )
        }
        if (refreshMeals) refreshMealsAndWidget()
    }

    private suspend fun finishCleanup(message: String, refreshMeals: Boolean = false) {
        env.state.update {
            it.copy(
                historicalMealImportInProgress = false,
                historicalMealImportMessage = message,
            )
        }
        if (refreshMeals) refreshMealsAndWidget()
    }

    private suspend fun refreshMealsAndWidget() {
        viewModel.readHealthConnectNutritionMeals()
        NutritionWidgetUpdater.updateAll(env.application)
    }
}
