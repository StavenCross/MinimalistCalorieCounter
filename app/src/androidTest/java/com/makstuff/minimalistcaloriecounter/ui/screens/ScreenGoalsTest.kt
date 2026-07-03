package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.GoalFieldKey
import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ScreenGoalsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsShowsMissingFieldsAndSexPickerDrawer() {
        var selectedSex: GoalSex? = null

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(goals = Goals(settingsVisible = true)),
                    onSettingsDismiss = {},
                    onRefreshHealthConnect = {},
                    onRecalculate = {},
                    onApplyRecommendation = {},
                    onDismissRecommendation = {},
                    onBirthdayChange = {},
                    onSexChange = { selectedSex = it },
                    onActivityLevelChange = {},
                    onWeightLossTargetChange = {},
                    onMeasurementChange = { _, _ -> },
                    onMeasurementLockToggle = {},
                    onMacroChange = { _, _ -> },
                    onMacroLockToggle = {},
                )
            }
        }

        composeRule.onNodeWithText("Missing birthday, sex, height, weight, lean mass or body fat").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_sex_picker").performClick()
        composeRule.onNodeWithTag("goals_sex_option_Female").assertIsDisplayed().performClick()
        assertEquals(GoalSex.Female, selectedSex)
    }

    @Test
    fun profileAndMacroFieldsEmitChanges() {
        var measurement: Pair<GoalFieldKey, Double?>? = null
        var macro: Pair<GoalMacro, Double?>? = null

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        goals = Goals(
                            profile = GoalProfile(
                                birthday = LocalDate.of(1988, 1, 1),
                                sex = GoalSex.Male,
                                heightCm = GoalMeasurement(180.0),
                                weightKg = GoalMeasurement(95.0),
                                bodyFatPercent = GoalMeasurement(25.0),
                            ),
                            currentTargets = MacroTargets(calories = 2100.0, protein = 160.0, carbs = 220.0, fat = 60.0, fiber = 30.0),
                            settingsVisible = true,
                        )
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
                    onMeasurementChange = { field, value -> measurement = field to value },
                    onMeasurementLockToggle = {},
                    onMacroChange = { field, value -> macro = field to value },
                    onMacroLockToggle = {},
                )
            }
        }

        composeRule.onNodeWithTag("goals_birthday_picker").assertIsDisplayed()

        composeRule.onNodeWithTag("goals_measurement_HeightCm").performTextClearance()
        composeRule.onNodeWithTag("goals_measurement_HeightCm").performTextInput("181")
        assertEquals(GoalFieldKey.HeightCm, measurement?.first)
        assertEquals(181.0, measurement?.second ?: 0.0, 0.01)

        composeRule.onNodeWithTag("goals_macro_Calories").performTextClearance()
        composeRule.onNodeWithTag("goals_macro_Calories").performTextInput("2050")
        assertEquals(GoalMacro.Calories, macro?.first)
        assertEquals(2050.0, macro?.second ?: 0.0, 0.01)
    }

    @Test
    fun birthdayPickerOpensDateDrawer() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(goals = Goals(settingsVisible = true)),
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

        composeRule.onNodeWithTag("goals_birthday_picker").performClick()
        composeRule.onNodeWithText("Tap a date to set it.").assertIsDisplayed()
    }

    @Test
    fun activityAndWeightLossPickersEmitChanges() {
        var activity: ActivityLevel? = null
        var target: WeeklyWeightLossTarget? = null

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(goals = Goals(settingsVisible = true)),
                    onSettingsDismiss = {},
                    onRefreshHealthConnect = {},
                    onRecalculate = {},
                    onApplyRecommendation = {},
                    onDismissRecommendation = {},
                    onBirthdayChange = {},
                    onSexChange = {},
                    onActivityLevelChange = { activity = it },
                    onWeightLossTargetChange = { target = it },
                    onMeasurementChange = { _, _ -> },
                    onMeasurementLockToggle = {},
                    onMacroChange = { _, _ -> },
                    onMacroLockToggle = {},
                )
            }
        }

        composeRule.onNodeWithTag("goals_activity_picker").performClick()
        composeRule.onNodeWithTag("goals_activity_option_ModeratelyActive").performClick()
        assertEquals(ActivityLevel.ModeratelyActive, activity)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("goals_activity_option_ModeratelyActive")
                .fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag("goals_weight_loss_picker").performScrollTo()
        composeRule.onNodeWithTag("goals_weight_loss_picker").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("goals_weight_loss_option_OnePound").performClick()
        assertEquals(WeeklyWeightLossTarget.OnePound, target)
    }

    @Test
    fun historyCardShowsSavedRecommendationContext() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        goals = Goals(
                            currentTargets = MacroTargets(calories = 2050.0),
                            history = listOf(
                                GoalHistoryEntry(
                                    effectiveDate = LocalDate.of(2026, 7, 3),
                                    targets = MacroTargets(calories = 2050.0),
                                    source = "recommended",
                                    bmr = 1850.0,
                                    tdee = 2550.0,
                                    weightKg = 90.0,
                                    leanMassKg = 72.0,
                                    weightLossTarget = WeeklyWeightLossTarget.OnePound,
                                )
                            ),
                        )
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

        composeRule.onNodeWithText("Recommendation history").assertIsDisplayed()
        composeRule.onNodeWithText("Recommended • BMR 1850 • TDEE 2550").assertIsDisplayed()
        composeRule.onNodeWithText("90 kg • 72 kg lean • 1 lb/week").assertIsDisplayed()
    }

    private fun baseState(): AppUiState {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return AppUiState(
            archive = Archive(context = context),
            day = Combo(context = context),
        )
    }
}
