package com.makstuff.minimalistcaloriecounter.health

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectReplaceOrderTest {
    @Test
    fun `nutrition replacement inserts new records before deleting originals`() {
        assertEquals(
            listOf(HealthConnectReplaceStep.InsertReplacement, HealthConnectReplaceStep.DeleteOriginal),
            nutritionReplaceSteps(recordIds = listOf("old-record"), replacementCount = 1),
        )
    }

    @Test
    fun `nutrition replacement skips unnecessary operations`() {
        assertEquals(
            listOf(HealthConnectReplaceStep.DeleteOriginal),
            nutritionReplaceSteps(recordIds = listOf("old-record"), replacementCount = 0),
        )
        assertEquals(
            listOf(HealthConnectReplaceStep.InsertReplacement),
            nutritionReplaceSteps(recordIds = emptyList(), replacementCount = 1),
        )
    }
}
