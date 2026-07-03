package com.makstuff.minimalistcaloriecounter.ui.model

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TodayCheckInSummaryTest {
    @Test
    fun checkInSummaryIncludesMealsTargetsAndBodyMetrics() {
        val summary = todayCheckInSummary(
            date = LocalDate.of(2026, 7, 3),
            meals = listOf(
                meal("rice bowl", 500.0, MealType.MEAL_TYPE_LUNCH),
                meal("greek yogurt", 150.0, MealType.MEAL_TYPE_SNACK),
            ),
            targets = MacroTargets(calories = 2200.0, protein = 180.0, carbs = 220.0, fat = 70.0, fiber = 35.0),
            profile = GoalProfile(
                weightKg = GoalMeasurement(value = 90.0),
                bodyFatPercent = GoalMeasurement(value = 20.0),
            ),
        )

        assertTrue(summary.contains("Nutrition check-in for 2026-07-03"))
        assertTrue(summary.contains("Lunch: 1 foods, 500 kcal"))
        assertTrue(summary.contains("Calories: 650/2200 kcal, 1550 kcal remaining"))
        assertTrue(summary.contains("Weight 90 kg"))
        assertTrue(summary.contains("Lean mass 72 kg"))
    }

    private fun meal(name: String, calories: Double, mealType: Int): HealthConnectNutritionMeal {
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
            mealType = mealType,
        )
    }
}
