package com.makstuff.minimalistcaloriecounter.classes

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class QuickImportParserTest {
    @Test
    fun parsesSingleParagraphMealTotalsFromChatGpt() {
        val meal = QuickImportParser.parse(
            "67g sourdough bread; Calories 182, Fat 1.6g, Sat Fat 0.3g, Trans Fat 0g, Cholesterol 0mg, Sodium 403mg, Carbs 34.8g, Fiber 1.5g, Sugar 3.1g, Added Sugar 0g, Protein 7.2g. " +
                "6 oz Safe Catch Ahi Wild Yellowfin Tuna; Calories 220, Fat 1.0g, Sat Fat 0g, Trans Fat 0g, Cholesterol 80mg, Sodium 440mg, Carbs 0g, Fiber 0g, Sugar 0g, Added Sugar 0g, Protein 52.0g. " +
                "30g Hellmann's Olive Oil Mayo; Calories 120, Fat 14.0g, Sat Fat 2.0g, Trans Fat 0g, Cholesterol 10mg, Sodium 220mg, Carbs 2.0g, Fiber 0g, Sugar 0g, Added Sugar 0g, Protein 0g. " +
                "Meal totals; Calories 522, Fat 16.6g, Sat Fat 2.3g, Trans Fat 0g, Cholesterol 90mg, Sodium 1064mg, Carbs 36.8g, Fiber 1.5g, Sugar 3.1g, Added Sugar 0g, Protein 59.3g."
        )

        assertEquals(3, meal.foods.size)
        assertClose(522.0, meal.totals.energy)
        assertClose(36.8, meal.totals.carbohydrate)
        assertClose(35.3, meal.totals.appCarbohydrate)
        assertClose(1.5, meal.totals.fiber)
        assertClose(59.3, meal.totals.protein)
        assertClose(16.6, meal.totals.fat)
        assertClose(2.3, meal.totals.saturatedFat)
    }

    @Test
    fun parsesNewlineSeparatedMealTotalsFromChatGpt() {
        val meal = QuickImportParser.parse(
            """
            300g bulgur wheat; Calories 249, Fat 0.7g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 2mg, Carbs 55.7g, Fiber 13.5g, Sugar 0.0g, Added Sugar 0g, Protein 9.2g.
            200g pork loin; Calories 384, Fat 17.6g, Sat Fat 5.7g, Trans Fat 0g, Cholesterol 160mg, Sodium 92mg, Carbs 0g, Fiber 0g, Sugar 0g, Added Sugar 0g, Protein 52.8g.
            150g broccoli; Calories 51, Fat 0.6g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 50mg, Carbs 9.9g, Fiber 3.9g, Sugar 2.6g, Added Sugar 0g, Protein 4.2g.
            3 oz Kevin's Natural Foods Kung Pao Sauce; Calories 134, Fat 4.5g, Sat Fat 0g, Trans Fat 0g, Cholesterol 0mg, Sodium 821mg, Carbs 20.9g, Fiber 1.5g, Sugar 11.9g, Added Sugar 0g, Protein 1.5g.
            Meal totals; Calories 818, Fat 23.4g, Sat Fat 5.9g, Trans Fat 0g, Cholesterol 160mg, Sodium 964mg, Carbs 86.5g, Fiber 18.9g, Sugar 14.5g, Added Sugar 0g, Protein 67.7g.
            """.trimIndent()
        )

        assertEquals(4, meal.foods.size)
        assertClose(818.0, meal.totals.energy)
        assertClose(86.5, meal.totals.carbohydrate)
        assertClose(67.6, meal.totals.appCarbohydrate)
        assertClose(18.9, meal.totals.fiber)
        assertClose(67.7, meal.totals.protein)
    }

    @Test
    fun computesFoodWeightsAndPer100gValuesForDatabaseEntries() {
        val meal = QuickImportParser.parse(
            "67g sourdough bread; Calories 182, Fat 1.6g, Sat Fat 0.3g, Trans Fat 0g, Cholesterol 0mg, Sodium 403mg, Carbs 34.8g, Fiber 1.5g, Sugar 3.1g, Added Sugar 0g, Protein 7.2g. " +
                "6 oz Safe Catch Ahi Wild Yellowfin Tuna; Calories 220, Fat 1.0g, Sat Fat 0g, Trans Fat 0g, Cholesterol 80mg, Sodium 440mg, Carbs 0g, Fiber 0g, Sugar 0g, Added Sugar 0g, Protein 52.0g. " +
                "Meal totals; Calories 402, Fat 2.6g, Sat Fat 0.3g, Trans Fat 0g, Cholesterol 80mg, Sodium 843mg, Carbs 34.8g, Fiber 1.5g, Sugar 3.1g, Added Sugar 0g, Protein 59.2g."
        )

        val bread = meal.foods[0]
        assertEquals("sourdough bread", bread.name)
        assertEquals("Sourdough bread", bread.databaseName)
        assertClose(67.0, bread.grams)
        val breadPer100g = bread.nutrientsPer100g()
        assertNotNull(breadPer100g)
        assertClose(271.641, breadPer100g!!.energy, tolerance = 0.001)

        val tuna = meal.foods[1]
        assertClose(170.097, tuna.grams, tolerance = 0.001)
        val tunaPer100g = tuna.nutrientsPer100g()
        assertNotNull(tunaPer100g)
        assertClose(129.338, tunaPer100g!!.energy, tolerance = 0.001)
    }

    @Test
    fun sumsFoodsWhenMealTotalsAreMissing() {
        val meal = QuickImportParser.parse(
            "210g Pork loin; Calories 403, Fat 18.5g, Sat Fat 6.0g, Trans Fat 0g, Cholesterol 168mg, Sodium 97mg, Carbs 0g, Fiber 0g, Sugar 0g, Added Sugar 0g, Protein 55.4g.\n" +
                "180g whole russet potato; Calories 171, Fat 0.2g, Sat Fat 0.0g, Trans Fat 0g, Cholesterol 0mg, Sodium 25mg, Carbs 38.6g, Fiber 4.1g, Sugar 1.9g, Added Sugar 0g, Protein 4.7g."
        )

        assertEquals(2, meal.foods.size)
        assertClose(574.0, meal.totals.energy)
        assertClose(38.6, meal.totals.carbohydrate)
        assertClose(34.5, meal.totals.appCarbohydrate)
        assertClose(60.1, meal.totals.protein)
        assertClose(18.7, meal.totals.fat)
    }

    @Test
    fun rejectsEmptyPaste() {
        assertThrows(IllegalArgumentException::class.java) {
            QuickImportParser.parse("   ")
        }
    }

    @Test
    fun rejectsFoodWithoutRequiredNutrients() {
        assertThrows(IllegalArgumentException::class.java) {
            QuickImportParser.parse("200g chicken; Calories 300, Protein 50g.")
        }
    }

    @Test
    fun sanitizesDatabaseNamesForLegacyDatabaseRules() {
        assertEquals("Food 2 eggs scrambled", QuickImportSanitizer.databaseName("2 eggs, scrambled"))
        assertEquals("Add Meal Food", QuickImportSanitizer.databaseName(" , "))
    }

    private fun assertClose(expected: Double, actual: Double?, tolerance: Double = 0.0001) {
        requireNotNull(actual)
        if (abs(expected - actual) > tolerance) {
            throw AssertionError("Expected <$expected>, actual <$actual>, tolerance <$tolerance>")
        }
    }
}
