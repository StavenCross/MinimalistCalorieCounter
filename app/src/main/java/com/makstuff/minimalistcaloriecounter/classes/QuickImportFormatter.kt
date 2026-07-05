package com.makstuff.minimalistcaloriecounter.classes

import kotlin.math.abs
import java.util.Locale

object QuickImportFormatter {
    fun text(meal: QuickImportMeal): String {
        val normalizedMeal = meal.copy(totals = sumFoods(meal.foods))
        return (normalizedMeal.foods.map(::foodLine) + totalsLine(normalizedMeal.totals)).joinToString("\n")
    }

    fun replaceFood(meal: QuickImportMeal, foodIndex: Int, food: QuickImportFood): QuickImportMeal {
        require(foodIndex in meal.foods.indices) { "Food could not be found in the parsed meal." }
        val foods = meal.foods.toMutableList().also { it[foodIndex] = food }
        return QuickImportMeal(foods = foods, totals = sumFoods(foods))
    }

    fun replaceFoodGroup(meal: QuickImportMeal, foodIndex: Int, food: QuickImportFood): QuickImportMeal {
        require(foodIndex in meal.foods.indices) { "Food could not be found in the parsed meal." }
        val originalFood = meal.foods[foodIndex]
        val foods = meal.foods.map { if (it == originalFood) food else it }
        return QuickImportMeal(foods = foods, totals = sumFoods(foods))
    }

    fun addFoodServing(meal: QuickImportMeal, foodIndex: Int): QuickImportMeal {
        require(foodIndex in meal.foods.indices) { "Food could not be found in the parsed meal." }
        val food = meal.foods[foodIndex]
        val insertAfter = meal.foods.indexOfLast { it == food }
        val foods = meal.foods.toMutableList().also { it.add(insertAfter + 1, food) }
        return QuickImportMeal(foods = foods, totals = sumFoods(foods))
    }

    fun removeFoodServing(meal: QuickImportMeal, foodIndex: Int): QuickImportMeal {
        require(foodIndex in meal.foods.indices) { "Food could not be found in the parsed meal." }
        val food = meal.foods[foodIndex]
        val matchingIndexes = meal.foods.mapIndexedNotNull { index, candidate -> if (candidate == food) index else null }
        require(matchingIndexes.size > 1) { "At least one serving is required." }
        val foods = meal.foods.toMutableList().also { it.removeAt(matchingIndexes.last()) }
        return QuickImportMeal(foods = foods, totals = sumFoods(foods))
    }

    private fun foodLine(food: QuickImportFood): String {
        val title = listOf(food.amountText, food.name).filter { it.isNotBlank() }.joinToString(" ")
        return "$title; ${nutrientText(food.nutrients)}."
    }

    private fun totalsLine(totals: QuickImportNutrients): String = "Meal totals; ${nutrientText(totals)}."

    private fun nutrientText(nutrients: QuickImportNutrients): String = listOf(
        "Calories ${format(nutrients.energy)}",
        "Protein ${format(nutrients.protein)}g",
        "Carbs ${format(nutrients.carbohydrate)}g",
        "Fat ${format(nutrients.fat)}g",
        "Fiber ${format(nutrients.fiber)}g",
        "Sugar ${format(nutrients.sugar)}g",
        "Sat Fat ${format(nutrients.saturatedFat)}g",
    ).joinToString(", ")

    private fun sumFoods(foods: List<QuickImportFood>): QuickImportNutrients = QuickImportNutrients(
        energy = foods.sumOf { it.nutrients.energy },
        carbohydrate = foods.sumOf { it.nutrients.carbohydrate },
        sugar = foods.sumOf { it.nutrients.sugar },
        protein = foods.sumOf { it.nutrients.protein },
        fat = foods.sumOf { it.nutrients.fat },
        saturatedFat = foods.sumOf { it.nutrients.saturatedFat },
        fiber = foods.sumOf { it.nutrients.fiber },
    )

    private fun format(value: Double): String {
        val roundedWhole = kotlin.math.round(value).toLong()
        if (abs(value - roundedWhole) < 0.0001) return roundedWhole.toString()
        return String.format(Locale.US, "%.1f", value)
    }
}
