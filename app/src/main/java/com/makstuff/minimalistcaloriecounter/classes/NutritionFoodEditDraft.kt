package com.makstuff.minimalistcaloriecounter.classes

data class NutritionFoodEditDraft(
    val name: String,
    val energy: Double?,
    val totalCarbohydrate: Double?,
    val protein: Double?,
    val totalFat: Double?,
    val dietaryFiber: Double?,
    val sugar: Double?,
    val saturatedFat: Double?,
) {
    val isComplete: Boolean
        get() = name.isNotBlank() &&
            listOf(energy, totalCarbohydrate, protein, totalFat, dietaryFiber, sugar, saturatedFat).all { it != null && it >= 0.0 }
}
