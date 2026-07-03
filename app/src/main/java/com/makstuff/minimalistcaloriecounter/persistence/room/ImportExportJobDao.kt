package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImportExportJobDao {
    @Query("SELECT * FROM import_export_jobs ORDER BY startedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 20): List<ImportExportJobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: ImportExportJobEntity)
}
