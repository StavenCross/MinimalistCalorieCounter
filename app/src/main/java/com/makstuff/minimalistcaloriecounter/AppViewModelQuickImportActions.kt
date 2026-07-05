package com.makstuff.minimalistcaloriecounter

import android.content.Context
import com.makstuff.minimalistcaloriecounter.classes.QuickImportCommitOptions
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFormatter
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutbox
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxState
import com.makstuff.minimalistcaloriecounter.classes.QuickImportParser
import com.makstuff.minimalistcaloriecounter.classes.QuickImportPlanner
import com.makstuff.minimalistcaloriecounter.classes.QuickImportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

internal class AppViewModelQuickImportActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    fun updateText(text: String) {
        val parsed = runCatching { QuickImportParser.parse(text) }
        env.state.update { currentState ->
            currentState.copy(
                inputQuickImportText = text,
                quickImportMeal = parsed.getOrNull(),
                quickImportError = if (text.isBlank()) null else parsed.exceptionOrNull()?.message,
            ).withoutQuickImportOutcome()
        }
    }

    fun updateParsedFood(foodIndex: Int, food: QuickImportFood) {
        val meal = env.uiState.quickImportMeal ?: return
        val updatedText = QuickImportFormatter.text(QuickImportFormatter.replaceFood(meal, foodIndex, food))
        updateText(updatedText)
    }

    fun reset() {
        env.state.update { currentState ->
            currentState.copy(
                inputQuickImportText = "",
                inputQuickImportDateTime = LocalDateTime.now(),
                quickImportSnackOverride = false,
                quickImportMealTypeOverride = null,
                quickImportMeal = null,
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

    fun refreshDateTime() {
        env.state.update { currentState ->
            currentState.copy(
                inputQuickImportDateTime = LocalDateTime.now(),
                quickImportSnackOverride = false,
                quickImportMealTypeOverride = null,
            ).withoutQuickImportOutcome()
        }
    }

    fun updateDateTime(dateTime: LocalDateTime) {
        env.state.update { currentState ->
            currentState.copy(
                inputQuickImportDateTime = dateTime,
                quickImportSnackOverride = false,
                quickImportMealTypeOverride = null,
            ).withoutQuickImportOutcome()
        }
    }

    fun toggleSnackOverride() {
        env.state.update { currentState ->
            currentState.copy(
                quickImportSnackOverride = !currentState.quickImportSnackOverride,
                quickImportMealTypeOverride = if (!currentState.quickImportSnackOverride) {
                    QuickImportMealType.Snack
                } else {
                    null
                },
            ).withoutQuickImportOutcome()
        }
    }

    fun updateSnackOverride(enabled: Boolean) {
        env.state.update { currentState ->
            currentState.copy(
                quickImportSnackOverride = enabled,
                quickImportMealTypeOverride = if (enabled) QuickImportMealType.Snack else null,
            ).withoutQuickImportOutcome()
        }
    }

    fun updateMealTypeOverride(mealType: QuickImportMealType) {
        env.state.update { currentState ->
            currentState.copy(
                inputQuickImportDateTime = mealType.applyDefaultTime(currentState.inputQuickImportDateTime),
                quickImportMealTypeOverride = mealType,
                quickImportSnackOverride = mealType == QuickImportMealType.Snack,
            ).withoutQuickImportOutcome()
        }
    }

    fun toggleAddFoodsToDatabase() {
        env.state.update { currentState ->
            currentState.copy(quickImportAddFoodsToDatabase = !currentState.quickImportAddFoodsToDatabase).withoutQuickImportOutcome()
        }
    }

    fun updateAddFoodsToDatabase(enabled: Boolean) {
        env.state.update { currentState ->
            currentState.copy(quickImportAddFoodsToDatabase = enabled).withoutQuickImportOutcome()
        }
    }

    fun toggleAddFoodsToDay() {
        env.state.update { currentState ->
            currentState.copy(quickImportAddFoodsToDay = !currentState.quickImportAddFoodsToDay).withoutQuickImportOutcome()
        }
    }

    fun updateAddFoodsToDay(enabled: Boolean) {
        env.state.update { currentState ->
            currentState.copy(quickImportAddFoodsToDay = enabled).withoutQuickImportOutcome()
        }
    }

    fun toggleWriteHealthConnect() {
        env.state.update { currentState ->
            currentState.copy(quickImportWriteHealthConnect = !currentState.quickImportWriteHealthConnect).withoutQuickImportOutcome()
        }
    }

    fun updateWriteHealthConnect(enabled: Boolean) {
        env.state.update { currentState ->
            currentState.copy(quickImportWriteHealthConnect = enabled).withoutQuickImportOutcome()
        }
    }

    fun commit(context: Context) {
        val state = env.uiState
        val meal = state.quickImportMeal ?: run {
            updateText(state.inputQuickImportText)
            env.uiState.quickImportMeal ?: return
        }
        val options = QuickImportCommitOptions(
            addFoodsToDatabase = state.quickImportAddFoodsToDatabase,
            addFoodsToDay = state.quickImportAddFoodsToDay,
            writeHealthConnect = state.quickImportWriteHealthConnect,
        )

        val plan = try {
            QuickImportPlanner.build(
                meal = meal,
                options = options,
                dateTime = state.inputQuickImportDateTime,
                mealType = state.quickImportMealType,
                existingDatabaseNames = state.database.map { it.name }.toSet(),
            )
        } catch (e: IllegalArgumentException) {
            env.state.update { it.copy(quickImportError = e.message, quickImportResult = null) }
            return
        }

        val databaseEntries = try {
            plan.foodDrafts.map { it.toDatabaseEntry(context) }
        } catch (e: IllegalStateException) {
            env.state.update { it.copy(quickImportError = e.message, quickImportResult = null) }
            return
        }

        env.state.update { it.copy(quickImportInProgress = true, quickImportError = null, quickImportResult = null) }
        env.scope.launch {
            var databaseEntriesAdded = 0
            var dayFoodsAdded = 0
            var outboxItem: QuickImportOutboxItem? = null

            try {
                if (options.addFoodsToDatabase) {
                    databaseEntries.forEach {
                        viewModel.databaseAddEntry(context, false, it)
                    }
                    viewModel.databaseSortByName()
                    viewModel.databaseQuickselectUpdate()
                    viewModel.databaseLetterReset()
                    env.csvStore.writeDatabase(context, env.uiState.database)
                    databaseEntriesAdded = databaseEntries.size
                }

                if (options.addFoodsToDay) {
                    databaseEntries.zip(plan.foodDrafts).forEach { (entry, draft) ->
                        env.uiState.day.addComponent(draft.grams, entry)
                    }
                    env.csvStore.writeDay(context, env.uiState.day)
                    dayFoodsAdded = databaseEntries.size
                }

                val healthResult = if (options.writeHealthConnect) {
                    val pendingItem = QuickImportOutbox.buildItem(
                        sourceText = state.inputQuickImportText,
                        meal = meal,
                        intendedDateTime = state.inputQuickImportDateTime,
                        mealType = state.quickImportMealType,
                        healthPayloads = plan.healthPayloads,
                        createdAt = LocalDateTime.now(),
                    )
                    val existingItem = env.uiState.quickImportOutbox.firstOrNull { it.id == pendingItem.id }
                    outboxItem = QuickImportOutbox.markAttempting(
                        item = existingItem ?: pendingItem,
                        attemptedAt = LocalDateTime.now(),
                    )
                    env.writeQuickImportOutboxItem(context, outboxItem)
                    env.writeLocalMealBackup(meal, state.inputQuickImportDateTime, state.quickImportMealType, outboxItem.healthPayloads.map { it.clientRecordId })
                    val result = env.healthConnectManager.insertQuickMealNutrition(outboxItem.healthPayloads)
                    outboxItem = QuickImportOutbox.markResult(outboxItem, result)
                    env.writeQuickImportOutboxItem(context, outboxItem)
                    result
                } else {
                    env.writeLocalMealBackup(meal, state.inputQuickImportDateTime, state.quickImportMealType)
                    null
                }
                val result = QuickImportResult(
                    databaseEntriesAdded = databaseEntriesAdded,
                    dayFoodsAdded = dayFoodsAdded,
                    healthWriteResult = healthResult,
                )
                val healthWriteSucceeded = healthResult == null || healthResult == QuickImportHealthWriteResult.Success
                if (healthWriteSucceeded) {
                    val committedDate = state.inputQuickImportDateTime.toLocalDate()
                    env.state.update {
                        it.copy(
                            inputQuickImportText = "",
                            inputQuickImportDateTime = LocalDateTime.now(),
                            quickImportSnackOverride = false,
                            quickImportMealTypeOverride = null,
                            quickImportMeal = null,
                            quickImportError = null,
                            quickImportResult = null,
                            quickImportSuccessMessage = quickImportResultText(
                                result.databaseEntriesAdded,
                                result.dayFoodsAdded,
                                result.healthWriteResult,
                            ),
                            quickImportSuccessToken = it.quickImportSuccessToken + 1L,
                            quickImportInProgress = false,
                            healthConnectViewerDate = committedDate,
                        )
                    }
                    viewModel.readHealthConnectNutritionMeals()
                } else {
                    env.state.update {
                        it.copy(
                            quickImportInProgress = false,
                            quickImportResult = result,
                        )
                    }
                }
            } catch (e: CancellationException) {
                env.state.update { it.copy(quickImportInProgress = false) }
                throw e
            } catch (e: Throwable) {
                outboxItem?.let {
                    env.writeQuickImportOutboxItem(
                        context = context,
                        item = it.copy(
                            state = QuickImportOutboxState.FailedHealthConnect,
                            lastErrorMessage = e.message ?: "Add Meal failed.",
                        ),
                    )
                }
                env.state.update {
                    it.copy(
                        quickImportInProgress = false,
                        quickImportError = e.message ?: "Add Meal failed.",
                    )
                }
            }
        }
    }
}

// Any Add Meal input change should invalidate the previous commit result without disturbing the parsed meal.
private fun AppUiState.withoutQuickImportOutcome(): AppUiState = copy(
    quickImportResult = null,
    quickImportSuccessMessage = null,
)
