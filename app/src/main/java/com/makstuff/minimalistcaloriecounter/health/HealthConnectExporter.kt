package com.makstuff.minimalistcaloriecounter.health

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
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
            val displayPath = writeCsvToDownloads(filename, HealthConnectExportCsv.build(rows, redacted))
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

    private fun writeCsvToDownloads(filename: String, csv: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create Downloads export file.")
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray(Charsets.UTF_8))
            } ?: error("Could not write Downloads export file.")
            "Downloads/$filename"
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = java.io.File(dir, filename)
            file.writeText(csv, Charsets.UTF_8)
            file.absolutePath
        }
    }
}
