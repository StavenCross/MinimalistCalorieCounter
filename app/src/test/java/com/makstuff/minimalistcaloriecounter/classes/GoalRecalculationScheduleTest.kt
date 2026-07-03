package com.makstuff.minimalistcaloriecounter.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalRecalculationScheduleTest {
    @Test
    fun sundayWithoutCurrentRecommendationIsDue() {
        val status = GoalRecalculationSchedule.status(
            Goals(),
            today = LocalDate.of(2026, 7, 5),
        )

        assertTrue(status.dueToday)
        assertEquals(LocalDate.of(2026, 7, 5), status.nextSunday)
    }

    @Test
    fun sundayWithCurrentRecommendationIsNotDue() {
        val today = LocalDate.of(2026, 7, 5)
        val goals = Goals(
            recommendation = GoalRecommendation(
                generatedDate = today,
                targets = MacroTargets(calories = 2050.0),
                bmr = 1850.0,
                tdee = 2550.0,
            )
        )

        val status = GoalRecalculationSchedule.status(goals, today)

        assertFalse(status.dueToday)
        assertEquals(today, status.lastRecommendationDate)
        assertEquals(today, status.nextSunday)
    }

    @Test
    fun weekdayUsesUpcomingSundayAndAppliedHistory() {
        val goals = Goals(
            history = listOf(
                GoalHistoryEntry(
                    effectiveDate = LocalDate.of(2026, 7, 1),
                    targets = MacroTargets(calories = 2050.0),
                    source = "recommended",
                    generatedDate = LocalDate.of(2026, 6, 28),
                )
            )
        )

        val status = GoalRecalculationSchedule.status(goals, LocalDate.of(2026, 7, 3))

        assertFalse(status.dueToday)
        assertEquals(LocalDate.of(2026, 6, 28), status.lastRecommendationDate)
        assertEquals(LocalDate.of(2026, 7, 5), status.nextSunday)
    }
}
