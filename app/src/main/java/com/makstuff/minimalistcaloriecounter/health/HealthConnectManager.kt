package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
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
    private fun getClient(): HealthConnectClient? = try {
        if (isSdkAvailable()) HealthConnectClient.getOrCreate(context) else null
    } catch (_: Throwable) {
        null
    }

    fun isSdkAvailable(): Boolean = try {
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    } catch (_: Throwable) {
        false
    }

    private val writeNutritionPermissions = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class),
    )

    private val writeArchivePermissions = writeNutritionPermissions + setOf(
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    private val writeGoalProfilePermissions = setOf(
        HealthPermission.getWritePermission(HeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    private val exportReadPermissions = allReadPermissions

    private val readNutritionPermissions = setOf(HealthPermission.getReadPermission(NutritionRecord::class))

    private val readGoalProfilePermissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(BodyWaterMassRecord::class),
        HealthPermission.getReadPermission(BoneMassRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
    )

    val permissions = writeNutritionPermissions + readNutritionPermissions + readGoalProfilePermissions

    fun exportPermissionsFor(mode: HealthConnectExportMode): Set<String> = mode.recordTypes()
        .map { HealthPermission.getReadPermission(it) }
        .toSet() + HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    private fun permissionSetFor(scope: HealthConnectPermissionScope): Set<String> = when (scope) {
        HealthConnectPermissionScope.AllAppFeatures -> permissions
        HealthConnectPermissionScope.WriteArchiveEntries -> writeArchivePermissions
        HealthConnectPermissionScope.ReadNutrition -> readNutritionPermissions
        HealthConnectPermissionScope.ReadGoalProfile -> readGoalProfilePermissions
        HealthConnectPermissionScope.WriteGoalProfile -> writeGoalProfilePermissions
        HealthConnectPermissionScope.ExportReadableData -> exportReadPermissions
        HealthConnectPermissionScope.MutateNutritionRecords -> writeNutritionPermissions + readNutritionPermissions
    }

    private suspend fun hasPermissions(scope: HealthConnectPermissionScope): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(permissionSetFor(scope))
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun hasAllPermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.AllAppFeatures)

    suspend fun hasArchiveSyncPermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.WriteArchiveEntries)

    private suspend fun hasArchiveWritePermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.WriteArchiveEntries)

    private suspend fun hasReadNutritionPermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.ReadNutrition)

    private suspend fun hasReadGoalProfilePermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.ReadGoalProfile)

    private suspend fun hasWriteGoalProfilePermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.WriteGoalProfile)

    suspend fun hasExportReadPermissions(mode: HealthConnectExportMode = HealthConnectExportMode.Full): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(exportPermissionsFor(mode))
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun hasNutritionMutationPermissions(): Boolean = hasPermissions(HealthConnectPermissionScope.MutateNutritionRecords)

    suspend fun exportHealthConnectCsv(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: HealthConnectExportMode,
        redacted: Boolean,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HealthConnectExportResult {
        if (!isSdkAvailable()) return HealthConnectExportResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectExportResult.HealthConnectUnavailable

        return try {
            if (!hasExportReadPermissions(mode)) return HealthConnectExportResult.PermissionsMissing
            HealthConnectExporter(context, client).exportCsv(startDate, endDate, mode, redacted, onProgress)
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
