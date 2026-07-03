package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val preferenceKey: String,
    val preferenceValue: String,
    val updatedAt: LocalDateTime,
)

@Entity(tableName = "goal_profile")
data class GoalProfileEntity(
    @PrimaryKey val id: Int = 1,
    val birthday: LocalDate?,
    val sex: String?,
    val activityLevel: String,
    val weightLossTarget: String,
    val heightCm: Double?,
    val heightLocked: Boolean,
    val heightSource: String,
    val heightUpdatedAt: LocalDateTime?,
    val weightKg: Double?,
    val weightLocked: Boolean,
    val weightSource: String,
    val weightUpdatedAt: LocalDateTime?,
    val bodyFatPercent: Double?,
    val bodyFatLocked: Boolean,
    val bodyFatSource: String,
    val bodyFatUpdatedAt: LocalDateTime?,
    val leanMassKg: Double?,
    val leanMassLocked: Boolean,
    val leanMassSource: String,
    val leanMassUpdatedAt: LocalDateTime?,
)

@Entity(tableName = "goal_targets")
data class GoalTargetEntity(
    @PrimaryKey val macro: String,
    val value: Double?,
    val locked: Boolean,
)

@Entity(tableName = "goal_history", indices = [Index("effectiveDate")])
data class GoalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveDate: LocalDate,
    val source: String,
    val calories: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val fiber: Double?,
    val generatedDate: LocalDate?,
    val bmr: Double?,
    val tdee: Double?,
    val weightKg: Double?,
    val bodyFatPercent: Double?,
    val leanMassKg: Double?,
    val activityLevel: String?,
    val weightLossTarget: String?,
    val applied: Boolean,
)

@Entity(tableName = "goal_recommendation")
data class GoalRecommendationEntity(
    @PrimaryKey val id: Int = 1,
    val generatedDate: LocalDate,
    val bmr: Double,
    val tdee: Double,
    val warning: String?,
    val calories: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val fiber: Double?,
)

@Entity(tableName = "quick_import_outbox")
data class QuickImportOutboxEntity(
    @PrimaryKey val id: String,
    val createdAt: LocalDateTime,
    val intendedDateTime: LocalDateTime,
    val mealType: String,
    val sourceTextHash: String,
    val mealSummary: String,
    val foodCount: Int,
    val state: String,
    val attemptCount: Int,
    val lastAttemptAt: LocalDateTime?,
    val lastErrorMessage: String?,
)

@Entity(
    tableName = "quick_import_outbox_payloads",
    primaryKeys = ["outboxId", "payloadIndex"],
    foreignKeys = [
        ForeignKey(
            entity = QuickImportOutboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["outboxId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("outboxId")],
)
data class QuickImportOutboxPayloadEntity(
    val outboxId: String,
    val payloadIndex: Int,
    val dateTime: LocalDateTime,
    val mealType: Int,
    val energy: Double,
    val energyFromFat: Double,
    val totalCarbohydrate: Double,
    val sugar: Double,
    val protein: Double,
    val totalFat: Double,
    val saturatedFat: Double,
    val dietaryFiber: Double,
    val name: String,
    val clientRecordId: String?,
)

@Entity(tableName = "local_meal_backups", indices = [Index("loggedDate"), Index("clientRecordId")])
data class LocalMealBackupEntity(
    @PrimaryKey val id: String,
    val loggedDate: LocalDate,
    val loggedAt: LocalDateTime,
    val mealType: String,
    val foodName: String,
    val amountText: String?,
    val grams: Double?,
    val calories: Double,
    val carbs: Double,
    val protein: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val saturatedFat: Double,
    val clientRecordId: String?,
    val createdAt: LocalDateTime,
)

@Entity(tableName = "import_export_jobs", indices = [Index("startedAt")])
data class ImportExportJobEntity(
    @PrimaryKey val id: String,
    val type: String,
    val mode: String,
    val state: String,
    val startedAt: LocalDateTime,
    val finishedAt: LocalDateTime?,
    val dateStart: LocalDate?,
    val dateEnd: LocalDate?,
    val outputPath: String?,
    val recordCount: Int,
    val errorMessage: String?,
)
