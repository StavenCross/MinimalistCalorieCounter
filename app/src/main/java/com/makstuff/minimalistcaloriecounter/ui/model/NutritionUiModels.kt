package com.makstuff.minimalistcaloriecounter.ui.model

import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.GoalCalculator
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString

data class NutritionStatItem(
    val label: String,
    val value: String,
)

data class MacroProgressArc(
    val progress: Float,
    val isOverTarget: Boolean,
)

data class NutritionDaySummary(
    val totals: QuickImportNutrients,
    val foodCount: Int,
    val progress: MacroTargets,
)

data class NutritionMealGroup(
    val mealType: Int,
    val label: String,
    val colorArgb: Long,
    val foods: List<HealthConnectNutritionMeal>,
)

fun mealGroupKey(group: NutritionMealGroup): String {
    val firstStart = group.foods.minOfOrNull { it.startTime.toString() }.orEmpty()
    return "${group.mealType}:$firstStart:${group.foods.size}"
}

fun shouldCollapseMealGroup(group: NutritionMealGroup, collapsedFoodLimit: Int = 3): Boolean {
    return group.foods.size > collapsedFoodLimit
}

fun visibleMealFoods(
    group: NutritionMealGroup,
    expanded: Boolean,
    collapsedFoodLimit: Int = 3,
): List<HealthConnectNutritionMeal> {
    return if (expanded || !shouldCollapseMealGroup(group, collapsedFoodLimit)) {
        group.foods
    } else {
        group.foods.take(collapsedFoodLimit)
    }
}

fun macroPercent(value: Double, target: Double?): Double? {
    if (target == null || target <= 0.0) return null
    return value / target * 100.0
}

fun macroProgressArc(value: Double?): MacroProgressArc {
    val isOver = value != null && value > 100.0
    val progress = when {
        value == null -> 0f
        isOver -> ((value - 100.0).coerceIn(0.0, 100.0) / 100.0).toFloat()
        else -> (value.coerceIn(0.0, 100.0) / 100.0).toFloat()
    }
    return MacroProgressArc(progress = progress, isOverTarget = isOver)
}

fun supportsMacroHint(label: String): Boolean {
    return label in setOf("Calories", "Carbs", "Protein", "Fat", "Fiber")
}

fun macroSummaryItems(nutrients: QuickImportNutrients): List<NutritionStatItem> {
    return listOf(
        NutritionStatItem("Carbs", "${nutrients.carbohydrate.toFormattedString(true)}g"),
        NutritionStatItem("Protein", "${nutrients.protein.toFormattedString(true)}g"),
        NutritionStatItem("Fat", "${nutrients.fat.toFormattedString(true)}g"),
        NutritionStatItem("Fiber", "${nutrients.fiber.toFormattedString(true)}g"),
    )
}

fun quickNutrientDetailItems(
    nutrients: QuickImportNutrients,
    includeAmount: String?,
): List<NutritionStatItem> {
    return buildList {
        if (includeAmount != null) add(NutritionStatItem("Amount", includeAmount))
        add(NutritionStatItem("Calories", "${nutrients.energy.toFormattedString(true)} kcal"))
        add(NutritionStatItem("Carbs", "${nutrients.carbohydrate.toFormattedString(true)}g"))
        add(NutritionStatItem("Protein", "${nutrients.protein.toFormattedString(true)}g"))
        add(NutritionStatItem("Fat", "${nutrients.fat.toFormattedString(true)}g"))
        add(NutritionStatItem("Fiber", "${nutrients.fiber.toFormattedString(true)}g"))
        add(NutritionStatItem("Sugar", "${nutrients.sugar.toFormattedString(true)}g"))
        add(NutritionStatItem("Sat fat", "${nutrients.saturatedFat.toFormattedString(true)}g"))
    }
}

fun healthMealDetailItems(meal: HealthConnectNutritionMeal): List<NutritionStatItem> {
    return listOf(
        NutritionStatItem("Calories", "${meal.energy.toFormattedString(true)} kcal"),
        NutritionStatItem("Carbs", "${meal.totalCarbohydrate.toFormattedString(true)}g"),
        NutritionStatItem("Protein", "${meal.protein.toFormattedString(true)}g"),
        NutritionStatItem("Fat", "${meal.totalFat.toFormattedString(true)}g"),
        NutritionStatItem("Fiber", "${meal.dietaryFiber.toFormattedString(true)}g"),
        NutritionStatItem("Sugar", "${meal.sugar.toFormattedString(true)}g"),
        NutritionStatItem("Sat fat", "${meal.saturatedFat.toFormattedString(true)}g"),
        NutritionStatItem("Fat kcal", meal.energyFromFat?.let { "${it.toFormattedString(true)} kcal" }.orEmpty()),
    )
}

