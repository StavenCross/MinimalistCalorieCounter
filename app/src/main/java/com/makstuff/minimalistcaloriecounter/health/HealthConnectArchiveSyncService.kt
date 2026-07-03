package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

internal class HealthConnectArchiveSyncService(
    private val context: Context,
    private val client: HealthConnectClient,
) {
    suspend fun syncSingleEntry(date: LocalDate, weight: Double, nutrients: Nutrients) {
        try {
            val (nutritionRecords, weightRecords, timeRange) = recordsForEntry(date, weight, nutrients)
            client.deleteRecords(NutritionRecord::class, timeRange)
            client.deleteRecords(WeightRecord::class, timeRange)
            client.insertRecords(nutritionRecords)
            client.insertRecords(weightRecords)
        } catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Health Connect Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun deleteSingleEntry(date: LocalDate) {
        try {
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
        onError: (String) -> Unit,
    ) {
        if (archive.entries.isEmpty()) return

        try {
            val totalEntries = archive.entries.size
            var currentEntriesDone = 0
            val chunks = archive.entries.chunked(5)

            chunks.forEachIndexed { index, chunk ->
                retryHealthConnectQuotaSensitive {
                    val nutritionRecords = mutableListOf<NutritionRecord>()
                    val weightRecords = mutableListOf<WeightRecord>()

                    chunk.forEach { (date, weight, nutrients) ->
                        val records = recordsForEntry(date, weight, nutrients)
                        client.deleteRecords(NutritionRecord::class, records.timeRange)
                        client.deleteRecords(WeightRecord::class, records.timeRange)
                        nutritionRecords += records.nutritionRecords
                        weightRecords += records.weightRecords
                    }

                    client.insertRecords(nutritionRecords)
                    client.insertRecords(weightRecords)
                }

                currentEntriesDone += chunk.size
                withContext(Dispatchers.Main) {
                    onProgress((index + 1).toFloat() / chunks.size, currentEntriesDone, totalEntries)
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

    private fun recordsForEntry(date: LocalDate, weight: Double, nutrients: Nutrients): ArchiveEntryRecords {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
        val nutritionRecords = mutableListOf<NutritionRecord>()
        val weightRecords = mutableListOf<WeightRecord>()

        nutritionRecords += NutritionRecord(
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

        if (weight > 0) {
            weightRecords += WeightRecord(
                time = startOfDay,
                zoneOffset = ZoneId.systemDefault().rules.getOffset(startOfDay),
                weight = Mass.kilograms(weight),
                metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry(),
            )
        }

        return ArchiveEntryRecords(
            nutritionRecords = nutritionRecords,
            weightRecords = weightRecords,
            timeRange = TimeRangeFilter.between(startOfDay, endOfDay),
        )
    }

    private data class ArchiveEntryRecords(
        val nutritionRecords: List<NutritionRecord>,
        val weightRecords: List<WeightRecord>,
        val timeRange: TimeRangeFilter,
    )
}
