package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDate
import java.time.LocalDateTime

object GoalsCsv {
    private const val VERSION = "1"

    fun defaultRows(): List<List<String>> = toRows(Goals())

    fun toRows(goals: Goals): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        rows += listOf("version", VERSION)
        rows += listOf(
            "profile",
            goals.profile.birthday?.toString().orEmpty(),
            goals.profile.sex?.name.orEmpty(),
            goals.profile.activityLevel.name,
            goals.profile.weightLossTarget.name,
        )
        rows += measurementRow("heightCm", goals.profile.heightCm)
        rows += measurementRow("weightKg", goals.profile.weightKg)
        rows += measurementRow("bodyFatPercent", goals.profile.bodyFatPercent)
        rows += measurementRow("leanMassKg", goals.profile.leanMassKg)
        GoalMacro.entries.forEach { macro ->
            rows += listOf(
                "target",
                macro.name,
                goals.currentTargets.valueFor(macro)?.toString().orEmpty(),
                (macro in goals.currentTargets.lockedMacros).toString(),
            )
        }
        goals.history.sortedBy { it.effectiveDate }.forEach { entry ->
            rows += listOf(
                "history",
                entry.effectiveDate.toString(),
                entry.source,
                entry.targets.calories?.toString().orEmpty(),
                entry.targets.protein?.toString().orEmpty(),
                entry.targets.carbs?.toString().orEmpty(),
                entry.targets.fat?.toString().orEmpty(),
                entry.targets.fiber?.toString().orEmpty(),
            )
        }
        goals.recommendation?.let { recommendation ->
            rows += listOf(
                "recommendation",
                recommendation.generatedDate.toString(),
                recommendation.bmr.toString(),
                recommendation.tdee.toString(),
                recommendation.warning.orEmpty(),
                recommendation.targets.calories?.toString().orEmpty(),
                recommendation.targets.protein?.toString().orEmpty(),
                recommendation.targets.carbs?.toString().orEmpty(),
                recommendation.targets.fat?.toString().orEmpty(),
                recommendation.targets.fiber?.toString().orEmpty(),
            )
        }
        return rows
    }

    fun fromRows(rows: List<List<String>>): Goals {
        if (rows.isEmpty()) return Goals()
        var profile = GoalProfile()
        var targets = MacroTargets()
        val history = mutableListOf<GoalHistoryEntry>()
        var recommendation: GoalRecommendation? = null

        rows.forEach { row ->
            when (row.getOrNull(0)) {
                "profile" -> {
                    profile = profile.copy(
                        birthday = row.getOrNull(1).parseDateOrNull(),
                        sex = GoalSex.fromName(row.getOrNull(2)),
                        activityLevel = ActivityLevel.fromName(row.getOrNull(3)),
                        weightLossTarget = WeeklyWeightLossTarget.fromName(row.getOrNull(4)),
                    )
                }
                "measurement" -> {
                    val measurement = GoalMeasurement(
                        value = row.getOrNull(2).parseDoubleOrNull(),
                        locked = row.getOrNull(3)?.toBooleanStrictOrNull() ?: false,
                        source = row.getOrNull(4)?.let { runCatching { GoalValueSource.valueOf(it) }.getOrNull() } ?: GoalValueSource.Manual,
                        updatedAt = row.getOrNull(5).parseDateTimeOrNull(),
                    )
                    profile = when (row.getOrNull(1)) {
                        "heightCm" -> profile.copy(heightCm = measurement)
                        "weightKg" -> profile.copy(weightKg = measurement)
                        "bodyFatPercent" -> profile.copy(bodyFatPercent = measurement)
                        "leanMassKg" -> profile.copy(leanMassKg = measurement)
                        else -> profile
                    }
                }
                "target" -> {
                    val macro = row.getOrNull(1)?.let { runCatching { GoalMacro.valueOf(it) }.getOrNull() }
                    if (macro != null) {
                        targets = targets.withValue(
                            macro = macro,
                            value = row.getOrNull(2).parseDoubleOrNull(),
                            lock = row.getOrNull(3)?.toBooleanStrictOrNull() ?: false,
                        )
                    }
                }
                "history" -> {
                    val date = row.getOrNull(1).parseDateOrNull()
                    if (date != null) {
                        history += GoalHistoryEntry(
                            effectiveDate = date,
                            source = row.getOrNull(2).orEmpty(),
                            targets = MacroTargets(
                                calories = row.getOrNull(3).parseDoubleOrNull(),
                                protein = row.getOrNull(4).parseDoubleOrNull(),
                                carbs = row.getOrNull(5).parseDoubleOrNull(),
                                fat = row.getOrNull(6).parseDoubleOrNull(),
                                fiber = row.getOrNull(7).parseDoubleOrNull(),
                            )
                        )
                    }
                }
                "recommendation" -> {
                    val date = row.getOrNull(1).parseDateOrNull()
                    val bmr = row.getOrNull(2).parseDoubleOrNull()
                    val tdee = row.getOrNull(3).parseDoubleOrNull()
                    if (date != null && bmr != null && tdee != null) {
                        recommendation = GoalRecommendation(
                            generatedDate = date,
                            bmr = bmr,
                            tdee = tdee,
                            warning = row.getOrNull(4).orEmpty().ifBlank { null },
                            targets = MacroTargets(
                                calories = row.getOrNull(5).parseDoubleOrNull(),
                                protein = row.getOrNull(6).parseDoubleOrNull(),
                                carbs = row.getOrNull(7).parseDoubleOrNull(),
                                fat = row.getOrNull(8).parseDoubleOrNull(),
                                fiber = row.getOrNull(9).parseDoubleOrNull(),
                            )
                        )
                    }
                }
            }
        }
        return Goals(
            profile = profile,
            currentTargets = targets,
            history = history,
            recommendation = recommendation,
        )
    }

    private fun measurementRow(name: String, measurement: GoalMeasurement): List<String> = listOf(
        "measurement",
        name,
        measurement.value?.toString().orEmpty(),
        measurement.locked.toString(),
        measurement.source.name,
        measurement.updatedAt?.toString().orEmpty(),
    )

    private fun String?.parseDoubleOrNull(): Double? = this?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
    private fun String?.parseDateOrNull(): LocalDate? = this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    private fun String?.parseDateTimeOrNull(): LocalDateTime? = this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
}
