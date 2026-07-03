package com.makstuff.minimalistcaloriecounter.classes

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
