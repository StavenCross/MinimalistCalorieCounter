package com.makstuff.minimalistcaloriecounter.classes

import androidx.health.connect.client.records.MealType
import java.time.LocalDateTime
import java.time.LocalTime

data class QuickImportNutrients(
    val energy: Double,
    val carbohydrate: Double,
    val sugar: Double,
    val protein: Double,
    val fat: Double,
    val saturatedFat: Double,
    val fiber: Double,
) {
    val appCarbohydrate: Double
        get() = (carbohydrate - fiber).coerceAtLeast(0.0)

    init {
        require(energy >= 0.0) { "Calories must be zero or greater." }
        require(carbohydrate >= 0.0) { "Carbs must be zero or greater." }
        require(sugar >= 0.0) { "Sugar must be zero or greater." }
        require(protein >= 0.0) { "Protein must be zero or greater." }
        require(fat >= 0.0) { "Fat must be zero or greater." }
        require(saturatedFat >= 0.0) { "Saturated fat must be zero or greater." }
        require(fiber >= 0.0) { "Fiber must be zero or greater." }
        require(carbohydrate + EPSILON >= fiber) { "Fiber cannot exceed total carbs." }
        require(fat + EPSILON >= saturatedFat) { "Saturated fat cannot exceed total fat." }
        require(appCarbohydrate + EPSILON >= sugar) { "Sugar cannot exceed carbs after fiber." }
    }

    fun toAppValues(): List<Double> = listOf(
        energy,
        appCarbohydrate,
        sugar,
        protein,
        fat,
        saturatedFat,
        fiber,
        0.0,
    )

    fun times(factor: Double): QuickImportNutrients = QuickImportNutrients(
        energy = energy * factor,
        carbohydrate = carbohydrate * factor,
        sugar = sugar * factor,
        protein = protein * factor,
        fat = fat * factor,
        saturatedFat = saturatedFat * factor,
        fiber = fiber * factor,
    )

    companion object {
        private const val EPSILON = 0.000001
    }
}

data class QuickImportFood(
    val amountText: String,
    val name: String,
    val grams: Double?,
    val nutrients: QuickImportNutrients,
) {
    val databaseName: String
        get() = QuickImportSanitizer.databaseName(name)

    fun nutrientsPer100g(): QuickImportNutrients? {
        val gramsValue = grams ?: return null
        if (gramsValue <= 0.0) return null
        return nutrients.times(100.0 / gramsValue)
    }
}

data class QuickImportMeal(
    val foods: List<QuickImportFood>,
    val totals: QuickImportNutrients,
)

data class QuickImportHealthPayload(
    val dateTime: LocalDateTime,
    val mealType: Int,
    val energy: Double,
    val energyFromFat: Double,
    val totalCarbohydrate: Double,
    val sugar: Double,
    val protein: Double,
    val totalFat: Double,
    val saturatedFat: Double,
    val dietaryFiber: Double,
    val name: String,
    val clientRecordId: String? = null,
)

enum class QuickImportMealType(val label: String, val healthConnectValue: Int) {
    Breakfast("Breakfast", MealType.MEAL_TYPE_BREAKFAST),
    Lunch("Lunch", MealType.MEAL_TYPE_LUNCH),
    Dinner("Dinner", MealType.MEAL_TYPE_DINNER),
    Snack("Snack", MealType.MEAL_TYPE_SNACK);

    fun applyDefaultTime(dateTime: LocalDateTime): LocalDateTime {
        val time = when (this) {
            Breakfast -> LocalTime.of(9, 0)
            Lunch -> LocalTime.of(12, 0)
            Dinner -> LocalTime.of(18, 0)
            Snack -> return dateTime
        }
        return dateTime.toLocalDate().atTime(time)
    }

    companion object {
        fun inferFrom(dateTime: LocalDateTime): QuickImportMealType {
            return when (dateTime.hour) {
                in 1..10 -> Breakfast
                in 11..14 -> Lunch
                in 15..22 -> Dinner
                else -> Snack
            }
        }
    }
}

data class QuickImportDatabaseEntryDraft(
    val name: String,
    val grams: Double,
    val nutrientsPer100g: QuickImportNutrients,
)

data class QuickImportCommitOptions(
    val addFoodsToDatabase: Boolean,
    val addFoodsToDay: Boolean,
    val writeHealthConnect: Boolean,
)

data class QuickImportCommitPlan(
    val foodDrafts: List<QuickImportDatabaseEntryDraft>,
    val healthPayloads: List<QuickImportHealthPayload>,
)

sealed class QuickImportHealthWriteResult {
    data object Success : QuickImportHealthWriteResult()
    data object HealthConnectUnavailable : QuickImportHealthWriteResult()
    data object PermissionsMissing : QuickImportHealthWriteResult()
    data class Failed(val message: String) : QuickImportHealthWriteResult()
}

data class QuickImportResult(
    val databaseEntriesAdded: Int,
    val dayFoodsAdded: Int,
    val healthWriteResult: QuickImportHealthWriteResult?,
)
