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
