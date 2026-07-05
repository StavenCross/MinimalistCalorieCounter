package com.makstuff.minimalistcaloriecounter.ui.model

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class NutritionUiModelsTest {
    @Test
    fun mealGroupsUseStableOrderAndSortFoodsByTime() {
        val dinnerLate = meal("late dinner", 18, MealType.MEAL_TYPE_DINNER)
        val breakfast = meal("breakfast", 8, MealType.MEAL_TYPE_BREAKFAST)
        val dinnerEarly = meal("early dinner", 17, MealType.MEAL_TYPE_DINNER)
        val other = meal("unknown", 23, MealType.MEAL_TYPE_UNKNOWN)

        val groups = mealGroups(listOf(dinnerLate, breakfast, other, dinnerEarly))

        assertEquals(listOf("Breakfast", "Dinner", "Other"), groups.map { it.label })
        assertEquals(listOf("early dinner", "late dinner"), groups.single { it.label == "Dinner" }.foods.map { it.name })
    }

    @Test
    fun nutritionDaySummarySumsMacrosAndProgress() {
        val summary = nutritionDaySummary(
            meals = listOf(
                meal("lunch", 12, MealType.MEAL_TYPE_LUNCH, energy = 400.0, carbs = 40.0, protein = 30.0, fat = 10.0, fiber = 8.0),
                meal("snack", 15, MealType.MEAL_TYPE_SNACK, energy = 100.0, carbs = 10.0, protein = 5.0, fat = 2.0, fiber = 2.0),
            ),
            targets = MacroTargets(calories = 1000.0, protein = 70.0, carbs = 100.0, fat = 40.0, fiber = 25.0),
        )

        assertEquals(2, summary.foodCount)
        assertEquals(500.0, summary.totals.energy, 0.001)
        assertEquals(50.0, summary.totals.carbohydrate, 0.001)
        assertEquals(35.0, summary.totals.protein, 0.001)
        assertEquals(12.0, summary.totals.fat, 0.001)
        assertEquals(10.0, summary.totals.fiber, 0.001)
        assertEquals(50.0, summary.progress.calories!!, 0.001)
        assertEquals(50.0, summary.progress.protein!!, 0.001)
        assertEquals(50.0, summary.progress.carbs!!, 0.001)
    }

    @Test
    fun macroPercentReturnsNullForMissingOrZeroTargets() {
        assertNull(macroPercent(10.0, null))
        assertNull(macroPercent(10.0, 0.0))
        assertEquals(50.0, macroPercent(25.0, 50.0)!!, 0.001)
    }

    @Test
    fun macroProgressArcModelsUnderTargetAndOverage() {
        assertEquals(0f, macroProgressArc(null).progress, 0.001f)
        assertEquals(false, macroProgressArc(null).isOverTarget)
        assertEquals(0.5f, macroProgressArc(50.0).progress, 0.001f)
        assertEquals(false, macroProgressArc(100.0).isOverTarget)
        assertEquals(0.5f, macroProgressArc(150.0).progress, 0.001f)
        assertEquals(true, macroProgressArc(150.0).isOverTarget)
        assertEquals(1.0f, macroProgressArc(250.0).progress, 0.001f)
    }

    @Test
    fun detailItemBuildersUseStableLabelsAndUnits() {
        val nutrients = meal("lunch", 12, MealType.MEAL_TYPE_LUNCH, energy = 400.0, carbs = 40.0, protein = 30.0, fat = 10.0, fiber = 8.0)
            .let { listOf(it).sumNutrition() }

        assertEquals(
            listOf("Amount", "Calories", "Carbs", "Protein", "Fat", "Fiber", "Sugar", "Sat fat"),
            quickNutrientDetailItems(nutrients, includeAmount = "100g").map { it.label },
        )
        assertEquals("400 kcal", quickNutrientDetailItems(nutrients, includeAmount = null).first().value)
        assertEquals("Fat kcal", healthMealDetailItems(meal("lunch", 12, MealType.MEAL_TYPE_LUNCH)).last().label)
        assert(supportsMacroHint("Calories"))
        assert(!supportsMacroHint("Sat fat"))
    }

    @Test
    fun unknownHealthConnectMealTypeFallsBackToTimeInference() {
        assertEquals(QuickImportMealType.Breakfast, meal("morning", 9, MealType.MEAL_TYPE_UNKNOWN).quickImportMealType())
        assertEquals(QuickImportMealType.Snack, meal("late", 23, MealType.MEAL_TYPE_UNKNOWN).quickImportMealType())
    }

    @Test
    fun mealServingGroupsCollapseIdenticalFoodsInSameMinute() {
        val first = meal("Whiskey", 21, MealType.MEAL_TYPE_SNACK, energy = 100.0, carbs = 0.0, protein = 0.0, fat = 0.0, fiber = 0.0)
        val second = first.copy(recordId = "second", startTime = first.startTime.plusSeconds(15), endTime = first.endTime.plusSeconds(15))
        val later = first.copy(recordId = "later", startTime = first.startTime.plusMinutes(1), endTime = first.endTime.plusMinutes(1))

        val groups = mealServingGroups(listOf(first, second, later))

        assertEquals(listOf(2, 1), groups.map { it.quantity })
        assertEquals(listOf(first.recordId, second.recordId), groups.first().foods.map { it.recordId })
    }

    @Test
    fun servingGroupForFindsMatchingSiblings() {
        val first = meal("Whiskey", 21, MealType.MEAL_TYPE_SNACK, energy = 100.0, carbs = 0.0, protein = 0.0, fat = 0.0, fiber = 0.0)
        val second = first.copy(recordId = "second", startTime = first.startTime.plusSeconds(30), endTime = first.endTime.plusSeconds(30))
        val other = first.copy(recordId = "other", name = "Soda")

        val group = servingGroupFor(first, listOf(other, second, first))

        assertEquals(2, group.quantity)
        assertEquals(listOf(first.recordId, second.recordId), group.foods.map { it.recordId })
    }

    private fun meal(
        name: String,
        hour: Int,
        mealType: Int,
        energy: Double = 100.0,
        carbs: Double = 10.0,
        protein: Double = 5.0,
        fat: Double = 2.0,
        fiber: Double = 1.0,
    ): HealthConnectNutritionMeal {
        val start = LocalDateTime.of(2026, 7, 3, hour, 0)
        return HealthConnectNutritionMeal(
            recordId = name,
            clientRecordId = null,
            startTime = start,
            endTime = start.plusMinutes(1),
            name = name,
            energy = energy,
            energyFromFat = fat * 9.0,
            totalCarbohydrate = carbs,
            sugar = 1.0,
            protein = protein,
            totalFat = fat,
            saturatedFat = 0.5,
            dietaryFiber = fiber,
            mealType = mealType,
        )
    }
}
