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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.GoalCalculator
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.quickImportResultText
import com.makstuff.minimalistcaloriecounter.ui.model.consumedMealsForQuickImportDate
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Hosts the Add Meal workflow from the Meals tab without changing navigation state.
 *
 * The caller owns all quick-import state and callbacks; this drawer only composes the existing
 * capture, meal-time, preview, edit, retry, and success surfaces so the Health Connect write path
 * stays centralized even though the old standalone Add Meal route has been removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MealsAddMealDrawer(
    uiState: AppUiState,
    onDismiss: () -> Unit,
    onTextChange: (String) -> Unit,
    onRefreshDateTime: () -> Unit,
    onDateTimeChange: (LocalDateTime) -> Unit,
    onMealTypeChange: (QuickImportMealType) -> Unit,
    onParsedFoodChange: (Int, QuickImportFood) -> Unit,
    onParsedFoodGroupChange: (Int, QuickImportFood) -> Unit = onParsedFoodChange,
    onParsedFoodServingAdd: (Int) -> Unit = {},
    onParsedFoodServingRemove: (Int) -> Unit = {},
    onImport: () -> Unit,
    onClear: () -> Unit,
    onRetryOutbox: (String) -> Unit,
) {
    val meal = uiState.quickImportMeal
    val hasDestination = uiState.quickImportAddFoodsToDatabase ||
        uiState.quickImportAddFoodsToDay ||
        uiState.quickImportWriteHealthConnect
    val canImport = meal != null && hasDestination && !uiState.quickImportInProgress
    val initialSuccessToken = remember { uiState.quickImportSuccessToken }
    val targetAllocation = GoalCalculator.mealAllocation(
        mealType = uiState.quickImportMealType,
        targets = uiState.goals.activeTargetsFor(uiState.inputQuickImportDateTime.toLocalDate()),
        consumedMeals = consumedMealsForQuickImportDate(uiState),
    )
    var selectedPreviewMeal by remember { mutableStateOf<QuickImportMeal?>(null) }
    var selectedPreviewFoodIndex by remember { mutableStateOf<Int?>(null) }
    var successAnimationVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.quickImportSuccessToken) {
        if (uiState.quickImportSuccessToken > initialSuccessToken) {
            successAnimationVisible = true
            delay(1_000)
            successAnimationVisible = false
            onDismiss()
        }
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
            quantity = meal.foods.count { it == previewFood },
            onDismiss = { selectedPreviewFoodIndex = null },
            onSave = { updatedFood ->
                selectedPreviewFoodIndex = null
                onParsedFoodGroupChange(foodIndex, updatedFood)
            },
            onIncrementQuantity = { onParsedFoodServingAdd(foodIndex) },
            onDecrementQuantity = { onParsedFoodServingRemove(foodIndex) },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.testTag("meals_add_meal_drawer"),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MealTimePanel(
                    selectedDateTime = uiState.inputQuickImportDateTime,
                    mealType = uiState.quickImportMealType,
                    onDateTimeChange = onDateTimeChange,
                    onRefreshDateTime = onRefreshDateTime,
                    onMealTypeChange = onMealTypeChange,
                    showActions = false,
                    wholeCardSelectsMeal = true,
                    compactHeader = true,
                )
                CapturePanel(
                    value = uiState.inputQuickImportText,
                    onValueChange = onTextChange,
                    onClear = onClear,
                )
                AddMealDrawerStatus(uiState = uiState, onRetryOutbox = onRetryOutbox)
                meal?.let {
                    ParsedMealPreviewCard(
                        meal = it,
                        mealType = uiState.quickImportMealType,
                        targetAllocation = targetAllocation,
                        onMealClick = { selectedPreviewMeal = it },
                        onFoodClick = { index -> selectedPreviewFoodIndex = index },
                        canSave = canImport,
                        isSaving = uiState.quickImportInProgress,
                        onSaveMeal = onImport,
                        modifier = Modifier.testTag("meals_add_meal_preview"),
                    )
                }
            }

            AddMealSuccessOverlay(
                visible = successAnimationVisible,
                modifier = Modifier
                    .padding(bottom = 28.dp)
                    .testTag("meals_add_meal_success_pill"),
            )
        }
    }
}

/**
 * Shows the transient success affordance for the Meals-hosted Add Meal drawer.
 *
 * The animation is kept outside the scroll column so a successful save is visible even when the
 * drawer content is scrolled down to the parsed meal preview.
 */
@Composable
private fun AddMealSuccessOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(140)) + slideInVertically(initialOffsetY = { it / 2 }) + scaleIn(initialScale = 0.94f),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 3 }) + scaleOut(targetScale = 0.96f),
        modifier = modifier,
    ) {
        QuickImportSuccessPill()
    }
}

/**
 * Renders non-blocking Add Meal status inside the drawer.
 *
 * Error, outbox, and result state all come from `AppUiState`; retry is delegated back to the
 * ViewModel so duplicate-prevention, stored payloads, and Health Connect permission handling stay
 * centralized.
 */
@Composable
private fun AddMealDrawerStatus(
    uiState: AppUiState,
    onRetryOutbox: (String) -> Unit,
) {
    uiState.quickImportError?.let { error ->
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

    val outboxAttention = uiState.quickImportOutbox.filter { it.needsAttention }
    if (outboxAttention.isNotEmpty()) {
        QuickImportOutboxStatusCard(
            count = outboxAttention.size,
            retryId = outboxAttention.firstOrNull { it.healthPayloads.isNotEmpty() }?.id,
            onRetry = onRetryOutbox,
        )
    }

    uiState.quickImportResult?.let { result ->
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
