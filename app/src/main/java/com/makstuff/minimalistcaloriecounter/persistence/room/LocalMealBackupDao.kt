package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate

@Dao
interface LocalMealBackupDao {
    @Query("SELECT * FROM local_meal_backups WHERE loggedDate = :date ORDER BY loggedAt, mealType, foodName")
    suspend fun mealsForDate(date: LocalDate): List<LocalMealBackupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meals: List<LocalMealBackupEntity>)
}