fun healthGroupDetailItems(group: NutritionMealGroup): List<NutritionStatItem> {
    val totals = group.foods.sumNutrition()
    return quickNutrientDetailItems(totals, includeAmount = null)
}

fun nutritionDaySummary(
    meals: List<HealthConnectNutritionMeal>,
    targets: MacroTargets,
): NutritionDaySummary {
    val totals = meals.sumNutrition()
    return NutritionDaySummary(
        totals = totals,
        foodCount = meals.size,
        progress = GoalCalculator.progress(totals, targets),
    )
}

fun consumedMealsForQuickImportDate(uiState: AppUiState): List<Pair<QuickImportMealType, QuickImportNutrients>> {
    if (!uiState.isQuickImportViewerDate()) return emptyList()
    return uiState.healthConnectViewerMeals
        .filter { it.startTime.isBefore(uiState.inputQuickImportDateTime) }
        .groupBy { it.quickImportMealType() }
        .map { (mealType, foods) ->
            mealType to foods.sumNutrition()
        }
}

fun currentDayTotalsForQuickImportDate(uiState: AppUiState): QuickImportNutrients {
    if (!uiState.isQuickImportViewerDate()) return emptyQuickImportNutrients()
    return uiState.healthConnectViewerMeals.sumNutrition()
}

fun currentDayFoodCountForQuickImportDate(uiState: AppUiState): Int {
    if (!uiState.isQuickImportViewerDate()) return 0
    return uiState.healthConnectViewerMeals.size
}

fun mealGroups(meals: List<HealthConnectNutritionMeal>): List<NutritionMealGroup> {
    return listOf(
        groupFor(MealType.MEAL_TYPE_BREAKFAST, "Breakfast", 0xFF4285F4, meals),
        groupFor(MealType.MEAL_TYPE_LUNCH, "Lunch", 0xFF00BCD4, meals),
        groupFor(MealType.MEAL_TYPE_DINNER, "Dinner", 0xFFE8710A, meals),
        groupFor(MealType.MEAL_TYPE_SNACK, "Snack", 0xFF9C27B0, meals),
        groupFor(MealType.MEAL_TYPE_UNKNOWN, "Other", 0xFF607D8B, meals),
    ).filter { it.foods.isNotEmpty() }
}

fun HealthConnectNutritionMeal.quickImportMealType(): QuickImportMealType {
    return when (mealType) {
        MealType.MEAL_TYPE_BREAKFAST -> QuickImportMealType.Breakfast
        MealType.MEAL_TYPE_LUNCH -> QuickImportMealType.Lunch
        MealType.MEAL_TYPE_DINNER -> QuickImportMealType.Dinner
        MealType.MEAL_TYPE_SNACK -> QuickImportMealType.Snack
        else -> QuickImportMealType.inferFrom(startTime)
    }
}

fun List<HealthConnectNutritionMeal>.sumNutrition(): QuickImportNutrients {
    return QuickImportNutrients(
        energy = sumOf { it.energy },
        carbohydrate = sumOf { it.totalCarbohydrate },
        sugar = sumOf { it.sugar },
        protein = sumOf { it.protein },
        fat = sumOf { it.totalFat },
        saturatedFat = sumOf { it.saturatedFat },
        fiber = sumOf { it.dietaryFiber },
    )
}

fun emptyQuickImportNutrients(): QuickImportNutrients {
    return QuickImportNutrients(
        energy = 0.0,
        carbohydrate = 0.0,
        sugar = 0.0,
        protein = 0.0,
        fat = 0.0,
        saturatedFat = 0.0,
        fiber = 0.0,
    )
}

private fun AppUiState.isQuickImportViewerDate(): Boolean {
    return healthConnectViewerDate == inputQuickImportDateTime.toLocalDate()
}

private fun groupFor(
    mealType: Int,
    label: String,
    colorArgb: Long,
    meals: List<HealthConnectNutritionMeal>,
): NutritionMealGroup {
    return NutritionMealGroup(
        mealType = mealType,
        label = label,
        colorArgb = colorArgb,
        foods = meals.filter { it.mealType == mealType }.sortedBy { it.startTime },
    )
}
