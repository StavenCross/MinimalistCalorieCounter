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
        return QuickImportHealthPayload(
            dateTime = dateTime,
            mealType = mealType.healthConnectValue,
            energy = food.nutrients.energy,
            energyFromFat = food.nutrients.fat * 9.0,
            totalCarbohydrate = food.nutrients.carbohydrate,
            sugar = food.nutrients.sugar,
            protein = food.nutrients.protein,
            totalFat = food.nutrients.fat,
            saturatedFat = food.nutrients.saturatedFat,
            dietaryFiber = food.nutrients.fiber,
            name = displayName.ifBlank { food.name },
        )
    }
}
