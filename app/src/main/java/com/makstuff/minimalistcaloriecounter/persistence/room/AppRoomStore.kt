package com.makstuff.minimalistcaloriecounter.persistence.room

import android.content.Context
import androidx.room.Room
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem
import com.makstuff.minimalistcaloriecounter.persistence.AppOptionsFile
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import java.time.LocalDateTime

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

    suspend fun readOptions(): AppOptionsFile? {
        val values = database.appPreferenceDao().list().associate { it.preferenceKey to it.preferenceValue }
        if (values.isEmpty()) return null
        return AppOptionsFile(
            theme = values[KEY_THEME].toTheme(),
            healthConnectSyncEnabled = values[KEY_HEALTH_SYNC]?.toBooleanStrictOrNull(),
            healthConnectToastsEnabled = values[KEY_HEALTH_TOASTS]?.toBooleanStrictOrNull(),
        )
    }

    suspend fun writeOptions(theme: AppTheme, syncEnabled: Boolean, toastsEnabled: Boolean) {
        val dao = database.appPreferenceDao()
        val updatedAt = LocalDateTime.now()
        dao.upsert(AppPreferenceEntity(KEY_THEME, theme.toPreferenceValue(), updatedAt))
        dao.upsert(AppPreferenceEntity(KEY_HEALTH_SYNC, syncEnabled.toString(), updatedAt))
        dao.upsert(AppPreferenceEntity(KEY_HEALTH_TOASTS, toastsEnabled.toString(), updatedAt))
    }

    suspend fun writeLocalMealBackups(meals: List<LocalMealBackupEntity>) {
        database.localMealBackupDao().upsert(meals)
    }

    suspend fun localMealBackupsForDate(date: java.time.LocalDate): List<LocalMealBackupEntity> {
        return database.localMealBackupDao().mealsForDate(date)
    }

    fun close() {
        database.close()
    }

    companion object {
        const val DATABASE_NAME = "mcc.db"
        private const val KEY_THEME = "theme"
        private const val KEY_HEALTH_SYNC = "healthConnectSyncEnabled"
        private const val KEY_HEALTH_TOASTS = "healthConnectToastsEnabled"
    }
}

private fun String?.toTheme(): AppTheme {
    return when (this) {
        "light" -> AppTheme.MODE_DAY
        "auto" -> AppTheme.MODE_AUTO
        else -> AppTheme.MODE_NIGHT
    }
}

private fun AppTheme.toPreferenceValue(): String {
    return when (this) {
        AppTheme.MODE_NIGHT -> "dark"
        AppTheme.MODE_DAY -> "light"
        AppTheme.MODE_AUTO -> "auto"
    }
}
