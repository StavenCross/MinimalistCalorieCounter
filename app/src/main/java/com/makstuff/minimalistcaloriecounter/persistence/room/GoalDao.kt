package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal_profile WHERE id = 1")
    suspend fun profile(): GoalProfileEntity?

    @Query("SELECT * FROM goal_targets ORDER BY macro")
    suspend fun targets(): List<GoalTargetEntity>

    @Query("SELECT * FROM goal_history ORDER BY effectiveDate")
    suspend fun history(): List<GoalHistoryEntity>

    @Query("SELECT * FROM goal_recommendation WHERE id = 1")
    suspend fun recommendation(): GoalRecommendationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: GoalProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTargets(targets: List<GoalTargetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: List<GoalHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecommendation(recommendation: GoalRecommendationEntity)

    @Query("DELETE FROM goal_targets")
    suspend fun clearTargets()

    @Query("DELETE FROM goal_history")
    suspend fun clearHistory()

    @Query("DELETE FROM goal_recommendation")
    suspend fun clearRecommendation()

    @Transaction
    suspend fun replace(seed: GoalRoomSeed) {
        upsertProfile(seed.profile)
        clearTargets()
        upsertTargets(seed.targets)
        clearHistory()
        upsertHistory(seed.history)
        clearRecommendation()
        seed.recommendation?.let { upsertRecommendation(it) }
    }
}
