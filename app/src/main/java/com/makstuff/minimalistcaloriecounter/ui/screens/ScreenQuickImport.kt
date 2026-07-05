package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.GoalCalculator
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.quickImportResultText
import com.makstuff.minimalistcaloriecounter.ui.model.consumedMealsForQuickImportDate
import com.makstuff.minimalistcaloriecounter.ui.model.currentDayFoodCountForQuickImportDate
import com.makstuff.minimalistcaloriecounter.ui.model.currentDayTotalsForQuickImportDate
import com.makstuff.minimalistcaloriecounter.ui.model.todayCheckInSummary
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    onParsedFoodChange: (Int, QuickImportFood) -> Unit,
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
    var selectedPreviewFoodIndex by remember { mutableStateOf<Int?>(null) }
    var datePickerVisible by remember { mutableStateOf(false) }
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
        if (datePickerVisible) {
            MealDatePickerSheet(
                selectedDate = uiState.inputQuickImportDateTime.toLocalDate(),
                onDismiss = { datePickerVisible = false },
                onDateSelected = { date ->
                    datePickerVisible = false
                    onDateTimeChange(LocalDateTime.of(date, uiState.inputQuickImportDateTime.toLocalTime()))
                },
            )
        }

        selectedPreviewMeal?.let { previewMeal ->
            QuickImportMealDetailSheet(
                meal = previewMeal,
                mealType = uiState.quickImportMealType,
                targetAllocation = targetAllocation,
                onDismiss = { selectedPreviewMeal = null },
                onFoodClick = { index ->
                    selectedPreviewMeal = null
                    selectedPreviewFoodIndex = index
                },
            )
        }
        selectedPreviewFoodIndex?.let { foodIndex ->
            val previewFood = meal?.foods?.getOrNull(foodIndex)
            if (previewFood == null) {
                selectedPreviewFoodIndex = null
                return@let
            }
            QuickImportFoodDetailSheet(
                food = previewFood,
                onDismiss = { selectedPreviewFoodIndex = null },
                onSave = { updatedFood ->
                    selectedPreviewFoodIndex = null
                    onParsedFoodChange(foodIndex, updatedFood)
                },
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .testTag("quick_import_list"),
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
                MealDateSelector(
                    selectedDate = uiState.inputQuickImportDateTime.toLocalDate(),
                    onPrevious = {
                        onDateTimeChange(uiState.inputQuickImportDateTime.minusDays(1))
                    },
                    onNext = {
                        onDateTimeChange(uiState.inputQuickImportDateTime.plusDays(1))
                    },
                    onDateClick = { datePickerVisible = true },
                    testTagPrefix = "add_meal",
                )
            }

            item {
                MealTimePanel(
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
                        onFoodClick = { index -> selectedPreviewFoodIndex = index },
                        canSave = canImport,
                        isSaving = uiState.quickImportInProgress,
                        onSaveMeal = onImport,
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
    }
}
