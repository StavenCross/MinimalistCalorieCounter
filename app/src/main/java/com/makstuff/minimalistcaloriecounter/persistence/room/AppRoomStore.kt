package com.makstuff.minimalistcaloriecounter.persistence.room

import android.content.Context
import androidx.room.Room
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem

class AppRoomStore(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        DATABASE_NAME,
    ).build()

    suspend fun readGoals(): Goals? {
        val dao = database.goalDao()
        val profile = dao.profile() ?: return null
        return GoalRoomMapper.fromSeed(
            GoalRoomSeed(
                profile = profile,
                targets = dao.targets(),
                history = dao.history(),
                recommendation = dao.recommendation(),
            )
        )
    }

    suspend fun writeGoals(goals: Goals) {
        database.goalDao().replace(GoalRoomMapper.toSeed(goals))
    }

    suspend fun readQuickImportOutbox(): List<QuickImportOutboxItem> {
        val dao = database.quickImportOutboxDao()
        return dao.listItems().map { item ->
            QuickImportOutboxRoomMapper.fromSeed(
                QuickImportOutboxRoomSeed(item = item, payloads = dao.payloads(item.id))
            )
        }
    }

    suspend fun writeQuickImportOutboxItem(item: QuickImportOutboxItem) {
        database.quickImportOutboxDao().upsert(QuickImportOutboxRoomMapper.toSeed(item))
    }

    suspend fun seedQuickImportOutbox(items: List<QuickImportOutboxItem>) {
        items.forEach { writeQuickImportOutboxItem(it) }
    }

    fun close() {
        database.close()
    }

    companion object {
        const val DATABASE_NAME = "mcc.db"
    }
}
