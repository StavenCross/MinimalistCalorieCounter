package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord

internal fun Record.toExportRow(recordType: String): List<String> {
    val structured = structuredExportValues()
    return listOf(
        recordType,
        structured["startTime"].orEmpty(),
        structured["endTime"].orEmpty(),
        metadata.id,
        metadata.clientRecordId.orEmpty(),
        metadata.clientRecordVersion.toString(),
        metadata.dataOrigin.packageName,
        metadata.recordingMethod.toString(),
        metadata.lastModifiedTime.toString(),
        structured["name"].orEmpty(),
        structured["mealType"].orEmpty(),
        structured["energyKcal"].orEmpty(),
        structured["carbsG"].orEmpty(),
        structured["proteinG"].orEmpty(),
        structured["fatG"].orEmpty(),
        structured["fiberG"].orEmpty(),
        structured["weightKg"].orEmpty(),
        structured["heightCm"].orEmpty(),
        structured["bodyFatPercent"].orEmpty(),
        structured["leanMassKg"].orEmpty(),
        toString(),
    )
}

private fun Record.structuredExportValues(): Map<String, String> {
    val values = commonExportValues()
    when (this) {
        is NutritionRecord -> values += mapOf(
            "name" to name.orEmpty(),
            "mealType" to mealType.toString(),
            "energyKcal" to (energy?.inKilocalories?.toString() ?: ""),
            "carbsG" to (totalCarbohydrate?.inGrams?.toString() ?: ""),
            "proteinG" to (protein?.inGrams?.toString() ?: ""),
            "fatG" to (totalFat?.inGrams?.toString() ?: ""),
            "fiberG" to (dietaryFiber?.inGrams?.toString() ?: ""),
        )
        is WeightRecord -> values["weightKg"] = weight.inKilograms.toString()
        is HeightRecord -> values["heightCm"] = (height.inMeters * 100.0).toString()
        is BodyFatRecord -> values["bodyFatPercent"] = percentage.value.toString()
        is LeanBodyMassRecord -> values["leanMassKg"] = mass.inKilograms.toString()
    }
    return values
}

private fun Record.commonExportValues(): MutableMap<String, String> {
    val time = callNoArgGetter("getTime")
    val startTime = callNoArgGetter("getStartTime") ?: time
    val endTime = callNoArgGetter("getEndTime") ?: time
    return mutableMapOf<String, String>().apply {
        startTime?.let { put("startTime", it) }
        endTime?.let { put("endTime", it) }
    }
}

private fun Record.callNoArgGetter(name: String): String? {
    return runCatching {
        javaClass.methods
            .firstOrNull { method -> method.name == name && method.parameterCount == 0 }
            ?.invoke(this)
            ?.toString()
    }.getOrNull()
}
