package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealFood
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class HealthConnectManager(private val context: Context) {
    private fun getClient(): HealthConnectClient? = try {
        if (isSdkAvailable()) HealthConnectClient.getOrCreate(context) else null
    } catch (_: Throwable) {
        null
    }

    internal val applicationContext: Context
        get() = context

    internal fun getClientOrNull(): HealthConnectClient? = getClient()

    fun isSdkAvailable(): Boolean = try {
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    } catch (_: Throwable) {
        false
    }

    private val writeGoalProfilePermissions = healthConnectGoalProfileWritePermissions
    private val readGoalProfilePermissions = healthConnectGoalProfileReadPermissions

    val permissions = defaultHealthConnectPermissions

    private suspend fun hasPermissions(scope: HealthConnectPermissionScope): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(healthConnectPermissionsFor(scope))
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun hasCorePermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.CoreAppFeatures)

    suspend fun hasArchiveSyncPermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.WriteArchiveEntries)

    private suspend fun hasArchiveWritePermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.WriteArchiveEntries)

    private suspend fun hasReadNutritionPermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.ReadNutrition)

    private suspend fun hasReadGoalProfilePermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.ReadGoalProfile)

    private suspend fun hasWriteGoalProfilePermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.WriteGoalProfile)

    private suspend fun hasNutritionMutationPermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.MutateNutritionRecords)

    private suspend fun hasGoalWeightMutationPermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            val permissions = client.permissionController.getGrantedPermissions()
            permissions.containsAll(readGoalProfilePermissions + writeGoalProfilePermissions)
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun readGoalProfileSnapshot(): HealthConnectGoalProfileReadResult {
        if (!isSdkAvailable()) return HealthConnectGoalProfileReadResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectGoalProfileReadResult.HealthConnectUnavailable

        return try {
            if (!hasReadGoalProfilePermissions()) return HealthConnectGoalProfileReadResult.PermissionsMissing
            HealthConnectGoalProfileReader(client).readSnapshot()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectGoalProfileReadResult.Failed(e.message ?: "Unknown Health Connect profile read error")
        }
    }

    suspend fun writeGoalProfileMeasurements(heightCm: Double? = null, weightKg: Double? = null): HealthConnectGoalProfileWriteResult {
        if (heightCm == null && weightKg == null) return HealthConnectGoalProfileWriteResult.Success
        if (!isSdkAvailable()) return HealthConnectGoalProfileWriteResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectGoalProfileWriteResult.HealthConnectUnavailable
        if (!hasWriteGoalProfilePermissions()) return HealthConnectGoalProfileWriteResult.PermissionsMissing
        return HealthConnectGoalProfileWriter(client).writeManualMeasurements(heightCm = heightCm, weightKg = weightKg)
    }

    suspend fun hasAnyPermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun readNutritionMeals(date: LocalDate): HealthConnectNutritionReadResult {
        if (!isSdkAvailable()) return HealthConnectNutritionReadResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectNutritionReadResult.HealthConnectUnavailable

        if (!hasReadNutritionPermissions()) return HealthConnectNutritionReadResult.PermissionsMissing
        return HealthConnectNutritionService(context, client).readNutritionMeals(date)
    }

    suspend fun insertQuickMealNutrition(payloads: List<QuickImportHealthPayload>): QuickImportHealthWriteResult {
        if (payloads.isEmpty()) return QuickImportHealthWriteResult.Success
        return withNutritionMutationService { it.insertQuickMealNutrition(payloads) }
    }

    suspend fun insertNutritionServings(payloads: List<QuickImportHealthPayload>): QuickImportHealthWriteResult {
        if (payloads.isEmpty()) return QuickImportHealthWriteResult.Success
        return withNutritionMutationService { it.insertNutritionServings(payloads) }
    }

    suspend fun replaceNutritionServings(
        recordIds: List<String>,
        payloads: List<QuickImportHealthPayload>,
    ): QuickImportHealthWriteResult {
        if (recordIds.isEmpty() && payloads.isEmpty()) return QuickImportHealthWriteResult.Success
        return withNutritionMutationService { it.replaceNutritionServings(recordIds, payloads) }
    }

    private suspend fun withNutritionMutationService(
        action: suspend (HealthConnectNutritionService) -> QuickImportHealthWriteResult,
    ): QuickImportHealthWriteResult {
        if (!isSdkAvailable()) return QuickImportHealthWriteResult.HealthConnectUnavailable
        val client = getClient() ?: return QuickImportHealthWriteResult.HealthConnectUnavailable
        if (!hasNutritionMutationPermissions()) return QuickImportHealthWriteResult.PermissionsMissing
        return action(HealthConnectNutritionService(context, client))
    }

    suspend fun deleteNutritionMeal(recordId: String): HealthConnectDeleteResult = deleteNutritionMeals(listOf(recordId))

    suspend fun deleteNutritionMeals(recordIds: List<String>): HealthConnectDeleteResult {
        if (recordIds.isEmpty()) return HealthConnectDeleteResult.Success
        if (!isSdkAvailable()) return HealthConnectDeleteResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectDeleteResult.HealthConnectUnavailable
        if (!hasNutritionMutationPermissions()) return HealthConnectDeleteResult.PermissionsMissing
        return HealthConnectNutritionService(context, client).deleteNutritionMeals(recordIds)
    }

    suspend fun deleteGoalHistoryWeight(entry: GoalHistoryEntry): HealthConnectDeleteResult {
        val targetWeightKg = entry.weightKg ?: return HealthConnectDeleteResult.Success
        if (!isSdkAvailable()) return HealthConnectDeleteResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectDeleteResult.HealthConnectUnavailable
        if (!hasGoalWeightMutationPermissions()) return HealthConnectDeleteResult.PermissionsMissing
        return try {
            val start = entry.effectiveDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            val end = entry.effectiveDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            val records = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                )
            ).records
            val recordIds = records
                .filter { record ->
                    record.metadata.clientRecordId?.startsWith(GOAL_WEIGHT_CLIENT_RECORD_PREFIX) == true &&
                        kotlin.math.abs(record.weight.inKilograms - targetWeightKg) <= GOAL_WEIGHT_MATCH_KG_TOLERANCE
                }
                .map { it.metadata.id }
                .filter { it.isNotBlank() }
            if (recordIds.isNotEmpty()) {
                client.deleteRecords(
                    WeightRecord::class,
                    recordIdsList = recordIds,
                    clientRecordIdsList = emptyList(),
                )
            }
            HealthConnectDeleteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectDeleteResult.Failed(e.message ?: "Unknown Health Connect weight delete error")
        }
    }

    suspend fun writeHistoricalMealFoods(
        foods: List<HistoricalMealFood>,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        if (foods.isEmpty()) return HistoricalMealHealthConnectResult.Success(written = 0, skippedDuplicates = 0)
        if (!isSdkAvailable()) return HistoricalMealHealthConnectResult.HealthConnectUnavailable
        val client = getClient() ?: return HistoricalMealHealthConnectResult.HealthConnectUnavailable

        if (!hasNutritionMutationPermissions()) {
            return HistoricalMealHealthConnectResult.PermissionsMissing
        }
        return HealthConnectNutritionService(context, client).writeHistoricalMealFoods(foods, onProgress)
    }

    suspend fun cleanupHistoricalMealRecords(dates: Set<LocalDate>): HistoricalMealHealthConnectResult {
        if (dates.isEmpty()) return HistoricalMealHealthConnectResult.Success(written = 0, skippedDuplicates = 0, deleted = 0)
        if (!isSdkAvailable()) return HistoricalMealHealthConnectResult.HealthConnectUnavailable
        val client = getClient() ?: return HistoricalMealHealthConnectResult.HealthConnectUnavailable

        if (!hasNutritionMutationPermissions()) {
            return HistoricalMealHealthConnectResult.PermissionsMissing
        }
        return HealthConnectNutritionService(context, client).cleanupHistoricalMealRecords(dates)
    }

    suspend fun deleteNutritionRecordsInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: HealthConnectCleanupMode,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        if (!isSdkAvailable()) return HistoricalMealHealthConnectResult.HealthConnectUnavailable
        val client = getClient() ?: return HistoricalMealHealthConnectResult.HealthConnectUnavailable

        if (!hasNutritionMutationPermissions()) {
            return HistoricalMealHealthConnectResult.PermissionsMissing
        }
        return HealthConnectNutritionService(context, client).deleteNutritionRecordsInRange(startDate, endDate, mode, onProgress)
    }

    suspend fun previewNutritionRecordsInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: HealthConnectCleanupMode,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HealthConnectCleanupPreviewResult {
        if (!isSdkAvailable()) return HealthConnectCleanupPreviewResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectCleanupPreviewResult.HealthConnectUnavailable

        if (!hasNutritionMutationPermissions()) {
            return HealthConnectCleanupPreviewResult.PermissionsMissing
        }
        return HealthConnectNutritionService(context, client)
            .previewNutritionRecordsInRange(startDate, endDate, mode, onProgress)
    }

    suspend fun syncSingleEntry(date: LocalDate, weight: Double, nutrients: Nutrients) {
        val client = getClient() ?: return
        if (!hasArchiveWritePermissions()) return
        HealthConnectArchiveSyncService(context, client).syncSingleEntry(date, weight, nutrients)
    }

    suspend fun deleteSingleEntry(date: LocalDate) {
        val client = getClient() ?: return
        if (!hasArchiveWritePermissions()) return
        HealthConnectArchiveSyncService(context, client).deleteSingleEntry(date)
    }

    suspend fun syncArchive(
        archive: Archive,
        onProgress: (Float?, Int, Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val client = getClient() ?: return
        if (archive.entries.isEmpty()) return
        try {
            if (!hasArchiveWritePermissions()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.health_connect_permissions_missing), Toast.LENGTH_SHORT).show()
                }
                return
            }

            HealthConnectArchiveSyncService(context, client).syncArchive(archive, onProgress, onError)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Unknown Error")
            }
        }
    }
}
