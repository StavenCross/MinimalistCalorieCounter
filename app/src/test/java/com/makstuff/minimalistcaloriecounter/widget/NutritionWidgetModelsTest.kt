package com.makstuff.minimalistcaloriecounter.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionWidgetModelsTest {
    @Test
    fun metricProgressCalculatesRemainingAndFraction() {
        val metric = MetricProgress("Calories", value = 500.0, target = 2000.0, unit = "kcal")

        assertEquals(1500.0, metric.remaining ?: 0.0, 0.001)
        assertEquals(0.25f, metric.progress, 0.001f)
    }

    @Test
    fun metricProgressCapsOverageAtFullBar() {
        val metric = MetricProgress("Calories", value = 2500.0, target = 2000.0, unit = "kcal")

        assertEquals(-500.0, metric.remaining ?: 0.0, 0.001)
        assertEquals(1.0f, metric.progress, 0.001f)
    }

    @Test
    fun widgetNumberKeepsCompactDecimals() {
        assertEquals("42", 42.0.widgetNumber())
        assertEquals("42.5", 42.45.widgetNumber())
    }
}
