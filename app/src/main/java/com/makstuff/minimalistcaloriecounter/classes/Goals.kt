package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

enum class GoalSex(val label: String) {
    Male("Male"),
    Female("Female");

    companion object {
        fun fromName(value: String?): GoalSex? = entries.firstOrNull { it.name == value }
    }
}

enum class GoalValueSource {
    Manual,
    HealthConnect,
}

enum class ActivityLevel(val label: String, val factor: Double) {
    Sedentary("Sedentary", 1.2),
    LightlyActive("Lightly active", 1.375),
    ModeratelyActive("Moderately active", 1.55),
    VeryActive("Very active", 1.725),
    ExtraActive("Extra active", 1.9);

    companion object {
        fun fromName(value: String?): ActivityLevel = entries.firstOrNull { it.name == value } ?: Sedentary
    }
}

enum class WeeklyWeightLossTarget(val label: String, val poundsPerWeek: Double, val dailyCalorieAdjustment: Double) {
    Maintain("Maintain", 0.0, 0.0),
    HalfPound("0.5 lb/week", 0.5, -250.0),
    OnePound("1 lb/week", 1.0, -500.0),
    OneAndHalfPounds("1.5 lb/week", 1.5, -750.0),
    TwoPounds("2 lb/week", 2.0, -1000.0);

    companion object {
        fun fromName(value: String?): WeeklyWeightLossTarget = entries.firstOrNull { it.name == value } ?: Maintain
    }
}

enum class GoalMacro {
    Calories,
    Protein,
    Carbs,
    Fat,
    Fiber,
}

enum class GoalFieldKey {
    Birthday,
    Sex,
    HeightCm,
    WeightKg,
    BodyFatPercent,
    LeanMassKg,
    ActivityLevel,
    WeightLossTarget,
    Calories,
    Protein,
    Carbs,
    Fat,
    Fiber,
}

data class GoalMeasurement(
    val value: Double? = null,
    val locked: Boolean = false,
    val source: GoalValueSource = GoalValueSource.Manual,
    val updatedAt: LocalDateTime? = null,
) {
    fun applyHealthConnect(value: Double?, updatedAt: LocalDateTime?): GoalMeasurement {
        if (locked || value == null) return this
        return copy(value = value, source = GoalValueSource.HealthConnect, updatedAt = updatedAt)
    }

    fun setManual(value: Double?): GoalMeasurement {
        return copy(value = value, locked = true, source = GoalValueSource.Manual, updatedAt = LocalDateTime.now())
    }
}

data class MacroTargets(
    val calories: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val lockedMacros: Set<GoalMacro> = emptySet(),
) {
    fun isComplete(): Boolean = listOf(calories, protein, carbs, fat, fiber).all { it != null }

    fun valueFor(macro: GoalMacro): Double? = when (macro) {
        GoalMacro.Calories -> calories
        GoalMacro.Protein -> protein
        GoalMacro.Carbs -> carbs
        GoalMacro.Fat -> fat
        GoalMacro.Fiber -> fiber
    }

    fun withValue(macro: GoalMacro, value: Double?, lock: Boolean = true): MacroTargets {
        val locks = if (lock) lockedMacros + macro else lockedMacros - macro
        return when (macro) {
            GoalMacro.Calories -> copy(calories = value, lockedMacros = locks)
            GoalMacro.Protein -> copy(protein = value, lockedMacros = locks)
            GoalMacro.Carbs -> copy(carbs = value, lockedMacros = locks)
            GoalMacro.Fat -> copy(fat = value, lockedMacros = locks)
            GoalMacro.Fiber -> copy(fiber = value, lockedMacros = locks)
        }
    }

    fun unlocked(macro: GoalMacro): MacroTargets = copy(lockedMacros = lockedMacros - macro)
}

