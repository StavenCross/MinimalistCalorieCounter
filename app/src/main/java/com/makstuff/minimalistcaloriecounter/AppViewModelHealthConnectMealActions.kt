package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.health.HealthConnectDeleteResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionReadResult
import com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectResult
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AppViewModelHealthConnectMealActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    fun readMeals() {
        env.state.update { currentState ->
            currentState.copy(
                healthConnectViewerLoading = true,
                healthConnectViewerMessage = null,
            )
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.readNutritionMeals(env.uiState.healthConnectViewerDate)) {
                is HealthConnectNutritionReadResult.Success -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMeals = result.meals,
                            healthConnectViewerMessage = if (result.meals.isEmpty()) {
                                "No Health Connect nutrition records found for this app on this date."
                            } else {
                                null
                            },
                        )
                    }
                }
                HealthConnectNutritionReadResult.HealthConnectUnavailable -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMeals = emptyList(),
                            healthConnectViewerMessage = env.application.getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HealthConnectNutritionReadResult.PermissionsMissing -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMeals = emptyList(),
                            healthConnectViewerMessage = env.application.getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HealthConnectNutritionReadResult.Failed -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMeals = emptyList(),
                            healthConnectViewerMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun deleteMeal(recordId: String) {
        env.state.update { currentState ->
            currentState.copy(
                healthConnectViewerLoading = true,
                healthConnectViewerMessage = null,
            )
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.deleteNutritionMeal(recordId)) {
                HealthConnectDeleteResult.Success -> viewModel.readHealthConnectNutritionMeals()
                HealthConnectDeleteResult.HealthConnectUnavailable -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = env.application.getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HealthConnectDeleteResult.PermissionsMissing -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = env.application.getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HealthConnectDeleteResult.Failed -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun previewHistoricalImport(rows: List<List<String>>) {
        val preview = HistoricalMealImporter.parseCsv(rows)
        env.state.update {
            it.copy(
                historicalMealImportPreview = preview,
                historicalMealImportMessage = "Preview: ${preview.validRows} foods, ${preview.mealCount} meals, ${preview.skippedRows} skipped rows.",
            )
        }
    }

    fun writeHistoricalImport() {
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
                is HistoricalMealHealthConnectResult.Success -> {
                    env.state.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = "Historical import complete: ${result.written} written, ${result.skippedDuplicates} duplicates skipped.",
                        )
                    }
                    viewModel.readHealthConnectNutritionMeals()
                }
                HistoricalMealHealthConnectResult.HealthConnectUnavailable -> {
                    env.state.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = env.application.getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HistoricalMealHealthConnectResult.PermissionsMissing -> {
                    env.state.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = env.application.getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HistoricalMealHealthConnectResult.Failed -> {
                    env.state.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun cleanupHistoricalImport() {
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
                is HistoricalMealHealthConnectResult.Success -> {
                    env.state.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            historicalMealImportMessage = "Cleanup complete: ${result.deleted} Health Connect records removed.",
                        )
                    }
                    viewModel.readHealthConnectNutritionMeals()
                }
                HistoricalMealHealthConnectResult.HealthConnectUnavailable -> {
                    env.state.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            historicalMealImportMessage = env.application.getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HistoricalMealHealthConnectResult.PermissionsMissing -> {
                    env.state.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            historicalMealImportMessage = env.application.getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HistoricalMealHealthConnectResult.Failed -> {
                    env.state.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            historicalMealImportMessage = result.message,
                        )
                    }
                }
            }
        }
    }
}
