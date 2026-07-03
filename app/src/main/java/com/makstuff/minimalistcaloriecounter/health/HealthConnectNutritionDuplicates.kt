package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import java.time.LocalDateTime
import java.time.ZoneId

internal data class HealthConnectNutritionSignature(
    val clientRecordId: String?,
    val fingerprint: String,
)

internal fun NutritionRecord.toNutritionSignature(zoneId: ZoneId = ZoneId.systemDefault()): HealthConnectNutritionSignature {
    return HealthConnectNutritionSignature(
        clientRecordId = metadata.clientRecordId,
        fingerprint = nutritionFingerprint(zoneId),
    )
}

internal fun QuickImportHealthPayload.toNutritionSignature(): HealthConnectNutritionSignature {
    return HealthConnectNutritionSignature(
        clientRecordId = clientRecordId,
        fingerprint = HistoricalMealImporter.fingerprint(
            dateTime = dateTime,
            mealType = mealType.toQuickImportMealType(dateTime),
            name = name,
            nutrients = QuickImportNutrients(
                energy = energy,
                carbohydrate = totalCarbohydrate,
                sugar = sugar,
                protein = protein,
                fat = totalFat,
                saturatedFat = saturatedFat,
                fiber = dietaryFiber,
            ),
        ),
    )
}

internal fun pendingNutritionPayloads(
    payloads: List<QuickImportHealthPayload>,
    existingSignatures: Collection<HealthConnectNutritionSignature>,
): List<QuickImportHealthPayload> {
    val existingIds = existingSignatures.mapNotNull { it.clientRecordId }.toSet()
    val existingFingerprints = existingSignatures.map { it.fingerprint }.toSet()
    return payloads.filter { payload ->
        val signature = payload.toNutritionSignature()
        signature.clientRecordId !in existingIds && signature.fingerprint !in existingFingerprints
    }
}

internal fun NutritionRecord.existingHistoricalMealFingerprint(zoneId: ZoneId = ZoneId.systemDefault()): String {
    return nutritionFingerprint(zoneId)
}

private fun NutritionRecord.nutritionFingerprint(zoneId: ZoneId): String {
    val dateTime = LocalDateTime.ofInstant(startTime, zoneId)
    return HistoricalMealImporter.fingerprint(
        dateTime = dateTime,
        mealType = mealType.toQuickImportMealType(dateTime),
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

private fun Int.toQuickImportMealType(dateTime: LocalDateTime): QuickImportMealType {
    return when (this) {
        MealType.MEAL_TYPE_BREAKFAST -> QuickImportMealType.Breakfast
        MealType.MEAL_TYPE_LUNCH -> QuickImportMealType.Lunch
        MealType.MEAL_TYPE_DINNER -> QuickImportMealType.Dinner
        MealType.MEAL_TYPE_SNACK -> QuickImportMealType.Snack
        else -> QuickImportMealType.inferFrom(dateTime)
    }
}
