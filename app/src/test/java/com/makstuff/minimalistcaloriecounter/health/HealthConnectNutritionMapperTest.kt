package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.classes.NutritionFoodEditDraft
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class HealthConnectNutritionMapperTest {
    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun quickImportPayloadMapsToNutritionRecordAndViewerMeal() {
        val payload = QuickImportHealthPayload(
            dateTime = LocalDateTime.of(2026, 7, 3, 12, 15),
            mealType = MealType.MEAL_TYPE_LUNCH,
            energy = 508.0,
            energyFromFat = 54.0,
            totalCarbohydrate = 56.0,
            sugar = 0.2,
            protein = 51.9,
            totalFat = 6.0,
            saturatedFat = 1.7,
            dietaryFiber = 2.0,
            name = "150 g chicken breast",
            clientRecordId = "mcc-test-1",
        )

        val record = payload.toNutritionRecord(zone)
        val meal = record.toHealthConnectNutritionMeal(zone)

        assertEquals("mcc-test-1", meal.clientRecordId)
        assertEquals(LocalDateTime.of(2026, 7, 3, 12, 15), meal.startTime)
        assertEquals(LocalDateTime.of(2026, 7, 3, 12, 16), meal.endTime)
        assertEquals("150 g chicken breast", meal.name)
        assertEquals(508.0, meal.energy, 0.001)
        assertEquals(54.0, meal.energyFromFat!!, 0.001)
        assertEquals(56.0, meal.totalCarbohydrate, 0.001)
        assertEquals(0.2, meal.sugar, 0.001)
        assertEquals(51.9, meal.protein, 0.001)
        assertEquals(6.0, meal.totalFat, 0.001)
        assertEquals(1.7, meal.saturatedFat, 0.001)
        assertEquals(2.0, meal.dietaryFiber, 0.001)
        assertEquals(MealType.MEAL_TYPE_LUNCH, meal.mealType)
    }

    @Test
    fun nutritionSignatureFingerprintUsesNutritionRecordContents() {
        val payload = QuickImportHealthPayload(
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
            name = "100 g oats",
        )

        val first = payload.toNutritionRecord(zone).toNutritionSignature(zone).fingerprint
        val second = payload.copy(protein = 6.0).toNutritionRecord(zone).toNutritionSignature(zone).fingerprint

        assert(first.isNotBlank())
        assert(first != second)
    }

    @Test
    fun editedViewerMealMapsToReplacementPayload() {
        val meal = HealthConnectNutritionMeal(
            recordId = "record-1",
            clientRecordId = "client-1",
            startTime = LocalDateTime.of(2026, 7, 3, 21, 0),
            endTime = LocalDateTime.of(2026, 7, 3, 21, 1),
            name = "Whiskey",
            energy = 100.0,
            energyFromFat = 0.0,
            totalCarbohydrate = 0.0,
            sugar = 0.0,
            protein = 0.0,
            totalFat = 0.0,
            saturatedFat = 0.0,
            dietaryFiber = 0.0,
            mealType = MealType.MEAL_TYPE_SNACK,
        )

        val payload = meal.toEditedHealthPayload(
            draft = NutritionFoodEditDraft(
                name = "Whiskey pour",
                energy = 120.0,
                totalCarbohydrate = 1.0,
                protein = 0.0,
                totalFat = 2.0,
                dietaryFiber = 0.0,
                sugar = 0.5,
                saturatedFat = 0.1,
            ),
            clientRecordId = "mcc-meal-edit-test",
        )

        assertEquals("Whiskey pour", payload.name)
        assertEquals("mcc-meal-edit-test", payload.clientRecordId)
        assertEquals(LocalDateTime.of(2026, 7, 3, 21, 0), payload.dateTime)
        assertEquals(MealType.MEAL_TYPE_SNACK, payload.mealType)
        assertEquals(120.0, payload.energy, 0.001)
        assertEquals(18.0, payload.energyFromFat, 0.001)
        assertEquals(1.0, payload.totalCarbohydrate, 0.001)
        assertEquals(0.5, payload.sugar, 0.001)
    }
}
