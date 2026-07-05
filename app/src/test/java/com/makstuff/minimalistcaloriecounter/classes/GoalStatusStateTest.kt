package com.makstuff.minimalistcaloriecounter.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalStatusStateTest {
    private val today = LocalDate.of(2026, 7, 2)

    @Test
    fun `goal status prioritizes incomplete profile`() {
        val goals = Goals(
            currentTargets = completeTargets(2000.0),
            recommendation = GoalRecommendation(
                generatedDate = today,
                targets = completeTargets(2200.0),
                bmr = 1800.0,
                tdee = 2500.0,
            ),
        )

        val state = goals.statusState(today)

        assertTrue(state is GoalStatusState.ProfileIncomplete)
    }

    @Test
    fun `goal status suppresses tiny recommendation drift`() {
        val recommendation = GoalRecommendation(
            generatedDate = today,
            targets = completeTargets(2040.0).copy(protein = 183.0, carbs = 223.0, fat = 72.0, fiber = 31.0),
            bmr = 1800.0,
            tdee = 2500.0,
        )
        val goals = Goals(
            profile = completeProfile(),
            currentTargets = completeTargets(2000.0),
            recommendation = recommendation.onlyIfMeaningfulComparedTo(completeTargets(2000.0)),
        )

        assertEquals(null, goals.recommendation)
        assertEquals(GoalStatusState.CurrentGoal, goals.statusState(today))
    }

    @Test
    fun `goal status surfaces meaningful unlocked recommendation differences`() {
        val recommendation = GoalRecommendation(
            generatedDate = today,
            targets = completeTargets(2100.0).copy(protein = 190.0),
            bmr = 1800.0,
            tdee = 2500.0,
        )
        val goals = Goals(
            profile = completeProfile(),
            currentTargets = completeTargets(2000.0),
            recommendation = recommendation.onlyIfMeaningfulComparedTo(completeTargets(2000.0)),
        )

        val state = goals.statusState(today)

        assertTrue(state is GoalStatusState.NewRecommendation)
        assertEquals(2, (state as GoalStatusState.NewRecommendation).differences.size)
    }

    @Test
    fun `locked macro differences do not create recommendations`() {
        val currentTargets = completeTargets(2000.0).withValue(GoalMacro.Calories, 2000.0)
        val recommendation = GoalRecommendation(
            generatedDate = today,
            targets = completeTargets(2200.0),
            bmr = 1800.0,
            tdee = 2500.0,
        )

        val filtered = recommendation.onlyIfMeaningfulComparedTo(currentTargets)

        assertEquals(null, filtered)
    }

    private fun completeProfile() = GoalProfile(
        birthday = LocalDate.of(1990, 7, 2),
        sex = GoalSex.Male,
        heightCm = GoalMeasurement(180.0),
        weightKg = GoalMeasurement(90.0),
        bodyFatPercent = GoalMeasurement(20.0),
        leanMassKg = GoalMeasurement(72.0),
    )

    private fun completeTargets(calories: Double) = MacroTargets(
        calories = calories,
        protein = 180.0,
        carbs = 220.0,
        fat = 70.0,
        fiber = 30.0,
    )
}
