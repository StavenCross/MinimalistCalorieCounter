package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.classes.GoalsCsv
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxCsv
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CsvRoomSeedPlannerTest {
    @Test
    fun buildsSeedFromExistingCsvShapes() {
        val plan = CsvRoomSeedPlanner.build(
            goalRows = GoalsCsv.defaultRows(),
            outboxRows = listOf(QuickImportOutboxCsv.header),
        )

        assertEquals(LocalDate::class, plan.goals.profile.birthday?.javaClass ?: LocalDate::class)
        assertEquals(5, plan.goals.targets.size)
        assertEquals(emptyList<QuickImportOutboxRoomSeed>(), plan.quickImportOutbox)
    }

    @Test
    fun rejectsCorruptOutboxCsvBeforeRoomWrites() {
        assertThrows(IllegalArgumentException::class.java) {
            CsvRoomSeedPlanner.build(
                goalRows = GoalsCsv.defaultRows(),
                outboxRows = listOf(listOf("wrong")),
            )
        }
    }
}
