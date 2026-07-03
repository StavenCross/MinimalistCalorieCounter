package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDate
import java.time.LocalDateTime

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
