package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeDown
import androidx.health.connect.client.records.MealType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.NutritionFoodEditDraft
import com.makstuff.minimalistcaloriecounter.classes.QuickImportParser
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppTopBar
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionMealGroup
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionServingGroup
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
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
        var copied by mutableStateOf(false)

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                AppTopBar(
                    title = "Meals",
                    currentRoute = AppRoutes.HEALTH_CONNECT_NUTRITION,
                    onOpenMenu = {},
                    onOpenQuickImportSettings = {},
                    onOpenGoalsSettings = {},
                    mealsDayCopied = copied,
                    onCopyMealsDay = { copied = true },
                    onExportMealsDay = {},
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
        var exported = false

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                AppTopBar(
                    title = "Meals",
                    currentRoute = AppRoutes.HEALTH_CONNECT_NUTRITION,
                    onOpenMenu = {},
                    onOpenQuickImportSettings = {},
                    onOpenGoalsSettings = {},
                    onCopyMealsDay = {},
                    onExportMealsDay = { exported = true },
                )
            }
        }

        composeRule.onNodeWithTag("meals_day_actions").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("meals_day_export_summary").assertIsDisplayed().performClick()
        assertTrue(exported)
    }

    @Test
    fun daySummaryAddMealOpensParserDrawerAndSavesParsedMeal() {
        var state by mutableStateOf(baseState().copy(
            healthConnectViewerDate = LocalDate.of(2026, 7, 2),
            healthConnectViewerLoading = false,
            healthConnectViewerMessage = null,
        ))
        var preparedDate: LocalDate? = null
        var saved = false

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = state,
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onPrepareAddMeal = { preparedDate = it },
                    onRepeatMealGroup = { _, _ -> },
                    onTextChange = { text ->
                        state = state.copy(
                            inputQuickImportText = text,
                            quickImportMeal = QuickImportParser.parse(text),
                            quickImportError = null,
                        )
                    },
                    onRefreshDateTime = {},
                    onDateTimeChange = { state = state.copy(inputQuickImportDateTime = it) },
                    onMealTypeChange = { state = state.copy(quickImportMealTypeOverride = it) },
                    onParsedFoodChange = { _, _ -> },
                    onImport = { saved = true },
                    onClear = { state = state.copy(inputQuickImportText = "") },
                    onRetryOutbox = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("meals_add_meal").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("meals_add_meal_drawer").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Meal time actions").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Edit meal type").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Clear nutrition blurb").assertCountEquals(0)
        composeRule.onNodeWithTag("quick_import_meal_time_panel").performClick()
        composeRule.onNodeWithText("Meal type").assertIsDisplayed()
        composeRule.onNodeWithText("Snack").performClick()
        composeRule.onNodeWithTag("quick_import_paste").performTextInput(
            "100g test oats; Calories 389, Fat 6.9g, Sat Fat 1.2g, Carbs 66.3g, Fiber 10.6g, Sugar 0.9g, Protein 16.9g. Meal totals; Calories 389, Fat 6.9g, Sat Fat 1.2g, Carbs 66.3g, Fiber 10.6g, Sugar 0.9g, Protein 16.9g.",
        )
        composeRule.onNodeWithContentDescription("Clear nutrition blurb").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_import_save_meal_button").performScrollTo().assertIsDisplayed().performClick()

        assertEquals(LocalDate.of(2026, 7, 2), preparedDate)
        assertTrue(saved)
    }

    @Test
    fun drawerClearPreservesSelectedAddMealDateTime() {
        val selectedDateTime = LocalDateTime.of(2026, 7, 2, 18, 0)
        var state by mutableStateOf(baseState().copy(
            healthConnectViewerDate = selectedDateTime.toLocalDate(),
            healthConnectViewerLoading = false,
            inputQuickImportDateTime = selectedDateTime,
            inputQuickImportText = "stale",
        ))
        var cleared = false
        var updatedDateTime: LocalDateTime? = null

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = state,
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {},
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onPrepareAddMeal = {},
                    onRepeatMealGroup = { _, _ -> },
                    onTextChange = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {
                        updatedDateTime = it
                        state = state.copy(inputQuickImportDateTime = it)
                    },
                    onMealTypeChange = {},
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {
                        cleared = true
                        state = state.copy(
                            inputQuickImportText = "",
                            inputQuickImportDateTime = LocalDateTime.of(2026, 7, 5, 9, 0),
                        )
                    },
                    onRetryOutbox = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("meals_add_meal").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Clear nutrition blurb").performClick()

        assertTrue(cleared)
        assertEquals(selectedDateTime, updatedDateTime)
    }

    @Test
    fun mealRepeatEmitsSelectedDateAndOpensAddMealDrawer() {
        val selectedDate = LocalDate.of(2026, 7, 2)
        var repeatedDate: LocalDate? = null
        var repeatedFoods = 0

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = selectedDate,
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
                    onPrepareAddMeal = {},
                    onRepeatMealGroup = { date, foods ->
                        repeatedDate = date
                        repeatedFoods = foods.size
                    },
                    onTextChange = {},
                    onRefreshDateTime = {},
                    onDateTimeChange = {},
                    onMealTypeChange = {},
                    onParsedFoodChange = { _, _ -> },
                    onImport = {},
                    onClear = {},
                    onRetryOutbox = {},
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("meal_card_lunch", useUnmergedTree = true).assertIsDisplayed().performTouchInput {
            click(Offset(32f, 24f))
        }
        composeRule.onNodeWithTag("meals_meal_detail_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("meals_repeat_meal_group").performScrollTo().assertIsDisplayed().performClick()

        assertEquals(selectedDate, repeatedDate)
        assertEquals(1, repeatedFoods)
        composeRule.onNodeWithTag("meals_add_meal_drawer").assertIsDisplayed()
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

        composeRule.onNodeWithText("Calories").performTextClearance()
        composeRule.onNodeWithText("Calories").performTextInput("120")
        composeRule.onNodeWithText("Carbs").performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) { savedDraft != null }

        assertEquals(120.0, savedDraft?.energy ?: 0.0, 0.001)
    }

    @Test
    fun foodDetailDismissSubmitsPendingDraft() {
        var savedDraft: NutritionFoodEditDraft? = null
        var dismissed = false

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                FoodDetailDialog(
                    servingGroup = NutritionServingGroup(listOf(sampleMeal(name = "Whiskey", minuteOffset = 0))),
                    onDismiss = { dismissed = true },
                    onDelete = {},
                    onAddServing = {},
                    onRemoveServing = {},
                    onSaveEdit = { savedDraft = it },
                )
            }
        }

        composeRule.onNodeWithText("Calories").performTextClearance()
        composeRule.onNodeWithText("Calories").performTextInput("130")
        composeRule.onNodeWithTag("meals_food_detail_sheet").performTouchInput { swipeDown() }
        composeRule.waitUntil(timeoutMillis = 2_000) { dismissed }

        assertEquals(130.0, savedDraft?.energy ?: 0.0, 0.001)
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
