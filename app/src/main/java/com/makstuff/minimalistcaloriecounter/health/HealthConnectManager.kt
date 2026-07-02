package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

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

    val permissions = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    suspend fun hasAllPermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(permissions)
        } catch (_: Throwable) {
            false
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

    suspend fun insertQuickMealNutrition(payload: QuickImportHealthPayload): QuickImportHealthWriteResult {
        if (!isSdkAvailable()) return QuickImportHealthWriteResult.HealthConnectUnavailable
        val client = getClient() ?: return QuickImportHealthWriteResult.HealthConnectUnavailable

        return try {
            if (!hasAllPermissions()) return QuickImportHealthWriteResult.PermissionsMissing

            val startTime = payload.dateTime.atZone(ZoneId.systemDefault()).toInstant()
            val endTime = payload.dateTime.plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant()

            client.insertRecords(
                listOf(
                    NutritionRecord(
                        startTime = startTime,
                        startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime),
                        endTime = endTime,
                        endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime),
                        energy = Energy.kilocalories(payload.energy),
                        totalCarbohydrate = Mass.grams(payload.totalCarbohydrate),
                        sugar = Mass.grams(payload.sugar),
                        protein = Mass.grams(payload.protein),
                        totalFat = Mass.grams(payload.totalFat),
                        saturatedFat = Mass.grams(payload.saturatedFat),
                        dietaryFiber = Mass.grams(payload.dietaryFiber),
                        mealType = 0,
                        name = payload.name,
                        metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry(),
                    )
                )
            )
            QuickImportHealthWriteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            QuickImportHealthWriteResult.Failed(e.message ?: "Unknown Health Connect error")
        }
    }

    suspend fun syncSingleEntry(date: LocalDate, weight: Double, nutrients: Nutrients) {
        val client = getClient() ?: return
        try {
            if (!hasAllPermissions()) return

            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

            // Overwrite by deleting first
            client.deleteRecords(NutritionRecord::class, timeRange)
            client.deleteRecords(WeightRecord::class, timeRange)

            val nutritionRecords = mutableListOf<NutritionRecord>()
            val weightRecords = mutableListOf<WeightRecord>()

            nutritionRecords.add(
                NutritionRecord(
                    startTime = startOfDay,
                    startZoneOffset = ZoneId.systemDefault().rules.getOffset(startOfDay),
                    endTime = endOfDay,
                    endZoneOffset = ZoneId.systemDefault().rules.getOffset(endOfDay),
                    energy = Energy.kilocalories(nutrients.values[0]),
                    totalCarbohydrate = Mass.grams(nutrients.values[1] + nutrients.values[6]),
                    sugar = Mass.grams(nutrients.values[2]),
                    protein = Mass.grams(nutrients.values[3]),
                    totalFat = Mass.grams(nutrients.values[4]),
                    saturatedFat = Mass.grams(nutrients.values[5]),
                    dietaryFiber = Mass.grams(nutrients.values[6]),
                    mealType = 0,
                    name = "Daily Total",
                    metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry(),
                )
            )

            if (weight > 0) {
                weightRecords.add(
                    WeightRecord(
                        time = startOfDay,
                        zoneOffset = ZoneId.systemDefault().rules.getOffset(startOfDay),
                        weight = Mass.kilograms(weight),
                        metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry(),
                    )
                )
            }

            client.insertRecords(nutritionRecords)
            client.insertRecords(weightRecords)
        } catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Health Connect Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun deleteSingleEntry(date: LocalDate) {
        val client = getClient() ?: return
        try {
            if (!hasAllPermissions()) return
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

            client.deleteRecords(NutritionRecord::class, timeRange)
            client.deleteRecords(WeightRecord::class, timeRange)
        } catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Health Connect Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun syncArchive(
        archive: Archive,
        onProgress: (Float?, Int, Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val client = getClient() ?: return
        if (archive.entries.isEmpty()) return

        try {
            if (!hasAllPermissions()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.health_connect_permissions_missing), Toast.LENGTH_SHORT).show()
                }
                return
            }

            val totalEntries = archive.entries.size
            var currentEntriesDone = 0

            // Conservative chunking to satisfy strict API quotas
            val chunks = archive.entries.chunked(5)
            val totalChunks = chunks.size

            chunks.forEachIndexed { index, chunk ->
                var success = false
                var attempts = 0
                val maxAttempts = 5

                while (!success && attempts < maxAttempts) {
                    try {
                        kotlinx.coroutines.yield()

                        val nutritionRecords = mutableListOf<NutritionRecord>()
                        val weightRecords = mutableListOf<WeightRecord>()

                        chunk.forEach { (date, weight, nutrients) ->
                            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                            val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
                            val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

                            client.deleteRecords(NutritionRecord::class, timeRange)
                            client.deleteRecords(WeightRecord::class, timeRange)

                            nutritionRecords.add(
                                NutritionRecord(
                                    startTime = startOfDay,
                                    startZoneOffset = ZoneId.systemDefault().rules.getOffset(startOfDay),
                                    endTime = endOfDay,
                                    endZoneOffset = ZoneId.systemDefault().rules.getOffset(endOfDay),
                                    energy = Energy.kilocalories(nutrients.values[0]),
                                    totalCarbohydrate = Mass.grams(nutrients.values[1] + nutrients.values[6]),
                                    sugar = Mass.grams(nutrients.values[2]),
                                    protein = Mass.grams(nutrients.values[3]),
                                    totalFat = Mass.grams(nutrients.values[4]),
                                    saturatedFat = Mass.grams(nutrients.values[5]),
                                    dietaryFiber = Mass.grams(nutrients.values[6]),
                                    mealType = 0,
                                    name = "Daily Total",
                                    metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry(),
                                )
                            )

                            if (weight > 0) {
                                weightRecords.add(
                                    WeightRecord(
                                        time = startOfDay,
                                        zoneOffset = ZoneId.systemDefault().rules.getOffset(startOfDay),
                                        weight = Mass.kilograms(weight),
                                        metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry(),
                                    )
                                )
                            }
                        }

                        client.insertRecords(nutritionRecords)
                        client.insertRecords(weightRecords)
                        
                        success = true
                    } catch (e: Throwable) {
                        attempts++
                        val isQuotaError = e.message?.contains("quota exceeded", ignoreCase = true) == true
                        if (isQuotaError && attempts < maxAttempts) {
                            kotlinx.coroutines.delay((3000L * attempts).milliseconds)
                        } else {
                            throw e
                        }
                    }
                }

                currentEntriesDone += chunk.size
                withContext(Dispatchers.Main) {
                    onProgress((index + 1).toFloat() / totalChunks, currentEntriesDone, totalEntries)
                }

                kotlinx.coroutines.delay(1000.milliseconds)
            }

            withContext(Dispatchers.Main) {
                onProgress(1.0f, totalEntries, totalEntries)
                Toast.makeText(context, context.getString(R.string.toast_hc_full_archive_synced), Toast.LENGTH_SHORT).show()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Unknown Error")
            }
        }
    }
}
