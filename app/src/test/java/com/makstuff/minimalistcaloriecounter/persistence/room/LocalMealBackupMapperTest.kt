package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LocalMealBackupMapperTest {
    @Test
    fun mapsFoodsToStableLocalMealBackups() {
        val meal = QuickImportMeal(
            foods = listOf(
                QuickImportFood(
                    amountText = "100g",
                    name = "rice",
                    grams = 100.0,
                    nutrients = QuickImportNutrients(130.0, 28.0, 0.1, 2.7, 0.3, 0.1, 0.4),
                ),
                QuickImportFood(
                    amountText = "150g",
                    name = "chicken",
                    grams = 150.0,
                    nutrients = QuickImportNutrients(248.0, 0.0, 0.0, 46.0, 5.4, 1.6, 0.0),
                ),
            ),
            totals = QuickImportNutrients(378.0, 28.0, 0.1, 48.7, 5.7, 1.7, 0.4),
        )
        val dateTime = LocalDateTime.of(2026, 7, 3, 12, 0)

        val entities = LocalMealBackupMapper.toEntities(
            meal = meal,
            dateTime = dateTime,
            mealType = QuickImportMealType.Lunch,
            clientRecordIds = listOf("record-0", "record-1"),
            createdAt = dateTime.plusMinutes(1),
        )
        val repeated = LocalMealBackupMapper.toEntities(meal, dateTime, QuickImportMealType.Lunch, createdAt = dateTime.plusMinutes(2))

        assertEquals(2, entities.size)
        assertEquals("rice", entities[0].foodName)
        assertEquals("record-0", entities[0].clientRecordId)
        assertEquals(dateTime.plusSeconds(1), entities[1].loggedAt)
        assertEquals(entities.map { it.id }, repeated.map { it.id })
        assertNotEquals(entities[0].id, entities[1].id)
    }
}
