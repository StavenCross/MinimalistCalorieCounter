package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.ui.model.quickNutrientDetailItems
import com.makstuff.minimalistcaloriecounter.ui.model.supportsMacroHint
import com.makstuff.minimalistcaloriecounter.ui.reused.MacroHintBox

/**
 * Renders Add Meal nutrient detail sheets and pills.
 *
 * These components are shared by meal and food detail drawers. They intentionally stay stateless:
 * callers own selection/dismissal and provide already-parsed quick-import nutrient models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickImportFoodDetailSheet(
    food: QuickImportFood,
    onDismiss: () -> Unit,
    onSave: (QuickImportFood) -> Unit,
) {
    var amount by remember(food) { mutableStateOf(food.amountText) }
    var name by remember(food) { mutableStateOf(food.name) }
    var calories by remember(food) { mutableStateOf(food.nutrients.energy.toFormattedString(true)) }
    var protein by remember(food) { mutableStateOf(food.nutrients.protein.toFormattedString(true)) }
    var carbs by remember(food) { mutableStateOf(food.nutrients.carbohydrate.toFormattedString(true)) }
    var fat by remember(food) { mutableStateOf(food.nutrients.fat.toFormattedString(true)) }
    var fiber by remember(food) { mutableStateOf(food.nutrients.fiber.toFormattedString(true)) }
    var sugar by remember(food) { mutableStateOf(food.nutrients.sugar.toFormattedString(true)) }
    var saturatedFat by remember(food) { mutableStateOf(food.nutrients.saturatedFat.toFormattedString(true)) }
    var error by remember(food) { mutableStateOf<String?>(null) }

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
                    text = "Edit food",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = quickFoodDisplayName(food),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                EditableFoodField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount",
                    modifier = Modifier.weight(0.85f),
                    testTag = "quick_food_edit_amount",
                    numeric = false,
                )
                EditableFoodField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Food",
                    modifier = Modifier.weight(1.4f),
                    testTag = "quick_food_edit_name",
                    numeric = false,
                )
            }
            EditableFoodField(
                value = calories,
                onValueChange = { calories = it },
                label = "Calories",
                modifier = Modifier.fillMaxWidth(),
                testTag = "quick_food_edit_calories",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                EditableFoodField(protein, { protein = it }, "Protein", Modifier.weight(1f), "quick_food_edit_protein")
                EditableFoodField(carbs, { carbs = it }, "Carbs", Modifier.weight(1f), "quick_food_edit_carbs")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                EditableFoodField(fat, { fat = it }, "Fat", Modifier.weight(1f), "quick_food_edit_fat")
                EditableFoodField(fiber, { fiber = it }, "Fiber", Modifier.weight(1f), "quick_food_edit_fiber")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                EditableFoodField(sugar, { sugar = it }, "Sugar", Modifier.weight(1f), "quick_food_edit_sugar")
                EditableFoodField(saturatedFat, { saturatedFat = it }, "Sat fat", Modifier.weight(1f), "quick_food_edit_sat_fat")
            }
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("quick_food_edit_error"),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val updated = runCatching {
                            food.copy(
                                amountText = amount.trim(),
                                name = name.trim().ifBlank { error("Food name is required.") },
                                nutrients = QuickImportNutrients(
                                    energy = calories.parseEditNumber("Calories"),
                                    carbohydrate = carbs.parseEditNumber("Carbs"),
                                    sugar = sugar.parseEditNumber("Sugar"),
                                    protein = protein.parseEditNumber("Protein"),
                                    fat = fat.parseEditNumber("Fat"),
                                    saturatedFat = saturatedFat.parseEditNumber("Sat fat"),
                                    fiber = fiber.parseEditNumber("Fiber"),
                                ),
                            )
                        }
                        updated.fold(
                            onSuccess = onSave,
                            onFailure = { error = it.message ?: "Food values are invalid." },
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_food_edit_save"),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun EditableFoodField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    testTag: String,
    numeric: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = modifier.testTag(testTag),
    )
}

private fun String.parseEditNumber(label: String): Double {
    return trim().toDoubleOrNull() ?: error("$label must be a number.")
}

@Composable
internal fun QuickNutrientDetailGrid(
    nutrients: QuickImportNutrients,
    includeAmount: String?,
) {
    val items = quickNutrientDetailItems(nutrients, includeAmount)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowItems.forEach { item ->
                    QuickNutrientDetailPill(
                        label = item.label,
                        value = item.value,
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
    if (supportsMacroHint(label)) {
        MacroHintBox(label = label, modifier = modifier) {
            QuickNutrientDetailPillContent(label = label, value = value)
        }
        return
    }

    QuickNutrientDetailPillContent(label = label, value = value, modifier = modifier)
}

@Composable
private fun QuickNutrientDetailPillContent(
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

internal fun quickFoodDisplayName(food: QuickImportFood): String =
    listOfNotNull(
        food.amountText.takeIf { it.isNotBlank() },
        food.name,
    ).joinToString(" ")
