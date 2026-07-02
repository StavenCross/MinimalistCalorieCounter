package com.makstuff.minimalistcaloriecounter.classes

import androidx.health.connect.client.records.MealType
import java.time.LocalDateTime

data class QuickImportNutrients(
    val energy: Double,
    val carbohydrate: Double,
    val sugar: Double,
    val protein: Double,
    val fat: Double,
    val saturatedFat: Double,
    val fiber: Double,
) {
    val appCarbohydrate: Double
        get() = (carbohydrate - fiber).coerceAtLeast(0.0)

    init {
        require(energy >= 0.0) { "Calories must be zero or greater." }
        require(carbohydrate >= 0.0) { "Carbs must be zero or greater." }
        require(sugar >= 0.0) { "Sugar must be zero or greater." }
        require(protein >= 0.0) { "Protein must be zero or greater." }
        require(fat >= 0.0) { "Fat must be zero or greater." }
        require(saturatedFat >= 0.0) { "Saturated fat must be zero or greater." }
        require(fiber >= 0.0) { "Fiber must be zero or greater." }
        require(carbohydrate + EPSILON >= fiber) { "Fiber cannot exceed total carbs." }
        require(fat + EPSILON >= saturatedFat) { "Saturated fat cannot exceed total fat." }
        require(appCarbohydrate + EPSILON >= sugar) { "Sugar cannot exceed carbs after fiber." }
    }

    fun toAppValues(): List<Double> = listOf(
        energy,
        appCarbohydrate,
        sugar,
        protein,
        fat,
        saturatedFat,
        fiber,
        0.0,
    )

    fun times(factor: Double): QuickImportNutrients = QuickImportNutrients(
        energy = energy * factor,
        carbohydrate = carbohydrate * factor,
        sugar = sugar * factor,
        protein = protein * factor,
        fat = fat * factor,
        saturatedFat = saturatedFat * factor,
        fiber = fiber * factor,
    )

    companion object {
        private const val EPSILON = 0.000001
    }
}

data class QuickImportFood(
    val amountText: String,
    val name: String,
    val grams: Double?,
    val nutrients: QuickImportNutrients,
) {
    val databaseName: String
        get() = QuickImportSanitizer.databaseName(name)

    fun nutrientsPer100g(): QuickImportNutrients? {
        val gramsValue = grams ?: return null
        if (gramsValue <= 0.0) return null
        return nutrients.times(100.0 / gramsValue)
    }
}

data class QuickImportMeal(
    val foods: List<QuickImportFood>,
    val totals: QuickImportNutrients,
)

data class QuickImportHealthPayload(
    val dateTime: LocalDateTime,
    val mealType: Int,
    val energy: Double,
    val energyFromFat: Double,
    val totalCarbohydrate: Double,
    val sugar: Double,
    val protein: Double,
    val totalFat: Double,
    val saturatedFat: Double,
    val dietaryFiber: Double,
    val name: String,
)

enum class QuickImportMealType(val label: String, val healthConnectValue: Int) {
    Breakfast("Breakfast", MealType.MEAL_TYPE_BREAKFAST),
    Lunch("Lunch", MealType.MEAL_TYPE_LUNCH),
    Dinner("Dinner", MealType.MEAL_TYPE_DINNER),
    Snack("Snack", MealType.MEAL_TYPE_SNACK);

    companion object {
        fun inferFrom(dateTime: LocalDateTime): QuickImportMealType {
            return when (dateTime.hour) {
                in 1..10 -> Breakfast
                in 11..14 -> Lunch
                in 15..22 -> Dinner
                else -> Snack
            }
        }
    }
}

data class QuickImportDatabaseEntryDraft(
    val name: String,
    val grams: Double,
    val nutrientsPer100g: QuickImportNutrients,
)

data class QuickImportCommitOptions(
    val addFoodsToDatabase: Boolean,
    val addFoodsToDay: Boolean,
    val writeHealthConnect: Boolean,
)

data class QuickImportCommitPlan(
    val foodDrafts: List<QuickImportDatabaseEntryDraft>,
    val healthPayloads: List<QuickImportHealthPayload>,
)

sealed class QuickImportHealthWriteResult {
    data object Success : QuickImportHealthWriteResult()
    data object HealthConnectUnavailable : QuickImportHealthWriteResult()
    data object PermissionsMissing : QuickImportHealthWriteResult()
    data class Failed(val message: String) : QuickImportHealthWriteResult()
}

