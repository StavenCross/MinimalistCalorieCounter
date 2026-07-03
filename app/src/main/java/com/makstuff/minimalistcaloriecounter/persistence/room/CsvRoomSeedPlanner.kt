package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.classes.GoalsCsv
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxCsv

data class CsvRoomSeedPlan(
    val goals: GoalRoomSeed,
    val quickImportOutbox: List<QuickImportOutboxRoomSeed>,
)

object CsvRoomSeedPlanner {
    fun build(
        goalRows: List<List<String>>,
        outboxRows: List<List<String>>,
    ): CsvRoomSeedPlan {
        return CsvRoomSeedPlan(
            goals = GoalRoomMapper.toSeed(GoalsCsv.fromRows(goalRows)),
            quickImportOutbox = QuickImportOutboxCsv.fromRows(outboxRows).map(QuickImportOutboxRoomMapper::toSeed),
        )
    }
}
