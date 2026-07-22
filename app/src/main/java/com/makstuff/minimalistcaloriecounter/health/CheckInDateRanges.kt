package com.makstuff.minimalistcaloriecounter.health

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class CheckInDateRange(
    val label: String,
    val filenameToken: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

fun previousCalendarWeek(today: LocalDate = LocalDate.now()): CheckInDateRange {
    val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val start = thisWeekMonday.minusWeeks(1)
    return CheckInDateRange(
        label = "Weekly check-in",
        filenameToken = "weekly",
        startDate = start,
        endDate = start.plusDays(6),
    )
}

fun previousCalendarMonth(today: LocalDate = LocalDate.now()): CheckInDateRange {
    val previousMonth = today.withDayOfMonth(1).minusMonths(1)
    return CheckInDateRange(
        label = "Monthly check-in",
        filenameToken = "monthly",
        startDate = previousMonth,
        endDate = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()),
    )
}

fun customCheckInRange(startDate: LocalDate, endDate: LocalDate): CheckInDateRange {
    val first = minOf(startDate, endDate)
    val last = maxOf(startDate, endDate)
    return CheckInDateRange(
        label = "Custom check-in",
        filenameToken = "custom",
        startDate = first,
        endDate = last,
    )
}
