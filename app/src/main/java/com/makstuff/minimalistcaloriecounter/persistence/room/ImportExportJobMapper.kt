package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportResult
import com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectResult
import java.time.LocalDate
import java.time.LocalDateTime

object ImportExportJobMapper {
    fun healthConnectExport(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: HealthConnectExportMode,
        redacted: Boolean,
        result: HealthConnectExportResult,
        startedAt: LocalDateTime,
        finishedAt: LocalDateTime = LocalDateTime.now(),
    ): ImportExportJobEntity {
        return ImportExportJobEntity(
            id = id("export", startedAt, mode.name),
            type = "health_connect_export",
            mode = mode.name + if (redacted) "_redacted" else "",
            state = result.stateName(),
            startedAt = startedAt,
            finishedAt = finishedAt,
            dateStart = startDate,
            dateEnd = endDate,
            outputPath = (result as? HealthConnectExportResult.Success)?.displayPath,
            recordCount = (result as? HealthConnectExportResult.Success)?.records ?: 0,
            errorMessage = result.errorMessage(),
        )
    }

    fun nutritionDelete(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: HealthConnectCleanupMode,
        result: HistoricalMealHealthConnectResult,
        startedAt: LocalDateTime,
        finishedAt: LocalDateTime = LocalDateTime.now(),
    ): ImportExportJobEntity {
        return ImportExportJobEntity(
            id = id("delete", startedAt, mode.name),
            type = "health_connect_delete",
            mode = mode.name,
            state = result.stateName(),
            startedAt = startedAt,
            finishedAt = finishedAt,
            dateStart = startDate,
            dateEnd = endDate,
            outputPath = null,
            recordCount = (result as? HistoricalMealHealthConnectResult.Success)?.deleted ?: 0,
            errorMessage = result.errorMessage(),
        )
    }

    private fun id(type: String, startedAt: LocalDateTime, mode: String): String {
        return listOf(type, startedAt, mode).joinToString("-")
    }
}

private fun HealthConnectExportResult.stateName(): String = when (this) {
    is HealthConnectExportResult.Success -> "success"
    HealthConnectExportResult.HealthConnectUnavailable -> "health_connect_unavailable"
    HealthConnectExportResult.PermissionsMissing -> "permissions_missing"
    is HealthConnectExportResult.Failed -> "failed"
}

private fun HealthConnectExportResult.errorMessage(): String? = when (this) {
    is HealthConnectExportResult.Failed -> message
    HealthConnectExportResult.HealthConnectUnavailable -> "Health Connect unavailable"
    HealthConnectExportResult.PermissionsMissing -> "Permissions missing"
    is HealthConnectExportResult.Success -> null
}

private fun HistoricalMealHealthConnectResult.stateName(): String = when (this) {
    is HistoricalMealHealthConnectResult.Success -> "success"
    HistoricalMealHealthConnectResult.HealthConnectUnavailable -> "health_connect_unavailable"
    HistoricalMealHealthConnectResult.PermissionsMissing -> "permissions_missing"
    is HistoricalMealHealthConnectResult.Failed -> "failed"
}

private fun HistoricalMealHealthConnectResult.errorMessage(): String? = when (this) {
    is HistoricalMealHealthConnectResult.Failed -> message
    HistoricalMealHealthConnectResult.HealthConnectUnavailable -> "Health Connect unavailable"
    HistoricalMealHealthConnectResult.PermissionsMissing -> "Permissions missing"
    is HistoricalMealHealthConnectResult.Success -> null
}
