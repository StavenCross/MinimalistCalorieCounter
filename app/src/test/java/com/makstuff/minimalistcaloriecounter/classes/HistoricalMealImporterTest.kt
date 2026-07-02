package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalMealImporterTest {
    @Test
    fun parsesExactAmPmTimestampAndBuildsHealthPayload() {
        val preview = HistoricalMealImporter.parseCsv(
            listOf(
                header,
                listOf(
                    "row-1",
                    "2026-05-03 10:41 AM",
                    "Breakfast plate",
                    "food_1",
                    "100g oatmeal",
                    "100",
                    "g",
                    "100",
                    "150",
                    "3",
                    "0.5",
                    "0",
                    "0",
                    "2",
                    "27",
                    "4",
                    "1",
                    "0",
                    "5",
                    "",
                    "",
                    "",
                    "database",
                    "",
                ),
            )
        )

        assertEquals(1, preview.validRows)
        assertEquals(0, preview.skippedRows)
        val food = preview.foods.single()
        assertEquals(LocalDateTime.of(2026, 5, 3, 10, 41), food.dateTime)
        assertEquals(QuickImportMealType.Breakfast, food.mealType)
        assertTrue(food.clientRecordId.startsWith(HistoricalMealImporter.CLIENT_RECORD_ID_PREFIX))
        val payload = food.toHealthPayload()
        assertEquals("100g oatmeal", payload.name)
        assertClose(150.0, payload.energy)
        assertClose(27.0, payload.totalCarbohydrate)
        assertClose(4.0, payload.dietaryFiber)
        assertClose(27.0, payload.energyFromFat)
    }

    @Test
    fun parsesTwentyFourHourTimestamp() {
        val dateTime = HistoricalMealImporter.parseDateTime("2026-05-03 18:26", "Snack")

        assertEquals(LocalDateTime.of(2026, 5, 3, 18, 26), dateTime)
    }

    @Test
    fun explicitMealLabelUsesFallbackTimeAndMealType() {
        val preview = HistoricalMealImporter.parseCsv(
            listOf(
                header,
                validRow(dateTime = "2026-06-10 Lunch", mealName = "Lunch", logId = "lunch-1"),
                validRow(dateTime = "2026-06-10 Dinner", mealName = "Dinner", logId = "dinner-1"),
                validRow(dateTime = "2026-06-10 Snack", mealName = "Snack", logId = "snack-1"),
            )
        )

        assertEquals(3, preview.validRows)
        assertEquals(LocalDateTime.of(2026, 6, 10, 12, 0), preview.foods[0].dateTime)
        assertEquals(QuickImportMealType.Lunch, preview.foods[0].mealType)
        assertEquals(LocalDateTime.of(2026, 6, 10, 18, 0), preview.foods[1].dateTime)
        assertEquals(QuickImportMealType.Dinner, preview.foods[1].mealType)
        assertEquals(LocalDateTime.of(2026, 6, 10, 15, 30), preview.foods[2].dateTime)
        assertEquals(QuickImportMealType.Snack, preview.foods[2].mealType)
    }

    @Test
    fun bareDateFallsBackToNoonAndTimeInference() {
        val dateTime = HistoricalMealImporter.parseDateTime("2026-06-14", "")

        assertEquals(LocalDateTime.of(2026, 6, 14, 12, 0), dateTime)
        assertEquals(QuickImportMealType.Lunch, QuickImportMealType.inferFrom(dateTime!!))
    }

    @Test
    fun staggersRowsWithinSameMeal() {
        val preview = HistoricalMealImporter.parseCsv(
            listOf(
                header,
                validRow(logId = "row-1"),
                validRow(logId = "row-2"),
            )
        )

        assertEquals(LocalDateTime.of(2026, 5, 3, 10, 41), preview.foods[0].dateTime)
        assertEquals(LocalDateTime.of(2026, 5, 3, 10, 41, 1), preview.foods[1].dateTime)
    }

    @Test
    fun skipsBlankAndInvalidRowsWithIssues() {
        val preview = HistoricalMealImporter.parseCsv(
            listOf(
                header,
                List(header.size) { "" },
                validRow(logId = "row-1"),
                validRow(logId = "row-2").toMutableList().also {
                    it[header.indexOf("calories")] = ""
                },
            )
        )

        assertEquals(1, preview.validRows)
        assertEquals(1, preview.skippedRows)
        assertEquals(4, preview.issues.single().rowNumber)
        assertTrue(preview.issues.single().message.contains("Missing calories"))
    }

    @Test
    fun fingerprintChangesWhenMacrosChange() {
        val base = QuickImportNutrients(100.0, 10.0, 1.0, 5.0, 3.0, 1.0, 2.0)
        val changed = base.copy(protein = 6.0)
        val dateTime = LocalDateTime.of(2026, 5, 3, 10, 41)

        val first = HistoricalMealImporter.fingerprint(dateTime, QuickImportMealType.Breakfast, "food", base)
        val second = HistoricalMealImporter.fingerprint(dateTime, QuickImportMealType.Breakfast, "food", changed)

        assertTrue(first != second)
    }

    @Test
    fun clientRecordIdIsStableAcrossRepeatedImports() {
        val rows = listOf(header, validRow(logId = "stable-row"))

        val first = HistoricalMealImporter.parseCsv(rows).foods.single()
        val second = HistoricalMealImporter.parseCsv(rows).foods.single()

        assertEquals(first.clientRecordId, second.clientRecordId)
        assertEquals(first.fingerprint, second.fingerprint)
    }

    @Test
    fun sameTimestampSameContentCreatesSameFingerprint() {
        val nutrients = QuickImportNutrients(150.0, 27.0, 1.0, 5.0, 3.0, 0.5, 4.0)
        val imported = HistoricalMealImporter.fingerprint(
            dateTime = LocalDateTime.of(2026, 5, 3, 10, 41),
            mealType = QuickImportMealType.Breakfast,
            name = "100g oatmeal",
            nutrients = nutrients,
        )
        val existing = HistoricalMealImporter.fingerprint(
            dateTime = LocalDateTime.of(2026, 5, 3, 10, 41),
            mealType = QuickImportMealType.Breakfast,
            name = " 100g   OATMEAL ",
            nutrients = nutrients,
        )

        assertEquals(imported, existing)
    }

    private fun validRow(
        logId: String = "row-1",
        dateTime: String = "2026-05-03 10:41 AM",
        mealName: String = "Breakfast plate",
    ): List<String> = listOf(
        logId,
        dateTime,
        mealName,
        "food_1",
        "100g oatmeal",
        "100",
        "g",
        "100",
        "150",
        "3",
        "0.5",
        "0",
        "0",
        "2",
        "27",
        "4",
        "1",
        "0",
        "5",
        "",
        "",
        "",
        "database",
        "",
    )

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        if (abs(expected - actual) > tolerance) {
            throw AssertionError("Expected <$expected>, actual <$actual>, tolerance <$tolerance>")
        }
    }

    private val header = listOf(
        "log_id",
        "date_time",
        "meal_name",
        "food_id",
        "item_description",
        "amount",
        "unit",
        "amount_g",
        "calories",
        "fat_g",
        "sat_fat_g",
        "trans_fat_g",
        "cholesterol_mg",
        "sodium_mg",
        "carbs_g",
        "fiber_g",
        "sugar_g",
        "added_sugar_g",
        "protein_g",
        "calcium_mg",
        "iron_mg",
        "potassium_mg",
        "source_status",
        "notes",
    )
}
