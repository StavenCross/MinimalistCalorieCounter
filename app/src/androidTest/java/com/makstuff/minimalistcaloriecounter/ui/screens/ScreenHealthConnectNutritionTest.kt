package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.health.connect.client.records.MealType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ScreenHealthConnectNutritionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dateHeaderButtonsEmitDateChangesAndPullRefreshes() {
        val selectedDate = LocalDate.of(2026, 7, 2)
        val requestedDates = mutableListOf<LocalDate>()
        var refreshCount = 0

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = selectedDate,
                        healthConnectViewerLoading = false,
                        healthConnectViewerMessage = null,
                        healthConnectViewerMeals = listOf(sampleMeal()),
                    ),
                    onDateChange = { requestedDates += it },
                    onRefresh = { refreshCount++ },
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onRepeatMealGroup = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Jul 2, 2026").assertIsDisplayed()
        composeRule.onNodeWithTag("meals_previous_day").performClick()
        composeRule.onNodeWithTag("meals_next_day").performClick()
        val beforePull = refreshCount
        composeRule.onRoot().performTouchInput { swipeDown() }
        composeRule.waitUntil(timeoutMillis = 2_000) { refreshCount > beforePull }

        assertTrue(LocalDate.of(2026, 7, 1) in requestedDates)
        assertTrue(LocalDate.of(2026, 7, 3) in requestedDates)
        assertTrue(refreshCount >= 2)
    }

    @Test
    fun tappingDateOpensDatePickerDrawer() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = LocalDate.of(2026, 7, 2),
                        healthConnectViewerLoading = false,
                    ),
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onRepeatMealGroup = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("meals_date_picker").performClick()
        composeRule.onNodeWithText("Set date").assertIsDisplayed()
    }

    @Test
    fun foodAndMealRowsOpenDetailDrawers() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = LocalDate.of(2026, 7, 2),
                        healthConnectViewerLoading = false,
                        healthConnectViewerMessage = null,
                        healthConnectViewerMeals = listOf(sampleMeal()),
                    ),
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onRepeatMealGroup = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("100 g test oats").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Delete food").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Edit food").assertIsDisplayed()
        composeRule.onAllNodesWithText("Close").assertCountEquals(0)
    }

    @Test
    fun goalProgressArcsStayVisibleWhenTargetsAreMissing() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = LocalDate.of(2026, 7, 2),
                        healthConnectViewerLoading = false,
                        healthConnectViewerMessage = null,
                        healthConnectViewerMeals = listOf(sampleMeal()),
                    ),
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onRepeatMealGroup = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Calories goal progress").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Protein goal progress").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Carbs goal progress").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Fat goal progress").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Fiber goal progress").assertIsDisplayed()
    }

    @Test
    fun mealCardShowsAllMacroChips() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = LocalDate.of(2026, 7, 2),
                        healthConnectViewerLoading = false,
                        healthConnectViewerMessage = null,
                        healthConnectViewerMeals = listOf(sampleMeal()),
                    ),
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onRepeatMealGroup = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onAllNodesWithTag("meal_macro_carbs", useUnmergedTree = true)[0].performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("meal_macro_protein", useUnmergedTree = true)[0].performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("meal_macro_fat", useUnmergedTree = true)[0].performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("meal_macro_fiber", useUnmergedTree = true)[0].performScrollTo().assertIsDisplayed()
    }

    @Test
    fun longMealStartsCollapsedAndExpandsInline() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = LocalDate.of(2026, 7, 2),
                        healthConnectViewerLoading = false,
                        healthConnectViewerMessage = null,
                        healthConnectViewerMeals = listOf(
                            sampleMeal(name = "food 1", minuteOffset = 0),
                            sampleMeal(name = "food 2", minuteOffset = 1),
                            sampleMeal(name = "food 3", minuteOffset = 2),
                            sampleMeal(name = "food 4", minuteOffset = 3),
                        ),
                    ),
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onRepeatMealGroup = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("food 1").assertIsDisplayed()
        composeRule.onAllNodesWithText("food 4").assertCountEquals(0)
        composeRule.onNodeWithTag("meal_expand_toggle_lunch").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("food 4").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Collapse Lunch").assertIsDisplayed()
    }

    @Test
    fun tappingMacroProgressShowsTemporaryDescription() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = LocalDate.of(2026, 7, 2),
                        healthConnectViewerLoading = false,
                        healthConnectViewerMessage = null,
                        healthConnectViewerMeals = listOf(sampleMeal()),
                    ),
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onRepeatMealGroup = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Fat goal progress").performClick()
        composeRule.onNodeWithText("Fat is dietary fat grams, including saturated fat.").assertIsDisplayed()
    }

    private fun baseState(): AppUiState {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return AppUiState(
            archive = Archive(context = context),
            day = Combo(context = context),
        )
    }

    private fun sampleMeal(
        name: String = "100 g test oats",
        minuteOffset: Int = 0,
    ): HealthConnectNutritionMeal {
        return HealthConnectNutritionMeal(
            recordId = "record-$minuteOffset",
            clientRecordId = "client-$minuteOffset",
            startTime = LocalDateTime.of(2026, 7, 2, 12, minuteOffset),
            endTime = LocalDateTime.of(2026, 7, 2, 12, minuteOffset + 1),
            name = name,
            energy = 389.0,
            energyFromFat = 62.1,
            totalCarbohydrate = 66.3,
            sugar = 0.9,
            protein = 16.9,
            totalFat = 6.9,
            saturatedFat = 1.2,
            dietaryFiber = 10.6,
            mealType = MealType.MEAL_TYPE_LUNCH,
        )
    }
}