data class QuickImportResult(
    val databaseEntriesAdded: Int,
    val dayFoodsAdded: Int,
    val healthWriteResult: QuickImportHealthWriteResult?,
)

object QuickImportParser {
    private const val OUNCES_TO_GRAMS = 28.3495

    fun parse(input: String): QuickImportMeal {
        val normalized = input.trim()
        require(normalized.isNotEmpty()) { "Paste a meal blurb before importing." }

        val records = splitRecords(normalized)
        require(records.isNotEmpty()) { "No food entries were found." }

        val foods = mutableListOf<QuickImportFood>()
        var mealTotals: QuickImportNutrients? = null

        records.forEach { record ->
            val semicolonIndex = record.indexOf(';')
            require(semicolonIndex >= 0) { "Each entry must use ';' between the food name and nutrition values." }

            val title = record.substring(0, semicolonIndex).trim()
            val nutrientText = record.substring(semicolonIndex + 1).trim()
            val nutrients = QuickImportNutritionReader.parse(nutrientText)

            if (title.equals("Meal totals", ignoreCase = true)) {
                mealTotals = nutrients
            } else {
                foods.add(parseFood(title, nutrients))
            }
        }

        require(foods.isNotEmpty()) { "No food entries were found." }

        return QuickImportMeal(
            foods = foods,
            totals = mealTotals ?: sumFoods(foods),
        )
    }

    private fun splitRecords(input: String): List<String> {
        val compact = input
            .replace(Regex("\\s*\\n+\\s*"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (compact.isEmpty()) return emptyList()

        return Regex("(?<=\\.)\\s+(?=(?:\\d+(?:\\.\\d+)?\\s*(?:g|oz)\\b|Meal totals\\s*;))", RegexOption.IGNORE_CASE)
            .split(compact)
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotEmpty() }
    }

    private fun parseFood(title: String, nutrients: QuickImportNutrients): QuickImportFood {
        val amountMatch = Regex("^\\s*(\\d+(?:\\.\\d+)?)\\s*(g|oz)\\s+(.+)$", RegexOption.IGNORE_CASE).find(title)
        if (amountMatch != null) {
            val amount = amountMatch.groupValues[1].toDouble()
            val unit = amountMatch.groupValues[2].lowercase()
            val name = amountMatch.groupValues[3].trim()
            val grams = if (unit == "oz") amount * OUNCES_TO_GRAMS else amount
            return QuickImportFood(
                amountText = "${amountMatch.groupValues[1]} ${amountMatch.groupValues[2]}",
                name = name,
                grams = grams,
                nutrients = nutrients,
            )
        }

        return QuickImportFood(
            amountText = "",
            name = title.trim(),
            grams = null,
            nutrients = nutrients,
        )
    }

    private fun sumFoods(foods: List<QuickImportFood>): QuickImportNutrients {
        return QuickImportNutrients(
            energy = foods.sumOf { it.nutrients.energy },
            carbohydrate = foods.sumOf { it.nutrients.carbohydrate },
            sugar = foods.sumOf { it.nutrients.sugar },
            protein = foods.sumOf { it.nutrients.protein },
            fat = foods.sumOf { it.nutrients.fat },
            saturatedFat = foods.sumOf { it.nutrients.saturatedFat },
            fiber = foods.sumOf { it.nutrients.fiber },
        )
    }
}

object QuickImportNutritionReader {
    fun parse(text: String): QuickImportNutrients {
        return QuickImportNutrients(
            energy = readField(text, "Calories"),
            carbohydrate = readField(text, "Carbs"),
            sugar = readField(text, "Sugar"),
            protein = readField(text, "Protein"),
            fat = readField(text, "Fat"),
            saturatedFat = readField(text, "Sat Fat"),
            fiber = readField(text, "Fiber"),
        )
    }

    private fun readField(text: String, label: String): Double {
        val pattern = Regex(
            "(?:^|[,;])\\s*${Regex.escape(label)}\\s+(-?\\d+(?:\\.\\d+)?)\\s*(?:g|mg|kcal)?\\b",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(text)
        require(match != null) { "Missing $label." }
        return match.groupValues[1].toDouble()
    }
}

object QuickImportMapper {
    fun toHealthPayloads(
        meal: QuickImportMeal,
        dateTime: LocalDateTime,
        mealType: QuickImportMealType,
    ): List<QuickImportHealthPayload> {
        return meal.foods.mapIndexed { index, food ->
            toHealthPayload(food, dateTime.plusSeconds(index.toLong()), mealType)
        }
    }

