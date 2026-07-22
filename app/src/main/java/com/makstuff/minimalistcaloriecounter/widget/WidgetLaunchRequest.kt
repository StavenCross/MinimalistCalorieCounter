package com.makstuff.minimalistcaloriecounter.widget

import android.content.Intent
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import java.time.LocalDate

internal const val ACTION_WIDGET_ADD_MEAL = "com.makstuff.minimalistcaloriecounter.action.WIDGET_ADD_MEAL"
internal const val EXTRA_WIDGET_TARGET_ROUTE = "com.makstuff.minimalistcaloriecounter.extra.WIDGET_TARGET_ROUTE"
internal const val EXTRA_WIDGET_OPEN_ADD_MEAL = "com.makstuff.minimalistcaloriecounter.extra.WIDGET_OPEN_ADD_MEAL"
internal const val EXTRA_WIDGET_DATE_EPOCH_DAY = "com.makstuff.minimalistcaloriecounter.extra.WIDGET_DATE_EPOCH_DAY"

internal data class WidgetLaunchRequest(
    val targetRoute: String,
    val date: LocalDate,
    val openAddMeal: Boolean,
) {
    companion object {
        fun fromIntent(intent: Intent?): WidgetLaunchRequest? {
            val epochDay = if (intent?.hasExtra(EXTRA_WIDGET_DATE_EPOCH_DAY) == true) {
                intent.getLongExtra(EXTRA_WIDGET_DATE_EPOCH_DAY, LocalDate.now().toEpochDay())
            } else {
                null
            }
            return fromValues(
                action = intent?.action,
                targetRoute = intent?.getStringExtra(EXTRA_WIDGET_TARGET_ROUTE),
                openAddMeal = intent?.getBooleanExtra(EXTRA_WIDGET_OPEN_ADD_MEAL, true) ?: true,
                epochDay = epochDay,
            )
        }

        fun fromValues(
            action: String?,
            targetRoute: String?,
            openAddMeal: Boolean,
            epochDay: Long?,
        ): WidgetLaunchRequest? {
            if (action != ACTION_WIDGET_ADD_MEAL) return null
            return WidgetLaunchRequest(
                targetRoute = targetRoute ?: AppRoutes.HEALTH_CONNECT_NUTRITION,
                date = LocalDate.ofEpochDay(epochDay ?: LocalDate.now().toEpochDay()),
                openAddMeal = openAddMeal,
            )
        }
    }
}

internal fun Intent.configureWidgetAddMealLaunch(date: LocalDate? = null): Intent = apply {
    action = ACTION_WIDGET_ADD_MEAL
    putExtra(EXTRA_WIDGET_TARGET_ROUTE, AppRoutes.HEALTH_CONNECT_NUTRITION)
    putExtra(EXTRA_WIDGET_OPEN_ADD_MEAL, true)
    if (date != null) putExtra(EXTRA_WIDGET_DATE_EPOCH_DAY, date.toEpochDay())
}
