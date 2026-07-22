package com.makstuff.minimalistcaloriecounter.widget

internal data class NutritionWidgetState(
    val calories: MetricProgress,
    val protein: MetricProgress,
    val carbs: MetricProgress,
    val fat: MetricProgress,
    val fiber: MetricProgress,
    val foodCount: Int,
    val permissionsMissing: Boolean = false,
)

internal data class MetricProgress(
    val label: String,
    val value: Double,
    val target: Double?,
    val unit: String,
) {
    val remaining: Double?
        get() = target?.minus(value)

    val progress: Float
        get() = if (target == null || target <= 0.0) 0f else (value / target).coerceIn(0.0, 1.0).toFloat()
}

internal fun Double.widgetNumber(): String {
    return if (this % 1.0 == 0.0) toLong().toString() else "%.1f".format(java.util.Locale.US, this)
}
