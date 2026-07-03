package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class HealthConnectNutritionDuplicatesTest {
    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun pendingPayloadsSkipExistingClientRecordIds() {
        val payload = payload(clientRecordId = "mcc-add-meal-abc-0")

        val pending = pendingNutritionPayloads(
            payloads = listOf(payload),
            existingSignatures = listOf(
                HealthConnectNutritionSignature(
                    clientRecordId = "mcc-add-meal-abc-0",
                    fingerprint = "other",
                )
            ),
        )

        assertTrue(pending.isEmpty())
    }

    @Test
    fun pendingPayloadsSkipExistingMatchingFingerprints() {
        val payload = payload(clientRecordId = "mcc-add-meal-new-0")
        val existing = payload.copy(clientRecordId = "mcc-add-meal-old-0")
            .toNutritionRecord(zone)
            .toNutritionSignature(zone)

        val pending = pendingNutritionPayloads(
            payloads = listOf(payload),
            existingSignatures = listOf(existing),
        )

        assertTrue(pending.isEmpty())
    }

    @Test
    fun pendingPayloadsKeepOnlyMissingRecords() {
        val existing = payload(name = "100 g oats", clientRecordId = "mcc-add-meal-abc-0")
        val missing = payload(name = "2 eggs", clientRecordId = "mcc-add-meal-abc-1")

        val pending = pendingNutritionPayloads(
            payloads = listOf(existing, missing),
            existingSignatures = listOf(existing.toNutritionRecord(zone).toNutritionSignature(zone)),
        )

        assertEquals(listOf(missing), pending)
    }

    @Test
    fun quickPayloadAndNutritionRecordUseSameFingerprint() {
        val payload = payload()

        assertEquals(
            payload.toNutritionSignature().fingerprint,
            payload.toNutritionRecord(zone).toNutritionSignature(zone).fingerprint,
        )
    }

    private fun payload(
        name: String = "100 g oats",
        clientRecordId: String = "mcc-add-meal-abc-0",
    ): QuickImportHealthPayload {
        return QuickImportHealthPayload(
            dateTime = LocalDateTime.of(2026, 7, 3, 9, 0),
            mealType = MealType.MEAL_TYPE_BREAKFAST,
            energy = 100.0,
            energyFromFat = 18.0,
            totalCarbohydrate = 10.0,
            sugar = 1.0,
            protein = 5.0,
            totalFat = 2.0,
            saturatedFat = 0.5,
            dietaryFiber = 3.0,
            name = name,
            clientRecordId = clientRecordId,
        )
    }
}
