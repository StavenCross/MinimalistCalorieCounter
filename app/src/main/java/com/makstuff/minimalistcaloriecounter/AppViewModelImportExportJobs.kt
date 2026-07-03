package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportResult
import com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectResult
import com.makstuff.minimalistcaloriecounter.persistence.room.ImportExportJobMapper
import java.time.LocalDate
import java.time.LocalDateTime

internal fun AppViewModelEnvironment.recordHealthConnectExportJob(
    startDate: LocalDate,
    endDate: LocalDate,
    mode: HealthConnectExportMode,
    redacted: Boolean,
    result: HealthConnectExportResult,
    startedAt: LocalDateTime,
) = launchRoomWrite {
    writeImportExportJob(ImportExportJobMapper.healthConnectExport(startDate, endDate, mode, redacted, result, startedAt))
}

internal fun AppViewModelEnvironment.recordHealthConnectDeleteJob(
    startDate: LocalDate,
    endDate: LocalDate,
    mode: HealthConnectCleanupMode,
    result: HistoricalMealHealthConnectResult,
    startedAt: LocalDateTime,
) = launchRoomWrite {
    writeImportExportJob(ImportExportJobMapper.nutritionDelete(startDate, endDate, mode, result, startedAt))
}
