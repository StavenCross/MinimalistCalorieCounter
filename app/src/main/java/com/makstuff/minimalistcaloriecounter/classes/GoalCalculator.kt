package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDate
import kotlin.math.roundToInt

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
        val protein = if (GoalMacro.Protein in locks) existingTargets.protein ?: 0.0 else kgToPounds(leanMassKg)
        val fat = if (GoalMacro.Fat in locks) existingTargets.fat ?: 0.0 else (calories * 0.25) / 9.0
        val fiber = if (GoalMacro.Fiber in locks) existingTargets.fiber ?: 0.0 else (calories / 1000.0) * 14.0
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
