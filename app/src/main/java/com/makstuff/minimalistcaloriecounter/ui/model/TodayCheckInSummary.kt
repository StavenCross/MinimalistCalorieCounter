package com.makstuff.minimalistcaloriecounter.ui.model

import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import java.time.LocalDate

fun todayCheckInSummary(
    date: LocalDate,
    meals: List<HealthConnectNutritionMeal>,
    targets: MacroTargets,
    profile: GoalProfile,
): String {
    val totals = meals.sumNutrition()
    return buildString {
        appendLine("Nutrition check-in for $date")
        appendLine()
        appendLine("Logged meals")
        if (meals.isEmpty()) {
            appendLine("- No meals logged.")
        } else {
            mealGroups(meals).forEach { group ->
                val groupTotals = group.foods.sumNutrition()
                appendLine("- ${group.label}: ${group.foods.size} foods, ${formatCalories(groupTotals.energy)}")
                group.foods.forEach { food ->
                    appendLine("  - ${food.name}: ${formatCalories(food.energy)}")
                }
            }
        }
        appendLine()
        appendLine("Daily totals")
        appendNutrients(totals)
        appendLine()
        appendLine("Targets and remaining")
        appendTarget("Calories", totals.energy, targets.calories, "kcal")
        appendTarget("Protein", totals.protein, targets.protein, "g")
        appendTarget("Carbs", totals.carbohydrate, targets.carbs, "g")
        appendTarget("Fat", totals.fat, targets.fat, "g")
        appendTarget("Fiber", totals.fiber, targets.fiber, "g")
        appendBodyMetrics(profile)
    }.trimEnd()
}

private fun StringBuilder.appendNutrients(nutrients: QuickImportNutrients) {
    appendLine("- Calories: ${formatCalories(nutrients.energy)}")
    appendLine("- Protein: ${formatGrams(nutrients.protein)}")
    appendLine("- Carbs: ${formatGrams(nutrients.carbohydrate)}")
    appendLine("- Fat: ${formatGrams(nutrients.fat)}")
    appendLine("- Fiber: ${formatGrams(nutrients.fiber)}")
}

private fun StringBuilder.appendTarget(label: String, current: Double, target: Double?, unit: String) {
    if (target == null) {
        appendLine("- $label: no target set")
        return
    }
    val remaining = (target - current).coerceAtLeast(0.0)
    appendLine("- $label: ${current.toFormattedString(true)}/${target.toFormattedString(true)} $unit, ${remaining.toFormattedString(true)} $unit remaining")
}

private fun StringBuilder.appendBodyMetrics(profile: GoalProfile) {
    val metrics = listOfNotNull(
        profile.weightKg.value?.let { "Weight ${formatPounds(it)}" },
        profile.bodyFatPercent.value?.let { "Body fat ${it.toFormattedString(true)}%" },
        profile.leanMassOrCalculatedKg()?.let { "Lean mass ${formatPounds(it)}" },
    )
    if (metrics.isNotEmpty()) {
        appendLine()
        appendLine("Body metrics")
        metrics.forEach { appendLine("- $it") }
    }
}

private fun formatCalories(value: Double): String = "${value.toFormattedString(true)} kcal"

private fun formatGrams(value: Double): String = "${value.toFormattedString(true)}g"

private fun formatPounds(valueKg: Double): String = "${(valueKg * 2.2046226218).toFormattedString(true)} lb"
