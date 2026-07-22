package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Base64

data class MealImportRequest(
    val source: String,
    val action: String,
    val idempotencyKey: String?,
    val dateTime: LocalDateTime,
    val mealType: QuickImportMealType,
    val meal: QuickImportMeal,
) {
    val sourceText: String
        get() = QuickImportFormatter.text(meal)
}

object MealImportContract {
    const val ACTION_LOG_MEAL = "log_meal"
    const val DEFAULT_CONFIRMATION_REQUIRED = true

    fun fromBase64UrlPayload(payload: String): MealImportRequest {
        require(payload.length <= MAX_ENCODED_PAYLOAD_CHARS) { "Meal import payload is too large." }
        val padded = payload.padEnd(payload.length + (4 - payload.length % 4) % 4, '=')
        val json = String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
        return fromJson(json)
    }

    fun fromJson(json: String): MealImportRequest {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "Meal import JSON is too large." }
        val root = SimpleJsonParser.parseObject(json)
        val source = root.string("source")?.takeIf { it.isNotBlank() } ?: "external"
        val action = root.string("action") ?: error("Meal import action is required.")
        require(action == ACTION_LOG_MEAL) { "Unsupported meal import action: $action." }
        val date = root.string("date")?.let(LocalDate::parse) ?: LocalDate.now()
        val mealType = root.string("meal")?.toMealType() ?: QuickImportMealType.inferFrom(date.atTime(LocalTime.now()))
        val time = root.string("time")?.let(LocalTime::parse) ?: mealType.defaultImportTime()
        val itemValues = root.array("items")?.values.orEmpty()
        require(itemValues.size <= MAX_MEAL_ITEMS) { "Meal import contains too many items." }
        val foods = itemValues.mapIndexed { index, value ->
            (value as? SimpleJson.Obj)?.toFood(index) ?: error("Meal item ${index + 1} must be an object.")
        }
        require(foods.isNotEmpty()) { "Meal import must contain at least one item." }
        val itemTotals = foods.sumNutrients()
        val providedTotals = root.obj("totals")?.toNutrients()
        if (providedTotals != null) validateTotals(providedTotals, itemTotals)
        val meal = QuickImportMeal(foods = foods, totals = providedTotals ?: itemTotals)
        return MealImportRequest(
            source = source,
            action = action,
            idempotencyKey = root.string("idempotency_key"),
            dateTime = LocalDateTime.of(date, time),
            mealType = mealType,
            meal = meal,
        )
    }

    private fun SimpleJson.Obj.toFood(index: Int): QuickImportFood {
        val name = string("name")?.takeIf { it.isNotBlank() } ?: error("Meal item ${index + 1} is missing a name.")
        val amount = string("amount").orEmpty()
        val title = listOf(amount, name).filter { it.isNotBlank() }.joinToString(" ")
        val parsed = QuickImportAmountParser.parseLeading(title)
        return QuickImportFood(
            amountText = parsed?.amountText ?: amount,
            name = parsed?.name ?: name,
            grams = parsed?.grams ?: QuickImportAmountParser.gramsFromAmountText(amount),
            nutrients = toNutrients(),
        )
    }

    private fun SimpleJson.Obj.toNutrients(): QuickImportNutrients = QuickImportNutrients(
        energy = macro("calories"),
        carbohydrate = macro("carbs_g"),
        sugar = macro("sugar_g"),
        protein = macro("protein_g"),
        fat = macro("fat_g"),
        saturatedFat = macro("sat_fat_g"),
        fiber = macro("fiber_g"),
    )

    private fun SimpleJson.Obj.macro(name: String): Double = number(name) ?: 0.0

    private fun List<QuickImportFood>.sumNutrients(): QuickImportNutrients = QuickImportNutrients(
        energy = sumOf { it.nutrients.energy },
        carbohydrate = sumOf { it.nutrients.carbohydrate },
        sugar = sumOf { it.nutrients.sugar },
        protein = sumOf { it.nutrients.protein },
        fat = sumOf { it.nutrients.fat },
        saturatedFat = sumOf { it.nutrients.saturatedFat },
        fiber = sumOf { it.nutrients.fiber },
    )

    private fun validateTotals(provided: QuickImportNutrients, calculated: QuickImportNutrients) {
        val mismatched = listOf(
            "calories" to (provided.energy to calculated.energy),
            "protein_g" to (provided.protein to calculated.protein),
            "carbs_g" to (provided.carbohydrate to calculated.carbohydrate),
            "fat_g" to (provided.fat to calculated.fat),
            "fiber_g" to (provided.fiber to calculated.fiber),
        ).firstOrNull { (_, values) -> kotlin.math.abs(values.first - values.second) > TOTALS_TOLERANCE }
        require(mismatched == null) {
            "Meal totals do not match items for ${mismatched!!.first}."
        }
    }

    private fun String.toMealType(): QuickImportMealType {
        return when (lowercase()) {
            "breakfast" -> QuickImportMealType.Breakfast
            "lunch" -> QuickImportMealType.Lunch
            "dinner" -> QuickImportMealType.Dinner
            "snack" -> QuickImportMealType.Snack
            else -> error("Unsupported meal type: $this.")
        }
    }

    private fun QuickImportMealType.defaultImportTime(): LocalTime {
        return when (this) {
            QuickImportMealType.Breakfast -> LocalTime.of(9, 0)
            QuickImportMealType.Lunch -> LocalTime.of(12, 0)
            QuickImportMealType.Dinner -> LocalTime.of(18, 0)
            QuickImportMealType.Snack -> LocalTime.now()
        }
    }

    private const val TOTALS_TOLERANCE = 1.0
    private const val MAX_ENCODED_PAYLOAD_CHARS = 64_000
    private const val MAX_JSON_BYTES = 48_000
    private const val MAX_MEAL_ITEMS = 100
}