    fun toHealthPayload(
        food: QuickImportFood,
        dateTime: LocalDateTime,
        mealType: QuickImportMealType,
    ): QuickImportHealthPayload {
        val displayName = listOf(food.amountText, food.name)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return QuickImportHealthPayload(
            dateTime = dateTime,
            mealType = mealType.healthConnectValue,
            energy = food.nutrients.energy,
            energyFromFat = food.nutrients.fat * 9.0,
            totalCarbohydrate = food.nutrients.carbohydrate,
            sugar = food.nutrients.sugar,
            protein = food.nutrients.protein,
            totalFat = food.nutrients.fat,
            saturatedFat = food.nutrients.saturatedFat,
            dietaryFiber = food.nutrients.fiber,
            name = displayName.ifBlank { food.name },
        )
    }
}

object QuickImportPlanner {
    private const val MACRO_GRAMS_PER_100G_EPSILON = 0.0001

    fun build(
        meal: QuickImportMeal,
        options: QuickImportCommitOptions,
        dateTime: LocalDateTime,
        mealType: QuickImportMealType = QuickImportMealType.inferFrom(dateTime),
        existingDatabaseNames: Set<String> = emptySet(),
    ): QuickImportCommitPlan {
        require(options.addFoodsToDatabase || options.addFoodsToDay || options.writeHealthConnect) {
            "Choose at least one import destination."
        }

        val needsFoodDrafts = options.addFoodsToDatabase || options.addFoodsToDay
        val foodDrafts = if (needsFoodDrafts) {
            buildFoodDrafts(meal, existingDatabaseNames)
        } else {
            emptyList()
        }

        return QuickImportCommitPlan(
            foodDrafts = foodDrafts,
            healthPayloads = if (options.writeHealthConnect) {
                QuickImportMapper.toHealthPayloads(meal, dateTime, mealType)
            } else {
                emptyList()
            },
        )
    }

    private fun buildFoodDrafts(
        meal: QuickImportMeal,
        existingDatabaseNames: Set<String>,
    ): List<QuickImportDatabaseEntryDraft> {
        val usedNames = existingDatabaseNames.toMutableSet()
        return meal.foods.map { food ->
            val grams = food.grams
            require(grams != null && grams > 0.0) {
                "Food '${food.name}' needs a gram or ounce amount before it can be added locally."
            }
            val per100g = food.nutrientsPer100g()
            require(per100g != null) {
                "Food '${food.name}' needs a valid weight before it can be added locally."
            }
            validatePer100g(food, per100g)
            val uniqueName = makeUniqueName(food.databaseName, usedNames)
            usedNames.add(uniqueName)
            QuickImportDatabaseEntryDraft(
                name = uniqueName,
                grams = grams,
                nutrientsPer100g = per100g,
            )
        }
    }

    private fun validatePer100g(food: QuickImportFood, nutrients: QuickImportNutrients) {
        val macroWeight = nutrients.appCarbohydrate + nutrients.protein + nutrients.fat + nutrients.fiber
        require(macroWeight <= 100.0 + MACRO_GRAMS_PER_100G_EPSILON) {
            "Food '${food.name}' has more than 100g of carbs, protein, fat, and fiber per 100g."
        }
    }

    private fun makeUniqueName(baseName: String, usedNames: Set<String>): String {
        if (!usedNames.contains(baseName)) return baseName
        var suffix = 2
        while (true) {
            val candidate = "$baseName $suffix"
            if (!usedNames.contains(candidate)) return candidate
            suffix++
        }
    }
}

object QuickImportSanitizer {
    fun databaseName(input: String): String {
        val cleaned = input.replace(',', ' ').replace(Regex("\\s+"), " ").trim()
        if (cleaned.isEmpty()) return "Quick Import Food"
        val capitalized = cleaned.replaceFirstChar { it.uppercase() }
        return if (capitalized.first() in 'A'..'Z') capitalized else "Food $capitalized"
    }
}
