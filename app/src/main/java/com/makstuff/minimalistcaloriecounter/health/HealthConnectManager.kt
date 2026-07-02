package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealFood
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import androidx.health.connect.client.records.metadata.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
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
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    private val writePermissions = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    private val readNutritionPermissions = setOf(
        HealthPermission.getReadPermission(NutritionRecord::class),
    )

    suspend fun hasAllPermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(permissions)
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun hasWritePermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(writePermissions)
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun hasReadNutritionPermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(readNutritionPermissions)
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

    suspend fun readNutritionMeals(date: LocalDate): HealthConnectNutritionReadResult {
        if (!isSdkAvailable()) return HealthConnectNutritionReadResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectNutritionReadResult.HealthConnectUnavailable

        return try {
            if (!hasReadNutritionPermissions()) return HealthConnectNutritionReadResult.PermissionsMissing

            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                    dataOriginFilter = setOf(DataOrigin(context.packageName)),
                    ascendingOrder = true,
                )
            )

            HealthConnectNutritionReadResult.Success(
                response.records.map { it.toHealthConnectNutritionMeal() }
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectNutritionReadResult.Failed(e.message ?: "Unknown Health Connect read error")
        }
    }

    suspend fun insertQuickMealNutrition(payloads: List<QuickImportHealthPayload>): QuickImportHealthWriteResult {
        if (payloads.isEmpty()) return QuickImportHealthWriteResult.Success
        if (!isSdkAvailable()) return QuickImportHealthWriteResult.HealthConnectUnavailable
        val client = getClient() ?: return QuickImportHealthWriteResult.HealthConnectUnavailable

        return try {
            if (!hasWritePermissions()) return QuickImportHealthWriteResult.PermissionsMissing

            payloads.forEach { payload ->
                Log.i(
                    "MCCHealthConnect",
                    "Writing quick food nutrition: name=${payload.name}, energy=${payload.energy} kcal, energyFromFat=${payload.energyFromFat} kcal, carbs=${payload.totalCarbohydrate} g, protein=${payload.protein} g, fat=${payload.totalFat} g"
                )
            }
            client.insertRecords(payloads.map { payload ->
                val startTime = payload.dateTime.atZone(ZoneId.systemDefault()).toInstant()
                val endTime = payload.dateTime.plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant()
                NutritionRecord(
                        startTime = startTime,
                        startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime),
                        endTime = endTime,
                        endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime),
                        energy = Energy.kilocalories(payload.energy),
                        energyFromFat = Energy.kilocalories(payload.energyFromFat),
                        totalCarbohydrate = Mass.grams(payload.totalCarbohydrate),
                        sugar = Mass.grams(payload.sugar),
                        protein = Mass.grams(payload.protein),
                        totalFat = Mass.grams(payload.totalFat),
                        saturatedFat = Mass.grams(payload.saturatedFat),
                        dietaryFiber = Mass.grams(payload.dietaryFiber),
                        mealType = payload.mealType,
                        name = payload.name,
                        metadata = payload.clientRecordId?.let { Metadata.manualEntry(it, 1L) } ?: Metadata.manualEntry(),
                )
            })
            Log.i("MCCHealthConnect", "Quick food nutrition write succeeded: ${payloads.size} records")
            QuickImportHealthWriteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            QuickImportHealthWriteResult.Failed(e.message ?: "Unknown Health Connect error")
        }
    }

    suspend fun deleteNutritionMeal(recordId: String): HealthConnectDeleteResult {
        if (!isSdkAvailable()) return HealthConnectDeleteResult.HealthConnectUnavailable
        val client = getClient() ?: return HealthConnectDeleteResult.HealthConnectUnavailable

        return try {
            if (!hasWritePermissions()) return HealthConnectDeleteResult.PermissionsMissing
            client.deleteRecords(
                NutritionRecord::class,
                recordIdsList = listOf(recordId),
                clientRecordIdsList = emptyList(),
            )
            HealthConnectDeleteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectDeleteResult.Failed(e.message ?: "Unknown Health Connect delete error")
        }
    }

    private fun NutritionRecord.toHealthConnectNutritionMeal(): HealthConnectNutritionMeal {
        return HealthConnectNutritionMeal(
            recordId = metadata.id,
            clientRecordId = metadata.clientRecordId,
            startTime = LocalDateTime.ofInstant(startTime, ZoneId.systemDefault()),
            endTime = LocalDateTime.ofInstant(endTime, ZoneId.systemDefault()),
            name = name ?: "Nutrition record",
            energy = energy?.inKilocalories ?: 0.0,
            energyFromFat = energyFromFat?.inKilocalories,
            totalCarbohydrate = totalCarbohydrate?.inGrams ?: 0.0,
            sugar = sugar?.inGrams ?: 0.0,
            protein = protein?.inGrams ?: 0.0,
            totalFat = totalFat?.inGrams ?: 0.0,
            saturatedFat = saturatedFat?.inGrams ?: 0.0,
            dietaryFiber = dietaryFiber?.inGrams ?: 0.0,
            mealType = mealType,
        )
    }

    suspend fun writeHistoricalMealFoods(
        foods: List<HistoricalMealFood>,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        if (foods.isEmpty()) return HistoricalMealHealthConnectResult.Success(written = 0, skippedDuplicates = 0)
        if (!isSdkAvailable()) return HistoricalMealHealthConnectResult.HealthConnectUnavailable
        val client = getClient() ?: return HistoricalMealHealthConnectResult.HealthConnectUnavailable

        return try {
            if (!hasWritePermissions() || !hasReadNutritionPermissions()) {
                return HistoricalMealHealthConnectResult.PermissionsMissing
            }

            val existingFingerprints = mutableSetOf<String>()
            val existingClientRecordIds = mutableSetOf<String>()
            foods.map { it.dateTime.toLocalDate() }.distinct().forEach { date ->
                readNutritionRecords(client, date).forEach { record ->
                    record.metadata.clientRecordId?.let(existingClientRecordIds::add)
                    existingFingerprint(record)?.let(existingFingerprints::add)
                }
            }

            val candidates = foods.filter { food ->
                food.clientRecordId !in existingClientRecordIds && food.fingerprint !in existingFingerprints
            }
            val total = candidates.size
            if (total == 0) {
                withContext(Dispatchers.Main) { onProgress(1.0f, 0, 0) }
                return HistoricalMealHealthConnectResult.Success(written = 0, skippedDuplicates = foods.size)
            }

            var written = 0
            candidates.chunked(25).forEachIndexed { index, chunk ->
                var success = false
                var attempts = 0
                while (!success && attempts < 5) {
                    try {
                        kotlinx.coroutines.yield()
                        client.insertRecords(chunk.map { it.toHealthPayload().toNutritionRecord() })
                        success = true
                    } catch (e: Throwable) {
                        attempts++
                        val isQuotaError = e.message?.contains("quota exceeded", ignoreCase = true) == true
                        if (isQuotaError && attempts < 5) {
                            kotlinx.coroutines.delay((3000L * attempts).milliseconds)
                        } else {
                            throw e
                        }
                    }
                }
                written += chunk.size
                withContext(Dispatchers.Main) {
                    onProgress((index + 1).toFloat() / candidates.chunked(25).size, written, total)
                }
                kotlinx.coroutines.delay(500.milliseconds)
            }

            HistoricalMealHealthConnectResult.Success(
                written = written,
                skippedDuplicates = foods.size - written,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HistoricalMealHealthConnectResult.Failed(e.message ?: "Unknown historical meal import error")
        }
    }

    suspend fun cleanupHistoricalMealRecords(dates: Set<LocalDate>): HistoricalMealHealthConnectResult {
        if (dates.isEmpty()) return HistoricalMealHealthConnectResult.Success(written = 0, skippedDuplicates = 0, deleted = 0)
        if (!isSdkAvailable()) return HistoricalMealHealthConnectResult.HealthConnectUnavailable
        val client = getClient() ?: return HistoricalMealHealthConnectResult.HealthConnectUnavailable

        return try {
            if (!hasWritePermissions() || !hasReadNutritionPermissions()) {
                return HistoricalMealHealthConnectResult.PermissionsMissing
            }

            var deleted = 0
            dates.forEach { date ->
                val records = readNutritionRecords(client, date)
                val recordIds = records
                    .filter { record ->
                        record.metadata.clientRecordId?.startsWith(HistoricalMealImporter.CLIENT_RECORD_ID_PREFIX) == true ||
                            (record.name == "Daily Total" && record.mealType == androidx.health.connect.client.records.MealType.MEAL_TYPE_UNKNOWN)
                    }
                    .map { it.metadata.id }
                    .filter { it.isNotBlank() }

                recordIds.chunked(100).forEach { ids ->
                    if (ids.isNotEmpty()) {
                        client.deleteRecords(
                            NutritionRecord::class,
                            recordIdsList = ids,
                            clientRecordIdsList = emptyList(),
                        )
                        deleted += ids.size
                    }
                }
            }

            HistoricalMealHealthConnectResult.Success(written = 0, skippedDuplicates = 0, deleted = deleted)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HistoricalMealHealthConnectResult.Failed(e.message ?: "Unknown historical meal cleanup error")
        }
    }

    suspend fun deleteNutritionRecordsInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        if (!isSdkAvailable()) return HistoricalMealHealthConnectResult.HealthConnectUnavailable
        val client = getClient() ?: return HistoricalMealHealthConnectResult.HealthConnectUnavailable

        return try {
            if (!hasWritePermissions() || !hasReadNutritionPermissions()) {
                return HistoricalMealHealthConnectResult.PermissionsMissing
            }
            val firstDate = minOf(startDate, endDate)
            val lastDate = maxOf(startDate, endDate)
            val dates = generateSequence(firstDate) { current ->
                current.plusDays(1).takeIf { it <= lastDate }
            }.toList()

            val idsToDelete = mutableListOf<String>()
            dates.forEachIndexed { index, date ->
                idsToDelete += readNutritionRecords(client, date)
                    .map { it.metadata.id }
                    .filter { it.isNotBlank() }
                withContext(Dispatchers.Main) {
                    onProgress((index + 1).toFloat() / dates.size, index + 1, dates.size)
                }
            }

            var deleted = 0
            idsToDelete.chunked(100).forEach { ids ->
                client.deleteRecords(
                    NutritionRecord::class,
                    recordIdsList = ids,
                    clientRecordIdsList = emptyList(),
                )
                deleted += ids.size
            }

            HistoricalMealHealthConnectResult.Success(written = 0, skippedDuplicates = 0, deleted = deleted)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HistoricalMealHealthConnectResult.Failed(e.message ?: "Unknown Health Connect nutrition cleanup error")
        }
    }

    private suspend fun readNutritionRecords(client: HealthConnectClient, date: LocalDate): List<NutritionRecord> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return client.readRecords(
            ReadRecordsRequest(
                recordType = NutritionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                dataOriginFilter = setOf(DataOrigin(context.packageName)),
                ascendingOrder = true,
            )
        ).records
    }

    private fun QuickImportHealthPayload.toNutritionRecord(): NutritionRecord {
        val startTime = dateTime.atZone(ZoneId.systemDefault()).toInstant()
        val endTime = dateTime.plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant()
        return NutritionRecord(
            startTime = startTime,
            startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime),
            endTime = endTime,
            endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime),
            energy = Energy.kilocalories(energy),
            energyFromFat = Energy.kilocalories(energyFromFat),
            totalCarbohydrate = Mass.grams(totalCarbohydrate),
            sugar = Mass.grams(sugar),
            protein = Mass.grams(protein),
            totalFat = Mass.grams(totalFat),
            saturatedFat = Mass.grams(saturatedFat),
            dietaryFiber = Mass.grams(dietaryFiber),
            mealType = mealType,
            name = name,
            metadata = clientRecordId?.let { Metadata.manualEntry(it, 1L) } ?: Metadata.manualEntry(),
        )
    }

    private fun existingFingerprint(record: NutritionRecord): String? {
        val dateTime = LocalDateTime.ofInstant(record.startTime, ZoneId.systemDefault())
        val mealType = when (record.mealType) {
            androidx.health.connect.client.records.MealType.MEAL_TYPE_BREAKFAST -> QuickImportMealType.Breakfast
            androidx.health.connect.client.records.MealType.MEAL_TYPE_LUNCH -> QuickImportMealType.Lunch
            androidx.health.connect.client.records.MealType.MEAL_TYPE_DINNER -> QuickImportMealType.Dinner
            androidx.health.connect.client.records.MealType.MEAL_TYPE_SNACK -> QuickImportMealType.Snack
            else -> QuickImportMealType.inferFrom(dateTime)
        }
        return HistoricalMealImporter.fingerprint(
            dateTime = dateTime,
            mealType = mealType,
            name = record.name ?: "",
            nutrients = QuickImportNutrients(
                energy = record.energy?.inKilocalories ?: 0.0,
                carbohydrate = record.totalCarbohydrate?.inGrams ?: 0.0,
                sugar = record.sugar?.inGrams ?: 0.0,
                protein = record.protein?.inGrams ?: 0.0,
                fat = record.totalFat?.inGrams ?: 0.0,
                saturatedFat = record.saturatedFat?.inGrams ?: 0.0,
                fiber = record.dietaryFiber?.inGrams ?: 0.0,
            ),
        )
    }

    suspend fun syncSingleEntry(date: LocalDate, weight: Double, nutrients: Nutrients) {
        val client = getClient() ?: return
        try {
            if (!hasWritePermissions()) return

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
            if (!hasWritePermissions()) return
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
            if (!hasWritePermissions()) {
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
