package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
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
import com.makstuff.minimalistcaloriecounter.classes.GoalRecommendation
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import androidx.health.connect.client.records.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

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
                    onSettingsOpen = {},
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

        assertTrue(composeRule.onAllNodesWithText("Birthday, sex, height, and weight are required.").fetchSemanticsNodes().isNotEmpty())
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
                                leanMassKg = GoalMeasurement(72.0),
                            ),
                            currentTargets = MacroTargets(calories = 2100.0, protein = 160.0, carbs = 220.0, fat = 60.0, fiber = 30.0),
                            settingsVisible = true,
                        )
                    ),
                    onSettingsOpen = {},
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
        assertTrue(composeRule.onAllNodesWithText("5' 11\"").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("209 lb").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("159 lb").fetchSemanticsNodes().isNotEmpty())

        composeRule.onNodeWithTag("goals_measurement_HeightCm").performClick()
        composeRule.onNodeWithTag("goals_height_feet_wheel").performScrollToIndex(3)
        composeRule.onNodeWithTag("goals_height_feet_6").performClick()
        composeRule.onNodeWithTag("goals_height_inches_wheel").performScrollToIndex(0)
        composeRule.onNodeWithTag("goals_height_inches_0").performClick()
        composeRule.onNodeWithTag("goals_height_set").performClick()
        assertEquals(GoalFieldKey.HeightCm, measurement?.first)
        assertEquals(182.88, measurement?.second ?: 0.0, 0.01)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("goals_height_set").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("goals_measurement_WeightKg").performClick()
        composeRule.onNodeWithTag("goals_weight_lb_wheel").performScrollToIndex(130)
        composeRule.onNodeWithTag("goals_weight_lb_210").performClick()
        composeRule.onNodeWithTag("goals_weight_set").performClick()
        assertEquals(GoalFieldKey.WeightKg, measurement?.first)
        assertEquals(95.25, measurement?.second ?: 0.0, 0.01)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("goals_weight_set").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("goals_measurement_LeanMassKg").performScrollTo()
        composeRule.onNodeWithTag("goals_measurement_LeanMassKg").performClick()
        composeRule.onNodeWithTag("goals_lean_mass_lb_wheel").performScrollToIndex(80)
        composeRule.onNodeWithTag("goals_lean_mass_lb_160").performClick()
        composeRule.onNodeWithTag("goals_lean_mass_set").performClick()
        assertEquals(GoalFieldKey.LeanMassKg, measurement?.first)
        assertEquals(72.57, measurement?.second ?: 0.0, 0.01)

        composeRule.onNodeWithTag("goals_macro_Calories").performTextClearance()
        composeRule.onNodeWithTag("goals_macro_Calories").performTextInput("2050")
        assertEquals(GoalMacro.Calories, macro?.first)
        assertEquals(2050.0, macro?.second ?: 0.0, 0.01)
    }

    @Test
    fun birthdayPickerOpensDateDrawer() {
        var birthday: LocalDate? = null
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        goals = Goals(
                            profile = GoalProfile(birthday = LocalDate.of(1988, 1, 1)),
                            settingsVisible = true,
                        )
                    ),
                    onSettingsOpen = {},
                    onSettingsDismiss = {},
                    onRefreshHealthConnect = {},
                    onRecalculate = {},
                    onApplyRecommendation = {},
                    onDismissRecommendation = {},
                    onBirthdayChange = { birthday = it },
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
        composeRule.waitForIdle()
        assertEquals(null, birthday)
    }

    @Test
    fun activityAndWeightLossPickersEmitChanges() {
        var activity: ActivityLevel? = null
        var target: WeeklyWeightLossTarget? = null

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(goals = Goals(settingsVisible = true)),
                    onSettingsOpen = {},
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
                            profile = completeProfile(),
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
                    onSettingsOpen = {},
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

        assertTrue(composeRule.onAllNodesWithText("Current goal").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithTag("goals_status_primary").performClick()
        assertTrue(composeRule.onAllNodesWithText("Current goal").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Goal timeline").performScrollTo().fetchSemanticsNode()
        composeRule.onNodeWithText("Started goal").fetchSemanticsNode()
        composeRule.onNodeWithText("Started").fetchSemanticsNode()
        assertTrue(composeRule.onAllNodesWithText("Baseline").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Recommended • BMR 1850 • TDEE 2550").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("198 lb").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("159 lb lean").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("1 lb/week").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithTag("goals_timeline_context_weight").performClick()
        composeRule.onNodeWithText("Weight is your latest body weight measurement.").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_timeline_context_lean_mass").performClick()
        composeRule.onNodeWithText("Lean mass is your weight excluding body fat.").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_timeline_context_weight_loss_target").performClick()
        composeRule.onNodeWithText("Weight loss target is the weekly pace used to adjust calories.").assertIsDisplayed()
    }

    @Test
    fun goalTimelineShowsTrendChartForMultipleSavedGoals() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        goals = Goals(
                            profile = completeProfile(),
                            currentTargets = MacroTargets(calories = 2146.0),
                            history = listOf(
                                GoalHistoryEntry(
                                    effectiveDate = LocalDate.of(2026, 7, 3),
                                    targets = MacroTargets(calories = 2896.0),
                                    source = "recommended",
                                ),
                                GoalHistoryEntry(
                                    effectiveDate = LocalDate.of(2026, 7, 4),
                                    targets = MacroTargets(calories = 2146.0),
                                    source = "recommended",
                                ),
                            ),
                        )
                    ),
                    onSettingsOpen = {},
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

        composeRule.onNodeWithTag("goals_status_primary").performClick()
        composeRule.onNodeWithTag("goal_history_trend_chart").performScrollTo().fetchSemanticsNode()
        composeRule.onNodeWithText("Updated goal").fetchSemanticsNode()
        assertTrue(composeRule.onAllNodesWithText("Baseline").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun currentGoalDrawerHidesSourceAndExplainsBodyStatusIcons() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        goals = Goals(
                            profile = completeProfile().copy(
                                heightCm = GoalMeasurement(180.0, locked = true),
                                leanMassKg = GoalMeasurement(),
                            ),
                            currentTargets = MacroTargets(calories = 2050.0, protein = 160.0, carbs = 220.0, fat = 60.0, fiber = 30.0),
                        )
                    ),
                    onSettingsOpen = {},
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

        assertTrue(composeRule.onAllNodesWithText("Current goal").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("Health Connect refreshes on app load").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("goals_status_primary").performClick()
        composeRule.onNodeWithText("Active targets").fetchSemanticsNode()
        assertTrue(composeRule.onAllNodesWithText("Daily macro plan").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Goal source").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("No calculated recommendation yet").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Body inputs").performScrollTo()
        composeRule.onNodeWithContentDescription("Locked").performClick()
        composeRule.onNodeWithText("Locked values stay manual and will not be replaced by Health Connect.").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithContentDescription("Estimated").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun incompleteProfileStatusReviewOpensSettings() {
        var settingsOpenCount = 0

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(goals = Goals()),
                    onSettingsOpen = { settingsOpenCount++ },
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

        composeRule.onNodeWithText("Complete your profile").assertIsDisplayed()
        composeRule.onNodeWithText("Review").assertIsDisplayed()
        composeRule.onNodeWithText("Birthday, sex, height, and weight are required.").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Missing birthday, sex, height, weight").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Required before recommendations").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("goals_status_primary").performClick()
        assertEquals(1, settingsOpenCount)
    }

    @Test
    fun newRecommendationStatusReviewOpensComparisonDrawerAndApplies() {
        var applyCount = 0

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        goals = Goals(
                            profile = completeProfile(),
                            currentTargets = MacroTargets(calories = 2000.0, protein = 160.0, carbs = 210.0, fat = 60.0, fiber = 30.0),
                            history = listOf(
                                GoalHistoryEntry(
                                    effectiveDate = LocalDate.of(2026, 7, 1),
                                    targets = MacroTargets(calories = 2000.0, protein = 160.0, carbs = 210.0, fat = 60.0, fiber = 30.0),
                                    source = "recommended",
                                )
                            ),
                            recommendation = GoalRecommendation(
                                generatedDate = LocalDate.of(2026, 7, 4),
                                targets = MacroTargets(calories = 2100.0, protein = 165.0, carbs = 220.0, fat = 62.0, fiber = 30.0),
                                bmr = 1850.0,
                                tdee = 2600.0,
                            ),
                        )
                    ),
                    onSettingsOpen = {},
                    onSettingsDismiss = {},
                    onRefreshHealthConnect = {},
                    onRecalculate = {},
                    onApplyRecommendation = { applyCount++ },
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

        composeRule.onNodeWithText("New goal available").assertIsDisplayed()
        composeRule.onNodeWithText("Your new plan is ready for review").assertIsDisplayed()
        composeRule.onNodeWithText("Review").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_status_primary").performClick()
        assertTrue(composeRule.onAllNodesWithText("Goal review").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("2000 kcal -> 2100 kcal").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("100 kcal").assertIsDisplayed()
        composeRule.onNodeWithText("5 g").assertIsDisplayed()
        composeRule.onNodeWithText("10 g").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Goal timeline").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Started").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Not now").assertIsDisplayed()
        composeRule.onNodeWithText("Apply").performClick()
        assertEquals(1, applyCount)
        assertTrue(composeRule.onAllNodesWithText("Goal review").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun firstGoalRecommendationShowsStartingTargetsInsteadOfEmptyChanges() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        goals = Goals(
                            profile = completeProfile(),
                            currentTargets = MacroTargets(),
                            recommendation = GoalRecommendation(
                                generatedDate = LocalDate.of(2026, 7, 4),
                                targets = MacroTargets(calories = 2100.0, protein = 165.0, carbs = 220.0, fat = 62.0, fiber = 30.0),
                                bmr = 1850.0,
                                tdee = 2600.0,
                            ),
                        )
                    ),
                    onSettingsOpen = {},
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

        composeRule.onNodeWithTag("goals_status_primary").performClick()
        composeRule.onNodeWithText("Based on your completed profile, these targets will become your starting plan.").assertIsDisplayed()
        composeRule.onNodeWithText("2100 kcal").assertIsDisplayed()
        composeRule.onNodeWithText("165 g").assertIsDisplayed()
        composeRule.onNodeWithText("220 g").assertIsDisplayed()
        composeRule.onNodeWithText("62 g").assertIsDisplayed()
        composeRule.onNodeWithText("30 g").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("No target changes need review right now.").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun todayProgressCaloriesCardShowsMacroHint() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                GoalProgressCard(
                    totals = QuickImportNutrients(
                        energy = 650.0,
                        protein = 70.0,
                        carbohydrate = 80.0,
                        fat = 20.0,
                        fiber = 12.0,
                        sugar = 8.0,
                        saturatedFat = 4.0,
                    ),
                    targets = MacroTargets(calories = 2100.0, protein = 160.0, carbs = 220.0, fat = 60.0, fiber = 30.0),
                    date = LocalDate.of(2026, 7, 5),
                )
            }
        }

        composeRule.onNodeWithTag("goals_calories_progress", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Calories are the meal's usable energy.").assertIsDisplayed()
    }

    @Test
    fun goalsProgressUsesTodayNotViewerDateMeals() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ScreenGoals(
                    uiState = baseState().copy(
                        goals = Goals(
                            profile = completeProfile(),
                            currentTargets = MacroTargets(calories = 2100.0),
                        ),
                        healthConnectViewerDate = LocalDate.now().minusDays(1),
                        healthConnectViewerMeals = listOf(sampleMeal(energy = 650.0)),
                    ),
                    onSettingsOpen = {},
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

        composeRule.onNodeWithTag("goals_calories_progress", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("0").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("650").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun profileSnapshotShowsEstimatedLeanMassWhenDirectValueMissing() {
        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                ProfileSnapshotCard(
                    uiState = baseState().copy(
                        goals = Goals(
                            profile = completeProfile().copy(leanMassKg = GoalMeasurement()),
                        ),
                    ),
                )
            }
        }

        assertTrue(composeRule.onAllNodesWithContentDescription("Estimated").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("159 lb").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_body_fact_sex", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Sex is used by the calorie formula for your current goal.").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_body_fact_age", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Age helps estimate your resting calorie burn.").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_body_fact_lifestyle", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Lifestyle estimates your daily activity level.").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_body_weight_card", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Weight is your latest body weight measurement.").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_body_metric_height", useUnmergedTree = true).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Height helps estimate your resting calorie burn.").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_body_metric_body_fat", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Body fat is the percent of your body weight from fat mass.").assertIsDisplayed()
        composeRule.onNodeWithTag("goals_body_metric_lean_mass", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Lean mass is your weight excluding body fat.").assertIsDisplayed()
    }

    @Test
    fun currentGoalHistoryEntryEmitsSelectedEntry() {
        val historyEntry = GoalHistoryEntry(
            effectiveDate = LocalDate.of(2026, 7, 4),
            targets = MacroTargets(calories = 2140.0),
            source = "recommended",
            weightKg = 90.0,
        )
        var deletedEntry: GoalHistoryEntry? = null

        composeRule.setContent {
            AppTheme(dynamicColor = false) {
                GoalsDetailsSheet(
                    goals = Goals(
                        profile = completeProfile(),
                        currentTargets = MacroTargets(calories = 2140.0),
                        history = listOf(historyEntry),
                    ),
                    targets = MacroTargets(calories = 2140.0),
                    mode = GoalDetailsMode.CurrentGoal,
                    onRecalculate = {},
                    onApplyRecommendation = {},
                    onDismissRecommendation = {},
                    onHistoryEntryClick = { deletedEntry = it },
                )
            }
        }

        composeRule.onNodeWithTag("goals_history_entry_2026-07-04", useUnmergedTree = true)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle {
            assertEquals(historyEntry, deletedEntry)
        }
    }

    private fun baseState(): AppUiState {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return AppUiState(
            archive = Archive(context = context),
            day = Combo(context = context),
        )
    }

    private fun completeProfile() = GoalProfile(
        birthday = LocalDate.of(1990, 7, 2),
        sex = GoalSex.Male,
        heightCm = GoalMeasurement(180.0),
        weightKg = GoalMeasurement(90.0),
        bodyFatPercent = GoalMeasurement(20.0),
        leanMassKg = GoalMeasurement(72.0),
    )

    private fun sampleMeal(energy: Double): HealthConnectNutritionMeal {
        return HealthConnectNutritionMeal(
            recordId = "goal-meal",
            clientRecordId = "goal-client",
            startTime = LocalDateTime.now(),
            endTime = LocalDateTime.now().plusMinutes(1),
            name = "test meal",
            energy = energy,
            energyFromFat = 0.0,
            totalCarbohydrate = 0.0,
            sugar = 0.0,
            protein = 0.0,
            totalFat = 0.0,
            saturatedFat = 0.0,
            dietaryFiber = 0.0,
            mealType = MealType.MEAL_TYPE_LUNCH,
        )
    }
}
