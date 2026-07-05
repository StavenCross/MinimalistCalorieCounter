package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.MealTargetAllocation
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.ui.model.macroPercent
import com.makstuff.minimalistcaloriecounter.ui.model.macroProgressArc
import com.makstuff.minimalistcaloriecounter.ui.model.macroSummaryItems
import com.makstuff.minimalistcaloriecounter.ui.reused.MacroHintBox
import com.makstuff.minimalistcaloriecounter.ui.reused.SurfacePanel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun ParsedMealPreviewCard(
    meal: QuickImportMeal,
    mealType: QuickImportMealType,
    targetAllocation: MealTargetAllocation,
    onMealClick: () -> Unit,
    onFoodClick: (Int) -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    onSaveMeal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfacePanel(
        modifier = modifier.clickable(onClick = onMealClick),
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 12,
    ) {
        ParsedMealSummaryRow(meal, mealType)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            meal.foods.forEachIndexed { index, food ->
                CompactQuickFoodRow(
                    food = food,
                    testTag = "quick_import_food_row_$index",
                    onClick = { onFoodClick(index) },
                )
            }
        }
        Button(
            onClick = onSaveMeal,
            enabled = canSave && !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_import_save_meal_button"),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text("Save meal", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
internal fun QuickDaySummaryCard(
    dateTime: LocalDateTime,
    totals: QuickImportNutrients,
    foodCount: Int,
    progress: MacroTargets,
    checkInCopied: Boolean,
    onCopyCheckIn: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    SurfacePanel(
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 12,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.weight(1f),
                ) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickFoodCountChip(foodCount)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("quick_import_day_actions"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Day actions",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (checkInCopied) "Check-in copied" else "Copy today check-in") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (checkInCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onCopyCheckIn()
                                },
                                modifier = Modifier.testTag("quick_import_check_in_copy"),
                            )
                        }
                    }
                }
            }

            QuickDayMacroGrid(totals)
            QuickGoalProgressRow(progress)
        }
    }
}

