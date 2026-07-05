package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalRecommendation
import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.GoalStatusState
import com.makstuff.minimalistcaloriecounter.classes.GoalTargetDifference
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.classes.meaningfulDifferences
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.ui.model.GoalTrendCard
import com.makstuff.minimalistcaloriecounter.ui.reused.MacroHintBox
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetTitle
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import java.time.LocalDate

internal enum class GoalDetailsMode {
    CurrentGoal,
    Recommendation,
}

@Composable
internal fun GoalHeroCard(
    statusState: GoalStatusState,
    targets: MacroTargets,
    message: String?,
    onPrimaryAction: () -> Unit,
) {
    val title = when (statusState) {
        is GoalStatusState.ProfileIncomplete -> "Complete your profile"
        is GoalStatusState.NewRecommendation -> "New goal available"
        GoalStatusState.CurrentGoal -> "Current goal"
    }
    val action = when (statusState) {
        GoalStatusState.CurrentGoal -> "View"
        else -> "Review"
    }
    val headline = when (statusState) {
        is GoalStatusState.ProfileIncomplete -> null
        is GoalStatusState.NewRecommendation -> "Your new plan is ready for review"
        GoalStatusState.CurrentGoal -> targets.calories?.let { "${it.toFormattedString(true)} kcal active" } ?: "Targets are not set yet"
    }
    val isIncomplete = statusState is GoalStatusState.ProfileIncomplete
    val headerIcon = if (isIncomplete) Icons.Default.Warning else Icons.Default.TrackChanges
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        contentPadding = 16,
        verticalSpacing = 14,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = AccentGoals,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AccentGoals.copy(alpha = 0.16f))
                            .padding(6.dp)
                            .size(20.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (headline != null) {
                    Text(
                        text = headline,
                        style = if (statusState is GoalStatusState.NewRecommendation) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.displaySmall
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = message ?: "Birthday, sex, height, and weight are required.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Button(onClick = onPrimaryAction, modifier = Modifier.testTag("goals_status_primary")) {
                    Text(action)
                }
            }
        }
    }
}

@Composable
internal fun RecommendationCard(
    currentTargets: MacroTargets,
    recommendedTargets: MacroTargets,
    differences: List<GoalTargetDifference>,
    bmr: Double,
    tdee: Double,
    warning: String?,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shownDifferences = differences.filter { it.delta != 0.0 }
    val isFirstPlan = shownDifferences.any { it.current == null }
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = AccentGoals.copy(alpha = 0.42f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.AutoMirrored.Filled.TrendingDown, AccentGoals, 42)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Goal review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = if (isFirstPlan) {
                "Based on your completed profile, these targets will become your starting plan."
            } else {
                "Based on your latest profile values, this plan gently adjusts the targets that moved enough to matter."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GoalDeltaList(differences = shownDifferences)
        if (shownDifferences.isEmpty()) {
            Text(
                text = "No target changes need review right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (isFirstPlan) {
                "Future reviews will show what moved up or down from this baseline."
            } else {
                "Your daily burn estimate moved from your latest profile update."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        warning?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Not now")
            }
            Button(onClick = onApply, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Apply")
            }
        }
    }
}

@Composable
internal fun GoalsDetailsSheet(
    goals: com.makstuff.minimalistcaloriecounter.classes.Goals,
    targets: MacroTargets,
    mode: GoalDetailsMode,
    onRecalculate: () -> Unit,
    onApplyRecommendation: () -> Unit,
    onDismissRecommendation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SheetTitle(
            title = if (mode == GoalDetailsMode.Recommendation) "Goal review" else "Current goal",
            subtitle = if (mode == GoalDetailsMode.Recommendation) {
                "Compare the active plan against the new recommendation before applying it."
            } else {
                "Active targets, source details, and recent goal history."
            },
        )
        if (mode == GoalDetailsMode.Recommendation) {
            goals.recommendation?.let { recommendation ->
                val differences = targets.meaningfulDifferencesForDisplay(recommendation.targets)
                RecommendationCard(
                    currentTargets = targets,
                    recommendedTargets = recommendation.targets,
                    differences = differences,
                    bmr = recommendation.bmr,
                    tdee = recommendation.tdee,
                    warning = recommendation.warning,
                    onApply = onApplyRecommendation,
                    onDismiss = onDismissRecommendation,
                )
            }
        } else {
            CurrentGoalCard(targets = targets)
            DailyMacroPlanCard(targets = targets)
            if (goals.history.isNotEmpty()) {
                GoalHistoryCard(entries = goals.history)
            }
            BodyDetailsCard(profile = goals.profile)
        }
    }
}