data class GoalProfile(
    val birthday: LocalDate? = null,
    val sex: GoalSex? = null,
    val heightCm: GoalMeasurement = GoalMeasurement(),
    val weightKg: GoalMeasurement = GoalMeasurement(),
    val bodyFatPercent: GoalMeasurement = GoalMeasurement(),
    val leanMassKg: GoalMeasurement = GoalMeasurement(),
    val activityLevel: ActivityLevel = ActivityLevel.Sedentary,
    val weightLossTarget: WeeklyWeightLossTarget = WeeklyWeightLossTarget.Maintain,
) {
    fun ageOn(date: LocalDate): Int? {
        val birth = birthday ?: return null
        var age = date.year - birth.year
        if (date.dayOfYear < birth.dayOfYear) age--
        return age.coerceAtLeast(0)
    }

    fun isRequiredComplete(date: LocalDate = LocalDate.now()): Boolean {
        return missingRequiredFields(date).isEmpty()
    }

    fun missingRequiredFields(date: LocalDate = LocalDate.now()): List<String> {
        val missing = mutableListOf<String>()
        if (ageOn(date) == null) missing += "birthday"
        if (sex == null) missing += "sex"
        if (heightCm.value == null) missing += "height"
        if (weightKg.value == null) missing += "weight"
        if (leanMassOrCalculatedKg() == null) missing += "lean mass or body fat"
        return missing
    }

    fun leanMassOrCalculatedKg(): Double? {
        leanMassKg.value?.let { return it }
        val weight = weightKg.value ?: return null
        val bodyFat = bodyFatPercent.value ?: return null
        return weight * (1.0 - bodyFat / 100.0)
    }
}

data class GoalHistoryEntry(
    val effectiveDate: LocalDate,
    val targets: MacroTargets,
    val source: String,
)

data class GoalRecommendation(
    val generatedDate: LocalDate,
    val targets: MacroTargets,
    val bmr: Double,
    val tdee: Double,
    val warning: String? = null,
)

data class Goals(
    val profile: GoalProfile = GoalProfile(),
    val currentTargets: MacroTargets = MacroTargets(),
    val history: List<GoalHistoryEntry> = emptyList(),
    val recommendation: GoalRecommendation? = null,
    val settingsVisible: Boolean = false,
    val message: String? = null,
) {
    fun activeTargetsFor(date: LocalDate): MacroTargets {
        return history
            .filter { !it.effectiveDate.isAfter(date) }
            .maxByOrNull { it.effectiveDate }
            ?.targets
            ?: currentTargets
    }
}

data class HealthConnectGoalSnapshot(
    val weightKg: Double? = null,
    val weightUpdatedAt: LocalDateTime? = null,
    val heightCm: Double? = null,
    val heightUpdatedAt: LocalDateTime? = null,
    val bodyFatPercent: Double? = null,
    val bodyFatUpdatedAt: LocalDateTime? = null,
    val leanMassKg: Double? = null,
    val leanMassUpdatedAt: LocalDateTime? = null,
)

data class MealTargetAllocation(
    val calories: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val fiber: Double?,
    val remainingMealCount: Int,
    val snackCaloriesReserved: Double,
) {
    companion object {
        val Empty = MealTargetAllocation(null, null, null, null, null, 0, 300.0)
    }
}

object GoalCalculator {
    const val DEFAULT_SNACK_CALORIES = 300.0
    private const val MIN_CALORIE_WARNING = 1200.0

    fun bmrMifflinStJeor(profile: GoalProfile, date: LocalDate = LocalDate.now()): Double? {
        val sex = profile.sex ?: return null
        val age = profile.ageOn(date) ?: return null
        val weight = profile.weightKg.value ?: return null
        val height = profile.heightCm.value ?: return null
        val sexConstant = when (sex) {
            GoalSex.Male -> 5.0
            GoalSex.Female -> -161.0
        }
        return (10.0 * weight) + (6.25 * height) - (5.0 * age) + sexConstant
    }

    fun recommendTargets(
        profile: GoalProfile,
        existingTargets: MacroTargets = MacroTargets(),
        date: LocalDate = LocalDate.now(),
    ): GoalRecommendation? {
        val bmr = bmrMifflinStJeor(profile, date) ?: return null
        val tdee = bmr * profile.activityLevel.factor
        val calories = (tdee + profile.weightLossTarget.dailyCalorieAdjustment).coerceAtLeast(0.0)
        val leanMassKg = profile.leanMassOrCalculatedKg() ?: return null
        val generated = generateMacros(
            calories = calories,
            leanMassKg = leanMassKg,
            existingTargets = existingTargets,
        )
        return GoalRecommendation(
            generatedDate = date,
            targets = generated,
            bmr = bmr,
            tdee = tdee,
            warning = if (calories < MIN_CALORIE_WARNING) "Recommended calories are very low." else null,
        )
    }

