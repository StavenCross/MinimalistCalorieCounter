package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectExportModeTest {
    @Test
    fun nutritionOnlyExportsOnlyNutritionRecords() {
        assertEquals(listOf(NutritionRecord::class), HealthConnectExportMode.NutritionOnly.recordTypes())
    }

    @Test
    fun nutritionAndGoalsExportsNutritionAndBodyMetrics() {
        assertEquals(
            listOf(
                NutritionRecord::class,
                WeightRecord::class,
                HeightRecord::class,
                BodyFatRecord::class,
                LeanBodyMassRecord::class,
            ),
            HealthConnectExportMode.NutritionAndGoals.recordTypes(),
        )
    }

    @Test
    fun fullExportIncludesNutritionAndMoreRecordTypes() {
        val types = HealthConnectExportMode.Full.recordTypes()

        assertTrue(types.contains(NutritionRecord::class))
        assertTrue(types.size > HealthConnectExportMode.NutritionAndGoals.recordTypes().size)
    }
}
