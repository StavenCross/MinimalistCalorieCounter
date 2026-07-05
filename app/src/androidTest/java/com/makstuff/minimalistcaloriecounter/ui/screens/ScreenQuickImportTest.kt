package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxState
import com.makstuff.minimalistcaloriecounter.classes.QuickImportParser
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ScreenQuickImportTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun validMealShowsPreviewAndEnablesImport() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sample = "100g rice; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g. " +
            "Meal totals; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g."
        val state = baseState(context).copy(
            inputQuickImportText = sample,
            inputQuickImportDateTime = LocalDateTime.of(2026, 7, 2, 12, 0),
            quickImportMeal = QuickImportParser.parse(sample),
            goals = Goals(
                currentTargets = MacroTargets(
                    calories = 2400.0,
                    protein = 180.0,
                    carbs = 240.0,
                    fat = 80.0,
                    fiber = 35.0,
                ),
            ),
        )

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = state,
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {},
                    onMealTypeChange = { _: QuickImportMealType -> },
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_list").performScrollToNode(hasTestTag("quick_import_preview_totals"))
        composeRule.onNodeWithTag("quick_import_preview_totals", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_meal_macro_carbs", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onAllNodesWithTag("quick_meal_macro_protein", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onAllNodesWithTag("quick_meal_macro_fat", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onAllNodesWithTag("quick_meal_macro_fiber", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onAllNodesWithTag("quick_goal_calories", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onAllNodesWithTag("quick_goal_protein", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onAllNodesWithTag("quick_goal_carbs", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onAllNodesWithTag("quick_goal_fat", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onAllNodesWithTag("quick_goal_fiber", useUnmergedTree = true)[0].fetchSemanticsNode()
        composeRule.onNodeWithTag("quick_import_save_meal_button").assertIsEnabled()
    }

    @Test
    fun missingParsedMealDisablesImport() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(inputQuickImportText = "not a meal"),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {},
                    onMealTypeChange = { _: QuickImportMealType -> },
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onAllNodesWithTag("quick_import_save_meal_button").assertCountEquals(0)
    }

    @Test
    fun daySelectorChangesOnlyDate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val start = LocalDateTime.of(2026, 7, 3, 12, 30)
        var changed: LocalDateTime? = null

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(inputQuickImportDateTime = start),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = { changed = it },
                    onMealTypeChange = { _: QuickImportMealType -> },
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("add_meal_previous_day").performClick()
        assertEquals(LocalDateTime.of(2026, 7, 2, 12, 30), changed)
    }

    @Test
    fun mealActionsDrawerCanSetNow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var nowClicked = false

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(
                        inputQuickImportDateTime = LocalDateTime.of(2026, 7, 2, 12, 0),
                    ),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = { nowClicked = true },
                    onDateTimeChange = {},
                    onMealTypeChange = { _: QuickImportMealType -> },
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Meal time actions").performClick()
        composeRule.onNodeWithText("Now").assertIsDisplayed().performClick()
        assertTrue(nowClicked)
    }

    @Test
    fun editMealTimeDrawerStartsFromSelectedMealTime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(
                        inputQuickImportDateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
                    ),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {},
                    onMealTypeChange = { _: QuickImportMealType -> },
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Meal time actions").performClick()
        composeRule.onNodeWithText("Edit time").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Current selection: Friday, July 3, 2026").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("12 o'clock")[0].assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("0 minutes")[0].assertIsDisplayed()
    }

    @Test
    fun mealTypeDrawerCanSelectSnack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selectedMealType: QuickImportMealType? = null

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(
                        inputQuickImportDateTime = LocalDateTime.of(2026, 7, 2, 18, 0),
                    ),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {},
                    onMealTypeChange = { selectedMealType = it },
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithText("Dinner").performClick()
        composeRule.onNodeWithText("Snack").assertIsDisplayed().performClick()
        assertEquals(QuickImportMealType.Snack, selectedMealType)
    }

    @Test
    fun successStateShowsAnimationMessageAndResetCapture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(
                        inputQuickImportText = "",
                        quickImportMeal = null,
                        quickImportSuccessMessage = "Added 3 database foods and 3 day foods. Health Connect write succeeded.",
                        quickImportSuccessToken = 1L,
                    ),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {},
                    onMealTypeChange = { _: QuickImportMealType -> },
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_success_pill").assertIsDisplayed()
        composeRule.onNodeWithText("Meal added").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_import_paste").assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_import_save_meal_button").assertCountEquals(0)
    }

    @Test
    fun parsedFoodDrawerEditsFoodValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sample = "100g rice; Calories 130, Fat 0.3g, Sat Fat 0.1g, Carbs 28g, Fiber 1g, Sugar 0.1g, Protein 2.7g. " +
            "Meal totals; Calories 130, Fat 0.3g, Sat Fat 0.1g, Carbs 28g, Fiber 1g, Sugar 0.1g, Protein 2.7g."
        var editedIndex: Int? = null
        var editedCalories: Double? = null

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(
                        inputQuickImportText = sample,
                        quickImportMeal = QuickImportParser.parse(sample),
                    ),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {},
                    onMealTypeChange = { _: QuickImportMealType -> },
                    onParsedFoodChange = { index, food ->
                        editedIndex = index
                        editedCalories = food.nutrients.energy
                    },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_list").performScrollToNode(hasTestTag("quick_import_preview_totals"))
        composeRule.onNodeWithText("100 g rice").performClick()
        composeRule.onNodeWithTag("quick_food_edit_calories").performTextClearance()
        composeRule.onNodeWithTag("quick_food_edit_calories").performTextInput("145")
        composeRule.onNodeWithTag("quick_food_edit_save").performClick()
        assertEquals(0, editedIndex)
        assertEquals(145.0, editedCalories!!, 0.01)
    }

    @Test
    fun outboxAttentionShowsSyncStatusCard() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(
                        quickImportOutbox = listOf(
                            QuickImportOutboxItem(
                                id = "abc123",
                                createdAt = LocalDateTime.of(2026, 7, 3, 12, 1),
                                intendedDateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
                                mealType = QuickImportMealType.Lunch,
                                sourceTextHash = "hash",
                                mealSummary = "1 foods, 389 kcal",
                                foodCount = 1,
                                state = QuickImportOutboxState.FailedHealthConnect,
                                attemptCount = 1,
                                lastAttemptAt = LocalDateTime.of(2026, 7, 3, 12, 2),
                                lastErrorMessage = "Health Connect permissions are missing.",
                                healthPayloads = listOf(
                                    QuickImportHealthPayload(
                                        dateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
                                        mealType = QuickImportMealType.Lunch.healthConnectValue,
                                        energy = 389.0,
                                        energyFromFat = 62.1,
                                        totalCarbohydrate = 66.3,
                                        sugar = 0.9,
                                        protein = 16.9,
                                        totalFat = 6.9,
                                        saturatedFat = 1.2,
                                        dietaryFiber = 10.6,
                                        name = "100g test oats",
                                        clientRecordId = "mcc-add-meal-abc123-0",
                                    )
                                ),
                            )
                        ),
                    ),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {},
                    onMealTypeChange = { _: QuickImportMealType -> },
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_list").performScrollToNode(hasTestTag("quick_import_outbox_status"))
        composeRule.onNodeWithTag("quick_import_outbox_status", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("1 Health Connect write needs sync attention.").fetchSemanticsNode()
        composeRule.onNodeWithTag("quick_import_outbox_retry").performScrollTo().assertIsDisplayed().assertIsEnabled()
    }

    private fun baseState(context: android.content.Context): AppUiState {
        return AppUiState(
            archive = Archive(context = context),
            day = Combo(context = context),
        )
    }
}
