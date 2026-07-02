package com.makstuff.minimalistcaloriecounter.health

import java.time.LocalDateTime

data class HealthConnectNutritionMeal(
    val recordId: String,
    val clientRecordId: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val name: String,
    val energy: Double,
    val energyFromFat: Double?,
    val totalCarbohydrate: Double,
    val sugar: Double,
    val protein: Double,
    val totalFat: Double,
    val saturatedFat: Double,
    val dietaryFiber: Double,
    val mealType: Int,
)

sealed class HealthConnectNutritionReadResult {
    data class Success(val meals: List<HealthConnectNutritionMeal>) : HealthConnectNutritionReadResult()
    data object HealthConnectUnavailable : HealthConnectNutritionReadResult()
    data object PermissionsMissing : HealthConnectNutritionReadResult()
    data class Failed(val message: String) : HealthConnectNutritionReadResult()
}

sealed class HealthConnectDeleteResult {
    data object Success : HealthConnectDeleteResult()
    data object HealthConnectUnavailable : HealthConnectDeleteResult()
    data object PermissionsMissing : HealthConnectDeleteResult()
    data class Failed(val message: String) : HealthConnectDeleteResult()
}

sealed class HistoricalMealHealthConnectResult {
    data class Success(
        val written: Int,
        val skippedDuplicates: Int,
        val deleted: Int = 0,
    ) : HistoricalMealHealthConnectResult()
    data object HealthConnectUnavailable : HistoricalMealHealthConnectResult()
    data object PermissionsMissing : HistoricalMealHealthConnectResult()
    data class Failed(val message: String) : HistoricalMealHealthConnectResult()
}
