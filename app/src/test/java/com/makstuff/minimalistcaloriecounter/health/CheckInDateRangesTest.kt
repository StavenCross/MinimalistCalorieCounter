package com.makstuff.minimalistcaloriecounter.health

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CheckInDateRangesTest {
    @Test
    fun previousCalendarWeekUsesPriorMondayThroughSunday() {
        val range = previousCalendarWeek(LocalDate.of(2026, 7, 6))

        assertEquals("Weekly check-in", range.label)
        assertEquals("weekly", range.filenameToken)
        assertEquals(LocalDate.of(2026, 6, 29), range.startDate)
        assertEquals(LocalDate.of(2026, 7, 5), range.endDate)
    }

    @Test
    fun previousCalendarMonthUsesPriorMonthBoundaries() {
        val range = previousCalendarMonth(LocalDate.of(2026, 7, 6))

        assertEquals("Monthly check-in", range.label)
        assertEquals("monthly", range.filenameToken)
        assertEquals(LocalDate.of(2026, 6, 1), range.startDate)
        assertEquals(LocalDate.of(2026, 6, 30), range.endDate)
    }

    @Test
    fun customCheckInRangeNormalizesBackwardsDates() {
        val range = customCheckInRange(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 1))

        assertEquals(LocalDate.of(2026, 7, 1), range.startDate)
        assertEquals(LocalDate.of(2026, 7, 5), range.endDate)
    }
}
