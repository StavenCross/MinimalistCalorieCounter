package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.classes.NutritionFoodEditDraft
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionMealGroup
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionServingGroup
import com.makstuff.minimalistcaloriecounter.ui.model.NutritionStatItem
import com.makstuff.minimalistcaloriecounter.ui.model.healthGroupDetailItems
import com.makstuff.minimalistcaloriecounter.ui.model.mealServingGroups
import com.makstuff.minimalistcaloriecounter.ui.model.supportsMacroHint
import com.makstuff.minimalistcaloriecounter.ui.reused.MacroHintBox
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import java.time.format.DateTimeFormatter

@Composable
internal fun MealCard(
    group: NutritionMealGroup,
    expanded: Boolean,
    canToggleExpand: Boolean,
    onToggleExpanded: () -> Unit,
    onMealClick: () -> Unit,
    onFoodClick: (HealthConnectNutritionMeal) -> Unit,
) {
    val calories = group.foods.sumOf { it.energy }
    val protein = group.foods.sumOf { it.protein }
    val carbs = group.foods.sumOf { it.totalCarbohydrate }
    val fat = group.foods.sumOf { it.totalFat }
    val fiber = group.foods.sumOf { it.dietaryFiber }
    val servingGroups = mealServingGroups(group.foods)
    val visibleServingGroups = if (expanded || servingGroups.size <= 3) servingGroups else servingGroups.take(3)
    val hiddenFoodCount = servingGroups.size - visibleServingGroups.size

    SurfacePanel(
        modifier = Modifier
            .clickable(onClick = onMealClick)
            .testTag("meal_card_${group.label.lowercase()}"),
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 12,
        verticalSpacing = 8,
        tonalElevation = 2,
    ) {
        MealSummaryRow(group, calories, carbs, protein, fat, fiber)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            visibleServingGroups.forEach { servingGroup ->
                CompactFoodRow(
                    meal = servingGroup.representative,
                    quantity = servingGroup.quantity,
                    onClick = { onFoodClick(servingGroup.representative) },
                )
            }
        }
        if (canToggleExpand) {
            TextButton(
                onClick = onToggleExpanded,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("meal_expand_toggle_${group.label.lowercase()}"),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse ${group.label}" else "Expand ${group.label}",
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (expanded) "Show fewer foods" else "$hiddenFoodCount more foods",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun MealSummaryRow(
    group: NutritionMealGroup,
    calories: Double,
    carbs: Double,
    protein: Double,
    fat: Double,
    fiber: Double,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 520.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MealTitle(group = group, modifier = Modifier.weight(1f))
                    MealCaloriesChip(calories = calories)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MacroSummaryChip("Carbs", Icons.Default.BakeryDining, carbs, MacroCarbs, Modifier.weight(1f), fillContainer = true)
                    MacroSummaryChip("Protein", Icons.Default.EggAlt, protein, MacroProtein, Modifier.weight(1f), fillContainer = true)
                    MacroSummaryChip("Fat", Icons.Default.OilBarrel, fat, MacroFat, Modifier.weight(1f), fillContainer = true)
                    MacroSummaryChip("Fiber", Icons.Default.Grass, fiber, MacroFiber, Modifier.weight(1f), fillContainer = true)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MealTitle(group = group, modifier = Modifier.weight(1f))
                MacroSummaryChip("Carbs", Icons.Default.BakeryDining, carbs, MacroCarbs)
                MacroSummaryChip("Protein", Icons.Default.EggAlt, protein, MacroProtein)
                MacroSummaryChip("Fat", Icons.Default.OilBarrel, fat, MacroFat)
                MacroSummaryChip("Fiber", Icons.Default.Grass, fiber, MacroFiber)
                MealCaloriesChip(calories = calories)
            }
        }
    }
}

