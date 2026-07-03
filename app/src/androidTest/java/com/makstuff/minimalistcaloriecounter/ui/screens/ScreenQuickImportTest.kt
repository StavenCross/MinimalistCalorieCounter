package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
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
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_preview_totals").assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_meal_macro_carbs", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_meal_macro_protein", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_meal_macro_fat", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_meal_macro_fiber", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_goal_calories", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_goal_protein", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_goal_carbs", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_goal_fat", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_goal_fiber", useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onNodeWithTag("quick_import_import_button").assertIsEnabled()
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
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_import_button").assertIsNotEnabled()
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
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_success_pill").assertIsDisplayed()
        composeRule.onNodeWithText("Meal added").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_import_paste").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_import_import_button").assertIsNotEnabled()
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
                    onImport = {},
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_outbox_status").assertIsDisplayed()
        composeRule.onNodeWithText("1 Health Connect write needs sync attention.").assertIsDisplayed()
    }

    private fun baseState(context: android.content.Context): AppUiState {
        return AppUiState(
            archive = Archive(context = context),
            day = Combo(context = context),
        )
    }
}
