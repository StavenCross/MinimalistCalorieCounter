package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.health.connect.client.records.MealType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ScreenGoalsTrendTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun trendCardShowsBodyTrendAndLoadedDayAdherence() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        healthConnectViewerDate = LocalDate.of(2026, 7, 3),
                        healthConnectViewerMeals = listOf(meal()),
                        goals = Goals(
                            profile = GoalProfile(
                                weightKg = GoalMeasurement(89.0),
                                leanMassKg = GoalMeasurement(72.0),
                            ),
                            currentTargets = MacroTargets(calories = 2000.0, protein = 150.0, carbs = 200.0, fat = 80.0, fiber = 40.0),
                            history = listOf(
                                GoalHistoryEntry(
                                    effectiveDate = LocalDate.of(2026, 6, 28),
                                    targets = MacroTargets(calories = 2000.0),
                                    source = "recommended",
                                    weightKg = 90.0,
                                    leanMassKg = 71.5,
                                )
                            ),
                        ),
                    ),
                    onSettingsDismiss = {},
                    onRefreshHealthConnect = {},
                    onRecalculate = {},
                    onApplyRecommendation = {},
                    onDismissRecommendation = {},
                    onBirthdayChange = {},
                    onSexChange = {},
                    onActivityLevelChange = {},
                    onWeightLossTargetChange = {},
                    onMeasurementChange = { _, _ -> },
                    onMeasurementLockToggle = {},
                    onMacroChange = { _, _ -> },
                    onMacroLockToggle = {},
                )
            }
        }

        composeRule.onNodeWithText("Trends and adherence").assertIsDisplayed()
        composeRule.onNodeWithText("-1 kg since prior check").assertIsDisplayed()
        composeRule.onNodeWithText("50%").assertIsDisplayed()
    }

    private fun baseState(): AppUiState {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return AppUiState(archive = Archive(context = context), day = Combo(context = context))
    }

    private fun meal(): HealthConnectNutritionMeal {
        return HealthConnectNutritionMeal(
            recordId = "record-1",
            clientRecordId = "client-1",
            startTime = LocalDateTime.of(2026, 7, 3, 12, 0),
            endTime = LocalDateTime.of(2026, 7, 3, 12, 1),
            name = "test meal",
            energy = 1000.0,
            energyFromFat = null,
            totalCarbohydrate = 100.0,
            sugar = 0.0,
            protein = 75.0,
            totalFat = 40.0,
            saturatedFat = 0.0,
            dietaryFiber = 20.0,
            mealType = MealType.MEAL_TYPE_LUNCH,
        )
    }
}
