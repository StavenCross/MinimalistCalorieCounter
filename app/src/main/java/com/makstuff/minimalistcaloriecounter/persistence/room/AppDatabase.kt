package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AppPreferenceEntity::class,
        GoalProfileEntity::class,
        GoalTargetEntity::class,
        GoalHistoryEntity::class,
        GoalRecommendationEntity::class,
        QuickImportOutboxEntity::class,
        QuickImportOutboxPayloadEntity::class,
        LocalMealBackupEntity::class,
        ImportExportJobEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun goalDao(): GoalDao
    abstract fun quickImportOutboxDao(): QuickImportOutboxDao
    abstract fun localMealBackupDao(): LocalMealBackupDao
    abstract fun importExportJobDao(): ImportExportJobDao
}
