package com.makstuff.minimalistcaloriecounter.widget

import android.content.Context
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionReadResult
import com.makstuff.minimalistcaloriecounter.persistence.AppCsvStore
import com.makstuff.minimalistcaloriecounter.persistence.room.AppRoomStore
import com.makstuff.minimalistcaloriecounter.ui.model.emptyQuickImportNutrients
import com.makstuff.minimalistcaloriecounter.ui.model.sumNutrition
import java.time.LocalDate

internal class NutritionWidgetDataLoader(private val context: Context) {
    suspend fun load(today: LocalDate = LocalDate.now()): NutritionWidgetState {
        val goals = readGoals()
        val targets = goals.activeTargetsFor(today)
        val mealsResult = HealthConnectManager(context).readNutritionMeals(today)
        val totals = when (mealsResult) {
            is HealthConnectNutritionReadResult.Success -> mealsResult.meals.sumNutrition()
            else -> emptyQuickImportNutrients()
        }
        return NutritionWidgetState(
            calories = MetricProgress("Calories", totals.energy, targets.calories, "kcal"),
            protein = MetricProgress("Protein", totals.protein, targets.protein, "g"),
            carbs = MetricProgress("Carbs", totals.carbohydrate, targets.carbs, "g"),
            fat = MetricProgress("Fat", totals.fat, targets.fat, "g"),
            fiber = MetricProgress("Fiber", totals.fiber, targets.fiber, "g"),
            foodCount = (mealsResult as? HealthConnectNutritionReadResult.Success)?.meals?.size ?: 0,
            permissionsMissing = mealsResult == HealthConnectNutritionReadResult.PermissionsMissing,
        )
    }

    private suspend fun readGoals(): Goals {
        val appContext = context.applicationContext
        return runCatching { AppRoomStore(appContext).use { it.readGoals() } }.getOrNull()
            ?: AppCsvStore().readGoals(appContext)
            ?: Goals()
    }
}

private inline fun <T> AppRoomStore.use(block: (AppRoomStore) -> T): T {
    return try {
        block(this)
    } finally {
        close()
    }
}
