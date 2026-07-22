package com.makstuff.minimalistcaloriecounter.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NutritionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NutritionWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        NutritionWidgetRefreshScheduler.scheduleNext(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        NutritionWidgetRefreshScheduler.scheduleNext(context)
    }

    override fun onDisabled(context: Context) {
        NutritionWidgetRefreshScheduler.cancel(context)
        super.onDisabled(context)
    }
}