    fun generateMacros(
        calories: Double,
        leanMassKg: Double,
        existingTargets: MacroTargets = MacroTargets(),
    ): MacroTargets {
        val locks = existingTargets.lockedMacros
        val protein = if (GoalMacro.Protein in locks) {
            existingTargets.protein ?: 0.0
        } else {
            kgToPounds(leanMassKg)
        }
        val fat = if (GoalMacro.Fat in locks) {
            existingTargets.fat ?: 0.0
        } else {
            ((calories * 0.25) / 9.0)
        }
        val fiber = if (GoalMacro.Fiber in locks) {
            existingTargets.fiber ?: 0.0
        } else {
            (calories / 1000.0) * 14.0
        }
        val lockedCalories = if (GoalMacro.Calories in locks) existingTargets.calories else null
        val calorieTarget = lockedCalories ?: calories
        val carbs = if (GoalMacro.Carbs in locks) {
            existingTargets.carbs ?: 0.0
        } else {
            ((calorieTarget - (protein * 4.0) - (fat * 9.0)).coerceAtLeast(0.0) / 4.0)
        }
        return MacroTargets(
            calories = calorieTarget.roundToWhole(),
            protein = protein.roundToTenths(),
            carbs = carbs.roundToTenths(),
            fat = fat.roundToTenths(),
            fiber = fiber.roundToTenths(),
            lockedMacros = locks,
        )
    }

    fun applyHealthSnapshot(profile: GoalProfile, snapshot: HealthConnectGoalSnapshot): GoalProfile {
        return profile.copy(
            weightKg = profile.weightKg.applyHealthConnect(snapshot.weightKg, snapshot.weightUpdatedAt),
            heightCm = profile.heightCm.applyHealthConnect(snapshot.heightCm, snapshot.heightUpdatedAt),
            bodyFatPercent = profile.bodyFatPercent.applyHealthConnect(snapshot.bodyFatPercent, snapshot.bodyFatUpdatedAt),
            leanMassKg = profile.leanMassKg.applyHealthConnect(snapshot.leanMassKg, snapshot.leanMassUpdatedAt),
        )
    }

    fun mealAllocation(
        mealType: QuickImportMealType,
        targets: MacroTargets,
        consumedMeals: List<Pair<QuickImportMealType, QuickImportNutrients>>,
    ): MealTargetAllocation {
        if (!targets.isComplete()) return MealTargetAllocation.Empty
        if (mealType == QuickImportMealType.Snack) {
            return MealTargetAllocation(DEFAULT_SNACK_CALORIES, null, null, null, null, 1, DEFAULT_SNACK_CALORIES)
        }
        val orderedMeals = listOf(QuickImportMealType.Breakfast, QuickImportMealType.Lunch, QuickImportMealType.Dinner)
        val currentIndex = orderedMeals.indexOf(mealType)
        if (currentIndex < 0) return MealTargetAllocation.Empty
        val consumedBefore = consumedMeals.filter { (type, _) ->
            val index = orderedMeals.indexOf(type)
            index in 0 until currentIndex
        }
        val remainingTypes = orderedMeals.drop(currentIndex).filter { type ->
            consumedMeals.none { it.first == type }
        }.ifEmpty { listOf(mealType) }
        val remainingCount = remainingTypes.size
        fun remainingFor(value: Double?, selector: (QuickImportNutrients) -> Double, reserve: Double = 0.0): Double? {
            value ?: return null
            val consumed = consumedBefore.sumOf { selector(it.second) }
            return ((value - reserve - consumed).coerceAtLeast(0.0) / remainingCount)
        }
        return MealTargetAllocation(
            calories = remainingFor(targets.calories, { it.energy }, DEFAULT_SNACK_CALORIES)?.roundToWhole(),
            protein = remainingFor(targets.protein, { it.protein })?.roundToTenths(),
            carbs = remainingFor(targets.carbs, { it.carbohydrate })?.roundToTenths(),
            fat = remainingFor(targets.fat, { it.fat })?.roundToTenths(),
            fiber = remainingFor(targets.fiber, { it.fiber })?.roundToTenths(),
            remainingMealCount = remainingCount,
            snackCaloriesReserved = DEFAULT_SNACK_CALORIES,
        )
    }

    fun progress(nutrients: QuickImportNutrients, targets: MacroTargets): MacroTargets {
        return MacroTargets(
            calories = percent(nutrients.energy, targets.calories),
            protein = percent(nutrients.protein, targets.protein),
            carbs = percent(nutrients.carbohydrate, targets.carbs),
            fat = percent(nutrients.fat, targets.fat),
            fiber = percent(nutrients.fiber, targets.fiber),
        )
    }

    private fun percent(value: Double, target: Double?): Double? {
        target ?: return null
        if (target <= 0.0) return null
        return ((value / target) * 100.0).roundToTenths()
    }

    private fun kgToPounds(kg: Double): Double = kg * 2.2046226218
    private fun Double.roundToTenths(): Double = (this * 10.0).roundToInt() / 10.0
    private fun Double.roundToWhole(): Double = roundToInt().toDouble()
}

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
