package com.makstuff.minimalistcaloriecounter.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Base64

class MealImportContractTest {
    @Test
    fun parsesStructuredMealJson() {
        val request = MealImportContract.fromJson(SAMPLE_JSON)

        assertEquals("chatgpt", request.source)
        assertEquals("abc-123", request.idempotencyKey)
        assertEquals(LocalDate.of(2026, 7, 8), request.dateTime.toLocalDate())
        assertEquals(LocalTime.NOON, request.dateTime.toLocalTime())
        assertEquals(QuickImportMealType.Lunch, request.mealType)
        assertEquals(2, request.meal.foods.size)
        assertEquals("Japanese curry", request.meal.foods[0].name)
        assertEquals("400 g", request.meal.foods[0].amountText)
        assertEquals(665.0, request.meal.totals.energy, 0.001)
    }

    @Test
    fun defaultsMissingMacrosToZero() {
        val request = MealImportContract.fromJson(
            """
            {
              "source": "chatgpt",
              "action": "log_meal",
              "date": "2026-07-08",
              "meal": "snack",
              "items": [{"name": "Whiskey", "amount": "3 fl oz", "calories": 220}]
            }
            """.trimIndent()
        )

        assertEquals(QuickImportMealType.Snack, request.mealType)
        assertEquals(0.0, request.meal.foods.single().nutrients.fat, 0.001)
        assertEquals(0.0, request.meal.foods.single().nutrients.saturatedFat, 0.001)
    }

    @Test
    fun decodesBase64UrlPayload() {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(SAMPLE_JSON.toByteArray())
        val request = MealImportContract.fromBase64UrlPayload(payload)

        assertEquals("chatgpt", request.source)
        assertEquals(QuickImportMealType.Lunch, request.mealType)
    }

    @Test
    fun acceptsReportedLunchWhereSugarAndFiberShareTotalCarbs() {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(REPORTED_LUNCH_JSON.toByteArray())

        val request = MealImportContract.fromBase64UrlPayload(payload)

        assertEquals(6, request.meal.foods.size)
        assertEquals("Broccolini", request.meal.foods.last().name)
        assertEquals(6.4, request.meal.foods.last().nutrients.carbohydrate, 0.001)
        assertEquals(6.1, request.meal.foods.last().nutrients.fiber, 0.001)
        assertEquals(0.9, request.meal.foods.last().nutrients.sugar, 0.001)
        assertEquals(654.0, request.meal.totals.energy, 0.001)
    }

    @Test
    fun acceptsStarbucksServingLabelsWithoutInventingGramWeights() {
        val request = MealImportContract.fromJson(STARBUCKS_BREAKFAST_JSON)

        assertEquals(QuickImportMealType.Breakfast, request.mealType)
        assertEquals(listOf("1 venti", "1 croissant"), request.meal.foods.map { it.amountText })
        assertEquals(listOf(null, null), request.meal.foods.map { it.grams })
        assertEquals(21.0, request.meal.foods.first().nutrients.sugar, 0.001)
        assertEquals(23.0, request.meal.foods.first().nutrients.carbohydrate, 0.001)
        assertEquals(527.0, request.meal.totals.energy, 0.001)
    }

    @Test
    fun formattedImportRoundTripsThroughExistingParser() {
        val request = MealImportContract.fromJson(SAMPLE_JSON)
        val parsed = QuickImportParser.parse(QuickImportFormatter.text(request.meal))

        assertEquals(request.meal.foods.size, parsed.foods.size)
        assertEquals(request.meal.totals.energy, parsed.totals.energy, 0.001)
    }

    @Test
    fun treatsProvidedTotalsAsAdvisoryAndSumsItems() {
        val request = MealImportContract.fromJson(
            """
            {
              "source": "chatgpt",
              "action": "log_meal",
              "items": [{"name": "Japanese curry", "amount": "400g", "calories": 425}],
              "totals": {"calories": 999}
            }
            """.trimIndent()
        )

        assertEquals(425.0, request.meal.totals.energy, 0.001)
    }

