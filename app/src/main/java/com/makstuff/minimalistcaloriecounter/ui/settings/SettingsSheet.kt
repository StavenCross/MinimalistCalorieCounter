package com.makstuff.minimalistcaloriecounter.ui.settings

enum class SettingsSheet(val key: String) {
    HealthData("health_data"),
    ImportTools("import_tools"),
    Theme("theme"),
    Language("language"),
    Maintenance("maintenance"),
    Support("support");

    companion object {
        fun fromKey(key: String?): SettingsSheet? {
            if (key.isNullOrBlank()) return null
            return entries.firstOrNull { it.key == key }
                ?: entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
        }
    }
}
