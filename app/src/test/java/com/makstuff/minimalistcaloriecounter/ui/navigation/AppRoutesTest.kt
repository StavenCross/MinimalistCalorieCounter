package com.makstuff.minimalistcaloriecounter.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppRoutesTest {
    @Test
    fun routeBuildersCreateParameterizedRoutes() {
        assertEquals("day_add_weight/3", AppRoutes.dayAddWeight(3))
        assertEquals("day_edit_weight/4", AppRoutes.dayEditWeight(4))
        assertEquals("archive_edit_entry/5", AppRoutes.archiveEditEntry(5))
        assertEquals("database_edit_entry/6", AppRoutes.databaseEditEntry(6))
    }

    @Test
    fun automationAliasesMapToStableAppRoutes() {
        assertEquals(AppRoutes.QUICK_IMPORT, AppRoutes.automationRouteFor("quick_import"))
        assertEquals(AppRoutes.QUICK_IMPORT, AppRoutes.automationRouteFor("quick-add"))
        assertEquals(AppRoutes.HEALTH_CONNECT_NUTRITION, AppRoutes.automationRouteFor("meals"))
        assertEquals(AppRoutes.GOALS_HOME, AppRoutes.automationRouteFor("goals"))
        assertEquals(AppRoutes.SETTINGS_HOME, AppRoutes.automationRouteFor("settings"))
    }

    @Test
    fun topLevelRoutesStayCentralized() {
        assertEquals(
            setOf(
                AppRoutes.QUICK_IMPORT,
                AppRoutes.HEALTH_CONNECT_NUTRITION,
                AppRoutes.GOALS_HOME,
                AppRoutes.SETTINGS_HOME,
                AppRoutes.DATABASE_HOME,
            ),
            AppRoutes.topLevelRoutes,
        )
    }

    @Test
    fun unknownAutomationRoutesFailLoudly() {
        assertThrows(IllegalStateException::class.java) {
            AppRoutes.automationRouteFor("unknown")
        }
    }
}
