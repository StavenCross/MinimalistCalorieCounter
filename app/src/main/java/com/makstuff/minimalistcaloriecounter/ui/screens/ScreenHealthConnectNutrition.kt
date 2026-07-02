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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                MealGroupHeader(group)
            }
            items(group.foods) { meal ->
                FoodRecordCard(
                    meal = meal,
                    onDelete = { onDeleteMeal(meal.recordId) },
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
private fun MealGroupHeader(group: MealGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(group.color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = group.color,
                    modifier = Modifier.size(17.dp),
                )
            }
            Column {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${group.foods.size} foods",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "${group.foods.sumOf { it.energy }.toFormattedString(true)} kcal",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoodRecordCard(
    meal: HealthConnectNutritionMeal,
    onDelete: () -> Unit,
) {
    SurfacePanel(contentPadding = 10) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meal.startTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete food record",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatPill("kcal", meal.energy.toFormattedString(true), Modifier.weight(1f))
            StatPill("carbs", "${meal.totalCarbohydrate.toFormattedString(true)}g", Modifier.weight(1f))
            StatPill("protein", "${meal.protein.toFormattedString(true)}g", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatPill("fat", "${meal.totalFat.toFormattedString(true)}g", Modifier.weight(1f))
            StatPill("fiber", "${meal.dietaryFiber.toFormattedString(true)}g", Modifier.weight(1f))
            StatPill("sugar", "${meal.sugar.toFormattedString(true)}g", Modifier.weight(1f))
        }
    }
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
