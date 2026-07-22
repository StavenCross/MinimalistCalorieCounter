package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QuickImportMealTypeSelectionTest {
    @Test
    fun `manual meal type selection preserves an early timestamp`() {
        val selectedTime = LocalDateTime.of(2026, 7, 14, 4, 0)
        val selection = quickImportMealTypeSelection(selectedTime, QuickImportMealType.Dinner)

        assertEquals(selectedTime, selection.dateTime)
        assertEquals(QuickImportMealType.Dinner, selection.mealType)
        assertFalse(selection.snackOverride)
    }

    @Test
    fun `manual lunch selection preserves a pre-lunch timestamp`() {
        val selectedTime = LocalDateTime.of(2026, 7, 14, 6, 0)
        val selection = quickImportMealTypeSelection(selectedTime, QuickImportMealType.Lunch)

        assertEquals(selectedTime, selection.dateTime)
        assertEquals(QuickImportMealType.Lunch, selection.mealType)
    }
}
