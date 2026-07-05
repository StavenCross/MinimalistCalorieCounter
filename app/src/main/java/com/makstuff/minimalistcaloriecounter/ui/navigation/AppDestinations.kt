package com.makstuff.minimalistcaloriecounter.ui.navigation

import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.essentials.NAV_ARCHIVE
import com.makstuff.minimalistcaloriecounter.essentials.NAV_DATABASE
import com.makstuff.minimalistcaloriecounter.essentials.NAV_DAY
import com.makstuff.minimalistcaloriecounter.essentials.NAV_GOALS
import com.makstuff.minimalistcaloriecounter.essentials.NavButton

data class AppDestination(
    val route: String,
    val label: String,
    val iconId: Int,
    val navButton: NavButton,
    val accentArgb: Long,
    val showInBottomBar: Boolean,
    val showInDrawer: Boolean,
)

object AppDestinations {
    private val all = listOf(
        AppDestination(AppRoutes.HEALTH_CONNECT_NUTRITION, "Meals", R.drawable.meals, NAV_ARCHIVE, 0xFFFFB74D, true, false),
        AppDestination(AppRoutes.QUICK_IMPORT, "Add Meal", R.drawable.plus, NAV_DAY, 0xFF4FC3F7, false, false),
        AppDestination(AppRoutes.GOALS_HOME, "Goals", R.drawable.goals, NAV_GOALS, 0xFFFF6E7F, true, false),
        AppDestination(AppRoutes.DATABASE_HOME, "Database", R.drawable.archive, NAV_DATABASE, 0xFF90CAF9, false, true),
        AppDestination(AppRoutes.SETTINGS_HOME, "Options", R.drawable.goals, NAV_DAY, 0xFFFFD166, false, true),
    )

    val bottomBar: List<AppDestination> = all.filter { it.showInBottomBar }
    val drawer: List<AppDestination> = all.filter { it.showInDrawer }

    fun forRoute(route: String): AppDestination? = all.firstOrNull { it.route == route }
}
