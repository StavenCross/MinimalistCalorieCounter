package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalRecalculationStatus
import com.makstuff.minimalistcaloriecounter.classes.GoalValueSource
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.ui.model.GoalTrendCard
import com.makstuff.minimalistcaloriecounter.ui.reused.MacroHintBox
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import java.time.LocalDate

@Composable
internal fun GoalHeroCard(
    targets: MacroTargets,
    profileComplete: Boolean,
    message: String?,
    onRecalculate: () -> Unit,
) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.Default.MonitorHeart, AccentGoals, 48)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Daily guidance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (targets.calories != null) {
                        "${targets.calories.toFormattedString(true)} kcal target"
                    } else {
                        "Set profile values to calculate a target"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRecalculate, modifier = Modifier.testTag("goals_recalculate_hero")) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Recalculate goals",
                    tint = AccentGoals,
                )
            }
        }
        if (!profileComplete || message != null) {
            Text(
                text = message ?: "Birthday, sex, height, weight, and lean mass or body fat are required.",
                style = MaterialTheme.typography.bodySmall,
                color = if (profileComplete) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun RecommendationCard(
    currentTargets: MacroTargets,
    recommendedTargets: MacroTargets,
    bmr: Double,
    tdee: Double,
    warning: String?,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                Text("New recommendation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "BMR ${bmr.toFormattedString(true)} • TDEE ${tdee.toFormattedString(true)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        MacroCompareRow("Calories", currentTargets.calories, recommendedTargets.calories, "kcal")
        MacroCompareRow("Protein", currentTargets.protein, recommendedTargets.protein, "g")
        MacroCompareRow("Carbs", currentTargets.carbs, recommendedTargets.carbs, "g")
        MacroCompareRow("Fat", currentTargets.fat, recommendedTargets.fat, "g")
        MacroCompareRow("Fiber", currentTargets.fiber, recommendedTargets.fiber, "g")
        warning?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dismiss")
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
internal fun RecalculationCard(
    status: GoalRecalculationStatus,
    onRecalculate: () -> Unit,
) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = if (status.dueToday) AccentGoals.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 14,
        verticalSpacing = 10,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.Default.Event, AccentGoals, 42)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Sunday recalculation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (status.dueToday) "Due today" else "Next check ${status.nextSunday}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.dueToday) AccentGoals else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRecalculate, modifier = Modifier.testTag("goals_recalculate_schedule")) {
                Icon(Icons.Default.Calculate, contentDescription = "Recalculate goals", tint = AccentGoals)
            }
        }
        Text(
            text = "Last recommendation: ${status.lastRecommendationDate?.toString() ?: "none yet"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun GoalHistoryCard(entries: List<GoalHistoryEntry>) {
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
                Text("Recommendation history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${entries.size} target decisions saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        entries.sortedByDescending { it.effectiveDate }.take(3).forEach { entry ->
            GoalHistoryRow(entry)
        }
    }
}

@Composable
private fun GoalHistoryRow(entry: GoalHistoryEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(entry.effectiveDate.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(entry.targets.calories.formatTarget("kcal"), style = MaterialTheme.typography.labelLarge)
        }
        Text(
            text = buildString {
                append(entry.source.replaceFirstChar { it.uppercase() })
                entry.bmr?.let { append(" • BMR ${it.toFormattedString(true)}") }
                entry.tdee?.let { append(" • TDEE ${it.toFormattedString(true)}") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = buildString {
                entry.weightKg?.let { append("${it.toFormattedString(true)} kg") }
                entry.leanMassKg?.let {
                    if (isNotEmpty()) append(" • ")
                    append("${it.toFormattedString(true)} kg lean")
                }
                entry.weightLossTarget?.let {
                    if (isNotEmpty()) append(" • ")
                    append(it.label)
                }
            }.ifBlank { "Source measurements unavailable" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MacroCompareRow(label: String, current: Double?, recommended: Double?, suffix: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${current.formatTarget(suffix)} -> ${recommended.formatTarget(suffix)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun MacroTargetGrid(targets: MacroTargets) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
internal fun ProfileSnapshotCard(uiState: AppUiState) {
    val profile = uiState.goals.profile
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
            AccentIcon(Icons.Default.SelfImprovement, AccentProfile, 42)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Profile inputs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = listOfNotNull(
                        profile.sex?.label,
                        profile.ageOn(LocalDate.now())?.let { "$it years" },
                        profile.activityLevel.label,
                    ).joinToString(" • ").ifBlank { "Use settings to enter profile data" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        MetricLine("Height", profile.heightCm, "cm")
        MetricLine("Weight", profile.weightKg, "kg")
        MetricLine("Body fat", profile.bodyFatPercent, "%")
        MetricLine("Lean mass", profile.leanMassKg, "kg")
    }
}

@Composable
private fun MetricLine(label: String, measurement: GoalMeasurement, suffix: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = buildString {
                append(measurement.value.formatTarget(suffix))
                if (measurement.locked) append(" locked")
                if (measurement.source == GoalValueSource.HealthConnect) append(" HC")
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun GoalTrendCards(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.Default.MonitorHeart, AccentProfile, 42)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Trends and adherence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Adherence from loaded meals on $adherenceDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (bodyCards.isEmpty()) {
            Text("Body trends need at least one saved measurement.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            bodyCards.forEach { GoalTrendRow(it) }
        }
        adherenceCards.take(3).forEach { GoalTrendRow(it) }
    }
}

@Composable
private fun GoalTrendRow(card: GoalTrendCard) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(card.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(card.value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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

internal val AccentGoals = Color(0xFFFFD166)
internal val AccentProfile = Color(0xFF90CAF9)
private val AccentProtein = Color(0xFFFF6E7F)
private val AccentCarbs = Color(0xFFFFB74D)
private val AccentFat = Color(0xFF64B5F6)
private val AccentFiber = Color(0xFF81C784)
