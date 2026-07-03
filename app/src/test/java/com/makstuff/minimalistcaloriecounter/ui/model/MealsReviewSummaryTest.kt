package com.makstuff.minimalistcaloriecounter.ui.model

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MealsReviewSummaryTest {
    @Test
    fun daySummaryIncludesTotalsTargetsAndMeals() {
        val text = mealsDaySummaryText(
            date = LocalDate.of(2026, 7, 3),
            meals = listOf(meal("tuna sandwich", 520.0), meal("yogurt", 150.0)),
            targets = MacroTargets(calories = 2200.0, protein = 180.0, carbs = 220.0, fat = 70.0, fiber = 35.0),
        )

        assertTrue(text.contains("Meals for 2026-07-03"))
        assertTrue(text.contains("- Foods: 2"))
        assertTrue(text.contains("- Calories: 670 kcal"))
        assertTrue(text.contains("- Calories: 670/2200 kcal, 1530 kcal remaining"))
        assertTrue(text.contains("Lunch: 2 foods, 670 kcal"))
        assertTrue(text.contains("tuna sandwich: 520 kcal"))
    }

    @Test
    fun mealSummaryIncludesFoodRows() {
        val group = mealGroups(listOf(meal("tuna sandwich", 520.0))).single()

        val text = mealGroupSummaryText(group)

        assertTrue(text.contains("Lunch: 1 foods, 520 kcal"))
        assertTrue(text.contains("Protein 30g, carbs 50g, fat 10g, fiber 5g"))
        assertTrue(text.contains("- tuna sandwich: 520 kcal"))
    }

    private fun meal(name: String, calories: Double): HealthConnectNutritionMeal {
        return HealthConnectNutritionMeal(
            recordId = name,
            clientRecordId = null,
            startTime = LocalDateTime.of(2026, 7, 3, 12, 0),
            endTime = LocalDateTime.of(2026, 7, 3, 12, 1),
            name = name,
            energy = calories,
            energyFromFat = null,
            totalCarbohydrate = 50.0,
            sugar = 0.0,
            protein = 30.0,
            totalFat = 10.0,
            saturatedFat = 2.0,
            dietaryFiber = 5.0,
            mealType = MealType.MEAL_TYPE_LUNCH,
        )
    }
}
