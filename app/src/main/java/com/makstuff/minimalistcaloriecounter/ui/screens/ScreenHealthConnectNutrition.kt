package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.NutritionFoodEditDraft
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionMealGroup
import com.makstuff.minimalistcaloriecounter.ui.model.macroProgressArc
import com.makstuff.minimalistcaloriecounter.ui.model.macroSummaryItems
import com.makstuff.minimalistcaloriecounter.ui.model.mealGroupKey
import com.makstuff.minimalistcaloriecounter.ui.model.mealGroupSummaryText
import com.makstuff.minimalistcaloriecounter.ui.model.mealGroups
import com.makstuff.minimalistcaloriecounter.ui.model.mealsDaySummaryText
import com.makstuff.minimalistcaloriecounter.ui.model.nutritionDaySummary
import com.makstuff.minimalistcaloriecounter.ui.model.servingGroupFor
import com.makstuff.minimalistcaloriecounter.ui.model.shouldCollapseMealGroup
import com.makstuff.minimalistcaloriecounter.ui.reused.MacroHintBox
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import com.makstuff.minimalistcaloriecounter.ui.reused.pullRefreshRubberBand
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHealthConnectNutrition(
    uiState: AppUiState,
    onDateChange: (LocalDate) -> Unit,
    onRefresh: () -> Unit,
    onDeleteMeal: (String) -> Unit,
    onDeleteMealGroup: (List<String>) -> Unit,
    onAddFoodServing: (HealthConnectNutritionMeal) -> Unit,
    onRemoveFoodServing: (HealthConnectNutritionMeal) -> Unit,
    onSaveFoodServingGroup: (List<HealthConnectNutritionMeal>, NutritionFoodEditDraft) -> Unit,
    onRepeatMealGroup: (List<HealthConnectNutritionMeal>) -> Unit,
    onExportDaySummary: (LocalDate, String) -> Unit,
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val selectedDate = uiState.healthConnectViewerDate
    val meals = uiState.healthConnectViewerMeals
    var selectedFood by remember { mutableStateOf<HealthConnectNutritionMeal?>(null) }
    var selectedMealGroup by remember { mutableStateOf<NutritionMealGroup?>(null) }
    var datePickerVisible by remember { mutableStateOf(false) }
    var expandedMealKeys by remember(selectedDate) { mutableStateOf(emptySet<String>()) }
    val clipboard = LocalClipboardManager.current
    var daySummaryCopied by remember { mutableStateOf(false) }
    var mealSummaryCopied by remember { mutableStateOf(false) }
    val targets = uiState.goals.activeTargetsFor(selectedDate)
    val daySummaryText = mealsDaySummaryText(selectedDate, meals, targets)

    LaunchedEffect(daySummaryText) {
        daySummaryCopied = false
    }
    LaunchedEffect(selectedMealGroup) {
        mealSummaryCopied = false
    }

    selectedFood?.let { food ->
        val servingGroup = servingGroupFor(food, meals)
        FoodDetailDialog(
            servingGroup = servingGroup,
            onDismiss = { selectedFood = null },
            onDelete = {
                selectedFood = null
                onDeleteMealGroup(servingGroup.foods.map { it.recordId })
            },
            onAddServing = {
                onAddFoodServing(food)
            },
            onRemoveServing = {
                onRemoveFoodServing(servingGroup.foods.last())
            },
            onSaveEdit = { draft ->
                onSaveFoodServingGroup(servingGroup.foods, draft)
                selectedFood = null
            },
        )
    }

    selectedMealGroup?.let { group ->
        MealDetailDialog(
            group = group,
            copied = mealSummaryCopied,
            onDismiss = { selectedMealGroup = null },
            onCopy = {
                clipboard.setText(AnnotatedString(mealGroupSummaryText(group)))
                mealSummaryCopied = true
            },
            onDelete = {
                selectedMealGroup = null
                onDeleteMealGroup(group.foods.map { it.recordId })
            },
            onRepeat = {
                selectedMealGroup = null
                onRepeatMealGroup(group.foods)
            },
            onFoodClick = {
                selectedMealGroup = null
                selectedFood = it
            },
        )
    }

    if (datePickerVisible) {
        MealDatePickerSheet(
            selectedDate = selectedDate,
            onDismiss = { datePickerVisible = false },
            onDateSelected = {
                datePickerVisible = false
                onDateChange(it)
            },
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.healthConnectViewerLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pullRefreshRubberBand(uiState.healthConnectViewerLoading)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                MealDateSelector(
                    selectedDate = selectedDate,
                    onPrevious = { onDateChange(selectedDate.minusDays(1)) },
                    onNext = { onDateChange(selectedDate.plusDays(1)) },
                    onDateClick = { datePickerVisible = true },
                    testTagPrefix = "meals",
                )
            }

            item {
                DaySummaryCard(
                    date = selectedDate,
                    meals = meals,
                    targets = targets,
                    isLoading = uiState.healthConnectViewerLoading,
                    message = uiState.healthConnectViewerMessage,
                    copied = daySummaryCopied,
                    onCopySummary = {
                        clipboard.setText(AnnotatedString(daySummaryText))
                        daySummaryCopied = true
                    },
                    onExportSummary = { onExportDaySummary(selectedDate, daySummaryText) },
                )
            }

            if (uiState.healthConnectViewerLoading) {
                item {
                    StatusCard("Reading Health Connect")
                }
            } else if (uiState.healthConnectViewerMessage != null && meals.isEmpty()) {
                item {
                    StatusCard(uiState.healthConnectViewerMessage)
                }
            }

            val groups = mealGroups(meals)
            if (!uiState.healthConnectViewerLoading && groups.isEmpty() && uiState.healthConnectViewerMessage == null) {
                item {
                    StatusCard("No foods logged for this day.")
                }
            }

            if (groups.isNotEmpty()) {
                item {
                    SectionTitle("Meals")
                }
                groups.forEach { group ->
                    val groupKey = mealGroupKey(group)
                    val canExpand = shouldCollapseMealGroup(group)
                    val expanded = !canExpand || groupKey in expandedMealKeys
                    item {
                        MealCard(
                            group = group,
                            expanded = expanded,
                            canToggleExpand = canExpand,
                            onToggleExpanded = {
                                expandedMealKeys = if (expanded) {
                                    expandedMealKeys - groupKey
                                } else {
                                    expandedMealKeys + groupKey
                                }
                            },
                            onMealClick = { selectedMealGroup = group },
                            onFoodClick = { selectedFood = it },
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DaySummaryCard(
    date: LocalDate,
    meals: List<HealthConnectNutritionMeal>,
    targets: MacroTargets,
    isLoading: Boolean,
    message: String?,
    copied: Boolean,
    onCopySummary: () -> Unit,
    onExportSummary: () -> Unit,
) {
    val summary = nutritionDaySummary(meals, targets)
    val totals = summary.totals
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                RoundedCornerShape(14.dp),
            )
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (isLoading) "Loading" else "${totals.energy.toFormattedString(true)} kcal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AccentGold.copy(alpha = 0.16f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "${summary.foodCount} foods",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("meals_day_actions"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Day actions",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (copied) "Day copied" else "Copy") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onCopySummary()
                                },
                                modifier = Modifier.testTag("meals_day_copy_summary"),
                            )
                            DropdownMenuItem(
                                text = { Text("Export") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onExportSummary()
                                },
                                modifier = Modifier.testTag("meals_day_export_summary"),
                            )
                        }
                    }
                }
            }
            if (message != null && meals.isNotEmpty()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            MacroGrid(
                items = macroSummaryItems(totals)
            )
            GoalProgressRow(summary.progress)
        }
    }
}

