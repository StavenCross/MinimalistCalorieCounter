package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.GoalFieldKey
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetTitle
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalsSettingsSheet(
    uiState: AppUiState,
    onDismiss: () -> Unit,
    onRefreshHealthConnect: () -> Unit,
    onRecalculate: () -> Unit,
    onBirthdayChange: (LocalDate?) -> Unit,
    onSexChange: (GoalSex) -> Unit,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    onWeightLossTargetChange: (WeeklyWeightLossTarget) -> Unit,
    onMeasurementChange: (GoalFieldKey, Double?) -> Unit,
    onMeasurementLockToggle: (GoalFieldKey) -> Unit,
    onMacroChange: (GoalMacro, Double?) -> Unit,
    onMacroLockToggle: (GoalMacro) -> Unit,
) {
    val missingFields = uiState.goals.profile.missingRequiredFields()

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetTitle("Goal settings", "Manual entries lock the field. Unlock it when Health Connect should win.")
            if (missingFields.isNotEmpty()) {
                MissingFieldsPanel(missingFields)
            }
            Button(onClick = onRefreshHealthConnect, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pull from Health Connect")
            }
            ProfileEditor(
                uiState = uiState,
                onBirthdayChange = onBirthdayChange,
                onSexChange = onSexChange,
                onActivityLevelChange = onActivityLevelChange,
                onWeightLossTargetChange = onWeightLossTargetChange,
                onMeasurementChange = onMeasurementChange,
                onMeasurementLockToggle = onMeasurementLockToggle,
            )
            MacroEditor(
                targets = uiState.goals.currentTargets,
                onMacroChange = onMacroChange,
                onMacroLockToggle = onMacroLockToggle,
            )
            Button(
                onClick = {
                    onRecalculate()
                    if (missingFields.isEmpty()) onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Recalculate")
            }
        }
    }
}

