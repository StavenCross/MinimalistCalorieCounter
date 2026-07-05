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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
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
import com.makstuff.minimalistcaloriecounter.classes.NutritionFoodEditDraft
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

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
    onPrepareAddMeal: (LocalDate) -> Unit = {},
    onRepeatMealGroup: (LocalDate, List<HealthConnectNutritionMeal>) -> Unit,
    onTextChange: (String) -> Unit = {},
    onRefreshDateTime: () -> Unit = {},
    onDateTimeChange: (LocalDateTime) -> Unit = {},
    onMealTypeChange: (QuickImportMealType) -> Unit = {},
    onParsedFoodChange: (Int, QuickImportFood) -> Unit = { _, _ -> },
    onParsedFoodGroupChange: (Int, QuickImportFood) -> Unit = onParsedFoodChange,
    onParsedFoodServingAdd: (Int) -> Unit = {},
    onParsedFoodServingRemove: (Int) -> Unit = {},
    onImport: () -> Unit = {},
    onClear: () -> Unit = {},
    onRetryOutbox: (String) -> Unit = {},
    onExportDaySummary: (LocalDate, String) -> Unit,
    onReviewHealthConnectPermissions: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val selectedDate = uiState.healthConnectViewerDate
    val meals = uiState.healthConnectViewerMeals
    val isSelectedDateLoading = uiState.healthConnectViewerLoading &&
        (uiState.healthConnectViewerLoadingDate == null || uiState.healthConnectViewerLoadingDate == selectedDate)
    val hasVisibleMealContent = meals.isNotEmpty() || uiState.healthConnectViewerMessage != null
    val hasSelectedDateContent = uiState.healthConnectViewerMealsDate == selectedDate && hasVisibleMealContent
    val hasStablePreviousContent = isSelectedDateLoading &&
        uiState.healthConnectViewerMealsDate != selectedDate &&
        hasVisibleMealContent
    val contentDate = if (hasStablePreviousContent) uiState.healthConnectViewerMealsDate else selectedDate
    val showInlineLoading = isSelectedDateLoading && !hasStablePreviousContent && !hasSelectedDateContent
    var selectedFood by remember { mutableStateOf<HealthConnectNutritionMeal?>(null) }
    var selectedMealGroup by remember { mutableStateOf<NutritionMealGroup?>(null) }
    var datePickerVisible by remember { mutableStateOf(false) }
    var addMealVisible by remember { mutableStateOf(false) }
    var expandedMealKeys by remember(selectedDate) { mutableStateOf(emptySet<String>()) }
    val clipboard = LocalClipboardManager.current
    var mealSummaryCopied by remember { mutableStateOf(false) }
    var foodSaveNoticeKey by remember { mutableStateOf<Long?>(null) }
    val targets = uiState.goals.activeTargetsFor(contentDate)
    fun openAddMeal() {
        onPrepareAddMeal(selectedDate)
        addMealVisible = true
    }

    LaunchedEffect(selectedMealGroup) {
        mealSummaryCopied = false
    }
    LaunchedEffect(foodSaveNoticeKey) {
        if (foodSaveNoticeKey != null) {
            delay(1_800)
            foodSaveNoticeKey = null
        }
    }

    selectedFood?.let { food ->
        val servingGroup = servingGroupFor(food, meals)
        FoodDetailDialog(
            servingGroup = servingGroup,
            saveNoticeVisible = foodSaveNoticeKey != null,
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
                foodSaveNoticeKey = System.currentTimeMillis()
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
                onRepeatMealGroup(selectedDate, group.foods)
                addMealVisible = true
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

    if (addMealVisible) {
        MealsAddMealDrawer(
            uiState = uiState,
            onDismiss = { addMealVisible = false },
            onTextChange = onTextChange,
            onRefreshDateTime = onRefreshDateTime,
            onDateTimeChange = onDateTimeChange,
            onMealTypeChange = onMealTypeChange,
            onParsedFoodChange = onParsedFoodChange,
            onParsedFoodGroupChange = onParsedFoodGroupChange,
            onParsedFoodServingAdd = onParsedFoodServingAdd,
            onParsedFoodServingRemove = onParsedFoodServingRemove,
            onImport = onImport,
            onClear = {
                val currentDrawerDateTime = uiState.inputQuickImportDateTime
                onClear()
                onDateTimeChange(currentDrawerDateTime)
            },
            onRetryOutbox = onRetryOutbox,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        MealDateSelector(
                            selectedDate = selectedDate,
                            onPrevious = { onDateChange(selectedDate.minusDays(1)) },
                            onNext = { onDateChange(selectedDate.plusDays(1)) },
                            onDateClick = { datePickerVisible = true },
                            testTagPrefix = "meals",
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                        )
                        AddMealAttachedAction(
                            onClick = ::openAddMeal,
                        )
                    }
                }

                item {
                    DaySummaryCard(
                        date = contentDate,
                        meals = meals,
                        targets = targets,
                        isLoading = showInlineLoading,
                        message = uiState.healthConnectViewerMessage,
                    )
                }

                val groups = if (showInlineLoading) emptyList() else mealGroups(meals)
                val viewerMessage = uiState.healthConnectViewerMessage
                val isEmptyNutritionMessage = viewerMessage?.isEmptyNutritionMessage() == true
                if (showInlineLoading) {
                    item {
                        StatusCard("Reading Health Connect")
                    }
                } else if (
                    viewerMessage != null &&
                    groups.isEmpty() &&
                    !uiState.healthConnectPermissionsGranted
                ) {
                    item {
                        PermissionEmptyStateCard(onReviewPermissions = onReviewHealthConnectPermissions)
                    }
                } else if (viewerMessage != null && groups.isEmpty()) {
                    if (isEmptyNutritionMessage) {
                        item {
                            SectionTitle("Meals")
                        }
                        item {
                            EmptyMealsCard()
                        }
                    } else {
                        item {
                            StatusCard(viewerMessage)
                        }
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
                } else if (!showInlineLoading && viewerMessage == null) {
                    item {
                        SectionTitle("Meals")
                    }
                    item {
                        EmptyMealsCard()
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (foodSaveNoticeKey != null) {
            ChangesSavedChip(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .testTag("meals_changes_saved"),
            )
        }
    }
}

@Composable
private fun ChangesSavedChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Changes saved",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
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
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (isLoading) "Loading" else "${summary.foodCount} foods",
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
            if (!isLoading) {
                MacroGrid(
                    items = macroSummaryItems(totals)
                )
                GoalProgressRow(summary.progress)
            }
        }
    }
}

private fun String.isEmptyNutritionMessage(): Boolean {
    return contains("No Health Connect nutrition records", ignoreCase = true)
}

@Composable
private fun EmptyMealsCard() {
    MealsInfoCard(
        icon = Icons.Default.Restaurant,
        body = "This day is empty so far. Add a meal when you're ready.",
        modifier = Modifier.testTag("meals_empty_state"),
    )
}

@Composable
private fun PermissionEmptyStateCard(
    onReviewPermissions: () -> Unit,
) {
    MealsInfoCard(
        icon = Icons.Default.Info,
        title = "Health Connect needs permission",
        body = "Allow nutrition access so Meals can read and update your log.",
        actionText = "Review permissions",
        onAction = onReviewPermissions,
        actionTestTag = "meals_review_permissions",
        modifier = Modifier.testTag("meals_permissions_state"),
    )
}

@Composable
private fun MealsInfoCard(
    icon: ImageVector,
    body: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    actionTestTag: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, FoodCaloriesBlue.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FoodCaloriesBlue.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = FoodCaloriesBlue,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (actionText != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier),
                ) {
                    actionIcon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                        )
                    }
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
private fun AddMealAttachedAction(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meals_add_meal")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Add meal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
