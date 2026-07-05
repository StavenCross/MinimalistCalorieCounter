package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupPreview
import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupPreviewResult
import com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectResult
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

internal class AppViewModelHealthConnectCleanupActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    fun updateStartDate(date: LocalDate) = env.state.update {
        it.copy(
            healthConnectNutritionCleanupStartDate = date,
            healthConnectNutritionCleanupPreview = it.healthConnectNutritionCleanupPreview.keepIfMatches(
                startDate = date,
                endDate = it.healthConnectNutritionCleanupEndDate,
                mode = it.healthConnectNutritionCleanupMode,
            ),
            historicalMealImportMessage = null,
        )
    }

    fun updateEndDate(date: LocalDate) = env.state.update {
        it.copy(
            healthConnectNutritionCleanupEndDate = date,
            healthConnectNutritionCleanupPreview = it.healthConnectNutritionCleanupPreview.keepIfMatches(
                startDate = it.healthConnectNutritionCleanupStartDate,
                endDate = date,
                mode = it.healthConnectNutritionCleanupMode,
            ),
            historicalMealImportMessage = null,
        )
    }

    fun updateMode(mode: HealthConnectCleanupMode) = env.state.update {
        it.copy(
            healthConnectNutritionCleanupMode = mode,
            healthConnectNutritionCleanupPreview = it.healthConnectNutritionCleanupPreview.keepIfMatches(
                startDate = it.healthConnectNutritionCleanupStartDate,
                endDate = it.healthConnectNutritionCleanupEndDate,
                mode = mode,
            ),
            historicalMealImportMessage = null,
        )
    }

    fun previewRange() {
        val state = env.uiState
        env.state.update {
            it.copy(
                historicalMealImportInProgress = true,
                historicalMealImportMessage = "Scanning Health Connect records.",
                healthConnectNutritionCleanupPreview = null,
                healthConnectSyncProgress = 0f,
                healthConnectSyncCurrentCount = 0,
                healthConnectSyncTotalCount = 0,
            )
        }
        env.scope.launch {
            val result = env.healthConnectManager.previewNutritionRecordsInRange(
                startDate = state.healthConnectNutritionCleanupStartDate,
                endDate = state.healthConnectNutritionCleanupEndDate,
                mode = state.healthConnectNutritionCleanupMode,
                onProgress = env::updateHealthConnectProgress,
            )
            handlePreviewResult(state, result)
        }
    }

    fun removeRange() {
        val state = env.uiState
        val preview = state.healthConnectNutritionCleanupPreview
        if (preview == null) {
            env.state.update { it.copy(historicalMealImportMessage = "Preview Health Connect records before removing.") }
            return
        }
        if (!preview.matches(state.healthConnectNutritionCleanupStartDate, state.healthConnectNutritionCleanupEndDate, state.healthConnectNutritionCleanupMode)) {
            env.state.update { it.copy(historicalMealImportMessage = "Preview the selected Health Connect range before removing.") }
            return
        }
        if (preview.total == 0) {
            env.state.update { it.copy(historicalMealImportMessage = "No matching Health Connect meal/nutrition records to remove.") }
            return
        }
        val startedAt = LocalDateTime.now()
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
            val result = env.healthConnectManager.deleteNutritionRecordsInRange(
                startDate = state.healthConnectNutritionCleanupStartDate,
                endDate = state.healthConnectNutritionCleanupEndDate,
                mode = state.healthConnectNutritionCleanupMode,
                onProgress = env::updateHealthConnectProgress,
            )
            env.recordHealthConnectDeleteJob(state.healthConnectNutritionCleanupStartDate, state.healthConnectNutritionCleanupEndDate, state.healthConnectNutritionCleanupMode, result, startedAt)
            handleDeleteResult(result)
        }
    }

    private fun handlePreviewResult(state: AppUiState, result: HealthConnectCleanupPreviewResult) {
        when (result) {
            is HealthConnectCleanupPreviewResult.Success -> {
                val preview = result.preview.copy(
                    startDate = state.healthConnectNutritionCleanupStartDate,
                    endDate = state.healthConnectNutritionCleanupEndDate,
                    mode = state.healthConnectNutritionCleanupMode,
                )
                env.state.update {
                    it.copy(
                        historicalMealImportInProgress = false,
                        healthConnectSyncProgress = null,
                        healthConnectNutritionCleanupPreview = preview,
                        historicalMealImportMessage = "Preview found ${preview.total} matching records.",
                    )
                }
            }
            HealthConnectCleanupPreviewResult.HealthConnectUnavailable -> env.finishCleanup(env.application.getString(R.string.toast_hc_not_available))
            HealthConnectCleanupPreviewResult.PermissionsMissing -> env.finishCleanup(env.application.getString(R.string.health_connect_permissions_missing))
            is HealthConnectCleanupPreviewResult.Failed -> env.finishCleanup(result.message)
        }
    }

    private fun handleDeleteResult(result: HistoricalMealHealthConnectResult) {
        when (result) {
            is HistoricalMealHealthConnectResult.Success -> {
                env.state.update {
                    it.copy(
                        historicalMealImportInProgress = false,
                        healthConnectSyncProgress = null,
                        healthConnectNutritionCleanupPreview = null,
                        historicalMealImportMessage = "Removed ${result.deleted} Health Connect meal/nutrition records.",
                    )
                }
                viewModel.readHealthConnectNutritionMeals()
            }
            HistoricalMealHealthConnectResult.HealthConnectUnavailable -> env.finishCleanup(env.application.getString(R.string.toast_hc_not_available))
            HistoricalMealHealthConnectResult.PermissionsMissing -> env.finishCleanup(env.application.getString(R.string.health_connect_permissions_missing))
            is HistoricalMealHealthConnectResult.Failed -> env.finishCleanup(result.message)
        }
    }
}

private fun HealthConnectCleanupPreview?.keepIfMatches(
    startDate: LocalDate,
    endDate: LocalDate,
    mode: HealthConnectCleanupMode,
): HealthConnectCleanupPreview? = takeIf { it?.matches(startDate, endDate, mode) == true }

private fun HealthConnectCleanupPreview.matches(
    startDate: LocalDate,
    endDate: LocalDate,
    mode: HealthConnectCleanupMode,
): Boolean = this.startDate == startDate && this.endDate == endDate && this.mode == mode

private fun AppViewModelEnvironment.updateHealthConnectProgress(progress: Float?, current: Int, total: Int) {
    state.update {
        it.copy(
            healthConnectSyncProgress = progress,
            healthConnectSyncCurrentCount = current,
            healthConnectSyncTotalCount = total,
        )
    }
}

private fun AppViewModelEnvironment.finishCleanup(message: String) {
    state.update {
        it.copy(
            historicalMealImportInProgress = false,
            healthConnectSyncProgress = null,
            historicalMealImportMessage = message,
        )
    }
}
