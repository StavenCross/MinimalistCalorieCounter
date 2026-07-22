package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.classes.NutritionFoodEditDraft
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectDeleteResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionReadResult
import com.makstuff.minimalistcaloriecounter.health.toEditedHealthPayload
import com.makstuff.minimalistcaloriecounter.health.toHealthPayload
import com.makstuff.minimalistcaloriecounter.widget.NutritionWidgetUpdater
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

internal class AppViewModelHealthConnectMealActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    fun readMeals(date: LocalDate? = null, showLoading: Boolean = true) {
        val requestedDate = date ?: env.uiState.healthConnectViewerDate
        env.state.update { currentState ->
            currentState.copy(
                healthConnectViewerLoading = if (showLoading) true else currentState.healthConnectViewerLoading,
                healthConnectViewerLoadingDate = requestedDate,
                healthConnectViewerMessage = if (showLoading) null else currentState.healthConnectViewerMessage,
            )
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.readNutritionMeals(requestedDate)) {
                is HealthConnectNutritionReadResult.Success -> {
                    env.state.update { currentState ->
                        val message = if (result.meals.isEmpty()) {
                            "No Health Connect nutrition records found for this app on this date."
                        } else {
                            null
                        }
                        val cachedState = currentState.copy(
                            healthConnectViewerMealsByDate = currentState.healthConnectViewerMealsByDate + (requestedDate to result.meals),
                            healthConnectViewerMessagesByDate = currentState.healthConnectViewerMessagesByDate + (requestedDate to message),
                        )
                        if (currentState.healthConnectViewerDate != requestedDate) {
                            cachedState
                        } else {
                            cachedState.copy(
                                healthConnectViewerLoading = false,
                                healthConnectViewerLoadingDate = null,
                                healthConnectViewerMeals = result.meals,
                                healthConnectViewerMealsDate = requestedDate,
                                healthConnectViewerMessage = message,
                            )
                        }
                    }
                }
                HealthConnectNutritionReadResult.HealthConnectUnavailable -> {
                    env.state.update { currentState ->
                        if (currentState.healthConnectViewerDate != requestedDate) {
                            currentState
                        } else {
                            currentState.copy(
                                healthConnectViewerLoading = false,
                                healthConnectViewerLoadingDate = null,
                                healthConnectViewerMeals = emptyList(),
                                healthConnectViewerMealsDate = requestedDate,
                                healthConnectViewerMessage = env.application.getString(R.string.toast_hc_not_available),
                            )
                        }
                    }
                }
                HealthConnectNutritionReadResult.PermissionsMissing -> {
                    env.state.update { currentState ->
                        if (currentState.healthConnectViewerDate != requestedDate) {
                            currentState
                        } else {
                            currentState.copy(
                                healthConnectViewerLoading = false,
                                healthConnectViewerLoadingDate = null,
                                healthConnectViewerMeals = emptyList(),
                                healthConnectViewerMealsDate = requestedDate,
                                healthConnectViewerMessage = env.application.getString(R.string.health_connect_permissions_missing),
                            )
                        }
                    }
                }
                is HealthConnectNutritionReadResult.Failed -> {
                    env.state.update { currentState ->
                        if (currentState.healthConnectViewerDate != requestedDate) {
                            currentState
                        } else {
                            currentState.copy(
                                healthConnectViewerLoading = false,
                                healthConnectViewerLoadingDate = null,
                                healthConnectViewerMeals = emptyList(),
                                healthConnectViewerMealsDate = requestedDate,
                                healthConnectViewerMessage = result.message,
                            )
                        }
                    }
                }
            }
        }
    }

    fun deleteMeal(recordId: String) = deleteMeals(listOf(recordId))

    fun deleteMeals(recordIds: List<String>) {
        env.state.update { currentState ->
            currentState.copy(
                healthConnectViewerLoading = true,
                healthConnectViewerMessage = null,
            )
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.deleteNutritionMeals(recordIds)) {
                HealthConnectDeleteResult.Success -> {
                    viewModel.readHealthConnectNutritionMeals()
                    NutritionWidgetUpdater.updateAll(env.application)
                }
                HealthConnectDeleteResult.HealthConnectUnavailable -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = env.application.getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HealthConnectDeleteResult.PermissionsMissing -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = env.application.getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HealthConnectDeleteResult.Failed -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun addServing(food: HealthConnectNutritionMeal) = mutateServingRecords {
        env.healthConnectManager.insertNutritionServings(listOf(food.toHealthPayload(clientRecordId = editClientRecordId())))
    }

    fun updateServingGroup(foods: List<HealthConnectNutritionMeal>, draft: NutritionFoodEditDraft) {
        if (foods.isEmpty() || !draft.isComplete) return
        mutateServingRecords {
            env.healthConnectManager.replaceNutritionServings(
                recordIds = foods.map { it.recordId },
                payloads = foods.map { food ->
                    food.toEditedHealthPayload(draft, clientRecordId = editClientRecordId())
                },
            )
        }
    }

    private fun mutateServingRecords(action: suspend () -> QuickImportHealthWriteResult) {
        env.state.update { currentState ->
            currentState.copy(
                healthConnectViewerLoading = true,
                healthConnectViewerMessage = null,
            )
        }
        env.scope.launch {
            when (val result = action()) {
                QuickImportHealthWriteResult.Success -> {
                    viewModel.readHealthConnectNutritionMeals()
                    NutritionWidgetUpdater.updateAll(env.application)
                }
                QuickImportHealthWriteResult.HealthConnectUnavailable -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = env.application.getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                QuickImportHealthWriteResult.PermissionsMissing -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = env.application.getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is QuickImportHealthWriteResult.Failed -> {
                    env.state.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun editClientRecordId(): String = "mcc-meal-edit-${UUID.randomUUID()}"
}
