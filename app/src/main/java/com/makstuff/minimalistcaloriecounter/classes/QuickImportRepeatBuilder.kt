package com.makstuff.minimalistcaloriecounter.classes

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import java.time.LocalDateTime

object QuickImportRepeatBuilder {
    fun build(foods: List<HealthConnectNutritionMeal>): QuickImportMeal {
        val quickFoods = foods.map { it.toQuickImportFood() }
        return QuickImportMeal(
            foods = quickFoods,
            totals = quickFoods.sumOfNutrients(),
        )
    }

    fun text(foods: List<HealthConnectNutritionMeal>): String {
        val meal = build(foods)
        return buildString {
            meal.foods.forEach { food ->
                append(food.toRepeatLine())
                appendLine()
            }
            append("Meal totals; ")
            append(meal.totals.toRepeatNutrientsText())
        }.trim()
    }

    fun mealType(foods: List<HealthConnectNutritionMeal>): QuickImportMealType {
        return when (foods.firstOrNull()?.mealType) {
            MealType.MEAL_TYPE_BREAKFAST -> QuickImportMealType.Breakfast
            MealType.MEAL_TYPE_LUNCH -> QuickImportMealType.Lunch
            MealType.MEAL_TYPE_DINNER -> QuickImportMealType.Dinner
            MealType.MEAL_TYPE_SNACK -> QuickImportMealType.Snack
            else -> QuickImportMealType.inferFrom(LocalDateTime.now())
        }
    }

    private fun HealthConnectNutritionMeal.toQuickImportFood(): QuickImportFood {
        return QuickImportFood(
            amountText = "",
            name = name,
            grams = null,
            nutrients = QuickImportNutrients(
                energy = energy,
                carbohydrate = totalCarbohydrate,
                sugar = sugar,
                protein = protein,
                fat = totalFat,
                saturatedFat = saturatedFat,
                fiber = dietaryFiber,
            ),
        )
    }

    private fun List<QuickImportFood>.sumOfNutrients(): QuickImportNutrients {
        return QuickImportNutrients(
            energy = sumOf { it.nutrients.energy },
            carbohydrate = sumOf { it.nutrients.carbohydrate },
            sugar = sumOf { it.nutrients.sugar },
            protein = sumOf { it.nutrients.protein },
            fat = sumOf { it.nutrients.fat },
            saturatedFat = sumOf { it.nutrients.saturatedFat },
            fiber = sumOf { it.nutrients.fiber },
        )
    }

    private fun QuickImportFood.toRepeatLine(): String {
        return "$name; ${nutrients.toRepeatNutrientsText()}"
    }

    private fun QuickImportNutrients.toRepeatNutrientsText(): String {
        return listOf(
            "Calories ${energy.toFormattedString(true)}",
            "Fat ${fat.toFormattedString(true)}g",
            "Sat Fat ${saturatedFat.toFormattedString(true)}g",
            "Trans Fat 0g",
            "Cholesterol 0mg",
            "Sodium 0mg",
            "Carbs ${carbohydrate.toFormattedString(true)}g",
            "Fiber ${fiber.toFormattedString(true)}g",
            "Sugar ${sugar.toFormattedString(true)}g",
            "Added Sugar 0g",
            "Protein ${protein.toFormattedString(true)}g",
        ).joinToString(", ") + "."
    }
}