@Composable
private fun CurrentGoalCard(targets: MacroTargets) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentIcon(Icons.Default.TrackChanges, AccentGoals, 42)
            Text("Active targets", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        TargetValueRow(GoalMacro.Calories, targets.calories, "kcal")
        TargetValueRow(GoalMacro.Protein, targets.protein, "g")
        TargetValueRow(GoalMacro.Carbs, targets.carbs, "g")
        TargetValueRow(GoalMacro.Fat, targets.fat, "g")
        TargetValueRow(GoalMacro.Fiber, targets.fiber, "g")
    }
}

@Composable
internal fun GoalRefreshCard(
    goals: com.makstuff.minimalistcaloriecounter.classes.Goals,
    onRecalculate: () -> Unit,
) {
    val lastGenerated = goals.recommendation?.generatedDate
        ?: goals.history.maxByOrNull { it.effectiveDate }?.generatedDate
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.Default.MonitorHeart, AccentGoals, 42)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Goal source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = lastGenerated?.let { "Last calculated $it" } ?: "No calculated recommendation yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRecalculate, modifier = Modifier.testTag("goals_recalculate_schedule")) {
                Text("Check now")
            }
        }
        Text(
            text = "Health Connect values refresh on app load for unlocked fields. New goals appear only when the recommendation meaningfully changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun MacroTargets.meaningfulDifferencesForDisplay(recommendedTargets: MacroTargets): List<GoalTargetDifference> {
    return meaningfulDifferences(recommendedTargets)
}

@Composable
internal fun GoalProgressCard(
    totals: QuickImportNutrients,
    targets: MacroTargets,
    date: LocalDate,
) {
    val calorieTarget = targets.calories
    val calorieProgress = progressFraction(totals.energy, calorieTarget)
    val remainingCalories = calorieTarget?.let { (it - totals.energy).coerceAtLeast(0.0) }
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 16,
        verticalSpacing = 14,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.Default.QueryStats, AccentGoals, 42)
            Text("Today's progress", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = calorieTarget?.let { "${(calorieProgress * 100.0).toFormattedString(true)}%" } ?: "Unset",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (targets.calories.isOver(totals.energy)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        MacroHintBox(label = "Calories") {
            SurfacePanel(
                modifier = Modifier.testTag("goals_calories_progress"),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                borderColor = AccentGoals.copy(alpha = 0.22f),
                contentPadding = 12,
                verticalSpacing = 8,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = totals.energy.toFormattedString(true),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = remainingCalories?.let { "${it.toFormattedString(true)} left" } ?: "Set target",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AccentIcon(Icons.Default.LocalFireDepartment, AccentGoals, 38)
                }
                GoalProgressBar(value = totals.energy, target = calorieTarget, color = AccentGoals)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GoalProgressChip(Icons.Default.EggAlt, AccentProtein, "Protein", totals.protein, targets.protein, "g", Modifier.weight(1f))
            GoalProgressChip(Icons.Default.BakeryDining, AccentCarbs, "Carbs", totals.carbohydrate, targets.carbs, "g", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GoalProgressChip(Icons.Default.OilBarrel, AccentFat, "Fat", totals.fat, targets.fat, "g", Modifier.weight(1f))
            GoalProgressChip(Icons.Default.Grass, AccentFiber, "Fiber", totals.fiber, targets.fiber, "g", Modifier.weight(1f))
        }
    }
}

@Composable
private fun GoalProgressChip(
    icon: ImageVector,
    color: Color,
    label: String,
    value: Double,
    target: Double?,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    MacroHintBox(label = label, modifier = modifier) {
        SurfacePanel(
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
            contentPadding = 10,
            verticalSpacing = 6,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentIcon(icon, color, 28)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${value.toFormattedString(true)} / ${target.formatTarget(suffix)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            GoalProgressBar(value = value, target = target, color = color)
        }
    }
}

@Composable
private fun GoalProgressBar(value: Double, target: Double?, color: Color) {
    val progress = progressFraction(value, target).toFloat()
    val progressColor = when {
        progress <= 0f -> Color.Transparent
        target.isOver(value) -> MaterialTheme.colorScheme.error
        else -> color
    }
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    )
}

private fun progressFraction(value: Double, target: Double?): Double {
    return if (target == null || target <= 0.0) 0.0 else (value / target).coerceIn(0.0, 1.0)
}

@Composable
internal fun GoalHistoryCard(entries: List<GoalHistoryEntry>) {
    val orderedEntries = entries.sortedBy { it.effectiveDate }.takeLast(6)
    val recentEntries = entries.sortedByDescending { it.effectiveDate }.take(3)
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.AutoMirrored.Filled.TrendingDown, AccentGoals, 42)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Goal timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${entries.size} saved ${if (entries.size == 1) "goal" else "goals"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (orderedEntries.size > 1) {
            HistoryTrendStrip(entries = orderedEntries)
        }
        recentEntries.forEachIndexed { index, entry ->
            GoalHistoryDecisionCard(entry = entry, previous = recentEntries.getOrNull(index + 1))
        }
    }
}

