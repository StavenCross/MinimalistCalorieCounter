package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportRepeatBuilder
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

internal class AppViewModelQuickImportRepeatActions(
    private val env: AppViewModelEnvironment,
) {
    fun prepare(foods: List<HealthConnectNutritionMeal>) {
        if (foods.isEmpty()) return
        val mealType = QuickImportRepeatBuilder.mealType(foods)
        val repeatTime = repeatDateTime(mealType)
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
                quickImportInProgress = false,
            )
        }
    }

    private fun repeatDateTime(mealType: QuickImportMealType): LocalDateTime {
        return mealType.applyDefaultTime(LocalDateTime.now())
    }
}
