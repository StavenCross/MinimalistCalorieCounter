package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxState

data class QuickImportOutboxRoomSeed(
    val item: QuickImportOutboxEntity,
    val payloads: List<QuickImportOutboxPayloadEntity>,
)

object QuickImportOutboxRoomMapper {
    fun toSeed(item: QuickImportOutboxItem): QuickImportOutboxRoomSeed {
        return QuickImportOutboxRoomSeed(
            item = item.toEntity(),
            payloads = item.healthPayloads.mapIndexed { index, payload -> payload.toEntity(item.id, index) },
        )
    }

    fun fromSeed(seed: QuickImportOutboxRoomSeed): QuickImportOutboxItem {
        val item = seed.item
        return QuickImportOutboxItem(
            id = item.id,
            createdAt = item.createdAt,
            intendedDateTime = item.intendedDateTime,
            mealType = QuickImportMealType.valueOf(item.mealType),
            sourceTextHash = item.sourceTextHash,
            mealSummary = item.mealSummary,
            foodCount = item.foodCount,
            state = QuickImportOutboxState.valueOf(item.state),
            attemptCount = item.attemptCount,
            lastAttemptAt = item.lastAttemptAt,
            lastErrorMessage = item.lastErrorMessage,
            healthPayloads = seed.payloads.sortedBy { it.payloadIndex }.map { it.toDomain() },
        )
    }
}

private fun QuickImportOutboxItem.toEntity(): QuickImportOutboxEntity = QuickImportOutboxEntity(
    id = id,
    createdAt = createdAt,
    intendedDateTime = intendedDateTime,
    mealType = mealType.name,
    sourceTextHash = sourceTextHash,
    mealSummary = mealSummary,
    foodCount = foodCount,
    state = state.name,
    attemptCount = attemptCount,
    lastAttemptAt = lastAttemptAt,
    lastErrorMessage = lastErrorMessage,
)

private fun QuickImportHealthPayload.toEntity(outboxId: String, index: Int): QuickImportOutboxPayloadEntity {
    return QuickImportOutboxPayloadEntity(
        outboxId = outboxId,
        payloadIndex = index,
        dateTime = dateTime,
        mealType = mealType,
        energy = energy,
        energyFromFat = energyFromFat,
        totalCarbohydrate = totalCarbohydrate,
        sugar = sugar,
        protein = protein,
        totalFat = totalFat,
        saturatedFat = saturatedFat,
        dietaryFiber = dietaryFiber,
        name = name,
        clientRecordId = clientRecordId,
    )
}

private fun QuickImportOutboxPayloadEntity.toDomain(): QuickImportHealthPayload = QuickImportHealthPayload(
    dateTime = dateTime,
    mealType = mealType,
    energy = energy,
    energyFromFat = energyFromFat,
    totalCarbohydrate = totalCarbohydrate,
    sugar = sugar,
    protein = protein,
    totalFat = totalFat,
    saturatedFat = saturatedFat,
    dietaryFiber = dietaryFiber,
    name = name,
    clientRecordId = clientRecordId,
)
