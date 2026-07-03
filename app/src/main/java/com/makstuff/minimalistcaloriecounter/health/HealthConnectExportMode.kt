package com.makstuff.minimalistcaloriecounter.health

enum class HealthConnectExportMode(val label: String, val filenameToken: String) {
    NutritionOnly("Nutrition only", "nutrition"),
    NutritionAndGoals("Nutrition and goals", "nutrition_goals"),
    Full("Full Health Connect export", "full"),
}
