package com.makstuff.minimalistcaloriecounter.health

import java.time.LocalDate

enum class HealthConnectCleanupMode(val label: String) {
    HistoricalImports("Historical imports only"),
    AddMeal("Add Meal records only"),
    AllAppNutrition("All app-owned nutrition"),
}

data class HealthConnectCleanupPreview(
    val total: Int,
    val historicalImports: Int,
    val addMeal: Int,
    val legacyDailyTotals: Int,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val mode: HealthConnectCleanupMode? = null,
)

sealed class HealthConnectCleanupPreviewResult {
    data class Success(val preview: HealthConnectCleanupPreview) : HealthConnectCleanupPreviewResult()
    data object HealthConnectUnavailable : HealthConnectCleanupPreviewResult()
    data object PermissionsMissing : HealthConnectCleanupPreviewResult()
    data class Failed(val message: String) : HealthConnectCleanupPreviewResult()
}
