package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime
import kotlin.math.abs
import org.junit.Test

class QuickImportMapperTest {
    @Test
    fun healthPayloadsUseIndividualFoodsAndKeepUsLabelTotalCarbs() {
        val meal = QuickImportParser.parse(
            "67g sourdough bread; Calories 182, Fat 1.6g, Sat Fat 0.3g, Trans Fat 0g, Cholesterol 0mg, Sodium 403mg, Carbs 34.8g, Fiber 1.5g, Sugar 3.1g, Added Sugar 0g, Protein 7.2g. " +
                "6 oz Safe Catch Ahi Wild Yellowfin Tuna; Calories 220, Fat 1.0g, Sat Fat 0g, Trans Fat 0g, Cholesterol 80mg, Sodium 440mg, Carbs 0g, Fiber 0g, Sugar 0g, Added Sugar 0g, Protein 52.0g. " +
                "Meal totals; Calories 402, Fat 2.6g, Sat Fat 0.3g, Trans Fat 0g, Cholesterol 80mg, Sodium 843mg, Carbs 34.8g, Fiber 1.5g, Sugar 3.1g, Added Sugar 0g, Protein 59.2g."
        )

        val payloads = QuickImportMapper.toHealthPayloads(
            meal = meal,
            dateTime = LocalDateTime.of(2026, 7, 2, 12, 30),
            mealType = QuickImportMealType.Lunch,
        )

        org.junit.Assert.assertEquals(2, payloads.size)
        org.junit.Assert.assertEquals("67 g sourdough bread", payloads[0].name)
        org.junit.Assert.assertEquals("6 oz Safe Catch Ahi Wild Yellowfin Tuna", payloads[1].name)
        assertClose(34.8, payloads[0].totalCarbohydrate)
        assertClose(1.5, payloads[0].dietaryFiber)
        assertClose(14.4, payloads[0].energyFromFat)
        assertClose(220.0, payloads[1].energy)
        assertClose(9.0, payloads[1].energyFromFat)
        org.junit.Assert.assertEquals(QuickImportMealType.Lunch.healthConnectValue, payloads[0].mealType)
        org.junit.Assert.assertEquals(QuickImportMealType.Lunch.healthConnectValue, payloads[1].mealType)
        assertClose(33.3, meal.totals.appCarbohydrate)
    }

    @Test
    fun healthPayloadsStaggerTimestampsToAvoidUiCollapsingRows() {
        val meal = QuickImportParser.parse(
            "100g rice; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g. " +
                "100g beans; Calories 120, Fat 0.5g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 22g, Fiber 7g, Sugar 1g, Added Sugar 0g, Protein 8g. " +
                "Meal totals; Calories 250, Fat 0.8g, Sat Fat 0.2g, Trans Fat 0g, Cholesterol 0mg, Sodium 2mg, Carbs 50g, Fiber 8g, Sugar 1.1g, Added Sugar 0g, Protein 10.7g."
        )

        val payloads = QuickImportMapper.toHealthPayloads(
            meal = meal,
            dateTime = LocalDateTime.of(2026, 7, 2, 12, 30),
            mealType = QuickImportMealType.Lunch,
        )

        org.junit.Assert.assertEquals(LocalDateTime.of(2026, 7, 2, 12, 30), payloads[0].dateTime)
        org.junit.Assert.assertEquals(LocalDateTime.of(2026, 7, 2, 12, 30, 1), payloads[1].dateTime)
    }

    @Test
    fun mealTypeInferenceUsesUserTimeWindows() {
        org.junit.Assert.assertEquals(
            QuickImportMealType.Snack,
            QuickImportMealType.inferFrom(LocalDateTime.of(2026, 7, 2, 0, 59)),
        )
        org.junit.Assert.assertEquals(
            QuickImportMealType.Breakfast,
            QuickImportMealType.inferFrom(LocalDateTime.of(2026, 7, 2, 1, 0)),
        )
        org.junit.Assert.assertEquals(
            QuickImportMealType.Breakfast,
            QuickImportMealType.inferFrom(LocalDateTime.of(2026, 7, 2, 10, 59)),
        )
        org.junit.Assert.assertEquals(
            QuickImportMealType.Lunch,
            QuickImportMealType.inferFrom(LocalDateTime.of(2026, 7, 2, 11, 0)),
        )
        org.junit.Assert.assertEquals(
            QuickImportMealType.Lunch,
            QuickImportMealType.inferFrom(LocalDateTime.of(2026, 7, 2, 14, 59)),
        )
        org.junit.Assert.assertEquals(
            QuickImportMealType.Dinner,
            QuickImportMealType.inferFrom(LocalDateTime.of(2026, 7, 2, 15, 0)),
        )
        org.junit.Assert.assertEquals(
            QuickImportMealType.Dinner,
            QuickImportMealType.inferFrom(LocalDateTime.of(2026, 7, 2, 22, 59)),
        )
        org.junit.Assert.assertEquals(
            QuickImportMealType.Snack,
            QuickImportMealType.inferFrom(LocalDateTime.of(2026, 7, 2, 23, 0)),
        )
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
