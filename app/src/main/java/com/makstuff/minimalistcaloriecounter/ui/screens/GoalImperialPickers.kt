package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetTitle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val WheelRowHeight = 54.dp
private val WheelHeight = WheelRowHeight * 5

@Composable
internal fun GoalHeightPickerSheet(
    currentHeightCm: Double?,
    onSetHeightCm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(currentHeightCm) { mutableStateOf(currentHeightCm.toImperialHeight()) }

    SheetTitle("Height", "Scroll feet and inches, then set the value.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        GoalNumberWheel(
            label = "Feet",
            values = (3..8).toList(),
            selected = selected.feet,
            suffix = "ft",
            modifier = Modifier.weight(1f),
            testTagPrefix = "goals_height_feet",
            onSelected = { selected = selected.copy(feet = it) },
        )
        GoalNumberWheel(
            label = "Inches",
            values = (0..11).toList(),
            selected = selected.inches,
            suffix = "in",
            modifier = Modifier.weight(1f),
            testTagPrefix = "goals_height_inches",
            onSelected = { selected = selected.copy(inches = it) },
        )
    }
    Button(
        onClick = {
            onSetHeightCm(selected.toCentimeters())
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goals_height_set"),
    ) {
        Text("Set height")
    }
}

@Composable
internal fun GoalWeightPickerSheet(
    currentWeightKg: Double?,
    onSetWeightKg: (Double) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Weight",
    testTagPrefix: String = "goals_weight_lb",
    setButtonTag: String = "goals_weight_set",
    setButtonText: String = "Set weight",
) {
    var selectedPounds by remember(currentWeightKg) { mutableStateOf(currentWeightKg.toImperialPounds()) }

    SheetTitle(title, "Scroll pounds, then set the value.")
    GoalNumberWheel(
        label = "Pounds",
        values = (80..500).toList(),
        selected = selectedPounds,
        suffix = "lb",
        modifier = Modifier.fillMaxWidth(),
        testTagPrefix = testTagPrefix,
        onSelected = { selectedPounds = it },
    )
    Button(
        onClick = {
            onSetWeightKg(selectedPounds.toKilograms())
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(setButtonTag),
    ) {
        Text(setButtonText)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalNumberWheel(
    label: String,
    values: List<Int>,
    selected: Int,
    suffix: String,
    modifier: Modifier = Modifier,
    testTagPrefix: String,
    onSelected: (Int) -> Unit,
) {
    val selectedIndex = values.indexOf(selected).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)

    LaunchedEffect(state, values) {
        snapshotFlow { state.firstVisibleItemIndex }
            .map { values.getOrNull(it) }
            .distinctUntilChanged()
            .collect { value ->
                if (value != null && value != selected) onSelected(value)
            }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                    RoundedCornerShape(20.dp),
                ),
        ) {
            WheelSelectionBand(Modifier.align(Alignment.Center))
            LazyColumn(
                state = state,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = WheelRowHeight * 2),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_wheel"),
            ) {
                items(values) { value ->
                    GoalNumberWheelRow(
                        value = value,
                        suffix = suffix,
                        selected = value == selected,
                        testTag = "${testTagPrefix}_$value",
                        onClick = {
                            onSelected(value)
                            scope.launch {
                                state.animateScrollToItem(values.indexOf(value).coerceAtLeast(0))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelSelectionBand(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WheelRowHeight)
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f))
            .border(
                BorderStroke(1.dp, AccentGoals.copy(alpha = 0.34f)),
                RoundedCornerShape(16.dp),
            ),
    )
}

@Composable
private fun GoalNumberWheelRow(
    value: Int,
    suffix: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(WheelRowHeight)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$value $suffix",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AccentGoals)
        }
    }
}
