package com.makstuff.minimalistcaloriecounter.health

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoricalMealHealthConnectImportTest {
    @Test
    fun importsFullHistoricalMealLogAndSkipsExistingJulyFirstDuplicates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val csv = File(context.getExternalFilesDir(null), "meal-log-health-connect-import-2026-07-02-cleaned.csv")
        assumeTrue("Missing full historical CSV at ${csv.absolutePath}", csv.exists())

        val rows = csvReader().readAll(csv.inputStream())
        val preview = HistoricalMealImporter.parseCsv(rows)

        assertEquals(preview.issues.joinToString { "${it.rowNumber}: ${it.message}" }, 500, preview.validRows)
        assertEquals(emptyList<String>(), preview.issues.map { it.message })
        assertEquals(LocalDate.of(2026, 5, 3), preview.startDate)
        assertEquals(LocalDate.of(2026, 7, 1), preview.endDate)

        val manager = HealthConnectManager(context)
        val julyFirstPreview = HistoricalMealImporter.parseCsv(csvReader().readAll(JULY_FIRST_CSV.byteInputStream()))
        val seedResult = manager.writeHistoricalMealFoods(julyFirstPreview.foods) { _, _, _ -> }
        assertTrue(
            "Expected July 1 seed success, got $seedResult",
            seedResult is HistoricalMealHealthConnectResult.Success,
        )

        val result = manager.writeHistoricalMealFoods(preview.foods) { _, _, _ -> }

        assertTrue(
            "Expected Health Connect success, got $result",
            result is HistoricalMealHealthConnectResult.Success,
        )
        result as HistoricalMealHealthConnectResult.Success
        assertEquals(500, result.written + result.skippedDuplicates)
        assertTrue("Expected at least the existing July 1 foods to be skipped, got $result", result.skippedDuplicates >= 12)

        val repeatedResult = manager.writeHistoricalMealFoods(preview.foods) { _, _, _ -> }
        assertTrue(
            "Expected repeated Health Connect import success, got $repeatedResult",
            repeatedResult is HistoricalMealHealthConnectResult.Success,
        )
        repeatedResult as HistoricalMealHealthConnectResult.Success
        assertEquals(0, repeatedResult.written)
        assertEquals(500, repeatedResult.skippedDuplicates)

        val totalImported = preview.dates.sumOf { date ->
            val readResult = manager.readNutritionMeals(date)
            assertTrue(
                "Expected Health Connect read success for $date, got $readResult",
                readResult is HealthConnectNutritionReadResult.Success,
            )
            readResult as HealthConnectNutritionReadResult.Success
            readResult.meals.count {
                it.clientRecordId?.startsWith(HistoricalMealImporter.CLIENT_RECORD_ID_PREFIX) == true
            }
        }
        assertEquals(500, totalImported)
    }

    @Test
    fun writesJulyFirstHistoricalMealsToHealthConnect() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rows = csvReader().readAll(JULY_FIRST_CSV.byteInputStream())
        val preview = HistoricalMealImporter.parseCsv(rows)

        assertEquals(preview.issues.joinToString { "${it.rowNumber}: ${it.message}" }, 12, preview.validRows)
        assertEquals(emptyList<String>(), preview.issues.map { it.message })

        val manager = HealthConnectManager(context)
        val result = manager.writeHistoricalMealFoods(preview.foods) { _, _, _ -> }

        assertTrue(
            "Expected Health Connect success, got $result",
            result is HistoricalMealHealthConnectResult.Success,
        )
        result as HistoricalMealHealthConnectResult.Success
        assertEquals(12, result.written + result.skippedDuplicates)

        val readResult = manager.readNutritionMeals(LocalDate.of(2026, 7, 1))
        assertTrue(
            "Expected Health Connect read success, got $readResult",
            readResult is HealthConnectNutritionReadResult.Success,
        )
        readResult as HealthConnectNutritionReadResult.Success
        val importedMeals = readResult.meals.filter {
            it.clientRecordId?.startsWith(HistoricalMealImporter.CLIENT_RECORD_ID_PREFIX) == true
        }
        assertEquals(12, importedMeals.size)
        assertEquals(1429.409, importedMeals.sumOf { it.energy }, 0.01)
    }

    private companion object {
        val JULY_FIRST_CSV = """
            log_id,date_time,meal_name,food_id,item_description,amount,unit,amount_g,calories,fat_g,sat_fat_g,trans_fat_g,cholesterol_mg,sodium_mg,carbs_g,fiber_g,sugar_g,added_sugar_g,protein_g,calcium_mg,iron_mg,potassium_mg,source_status,notes
            20260701_lunch_1,2026-07-01 12:00 PM,Lunch,russet_potato_baked_flesh_and_skin,193g whole russet potato,193,g,193,183.35,0.2509,0.04825,0,0,27.02,41.3792,4.439,2.0844,0,5.0759,34.74,2.0651,1061.5,database,Whole baked russet with flesh and skin; values from Foods tab.
            20260701_lunch_2,2026-07-01 12:00 PM,Lunch,i_cant_believe_its_not_butter_light,15g I Can't Believe It's Not Butter Lite,15,g,15,37.5,4.285714286,1.071428571,0,0,96.42857143,0,0,0,0,0,0,0,0,database,Light spread database label estimate.
            20260701_lunch_3,2026-07-01 12:00 PM,Lunch,chobani_nonfat_plain_greek_yogurt,2 oz Chobani nonfat plain Greek yogurt,2,oz,56.699,29.993771,0,0,0,3.345241,21.659018,1.984465,0,1.984465,0,5.329706,63.389482,0,83.404229,database,2 oz converted by weight using 1 oz = 28.3495g; values from Foods tab.
            20260701_lunch_4,2026-07-01 12:00 PM,Lunch,broccoli_raw,150g broccoli,150,g,150,51,0.6,0.06,0,0,49.5,9.9,3.9,2.55,0,4.2,70.5,1.05,474,database,Generic broccoli database row; assumes no added oil.
            20260701_lunch_5,2026-07-01 12:00 PM,Lunch,chicken_thigh_cooked_skinless,150g chicken thigh,150,g,150,313.5,16.35,4.5,0,202.5,142.5,0,0,0,0,38.85,18,1.5,358.5,database,Assumes cooked skinless chicken thigh meat with no added oil.
            20260701_lunch_6,2026-07-01 12:00 PM,Lunch,better_than_gravy_roasted_chicken,2 oz Better Than Gravy chicken gravy,2,oz,60,25,0.5,0,0,5,160,3,0,0,0,1,0,0.36,0,database,2 oz treated as 1/4 cup / 60g serving per Foods tab.
            20260701_snack_1,2026-07-01 3:00 PM,Snack,chobani_nonfat_plain_greek_yogurt,300g Chobani nonfat plain Greek yogurt,300,g,300,158.7,0,0,0,17.7,114.6,10.5,0,10.5,0,28.2,335.4,0,441.3,database,Chobani nonfat plain Greek yogurt; values from Foods tab.
            20260701_snack_2,2026-07-01 3:00 PM,Snack,blueberries_raw,100g blueberries,100,g,100,57,0.3,0.03,0,0,1,14.5,2.4,10,0,0.7,6,0.3,77,database,Generic raw blueberries; values from Foods tab.
            20260701_snack_3,2026-07-01 3:00 PM,Snack,heavy_whipping_cream_generic,15g heavy whipping cream,15,g,15,51,5.412,3.4545,0,16.95,4.05,0.426,0,0.426,0,0.426,9.9,0.015,14.25,database,Generic heavy whipping cream database row.
            20260701_dinner_1,2026-07-01 7:00 PM,Dinner,generic_sourdough_bread_enriched_white,67g sourdough bread,67,g,67,182.24,1.6214,0.33902,0,0,403.34,34.773,1.474,3.0954,0,7.236,34.84,2.6197,78.39,new lookup,Generic sourdough bread based on USDA-style per-100g values; brand/recipe not specified.
            20260701_dinner_2,2026-07-01 7:00 PM,Dinner,safe_catch_ahi_wild_yellowfin_tuna,6 oz Safe Catch Ahi Wild Yellowfin Tuna,6,oz,170.097,220.125529,1.000571,0,0,80.045647,440.251059,0,0,0,0,52.029671,0,0.720411,564.321812,new lookup,6 oz converted at 28.3495g/oz and scaled from 3 oz / 85g label serving.
            20260701_dinner_3,2026-07-01 7:00 PM,Dinner,hellmanns_olive_oil_mayonnaise_dressing,30g Hellmann's olive oil mayo,30,g,30,120,14,2,0,10,220,2,0,0,0,0,0,0,0,new lookup,30g equals 2 x 15g label servings.
        """.trimIndent()
    }
}
