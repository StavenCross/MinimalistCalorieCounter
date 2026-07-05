package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.health.connect.client.records.MealType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ScreenHealthConnectNutritionQuantityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quantityChangeKeepsFoodDrawerOpen() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                var meals by remember { mutableStateOf(listOf(sampleMeal(name = "Whiskey"))) }
                ScreenHealthConnectNutrition(
                    uiState = baseState().copy(
                        healthConnectViewerDate = LocalDate.of(2026, 7, 2),
                        healthConnectViewerLoading = false,
                        healthConnectViewerMessage = null,
                        healthConnectViewerMeals = meals,
                    ),
                    onDateChange = {},
                    onRefresh = {},
                    onDeleteMeal = {},
                    onDeleteMealGroup = {},
                    onAddFoodServing = {
                        meals = meals + it.copy(recordId = "record-added", clientRecordId = "client-added")
                    },
                    onRemoveFoodServing = {},
                    onSaveFoodServingGroup = { _, _ -> },
                    onRepeatMealGroup = { _, _ -> },
                    onExportDaySummary = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Whiskey").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("meals_food_detail_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("meals_food_quantity_increment").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("meals_food_detail_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("meals_food_quantity_value").assertTextEquals("2")
    }

    private fun baseState(): AppUiState {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return AppUiState(
            archive = Archive(context = context),
            day = Combo(context = context),
        )
    }

    private fun sampleMeal(name: String): HealthConnectNutritionMeal {
        return HealthConnectNutritionMeal(
            recordId = "record-1",
            clientRecordId = "client-1",
            startTime = LocalDateTime.of(2026, 7, 2, 12, 0),
            endTime = LocalDateTime.of(2026, 7, 2, 12, 1),
            name = name,
            energy = 97.0,
            energyFromFat = 0.0,
            totalCarbohydrate = 0.0,
            sugar = 0.0,
            protein = 0.0,
            totalFat = 0.0,
            saturatedFat = 0.0,
            dietaryFiber = 0.0,
            mealType = MealType.MEAL_TYPE_SNACK,
        )
    }
}
