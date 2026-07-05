package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.GoalFieldKey
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalStatusState
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.classes.statusState
import com.makstuff.minimalistcaloriecounter.ui.model.goalAdherenceCards
import com.makstuff.minimalistcaloriecounter.ui.model.goalBodyTrendCards
import com.makstuff.minimalistcaloriecounter.ui.model.sumNutrition
import java.time.LocalDate

private enum class GoalDetailsDestination {
    CurrentGoal,
    Recommendation,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenGoals(
    uiState: AppUiState,
    onSettingsOpen: () -> Unit,
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
    val statusState = goals.statusState()
    val bodyTrendCards = goalBodyTrendCards(goals)
    val dayTotals = uiState.healthConnectViewerMeals.sumNutrition()
    val adherenceCards = goalAdherenceCards(dayTotals, activeTargets)
    var detailsDestination by remember { mutableStateOf<GoalDetailsDestination?>(null) }

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
    detailsDestination?.let { destination ->
        ModalBottomSheet(
            onDismissRequest = { detailsDestination = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            GoalsDetailsSheet(
                goals = goals,
                targets = activeTargets,
                mode = when (destination) {
                    GoalDetailsDestination.CurrentGoal -> GoalDetailsMode.CurrentGoal
                    GoalDetailsDestination.Recommendation -> GoalDetailsMode.Recommendation
                },
                onRecalculate = onRecalculate,
                onApplyRecommendation = onApplyRecommendation,
                onDismissRecommendation = onDismissRecommendation,
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GoalHeroCard(
                statusState = statusState,
                targets = activeTargets,
                message = goals.message,
                onPrimaryAction = {
                    when (statusState) {
                        is GoalStatusState.ProfileIncomplete -> onSettingsOpen()
                        is GoalStatusState.NewRecommendation -> detailsDestination = GoalDetailsDestination.Recommendation
                        GoalStatusState.CurrentGoal -> detailsDestination = GoalDetailsDestination.CurrentGoal
                    }
                },
            )
        }

        item {
            GoalProgressCard(
                totals = dayTotals,
                targets = activeTargets,
                date = uiState.healthConnectViewerDate,
            )
        }

        item {
            ProfileSnapshotCard(uiState = uiState)
        }

        item {
            RecentTrendCard(
                bodyCards = bodyTrendCards,
                adherenceCards = adherenceCards,
                adherenceDate = uiState.healthConnectViewerDate,
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
