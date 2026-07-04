package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.GoalCalculator
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.MealTargetAllocation
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.quickImportResultText
import com.makstuff.minimalistcaloriecounter.ui.model.consumedMealsForQuickImportDate
import com.makstuff.minimalistcaloriecounter.ui.model.currentDayFoodCountForQuickImportDate
import com.makstuff.minimalistcaloriecounter.ui.model.currentDayTotalsForQuickImportDate
import com.makstuff.minimalistcaloriecounter.ui.model.macroPercent
import com.makstuff.minimalistcaloriecounter.ui.model.macroProgressArc
import com.makstuff.minimalistcaloriecounter.ui.model.macroSummaryItems
import com.makstuff.minimalistcaloriecounter.ui.model.todayCheckInSummary
import com.makstuff.minimalistcaloriecounter.ui.reused.MacroHintBox
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetTitle
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import kotlinx.coroutines.delay
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
private val AccentDay = Color(0xFFFFB74D)
private val AccentHealth = Color(0xFF4DD0E1)
private val GoalOverage = Color(0xFFFF5252)

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
    onRetryOutbox: (String) -> Unit = {},
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
    val dailyTotals = currentDayTotalsForQuickImportDate(uiState)
    val dailyFoodCount = currentDayFoodCountForQuickImportDate(uiState)
    val dailyGoalProgress = GoalCalculator.progress(
        dailyTotals,
        uiState.goals.activeTargetsFor(uiState.inputQuickImportDateTime.toLocalDate()),
    )
    val activeTargets = uiState.goals.activeTargetsFor(uiState.inputQuickImportDateTime.toLocalDate())
    val checkInMeals = if (uiState.healthConnectViewerDate == uiState.inputQuickImportDateTime.toLocalDate()) {
        uiState.healthConnectViewerMeals
    } else {
        emptyList()
    }
    val clipboard = LocalClipboardManager.current
    var checkInCopied by remember { mutableStateOf(false) }
    val checkInText = todayCheckInSummary(
        date = uiState.inputQuickImportDateTime.toLocalDate(),
        meals = checkInMeals,
        targets = activeTargets,
        profile = uiState.goals.profile,
    )
    LaunchedEffect(checkInText) {
        checkInCopied = false
    }
    var successAnimationVisible by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.quickImportSuccessToken) {
        if (uiState.quickImportSuccessToken > 0L) {
            successAnimationVisible = true
            delay(1_500)
            successAnimationVisible = false
        }
    }

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

            item {
                QuickDaySummaryCard(
                    dateTime = uiState.inputQuickImportDateTime,
                    totals = dailyTotals,
                    foodCount = dailyFoodCount,
                    progress = dailyGoalProgress,
                    checkInCopied = checkInCopied,
                    onCopyCheckIn = {
                        clipboard.setText(AnnotatedString(checkInText))
                        checkInCopied = true
                    },
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

            val outboxAttention = uiState.quickImportOutbox.filter { it.needsAttention }
            val outboxAttentionCount = outboxAttention.size
            if (outboxAttentionCount > 0) {
                item {
                    QuickImportOutboxStatusCard(
                        count = outboxAttentionCount,
                        retryId = outboxAttention.firstOrNull { it.healthPayloads.isNotEmpty() }?.id,
                        onRetry = onRetryOutbox,
                    )
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

        AnimatedVisibility(
            visible = successAnimationVisible,
            enter = fadeIn(animationSpec = tween(140)) + slideInVertically(initialOffsetY = { it / 2 }) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 3 }) + scaleOut(targetScale = 0.96f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .testTag("quick_import_success_pill"),
        ) {
            QuickImportSuccessPill()
        }

        val buttonIsSuccess = successAnimationVisible && !uiState.quickImportInProgress
        val buttonEnabled = canImport && !buttonIsSuccess
        val buttonColor by animateColorAsState(
            targetValue = when {
                buttonIsSuccess -> AccentHealth.copy(alpha = 0.30f)
                canImport -> AccentSend.copy(alpha = 0.28f)
                else -> AccentSend.copy(alpha = 0.14f)
            },
            animationSpec = tween(180),
            label = "quickImportButtonColor",
        )
        val buttonContentColor by animateColorAsState(
            targetValue = when {
                buttonIsSuccess -> AccentHealth
                canImport -> AccentSend
                else -> AccentSend.copy(alpha = 0.46f)
            },
            animationSpec = tween(180),
            label = "quickImportButtonContentColor",
        )
        val buttonBorderColor by animateColorAsState(
            targetValue = when {
                buttonIsSuccess -> AccentHealth.copy(alpha = 0.72f)
                canImport -> AccentSend.copy(alpha = 0.70f)
                else -> AccentSend.copy(alpha = 0.32f)
            },
            animationSpec = tween(180),
            label = "quickImportButtonBorderColor",
        )
        val buttonSize by animateDpAsState(
            targetValue = if (buttonIsSuccess) 70.dp else 64.dp,
            animationSpec = tween(180),
            label = "quickImportButtonSize",
        )
        Surface(
            onClick = { if (buttonEnabled) onImport() },
            enabled = buttonEnabled,
            shape = RoundedCornerShape(18.dp),
            color = buttonColor,
            contentColor = buttonContentColor,
            border = BorderStroke(2.dp, buttonBorderColor),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .size(buttonSize)
                .testTag("quick_import_import_button"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    uiState.quickImportInProgress -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = AccentSend,
                            trackColor = AccentSend.copy(alpha = 0.18f),
                        )
                    }
                    buttonIsSuccess -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Meal added",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Import meal",
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickImportSuccessPill() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, AccentHealth.copy(alpha = 0.42f)),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.testTag("quick_import_success_animation"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AccentHealth,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Meal added",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
                        text = "Meal Capture",
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

@Composable
private fun QuickImportOutboxStatusCard(
    count: Int,
    retryId: String?,
    onRetry: (String) -> Unit,
) {
    SurfacePanel(
        borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.testTag("quick_import_outbox_status"),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = if (count == 1) {
                    "1 Health Connect write needs sync attention."
                } else {
                    "$count Health Connect writes need sync attention."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (retryId != null) {
                TextButton(
                    onClick = { onRetry(retryId) },
                    modifier = Modifier.testTag("quick_import_outbox_retry"),
                ) {
                    Text("Retry")
                }
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
                SheetTitle("Meal time", "The time sets breakfast, lunch, dinner, or snack.")
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
                SheetTitle("Meal type", "Choose how this meal should be labeled in Health Connect.")
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
                SheetTitle("Meal actions", "Adjust the logged time for this meal.")
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
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
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
        ParsedMealSummaryRow(meal, mealType)

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
private fun QuickDaySummaryCard(
    dateTime: LocalDateTime,
    totals: QuickImportNutrients,
    foodCount: Int,
    progress: MacroTargets,
    checkInCopied: Boolean,
    onCopyCheckIn: () -> Unit,
) {
    SurfacePanel(
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 12,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 112.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = dateTime.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${totals.energy.toFormattedString(true)} kcal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                QuickFoodCountChip(foodCount)
            }

            QuickDayMacroGrid(totals)
            QuickGoalProgressRow(progress)
            TextButton(
                onClick = onCopyCheckIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_import_check_in_copy"),
            ) {
                Icon(
                    imageVector = if (checkInCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(if (checkInCopied) "Check-in copied" else "Copy today check-in")
            }
        }
    }
}

@Composable
private fun QuickFoodCountChip(foodCount: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AccentSend.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = AccentSend,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "$foodCount foods",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun QuickDayMacroGrid(totals: QuickImportNutrients) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        macroSummaryItems(totals).chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowItems.forEach { item ->
                    QuickDayMacroPill(item.label, item.value, Modifier.weight(1f))
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickDayMacroPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    MacroHintBox(label = label, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ParsedMealSummaryRow(
    meal: QuickImportMeal,
    mealType: QuickImportMealType,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 520.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ParsedMealTitle(
                    mealType = mealType,
                    foodCount = meal.foods.size,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickMacroSummaryChip("Carbs", Icons.Default.BakeryDining, meal.totals.carbohydrate, AccentDay, Modifier.weight(1f), fillContainer = true)
                    QuickMacroSummaryChip("Protein", Icons.Default.EggAlt, meal.totals.protein, AccentClear, Modifier.weight(1f), fillContainer = true)
                    QuickMacroSummaryChip("Fat", Icons.Default.OilBarrel, meal.totals.fat, AccentFood, Modifier.weight(1f), fillContainer = true)
                    QuickMacroSummaryChip("Fiber", Icons.Default.Grass, meal.totals.fiber, AccentHealth, Modifier.weight(1f), fillContainer = true)
                }
                QuickCaloriesChip(meal.totals.energy, Modifier.align(Alignment.End))
            }
        } else {
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
                QuickMacroSummaryChip("Carbs", Icons.Default.BakeryDining, meal.totals.carbohydrate, AccentDay)
                QuickMacroSummaryChip("Protein", Icons.Default.EggAlt, meal.totals.protein, AccentClear)
                QuickMacroSummaryChip("Fat", Icons.Default.OilBarrel, meal.totals.fat, AccentFood)
                QuickMacroSummaryChip("Fiber", Icons.Default.Grass, meal.totals.fiber, AccentHealth)
                QuickCaloriesChip(meal.totals.energy)
            }
        }
    }
}

@Composable
private fun QuickCaloriesChip(
    calories: Double,
    modifier: Modifier = Modifier,
) {
    MacroHintBox(label = "Calories", modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(AccentSend.copy(alpha = 0.18f))
                .border(BorderStroke(1.dp, AccentSend.copy(alpha = 0.24f)), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = AccentSend,
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
                .testTag("quick_meal_macro_${label.lowercase()}"),
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
                QuickCaloriesChip(meal.totals.energy)
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
                    QuickMacroSummaryChip("Carbs", Icons.Default.BakeryDining, meal.totals.carbohydrate, AccentDay)
                    QuickMacroSummaryChip("Protein", Icons.Default.EggAlt, meal.totals.protein, AccentClear)
                    QuickMacroSummaryChip("Fat", Icons.Default.OilBarrel, meal.totals.fat, AccentFood)
                    QuickMacroSummaryChip("Fiber", Icons.Default.Grass, meal.totals.fiber, AccentHealth)
                }
                QuickNutrientDetailGrid(
                    nutrients = meal.totals,
                    includeAmount = null,
                )
                QuickMealTargetProgressRow(meal.totals, targetAllocation)
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
private fun QuickGoalProgressRow(
    progress: MacroTargets,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuickGoalArcTile(Icons.Default.LocalFireDepartment, progress.calories, AccentSend, "Calories", "daily goal", Modifier.weight(1f))
        QuickGoalArcTile(Icons.Default.EggAlt, progress.protein, AccentClear, "Protein", "daily goal", Modifier.weight(1f))
        QuickGoalArcTile(Icons.Default.BakeryDining, progress.carbs, AccentDay, "Carbs", "daily goal", Modifier.weight(1f))
        QuickGoalArcTile(Icons.Default.OilBarrel, progress.fat, AccentFood, "Fat", "daily goal", Modifier.weight(1f))
        QuickGoalArcTile(Icons.Default.Grass, progress.fiber, AccentHealth, "Fiber", "daily goal", Modifier.weight(1f))
    }
}

@Composable
private fun QuickMealTargetProgressRow(
    totals: QuickImportNutrients,
    allocation: MealTargetAllocation,
) {
    if (allocation.calories == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuickGoalArcTile(Icons.Default.LocalFireDepartment, macroPercent(totals.energy, allocation.calories), AccentSend, "Calories", "meal target", Modifier.weight(1f))
        QuickGoalArcTile(Icons.Default.EggAlt, macroPercent(totals.protein, allocation.protein), AccentClear, "Protein", "meal target", Modifier.weight(1f))
        QuickGoalArcTile(Icons.Default.BakeryDining, macroPercent(totals.carbohydrate, allocation.carbs), AccentDay, "Carbs", "meal target", Modifier.weight(1f))
        QuickGoalArcTile(Icons.Default.OilBarrel, macroPercent(totals.fat, allocation.fat), AccentFood, "Fat", "meal target", Modifier.weight(1f))
        QuickGoalArcTile(Icons.Default.Grass, macroPercent(totals.fiber, allocation.fiber), AccentHealth, "Fiber", "meal target", Modifier.weight(1f))
    }
}

@Composable
private fun QuickGoalArcTile(
    icon: ImageVector,
    value: Double?,
    color: Color,
    label: String,
    descriptionSuffix: String,
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
                    .padding(horizontal = 5.dp, vertical = 6.dp)
                    .testTag("quick_goal_${label.lowercase()}"),
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
                    contentDescription = "$label $descriptionSuffix progress",
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
