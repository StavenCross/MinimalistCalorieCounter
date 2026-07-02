package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        )

        assertEquals(1, plan.foodDrafts.size)
        assertEquals("Rice", plan.foodDrafts[0].name)
        assertClose(100.0, plan.foodDrafts[0].grams)
        assertClose(27.0, plan.foodDrafts[0].nutrientsPer100g.appCarbohydrate)
        assertNull(plan.healthPayload)
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
    fun healthOnlyImportDoesNotRequireFoodWeights() {
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
        assertClose(40.0, plan.healthPayload!!.totalCarbohydrate)
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
