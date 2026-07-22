package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.health.CheckInDateRange
import com.makstuff.minimalistcaloriecounter.health.customCheckInRange
import com.makstuff.minimalistcaloriecounter.health.previousCalendarMonth
import com.makstuff.minimalistcaloriecounter.health.previousCalendarWeek
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetTitle
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class CheckInChildSheet {
    Range,
    StartDate,
    EndDate,
}

@Composable
internal fun CheckInsCard(
    message: String?,
    inProgress: Boolean,
    successToken: Long,
    onOpen: () -> Unit,
) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 16,
        verticalSpacing = 12,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(Icons.Default.TableChart, AccentProfile, 42)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Check-ins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Export a ChatGPT-ready workbook from Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = onOpen,
            enabled = !inProgress,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goals_checkins_open"),
        ) {
            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (inProgress) "Exporting..." else "Create check-in")
        }
        if (successToken > 0 && message != null) {
            CheckInStatusChip(message, isError = false)
        } else if (message != null && !inProgress) {
            CheckInStatusChip(message, isError = message.contains("failed", ignoreCase = true) || message.contains("missing", ignoreCase = true))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CheckInExportSheet(
    inProgress: Boolean,
    onDismiss: () -> Unit,
    onExport: (CheckInDateRange) -> Unit,
) {
    var childSheet by remember { mutableStateOf<CheckInChildSheet?>(null) }
    var customStart by remember { mutableStateOf<LocalDate?>(null) }
    var customEnd by remember { mutableStateOf<LocalDate?>(null) }

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
            SheetTitle("Check-ins", "Create a workbook for your ChatGPT nutrition review.")
            CheckInSelectorRow(
                label = "Range",
                value = when {
                    customStart != null || customEnd != null -> "Custom check-in"
                    else -> "Choose check-in"
                },
                enabled = !inProgress,
                onClick = { childSheet = CheckInChildSheet.Range },
            )
            if (customStart != null || customEnd != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CheckInDateBox(
                        label = "Start",
                        value = customStart?.toString() ?: "Choose",
                        modifier = Modifier.weight(1f),
                        onClick = { childSheet = CheckInChildSheet.StartDate },
                    )
                    CheckInDateBox(
                        label = "End",
                        value = customEnd?.toString() ?: "Choose",
                        modifier = Modifier.weight(1f),
                        onClick = { childSheet = CheckInChildSheet.EndDate },
                    )
                }
            }
            if (customStart != null && customEnd != null) {
                Button(
                    onClick = {
                        onExport(customCheckInRange(requireNotNull(customStart), requireNotNull(customEnd)))
                        onDismiss()
                    },
                    enabled = !inProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goals_checkins_export_custom"),
                ) {
                    Text("Export custom check-in")
                }
            }
            if (inProgress) {
                CheckInStatusChip("Creating workbook...", isError = false)
            }
        }
    }

    childSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { childSheet = null },
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
                when (sheet) {
                    CheckInChildSheet.Range -> CheckInRangePicker(
                        onWeekly = {
                            onExport(previousCalendarWeek())
                            childSheet = null
                            onDismiss()
                        },
                        onMonthly = {
                            onExport(previousCalendarMonth())
                            childSheet = null
                            onDismiss()
                        },
                        onCustom = {
                            customStart = customStart ?: LocalDate.now().minusWeeks(1)
                            customEnd = customEnd ?: LocalDate.now()
                            childSheet = null
                        },
                    )
                    CheckInChildSheet.StartDate -> CheckInDatePicker(
                        title = "Start date",
                        initialDate = customStart ?: LocalDate.now().minusWeeks(1),
                        onPicked = { picked ->
                            customStart = picked
                            childSheet = null
                        },
                    )
                    CheckInChildSheet.EndDate -> CheckInDatePicker(
                        title = "End date",
                        initialDate = customEnd ?: LocalDate.now(),
                        onPicked = { picked ->
                            customEnd = picked
                            childSheet = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInRangePicker(
    onWeekly: () -> Unit,
    onMonthly: () -> Unit,
    onCustom: () -> Unit,
) {
    SheetTitle("Check-in range", "Weekly and monthly exports start right away.")
    CheckInOptionRow("Weekly check-in", previousCalendarWeek().rangeLabel(), onWeekly)
    CheckInOptionRow("Monthly check-in", previousCalendarMonth().rangeLabel(), onMonthly)
    CheckInOptionRow("Custom check-in", "Choose start and end dates", onCustom)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInDatePicker(
    title: String,
    initialDate: LocalDate,
    onPicked: (LocalDate) -> Unit,
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    var appliedMillis by remember(initialMillis) { mutableStateOf(initialMillis) }
    var readyToApply by remember(initialMillis) { mutableStateOf(false) }
    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis
        if (!readyToApply) {
            readyToApply = true
        } else if (millis != null && millis != appliedMillis) {
            appliedMillis = millis
            onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        }
    }
    SheetTitle(title, "Tap a date to use it.")
    DatePicker(state = datePickerState)
}

@Composable
private fun CheckInSelectorRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    SurfacePanel(
        modifier = Modifier
            .testTag("goals_checkins_range_selector")
            .clickable(enabled = enabled, onClick = onClick),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        contentPadding = 12,
        verticalSpacing = 0,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentIcon(Icons.Default.Event, AccentGoals, 34)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AccentGoals)
        }
    }
}

@Composable
private fun CheckInDateBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SurfacePanel(
        modifier = modifier.clickable(onClick = onClick),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        contentPadding = 10,
        verticalSpacing = 3,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CheckInOptionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    SurfacePanel(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("goals_checkins_option_${title.lowercase().replace(" ", "_")}"),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
        contentPadding = 12,
        verticalSpacing = 0,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentIcon(Icons.Default.TableChart, AccentProfile, 34)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Check, contentDescription = null, tint = AccentGoals, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CheckInStatusChip(message: String, isError: Boolean) {
    SurfacePanel(
        backgroundColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f) else AccentGoals.copy(alpha = 0.12f),
        borderColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.38f) else AccentGoals.copy(alpha = 0.20f),
        contentPadding = 10,
        verticalSpacing = 0,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else AccentGoals,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Spacer(Modifier.height(2.dp))
}

private fun CheckInDateRange.rangeLabel(): String = "${startDate} to ${endDate}"
