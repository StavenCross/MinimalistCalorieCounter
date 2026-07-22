package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.permission.HealthPermission
import java.time.LocalDate

internal fun HealthConnectManager.exportPermissionsFor(mode: HealthConnectExportMode): Set<String> = mode.recordTypes()
    .map { HealthPermission.getReadPermission(it) }
    .toSet() + HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

internal suspend fun HealthConnectManager.hasExportReadPermissions(mode: HealthConnectExportMode = HealthConnectExportMode.Full): Boolean {
    val client = getClientOrNull() ?: return false
    return try {
        client.permissionController.getGrantedPermissions().containsAll(exportPermissionsFor(mode))
    } catch (_: Throwable) {
        false
    }
}

internal suspend fun HealthConnectManager.hasCheckInReadPermissions(): Boolean {
    val client = getClientOrNull() ?: return false
    return try {
        client.permissionController.getGrantedPermissions().containsAll(checkInReadPermissions)
    } catch (_: Throwable) {
        false
    }
}

internal suspend fun HealthConnectManager.exportHealthConnectCsv(
    startDate: LocalDate,
    endDate: LocalDate,
    mode: HealthConnectExportMode,
    redacted: Boolean,
    onProgress: (Float?, Int, Int) -> Unit,
): HealthConnectExportResult {
    if (!isSdkAvailable()) return HealthConnectExportResult.HealthConnectUnavailable
    val client = getClientOrNull() ?: return HealthConnectExportResult.HealthConnectUnavailable

    return try {
        if (!hasExportReadPermissions(mode)) return HealthConnectExportResult.PermissionsMissing
        HealthConnectExporter(applicationContext, client).exportCsv(startDate, endDate, mode, redacted, onProgress)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) {
        HealthConnectExportResult.Failed(e.message ?: "Unknown Health Connect export error")
    }
}

internal suspend fun HealthConnectManager.exportHealthConnectCheckInXlsx(
    range: CheckInDateRange,
    onProgress: (Float?, Int, Int) -> Unit,
): HealthConnectExportResult {
    if (!isSdkAvailable()) return HealthConnectExportResult.HealthConnectUnavailable
    val client = getClientOrNull() ?: return HealthConnectExportResult.HealthConnectUnavailable

    return try {
        if (!hasCheckInReadPermissions()) return HealthConnectExportResult.PermissionsMissing
        HealthConnectCheckInExporter(applicationContext, client).exportXlsx(range, onProgress)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) {
        HealthConnectExportResult.Failed(e.message ?: "Unknown check-in export error")
    }
}
