package com.makstuff.minimalistcaloriecounter.widget

import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WidgetLaunchRequestTest {
    @Test
    fun fromValuesBuildsAddMealRequest() {
        val date = LocalDate.of(2026, 7, 6)
        val request = WidgetLaunchRequest.fromValues(
            action = ACTION_WIDGET_ADD_MEAL,
            targetRoute = AppRoutes.HEALTH_CONNECT_NUTRITION,
            openAddMeal = true,
            epochDay = date.toEpochDay(),
        )

        requireNotNull(request)
        assertEquals(AppRoutes.HEALTH_CONNECT_NUTRITION, request.targetRoute)
        assertEquals(date, request.date)
        assertEquals(true, request.openAddMeal)
    }

    @Test
    fun fromValuesIgnoresOtherActions() {
        val request = WidgetLaunchRequest.fromValues(
            action = "other",
            targetRoute = AppRoutes.HEALTH_CONNECT_NUTRITION,
            openAddMeal = true,
            epochDay = LocalDate.of(2026, 7, 6).toEpochDay(),
        )

        assertNull(request)
    }
}
