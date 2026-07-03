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
            healthPayloads = listOf(
                QuickImportHealthPayload(
                    dateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
                    mealType = QuickImportMealType.Lunch.healthConnectValue,
                    energy = 389.0,
                    energyFromFat = 62.1,
                    totalCarbohydrate = 66.3,
                    sugar = 0.9,
                    protein = 16.9,
                    totalFat = 6.9,
                    saturatedFat = 1.2,
                    dietaryFiber = 10.6,
                    name = "100g test oats, cooked",
                    clientRecordId = "mcc-add-meal-abc123-0",
                )
            ),
        )

        val parsed = QuickImportOutboxCsv.fromRows(QuickImportOutboxCsv.toRows(listOf(item)))

        assertEquals(listOf(item), parsed)
    }

    @Test
    fun acceptsLegacyRowsWithoutPayloads() {
        val rows = listOf(
            listOf(
                "id",
                "createdAt",
                "intendedDateTime",
                "mealType",
                "sourceTextHash",
                "mealSummary",
                "foodCount",
                "state",
                "attemptCount",
                "lastAttemptAt",
                "lastErrorMessage",
            ),
            listOf(
                "abc123",
                "2026-07-03T12:01",
                "2026-07-03T12:00",
                "Lunch",
                "hash",
                "1 foods, 389 kcal",
                "1",
                "FailedHealthConnect",
                "1",
                "2026-07-03T12:02",
                "Missing permissions",
            ),
        )

        val parsed = QuickImportOutboxCsv.fromRows(rows)

        assertEquals(emptyList<QuickImportHealthPayload>(), parsed.single().healthPayloads)
    }

    @Test
    fun rejectsUnexpectedHeader() {
        assertThrows(IllegalArgumentException::class.java) {
            QuickImportOutboxCsv.fromRows(listOf(listOf("wrong")))
        }
    }
}
