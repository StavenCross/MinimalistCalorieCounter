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
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {
    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class),
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
