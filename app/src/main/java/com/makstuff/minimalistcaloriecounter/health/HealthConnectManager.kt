package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import com.makstuff.minimalistcaloriecounter.classes.HealthConnectGoalSnapshot
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealFood
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class HealthConnectManager(private val context: Context) {
    
    private fun getClient(): HealthConnectClient? {
        return try {
            if (isSdkAvailable()) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun isSdkAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (_: Throwable) {
            false
        }
    }

    private val writePermissions = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    private val exportReadPermissions = allReadPermissions

    private val readNutritionPermissions = setOf(
        HealthPermission.getReadPermission(NutritionRecord::class),
    )

    private val readGoalProfilePermissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
    )

    val permissions = writePermissions + readNutritionPermissions + readGoalProfilePermissions

    val exportPermissions = exportReadPermissions

    private fun permissionSetFor(scope: HealthConnectPermissionScope): Set<String> {
        return when (scope) {
            HealthConnectPermissionScope.AllAppFeatures -> permissions
            HealthConnectPermissionScope.WriteArchiveEntries -> writePermissions
            HealthConnectPermissionScope.ReadNutrition -> readNutritionPermissions
            HealthConnectPermissionScope.ReadGoalProfile -> readGoalProfilePermissions
            HealthConnectPermissionScope.ExportReadableData -> exportReadPermissions
            HealthConnectPermissionScope.MutateNutritionRecords -> writePermissions + readNutritionPermissions
        }
    }

    private suspend fun hasPermissions(scope: HealthConnectPermissionScope): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(permissionSetFor(scope))
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        return hasPermissions(HealthConnectPermissionScope.AllAppFeatures)
    }

    suspend fun hasArchiveSyncPermissions(): Boolean {
        return hasPermissions(HealthConnectPermissionScope.WriteArchiveEntries)
    }

    private suspend fun hasWritePermissions(): Boolean {
        return hasPermissions(HealthConnectPermissionScope.WriteArchiveEntries)
    }

    private suspend fun hasReadNutritionPermissions(): Boolean {
        return hasPermissions(HealthConnectPermissionScope.ReadNutrition)
    }

    private suspend fun hasReadGoalProfilePermissions(): Boolean {
        return hasPermissions(HealthConnectPermissionScope.ReadGoalProfile)
    }

    suspend fun hasExportReadPermissions(): Boolean {
        return hasPermissions(HealthConnectPermissionScope.ExportReadableData)
    }

    private suspend fun hasNutritionMutationPermissions(): Boolean {
        return hasPermissions(HealthConnectPermissionScope.MutateNutritionRecords)
    }

    suspend fun exportHealthConnectCsv(
        startDate: LocalDate,
        endDate: LocalDate,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HealthConnectExportResult {
        if (!isSdkAvailable()) return HealthConnectExportResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectExportResult.HealthConnectUnavailable

        return try {
            if (!hasExportReadPermissions()) return HealthConnectExportResult.PermissionsMissing
            HealthConnectExporter(context, client).exportCsv(startDate, endDate, onProgress)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectExportResult.Failed(e.message ?: "Unknown Health Connect export error")
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
        if (!isSdkAvailable()) return QuickImportHealthWriteResult.HealthConnectUnavailable
        val client = getClient() ?: return QuickImportHealthWriteResult.HealthConnectUnavailable

        if (!hasNutritionMutationPermissions()) return QuickImportHealthWriteResult.PermissionsMissing
        return HealthConnectNutritionService(context, client).insertQuickMealNutrition(payloads)
    }

    suspend fun deleteNutritionMeal(recordId: String): HealthConnectDeleteResult {
        if (!isSdkAvailable()) return HealthConnectDeleteResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectDeleteResult.HealthConnectUnavailable

        if (!hasWritePermissions()) return HealthConnectDeleteResult.PermissionsMissing
        return HealthConnectNutritionService(context, client).deleteNutritionMeal(recordId)
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
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        if (!isSdkAvailable()) return HistoricalMealHealthConnectResult.HealthConnectUnavailable
        val client = getClient() ?: return HistoricalMealHealthConnectResult.HealthConnectUnavailable

        if (!hasNutritionMutationPermissions()) {
            return HistoricalMealHealthConnectResult.PermissionsMissing
        }
        return HealthConnectNutritionService(context, client).deleteNutritionRecordsInRange(startDate, endDate, onProgress)
    }

    suspend fun syncSingleEntry(date: LocalDate, weight: Double, nutrients: Nutrients) {
        val client = getClient() ?: return
        if (!hasWritePermissions()) return
        HealthConnectArchiveSyncService(context, client).syncSingleEntry(date, weight, nutrients)
    }

    suspend fun deleteSingleEntry(date: LocalDate) {
        val client = getClient() ?: return
        if (!hasWritePermissions()) return
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
            if (!hasWritePermissions()) {
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

sealed class HealthConnectGoalProfileReadResult {
    data class Success(val snapshot: HealthConnectGoalSnapshot) : HealthConnectGoalProfileReadResult()
    data object HealthConnectUnavailable : HealthConnectGoalProfileReadResult()
    data object PermissionsMissing : HealthConnectGoalProfileReadResult()
    data class Failed(val message: String) : HealthConnectGoalProfileReadResult()
}

sealed class HealthConnectExportResult {
    data class Success(val displayPath: String, val records: Int) : HealthConnectExportResult()
    data object HealthConnectUnavailable : HealthConnectExportResult()
    data object PermissionsMissing : HealthConnectExportResult()
    data class Failed(val message: String) : HealthConnectExportResult()
}
