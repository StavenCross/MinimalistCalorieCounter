package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import java.time.LocalDateTime
import java.time.ZoneId

internal fun QuickImportHealthPayload.toNutritionRecord(zoneId: ZoneId = ZoneId.systemDefault()): NutritionRecord {
    val startTime = dateTime.atZone(zoneId).toInstant()
    val endTime = dateTime.plusMinutes(1).atZone(zoneId).toInstant()
    return NutritionRecord(
        startTime = startTime,
        startZoneOffset = zoneId.rules.getOffset(startTime),
        endTime = endTime,
        endZoneOffset = zoneId.rules.getOffset(endTime),
        energy = Energy.kilocalories(energy),
        energyFromFat = Energy.kilocalories(energyFromFat),
        totalCarbohydrate = Mass.grams(totalCarbohydrate),
        sugar = Mass.grams(sugar),
        protein = Mass.grams(protein),
        totalFat = Mass.grams(totalFat),
        saturatedFat = Mass.grams(saturatedFat),
        dietaryFiber = Mass.grams(dietaryFiber),
        mealType = mealType,
        name = name,
        metadata = clientRecordId?.let { Metadata.manualEntry(it, 1L) } ?: Metadata.manualEntry(),
    )
}

internal fun NutritionRecord.toHealthConnectNutritionMeal(zoneId: ZoneId = ZoneId.systemDefault()): HealthConnectNutritionMeal {
    return HealthConnectNutritionMeal(
        recordId = metadata.id,
        clientRecordId = metadata.clientRecordId,
        startTime = LocalDateTime.ofInstant(startTime, zoneId),
        endTime = LocalDateTime.ofInstant(endTime, zoneId),
        name = name ?: "Nutrition record",
        energy = energy?.inKilocalories ?: 0.0,
        energyFromFat = energyFromFat?.inKilocalories,
        totalCarbohydrate = totalCarbohydrate?.inGrams ?: 0.0,
        sugar = sugar?.inGrams ?: 0.0,
        protein = protein?.inGrams ?: 0.0,
        totalFat = totalFat?.inGrams ?: 0.0,
        saturatedFat = saturatedFat?.inGrams ?: 0.0,
        dietaryFiber = dietaryFiber?.inGrams ?: 0.0,
        mealType = mealType,
    )
}

internal fun NutritionRecord.existingHistoricalMealFingerprint(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val dateTime = LocalDateTime.ofInstant(startTime, zoneId)
    val mealType = when (mealType) {
        MealType.MEAL_TYPE_BREAKFAST -> QuickImportMealType.Breakfast
        MealType.MEAL_TYPE_LUNCH -> QuickImportMealType.Lunch
        MealType.MEAL_TYPE_DINNER -> QuickImportMealType.Dinner
        MealType.MEAL_TYPE_SNACK -> QuickImportMealType.Snack
        else -> QuickImportMealType.inferFrom(dateTime)
    }
    return HistoricalMealImporter.fingerprint(
        dateTime = dateTime,
        mealType = mealType,
        name = name ?: "",
        nutrients = QuickImportNutrients(
            energy = energy?.inKilocalories ?: 0.0,
            carbohydrate = totalCarbohydrate?.inGrams ?: 0.0,
            sugar = sugar?.inGrams ?: 0.0,
            protein = protein?.inGrams ?: 0.0,
            fat = totalFat?.inGrams ?: 0.0,
            saturatedFat = saturatedFat?.inGrams ?: 0.0,
            fiber = dietaryFiber?.inGrams ?: 0.0,
        ),
    )
}