@Composable
private fun HistoryTrendStrip(entries: List<GoalHistoryEntry>) {
    val calorieValues = entries.mapNotNull { it.targets.calories }
    val minCalories = calorieValues.minOrNull() ?: 0.0
    val maxCalories = calorieValues.maxOrNull() ?: minCalories
    val range = (maxCalories - minCalories).takeIf { it > 0.0 } ?: 1.0
    val lineColor = AccentGoals
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        borderColor = AccentGoals.copy(alpha = 0.20f),
        contentPadding = 12,
        verticalSpacing = 8,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .testTag("goal_history_trend_chart"),
        ) {
            val leftPadding = 18.dp.toPx()
            val rightPadding = 18.dp.toPx()
            val topPadding = 14.dp.toPx()
            val bottomPadding = 16.dp.toPx()
            val usableWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val usableHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val step = usableWidth / (entries.size - 1).coerceAtLeast(1)
            val points = entries.mapIndexed { index, entry ->
                val calories = entry.targets.calories ?: minCalories
                val fraction = ((calories - minCalories) / range).coerceIn(0.0, 1.0)
                Offset(
                    x = leftPadding + step * index,
                    y = topPadding + usableHeight * (1f - fraction.toFloat()),
                )
            }
            drawLine(
                color = trackColor,
                start = Offset(leftPadding, topPadding + usableHeight),
                end = Offset(size.width - rightPadding, topPadding + usableHeight),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            points.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = lineColor.copy(alpha = 0.82f),
                    start = start,
                    end = end,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            points.forEach { point ->
                drawCircle(color = trackColor, radius = 9.dp.toPx(), center = point)
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = point)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = entry.targets.calories?.toFormattedString(true) ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${entry.effectiveDate.monthValue}/${entry.effectiveDate.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalHistoryDecisionCard(entry: GoalHistoryEntry, previous: GoalHistoryEntry?) {
    val delta = previous?.targets?.calories?.let { previousCalories ->
        entry.targets.calories?.minus(previousCalories)
    }
    val isFirstSavedGoal = previous == null
    val contextChips = listOfNotNull(
        entry.weightKg?.let { GoalContextChip(Icons.Default.Scale, AccentProfile, "${it.toImperialPounds()} lb") },
        entry.leanMassKg?.let { GoalContextChip(Icons.Default.EggAlt, AccentProtein, "${it.toImperialPounds()} lb lean") },
        entry.weightLossTarget?.let { GoalContextChip(Icons.Default.TrackChanges, AccentGoals, it.label) },
    )
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        contentPadding = 12,
        verticalSpacing = 9,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimelineBadge(delta = delta)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isFirstSavedGoal) "Started goal" else "Updated goal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${entry.effectiveDate.monthValue}/${entry.effectiveDate.dayOfMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.targets.calories.formatTarget("kcal"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (contextChips.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                contextChips.take(3).forEach { chip ->
                    GoalContextChipView(chip = chip, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class GoalContextChip(
    val icon: ImageVector,
    val color: Color,
    val label: String,
)

@Composable
private fun GoalContextChipView(chip: GoalContextChip, modifier: Modifier = Modifier) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = chip.color.copy(alpha = 0.16f),
        contentPadding = 8,
        verticalSpacing = 0,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(chip.icon, contentDescription = null, tint = chip.color, modifier = Modifier.size(16.dp))
            Text(
                text = chip.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GoalDeltaList(differences: List<GoalTargetDifference>) {
    if (differences.isEmpty()) return
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        contentPadding = 10,
        verticalSpacing = 8,
    ) {
        differences.forEach { difference ->
            GoalDeltaRow(difference = difference)
        }
    }
}

@Composable
private fun GoalDeltaRow(difference: GoalTargetDifference) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentIcon(difference.macro.icon, difference.macro.accentColor, 30)
        Text(
            text = difference.macro.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DeltaBadge(difference = difference)
    }
}

@Composable
private fun DeltaBadge(difference: GoalTargetDifference) {
    val delta = difference.delta
    val suffix = difference.macro.suffix
    val color = delta?.deltaColor() ?: AccentGoals
    val icon = when {
        delta == null -> Icons.Default.Check
        delta >= 0.0 -> Icons.Default.KeyboardArrowUp
        else -> Icons.Default.KeyboardArrowDown
    }
    val value = delta?.let { kotlin.math.abs(it) } ?: difference.recommended
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = value.formatTarget(suffix),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun TimelineBadge(delta: Double?) {
    val color = delta?.deltaColor() ?: AccentGoals
    val icon = when {
        delta == null -> Icons.Default.Check
        delta >= 0.0 -> Icons.Default.KeyboardArrowUp
        else -> Icons.Default.KeyboardArrowDown
    }
    val text = delta?.let { "${kotlin.math.abs(it).toFormattedString(true)} kcal" } ?: "Started"
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun TargetValueRow(macro: GoalMacro, value: Double?, suffix: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(macro.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value.formatTarget(suffix),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private val GoalMacro.label: String
    get() = when (this) {
        GoalMacro.Calories -> "Calories"
        GoalMacro.Protein -> "Protein"
        GoalMacro.Carbs -> "Carbs"
        GoalMacro.Fat -> "Fat"
        GoalMacro.Fiber -> "Fiber"
    }

private val GoalMacro.suffix: String
    get() = when (this) {
        GoalMacro.Calories -> "kcal"
        GoalMacro.Protein,
        GoalMacro.Carbs,
        GoalMacro.Fat,
        GoalMacro.Fiber -> "g"
    }

private val GoalMacro.icon: ImageVector
    get() = when (this) {
        GoalMacro.Calories -> Icons.Default.LocalFireDepartment
        GoalMacro.Protein -> Icons.Default.EggAlt
        GoalMacro.Carbs -> Icons.Default.BakeryDining
        GoalMacro.Fat -> Icons.Default.OilBarrel
        GoalMacro.Fiber -> Icons.Default.Grass
    }

private val GoalMacro.accentColor: Color
    get() = when (this) {
        GoalMacro.Calories -> AccentGoals
        GoalMacro.Protein -> AccentProtein
        GoalMacro.Carbs -> AccentCarbs
        GoalMacro.Fat -> AccentFat
        GoalMacro.Fiber -> AccentFiber
    }

private fun Double.deltaColor(): Color = if (this >= 0.0) DeltaUpColor else DeltaDownColor

@Composable
internal fun DailyMacroPlanCard(targets: MacroTargets) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentIcon(Icons.Default.MonitorHeart, AccentGoals, 42)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Daily macro plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Targets used by Add Meal and Meals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TargetTile(Icons.Default.LocalFireDepartment, AccentGoals, "Calories", targets.calories, "kcal", Modifier.weight(1f))
            TargetTile(Icons.Default.EggAlt, AccentProtein, "Protein", targets.protein, "g", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TargetTile(Icons.Default.BakeryDining, AccentCarbs, "Carbs", targets.carbs, "g", Modifier.weight(1f))
            TargetTile(Icons.Default.OilBarrel, AccentFat, "Fat", targets.fat, "g", Modifier.weight(1f))
            TargetTile(Icons.Default.Grass, AccentFiber, "Fiber", targets.fiber, "g", Modifier.weight(1f))
        }
    }
}

@Composable
private fun TargetTile(icon: ImageVector, color: Color, label: String, value: Double?, suffix: String, modifier: Modifier = Modifier) {
    MacroHintBox(label = label, modifier = modifier) {
        SurfacePanel(
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            contentPadding = 10,
            verticalSpacing = 10,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentIcon(icon, color, 32)
                Column {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        value.formatTarget(suffix),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BodyDetailsCard(profile: GoalProfile) {
    val resolvedLeanMassKg = profile.leanMassOrCalculatedKg()
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentIcon(Icons.Default.SelfImprovement, AccentProfile, 42)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Body inputs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Measurements used by goal recommendations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        MetricLine("Height", profile.heightCm, profile.heightCm.heightImperialLabel())
        MetricLine("Weight", profile.weightKg, profile.weightKg.weightImperialLabel())
        MetricLine("Body fat", profile.bodyFatPercent, profile.bodyFatPercent.value.formatTarget("%"))
        MetricLine(
            label = "Lean mass",
            measurement = profile.leanMassKg,
            displayValue = GoalMeasurement(resolvedLeanMassKg).weightImperialLabel(),
            statusLabel = if (profile.leanMassKg.value == null && resolvedLeanMassKg != null) "Estimated" else null,
        )
    }
}

@Composable
internal fun ProfileSnapshotCard(uiState: AppUiState) {
    val profile = uiState.goals.profile
    val resolvedLeanMassKg = profile.leanMassOrCalculatedKg()
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 16,
        verticalSpacing = 12,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.Default.SelfImprovement, AccentProfile, 42)
            Text("Body profile", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                profile.sex?.label?.let { ProfileSourceChip(it) }
                profile.ageOn(LocalDate.now())?.let { ProfileSourceChip("${it}y") }
                ProfileSourceChip(profile.activityLevel.label)
            }
        }
        SurfacePanel(
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            contentPadding = 12,
            verticalSpacing = 8,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentIcon(Icons.Default.Scale, AccentProfile, 34)
                Text(
                    profile.weightKg.weightImperialLabel(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            BodyMetricChip(
                icon = Icons.Default.OilBarrel,
                color = AccentFat,
                label = "Body fat",
                value = profile.bodyFatPercent.value.formatTarget("%"),
                modifier = Modifier.weight(1f),
            )
            BodyMetricChip(
                icon = Icons.Default.EggAlt,
                color = AccentProtein,
                label = "Lean mass",
                value = GoalMeasurement(resolvedLeanMassKg).weightImperialLabel(),
                statusLabel = null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BodyMetricChip(
    icon: ImageVector,
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    statusLabel: String? = null,
) {
    MacroHintBox(label = label, modifier = modifier) {
        SurfacePanel(
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            contentPadding = 10,
            verticalSpacing = 6,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentIcon(icon, color, 30)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricLine(
    label: String,
    measurement: GoalMeasurement,
    displayValue: String,
    statusLabel: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (measurement.locked) ProfileStatusIconChip("Locked")
            statusLabel?.let { ProfileStatusIconChip(it) }
        }
    }
}

@Composable
private fun ProfileStatusIconChip(label: String) {
    val icon = when (label) {
        "Locked" -> Icons.Default.Lock
        "Estimated" -> Icons.Default.MonitorHeart
        else -> Icons.Default.TrackChanges
    }
    MacroHintBox(label = label) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(AccentGoals.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AccentGoals,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun ProfileSourceChip(label: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun RecentTrendCard(
    bodyCards: List<GoalTrendCard>,
    adherenceCards: List<GoalTrendCard>,
    adherenceDate: LocalDate,
) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentIcon(Icons.Default.TrackChanges, AccentProfile, 42)
            Text("Recent trend", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        val primaryTrend = bodyCards.firstOrNull { it.label == "Weight" } ?: bodyCards.firstOrNull()
        if (primaryTrend == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("More scans will build your trend.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            SurfacePanel(
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                borderColor = AccentProfile.copy(alpha = 0.20f),
                contentPadding = 12,
                verticalSpacing = 6,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccentIcon(Icons.AutoMirrored.Filled.TrendingDown, AccentProfile, 38)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(primaryTrend.detail, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(primaryTrend.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            val supporting = bodyCards.filterNot { it == primaryTrend }.take(2)
            if (supporting.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    supporting.forEach { card ->
                        TrendMetricChip(card = card, modifier = Modifier.weight(1f))
                    }
                    if (supporting.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TrendMetricChip(card: GoalTrendCard, modifier: Modifier = Modifier) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        contentPadding = 10,
        verticalSpacing = 3,
        modifier = modifier,
    ) {
        Text(card.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(card.value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun AccentIcon(icon: ImageVector, color: Color, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size((size * 0.52f).dp))
    }
}

internal fun Double?.formatTarget(suffix: String): String {
    return if (this == null) "Unset" else "${this.toFormattedString(true)} $suffix"
}

private fun Double?.isOver(value: Double): Boolean = this != null && this > 0.0 && value > this

internal val AccentGoals = Color(0xFFFFD166)
internal val AccentProfile = Color(0xFF90CAF9)
private val AccentProtein = Color(0xFFFF6E7F)
private val AccentCarbs = Color(0xFFFFB74D)
private val AccentFat = Color(0xFF64B5F6)
private val AccentFiber = Color(0xFF81C784)
private val DeltaUpColor = Color(0xFF81C784)
private val DeltaDownColor = Color(0xFFFF6E7F)
