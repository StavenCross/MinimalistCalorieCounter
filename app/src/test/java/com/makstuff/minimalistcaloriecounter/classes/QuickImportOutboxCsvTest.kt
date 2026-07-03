package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QuickImportOutboxCsvTest {
    @Test
    fun roundTripsOutboxRows() {
        val item = QuickImportOutboxItem(
            id = "abc123",
            createdAt = LocalDateTime.of(2026, 7, 3, 12, 1),
            intendedDateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
            mealType = QuickImportMealType.Lunch,
            sourceTextHash = "hash",
            mealSummary = "2 foods, 500 kcal",
            foodCount = 2,
            state = QuickImportOutboxState.FailedHealthConnect,
            attemptCount = 2,
            lastAttemptAt = LocalDateTime.of(2026, 7, 3, 12, 2),
            lastErrorMessage = "Health Connect permissions are missing.",
        )

        val parsed = QuickImportOutboxCsv.fromRows(QuickImportOutboxCsv.toRows(listOf(item)))

        assertEquals(listOf(item), parsed)
    }

    @Test
    fun rejectsUnexpectedHeader() {
        assertThrows(IllegalArgumentException::class.java) {
            QuickImportOutboxCsv.fromRows(listOf(listOf("wrong")))
        }
    }
}

