package com.makstuff.minimalistcaloriecounter.classes

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class QuickImportRepeatBuilderTest {
    @Test
    fun textRoundTripsThroughQuickImportParser() {
        val text = QuickImportRepeatBuilder.text(
            listOf(
                meal("100g oats", 389.0),
                meal("50g yogurt", 60.0),
            )
        )

        val parsed = QuickImportParser.parse(text)

        assertEquals(2, parsed.foods.size)
        assertEquals("100 g", parsed.foods[0].amountText)
        assertEquals("oats", parsed.foods[0].name)
        assertEquals(449.0, parsed.totals.energy, 0.001)
        assertTrue(text.contains("Meal totals; Calories 449"))
    }

    @Test
    fun mealTypeUsesFirstFoodMealType() {
        val mealType = QuickImportRepeatBuilder.mealType(
            listOf(meal("lunch", 100.0, MealType.MEAL_TYPE_LUNCH))
        )

        assertEquals(QuickImportMealType.Lunch, mealType)
    }

    private fun meal(
        name: String,
        calories: Double,
        mealType: Int = MealType.MEAL_TYPE_DINNER,
    ): HealthConnectNutritionMeal {
        return HealthConnectNutritionMeal(
            recordId = name,
            clientRecordId = null,
            startTime = LocalDateTime.of(2026, 7, 3, 18, 0),
            endTime = LocalDateTime.of(2026, 7, 3, 18, 1),
            name = name,
            energy = calories,
            energyFromFat = null,
            totalCarbohydrate = 30.0,
            sugar = 5.0,
            protein = 20.0,
            totalFat = 10.0,
            saturatedFat = 2.0,
            dietaryFiber = 4.0,
            mealType = mealType,
        )
    }
}