@Composable
private fun MealCaloriesChip(calories: Double) {
    MacroHintBox(label = "Calories") {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(AccentGold.copy(alpha = 0.18f))
                .border(BorderStroke(1.dp, AccentGold.copy(alpha = 0.24f)), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = calories.toFormattedString(true),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MealTitle(
    group: NutritionMealGroup,
    modifier: Modifier = Modifier,
) {
    val color = Color(group.colorArgb)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = group.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${group.foods.size} foods",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MacroSummaryChip(
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
                .testTag("meal_macro_${label.lowercase()}"),
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
private fun CompactFoodRow(
    meal: HealthConnectNutritionMeal,
    quantity: Int = 1,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.78f)
        },
        label = "mealFoodRowColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowColor)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isPressed) 0.22f else 0.14f)),
                RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(start = 10.dp, top = 8.dp, end = 7.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = meal.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (quantity > 1) {
                Text(
                    text = "x$quantity",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    maxLines = 1,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = FoodCaloriesBlue.copy(alpha = if (isPressed) 1f else 0.84f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MealDetailDialog(
    group: NutritionMealGroup,
    copied: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRepeat: () -> Unit,
    onFoodClick: (HealthConnectNutritionMeal) -> Unit,
) {
    val calories = group.foods.sumOf { it.energy }
    val protein = group.foods.sumOf { it.protein }
    val carbs = group.foods.sumOf { it.totalCarbohydrate }
    val fat = group.foods.sumOf { it.totalFat }
    val fiber = group.foods.sumOf { it.dietaryFiber }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.testTag("meals_meal_detail_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MealTitle(group = group, modifier = Modifier.weight(1f))
                Text(
                    text = "${calories.toFormattedString(true)} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.testTag("meals_meal_copy_summary"),
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (copied) "Meal summary copied" else "Copy meal summary",
                        tint = if (copied) MacroFiber else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MacroSummaryChip("Carbs", Icons.Default.BakeryDining, carbs, MacroCarbs)
                    MacroSummaryChip("Protein", Icons.Default.EggAlt, protein, MacroProtein)
                    MacroSummaryChip("Fat", Icons.Default.OilBarrel, fat, MacroFat)
                    MacroSummaryChip("Fiber", Icons.Default.Grass, fiber, MacroFiber)
                }
                MacroGrid(items = healthGroupDetailItems(group))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    mealServingGroups(group.foods).forEach { servingGroup ->
                        CompactFoodRow(
                            meal = servingGroup.representative,
                            quantity = servingGroup.quantity,
                            onClick = { onFoodClick(servingGroup.representative) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = onRepeat,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("meals_repeat_meal_group"),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Repeat")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("meals_delete_meal_group"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Delete meal",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodDetailDialog(
    servingGroup: NutritionServingGroup,
    saveNoticeVisible: Boolean = false,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onAddServing: () -> Unit,
    onRemoveServing: () -> Unit,
    onSaveEdit: (NutritionFoodEditDraft) -> Unit,
) {
    val meal = servingGroup.representative
    var name by remember(servingGroup) { mutableStateOf(meal.name) }
    var calories by remember(servingGroup) { mutableStateOf(meal.energy.toFormattedString(true)) }
    var carbs by remember(servingGroup) { mutableStateOf(meal.totalCarbohydrate.toFormattedString(true)) }
    var protein by remember(servingGroup) { mutableStateOf(meal.protein.toFormattedString(true)) }
    var fat by remember(servingGroup) { mutableStateOf(meal.totalFat.toFormattedString(true)) }
    var fiber by remember(servingGroup) { mutableStateOf(meal.dietaryFiber.toFormattedString(true)) }
    var sugar by remember(servingGroup) { mutableStateOf(meal.sugar.toFormattedString(true)) }
    var saturatedFat by remember(servingGroup) { mutableStateOf(meal.saturatedFat.toFormattedString(true)) }
    val initialDraft = remember(servingGroup) {
        NutritionFoodEditDraft(
            name = meal.name,
            energy = meal.energy,
            totalCarbohydrate = meal.totalCarbohydrate,
            protein = meal.protein,
            totalFat = meal.totalFat,
            dietaryFiber = meal.dietaryFiber,
            sugar = meal.sugar,
            saturatedFat = meal.saturatedFat,
        )
    }
    var lastSubmittedDraft by remember(servingGroup) { mutableStateOf(initialDraft) }
    val draft = NutritionFoodEditDraft(
        name = name.trim(),
        energy = calories.toDoubleOrNull(),
        totalCarbohydrate = carbs.toDoubleOrNull(),
        protein = protein.toDoubleOrNull(),
        totalFat = fat.toDoubleOrNull(),
        dietaryFiber = fiber.toDoubleOrNull(),
        sugar = sugar.toDoubleOrNull(),
        saturatedFat = saturatedFat.toDoubleOrNull(),
    )
    fun submitDraftIfReady() {
        if (!draft.isComplete || draft == lastSubmittedDraft) return
        lastSubmittedDraft = draft
        onSaveEdit(draft)
    }
    ModalBottomSheet(
        onDismissRequest = {
            submitDraftIfReady()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.testTag("meals_food_detail_sheet"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp)
                    .padding(top = 4.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Food") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) submitDraftIfReady() },
                    )
                    Text(
                        text = listOf(
                            meal.startTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                            if (servingGroup.quantity > 1) "x${servingGroup.quantity}" else null,
                        ).filterNotNull().joinToString(" | "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        EditNumberField("Calories", calories, { calories = it }, Modifier.weight(1f), onBlur = ::submitDraftIfReady)
                        EditNumberField("Carbs", carbs, { carbs = it }, Modifier.weight(1f), onBlur = ::submitDraftIfReady)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        EditNumberField("Protein", protein, { protein = it }, Modifier.weight(1f), onBlur = ::submitDraftIfReady)
                        EditNumberField("Fat", fat, { fat = it }, Modifier.weight(1f), onBlur = ::submitDraftIfReady)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        EditNumberField("Fiber", fiber, { fiber = it }, Modifier.weight(1f), onBlur = ::submitDraftIfReady)
                        EditNumberField("Sugar", sugar, { sugar = it }, Modifier.weight(1f), onBlur = ::submitDraftIfReady)
                    }
                    EditNumberField("Sat fat", saturatedFat, { saturatedFat = it }, Modifier.fillMaxWidth(), onBlur = ::submitDraftIfReady)
                    if (servingGroup.quantity > 1) {
                        Text(
                            text = "Macro edits apply to all ${servingGroup.quantity} servings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SurfacePanel(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                    contentPadding = 10,
                    verticalSpacing = 6,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Quantity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onRemoveServing,
                                enabled = servingGroup.quantity > 1,
                                modifier = Modifier.testTag("meals_food_quantity_decrement"),
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = servingGroup.quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("meals_food_quantity_value"),
                            )
                            IconButton(
                                onClick = onAddServing,
                                modifier = Modifier.testTag("meals_food_quantity_increment"),
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meals_food_delete"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (saveNoticeVisible) {
                ChangesSavedInlineChip(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 14.dp)
                        .testTag("meals_food_changes_saved"),
                )
            }
        }
    }
}

@Composable
private fun ChangesSavedInlineChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Changes saved",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun EditNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    onBlur: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.onFocusChanged { if (!it.isFocused) onBlur() },
    )
}

@Composable
internal fun MacroGrid(items: List<NutritionStatItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { item ->
                    StatPill(item.label, item.value, Modifier.weight(1f))
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
    if (supportsMacroHint(label)) {
        MacroHintBox(label = label, modifier = modifier) {
            StatPillContent(label = label, value = value, modifier = Modifier.fillMaxWidth())
        }
        return
    }

    StatPillContent(label = label, value = value, modifier = modifier)
}

@Composable
private fun StatPillContent(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
        )
    }
}

@Composable
internal fun StatusCard(text: String) {
    SurfacePanel(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = 12,
        verticalSpacing = 8,
        tonalElevation = 2,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp, end = 2.dp),
    )
}

internal val AccentGold = Color(0xFFFBBC04)
internal val FoodCaloriesBlue = Color(0xFF4FC3F7)
internal val MacroCarbs = Color(0xFFFFB74D)
internal val MacroProtein = Color(0xFFFF6E7F)
internal val MacroFat = Color(0xFF64B5F6)
internal val MacroFiber = Color(0xFF4DD0E1)
internal val HealthGoalOverage = Color(0xFFFF5252)
