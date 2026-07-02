package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import java.time.format.DateTimeFormatter

@Composable
fun ScreenQuickImport(
    uiState: AppUiState,
    onTextChange: (String) -> Unit,
    onToggleAddDatabase: () -> Unit,
    onToggleAddDay: () -> Unit,
    onToggleHealthConnect: () -> Unit,
    onRefreshDateTime: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val meal = uiState.quickImportMeal
    val hasDestination = uiState.quickImportAddFoodsToDatabase ||
        uiState.quickImportAddFoodsToDay ||
        uiState.quickImportWriteHealthConnect
    val canImport = meal != null && hasDestination && !uiState.quickImportInProgress

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OutlinedTextField(
                value = uiState.inputQuickImportText,
                onValueChange = onTextChange,
                label = { Text("Paste nutrition blurb") },
                keyboardOptions = KeyboardOptions.Default,
                minLines = 8,
                maxLines = 14,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_import_paste"),
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Import time: ${
                        uiState.inputQuickImportDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onRefreshDateTime) {
                    Text("Use current time")
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DestinationToggle(
                    checked = uiState.quickImportAddFoodsToDatabase,
                    text = "Add foods to database",
                    onClick = onToggleAddDatabase,
                    modifier = Modifier.testTag("quick_import_toggle_database"),
                )
                DestinationToggle(
                    checked = uiState.quickImportAddFoodsToDay,
                    text = "Add foods to current day",
                    onClick = onToggleAddDay,
                    modifier = Modifier.testTag("quick_import_toggle_day"),
                )
                DestinationToggle(
                    checked = uiState.quickImportWriteHealthConnect,
                    text = "Write meal to Health Connect",
                    onClick = onToggleHealthConnect,
                    modifier = Modifier.testTag("quick_import_toggle_health"),
                )
            }
        }

        uiState.quickImportError?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        meal?.let {
            item {
                HorizontalDivider()
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Meal totals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("quick_import_preview_totals"),
                    )
                    NutrientSummary(it.totals)
                }
            }
            item {
                Text(
                    text = "Foods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(it.foods) { food ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = listOf(food.amountText, food.name)
                            .filter { part -> part.isNotBlank() }
                            .joinToString(" "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    NutrientSummary(food.nutrients)
                }
            }
        }

        uiState.quickImportResult?.let { result ->
            item {
                Text(
                    text = quickImportResultText(result.databaseEntriesAdded, result.dayFoodsAdded, result.healthWriteResult),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (result.healthWriteResult) {
                        is QuickImportHealthWriteResult.Failed,
                        QuickImportHealthWriteResult.HealthConnectUnavailable,
                        QuickImportHealthWriteResult.PermissionsMissing -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
                Button(
                    onClick = onImport,
                    enabled = canImport,
                    modifier = Modifier.testTag("quick_import_import_button"),
                ) {
                    Text(if (uiState.quickImportInProgress) "Importing" else "Import")
                }
            }
        }
    }
}

@Composable
private fun DestinationToggle(
    checked: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onClick() },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NutrientSummary(nutrients: QuickImportNutrients) {
    Text(
        text = "Calories ${nutrients.energy.toFormattedString(true)}; " +
            "Carbs ${nutrients.carbohydrate.toFormattedString(true)}g; " +
            "Fiber ${nutrients.fiber.toFormattedString(true)}g; " +
            "Protein ${nutrients.protein.toFormattedString(true)}g; " +
            "Fat ${nutrients.fat.toFormattedString(true)}g",
        style = MaterialTheme.typography.bodySmall,
    )
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
