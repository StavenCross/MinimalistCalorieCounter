package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportResult
import com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectResult
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportExportJobMapperTest {
    @Test
    fun mapsHealthConnectExportSuccess() {
        val startedAt = LocalDateTime.of(2026, 7, 3, 8, 0)

        val job = ImportExportJobMapper.healthConnectExport(
            startDate = LocalDate.of(2026, 7, 1),
            endDate = LocalDate.of(2026, 7, 3),
            mode = HealthConnectExportMode.NutritionOnly,
            redacted = true,
            result = HealthConnectExportResult.Success("/Downloads/export.csv", records = 42),
            startedAt = startedAt,
            finishedAt = startedAt.plusSeconds(5),
        )

        assertEquals("health_connect_export", job.type)
        assertEquals("NutritionOnly_redacted", job.mode)
        assertEquals("success", job.state)
        assertEquals(42, job.recordCount)
        assertEquals("/Downloads/export.csv", job.outputPath)
        assertNull(job.errorMessage)
    }

    @Test
    fun mapsNutritionDeleteFailure() {
        val startedAt = LocalDateTime.of(2026, 7, 3, 8, 0)

        val job = ImportExportJobMapper.nutritionDelete(
            startDate = LocalDate.of(2026, 7, 1),
            endDate = LocalDate.of(2026, 7, 3),
            mode = HealthConnectCleanupMode.AddMeal,
            result = HistoricalMealHealthConnectResult.Failed("Delete failed"),
            startedAt = startedAt,
            finishedAt = startedAt.plusSeconds(5),
        )

        assertEquals("health_connect_delete", job.type)
        assertEquals("AddMeal", job.mode)
        assertEquals("failed", job.state)
        assertEquals(0, job.recordCount)
        assertEquals("Delete failed", job.errorMessage)
    }
}
