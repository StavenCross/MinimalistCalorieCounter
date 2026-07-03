package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.makstuff.minimalistcaloriecounter.classes.GoalCalculator
import com.makstuff.minimalistcaloriecounter.classes.MealTargetAllocation
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val AccentCapture = Color(0xFFFF8A65)
private val AccentMeal = Color(0xFFB39DDB)
private val AccentFood = Color(0xFF64B5F6)
private val AccentSend = Color(0xFF4FC3F7)
private val AccentClear = Color(0xFFFF6E7F)
private val AccentEdit = Color(0xFFFFD166)
private val AccentNow = Color(0xFF7BDFF2)
private val AccentDatabase = Color(0xFFCE93D8)
private val AccentDay = Color(0xFFFFB74D)
private val AccentHealth = Color(0xFF4DD0E1)

@Composable
fun ScreenQuickImport(
    uiState: AppUiState,
    onTextChange: (String) -> Unit,
    onToggleAddDatabase: () -> Unit,
    onToggleAddDay: () -> Unit,
    onToggleHealthConnect: () -> Unit,
    onRefreshDateTime: () -> Unit,
    onDateTimeChange: (LocalDateTime) -> Unit,
    onMealTypeChange: (QuickImportMealType) -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
) {
    val meal = uiState.quickImportMeal
    val hasDestination = uiState.quickImportAddFoodsToDatabase ||
        uiState.quickImportAddFoodsToDay ||
        uiState.quickImportWriteHealthConnect
    val canImport = meal != null && hasDestination && !uiState.quickImportInProgress
    var selectedPreviewMeal by remember { mutableStateOf<QuickImportMeal?>(null) }
    var selectedPreviewFood by remember { mutableStateOf<QuickImportFood?>(null) }
    val targetAllocation = GoalCalculator.mealAllocation(
        mealType = uiState.quickImportMealType,
        targets = uiState.goals.activeTargetsFor(uiState.inputQuickImportDateTime.toLocalDate()),
        consumedMeals = consumedMealsForQuickImportDate(uiState),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        selectedPreviewMeal?.let { previewMeal ->
            QuickImportMealDetailSheet(
                meal = previewMeal,
                mealType = uiState.quickImportMealType,
                targetAllocation = targetAllocation,
                onDismiss = { selectedPreviewMeal = null },
                onFoodClick = { food ->
                    selectedPreviewMeal = null
                    selectedPreviewFood = food
                },
            )
        }
        selectedPreviewFood?.let { previewFood ->
            QuickImportFoodDetailSheet(
                food = previewFood,
                onDismiss = { selectedPreviewFood = null },
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                CapturePanel(
                    value = uiState.inputQuickImportText,
                    onValueChange = onTextChange,
                    onClear = onClear,
                )
            }

            item {
                MealTimePanel(
                    dateTimeText = uiState.inputQuickImportDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    selectedDateTime = uiState.inputQuickImportDateTime,
                    mealType = uiState.quickImportMealType,
                    onDateTimeChange = onDateTimeChange,
                    onRefreshDateTime = onRefreshDateTime,
                    onMealTypeChange = onMealTypeChange,
                )
            }

            uiState.quickImportError?.let { error ->
                item {
                    SurfacePanel(
                        borderColor = MaterialTheme.colorScheme.error,
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            meal?.let {
                item {
                    ParsedMealPreviewCard(
                        meal = it,
                        mealType = uiState.quickImportMealType,
                        targetAllocation = targetAllocation,
                        onMealClick = { selectedPreviewMeal = it },
                        onFoodClick = { food -> selectedPreviewFood = food },
                        modifier = Modifier.testTag("quick_import_preview_totals"),
                    )
                }
            }

            uiState.quickImportResult?.let { result ->
                item {
                    val resultColor = when (result.healthWriteResult) {
                        is QuickImportHealthWriteResult.Failed,
                        QuickImportHealthWriteResult.HealthConnectUnavailable,
                        QuickImportHealthWriteResult.PermissionsMissing -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                    SurfacePanel(borderColor = resultColor.copy(alpha = 0.5f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = resultColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = quickImportResultText(result.databaseEntriesAdded, result.dayFoodsAdded, result.healthWriteResult),
                                style = MaterialTheme.typography.bodyMedium,
                                color = resultColor,
                            )
                        }
                    }
                }
            }
        }

        Surface(
            onClick = { if (canImport) onImport() },
            enabled = canImport,
            shape = RoundedCornerShape(18.dp),
            color = if (canImport) {
                AccentSend.copy(alpha = 0.28f)
            } else {
                AccentSend.copy(alpha = 0.14f)
            },
            contentColor = if (canImport) {
                AccentSend
            } else {
                AccentSend.copy(alpha = 0.46f)
            },
            border = BorderStroke(2.dp, AccentSend.copy(alpha = if (canImport) 0.70f else 0.32f)),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .size(64.dp)
                .testTag("quick_import_import_button"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (uiState.quickImportInProgress) "Importing meal" else "Import meal",
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun CapturePanel(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AccentCapture.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = AccentCapture,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quick capture",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Paste the nutrition blurb and let the app turn it into foods",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear nutrition blurb",
                        tint = AccentClear,
                    )
                }
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Nutrition blurb") },
                keyboardOptions = KeyboardOptions.Default,
                minLines = 8,
                maxLines = 14,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.26f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_import_paste"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDialog(
    addDatabase: Boolean,
    addDay: Boolean,
    writeHealthConnect: Boolean,
    onToggleAddDatabase: () -> Unit,
    onToggleAddDay: () -> Unit,
    onToggleHealthConnect: () -> Unit,
    onDismiss: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetTitleLite("Quick Import settings", "These stay on by default for your usual workflow.")
            DestinationToggle(
                checked = addDatabase,
                text = "Add foods to database",
                icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = AccentDatabase) },
                onClick = onToggleAddDatabase,
                modifier = Modifier.testTag("quick_import_toggle_database"),
            )
            DestinationToggle(
                checked = addDay,
                text = "Add foods to current day",
                icon = { Icon(Icons.Default.Event, contentDescription = null, tint = AccentDay) },
                onClick = onToggleAddDay,
                modifier = Modifier.testTag("quick_import_toggle_day"),
            )
            DestinationToggle(
                checked = writeHealthConnect,
                text = "Write foods to Health Connect",
                icon = { Icon(Icons.Default.CloudDone, contentDescription = null, tint = AccentHealth) },
                onClick = onToggleHealthConnect,
                modifier = Modifier.testTag("quick_import_toggle_health"),
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealTimePanel(
    dateTimeText: String,
    selectedDateTime: LocalDateTime,
    mealType: QuickImportMealType,
    onDateTimeChange: (LocalDateTime) -> Unit,
    onRefreshDateTime: () -> Unit,
    onMealTypeChange: (QuickImportMealType) -> Unit,
) {
    var timeSheetVisible by remember { mutableStateOf(false) }
    var mealTypeSheetVisible by remember { mutableStateOf(false) }
    var mealActionsSheetVisible by remember { mutableStateOf(false) }
    val zoneId = ZoneId.systemDefault()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateTime.toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli(),
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedDateTime.hour,
        initialMinute = selectedDateTime.minute,
        is24Hour = false,
    )

    if (timeSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { timeSheetVisible = false },
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
                SheetTitleLite("Meal time", "The time sets breakfast, lunch, dinner, or snack.")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        onClick = { timeSheetVisible = false },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                val date = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
                                onDateTimeChange(
                                    LocalDateTime.of(date.year, date.month, date.dayOfMonth, timePickerState.hour, timePickerState.minute)
                                )
                            }
                            timeSheetVisible = false
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Set")
                    }
                }
                DatePicker(state = datePickerState)
                TimePicker(state = timePickerState)
            }
        }
    }

    if (mealTypeSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { mealTypeSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetTitleLite("Meal type", "Choose how this meal should be labeled in Health Connect.")
                MealTypeWheel(
                    selectedMealType = mealType,
                    onSelect = {
                        onMealTypeChange(it)
                        mealTypeSheetVisible = false
                    },
                )
            }
        }
    }

    if (mealActionsSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { mealActionsSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetTitleLite("Meal actions", "Adjust the logged time for this meal.")
                MealActionRow(
                    text = "Edit time",
                    iconColor = AccentEdit,
                    icon = { color -> Icon(Icons.Default.Edit, contentDescription = null, tint = color) },
                    onClick = {
                        mealActionsSheetVisible = false
                        timeSheetVisible = true
                    },
                )
                MealActionRow(
                    text = "Now",
                    iconColor = AccentNow,
                    icon = { color -> Icon(Icons.Default.Schedule, contentDescription = null, tint = color) },
                    onClick = {
                        mealActionsSheetVisible = false
                        onRefreshDateTime()
                    },
                )
            }
        }
    }

    SurfacePanel(
        contentPadding = 10,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AccentMeal.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = AccentMeal,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { mealTypeSheetVisible = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "Meal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = mealType.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = dateTimeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { mealActionsSheetVisible = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Meal time actions",
                    tint = AccentEdit,
                )
            }
        }
    }
}

