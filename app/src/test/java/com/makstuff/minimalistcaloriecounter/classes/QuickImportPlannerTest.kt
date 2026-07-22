package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QuickImportPlannerTest {
    @Test
    fun createsFoodDraftsBeforeLocalCommit() {
        val meal = QuickImportParser.parse(
            "100g rice; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g. " +
                "Meal totals; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g."
        )

        val plan = QuickImportPlanner.build(
            meal = meal,
            options = QuickImportCommitOptions(
                addFoodsToDatabase = true,
                addFoodsToDay = true,
                writeHealthConnect = false,
            ),
            dateTime = LocalDateTime.of(2026, 7, 2, 18, 0),
            mealType = QuickImportMealType.Dinner,
        )

        assertEquals(1, plan.foodDrafts.size)
        assertEquals("Rice", plan.foodDrafts[0].name)
        assertClose(100.0, plan.foodDrafts[0].grams)
        assertClose(27.0, plan.foodDrafts[0].nutrientsPer100g.appCarbohydrate)
        assertEquals(0, plan.healthPayloads.size)
    }

    @Test
    fun makesDuplicateDatabaseNamesUnique() {
        val meal = QuickImportParser.parse(
            "100g rice; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g. " +
                "Meal totals; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g."
        )

        val plan = QuickImportPlanner.build(
            meal = meal,
            options = QuickImportCommitOptions(
                addFoodsToDatabase = true,
                addFoodsToDay = false,
                writeHealthConnect = false,
            ),
            dateTime = LocalDateTime.of(2026, 7, 2, 18, 0),
            existingDatabaseNames = setOf("Rice"),
        )

        assertEquals("Rice 2", plan.foodDrafts[0].name)
    }

    @Test
    fun healthOnlyImportCreatesFoodPayloadsAndDoesNotRequireFoodWeights() {
        val meal = QuickImportParser.parse(
            "restaurant meal; Calories 500, Fat 20g, Sat Fat 5g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 40g, Fiber 6g, Sugar 9g, Added Sugar 0g, Protein 30g. " +
                "Meal totals; Calories 500, Fat 20g, Sat Fat 5g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 40g, Fiber 6g, Sugar 9g, Added Sugar 0g, Protein 30g."
        )

        val plan = QuickImportPlanner.build(
            meal = meal,
            options = QuickImportCommitOptions(
                addFoodsToDatabase = false,
                addFoodsToDay = false,
                writeHealthConnect = true,
            ),
            dateTime = LocalDateTime.of(2026, 7, 2, 18, 0),
        )

        assertEquals(0, plan.foodDrafts.size)
        assertEquals(1, plan.healthPayloads.size)
        assertEquals("restaurant meal", plan.healthPayloads[0].name)
        assertEquals(QuickImportMealType.Dinner.healthConnectValue, plan.healthPayloads[0].mealType)
        assertClose(40.0, plan.healthPayloads[0].totalCarbohydrate)
    }

    @Test
    fun defaultImportLogsServingLabelsWithoutLegacyGramDrafts() {
        val meal = MealImportContract.fromJson(MealImportContractTest.STARBUCKS_BREAKFAST_JSON).meal

        val plan = QuickImportPlanner.build(
            meal = meal,
            options = QuickImportCommitOptions(
                addFoodsToDatabase = true,
                addFoodsToDay = true,
                writeHealthConnect = true,
            ),
            dateTime = LocalDateTime.of(2026, 7, 21, 9, 0),
            mealType = QuickImportMealType.Breakfast,
        )

        assertEquals(0, plan.foodDrafts.size)
        assertEquals(true, plan.localDestinationsSkipped)
        assertEquals(2, plan.healthPayloads.size)
        assertEquals(
            listOf("1 venti Starbucks Venti Caffè Latte", "1 croissant Starbucks Butter Croissant"),
            plan.healthPayloads.map { it.name },
        )
        assertClose(527.0, plan.healthPayloads.sumOf { it.energy })
    }

    @Test
    fun roundedLabelRelationshipsDoNotBlockServingImport() {
        val meal = MealImportContract.fromJson(
            """
            {
              "action":"log_meal",
              "items":[
                {"name":"Rounded restaurant item","amount":"1 serving","calories":100,"carbs_g":4,"fiber_g":5,"sugar_g":5,"fat_g":1,"sat_fat_g":2}
              ]
            }
            """.trimIndent(),
        ).meal

        val plan = QuickImportPlanner.build(
            meal = meal,
            options = QuickImportCommitOptions(true, true, true),
            dateTime = LocalDateTime.of(2026, 7, 21, 9, 0),
        )

        assertEquals(0, plan.foodDrafts.size)
        assertEquals(true, plan.localDestinationsSkipped)
        assertEquals(1, plan.healthPayloads.size)
        assertClose(5.0, plan.healthPayloads.single().sugar)
    }

    @Test
    fun healthPayloadsExcludeMealTotalsToAvoidDoubleCounting() {
        val meal = QuickImportParser.parse(
            "100g rice; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g. " +
                "100g beans; Calories 120, Fat 0.5g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 22g, Fiber 7g, Sugar 1g, Added Sugar 0g, Protein 8g. " +
                "Meal totals; Calories 250, Fat 0.8g, Sat Fat 0.2g, Trans Fat 0g, Cholesterol 0mg, Sodium 2mg, Carbs 50g, Fiber 8g, Sugar 1.1g, Added Sugar 0g, Protein 10.7g."
        )

        val plan = QuickImportPlanner.build(
            meal = meal,
            options = QuickImportCommitOptions(
                addFoodsToDatabase = false,
                addFoodsToDay = false,
                writeHealthConnect = true,
            ),
            dateTime = LocalDateTime.of(2026, 7, 2, 18, 0),
            mealType = QuickImportMealType.Snack,
        )

        assertEquals(2, plan.healthPayloads.size)
        assertClose(250.0, plan.healthPayloads.sumOf { it.energy })
        assertEquals(listOf("100 g rice", "100 g beans"), plan.healthPayloads.map { it.name })
        assertEquals(
            listOf(QuickImportMealType.Snack.healthConnectValue, QuickImportMealType.Snack.healthConnectValue),
            plan.healthPayloads.map { it.mealType },
        )
    }

    @Test
    fun localImportRequiresFoodWeights() {
        val meal = QuickImportParser.parse(
            "restaurant meal; Calories 500, Fat 20g, Sat Fat 5g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 40g, Fiber 6g, Sugar 9g, Added Sugar 0g, Protein 30g. " +
                "Meal totals; Calories 500, Fat 20g, Sat Fat 5g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 40g, Fiber 6g, Sugar 9g, Added Sugar 0g, Protein 30g."
        )

        assertThrows(IllegalArgumentException::class.java) {
            QuickImportPlanner.build(
                meal = meal,
                options = QuickImportCommitOptions(
                    addFoodsToDatabase = true,
                    addFoodsToDay = false,
                    writeHealthConnect = false,
                ),
                dateTime = LocalDateTime.of(2026, 7, 2, 18, 0),
            )
        }
    }

    @Test
    fun rejectsImportWithNoDestinations() {
        val meal = QuickImportParser.parse(
            "100g rice; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g."
        )

        assertThrows(IllegalArgumentException::class.java) {
            QuickImportPlanner.build(
                meal = meal,
                options = QuickImportCommitOptions(
                    addFoodsToDatabase = false,
                    addFoodsToDay = false,
                    writeHealthConnect = false,
                ),
                dateTime = LocalDateTime.of(2026, 7, 2, 18, 0),
            )
        }
    }

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        if (abs(expected - actual) > tolerance) {
            throw AssertionError("Expected <$expected>, actual <$actual>, tolerance <$tolerance>")
        }
    }
}
