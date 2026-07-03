package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime

object QuickImportOutboxCsv {
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
            )
        }
    }

    fun fromRows(rows: List<List<String>>): List<QuickImportOutboxItem> {
        if (rows.isEmpty()) return emptyList()
        require(rows.first() == header) { "Add Meal outbox: wrong CSV header." }
        return rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row ->
                require(row.size == header.size) { "Add Meal outbox: wrong number of CSV fields." }
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
                )
            }
    }
}

