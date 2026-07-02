package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.MealType
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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
    var selectedMealGroup by remember { mutableStateOf<MealGroup?>(null) }

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
            onDismiss = { selectedMealGroup = null },
            onFoodClick = {
                selectedMealGroup = null
                selectedFood = it
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MonthCalendarCard(
                selectedDate = selectedDate,
                onDateChange = onDateChange,
                onRefresh = onRefresh,
            )
        }

        item {
            DaySummaryCard(
                date = selectedDate,
                meals = meals,
                isLoading = uiState.healthConnectViewerLoading,
                message = uiState.healthConnectViewerMessage,
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

        groups.forEach { group ->
            item {
                MealCard(
                    group = group,
                    onMealClick = { selectedMealGroup = group },
                    onFoodClick = { selectedFood = it },
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MonthCalendarCard(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onRefresh: () -> Unit,
) {
    val yearMonth = YearMonth.from(selectedDate)
    val monthStart = yearMonth.atDay(1)
    val firstDayOffset = dayOffset(monthStart.dayOfWeek)
    val daysInMonth = yearMonth.lengthOfMonth()
    val cells = (0 until 42).map { index ->
        val dayNumber = index - firstDayOffset + 1
        if (dayNumber in 1..daysInMonth) yearMonth.atDay(dayNumber) else null
    }

    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 10,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Pick a day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh meals",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        CalendarWeekLabels()

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    week.forEach { date ->
                        CalendarDayCell(
                            date = date,
                            selectedDate = selectedDate,
                            onDateChange = onDateChange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarWeekLabels() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val selected = date == selectedDate
    val isToday = date == today
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    isToday -> AccentGold.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                BorderStroke(
                    1.dp,
                    when {
                        selected -> MaterialTheme.colorScheme.primary
                        isToday -> AccentGold.copy(alpha = 0.65f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    },
                ),
                shape,
            )
            .clickable(enabled = date != null) {
                date?.let(onDateChange)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date?.dayOfMonth?.toString().orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun DaySummaryCard(
    date: LocalDate,
    meals: List<HealthConnectNutritionMeal>,
    isLoading: Boolean,
    message: String?,
) {
    val calories = meals.sumOf { it.energy }
    val protein = meals.sumOf { it.protein }
    val carbs = meals.sumOf { it.totalCarbohydrate }
    val fat = meals.sumOf { it.totalFat }
    val fiber = meals.sumOf { it.dietaryFiber }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
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
                        text = if (isLoading) "Loading" else "${calories.toFormattedString(true)} kcal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = AccentGold,
                    )
                    Text(
                        text = "${meals.size} foods",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                items = listOf(
                    "Carbs" to "${carbs.toFormattedString(true)}g",
                    "Protein" to "${protein.toFormattedString(true)}g",
                    "Fat" to "${fat.toFormattedString(true)}g",
                    "Fiber" to "${fiber.toFormattedString(true)}g",
                )
            )
        }
    }
}

@Composable
private fun MealCard(
    group: MealGroup,
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
        borderColor = group.color.copy(alpha = 0.34f),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 10,
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
            MacroSummaryChip("C", carbs)
            MacroSummaryChip("P", protein)
            MacroSummaryChip("F", fat)
            MacroSummaryChip("Fi", fiber)
            Text(
                text = "${calories.toFormattedString(true)} kcal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            group.foods.forEach { food ->
                CompactFoodRow(
                    meal = food,
                    accentColor = group.color,
                    onClick = { onFoodClick(food) },
                )
            }
        }
    }
}

@Composable
private fun MealTitle(
    group: MealGroup,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(group.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = group.color,
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
    value: Double,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                RoundedCornerShape(7.dp),
            )
            .heightIn(min = 46.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value.toFormattedString(true),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactFoodRow(
    meal: HealthConnectNutritionMeal,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
            .border(
                BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
                RoundedCornerShape(8.dp),
            )
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

@Composable
private fun MealDetailDialog(
    group: MealGroup,
    onDismiss: () -> Unit,
    onFoodClick: (HealthConnectNutritionMeal) -> Unit,
) {
    val calories = group.foods.sumOf { it.energy }
    val protein = group.foods.sumOf { it.protein }
    val carbs = group.foods.sumOf { it.totalCarbohydrate }
    val fat = group.foods.sumOf { it.totalFat }
    val fiber = group.foods.sumOf { it.dietaryFiber }
    val sugar = group.foods.sumOf { it.sugar }
    val saturatedFat = group.foods.sumOf { it.saturatedFat }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MacroSummaryChip("C", carbs)
                    MacroSummaryChip("P", protein)
                    MacroSummaryChip("F", fat)
                    MacroSummaryChip("Fi", fiber)
                }
                MacroGrid(
                    items = listOf(
                        "Calories" to "${calories.toFormattedString(true)} kcal",
                        "Carbs" to "${carbs.toFormattedString(true)}g",
                        "Protein" to "${protein.toFormattedString(true)}g",
                        "Fat" to "${fat.toFormattedString(true)}g",
                        "Fiber" to "${fiber.toFormattedString(true)}g",
                        "Sugar" to "${sugar.toFormattedString(true)}g",
                        "Sat fat" to "${saturatedFat.toFormattedString(true)}g",
                    )
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.foods.forEach { food ->
                        CompactFoodRow(
                            meal = food,
                            accentColor = group.color,
                            onClick = { onFoodClick(food) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun FoodDetailDialog(
    meal: HealthConnectNutritionMeal,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroGrid(
                    items = listOf(
                        "Calories" to "${meal.energy.toFormattedString(true)} kcal",
                        "Carbs" to "${meal.totalCarbohydrate.toFormattedString(true)}g",
                        "Protein" to "${meal.protein.toFormattedString(true)}g",
                        "Fat" to "${meal.totalFat.toFormattedString(true)}g",
                        "Fiber" to "${meal.dietaryFiber.toFormattedString(true)}g",
                        "Sugar" to "${meal.sugar.toFormattedString(true)}g",
                        "Sat fat" to "${meal.saturatedFat.toFormattedString(true)}g",
                        "Fat kcal" to meal.energyFromFat?.let { "${it.toFormattedString(true)} kcal" }.orEmpty(),
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
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
        },
    )
}

@Composable
private fun MacroGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { item ->
                    StatPill(item.first, item.second, Modifier.weight(1f))
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
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
    SurfacePanel(contentPadding = 12) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SurfacePanel(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: Int = 8,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

private data class MealGroup(
    val mealType: Int,
    val label: String,
    val color: Color,
    val foods: List<HealthConnectNutritionMeal>,
)

private fun mealGroups(meals: List<HealthConnectNutritionMeal>): List<MealGroup> {
    return listOf(
        groupFor(MealType.MEAL_TYPE_BREAKFAST, "Breakfast", Color(0xFF4285F4), meals),
        groupFor(MealType.MEAL_TYPE_LUNCH, "Lunch", AccentGreen, meals),
        groupFor(MealType.MEAL_TYPE_DINNER, "Dinner", Color(0xFFE8710A), meals),
        groupFor(MealType.MEAL_TYPE_SNACK, "Snack", Color(0xFF9C27B0), meals),
        groupFor(MealType.MEAL_TYPE_UNKNOWN, "Other", Color(0xFF607D8B), meals),
    ).filter { it.foods.isNotEmpty() }
}

private fun groupFor(
    mealType: Int,
    label: String,
    color: Color,
    meals: List<HealthConnectNutritionMeal>,
): MealGroup {
    return MealGroup(
        mealType = mealType,
        label = label,
        color = color,
        foods = meals.filter { it.mealType == mealType }.sortedBy { it.startTime },
    )
}

private fun dayOffset(dayOfWeek: DayOfWeek): Int {
    return dayOfWeek.value % 7
}

private val AccentGreen = Color(0xFF34A853)
private val AccentGold = Color(0xFFFBBC04)
