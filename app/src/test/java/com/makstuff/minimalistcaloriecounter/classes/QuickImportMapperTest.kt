package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime
import kotlin.math.abs
import org.junit.Test

class QuickImportMapperTest {
    @Test
    fun healthPayloadKeepsUsLabelTotalCarbs() {
        val meal = QuickImportParser.parse(
            "67g sourdough bread; Calories 182, Fat 1.6g, Sat Fat 0.3g, Trans Fat 0g, Cholesterol 0mg, Sodium 403mg, Carbs 34.8g, Fiber 1.5g, Sugar 3.1g, Added Sugar 0g, Protein 7.2g. " +
                "Meal totals; Calories 182, Fat 1.6g, Sat Fat 0.3g, Trans Fat 0g, Cholesterol 0mg, Sodium 403mg, Carbs 34.8g, Fiber 1.5g, Sugar 3.1g, Added Sugar 0g, Protein 7.2g."
        )

        val payload = QuickImportMapper.toHealthPayload(
            meal = meal,
            dateTime = LocalDateTime.of(2026, 7, 2, 12, 30),
        )

        assertClose(34.8, payload.totalCarbohydrate)
        assertClose(1.5, payload.dietaryFiber)
        assertClose(33.3, meal.totals.appCarbohydrate)
    }

    @Test
    fun appValuesStoreCarbsWithoutFiber() {
        val nutrients = QuickImportNutrients(
            energy = 100.0,
            carbohydrate = 20.0,
            sugar = 5.0,
            protein = 3.0,
            fat = 1.0,
            saturatedFat = 0.2,
            fiber = 4.0,
        )

        val appValues = nutrients.toAppValues()

        assertClose(100.0, appValues[0])
        assertClose(16.0, appValues[1])
        assertClose(5.0, appValues[2])
        assertClose(4.0, appValues[6])
    }

    @Test
    fun rejectsFiberGreaterThanTotalCarbs() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            QuickImportNutrients(
                energy = 100.0,
                carbohydrate = 2.0,
                sugar = 0.0,
                protein = 3.0,
                fat = 1.0,
                saturatedFat = 0.2,
                fiber = 4.0,
            )
        }
    }

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        if (abs(expected - actual) > tolerance) {
            throw AssertionError("Expected <$expected>, actual <$actual>, tolerance <$tolerance>")
        }
    }
}
