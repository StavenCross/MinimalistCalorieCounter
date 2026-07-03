package com.makstuff.minimalistcaloriecounter.ui.navigation

object AppRoutes {
    const val DAY_HOME = "day_home"
    const val DAY_CONTENT = "day_content"
    const val QUICK_IMPORT = "quick_import"
    const val DAY_ADD_FOOD = "day_add_food"
    const val DAY_ADD_WEIGHT = "day_add_weight/{index}"
    const val DAY_EDIT_WEIGHT = "day_edit_weight/{index}"
    const val ARCHIVE_HOME = "archive_home"
    const val HEALTH_CONNECT_NUTRITION = "health_connect_nutrition"
    const val GOALS_HOME = "goals_home"
    const val SETTINGS_HOME = "settings_home"
    const val ARCHIVE_CREATE_ENTRY_MANUALLY = "archive_create_entry_manually"
    const val ARCHIVE_CREATE_ENTRY_FROM_DAY = "archive_create_entry_from_day"
    const val ARCHIVE_EDIT_ENTRY = "archive_edit_entry/{index}"
    const val DATABASE_HOME = "database_home"
    const val DATABASE_EDIT_ENTRY = "database_edit_entry/{index}"
    const val CREATE_HOME = "create_home"

    fun dayAddWeight(index: Int): String = "day_add_weight/$index"

    fun dayEditWeight(index: Int): String = "day_edit_weight/$index"

    fun archiveEditEntry(index: Int): String = "archive_edit_entry/$index"

    fun databaseEditEntry(index: Int): String = "database_edit_entry/$index"

    val topLevelRoutes = setOf(
        QUICK_IMPORT,
        HEALTH_CONNECT_NUTRITION,
        GOALS_HOME,
        SETTINGS_HOME,
        DATABASE_HOME,
    )

    fun isTopLevelRoute(route: String): Boolean = route in topLevelRoutes

    fun automationRouteFor(screen: String): String {
        return when (screen.lowercase().replace("-", "_")) {
            "quick_add", QUICK_IMPORT -> QUICK_IMPORT
            "meals", "health_connect", HEALTH_CONNECT_NUTRITION -> HEALTH_CONNECT_NUTRITION
            "goals", GOALS_HOME -> GOALS_HOME
            "settings", "options" -> SETTINGS_HOME
            "database" -> DATABASE_HOME
            "day" -> DAY_HOME
            else -> error("Unknown screen '$screen'")
        }
    }
}
