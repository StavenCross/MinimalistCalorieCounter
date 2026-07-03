package com.makstuff.minimalistcaloriecounter.classes

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class GoalRecalculationStatus(
    val today: LocalDate,
    val nextSunday: LocalDate,
    val lastRecommendationDate: LocalDate?,
    val dueToday: Boolean,
)

object GoalRecalculationSchedule {
    fun status(goals: Goals, today: LocalDate = LocalDate.now()): GoalRecalculationStatus {
        val lastDate = lastRecommendationDate(goals)
        val dueToday = today.dayOfWeek == DayOfWeek.SUNDAY && lastDate != today
        return GoalRecalculationStatus(
            today = today,
            nextSunday = nextSunday(today, dueToday),
            lastRecommendationDate = lastDate,
            dueToday = dueToday,
        )
    }

    private fun lastRecommendationDate(goals: Goals): LocalDate? {
        val applied = goals.history.mapNotNull { it.generatedDate ?: it.effectiveDate }.maxOrNull()
        val pending = goals.recommendation?.generatedDate
        return listOfNotNull(applied, pending).maxOrNull()
    }

    private fun nextSunday(today: LocalDate, dueToday: Boolean): LocalDate {
        return if (dueToday) today else today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }
}
