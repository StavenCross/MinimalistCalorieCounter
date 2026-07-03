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
            client.insertRecords(payloads.map { it.toNutritionRecord() })
            QuickImportHealthWriteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            QuickImportHealthWriteResult.Failed(e.message ?: "Unknown Health Connect error")
        }
    }

    suspend fun deleteNutritionMeal(recordId: String): HealthConnectDeleteResult {
        return try {
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

    suspend fun writeHistoricalMealFoods(
        foods: List<HistoricalMealFood>,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        return try {
            val existingFingerprints = mutableSetOf<String>()
            val existingClientRecordIds = mutableSetOf<String>()
            foods.map { it.dateTime.toLocalDate() }.distinct().forEach { date ->
                readNutritionRecords(date).forEach { record ->
                    record.metadata.clientRecordId?.let(existingClientRecordIds::add)
                    existingFingerprints += record.existingHistoricalMealFingerprint()
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
        onProgress: (Float?, Int, Int) -> Unit,
    ): HistoricalMealHealthConnectResult {
        return try {
            val firstDate = minOf(startDate, endDate)
            val lastDate = maxOf(startDate, endDate)
            val dates = generateSequence(firstDate) { current ->
                current.plusDays(1).takeIf { it <= lastDate }
            }.toList()

            val idsToDelete = mutableListOf<String>()
            dates.forEachIndexed { index, date ->
                idsToDelete += readNutritionRecords(date)
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