    @Test
    fun rejectsUnsupportedActions() {
        val result = runCatching {
            MealImportContract.fromJson("""{"action":"delete_meal","items":[]}""")
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsOversizedEncodedPayloadBeforeDecoding() {
        val result = runCatching {
            MealImportContract.fromBase64UrlPayload("A".repeat(64_001))
        }

        assertTrue(result.exceptionOrNull()?.message?.contains("too large") == true)
    }

    @Test
    fun rejectsExcessivelyNestedJson() {
        val nestedValue = "[".repeat(40) + "0" + "]".repeat(40)
        val result = runCatching {
            MealImportContract.fromJson(
                """{"action":"log_meal","items":[{"name":"Food","calories":1}],"extra":$nestedValue}"""
            )
        }

        assertTrue(result.exceptionOrNull()?.message?.contains("nested too deeply") == true)
    }

    @Test
    fun rejectsUnboundedMealItemCounts() {
        val items = List(101) { """{"name":"Food $it","calories":1}""" }.joinToString(",")
        val result = runCatching {
            MealImportContract.fromJson("""{"action":"log_meal","items":[$items]}""")
        }

        assertTrue(result.exceptionOrNull()?.message?.contains("too many items") == true)
    }

    @Test
    fun rejectsOverflowingNonFiniteNutritionNumbers() {
        val result = runCatching {
            MealImportContract.fromJson(
                """{"action":"log_meal","items":[{"name":"Food","calories":1e309}]}"""
            )
        }

        assertTrue(result.exceptionOrNull()?.message?.contains("finite") == true)
    }

    companion object {
        const val SAMPLE_JSON = """
            {
              "version": 1,
              "source": "chatgpt",
              "action": "log_meal",
              "idempotency_key": "abc-123",
              "date": "2026-07-08",
              "time": "12:00",
              "meal": "lunch",
              "items": [
                {
                  "name": "Japanese curry",
                  "amount": "400g",
                  "calories": 425,
                  "protein_g": 32,
                  "fat_g": 21,
                  "carbs_g": 25
                },
                {
                  "name": "White rice",
                  "amount": "186g",
                  "calories": 240,
                  "protein_g": 5,
                  "fat_g": 0.5,
                  "carbs_g": 52.1,
                  "fiber_g": 0.7
                }
              ],
              "totals": {
                "calories": 665,
                "protein_g": 37,
                "fat_g": 21.5,
                "carbs_g": 77.1,
                "fiber_g": 0.7
              }
            }
        """

        const val REPORTED_LUNCH_JSON = """
            {
              "source": "chatgpt",
              "action": "log_meal",
              "date": "2026-07-21",
              "meal": "lunch",
              "items": [
                {"name":"Pork loin","amount":"211g","calories":405,"protein_g":55.7,"carbs_g":0.0,"fat_g":18.6,"fiber_g":0.0,"sugar_g":0.0,"sat_fat_g":6.0},
                {"name":"Better Than Gravy chicken","amount":"60g","calories":25,"protein_g":1.0,"carbs_g":3.0,"fat_g":0.5,"fiber_g":0.0,"sugar_g":0.0,"sat_fat_g":0.0},
                {"name":"Russet potato","amount":"142g","calories":112,"protein_g":3.0,"carbs_g":25.7,"fat_g":0.1,"fiber_g":1.8,"sugar_g":0.9,"sat_fat_g":0.0},
                {"name":"H-E-B Fat Free Cheddar","amount":"15g","calories":24,"protein_g":4.8,"carbs_g":1.6,"fat_g":0.0,"fiber_g":0.0,"sugar_g":0.0,"sat_fat_g":0.0},
                {"name":"Country Crock Light Spread","amount":"15g","calories":38,"protein_g":0.0,"carbs_g":0.0,"fat_g":4.3,"fiber_g":0.0,"sugar_g":0.0,"sat_fat_g":1.1},
                {"name":"Broccolini","amount":"225g","calories":50,"protein_g":7.1,"carbs_g":6.4,"fat_g":1.1,"fiber_g":6.1,"sugar_g":0.9,"sat_fat_g":0.2}
              ],
              "totals":{"calories":654,"protein_g":71.6,"carbs_g":36.7,"fat_g":24.6,"fiber_g":7.9,"sugar_g":1.8,"sat_fat_g":7.3}
            }
        """

        const val STARBUCKS_BREAKFAST_JSON = """
            {
              "source":"chatgpt",
              "action":"log_meal",
              "date":"2026-07-21",
              "meal":"breakfast",
              "items":[
                {"name":"Starbucks Venti Caffè Latte","amount":"1 venti","calories":277,"protein_g":14.0,"carbs_g":23.0,"fat_g":14.0,"fiber_g":0.0,"sugar_g":21.0,"sat_fat_g":8.0},
                {"name":"Starbucks Butter Croissant","amount":"1 croissant","calories":250,"protein_g":5.0,"carbs_g":26.0,"fat_g":14.0,"fiber_g":1.0,"sugar_g":4.0,"sat_fat_g":8.0}
              ],
              "totals":{"calories":527,"protein_g":19.0,"carbs_g":49.0,"fat_g":28.0,"fiber_g":1.0,"sugar_g":25.0,"sat_fat_g":16.0}
            }
        """
    }
}
