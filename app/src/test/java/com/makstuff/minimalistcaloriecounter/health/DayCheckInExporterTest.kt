package com.makstuff.minimalistcaloriecounter.health

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayCheckInExporterTest {
    @Test
    fun filenameIncludesSelectedDate() {
        assertEquals(
            "meals_check_in_2026-07-03.txt",
            dayCheckInFilename(LocalDate.of(2026, 7, 3)),
        )
    }
}
