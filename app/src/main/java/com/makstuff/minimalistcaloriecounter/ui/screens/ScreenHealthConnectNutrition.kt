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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
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
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionMealGroup
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionStatItem
import com.makstuff.minimalistcaloriecounter.ui.model.healthGroupDetailItems
import com.makstuff.minimalistcaloriecounter.ui.model.healthMealDetailItems
import com.makstuff.minimalistcaloriecounter.ui.model.macroProgressArc
import com.makstuff.minimalistcaloriecounter.ui.model.macroSummaryItems
import com.makstuff.minimalistcaloriecounter.ui.model.mealGroupSummaryText
import com.makstuff.minimalistcaloriecounter.ui.model.mealGroups
import com.makstuff.minimalistcaloriecounter.ui.model.mealsDaySummaryText
import com.makstuff.minimalistcaloriecounter.ui.model.nutritionDaySummary
import com.makstuff.minimalistcaloriecounter.ui.model.supportsMacroHint
import com.makstuff.minimalistcaloriecounter.ui.reused.MacroHintBox
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
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
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val selectedDate = uiState.healthConnectViewerDate
    val meals = uiState.healthConnectViewerMeals
    var selectedFood by remember { mutableStateOf<HealthConnectNutritionMeal?>(null) }
    var selectedMealGroup by remember { mutableStateOf<NutritionMealGroup?>(null) }
    var datePickerVisible by remember { mutableStateOf(false) }
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
        FoodDetailDialog(
            meal = food,
            onDismiss = { selectedFood = null },
            onDelete = {
                selectedFood = null
                onDeleteMeal(food.recordId)
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
            onFoodClick = {
                selectedMealGroup = null
                selectedFood = it
            },
        )
    }

    if (datePickerVisible) {
        DatePickerSheet(
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
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                MealsDateHeader(
                    selectedDate = selectedDate,
                    onPrevious = { onDateChange(selectedDate.minusDays(1)) },
                    onNext = { onDateChange(selectedDate.plusDays(1)) },
                    onDateClick = { datePickerVisible = true },
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
                    item {
                        MealCard(
                            group = group,
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
private fun MealsDateHeader(
    selectedDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit,
) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        contentPadding = 10,
        verticalSpacing = 8,
        tonalElevation = 2,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.testTag("meals_previous_day"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous day",
                    tint = Color(0xFF90CAF9),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDateClick)
                    .testTag("meals_date_picker")
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("meals_next_day"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next day",
                    tint = Color(0xFF90CAF9),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val zoneId = ZoneId.systemDefault()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli(),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DatePicker(state = datePickerState)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            onDateSelected(Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate())
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Set date")
                }
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
) {
    val summary = nutritionDaySummary(meals, targets)
    val totals = summary.totals

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
                Column {
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
            TextButton(
                onClick = onCopySummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("meals_day_copy_summary"),
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(if (copied) "Day copied" else "Copy day summary")
            }
        }
    }
}

@Composable
private fun GoalProgressRow(progress: MacroTargets) {
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

@Composable
private fun GoalArcTile(
    icon: ImageVector,
    value: Double?,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    val arc = macroProgressArc(value)
    val progressColor = if (arc.isOverTarget) GoalOverage else color
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

@Composable
private fun MealCard(
    group: NutritionMealGroup,
    onMealClick: () -> Unit,
    onFoodClick: (HealthConnectNutritionMeal) -> Unit,
) {
    val calories = group.foods.sumOf { it.energy }
    val protein = group.foods.sumOf { it.protein }
    val carbs = group.foods.sumOf { it.totalCarbohydrate }
    val fat = group.foods.sumOf { it.totalFat }
    val fiber = group.foods.sumOf { it.dietaryFiber }

    SurfacePanel(
        modifier = Modifier.clickable(onClick = onMealClick),
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 12,
        verticalSpacing = 8,
        tonalElevation = 2,
    ) {
        MealSummaryRow(group, calories, carbs, protein, fat, fiber)

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            group.foods.forEach { food ->
                CompactFoodRow(
                    meal = food,
                    onClick = { onFoodClick(food) },
                )
            }
        }
    }
}

@Composable
private fun MealSummaryRow(
    group: NutritionMealGroup,
    calories: Double,
    carbs: Double,
    protein: Double,
    fat: Double,
    fiber: Double,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 520.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MealTitle(
                        group = group,
                        modifier = Modifier.weight(1f),
                    )
                    MealCaloriesChip(calories = calories)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MacroSummaryChip("Carbs", Icons.Default.BakeryDining, carbs, MacroCarbs, Modifier.weight(1f), fillContainer = true)
                    MacroSummaryChip("Protein", Icons.Default.EggAlt, protein, MacroProtein, Modifier.weight(1f), fillContainer = true)
                    MacroSummaryChip("Fat", Icons.Default.OilBarrel, fat, MacroFat, Modifier.weight(1f), fillContainer = true)
                    MacroSummaryChip("Fiber", Icons.Default.Grass, fiber, MacroFiber, Modifier.weight(1f), fillContainer = true)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MealTitle(
                    group = group,
                    modifier = Modifier.weight(1f),
                )
                MacroSummaryChip("Carbs", Icons.Default.BakeryDining, carbs, MacroCarbs)
                MacroSummaryChip("Protein", Icons.Default.EggAlt, protein, MacroProtein)
                MacroSummaryChip("Fat", Icons.Default.OilBarrel, fat, MacroFat)
                MacroSummaryChip("Fiber", Icons.Default.Grass, fiber, MacroFiber)
                MealCaloriesChip(calories = calories)
            }
        }
    }
}

@Composable
private fun MealCaloriesChip(calories: Double) {
    MacroHintBox(label = "Calories") {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(AccentGold.copy(alpha = 0.18f))
                .border(BorderStroke(1.dp, AccentGold.copy(alpha = 0.24f)), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = "${calories.toFormattedString(true)} kcal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MealTitle(
    group: NutritionMealGroup,
    modifier: Modifier = Modifier,
) {
    val color = groupColor(group)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = group.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${group.foods.size} foods",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MacroSummaryChip(
    label: String,
    icon: ImageVector,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
    fillContainer: Boolean = false,
) {
    MacroHintBox(label = label, modifier = modifier) {
        val chipModifier = if (fillContainer) Modifier.fillMaxWidth() else Modifier
        Row(
            modifier = chipModifier
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.86f))
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                    RoundedCornerShape(7.dp),
                )
                .heightIn(min = 46.dp)
                .padding(horizontal = 8.dp, vertical = 7.dp)
                .testTag("meal_macro_${label.lowercase()}"),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = "${value.toFormattedString(true)}g",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactFoodRow(
    meal: HealthConnectNutritionMeal,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.78f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = meal.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${meal.energy.toFormattedString(true)} kcal",
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealDetailDialog(
    group: NutritionMealGroup,
    copied: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onFoodClick: (HealthConnectNutritionMeal) -> Unit,
) {
    val calories = group.foods.sumOf { it.energy }
    val protein = group.foods.sumOf { it.protein }
    val carbs = group.foods.sumOf { it.totalCarbohydrate }
    val fat = group.foods.sumOf { it.totalFat }
    val fiber = group.foods.sumOf { it.dietaryFiber }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MealTitle(
                    group = group,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${calories.toFormattedString(true)} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.testTag("meals_meal_copy_summary"),
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (copied) "Meal summary copied" else "Copy meal summary",
                        tint = if (copied) MacroFiber else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MacroSummaryChip("Carbs", Icons.Default.BakeryDining, carbs, MacroCarbs)
                    MacroSummaryChip("Protein", Icons.Default.EggAlt, protein, MacroProtein)
                    MacroSummaryChip("Fat", Icons.Default.OilBarrel, fat, MacroFat)
                    MacroSummaryChip("Fiber", Icons.Default.Grass, fiber, MacroFiber)
                }
                MacroGrid(
                    items = healthGroupDetailItems(group)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.foods.forEach { food ->
                        CompactFoodRow(
                            meal = food,
                            onClick = { onFoodClick(food) },
                        )
                    }
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodDetailDialog(
    meal: HealthConnectNutritionMeal,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = meal.startTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroGrid(
                    items = healthMealDetailItems(meal)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun MacroGrid(items: List<NutritionStatItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { item ->
                    StatPill(item.label, item.value, Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    if (supportsMacroHint(label)) {
        MacroHintBox(label = label, modifier = modifier) {
            StatPillContent(label = label, value = value, modifier = Modifier.fillMaxWidth())
        }
        return
    }

    StatPillContent(label = label, value = value, modifier = modifier)
}

@Composable
private fun StatPillContent(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.78f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatusCard(text: String) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = 12,
        verticalSpacing = 8,
        tonalElevation = 2,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp, end = 2.dp),
    )
}

private fun groupColor(group: NutritionMealGroup): Color = Color(group.colorArgb)

private val AccentGold = Color(0xFFFBBC04)
private val MacroCarbs = Color(0xFFFFB74D)
private val MacroProtein = Color(0xFFFF6E7F)
private val MacroFat = Color(0xFF64B5F6)
private val MacroFiber = Color(0xFF4DD0E1)
private val GoalOverage = Color(0xFFFF5252)
