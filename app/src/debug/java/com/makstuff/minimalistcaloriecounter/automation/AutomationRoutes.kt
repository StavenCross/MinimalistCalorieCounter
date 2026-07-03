package com.makstuff.minimalistcaloriecounter.automation

internal fun automationRouteFor(screen: String): String {
    return when (screen.lowercase().replace("-", "_")) {
        "quick_add", "quick_import" -> "quick_import"
        "meals", "health_connect", "health_connect_nutrition" -> "health_connect_nutrition"
        "goals", "goals_home" -> "goals_home"
        "settings", "options" -> "settings_home"
        "database" -> "database_home"
        "day" -> "day_home"
        else -> error("Unknown screen '$screen'")
    }
}
