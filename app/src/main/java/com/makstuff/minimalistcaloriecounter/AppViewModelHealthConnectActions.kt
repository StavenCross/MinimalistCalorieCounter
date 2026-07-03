package com.makstuff.minimalistcaloriecounter

import android.widget.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

internal class AppViewModelHealthConnectActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    private var syncJob: Job? = null

    fun updatePermissionsStatus() {
        env.scope.launch {
            val allGranted = env.healthConnectManager.hasAllPermissions()
            val exportGranted = env.healthConnectManager.hasExportReadPermissions()
            val anyGranted = env.healthConnectManager.hasAnyPermissions()
            var needsSave = false
            env.state.update { currentState ->
                val newSyncStatus = if (!allGranted) false else currentState.healthConnectSyncEnabled
                if (newSyncStatus != currentState.healthConnectSyncEnabled) {
                    needsSave = true
                }
                val newToastsStatus = if (!allGranted) false else currentState.healthConnectToastsEnabled
                if (newToastsStatus != currentState.healthConnectToastsEnabled) {
                    needsSave = true
                }
                currentState.copy(
                    healthConnectPermissionsGranted = allGranted,
                    healthConnectExportPermissionsGranted = exportGranted,
                    healthConnectAnyPermissionsGranted = anyGranted,
                    healthConnectSyncEnabled = newSyncStatus,
                    healthConnectToastsEnabled = newToastsStatus,
                )
            }
            if (needsSave) {
                viewModel.optionsWriteToFile(env.context)
            }
        }
    }

    fun toggleSyncEnabled() {
        env.state.update { currentState ->
            val newState = !currentState.healthConnectSyncEnabled
            if (newState) {
                env.scope.launch { viewModel.setAlertDialogHealthConnectActivation(bool = true) }
            }
            currentState.copy(healthConnectSyncEnabled = newState)
        }
        viewModel.optionsWriteToFile(env.context)
    }

    fun toggleToastsEnabled() {
        env.state.update { currentState ->
            val newState = !currentState.healthConnectToastsEnabled
            if (newState) {
                env.scope.launch { viewModel.setAlertDialogHealthConnectToasts(bool = true) }
            }
            currentState.copy(healthConnectToastsEnabled = newState)
        }
        viewModel.optionsWriteToFile(env.context)
    }

    fun syncArchive() {
        syncJob?.cancel()
        syncJob = env.scope.launch {
            if (env.healthConnectManager.hasAllPermissions()) {
                env.healthConnectManager.syncArchive(
                    archive = env.uiState.archive,
                    onProgress = { progress, current, total ->
                        env.state.update {
                            it.copy(
                                healthConnectSyncProgress = progress,
                                healthConnectSyncCurrentCount = current,
                                healthConnectSyncTotalCount = total,
                            )
                        }
                    },
                    onError = { error ->
                        env.state.update { it.copy(healthConnectSyncMessage = error) }
                    },
                )
            } else {
                Toast.makeText(
                    env.context,
                    env.application.getString(R.string.health_connect_permissions_missing),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        env.state.update { it.copy(healthConnectSyncProgress = null, healthConnectSyncMessage = null) }
    }

    fun finishSync() {
        env.state.update { it.copy(healthConnectSyncProgress = null) }
    }

    fun dismissSyncError() {
        env.state.update { it.copy(healthConnectSyncProgress = null, healthConnectSyncMessage = null) }
    }

    fun updateViewerDate(date: LocalDate) {
        env.state.update { currentState ->
            currentState.copy(
                healthConnectViewerDate = date,
                healthConnectViewerMessage = null,
            )
        }
        viewModel.readHealthConnectNutritionMeals()
    }
}
