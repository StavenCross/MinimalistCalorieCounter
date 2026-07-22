package com.makstuff.minimalistcaloriecounter.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NutritionWidgetUpdater {
    /** Updates every active instance and returns the number that still exist. */
    suspend fun updateAll(context: Context): Int {
        return withContext(Dispatchers.Default) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(NutritionWidget::class.java)
            ids.forEach { id ->
                NutritionWidget().update(context, id)
            }
            ids.size
        }
    }
}
