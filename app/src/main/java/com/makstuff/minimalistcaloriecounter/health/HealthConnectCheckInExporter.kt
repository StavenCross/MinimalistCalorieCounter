package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.makstuff.minimalistcaloriecounter.io.DownloadsTextWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import kotlin.reflect.KClass

internal class HealthConnectCheckInExporter(
    private val context: Context,
    private val client: HealthConnectClient,
) {
    suspend fun exportXlsx(
        range: CheckInDateRange,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HealthConnectExportResult {
        return try {
            val zoneId = ZoneId.systemDefault()
            val start = range.startDate.atStartOfDay(zoneId).toInstant()
            val end = range.endDate.plusDays(1).atStartOfDay(zoneId).toInstant()
            val timeRange = TimeRangeFilter.between(start, end)
            val recordSets = mutableMapOf<String, List<Record>>()
            val errors = mutableListOf<List<String>>()

            checkInRecordTypes.forEachIndexed { index, type ->
                val result = readRecordsOrError(type, timeRange)
                if (result.error == null) {
                    recordSets[type.simpleName.orEmpty()] = result.records
                } else {
                    errors += listOf(type.simpleName.orEmpty(), result.error)
                }
                withContext(Dispatchers.Main) {
                    onProgress((index + 1).toFloat() / checkInRecordTypes.size, index + 1, checkInRecordTypes.size)
                }
            }

            val workbook = CheckInWorkbookBuilder.build(range, recordSets, errors, zoneId)
            val filename = "chatgpt_checkin_${range.filenameToken}_${range.startDate}_${range.endDate}.xlsx"
            val displayPath = DownloadsTextWriter(context).writeBytes(
                filename = filename,
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content = SimpleXlsxWriter.build(workbook),
            )
            val recordCount = recordSets.values.sumOf { it.size }
            HealthConnectExportResult.Success(displayPath = displayPath, records = recordCount)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectExportResult.Failed(e.message ?: "Unknown check-in export error")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun readRecordsOrError(
        type: KClass<out Record>,
        range: TimeRangeFilter,
    ): CheckInReadResult = try {
        val records = readAllHealthConnectPages { pageToken ->
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = type as KClass<Record>,
                    timeRangeFilter = range,
                    ascendingOrder = true,
                    pageSize = HEALTH_CONNECT_PAGE_SIZE,
                    pageToken = pageToken,
                )
            )
            HealthConnectPage(response.records, response.pageToken)
        }
        CheckInReadResult(records = records)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) {
        CheckInReadResult(error = e.message ?: e::class.java.simpleName)
    }
}

private data class CheckInReadResult(
    val records: List<Record> = emptyList(),
    val error: String? = null,
)

internal val checkInRecordTypes: List<KClass<out Record>> = listOf(
    NutritionRecord::class,
    WeightRecord::class,
    HeightRecord::class,
    BodyFatRecord::class,
    BasalMetabolicRateRecord::class,
    SleepSessionRecord::class,
    HeartRateRecord::class,
    RestingHeartRateRecord::class,
    OxygenSaturationRecord::class,
    TotalCaloriesBurnedRecord::class,
    DistanceRecord::class,
    StepsRecord::class,
    ExerciseSessionRecord::class,
)