@Composable
private fun GoalProgressRow(progress: MacroTargets) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 420.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoalArcTile(Icons.Default.LocalFireDepartment, progress.calories, AccentGold, "Calories", Modifier.weight(1f))
                    GoalArcTile(Icons.Default.EggAlt, progress.protein, MacroProtein, "Protein", Modifier.weight(1f))
                    GoalArcTile(Icons.Default.BakeryDining, progress.carbs, MacroCarbs, "Carbs", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoalArcTile(Icons.Default.OilBarrel, progress.fat, MacroFat, "Fat", Modifier.weight(1f))
                    GoalArcTile(Icons.Default.Grass, progress.fiber, MacroFiber, "Fiber", Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoalArcTile(Icons.Default.LocalFireDepartment, progress.calories, AccentGold, "Calories", Modifier.weight(1f))
                GoalArcTile(Icons.Default.EggAlt, progress.protein, MacroProtein, "Protein", Modifier.weight(1f))
                GoalArcTile(Icons.Default.BakeryDining, progress.carbs, MacroCarbs, "Carbs", Modifier.weight(1f))
                GoalArcTile(Icons.Default.OilBarrel, progress.fat, MacroFat, "Fat", Modifier.weight(1f))
                GoalArcTile(Icons.Default.Grass, progress.fiber, MacroFiber, "Fiber", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GoalArcTile(
    icon: ImageVector,
    value: Double?,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    val arc = macroProgressArc(value)
    val progressColor = if (arc.isOverTarget) HealthGoalOverage else color
    MacroHintBox(label = label, modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val arcCanvasHeight = (maxWidth * 0.52f).coerceIn(30.dp, 62.dp)
            val iconSize = (arcCanvasHeight * 0.42f).coerceIn(18.dp, 26.dp)
            val tileMinHeight = (arcCanvasHeight + 18.dp).coerceAtLeast(48.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
                        RoundedCornerShape(8.dp),
                    )
                    .heightIn(min = tileMinHeight)
                    .padding(horizontal = 5.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(arcCanvasHeight),
                ) {
                    val strokeWidth = 4.dp.toPx()
                    val horizontalInset = 4.dp.toPx()
                    val arcSize = Size(
                        width = size.width - horizontalInset * 2,
                        height = (size.height - strokeWidth) * 1.8f,
                    )
                    val top = strokeWidth / 2
                    drawArc(
                        color = Color.White.copy(alpha = 0.16f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(horizontalInset, top),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    if (arc.progress > 0f) {
                        drawArc(
                            color = progressColor,
                            startAngle = 180f,
                            sweepAngle = 180f * arc.progress,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(horizontalInset, top),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }
                Icon(
                    icon,
                    contentDescription = "$label goal progress",
                    tint = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else color,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(iconSize),
                )
            }
        }
    }
}
