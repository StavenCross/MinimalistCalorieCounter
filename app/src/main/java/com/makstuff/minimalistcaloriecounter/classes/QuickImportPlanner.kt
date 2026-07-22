package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime

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
        val localDraftIssue = meal.foods.firstNotNullOfOrNull { it.legacyLocalDraftIssue() }
        val foodDrafts = when {
            !needsFoodDrafts -> emptyList()
            localDraftIssue == null -> buildFoodDrafts(meal, existingDatabaseNames)
            options.writeHealthConnect -> emptyList()
            else -> throw IllegalArgumentException(localDraftIssue)
        }

        return QuickImportCommitPlan(
            foodDrafts = foodDrafts,
            healthPayloads = if (options.writeHealthConnect) {
                QuickImportMapper.toHealthPayloads(meal, dateTime, mealType)
            } else {
                emptyList()
            },
            localDestinationsSkipped = needsFoodDrafts && localDraftIssue != null,
        )
    }

    private fun buildFoodDrafts(
        meal: QuickImportMeal,
        existingDatabaseNames: Set<String>,
    ): List<QuickImportDatabaseEntryDraft> {
        val usedNames = existingDatabaseNames.toMutableSet()
        return meal.foods.map { food ->
            val issue = food.legacyLocalDraftIssue()
            require(issue == null) { issue.orEmpty() }
            val grams = requireNotNull(food.grams)
            val per100g = requireNotNull(food.nutrientsPer100g())
            val uniqueName = makeUniqueName(food.databaseName, usedNames)
            usedNames.add(uniqueName)
            QuickImportDatabaseEntryDraft(
                name = uniqueName,
                grams = grams,
                nutrientsPer100g = per100g,
            )
        }
    }

    /**
     * The legacy database stores per-100g food definitions, while Health Connect stores complete
     * servings. If any item cannot be represented faithfully in the legacy model, the whole meal
     * stays serving-based so a default import never becomes a partial local meal.
     */
    private fun QuickImportFood.legacyLocalDraftIssue(): String? {
        val gramsValue = grams
        if (gramsValue == null || gramsValue <= 0.0) {
            return "Food '$name' needs a gram or ounce amount before it can be added locally."
        }
        val per100g = nutrientsPer100g()
            ?: return "Food '$name' needs a valid weight before it can be added locally."
        val macroWeight = per100g.appCarbohydrate + per100g.protein + per100g.fat + per100g.fiber
        if (macroWeight > 100.0 + MACRO_GRAMS_PER_100G_EPSILON) {
            return "Food '$name' has more than 100g of carbs, protein, fat, and fiber per 100g."
        }
        if (per100g.appCarbohydrate + MACRO_GRAMS_PER_100G_EPSILON < per100g.sugar) {
            return "Food '$name' cannot be represented in the local per-100g database because sugar exceeds stored carbs."
        }
        if (per100g.fat + MACRO_GRAMS_PER_100G_EPSILON < per100g.saturatedFat) {
            return "Food '$name' cannot be represented in the local per-100g database because saturated fat exceeds fat."
        }
        return null
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