@Composable
private fun MissingFieldsPanel(missingFields: List<String>) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.40f),
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.42f),
        contentPadding = 10,
        verticalSpacing = 10,
    ) {
        Text(
            text = "Missing ${missingFields.joinToString(", ")}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditor(
    uiState: AppUiState,
    onBirthdayChange: (LocalDate?) -> Unit,
    onSexChange: (GoalSex) -> Unit,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    onWeightLossTargetChange: (WeeklyWeightLossTarget) -> Unit,
    onMeasurementChange: (GoalFieldKey, Double?) -> Unit,
    onMeasurementLockToggle: (GoalFieldKey) -> Unit,
) {
    val profile = uiState.goals.profile
    var pickerSheet by remember { mutableStateOf<GoalPickerSheet?>(null) }
    val zoneId = ZoneId.systemDefault()

    pickerSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { pickerSheet = null },
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
                    GoalPickerSheet.Birthday -> {
                        val initialMillis = profile.birthday
                            ?.atStartOfDay(zoneId)
                            ?.toInstant()
                            ?.toEpochMilli()
                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                        var appliedMillis by remember(initialMillis) { mutableStateOf(initialMillis) }
                        LaunchedEffect(datePickerState.selectedDateMillis) {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null && millis != appliedMillis) {
                                appliedMillis = millis
                                onBirthdayChange(Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate())
                                pickerSheet = null
                            }
                        }

                        SheetTitle("Birthday", "Tap a date to set it.")
                        DatePicker(state = datePickerState)
                    }
                    GoalPickerSheet.Sex -> {
                        SheetTitle("Sex", "Used by the Mifflin-St Jeor recommendation.")
                        GoalSex.entries.forEach { sex ->
                            PickerOptionRow(
                                label = sex.label,
                                selected = profile.sex == sex,
                                testTag = "goals_sex_option_${sex.name}",
                                onClick = {
                                    onSexChange(sex)
                                    pickerSheet = null
                                },
                            )
                        }
                    }
                    GoalPickerSheet.Activity -> {
                        SheetTitle("Lifestyle", "Choose the activity level used for TDEE.")
                        ActivityLevel.entries.forEach { activity ->
                            PickerOptionRow(
                                label = activity.label,
                                selected = profile.activityLevel == activity,
                                testTag = "goals_activity_option_${activity.name}",
                                onClick = {
                                    onActivityLevelChange(activity)
                                    pickerSheet = null
                                },
                            )
                        }
                    }
                    GoalPickerSheet.WeightLoss -> {
                        SheetTitle("Weekly pace", "Choose the calorie deficit target.")
                        WeeklyWeightLossTarget.entries.forEach { target ->
                            PickerOptionRow(
                                label = target.label,
                                selected = profile.weightLossTarget == target,
                                testTag = "goals_weight_loss_option_${target.name}",
                                onClick = {
                                    onWeightLossTargetChange(target)
                                    pickerSheet = null
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    SettingsGroup("Profile", Icons.Default.Event, AccentProfile) {
        PickerField(
            label = "Birthday",
            value = profile.birthday?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Required",
            isMissing = profile.birthday == null,
            testTag = "goals_birthday_picker",
            onClick = { pickerSheet = GoalPickerSheet.Birthday },
        )
        PickerField(
            label = "Sex",
            value = profile.sex?.label ?: "Required",
            isMissing = profile.sex == null,
            testTag = "goals_sex_picker",
            onClick = { pickerSheet = GoalPickerSheet.Sex },
        )
        MeasurementField("Height", GoalFieldKey.HeightCm, profile.heightCm, "cm", onMeasurementChange, onMeasurementLockToggle)
        MeasurementField("Weight", GoalFieldKey.WeightKg, profile.weightKg, "kg", onMeasurementChange, onMeasurementLockToggle)
        MeasurementField("Body fat", GoalFieldKey.BodyFatPercent, profile.bodyFatPercent, "%", onMeasurementChange, onMeasurementLockToggle)
        MeasurementField("Lean mass", GoalFieldKey.LeanMassKg, profile.leanMassKg, "kg", onMeasurementChange, onMeasurementLockToggle)
        PickerField(
            label = "Lifestyle",
            value = profile.activityLevel.label,
            testTag = "goals_activity_picker",
            onClick = { pickerSheet = GoalPickerSheet.Activity },
        )
        PickerField(
            label = "Weekly pace",
            value = profile.weightLossTarget.label,
            testTag = "goals_weight_loss_picker",
            onClick = { pickerSheet = GoalPickerSheet.WeightLoss },
        )
    }
}

@Composable
private fun MacroEditor(
    targets: MacroTargets,
    onMacroChange: (GoalMacro, Double?) -> Unit,
    onMacroLockToggle: (GoalMacro) -> Unit,
) {
    SettingsGroup("Macro targets", Icons.Default.Scale, AccentGoals) {
        MacroInput("Calories", GoalMacro.Calories, targets.calories, "kcal", targets, onMacroChange, onMacroLockToggle)
        MacroInput("Protein", GoalMacro.Protein, targets.protein, "g", targets, onMacroChange, onMacroLockToggle)
        MacroInput("Carbs", GoalMacro.Carbs, targets.carbs, "g", targets, onMacroChange, onMacroLockToggle)
        MacroInput("Fat", GoalMacro.Fat, targets.fat, "g", targets, onMacroChange, onMacroLockToggle)
        MacroInput("Fiber", GoalMacro.Fiber, targets.fiber, "g", targets, onMacroChange, onMacroLockToggle)
    }
}

@Composable
private fun MeasurementField(
    label: String,
    field: GoalFieldKey,
    measurement: GoalMeasurement,
    suffix: String,
    onMeasurementChange: (GoalFieldKey, Double?) -> Unit,
    onMeasurementLockToggle: (GoalFieldKey) -> Unit,
) {
    var text by remember(measurement.value) { mutableStateOf(measurement.value?.toFormattedString(false).orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onMeasurementChange(field, it.toDoubleOrNull())
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = {
            LockButton(locked = measurement.locked, onClick = { onMeasurementLockToggle(field) })
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goals_measurement_${field.name}"),
        colors = fieldColors(),
    )
}

@Composable
private fun MacroInput(
    label: String,
    macro: GoalMacro,
    value: Double?,
    suffix: String,
    targets: MacroTargets,
    onMacroChange: (GoalMacro, Double?) -> Unit,
    onMacroLockToggle: (GoalMacro) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value?.toFormattedString(false).orEmpty()) }
    val locked = macro in targets.lockedMacros
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onMacroChange(macro, it.toDoubleOrNull())
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = { LockButton(locked = locked, onClick = { onMacroLockToggle(macro) }) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goals_macro_${macro.name}"),
        colors = fieldColors(),
    )
}

@Composable
private fun LockButton(locked: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = if (locked) "Unlock field" else "Lock field",
            tint = if (locked) AccentGoals else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    isMissing: Boolean = false,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f))
            .border(
                BorderStroke(
                    1.dp,
                    if (isMissing) MaterialTheme.colorScheme.error.copy(alpha = 0.46f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                ),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PickerOptionRow(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.44f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                ),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AccentGoals)
        }
    }
}

private enum class GoalPickerSheet {
    Birthday,
    Sex,
    Activity,
    WeightLoss,
}

@Composable
private fun SettingsGroup(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        contentPadding = 12,
        verticalSpacing = 10,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentIcon(icon, color, 34)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f),
    focusedBorderColor = AccentGoals,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
)
