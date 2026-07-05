package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.health.connect.client.records.MealType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.NutritionFoodEditDraft
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionMealGroup
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionServingGroup
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ScreenHealthConnectNutritionActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun daySummaryCopyButtonShowsCopiedState() {
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

        composeRule.onNodeWithTag("meals_day_actions").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("meals_day_copy_summary").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("meals_day_actions").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Day copied").assertIsDisplayed()
    }

    @Test
    fun mealSummaryCopyButtonShowsCopiedState() {
        var copied = false

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                MealDetailDialog(
                    group = sampleGroup(),
                    copied = false,
                    onDismiss = {},
                    onCopy = { copied = true },
                    onDelete = {},
                    onRepeat = {},
                    onFoodClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("meals_meal_copy_summary").assertIsDisplayed().performClick()
        assertTrue(copied)
    }

    @Test
    fun mealDetailDeleteEmitsGroupRecordIds() {
        var deleteClicked = false

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                MealDetailDialog(
                    group = sampleGroup(),
                    copied = false,
                    onDismiss = {},
                    onCopy = {},
                    onDelete = { deleteClicked = true },
                    onRepeat = {},
                    onFoodClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("meals_delete_meal_group").assertIsDisplayed().performClick()
        assertTrue(deleteClicked)
    }

    @Test
    fun mealDetailRepeatEmitsGroupFoods() {
        var repeatClicked = false

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                MealDetailDialog(
                    group = sampleGroup(),
                    copied = false,
                    onDismiss = {},
                    onCopy = {},
                    onDelete = {},
                    onRepeat = { repeatClicked = true },
                    onFoodClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("meals_repeat_meal_group").assertIsDisplayed().performClick()
        assertTrue(repeatClicked)
    }

    @Test
    fun daySummaryExportEmitsSelectedDateAndSummary() {
        var exportedDate: LocalDate? = null
        var exportedSummary = ""

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
                    onExportDaySummary = { date, summary ->
                        exportedDate = date
                        exportedSummary = summary
                    },
                )
            }
        }

        composeRule.onNodeWithTag("meals_day_actions").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("meals_day_export_summary").assertIsDisplayed().performClick()
        assertEquals(LocalDate.of(2026, 7, 2), exportedDate)
        assertTrue(exportedSummary.contains("Meals for 2026-07-02"))
    }

    @Test
    fun foodDetailQuantityButtonsEmitServingActions() {
        var added = false
        var removed = false

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                FoodDetailDialog(
                    servingGroup = NutritionServingGroup(
                        listOf(
                            sampleMeal(name = "Whiskey", minuteOffset = 0),
                            sampleMeal(name = "Whiskey", minuteOffset = 0).copy(recordId = "record-2", clientRecordId = "client-2"),
                        )
                    ),
                    onDismiss = {},
                    onDelete = {},
                    onAddServing = { added = true },
                    onRemoveServing = { removed = true },
                    onSaveEdit = {},
                )
            }
        }

        composeRule.onNodeWithTag("meals_food_quantity_value").assertIsDisplayed()
        composeRule.onNodeWithTag("meals_food_quantity_increment").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("meals_food_quantity_decrement").assertIsDisplayed().performClick()
        assertTrue(added)
        assertTrue(removed)
    }

    @Test
    fun foodDetailEditSavesMacroDraftForServingGroup() {
        var savedDraft: NutritionFoodEditDraft? = null

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                FoodDetailDialog(
                    servingGroup = NutritionServingGroup(listOf(sampleMeal(name = "Whiskey", minuteOffset = 0))),
                    onDismiss = {},
                    onDelete = {},
                    onAddServing = {},
                    onRemoveServing = {},
                    onSaveEdit = { savedDraft = it },
                )
            }
        }

        composeRule.onNodeWithTag("meals_food_edit_save").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Calories").performTextClearance()
        composeRule.onNodeWithText("Calories").performTextInput("120")
        composeRule.onNodeWithTag("meals_food_edit_save").performScrollTo().assertIsDisplayed().performClick()

        assertEquals(120.0, savedDraft?.energy ?: 0.0, 0.001)
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

    private fun sampleGroup(): NutritionMealGroup {
        return NutritionMealGroup(
            mealType = MealType.MEAL_TYPE_LUNCH,
            label = "Lunch",
            colorArgb = 0xFF64B5F6,
            foods = listOf(
                sampleMeal(name = "food 1", minuteOffset = 0),
                sampleMeal(name = "food 2", minuteOffset = 1),
            ),
        )
    }
}
