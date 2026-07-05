package com.makstuff.minimalistcaloriecounter.classes

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalMealAllocationTest {
    @Test
    fun `breakfast allocation splits non snack calories across three meals`() {
        val allocation = GoalCalculator.mealAllocation(QuickImportMealType.Breakfast, completeTargets(), emptyList())

        assertEquals(600.0, allocation.calories!!, 0.01)
        assertEquals(3, allocation.remainingMealCount)
    }

    @Test
    fun `lunch allocation splits skipped breakfast calories across lunch and dinner`() {
        val allocation = GoalCalculator.mealAllocation(QuickImportMealType.Lunch, completeTargets(), emptyList())

        assertEquals(900.0, allocation.calories!!, 0.01)
        assertEquals(2, allocation.remainingMealCount)
    }

    @Test
    fun `dinner first gets full non snack day goal`() {
        val allocation = GoalCalculator.mealAllocation(QuickImportMealType.Dinner, completeTargets(), emptyList())

        assertEquals(1800.0, allocation.calories!!, 0.01)
        assertEquals(1, allocation.remainingMealCount)
    }

    @Test
    fun `snack allocation is fixed at three hundred calories`() {
        val allocation = GoalCalculator.mealAllocation(QuickImportMealType.Snack, completeTargets(), emptyList())

        assertEquals(300.0, allocation.calories!!, 0.01)
        assertEquals(1, allocation.remainingMealCount)
    }

    private fun completeTargets() = MacroTargets(
        calories = 2100.0,
        protein = 180.0,
        carbs = 220.0,
        fat = 70.0,
        fiber = 30.0,
    )
}
