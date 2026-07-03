package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime
import java.util.Base64

object QuickImportOutboxCsv {
    private val legacyHeader: List<String> = listOf(
        "id",
        "createdAt",
        "intendedDateTime",
        "mealType",
        "sourceTextHash",
        "mealSummary",
        "foodCount",
        "state",
        "attemptCount",
        "lastAttemptAt",
        "lastErrorMessage",
    )

    val header: List<String> = listOf(
        "id",
        "createdAt",
        "intendedDateTime",
        "mealType",
        "sourceTextHash",
        "mealSummary",
        "foodCount",
        "state",
        "attemptCount",
        "lastAttemptAt",
        "lastErrorMessage",
        "healthPayloads",
    )

    fun toRows(items: List<QuickImportOutboxItem>): List<List<String>> {
        return listOf(header) + items.map { item ->
            listOf(
                item.id,
                item.createdAt.toString(),
                item.intendedDateTime.toString(),
                item.mealType.name,
                item.sourceTextHash,
                item.mealSummary,
                item.foodCount.toString(),
                item.state.name,
                item.attemptCount.toString(),
                item.lastAttemptAt?.toString().orEmpty(),
                item.lastErrorMessage.orEmpty(),
                encodePayloads(item.healthPayloads),
            )
        }
    }

    fun fromRows(rows: List<List<String>>): List<QuickImportOutboxItem> {
        if (rows.isEmpty()) return emptyList()
        val headerRow = rows.first()
        require(headerRow == header || headerRow == legacyHeader) { "Add Meal outbox: wrong CSV header." }
        return rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row ->
                require(row.size == headerRow.size) { "Add Meal outbox: wrong number of CSV fields." }
                QuickImportOutboxItem(
                    id = row[0],
                    createdAt = LocalDateTime.parse(row[1]),
                    intendedDateTime = LocalDateTime.parse(row[2]),
                    mealType = QuickImportMealType.valueOf(row[3]),
                    sourceTextHash = row[4],
                    mealSummary = row[5],
                    foodCount = row[6].toInt(),
                    state = QuickImportOutboxState.valueOf(row[7]),
                    attemptCount = row[8].toInt(),
                    lastAttemptAt = row[9].takeIf { it.isNotBlank() }?.let(LocalDateTime::parse),
                    lastErrorMessage = row[10].takeIf { it.isNotBlank() },
                    healthPayloads = row.getOrNull(11)?.let(::decodePayloads).orEmpty(),
                )
            }
    }

    // Payloads are nested inside one CSV field, so each value is Base64 encoded before row joining.
    private fun encodePayloads(payloads: List<QuickImportHealthPayload>): String {
        return payloads.joinToString("\n") { payload ->
            listOf(
                payload.dateTime.toString(),
                payload.mealType.toString(),
                payload.energy.toString(),
                payload.energyFromFat.toString(),
                payload.totalCarbohydrate.toString(),
                payload.sugar.toString(),
                payload.protein.toString(),
                payload.totalFat.toString(),
                payload.saturatedFat.toString(),
                payload.dietaryFiber.toString(),
                payload.name,
                payload.clientRecordId.orEmpty(),
            ).joinToString(",") { encodeField(it) }
        }
    }

    private fun decodePayloads(value: String): List<QuickImportHealthPayload> {
        if (value.isBlank()) return emptyList()
        return value.lines().filter { it.isNotBlank() }.map { line ->
            val fields = line.split(",").map(::decodeField)
            require(fields.size == 12) { "Add Meal outbox: wrong number of Health Connect payload fields." }
            QuickImportHealthPayload(
                dateTime = LocalDateTime.parse(fields[0]),
                mealType = fields[1].toInt(),
                energy = fields[2].toDouble(),
                energyFromFat = fields[3].toDouble(),
                totalCarbohydrate = fields[4].toDouble(),
                sugar = fields[5].toDouble(),
                protein = fields[6].toDouble(),
                totalFat = fields[7].toDouble(),
                saturatedFat = fields[8].toDouble(),
                dietaryFiber = fields[9].toDouble(),
                name = fields[10],
                clientRecordId = fields[11].takeIf { it.isNotBlank() },
            )
        }
    }

    private fun encodeField(value: String): String {
        return Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decodeField(value: String): String {
        return String(Base64.getDecoder().decode(value), Charsets.UTF_8)
    }
}
