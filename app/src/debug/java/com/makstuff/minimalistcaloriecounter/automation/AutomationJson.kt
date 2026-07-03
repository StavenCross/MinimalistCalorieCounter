package com.makstuff.minimalistcaloriecounter.automation

import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem
import com.makstuff.minimalistcaloriecounter.classes.QuickImportResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal fun JSONObject.requireString(name: String): String {
    require(has(name) && !isNull(name)) { "Missing '$name'" }
    return getString(name)
}

internal fun JSONObject.optDoubleOrNull(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return optDouble(name)
}

internal fun Goals.toJson(): JSONObject = goalsJson(this)

internal fun goalsJson(goals: Goals): JSONObject = JSONObject()
    .put("profile", JSONObject()
        .put("birthday", goals.profile.birthday?.toString())
        .put("sex", goals.profile.sex?.name)
        .put("activityLevel", goals.profile.activityLevel.name)
        .put("weightLossTarget", goals.profile.weightLossTarget.name)
        .put("heightCm", goals.profile.heightCm.value)
        .put("heightLocked", goals.profile.heightCm.locked)
        .put("weightKg", goals.profile.weightKg.value)
        .put("weightLocked", goals.profile.weightKg.locked)
        .put("bodyFatPercent", goals.profile.bodyFatPercent.value)
        .put("bodyFatLocked", goals.profile.bodyFatPercent.locked)
        .put("leanMassKg", goals.profile.leanMassKg.value)
        .put("leanMassLocked", goals.profile.leanMassKg.locked))
    .put("currentTargets", goals.currentTargets.toJson())
    .put("activeTargets", goals.activeTargetsFor(LocalDate.now()).toJson())
    .put("recommendation", goals.recommendation?.let { recommendation ->
        JSONObject()
            .put("generatedDate", recommendation.generatedDate.toString())
            .put("bmr", recommendation.bmr)
            .put("tdee", recommendation.tdee)
            .put("warning", recommendation.warning)
            .put("targets", recommendation.targets.toJson())
    })
    .put("message", goals.message)

internal fun MacroTargets.toJson(): JSONObject = JSONObject()
    .put("calories", calories)
    .put("protein", protein)
    .put("carbs", carbs)
    .put("fat", fat)
    .put("fiber", fiber)
    .put("lockedMacros", JSONArray(lockedMacros.map { it.name }))

internal fun QuickImportMeal.toJson(): JSONObject = JSONObject()
    .put("foods", JSONArray(foods.map { food ->
        JSONObject()
            .put("amountText", food.amountText)
            .put("name", food.name)
            .put("grams", food.grams)
            .put("nutrients", food.nutrients.toJson())
    }))
    .put("totals", totals.toJson())

internal fun QuickImportNutrients.toJson(): JSONObject = JSONObject()
    .put("energy", energy)
    .put("carbohydrate", carbohydrate)
    .put("appCarbohydrate", appCarbohydrate)
    .put("sugar", sugar)
    .put("protein", protein)
    .put("fat", fat)
    .put("saturatedFat", saturatedFat)
    .put("fiber", fiber)

internal fun QuickImportResult.toJson(): JSONObject = JSONObject()
    .put("databaseEntriesAdded", databaseEntriesAdded)
    .put("dayFoodsAdded", dayFoodsAdded)
    .put("healthWriteResult", healthWriteResult.toJson())

internal fun QuickImportOutboxItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("createdAt", createdAt.toString())
    .put("intendedDateTime", intendedDateTime.toString())
    .put("mealType", mealType.label)
    .put("mealSummary", mealSummary)
    .put("foodCount", foodCount)
    .put("state", state.name)
    .put("attemptCount", attemptCount)
    .put("lastAttemptAt", lastAttemptAt?.toString())
    .put("lastErrorMessage", lastErrorMessage)
    .put("needsAttention", needsAttention)

internal fun QuickImportHealthWriteResult?.toJson(): JSONObject? {
    return when (this) {
        null -> null
        QuickImportHealthWriteResult.Success -> JSONObject().put("status", "success")
        QuickImportHealthWriteResult.HealthConnectUnavailable -> JSONObject().put("status", "health_connect_unavailable")
        QuickImportHealthWriteResult.PermissionsMissing -> JSONObject().put("status", "permissions_missing")
        is QuickImportHealthWriteResult.Failed -> JSONObject().put("status", "failed").put("message", message)
    }
}

internal fun HealthConnectNutritionMeal.toJson(): JSONObject = JSONObject()
    .put("recordId", recordId)
    .put("clientRecordId", clientRecordId)
    .put("startTime", startTime.toString())
    .put("endTime", endTime.toString())
    .put("name", name)
    .put("energy", energy)
    .put("energyFromFat", energyFromFat)
    .put("totalCarbohydrate", totalCarbohydrate)
    .put("sugar", sugar)
    .put("protein", protein)
    .put("totalFat", totalFat)
    .put("saturatedFat", saturatedFat)
    .put("dietaryFiber", dietaryFiber)
    .put("mealType", mealType)
