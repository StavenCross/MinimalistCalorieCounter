package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutbox

internal enum class HealthConnectCleanupCategory {
    HistoricalImport,
    AddMeal,
    LegacyDailyTotal,
    OtherAppNutrition,
}

internal fun NutritionRecord.cleanupCategory(): HealthConnectCleanupCategory {
    val clientRecordId = metadata.clientRecordId.orEmpty()
    return when {
        clientRecordId.startsWith(HistoricalMealImporter.CLIENT_RECORD_ID_PREFIX) ->
            HealthConnectCleanupCategory.HistoricalImport
        clientRecordId.startsWith(QuickImportOutbox.CLIENT_RECORD_PREFIX) ->
            HealthConnectCleanupCategory.AddMeal
        name == "Daily Total" && mealType == MealType.MEAL_TYPE_UNKNOWN ->
            HealthConnectCleanupCategory.LegacyDailyTotal
        else -> HealthConnectCleanupCategory.OtherAppNutrition
    }
}

internal fun HealthConnectCleanupCategory.matches(mode: HealthConnectCleanupMode): Boolean {
    return when (mode) {
        HealthConnectCleanupMode.HistoricalImports ->
            this == HealthConnectCleanupCategory.HistoricalImport || this == HealthConnectCleanupCategory.LegacyDailyTotal
        HealthConnectCleanupMode.AddMeal -> this == HealthConnectCleanupCategory.AddMeal
        HealthConnectCleanupMode.AllAppNutrition -> true
    }
}

internal fun Iterable<HealthConnectCleanupCategory>.toCleanupPreview(): HealthConnectCleanupPreview {
    return HealthConnectCleanupPreview(
        total = count(),
        historicalImports = count { it == HealthConnectCleanupCategory.HistoricalImport },
        addMeal = count { it == HealthConnectCleanupCategory.AddMeal },
        legacyDailyTotals = count { it == HealthConnectCleanupCategory.LegacyDailyTotal },
    )
}
