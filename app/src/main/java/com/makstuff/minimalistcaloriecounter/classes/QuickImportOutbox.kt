package com.makstuff.minimalistcaloriecounter.classes

import java.security.MessageDigest
import java.time.LocalDateTime

enum class QuickImportOutboxState {
    PendingHealthConnect,
    Synced,
    FailedHealthConnect,
    Retrying,
}

data class QuickImportOutboxItem(
    val id: String,
    val createdAt: LocalDateTime,
    val intendedDateTime: LocalDateTime,
    val mealType: QuickImportMealType,
    val sourceTextHash: String,
    val mealSummary: String,
    val foodCount: Int,
    val state: QuickImportOutboxState,
    val attemptCount: Int,
    val lastAttemptAt: LocalDateTime?,
    val lastErrorMessage: String?,
    val healthPayloads: List<QuickImportHealthPayload> = emptyList(),
) {
    val needsAttention: Boolean
        get() = state == QuickImportOutboxState.PendingHealthConnect ||
            state == QuickImportOutboxState.FailedHealthConnect ||
            state == QuickImportOutboxState.Retrying
}

object QuickImportOutbox {
    const val CLIENT_RECORD_PREFIX = "mcc-add-meal"

    fun buildItem(
        sourceText: String,
        meal: QuickImportMeal,
        intendedDateTime: LocalDateTime,
        mealType: QuickImportMealType,
        healthPayloads: List<QuickImportHealthPayload>,
        createdAt: LocalDateTime,
    ): QuickImportOutboxItem {
        val sourceTextHash = sha256(sourceText.trim())
        val id = sha256(
            listOf(
                sourceTextHash,
                intendedDateTime.toString(),
                mealType.name,
                meal.foods.joinToString("|") { food ->
                    listOf(food.amountText, food.name, food.nutrients.energy.toString()).joinToString(":")
                },
            ).joinToString("||")
        ).take(32)
        return QuickImportOutboxItem(
            id = id,
            createdAt = createdAt,
            intendedDateTime = intendedDateTime,
            mealType = mealType,
            sourceTextHash = sourceTextHash,
            mealSummary = mealSummary(meal),
            foodCount = meal.foods.size,
            state = QuickImportOutboxState.PendingHealthConnect,
            attemptCount = 0,
            lastAttemptAt = null,
            lastErrorMessage = null,
            healthPayloads = withClientRecordIds(healthPayloads, id),
        )
    }

    fun withClientRecordIds(
        payloads: List<QuickImportHealthPayload>,
        item: QuickImportOutboxItem,
    ): List<QuickImportHealthPayload> {
        return withClientRecordIds(payloads, item.id)
    }

    private fun withClientRecordIds(
        payloads: List<QuickImportHealthPayload>,
        itemId: String,
    ): List<QuickImportHealthPayload> = payloads.mapIndexed { index, payload ->
        payload.copy(clientRecordId = clientRecordId(itemId, index))
    }

    fun markAttempting(item: QuickImportOutboxItem, attemptedAt: LocalDateTime): QuickImportOutboxItem {
        return item.copy(
            state = QuickImportOutboxState.Retrying,
            attemptCount = item.attemptCount + 1,
            lastAttemptAt = attemptedAt,
            lastErrorMessage = null,
        )
    }

    fun markResult(item: QuickImportOutboxItem, result: QuickImportHealthWriteResult): QuickImportOutboxItem {
        return when (result) {
            QuickImportHealthWriteResult.Success -> item.copy(
                state = QuickImportOutboxState.Synced,
                lastErrorMessage = null,
            )
            QuickImportHealthWriteResult.HealthConnectUnavailable -> item.copy(
                state = QuickImportOutboxState.FailedHealthConnect,
                lastErrorMessage = "Health Connect is unavailable.",
            )
            QuickImportHealthWriteResult.PermissionsMissing -> item.copy(
                state = QuickImportOutboxState.FailedHealthConnect,
                lastErrorMessage = "Health Connect permissions are missing.",
            )
            is QuickImportHealthWriteResult.Failed -> item.copy(
                state = QuickImportOutboxState.FailedHealthConnect,
                lastErrorMessage = result.message,
            )
        }
    }

    fun upsert(items: List<QuickImportOutboxItem>, item: QuickImportOutboxItem): List<QuickImportOutboxItem> {
        val index = items.indexOfFirst { it.id == item.id }
        return if (index == -1) {
            items + item
        } else {
            items.toMutableList().also { it[index] = item }
        }
    }

    private fun clientRecordId(itemId: String, index: Int): String = "$CLIENT_RECORD_PREFIX-$itemId-$index"

    private fun mealSummary(meal: QuickImportMeal): String {
        return "${meal.foods.size} foods, ${meal.totals.energy.toInt()} kcal"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
