package com.makstuff.minimalistcaloriecounter.ui.model

import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalTrendSummaryTest {
    @Test
    fun bodyTrendCardsUseHistoryAndCurrentProfile() {
        val goals = Goals(
            profile = GoalProfile(
                weightKg = GoalMeasurement(89.0),
                bodyFatPercent = GoalMeasurement(19.0),
                leanMassKg = GoalMeasurement(72.0),
            ),
            history = listOf(
                GoalHistoryEntry(
                    effectiveDate = LocalDate.of(2026, 6, 28),
                    targets = MacroTargets(),
                    source = "recommended",
                    weightKg = 90.0,
                    bodyFatPercent = 20.0,
                    leanMassKg = 71.5,
                )
            ),
        )

        val cards = goalBodyTrendCards(goals, LocalDate.of(2026, 7, 5))

        assertEquals("196 lb", cards.first { it.label == "Weight" }.value)
        assertTrue(cards.first { it.label == "Weight" }.detail.contains("-2.2 lb"))
        assertTrue(cards.first { it.label == "Lean mass" }.detail.contains("+1.1 lb"))
    }

    @Test
    fun adherenceCardsCompareTotalsToTargets() {
        val cards = goalAdherenceCards(
            totals = QuickImportNutrients(
                energy = 1000.0,
                carbohydrate = 100.0,
                sugar = 0.0,
                protein = 75.0,
                fat = 40.0,
                saturatedFat = 0.0,
                fiber = 20.0,
            ),
            targets = MacroTargets(calories = 2000.0, protein = 150.0, carbs = 200.0, fat = 80.0, fiber = 40.0),
        )

        assertEquals("50%", cards.first { it.label == "Calories" }.value)
        assertEquals("75/150 g", cards.first { it.label == "Protein" }.detail)
    }
}