@Composable
private fun QuickFoodCountChip(foodCount: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = QuickAccentSend.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = QuickAccentSend,
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
                    QuickMacroSummaryChip("Carbs", Icons.Default.BakeryDining, meal.totals.carbohydrate, QuickAccentDay, Modifier.weight(1f), fillContainer = true)
                    QuickMacroSummaryChip("Protein", Icons.Default.EggAlt, meal.totals.protein, QuickAccentClear, Modifier.weight(1f), fillContainer = true)
                    QuickMacroSummaryChip("Fat", Icons.Default.OilBarrel, meal.totals.fat, QuickAccentFood, Modifier.weight(1f), fillContainer = true)
                    QuickMacroSummaryChip("Fiber", Icons.Default.Grass, meal.totals.fiber, QuickAccentHealth, Modifier.weight(1f), fillContainer = true)
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
                QuickMacroSummaryChip("Carbs", Icons.Default.BakeryDining, meal.totals.carbohydrate, QuickAccentDay)
                QuickMacroSummaryChip("Protein", Icons.Default.EggAlt, meal.totals.protein, QuickAccentClear)
                QuickMacroSummaryChip("Fat", Icons.Default.OilBarrel, meal.totals.fat, QuickAccentFood)
                QuickMacroSummaryChip("Fiber", Icons.Default.Grass, meal.totals.fiber, QuickAccentHealth)
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
                .background(QuickAccentMeal.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = QuickAccentMeal,
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
    testTag: String? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.74f)
        },
        label = "quickFoodRowColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
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
        Text(
            text = quickFoodDisplayName(food),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(QuickAccentSend.copy(alpha = if (isPressed) 0.24f else 0.18f))
                .border(BorderStroke(1.dp, QuickAccentSend.copy(alpha = if (isPressed) 0.38f else 0.26f)), RoundedCornerShape(999.dp))
                .padding(start = 9.dp, top = 5.dp, end = 4.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = QuickAccentSend,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = food.nutrients.energy.toFormattedString(true),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = QuickAccentSend,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickImportMealDetailSheet(
    meal: QuickImportMeal,
    mealType: QuickImportMealType,
    targetAllocation: MealTargetAllocation,
    onDismiss: () -> Unit,
    onFoodClick: (Int) -> Unit,
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
                    QuickMacroSummaryChip("Carbs", Icons.Default.BakeryDining, meal.totals.carbohydrate, QuickAccentDay)
                    QuickMacroSummaryChip("Protein", Icons.Default.EggAlt, meal.totals.protein, QuickAccentClear)
                    QuickMacroSummaryChip("Fat", Icons.Default.OilBarrel, meal.totals.fat, QuickAccentFood)
                    QuickMacroSummaryChip("Fiber", Icons.Default.Grass, meal.totals.fiber, QuickAccentHealth)
                }
                QuickNutrientDetailGrid(
                    nutrients = meal.totals,
                    includeAmount = null,
                )
                QuickMealTargetProgressRow(meal.totals, targetAllocation)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    meal.foods.forEachIndexed { index, food ->
                        CompactQuickFoodRow(
                            food = food,
                            testTag = "quick_import_meal_detail_food_row_$index",
                            onClick = { onFoodClick(index) },
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
private fun QuickGoalProgressRow(progress: MacroTargets) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 420.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickGoalArcTile(Icons.Default.LocalFireDepartment, progress.calories, QuickAccentSend, "Calories", "daily goal", Modifier.weight(1f))
                    QuickGoalArcTile(Icons.Default.EggAlt, progress.protein, QuickAccentClear, "Protein", "daily goal", Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickGoalArcTile(Icons.Default.BakeryDining, progress.carbs, QuickAccentDay, "Carbs", "daily goal", Modifier.weight(1f))
                    QuickGoalArcTile(Icons.Default.OilBarrel, progress.fat, QuickAccentFood, "Fat", "daily goal", Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickGoalArcTile(Icons.Default.Grass, progress.fiber, QuickAccentHealth, "Fiber", "daily goal", Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickGoalArcTile(Icons.Default.LocalFireDepartment, progress.calories, QuickAccentSend, "Calories", "daily goal", Modifier.weight(1f))
                QuickGoalArcTile(Icons.Default.EggAlt, progress.protein, QuickAccentClear, "Protein", "daily goal", Modifier.weight(1f))
                QuickGoalArcTile(Icons.Default.BakeryDining, progress.carbs, QuickAccentDay, "Carbs", "daily goal", Modifier.weight(1f))
                QuickGoalArcTile(Icons.Default.OilBarrel, progress.fat, QuickAccentFood, "Fat", "daily goal", Modifier.weight(1f))
                QuickGoalArcTile(Icons.Default.Grass, progress.fiber, QuickAccentHealth, "Fiber", "daily goal", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickMealTargetProgressRow(
    totals: QuickImportNutrients,
    allocation: MealTargetAllocation,
) {
    if (allocation.calories == null) return
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 420.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickGoalArcTile(Icons.Default.LocalFireDepartment, macroPercent(totals.energy, allocation.calories), QuickAccentSend, "Calories", "meal target", Modifier.weight(1f))
                    QuickGoalArcTile(Icons.Default.EggAlt, macroPercent(totals.protein, allocation.protein), QuickAccentClear, "Protein", "meal target", Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickGoalArcTile(Icons.Default.BakeryDining, macroPercent(totals.carbohydrate, allocation.carbs), QuickAccentDay, "Carbs", "meal target", Modifier.weight(1f))
                    QuickGoalArcTile(Icons.Default.OilBarrel, macroPercent(totals.fat, allocation.fat), QuickAccentFood, "Fat", "meal target", Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickGoalArcTile(Icons.Default.Grass, macroPercent(totals.fiber, allocation.fiber), QuickAccentHealth, "Fiber", "meal target", Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickGoalArcTile(Icons.Default.LocalFireDepartment, macroPercent(totals.energy, allocation.calories), QuickAccentSend, "Calories", "meal target", Modifier.weight(1f))
                QuickGoalArcTile(Icons.Default.EggAlt, macroPercent(totals.protein, allocation.protein), QuickAccentClear, "Protein", "meal target", Modifier.weight(1f))
                QuickGoalArcTile(Icons.Default.BakeryDining, macroPercent(totals.carbohydrate, allocation.carbs), QuickAccentDay, "Carbs", "meal target", Modifier.weight(1f))
                QuickGoalArcTile(Icons.Default.OilBarrel, macroPercent(totals.fat, allocation.fat), QuickAccentFood, "Fat", "meal target", Modifier.weight(1f))
                QuickGoalArcTile(Icons.Default.Grass, macroPercent(totals.fiber, allocation.fiber), QuickAccentHealth, "Fiber", "meal target", Modifier.weight(1f))
            }
        }
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
    val progressColor = if (arc.isOverTarget) QuickGoalOverage else color
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

internal val QuickAccentCapture = Color(0xFFFF8A65)
internal val QuickAccentMeal = Color(0xFFB39DDB)
internal val QuickAccentFood = Color(0xFF64B5F6)
internal val QuickAccentSend = Color(0xFF4FC3F7)
internal val QuickAccentClear = Color(0xFFFF6E7F)
internal val QuickAccentEdit = Color(0xFFFFD166)
internal val QuickAccentNow = Color(0xFF7BDFF2)
internal val QuickAccentDay = Color(0xFFFFB74D)
internal val QuickAccentHealth = Color(0xFF4DD0E1)
internal val QuickGoalOverage = Color(0xFFFF5252)
