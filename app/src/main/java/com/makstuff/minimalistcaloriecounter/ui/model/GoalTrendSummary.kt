package com.makstuff.minimalistcaloriecounter.ui.model

import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import java.time.LocalDate

data class GoalTrendCard(
    val label: String,
    val value: String,
    val detail: String,
)

fun goalBodyTrendCards(goals: Goals, today: LocalDate = LocalDate.now()): List<GoalTrendCard> {
    return listOfNotNull(
        bodyTrendCard(
            label = "Weight",
            unit = "lb",
            points = bodyPoints(goals, today, goals.profile.weightKg.value) { it.weightKg }.map { it.toPounds() },
        ),
        bodyTrendCard("Body fat", "%", bodyPoints(goals, today, goals.profile.bodyFatPercent.value) { it.bodyFatPercent }),
        bodyTrendCard(
            label = "Lean mass",
            unit = "lb",
            points = bodyPoints(goals, today, goals.profile.leanMassOrCalculatedKg()) { it.leanMassKg }.map { it.toPounds() },
        ),
    )
}

fun goalAdherenceCards(
    totals: QuickImportNutrients,
    targets: MacroTargets,
): List<GoalTrendCard> {
    return listOf(
        adherenceCard("Calories", totals.energy, targets.calories, "kcal"),
        adherenceCard("Protein", totals.protein, targets.protein, "g"),
        adherenceCard("Carbs", totals.carbohydrate, targets.carbs, "g"),
        adherenceCard("Fat", totals.fat, targets.fat, "g"),
        adherenceCard("Fiber", totals.fiber, targets.fiber, "g"),
    )
}

private data class TrendPoint(val date: LocalDate, val value: Double)

private fun bodyPoints(
    goals: Goals,
    today: LocalDate,
    currentValue: Double?,
    valueFor: (GoalHistoryEntry) -> Double?,
): List<TrendPoint> {
    val history = goals.history.mapNotNull { entry ->
        valueFor(entry)?.let { TrendPoint(entry.effectiveDate, it) }
    }
    val current = currentValue?.let { TrendPoint(today, it) }
    return (history + listOfNotNull(current)).distinctBy { it.date to it.value }.sortedBy { it.date }
}

private fun bodyTrendCard(label: String, unit: String, points: List<TrendPoint>): GoalTrendCard? {
    val latest = points.lastOrNull()?.value ?: return null
    val previous = points.dropLast(1).lastOrNull()?.value
    val detail = if (previous == null) {
        "No prior trend point"
    } else {
        val delta = latest - previous
        "${delta.signed()} $unit since prior check"
    }
    return GoalTrendCard(label, "${latest.toFormattedString(true)} $unit", detail)
}

private fun adherenceCard(label: String, value: Double, target: Double?, unit: String): GoalTrendCard {
    if (target == null || target <= 0.0) {
        return GoalTrendCard(label, "${value.toFormattedString(true)} $unit", "No target set")
    }
    val percent = value / target * 100.0
    return GoalTrendCard(
        label = label,
        value = "${percent.toFormattedString(true)}%",
        detail = "${value.toFormattedString(true)}/${target.toFormattedString(true)} $unit",
    )
}

private fun Double.signed(): String {
    val prefix = if (this > 0.0) "+" else ""
    return "$prefix${toFormattedString(true)}"
}

private fun TrendPoint.toPounds(): TrendPoint = copy(value = value * 2.2046226218)