@Composable
private fun MealActionRow(
    text: String,
    iconColor: Color,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            icon(iconColor)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MealTypeWheel(
    selectedMealType: QuickImportMealType,
    onSelect: (QuickImportMealType) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            QuickImportMealType.Breakfast,
            QuickImportMealType.Lunch,
            QuickImportMealType.Dinner,
            QuickImportMealType.Snack,
        ).forEach { mealType ->
            val selected = mealType == selectedMealType
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                    .border(
                        BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                            },
                        ),
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(mealType) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mealType.label,
                    style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SheetTitleLite(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DestinationToggle(
    checked: Boolean,
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                BorderStroke(
                    1.dp,
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                ),
                RoundedCornerShape(8.dp),
            )
            .background(
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onClick() },
        )
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ParsedMealPreviewCard(
    meal: QuickImportMeal,
    mealType: QuickImportMealType,
    targetAllocation: MealTargetAllocation,
    onMealClick: () -> Unit,
    onFoodClick: (QuickImportFood) -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfacePanel(
        modifier = modifier
            .clickable(onClick = onMealClick),
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 12,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ParsedMealTitle(
                mealType = mealType,
                foodCount = meal.foods.size,
                modifier = Modifier.weight(1f),
            )
            QuickMacroSummaryChip(Icons.Default.BakeryDining, meal.totals.carbohydrate, AccentDay)
            QuickMacroSummaryChip(Icons.Default.EggAlt, meal.totals.protein, AccentClear)
            QuickMacroSummaryChip(Icons.Default.Opacity, meal.totals.fat, AccentFood)
            QuickMacroSummaryChip(Icons.Default.Grass, meal.totals.fiber, AccentHealth)
            Text(
                text = "${meal.totals.energy.toFormattedString(true)} kcal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        GoalAllocationRow(targetAllocation)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            meal.foods.forEach { food ->
                CompactQuickFoodRow(
                    food = food,
                    reserveActionSpace = true,
                    onClick = { onFoodClick(food) },
                )
            }
        }
    }
}

