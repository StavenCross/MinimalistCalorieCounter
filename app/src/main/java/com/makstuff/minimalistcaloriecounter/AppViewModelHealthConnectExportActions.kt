package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectResult
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

internal class AppViewModelHealthConnectExportActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    fun updateCleanupStartDate(date: LocalDate) {
        env.state.update {
            it.copy(
                healthConnectNutritionCleanupStartDate = date,
                historicalMealImportMessage = null,
            )
        }
    }

    fun updateCleanupEndDate(date: LocalDate) {
        env.state.update {
            it.copy(
                healthConnectNutritionCleanupEndDate = date,
                historicalMealImportMessage = null,
            )
        }
    }

    fun updateExportStartDate(date: LocalDate) {
        env.state.update {
            it.copy(
                healthConnectExportStartDate = date,
                healthConnectExportMessage = null,
            )
        }
    }

    fun updateExportEndDate(date: LocalDate) {
        env.state.update {
            it.copy(
                healthConnectExportEndDate = date,
                healthConnectExportMessage = null,
            )
        }
    }

    fun updateExportMode(mode: HealthConnectExportMode) {
        env.scope.launch {
            val exportGranted = env.healthConnectManager.hasExportReadPermissions(mode)
            env.state.update {
                it.copy(
                    healthConnectExportMode = mode,
                    healthConnectExportPermissionsGranted = exportGranted,
                    healthConnectExportMessage = null,
                )
            }
        }
    }

    fun updateExportRedacted(redacted: Boolean) {
        env.state.update {
            it.copy(
                healthConnectExportRedacted = redacted,
                healthConnectExportMessage = null,
            )
        }
    }

    fun exportRange() {
        val state = env.uiState
        if (state.healthConnectExportInProgress) return
        env.state.update {
            it.copy(
                healthConnectExportInProgress = true,
                healthConnectExportMessage = "Exporting Health Connect records.",
                healthConnectSyncProgress = 0f,
                healthConnectSyncCurrentCount = 0,
                healthConnectSyncTotalCount = 0,
                healthConnectSyncMessage = "Exporting Health Connect records.",
            )
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.exportHealthConnectCsv(
                startDate = state.healthConnectExportStartDate,
                endDate = state.healthConnectExportEndDate,
                mode = state.healthConnectExportMode,
                redacted = state.healthConnectExportRedacted,
                onProgress = { progress, current, total ->
                    env.state.update {
                        it.copy(
                            healthConnectSyncProgress = progress,
                            healthConnectSyncCurrentCount = current,
                            healthConnectSyncTotalCount = total,
                            healthConnectSyncMessage = "Exporting Health Connect records: $current/$total types",
                        )
                    }
                },
            )) {
                is HealthConnectExportResult.Success -> {
                    env.state.update {
                        it.copy(
                            healthConnectExportInProgress = false,
                            healthConnectExportMessage = "Exported ${result.records} records to ${result.displayPath}.",
                            healthConnectSyncProgress = null,
                            healthConnectSyncMessage = null,
                        )
                    }
                }
                HealthConnectExportResult.HealthConnectUnavailable -> {
                    env.state.update {
                        it.copy(
                            healthConnectExportInProgress = false,
                            healthConnectExportMessage = "Health Connect is unavailable on this device.",
                            healthConnectSyncProgress = null,
                            healthConnectSyncMessage = null,
                        )
                    }
                }
                HealthConnectExportResult.PermissionsMissing -> {
                    env.state.update {
                        it.copy(
                            healthConnectExportInProgress = false,
                            healthConnectExportMessage = "Health Connect read permissions are missing.",
                            healthConnectSyncProgress = null,
                            healthConnectSyncMessage = null,
                        )
                    }
                }
                is HealthConnectExportResult.Failed -> {
                    env.state.update {
                        it.copy(
                            healthConnectExportInProgress = false,
                            healthConnectExportMessage = "Health Connect export failed: ${result.message}",
                            healthConnectSyncProgress = null,
                            healthConnectSyncMessage = null,
                        )
                    }
                }
            }
        }
    }

    fun removeNutritionRange() {
        val state = env.uiState
        env.state.update {
            it.copy(
                historicalMealImportInProgress = true,
                historicalMealImportMessage = "Removing Health Connect meals and nutrition.",
                healthConnectSyncProgress = 0f,
                healthConnectSyncCurrentCount = 0,
                healthConnectSyncTotalCount = 0,
            )
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.deleteNutritionRecordsInRange(
                startDate = state.healthConnectNutritionCleanupStartDate,
                endDate = state.healthConnectNutritionCleanupEndDate,
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
                            historicalMealImportMessage = "Removed ${result.deleted} Health Connect meal/nutrition records.",
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
}
