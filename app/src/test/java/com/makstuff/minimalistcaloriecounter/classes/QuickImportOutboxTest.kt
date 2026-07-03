package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickImportOutboxTest {
    @Test
    fun buildsStableItemForSameMealIntent() {
        val meal = sampleMeal()
        val dateTime = LocalDateTime.of(2026, 7, 3, 12, 0)

        val first = QuickImportOutbox.buildItem(
            sourceText = SAMPLE_TEXT,
            meal = meal,
            intendedDateTime = dateTime,
            mealType = QuickImportMealType.Lunch,
            createdAt = LocalDateTime.of(2026, 7, 3, 12, 1),
        )
        val second = QuickImportOutbox.buildItem(
            sourceText = SAMPLE_TEXT,
            meal = meal,
            intendedDateTime = dateTime,
            mealType = QuickImportMealType.Lunch,
            createdAt = LocalDateTime.of(2026, 7, 3, 12, 5),
        )

        assertEquals(first.id, second.id)
        assertEquals(QuickImportOutboxState.PendingHealthConnect, first.state)
        assertEquals("1 foods, 389 kcal", first.mealSummary)
    }

    @Test
    fun assignsDeterministicClientRecordIds() {
        val item = QuickImportOutbox.buildItem(
            sourceText = SAMPLE_TEXT,
            meal = sampleMeal(),
            intendedDateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
            mealType = QuickImportMealType.Lunch,
            createdAt = LocalDateTime.of(2026, 7, 3, 12, 1),
        )

        val payloads = QuickImportOutbox.withClientRecordIds(
            payloads = QuickImportMapper.toHealthPayloads(
                meal = sampleMeal(),
                dateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
                mealType = QuickImportMealType.Lunch,
            ),
            item = item,
        )

        assertEquals("mcc-add-meal-${item.id}-0", payloads.single().clientRecordId)
    }

    @Test
    fun tracksAttemptAndFailureState() {
        val item = QuickImportOutbox.buildItem(
            sourceText = SAMPLE_TEXT,
            meal = sampleMeal(),
            intendedDateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
            mealType = QuickImportMealType.Lunch,
            createdAt = LocalDateTime.of(2026, 7, 3, 12, 1),
        )

        val attempting = QuickImportOutbox.markAttempting(item, LocalDateTime.of(2026, 7, 3, 12, 2))
        val failed = QuickImportOutbox.markResult(attempting, QuickImportHealthWriteResult.Failed("network down"))

        assertEquals(1, attempting.attemptCount)
        assertEquals(QuickImportOutboxState.Retrying, attempting.state)
        assertEquals(QuickImportOutboxState.FailedHealthConnect, failed.state)
        assertEquals("network down", failed.lastErrorMessage)
        assertTrue(failed.needsAttention)
    }

    @Test
    fun upsertReplacesExistingItemById() {
        val item = QuickImportOutbox.buildItem(
            sourceText = SAMPLE_TEXT,
            meal = sampleMeal(),
            intendedDateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
            mealType = QuickImportMealType.Lunch,
            createdAt = LocalDateTime.of(2026, 7, 3, 12, 1),
        )
        val synced = QuickImportOutbox.markResult(item, QuickImportHealthWriteResult.Success)

        val items = QuickImportOutbox.upsert(listOf(item), synced)

        assertEquals(1, items.size)
        assertEquals(QuickImportOutboxState.Synced, items.single().state)
    }

    private fun sampleMeal(): QuickImportMeal = QuickImportParser.parse(SAMPLE_TEXT)

    private companion object {
        const val SAMPLE_TEXT =
            "100g test oats; Calories 389, Fat 6.9g, Sat Fat 1.2g, Trans Fat 0g, Cholesterol 0mg, Sodium 2mg, Carbs 66.3g, Fiber 10.6g, Sugar 0.9g, Added Sugar 0g, Protein 16.9g. Meal totals; Calories 389, Fat 6.9g, Sat Fat 1.2g, Trans Fat 0g, Cholesterol 0mg, Sodium 2mg, Carbs 66.3g, Fiber 10.6g, Sugar 0.9g, Added Sugar 0g, Protein 16.9g."
    }
}

