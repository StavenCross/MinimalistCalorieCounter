package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthConnectManager(private val context: Context) {
    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    suspend fun hasAllPermissions(): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun hasAnyPermissions(): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().isNotEmpty()
    }

    suspend fun syncSingleEntry(date: LocalDate, weight: Double, nutrients: Nutrients) {
        try {
            if (!hasAllPermissions()) return

            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

            // Overwrite by deleting first
            healthConnectClient.deleteRecords(NutritionRecord::class, timeRange)
            healthConnectClient.deleteRecords(WeightRecord::class, timeRange)

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

            healthConnectClient.insertRecords(nutritionRecords)
            healthConnectClient.insertRecords(weightRecords)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Health Connect Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun deleteSingleEntry(date: LocalDate) {
        try {
            if (!hasAllPermissions()) return
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

            healthConnectClient.deleteRecords(NutritionRecord::class, timeRange)
            healthConnectClient.deleteRecords(WeightRecord::class, timeRange)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Health Connect Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun readArchiveFromHealthConnect(): List<Triple<LocalDate, Double, Nutrients>> {
        val result = mutableListOf<Triple<LocalDate, Double, Nutrients>>()
        try {
            if (!hasAllPermissions()) return emptyList()

            val startTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant()
            val endTime = ZonedDateTime.now().plusDays(1).toInstant()
            val timeRange = TimeRangeFilter.between(startTime, endTime)

            val nutritionRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(NutritionRecord::class, timeRange)
            ).records

            val weightRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(WeightRecord::class, timeRange)
            ).records

            // Group by date
            val dataByDate = mutableMapOf<LocalDate, Pair<Double, MutableList<Double>>>()

            nutritionRecords.forEach { record ->
                val date = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                val nutrients = dataByDate.getOrPut(date) { Pair(0.0, MutableList(8) { 0.0 }) }
                
                nutrients.second[0] += record.energy?.inKilocalories ?: 0.0
                // Revert fiber carbs addition: App Carbs = HC Total Carbs - HC Fiber
                val hcTotalCarbs = record.totalCarbohydrate?.inGrams ?: 0.0
                val hcFiber = record.dietaryFiber?.inGrams ?: 0.0
                val hcSugar = record.sugar?.inGrams ?: 0.0
                // Ensure App Carbs >= App Sugar to pass validation
                nutrients.second[1] += (hcTotalCarbs - hcFiber).coerceAtLeast(hcSugar)
                nutrients.second[2] += hcSugar
                nutrients.second[3] += record.protein?.inGrams ?: 0.0
                
                val hcTotalFat = record.totalFat?.inGrams ?: 0.0
                val hcSaturatedFat = record.saturatedFat?.inGrams ?: 0.0
                // Ensure App Fat >= App Saturated Fat to pass validation
                nutrients.second[4] += hcTotalFat.coerceAtLeast(hcSaturatedFat)
                nutrients.second[5] += hcSaturatedFat

                nutrients.second[6] += hcFiber
                nutrients.second[7] = 0.0 // Cost set to zero
            }

            weightRecords.forEach { record ->
                val date = record.time.atZone(ZoneId.systemDefault()).toLocalDate()
                val current = dataByDate.getOrPut(date) { Pair(0.0, MutableList(8) { 0.0 }) }
                dataByDate[date] = Pair(record.weight.inKilograms, current.second)
            }

            dataByDate.forEach { (date, pair) ->
                result.add(Triple(date, pair.first, Nutrients(pair.second.toMutableStateList(), context)))
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Health Connect Restore Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        return result.sortedByDescending { it.first }
    }

    fun syncArchive(archive: Archive) {
        if (archive.entries.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!hasAllPermissions()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.health_connect_permissions_missing), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val nutritionRecords = mutableListOf<NutritionRecord>()
                val weightRecords = mutableListOf<WeightRecord>()

                archive.entries.forEach { (date, weight, nutrients) ->
                    val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
                    val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

                    // Delete existing records specifically for this day before inserting new ones
                    // This prevents wiping data from other apps on days not included in the archive
                    healthConnectClient.deleteRecords(NutritionRecord::class, timeRange)
                    healthConnectClient.deleteRecords(WeightRecord::class, timeRange)

                    // Nutrition
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
                            mealType = 0, // MEAL_TYPE_UNKNOWN
                            name = "Daily Total",
                            metadata = androidx.health.connect.client.records.metadata.Metadata.manualEntry(),
                        )
                    )

                    // Weight
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

                if (nutritionRecords.isNotEmpty()) {
                    healthConnectClient.insertRecords(nutritionRecords)
                }
                if (weightRecords.isNotEmpty()) {
                    healthConnectClient.insertRecords(weightRecords)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.toast_hc_full_archive_synced), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Health Connect Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
