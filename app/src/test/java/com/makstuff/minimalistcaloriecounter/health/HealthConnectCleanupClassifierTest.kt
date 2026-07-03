package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutbox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectCleanupClassifierTest {
    @Test
    fun cleanupModesMatchExpectedCategories() {
        assertTrue(HealthConnectCleanupCategory.HistoricalImport.matches(HealthConnectCleanupMode.HistoricalImports))
        assertTrue(HealthConnectCleanupCategory.LegacyDailyTotal.matches(HealthConnectCleanupMode.HistoricalImports))
        assertFalse(HealthConnectCleanupCategory.AddMeal.matches(HealthConnectCleanupMode.HistoricalImports))

        assertTrue(HealthConnectCleanupCategory.AddMeal.matches(HealthConnectCleanupMode.AddMeal))
        assertFalse(HealthConnectCleanupCategory.HistoricalImport.matches(HealthConnectCleanupMode.AddMeal))

        assertTrue(HealthConnectCleanupCategory.HistoricalImport.matches(HealthConnectCleanupMode.AllAppNutrition))
        assertTrue(HealthConnectCleanupCategory.AddMeal.matches(HealthConnectCleanupMode.AllAppNutrition))
        assertTrue(HealthConnectCleanupCategory.OtherAppNutrition.matches(HealthConnectCleanupMode.AllAppNutrition))
    }

    @Test
    fun categoriesBuildPreviewCounts() {
        val preview = listOf(
            HealthConnectCleanupCategory.HistoricalImport,
            HealthConnectCleanupCategory.AddMeal,
            HealthConnectCleanupCategory.AddMeal,
            HealthConnectCleanupCategory.LegacyDailyTotal,
        ).toCleanupPreview()

        assertEquals(4, preview.total)
        assertEquals(1, preview.historicalImports)
        assertEquals(2, preview.addMeal)
        assertEquals(1, preview.legacyDailyTotals)
    }

    @Test
    fun constantsKeepExpectedHealthConnectPrefixes() {
        assertEquals("mcc-historical-meal:", HistoricalMealImporter.CLIENT_RECORD_ID_PREFIX)
        assertEquals("mcc-add-meal", QuickImportOutbox.CLIENT_RECORD_PREFIX)
        assertEquals(MealType.MEAL_TYPE_UNKNOWN, 0)
    }
}
