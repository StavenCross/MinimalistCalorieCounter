package com.makstuff.minimalistcaloriecounter.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NutritionWidgetRefreshWorkerTest {
    @Test
    fun delayUntilNextRolloverTargetsTwoMinutesAfterTomorrowMidnight() {
        val zone = ZoneId.of("America/Chicago")
        val now = ZonedDateTime.of(2026, 7, 7, 23, 59, 0, 0, zone)

        assertEquals(180_000L, delayUntilNextRolloverMillis(now))
    }

    @Test
    fun delayUntilNextRolloverNeverReturnsZero() {
        val zone = ZoneId.of("America/Chicago")
        val now = ZonedDateTime.of(2026, 7, 8, 0, 2, 0, 0, zone)

        assertTrue(delayUntilNextRolloverMillis(now) > 0L)
    }

    @Test
    fun rolloverReschedulesOnlyWhileWidgetsExist() {
        assertTrue(shouldScheduleNextWidgetRefresh(1))
        assertFalse(shouldScheduleNextWidgetRefresh(0))
    }
}
