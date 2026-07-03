package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime

object QuickImportMapper {
    fun toHealthPayloads(
        meal: QuickImportMeal,
        dateTime: LocalDateTime,
        mealType: QuickImportMealType,
    ): List<QuickImportHealthPayload> {
        return meal.foods.mapIndexed { index, food ->
            toHealthPayload(food, dateTime.plusSeconds(index.toLong()), mealType)
        }
    }

    fun toHealthPayload(
        food: QuickImportFood,
        dateTime: LocalDateTime,
        mealType: QuickImportMealType,
    ): QuickImportHealthPayload {
        val displayName = listOf(food.amountText, food.name)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return toHealthPayload(
            dateTime = dateTime,
            mealType = mealType,
            nutrients = food.nutrients,
            name = displayName.ifBlank { food.name },
        )
    }

    fun toHealthPayload(
        dateTime: LocalDateTime,
        mealType: QuickImportMealType,
        nutrients: QuickImportNutrients,
        name: String,
        clientRecordId: String? = null,
    ): QuickImportHealthPayload {
        // Health Connect records need total carbs and energy-from-fat, so all meal import paths share this mapping.
        return QuickImportHealthPayload(
            dateTime = dateTime,
            mealType = mealType.healthConnectValue,
            energy = nutrients.energy,
            energyFromFat = nutrients.fat * 9.0,
            totalCarbohydrate = nutrients.carbohydrate,
            sugar = nutrients.sugar,
            protein = nutrients.protein,
            totalFat = nutrients.fat,
            saturatedFat = nutrients.saturatedFat,
            dietaryFiber = nutrients.fiber,
            name = name,
            clientRecordId = clientRecordId,
        )
    }
}
