package com.makstuff.minimalistcaloriecounter.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.makstuff.minimalistcaloriecounter.MainActivity

class NutritionWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(160.dp, 56.dp),
            DpSize(160.dp, 96.dp),
            DpSize(260.dp, 150.dp),
            DpSize(320.dp, 230.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = NutritionWidgetDataLoader(context).load()
        provideContent {
            NutritionWidgetContent(state = state, onClickIntent = widgetIntent(context))
        }
    }
}

@Composable
private fun NutritionWidgetContent(state: NutritionWidgetState, onClickIntent: Intent) {
    val size = LocalSize.current
    val isWide = size.width >= 250.dp
    val isTall = size.height >= 170.dp
    val isShort = size.height < 120.dp
    val remaining = state.calories.remaining
    val isOver = remaining != null && remaining < 0.0
    val calorieColor = if (isOver) WidgetColors.red else WidgetColors.gold
    val remainingText = when {
        remaining == null -> "Set goal"
        isOver -> "${kotlin.math.abs(remaining).widgetNumber()}"
        else -> remaining.widgetNumber()
    }
    val remainingLabel = when {
        remaining == null -> "Daily target needed"
        isOver -> "cal over"
        else -> "cal left"
    }
    if (isShort) {
        ShortNutritionWidgetContent(
            state = state,
            remainingText = remainingText,
            remainingLabel = remainingLabel,
            calorieColor = calorieColor,
            onClickIntent = onClickIntent,
        )
        return
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .clickable(actionStartActivity(onClickIntent))
            .padding(10.dp),
    ) {
        WidgetHeader(state = state, showFoodCount = isWide)
        Spacer(GlanceModifier.height(8.dp))
        CalorieBudgetCard(
            remainingText = remainingText,
            remainingLabel = remainingLabel,
            metric = state.calories,
            color = calorieColor,
            compact = !isWide,
        )
        if (isWide) {
            Spacer(GlanceModifier.height(9.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                CompactMetric("Protein", state.protein, WidgetColors.protein, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(8.dp))
                CompactMetric("Carbs", state.carbs, WidgetColors.carbs, GlanceModifier.defaultWeight())
            }
        }
        if (isWide && isTall) {
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                CompactMetric("Fat", state.fat, WidgetColors.fat, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(8.dp))
                CompactMetric("Fiber", state.fiber, WidgetColors.fiber, GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun ShortNutritionWidgetContent(
    state: NutritionWidgetState,
    remainingText: String,
    remainingLabel: String,
    calorieColor: Color,
    onClickIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .clickable(actionStartActivity(onClickIntent))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "Today",
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(
                color = ColorProvider(WidgetColors.muted),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
            ),
        )
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = remainingText,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(calorieColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    textAlign = TextAlign.End,
                ),
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = remainingLabel,
                style = TextStyle(
                    color = ColorProvider(WidgetColors.text),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                ),
            )
        }
        Spacer(GlanceModifier.height(3.dp))
        MetricBar(state.calories, calorieColor, height = 5.dp)
    }
}

@Composable
private fun WidgetHeader(state: NutritionWidgetState, showFoodCount: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Today",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.text),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                ),
            )
            Text(
                text = if (state.permissionsMissing) "Allow Health Connect" else "Tap to add meal",
                style = TextStyle(
                    color = ColorProvider(if (state.permissionsMissing) WidgetColors.redSoft else WidgetColors.muted),
                    fontSize = 11.sp,
                ),
            )
        }
        if (showFoodCount) {
            Box(
                modifier = GlanceModifier
                    .background(WidgetColors.blueSurface)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${state.foodCount} foods",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.blue),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CalorieBudgetCard(
    remainingText: String,
    remainingLabel: String,
    metric: MetricProgress,
    color: Color,
    compact: Boolean,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.20f))
            .padding(horizontal = 12.dp, vertical = if (compact) 9.dp else 11.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = remainingText,
                    style = TextStyle(
                        color = ColorProvider(color),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 28.sp else 32.sp,
                    ),
                )
                Text(
                    text = remainingLabel,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.text),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    ),
                )
            }
            Text(
                text = "${metric.value.widgetNumber()} eaten",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.text),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                ),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        MetricBar(metric, color, height = if (compact) 8.dp else 9.dp)
    }
}

@Composable
private fun CompactMetric(
    label: String,
    metric: MetricProgress,
    color: Color,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(color),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = metric.value.widgetNumber(),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(WidgetColors.text),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.End,
                ),
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        MetricBar(metric, color, height = 6.dp)
    }
}

@Composable
private fun MetricBar(metric: MetricProgress, color: Color, height: androidx.compose.ui.unit.Dp) {
    LinearProgressIndicator(
        progress = metric.progress,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height),
        color = ColorProvider(color),
        backgroundColor = ColorProvider(color.copy(alpha = 0.30f)),
    )
}

private fun widgetIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java).apply {
        configureWidgetAddMealLaunch()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}

private object WidgetColors {
    val background = Color(0xFF101214)
    val text = Color(0xFFF4F6F8)
    val muted = Color(0xFFB1B7BF)
    val gold = Color(0xFFFFD166)
    val blueSurface = Color(0xFF143449)
    val blue = Color(0xFF5CC8FF)
    val protein = Color(0xFFFF6E7F)
    val carbs = Color(0xFFFFB74D)
    val fat = Color(0xFF64B5F6)
    val fiber = Color(0xFF81C784)
    val red = Color(0xFFFF5F6D)
    val redSoft = Color(0xFFFFA7AD)
}
