package com.makstuff.minimalistcaloriecounter.health

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
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
        onProgress: (Float?, Int, Int) -> Unit,
    ): HealthConnectExportResult {
        return try {
            val firstDate = minOf(startDate, endDate)
            val lastDate = maxOf(startDate, endDate)
            val start = firstDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = lastDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val range = TimeRangeFilter.between(start, end)
            val rows = mutableListOf<List<String>>()

            exportRecordTypes.forEachIndexed { index, type ->
                rows += readExportRows(type, range)
                withContext(Dispatchers.Main) {
                    onProgress((index + 1).toFloat() / exportRecordTypes.size, index + 1, exportRecordTypes.size)
                }
            }

            val filename = "health_connect_export_${firstDate}_${lastDate}.csv"
            val displayPath = writeCsvToDownloads(filename, HealthConnectExportCsv.build(rows))
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

internal object HealthConnectExportCsv {
    fun build(rows: List<List<String>>): String {
        val header = listOf(
            "record_type",
            "start_time",
            "end_time",
            "record_id",
            "client_record_id",
            "client_record_version",
            "data_origin_package",
            "recording_method",
            "last_modified_time",
            "name",
            "meal_type",
            "energy_kcal",
            "carbs_g",
            "protein_g",
            "fat_g",
            "fiber_g",
            "weight_kg",
            "height_cm",
            "body_fat_percent",
            "lean_mass_kg",
            "raw_record",
        )
        return (listOf(header) + rows).joinToString("\n") { row ->
            row.joinToString(",") { csvEscape(it) }
        } + "\n"
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}

private fun Record.toExportRow(recordType: String): List<String> {
    val structured = structuredExportValues()
    return listOf(
        recordType,
        structured["startTime"].orEmpty(),
        structured["endTime"].orEmpty(),
        metadata.id,
        metadata.clientRecordId.orEmpty(),
        metadata.clientRecordVersion.toString(),
        metadata.dataOrigin.packageName,
        metadata.recordingMethod.toString(),
        metadata.lastModifiedTime.toString(),
        structured["name"].orEmpty(),
        structured["mealType"].orEmpty(),
        structured["energyKcal"].orEmpty(),
        structured["carbsG"].orEmpty(),
        structured["proteinG"].orEmpty(),
        structured["fatG"].orEmpty(),
        structured["fiberG"].orEmpty(),
        structured["weightKg"].orEmpty(),
        structured["heightCm"].orEmpty(),
        structured["bodyFatPercent"].orEmpty(),
        structured["leanMassKg"].orEmpty(),
        toString(),
    )
}

private fun Record.structuredExportValues(): Map<String, String> {
    val values = commonExportValues()
    when (this) {
        is NutritionRecord -> values += mapOf(
            "name" to name.orEmpty(),
            "mealType" to mealType.toString(),
            "energyKcal" to (energy?.inKilocalories?.toString() ?: ""),
            "carbsG" to (totalCarbohydrate?.inGrams?.toString() ?: ""),
            "proteinG" to (protein?.inGrams?.toString() ?: ""),
            "fatG" to (totalFat?.inGrams?.toString() ?: ""),
            "fiberG" to (dietaryFiber?.inGrams?.toString() ?: ""),
        )
        is WeightRecord -> values["weightKg"] = weight.inKilograms.toString()
        is HeightRecord -> values["heightCm"] = (height.inMeters * 100.0).toString()
        is BodyFatRecord -> values["bodyFatPercent"] = percentage.value.toString()
        is LeanBodyMassRecord -> values["leanMassKg"] = mass.inKilograms.toString()
    }
    return values
}

private fun Record.commonExportValues(): MutableMap<String, String> {
    val time = callNoArgGetter("getTime")
    val startTime = callNoArgGetter("getStartTime") ?: time
    val endTime = callNoArgGetter("getEndTime") ?: time
    return mutableMapOf<String, String>().apply {
        startTime?.let { put("startTime", it) }
        endTime?.let { put("endTime", it) }
    }
}

private fun Record.callNoArgGetter(name: String): String? {
    return runCatching {
        javaClass.methods
            .firstOrNull { method -> method.name == name && method.parameterCount == 0 }
            ?.invoke(this)
            ?.toString()
    }.getOrNull()
}

@OptIn(ExperimentalMindfulnessSessionApi::class)
private val exportRecordTypes: List<KClass<out Record>> = listOf(
    ActiveCaloriesBurnedRecord::class,
    BasalBodyTemperatureRecord::class,
    BasalMetabolicRateRecord::class,
    BloodGlucoseRecord::class,
    BloodPressureRecord::class,
    BodyFatRecord::class,
    BodyTemperatureRecord::class,
    BodyWaterMassRecord::class,
    BoneMassRecord::class,
    CervicalMucusRecord::class,
    CyclingPedalingCadenceRecord::class,
    DistanceRecord::class,
    ElevationGainedRecord::class,
    ExerciseSessionRecord::class,
    FloorsClimbedRecord::class,
    HeartRateRecord::class,
    HeartRateVariabilityRmssdRecord::class,
    HeightRecord::class,
    HydrationRecord::class,
    IntermenstrualBleedingRecord::class,
    LeanBodyMassRecord::class,
    MenstruationFlowRecord::class,
    MenstruationPeriodRecord::class,
    MindfulnessSessionRecord::class,
    NutritionRecord::class,
    OvulationTestRecord::class,
    OxygenSaturationRecord::class,
    PlannedExerciseSessionRecord::class,
    PowerRecord::class,
    RespiratoryRateRecord::class,
    RestingHeartRateRecord::class,
    SexualActivityRecord::class,
    SkinTemperatureRecord::class,
    SleepSessionRecord::class,
    SpeedRecord::class,
    StepsCadenceRecord::class,
    StepsRecord::class,
    TotalCaloriesBurnedRecord::class,
    Vo2MaxRecord::class,
    WeightRecord::class,
    WheelchairPushesRecord::class,
)

internal val allReadPermissions: Set<String> = exportRecordTypes
    .map { HealthPermission.getReadPermission(it) }
    .toSet() + HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
