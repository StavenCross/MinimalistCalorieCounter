package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportRepeatBuilder
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalDateTime

internal class AppViewModelQuickImportRepeatActions(
    private val env: AppViewModelEnvironment,
) {
    /**
     * Seeds Add Meal from existing Health Connect foods.
     *
     * `targetDate` is supplied by the Meals page when the user repeats a historical meal. Keeping
     * the date explicit prevents a copied meal from silently jumping to today while still reusing
     * meal-type default times. Repeat also restores the normal all-destinations-on defaults because
     * those toggles are no longer part of the primary Meals drawer workflow.
     */
    fun prepare(foods: List<HealthConnectNutritionMeal>, targetDate: LocalDate? = null) {
        if (foods.isEmpty()) return
        val mealType = QuickImportRepeatBuilder.mealType(foods)
        val repeatTime = repeatDateTime(mealType, targetDate)
        env.state.update {
            it.copy(
                inputQuickImportText = QuickImportRepeatBuilder.text(foods),
                inputQuickImportDateTime = repeatTime,
                quickImportSnackOverride = mealType == QuickImportMealType.Snack,
                quickImportMealTypeOverride = mealType,
                quickImportMeal = QuickImportRepeatBuilder.build(foods),
                quickImportError = null,
                quickImportResult = null,
                quickImportSuccessMessage = null,
                quickImportAddFoodsToDatabase = true,
                quickImportAddFoodsToDay = true,
                quickImportWriteHealthConnect = true,
                quickImportInProgress = false,
            )
        }
    }

    private fun repeatDateTime(mealType: QuickImportMealType, targetDate: LocalDate?): LocalDateTime {
        val base = targetDate?.atTime(LocalDateTime.now().toLocalTime()) ?: LocalDateTime.now()
        return mealType.applyDefaultTime(base)
    }
}