@Composable
private fun ParsedMealTitle(
    mealType: QuickImportMealType,
    foodCount: Int,
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
                .background(AccentMeal.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = AccentMeal,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = mealType.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$foodCount foods",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun QuickMacroSummaryChip(
    icon: ImageVector,
    value: Double,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.86f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                RoundedCornerShape(7.dp),
            )
            .heightIn(min = 46.dp)
            .padding(horizontal = 11.dp, vertical = 7.dp),
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

@Composable
private fun CompactQuickFoodRow(
    food: QuickImportFood,
    reserveActionSpace: Boolean = false,
    onClick: () -> Unit,
) {
    val endPadding = if (reserveActionSpace) 132.dp else 10.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.70f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(start = 10.dp, top = 9.dp, end = endPadding, bottom = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = quickFoodDisplayName(food),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${food.nutrients.energy.toFormattedString(true)} kcal",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun quickFoodDisplayName(food: QuickImportFood): String {
    return listOf(food.amountText, food.name)
        .filter { part -> part.isNotBlank() }
        .joinToString(" ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickImportMealDetailSheet(
    meal: QuickImportMeal,
    mealType: QuickImportMealType,
    targetAllocation: MealTargetAllocation,
    onDismiss: () -> Unit,
    onFoodClick: (QuickImportFood) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ParsedMealTitle(
                    mealType = mealType,
                    foodCount = meal.foods.size,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${meal.totals.energy.toFormattedString(true)} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
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
                    QuickMacroSummaryChip(Icons.Default.BakeryDining, meal.totals.carbohydrate, AccentDay)
                    QuickMacroSummaryChip(Icons.Default.EggAlt, meal.totals.protein, AccentClear)
                    QuickMacroSummaryChip(Icons.Default.Opacity, meal.totals.fat, AccentFood)
                    QuickMacroSummaryChip(Icons.Default.Grass, meal.totals.fiber, AccentHealth)
                }
                QuickNutrientDetailGrid(
                    nutrients = meal.totals,
                    includeAmount = null,
                )
                GoalAllocationRow(targetAllocation)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    meal.foods.forEach { food ->
                        CompactQuickFoodRow(
                            food = food,
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

@Composable
private fun GoalAllocationRow(allocation: MealTargetAllocation) {
    if (allocation.calories == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.66f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = AccentSend,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "Meal target",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${allocation.calories.toFormattedString(true)} kcal",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        allocation.protein?.let {
            Text(
                text = "P ${it.toFormattedString(true)}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        allocation.carbs?.let {
            Text(
                text = "C ${it.toFormattedString(true)}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun consumedMealsForQuickImportDate(uiState: AppUiState): List<Pair<QuickImportMealType, QuickImportNutrients>> {
    if (uiState.healthConnectViewerDate != uiState.inputQuickImportDateTime.toLocalDate()) return emptyList()
    return uiState.healthConnectViewerMeals
        .filter { it.startTime.isBefore(uiState.inputQuickImportDateTime) }
        .groupBy { mealTypeFromHealthConnect(it) }
        .map { (mealType, foods) ->
            mealType to sumHealthConnectFoods(foods)
        }
}

private fun sumHealthConnectFoods(foods: List<HealthConnectNutritionMeal>): QuickImportNutrients {
    return QuickImportNutrients(
        energy = foods.sumOf { it.energy },
        carbohydrate = foods.sumOf { it.totalCarbohydrate },
        sugar = foods.sumOf { it.sugar },
        protein = foods.sumOf { it.protein },
        fat = foods.sumOf { it.totalFat },
        saturatedFat = foods.sumOf { it.saturatedFat },
        fiber = foods.sumOf { it.dietaryFiber },
    )
}

private fun mealTypeFromHealthConnect(meal: HealthConnectNutritionMeal): QuickImportMealType {
    return when (meal.mealType) {
        androidx.health.connect.client.records.MealType.MEAL_TYPE_BREAKFAST -> QuickImportMealType.Breakfast
        androidx.health.connect.client.records.MealType.MEAL_TYPE_LUNCH -> QuickImportMealType.Lunch
        androidx.health.connect.client.records.MealType.MEAL_TYPE_DINNER -> QuickImportMealType.Dinner
        androidx.health.connect.client.records.MealType.MEAL_TYPE_SNACK -> QuickImportMealType.Snack
        else -> QuickImportMealType.inferFrom(meal.startTime)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickImportFoodDetailSheet(
    food: QuickImportFood,
    onDismiss: () -> Unit,
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
                    text = quickFoodDisplayName(food),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${food.nutrients.energy.toFormattedString(true)} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            QuickNutrientDetailGrid(
                nutrients = food.nutrients,
                includeAmount = food.amountText.takeIf { it.isNotBlank() },
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun QuickNutrientDetailGrid(
    nutrients: QuickImportNutrients,
    includeAmount: String?,
) {
    val items = buildList {
        if (includeAmount != null) add("Amount" to includeAmount)
        add("Calories" to "${nutrients.energy.toFormattedString(true)} kcal")
        add("Carbs" to "${nutrients.carbohydrate.toFormattedString(true)}g")
        add("Protein" to "${nutrients.protein.toFormattedString(true)}g")
        add("Fat" to "${nutrients.fat.toFormattedString(true)}g")
        add("Fiber" to "${nutrients.fiber.toFormattedString(true)}g")
        add("Sugar" to "${nutrients.sugar.toFormattedString(true)}g")
        add("Sat fat" to "${nutrients.saturatedFat.toFormattedString(true)}g")
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowItems.forEach { (label, value) ->
                    QuickNutrientDetailPill(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickNutrientDetailPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SurfacePanel(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentPadding: Int = 8,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

private fun quickImportResultText(
    databaseEntriesAdded: Int,
    dayFoodsAdded: Int,
    healthWriteResult: QuickImportHealthWriteResult?,
): String {
    val localText = "Added $databaseEntriesAdded database foods and $dayFoodsAdded day foods."
    val healthText = when (healthWriteResult) {
        null -> "Health Connect skipped."
        QuickImportHealthWriteResult.Success -> "Health Connect write succeeded."
        QuickImportHealthWriteResult.HealthConnectUnavailable -> "Health Connect is unavailable."
        QuickImportHealthWriteResult.PermissionsMissing -> "Health Connect permissions are missing."
        is QuickImportHealthWriteResult.Failed -> "Health Connect failed: ${healthWriteResult.message}"
    }
    return "$localText $healthText"
}
