package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppPreferenceDao {
    @Query("SELECT * FROM app_preferences WHERE preferenceKey = :key")
    suspend fun get(key: String): AppPreferenceEntity?

    @Query("SELECT * FROM app_preferences ORDER BY preferenceKey")
    suspend fun list(): List<AppPreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: AppPreferenceEntity)
}
