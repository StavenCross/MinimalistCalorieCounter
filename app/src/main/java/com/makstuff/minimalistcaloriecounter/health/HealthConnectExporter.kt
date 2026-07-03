package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.makstuff.minimalistcaloriecounter.io.DownloadsTextWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.reflect.KClass

internal class HealthConnectExporter(
    private val context: Context,
    private val client: HealthConnectClient,
) {
    suspend fun exportCsv(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: HealthConnectExportMode,
        redacted: Boolean,
        onProgress: (Float?, Int, Int) -> Unit,
    ): HealthConnectExportResult {
        return try {
            val firstDate = minOf(startDate, endDate)
            val lastDate = maxOf(startDate, endDate)
            val start = firstDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = lastDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val range = TimeRangeFilter.between(start, end)
            val rows = mutableListOf<List<String>>()
            val recordTypes = mode.recordTypes()

            recordTypes.forEachIndexed { index, type ->
                rows += readExportRows(type, range)
                withContext(Dispatchers.Main) {
                    onProgress((index + 1).toFloat() / recordTypes.size, index + 1, recordTypes.size)
                }
            }

            val redactionToken = if (redacted) "redacted" else "raw"
            val filename = "health_connect_${mode.filenameToken}_${redactionToken}_${firstDate}_${lastDate}.csv"
            val displayPath = DownloadsTextWriter(context).write(filename, "text/csv", HealthConnectExportCsv.build(rows, redacted))
            HealthConnectExportResult.Success(displayPath = displayPath, records = rows.size)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectExportResult.Failed(e.message ?: "Unknown Health Connect export error")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun readExportRows(
        recordType: KClass<out Record>,
        range: TimeRangeFilter,
    ): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = recordType as KClass<Record>,
                    timeRangeFilter = range,
                    ascendingOrder = true,
                    pageSize = 500,
                    pageToken = pageToken,
                )
            )
            response.records.forEach { rows += it.toExportRow(recordType.simpleName ?: "Record") }
            pageToken = response.pageToken
        } while (!pageToken.isNullOrBlank())
        return rows
    }

}
