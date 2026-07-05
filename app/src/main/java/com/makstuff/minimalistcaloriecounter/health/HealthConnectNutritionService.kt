package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealFood
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

internal class HealthConnectNutritionService(
    private val context: Context,
    private val client: HealthConnectClient,
) {
    suspend fun readNutritionMeals(date: LocalDate): HealthConnectNutritionReadResult {
        return try {
            HealthConnectNutritionReadResult.Success(
                readNutritionRecords(date).map { it.toHealthConnectNutritionMeal() }
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectNutritionReadResult.Failed(e.message ?: "Unknown Health Connect read error")
        }
    }

    suspend fun insertQuickMealNutrition(payloads: List<QuickImportHealthPayload>): QuickImportHealthWriteResult {
        return try {
            val existingSignatures = payloads.map { it.dateTime.toLocalDate() }
                .distinct()
                .flatMap { date -> readNutritionRecords(date).map { it.toNutritionSignature() } }
            val candidates = pendingNutritionPayloads(payloads, existingSignatures)
            if (candidates.isNotEmpty()) {
                client.insertRecords(candidates.map { it.toNutritionRecord() })
            }
            QuickImportHealthWriteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            QuickImportHealthWriteResult.Failed(e.message ?: "Unknown Health Connect error")
        }
    }

    suspend fun insertNutritionServings(payloads: List<QuickImportHealthPayload>): QuickImportHealthWriteResult {
        return try {
            if (payloads.isNotEmpty()) {
                client.insertRecords(payloads.map { it.toNutritionRecord() })
            }
            QuickImportHealthWriteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            QuickImportHealthWriteResult.Failed(e.message ?: "Unknown Health Connect error")
        }
    }

    suspend fun replaceNutritionServings(
        recordIds: List<String>,
        payloads: List<QuickImportHealthPayload>,
    ): QuickImportHealthWriteResult {
        return try {
            nutritionReplaceSteps(recordIds, payloads.size).forEach { step ->
                when (step) {
                    HealthConnectReplaceStep.InsertReplacement -> {
                        client.insertRecords(payloads.map { it.toNutritionRecord() })
                    }
                    HealthConnectReplaceStep.DeleteOriginal -> {
                        client.deleteRecords(
                            NutritionRecord::class,
                            recordIdsList = recordIds,
                            clientRecordIdsList = emptyList(),
                        )
                    }
                }
            }
            QuickImportHealthWriteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            QuickImportHealthWriteResult.Failed(e.message ?: "Unknown Health Connect edit error")
        }
    }

    suspend fun deleteNutritionMeal(recordId: String): HealthConnectDeleteResult {
        return deleteNutritionMeals(listOf(recordId))
    }

    suspend fun deleteNutritionMeals(recordIds: List<String>): HealthConnectDeleteResult {
        if (recordIds.isEmpty()) return HealthConnectDeleteResult.Success
        return try {
            client.deleteRecords(
                NutritionRecord::class,
                recordIdsList = recordIds,
                clientRecordIdsList = emptyList(),
            )
            HealthConnectDeleteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectDeleteResult.Failed(e.message ?: "Unknown Health Connect delete error")
        }
    }

    suspend fun previewNutritionRecordsInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: HealthConnectCleanupMode,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HealthConnectCleanupPreviewResult {
        return try {
            val categories = cleanupCategoriesInRange(startDate, endDate, mode, onProgress)
            HealthConnectCleanupPreviewResult.Success(categories.toCleanupPreview())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectCleanupPreviewResult.Failed(e.message ?: "Unknown Health Connect cleanup preview error")
        }
    }

    suspend fun writeHistoricalMealFoods(
        foods: List<HistoricalMealFood>,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        return try {
            val existingFingerprints = mutableSetOf<String>()
            val existingClientRecordIds = mutableSetOf<String>()
            foods.map { it.dateTime.toLocalDate() }.distinct().forEach { date ->
                readNutritionRecords(date).forEach { record ->
                    val signature = record.toNutritionSignature()
                    signature.clientRecordId?.let(existingClientRecordIds::add)
                    existingFingerprints += signature.fingerprint
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
            val chunks = candidates.chunked(25)
            chunks.forEachIndexed { index, chunk ->
                retryHealthConnectQuotaSensitive {
                    client.insertRecords(chunk.map { it.toHealthPayload().toNutritionRecord() })
                }
                written += chunk.size
                withContext(Dispatchers.Main) {
                    onProgress((index + 1).toFloat() / chunks.size, written, total)
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
        return try {
            var deleted = 0
            dates.forEach { date ->
                val recordIds = readNutritionRecords(date)
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
        mode: HealthConnectCleanupMode,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        return try {
            val idsToDelete = mutableListOf<String>()
            val dates = cleanupDates(startDate, endDate)
            dates.forEachIndexed { index, date ->
                idsToDelete += readNutritionRecords(date)
                    .filter { it.cleanupCategory().matches(mode) }
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

    private suspend fun cleanupCategoriesInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: HealthConnectCleanupMode,
        onProgress: (Float?, Int, Int) -> Unit,
    ): List<HealthConnectCleanupCategory> {
        val dates = cleanupDates(startDate, endDate)
        val categories = mutableListOf<HealthConnectCleanupCategory>()
        dates.forEachIndexed { index, date ->
            categories += readNutritionRecords(date)
                .map { it.cleanupCategory() }
                .filter { it.matches(mode) }
            withContext(Dispatchers.Main) {
                onProgress((index + 1).toFloat() / dates.size, index + 1, dates.size)
            }
        }
        return categories
    }

    private fun cleanupDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val firstDate = minOf(startDate, endDate)
        val lastDate = maxOf(startDate, endDate)
        return generateSequence(firstDate) { current ->
            current.plusDays(1).takeIf { it <= lastDate }
        }.toList()
    }

    private suspend fun readNutritionRecords(date: LocalDate): List<NutritionRecord> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return readAllHealthConnectPages { pageToken ->
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                    dataOriginFilter = setOf(DataOrigin(context.packageName)),
                    ascendingOrder = true,
                    pageSize = HEALTH_CONNECT_PAGE_SIZE,
                    pageToken = pageToken,
                )
            )
            HealthConnectPage(response.records, response.pageToken)
        }
    }

}
