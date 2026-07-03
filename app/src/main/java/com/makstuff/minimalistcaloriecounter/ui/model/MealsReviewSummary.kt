package com.makstuff.minimalistcaloriecounter.ui.model

import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import java.time.LocalDate

fun mealsDaySummaryText(
    date: LocalDate,
    meals: List<HealthConnectNutritionMeal>,
    targets: MacroTargets,
): String {
    val totals = meals.sumNutrition()
    val groups = mealGroups(meals)
    return buildString {
        appendLine("Meals for $date")
        appendLine("- Foods: ${meals.size}")
        appendLine("- Calories: ${formatCalories(totals.energy)}")
        appendLine("- Protein: ${formatGrams(totals.protein)}")
        appendLine("- Carbs: ${formatGrams(totals.carbohydrate)}")
        appendLine("- Fat: ${formatGrams(totals.fat)}")
        appendLine("- Fiber: ${formatGrams(totals.fiber)}")
        appendLine()
        appendLine("Targets")
        appendTarget("Calories", totals.energy, targets.calories, "kcal")
        appendTarget("Protein", totals.protein, targets.protein, "g")
        appendTarget("Carbs", totals.carbohydrate, targets.carbs, "g")
        appendTarget("Fat", totals.fat, targets.fat, "g")
        appendTarget("Fiber", totals.fiber, targets.fiber, "g")
        if (groups.isNotEmpty()) {
            appendLine()
            appendLine("Meals")
            groups.forEach { group -> appendLine("- ${mealGroupSummaryText(group).replace("\n", "\n  ")}") }
        }
    }.trimEnd()
}

fun mealGroupSummaryText(group: NutritionMealGroup): String {
    val totals = group.foods.sumNutrition()
    return buildString {
        appendLine("${group.label}: ${group.foods.size} foods, ${formatCalories(totals.energy)}")
        appendLine("Protein ${formatGrams(totals.protein)}, carbs ${formatGrams(totals.carbohydrate)}, fat ${formatGrams(totals.fat)}, fiber ${formatGrams(totals.fiber)}")
        group.foods.forEach { food ->
            appendLine("- ${food.name}: ${formatCalories(food.energy)}")
        }
    }.trimEnd()
}

private fun StringBuilder.appendTarget(label: String, current: Double, target: Double?, unit: String) {
    if (target == null) {
        appendLine("- $label: no target set")
        return
    }
    val remaining = (target - current).coerceAtLeast(0.0)
    appendLine("- $label: ${current.toFormattedString(true)}/${target.toFormattedString(true)} $unit, ${remaining.toFormattedString(true)} $unit remaining")
}

private fun formatCalories(value: Double): String = "${value.toFormattedString(true)} kcal"

private fun formatGrams(value: Double): String = "${value.toFormattedString(true)}g"
