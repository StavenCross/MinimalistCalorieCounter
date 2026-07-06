package com.makstuff.minimalistcaloriecounter.ui.reused

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@Composable
fun MacroHintBox(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(visible, label) {
        if (visible) {
            delay(1_500)
            visible = false
        }
    }

    Box(
        modifier = modifier.clickable { visible = true },
    ) {
        content()
        if (visible) {
            Popup(
                onDismissRequest = { visible = false },
                offset = IntOffset(0, -92),
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true,
                ),
            ) {
                AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = macroDescription(label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

fun macroDescription(label: String): String {
    return when (label.lowercase()) {
        "calories" -> "Calories are the meal's usable energy."
        "remaining calories" -> "Remaining calories left for today."
        "calories exceeded" -> "Exceeded total calories for the day."
        "carbs" -> "Carbs are the meal's carbohydrate grams."
        "protein" -> "Protein supports muscle repair and fullness."
        "fat" -> "Fat is dietary fat grams, including saturated fat."
        "fiber" -> "Fiber is the carb portion your body does not fully digest."
        "height" -> "Height helps estimate your resting calorie burn."
        "weight" -> "Weight is your latest body weight measurement."
        "body fat" -> "Body fat is the percent of your body weight from fat mass."
        "lean mass" -> "Lean mass is your weight excluding body fat."
        "sex" -> "Sex is used by the calorie formula for your current goal."
        "age" -> "Age helps estimate your resting calorie burn."
        "lifestyle" -> "Lifestyle estimates your daily activity level."
        "weight loss target" -> "Weight loss target is the weekly pace used to adjust calories."
        "locked" -> "Locked values stay manual and will not be replaced by Health Connect."
        "estimated" -> "Estimated values are calculated from your available body measurements."
        else -> label
    }
}
