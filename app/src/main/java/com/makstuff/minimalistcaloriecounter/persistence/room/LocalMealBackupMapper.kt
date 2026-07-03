package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import java.security.MessageDigest
import java.time.LocalDateTime

object LocalMealBackupMapper {
    fun toEntities(
        meal: QuickImportMeal,
        dateTime: LocalDateTime,
        mealType: QuickImportMealType,
        clientRecordIds: List<String?> = emptyList(),
        createdAt: LocalDateTime,
    ): List<LocalMealBackupEntity> {
        return meal.foods.mapIndexed { index, food ->
            val loggedAt = dateTime.plusSeconds(index.toLong())
            LocalMealBackupEntity(
                id = idFor(loggedAt, mealType, food.name, food.amountText, food.nutrients.energy),
                loggedDate = loggedAt.toLocalDate(),
                loggedAt = loggedAt,
                mealType = mealType.name,
                foodName = food.name,
                amountText = food.amountText.ifBlank { null },
                grams = food.grams,
                calories = food.nutrients.energy,
                carbs = food.nutrients.carbohydrate,
                protein = food.nutrients.protein,
                fat = food.nutrients.fat,
                fiber = food.nutrients.fiber,
                sugar = food.nutrients.sugar,
                saturatedFat = food.nutrients.saturatedFat,
                clientRecordId = clientRecordIds.getOrNull(index),
                createdAt = createdAt,
            )
        }
    }

    private fun idFor(
        loggedAt: LocalDateTime,
        mealType: QuickImportMealType,
        foodName: String,
        amountText: String,
        calories: Double,
    ): String {
        return sha256(listOf(loggedAt, mealType.name, foodName, amountText, calories).joinToString("||")).take(32)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
