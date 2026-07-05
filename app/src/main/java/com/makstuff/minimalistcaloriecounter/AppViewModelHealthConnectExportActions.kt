package com.makstuff.minimalistcaloriecounter
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
internal class AppViewModelHealthConnectExportActions(private val env: AppViewModelEnvironment, private val viewModel: AppViewModel) {
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
        env.state.update {
            it.copy(
                healthConnectExportMode = mode,
                healthConnectExportMessage = null,
            )
        }
        env.scope.launch {
            val exportGranted = env.healthConnectManager.hasExportReadPermissions(mode)
            env.state.update {
                if (it.healthConnectExportMode == mode) it.copy(healthConnectExportPermissionsGranted = exportGranted) else it
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
        val startedAt = java.time.LocalDateTime.now()
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
            val result = env.healthConnectManager.exportHealthConnectCsv(
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
            )
            env.recordHealthConnectExportJob(state.healthConnectExportStartDate, state.healthConnectExportEndDate, state.healthConnectExportMode, state.healthConnectExportRedacted, result, startedAt)
            when (result) {
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
}
