package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.GoalFieldKey
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalRecalculationSchedule
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.ui.model.goalAdherenceCards
import com.makstuff.minimalistcaloriecounter.ui.model.goalBodyTrendCards
import com.makstuff.minimalistcaloriecounter.ui.model.sumNutrition
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenGoals(
    uiState: AppUiState,
    onSettingsDismiss: () -> Unit,
    onRefreshHealthConnect: () -> Unit,
    onRecalculate: () -> Unit,
    onApplyRecommendation: () -> Unit,
    onDismissRecommendation: () -> Unit,
    onBirthdayChange: (LocalDate?) -> Unit,
    onSexChange: (GoalSex) -> Unit,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    onWeightLossTargetChange: (WeeklyWeightLossTarget) -> Unit,
    onMeasurementChange: (GoalFieldKey, Double?) -> Unit,
    onMeasurementLockToggle: (GoalFieldKey) -> Unit,
    onMacroChange: (GoalMacro, Double?) -> Unit,
    onMacroLockToggle: (GoalMacro) -> Unit,
) {
    val goals = uiState.goals
    val activeTargets = goals.activeTargetsFor(LocalDate.now())
    val recalculationStatus = GoalRecalculationSchedule.status(goals)
    val bodyTrendCards = goalBodyTrendCards(goals)
    val adherenceCards = goalAdherenceCards(uiState.healthConnectViewerMeals.sumNutrition(), activeTargets)

    if (goals.settingsVisible) {
        GoalsSettingsSheet(
            uiState = uiState,
            onDismiss = onSettingsDismiss,
            onRefreshHealthConnect = onRefreshHealthConnect,
            onRecalculate = onRecalculate,
            onBirthdayChange = onBirthdayChange,
            onSexChange = onSexChange,
            onActivityLevelChange = onActivityLevelChange,
            onWeightLossTargetChange = onWeightLossTargetChange,
            onMeasurementChange = onMeasurementChange,
            onMeasurementLockToggle = onMeasurementLockToggle,
            onMacroChange = onMacroChange,
            onMacroLockToggle = onMacroLockToggle,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GoalHeroCard(
                targets = activeTargets,
                profileComplete = goals.profile.isRequiredComplete(),
                message = goals.message,
                onRecalculate = onRecalculate,
            )
        }

        goals.recommendation?.let { recommendation ->
            item {
                RecommendationCard(
                    currentTargets = activeTargets,
                    recommendedTargets = recommendation.targets,
                    bmr = recommendation.bmr,
                    tdee = recommendation.tdee,
                    warning = recommendation.warning,
                    onApply = onApplyRecommendation,
                    onDismiss = onDismissRecommendation,
                )
            }
        }

        item {
            RecalculationCard(
                status = recalculationStatus,
                onRecalculate = onRecalculate,
            )
        }

        if (goals.history.isNotEmpty()) {
            item {
                GoalHistoryCard(entries = goals.history)
            }
        }

        item {
            MacroTargetGrid(targets = activeTargets)
        }

        item {
            ProfileSnapshotCard(uiState = uiState)
        }

        item {
            GoalTrendCards(
                bodyCards = bodyTrendCards,
                adherenceCards = adherenceCards,
                adherenceDate = uiState.healthConnectViewerDate,
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
