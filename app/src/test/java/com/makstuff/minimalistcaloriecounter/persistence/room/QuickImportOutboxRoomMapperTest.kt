package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxState
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickImportOutboxRoomMapperTest {
    @Test
    fun roundTripsOutboxSeedWithOrderedPayloads() {
        val item = QuickImportOutboxItem(
            id = "abc123",
            createdAt = LocalDateTime.of(2026, 7, 3, 12, 1),
            intendedDateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
            mealType = QuickImportMealType.Lunch,
            sourceTextHash = "hash",
            mealSummary = "2 foods, 500 kcal",
            foodCount = 2,
            state = QuickImportOutboxState.Retrying,
            attemptCount = 3,
            lastAttemptAt = LocalDateTime.of(2026, 7, 3, 12, 2),
            lastErrorMessage = null,
            healthPayloads = listOf(
                QuickImportHealthPayload(
                    dateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
                    mealType = QuickImportMealType.Lunch.healthConnectValue,
                    energy = 500.0,
                    energyFromFat = 90.0,
                    totalCarbohydrate = 55.0,
                    sugar = 4.0,
                    protein = 35.0,
                    totalFat = 10.0,
                    saturatedFat = 2.0,
                    dietaryFiber = 8.0,
                    name = "Lunch bowl",
                    clientRecordId = "mcc-add-meal-abc123-0",
                )
            ),
        )

        val seed = QuickImportOutboxRoomMapper.toSeed(item)
        val restored = QuickImportOutboxRoomMapper.fromSeed(seed.copy(payloads = seed.payloads.reversed()))

        assertEquals(item, restored)
    }
}
