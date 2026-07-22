package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.records.Record
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal object CheckInWorkbookBuilder {
    fun build(
        range: CheckInDateRange,
        recordsByType: Map<String, List<Record>>,
        errors: List<List<String>>,
        zoneId: ZoneId,
    ): List<XlsxSheet> {
        val nutrition = recordsByType["NutritionRecord"].orEmpty()
        return listOf(
            XlsxSheet("Summary", summaryRows(range, recordsByType, errors)),
            XlsxSheet("Meals Foods", nutritionRows(nutrition, zoneId)),
            XlsxSheet("Daily Nutrition", dailyNutritionRows(nutrition, zoneId)),
            XlsxSheet("Body Metrics", bodyMetricRows(recordsByType, zoneId)),
            XlsxSheet("Sleep", sleepRows(recordsByType["SleepSessionRecord"].orEmpty(), zoneId)),
            XlsxSheet("Activity Burn", activityRows(recordsByType, zoneId)),
            XlsxSheet("Heart Oxygen", heartRows(recordsByType, zoneId)),
            XlsxSheet("Exercise", exerciseRows(recordsByType["ExerciseSessionRecord"].orEmpty(), zoneId)),
        )
    }

    private fun summaryRows(
        range: CheckInDateRange,
        recordsByType: Map<String, List<Record>>,
        errors: List<List<String>>,
    ): List<List<String>> {
        val rows = mutableListOf(
            listOf("field", "value"),
            listOf("check_in", range.label),
            listOf("start_date", range.startDate.toString()),
            listOf("end_date", range.endDate.toString()),
            listOf("generated_at", Instant.now().toString()),
            emptyList(),
            listOf("record_type", "count"),
        )
        rows.addAll(checkInRecordTypes.map { type ->
            val name = type.simpleName.orEmpty()
            listOf(name, recordsByType[name].orEmpty().size.toString())
        })
        if (errors.isNotEmpty()) {
            rows.add(emptyList())
            rows.add(listOf("skipped_record_type", "reason"))
            rows.addAll(errors)
        }
        return rows
    }

    private fun nutritionRows(records: List<Record>, zoneId: ZoneId): List<List<String>> {
        return listOf(listOf("date", "time", "meal_type", "food", "kcal", "protein_g", "carbs_g", "fat_g", "fiber_g", "sugar_g", "sat_fat_g")) +
            records.map { record ->
                val start = record.instant("getStartTime") ?: record.instant("getTime")
                listOf(
                    start?.atZone(zoneId)?.toLocalDate()?.toString().orEmpty(),
                    start?.atZone(zoneId)?.toLocalTime()?.toString().orEmpty(),
                    record.text("getMealType"),
                    record.text("getName"),
                    record.energy("getEnergy"),
                    record.mass("getProtein", "getInGrams"),
                    record.mass("getTotalCarbohydrate", "getInGrams"),
                    record.mass("getTotalFat", "getInGrams"),
                    record.mass("getDietaryFiber", "getInGrams"),
                    record.mass("getSugar", "getInGrams"),
                    record.mass("getSaturatedFat", "getInGrams"),
                )
            }
    }

    private fun dailyNutritionRows(records: List<Record>, zoneId: ZoneId): List<List<String>> {
        val totals = records.groupBy { record ->
            (record.instant("getStartTime") ?: record.instant("getTime"))?.atZone(zoneId)?.toLocalDate()
        }.toSortedMap(compareBy { it })
        return listOf(listOf("date", "kcal", "protein_g", "carbs_g", "fat_g", "fiber_g", "sugar_g", "sat_fat_g")) +
            totals.map { (date, dayRecords) ->
                listOf(
                    date?.toString().orEmpty(),
                    dayRecords.sumOf { it.energyDouble("getEnergy") }.number(),
                    dayRecords.sumOf { it.massDouble("getProtein", "getInGrams") }.number(),
                    dayRecords.sumOf { it.massDouble("getTotalCarbohydrate", "getInGrams") }.number(),
                    dayRecords.sumOf { it.massDouble("getTotalFat", "getInGrams") }.number(),
                    dayRecords.sumOf { it.massDouble("getDietaryFiber", "getInGrams") }.number(),
                    dayRecords.sumOf { it.massDouble("getSugar", "getInGrams") }.number(),
                    dayRecords.sumOf { it.massDouble("getSaturatedFat", "getInGrams") }.number(),
                )
            }
    }

    private fun bodyMetricRows(recordsByType: Map<String, List<Record>>, zoneId: ZoneId): List<List<String>> {
        val rows = mutableListOf(listOf("date", "time", "metric", "value", "unit"))
        rows += metricRows(recordsByType["WeightRecord"].orEmpty(), zoneId, "weight", "getWeight", "getInPounds", "lb")
        rows += metricRows(recordsByType["HeightRecord"].orEmpty(), zoneId, "height", "getHeight", "getInInches", "in")
        rows += metricRows(recordsByType["BodyFatRecord"].orEmpty(), zoneId, "body_fat", "getPercentage", "getValue", "%")
        rows += metricRows(recordsByType["BasalMetabolicRateRecord"].orEmpty(), zoneId, "bmr", "getBasalMetabolicRate", "getInKilocaloriesPerDay", "kcal/day")
        return rows
    }

    private fun sleepRows(records: List<Record>, zoneId: ZoneId): List<List<String>> {
        return listOf(listOf("date", "start", "end", "duration_hours", "stage_count")) +
            records.map { record ->
                val start = record.instant("getStartTime")
                val end = record.instant("getEndTime")
                listOf(
                    start?.atZone(zoneId)?.toLocalDate()?.toString().orEmpty(),
                    start?.atZone(zoneId)?.toLocalTime()?.toString().orEmpty(),
                    end?.atZone(zoneId)?.toLocalTime()?.toString().orEmpty(),
                    durationHours(start, end),
                    (record.callGetter("getStages") as? Collection<*>)?.size?.toString().orEmpty(),
                )
            }
    }

    private fun activityRows(recordsByType: Map<String, List<Record>>, zoneId: ZoneId): List<List<String>> {
        val rows = mutableListOf(listOf("date", "metric", "value", "unit"))
        rows += metricRows(recordsByType["TotalCaloriesBurnedRecord"].orEmpty(), zoneId, "total_calories_burned", "getEnergy", "getInKilocalories", "kcal")
            .map { listOf(it[0], it[2], it[3], it[4]) }
        rows += metricRows(recordsByType["DistanceRecord"].orEmpty(), zoneId, "distance", "getDistance", "getInMiles", "mi")
            .map { listOf(it[0], it[2], it[3], it[4]) }
        rows += metricRows(recordsByType["StepsRecord"].orEmpty(), zoneId, "steps", "getCount", null, "steps")
            .map { listOf(it[0], it[2], it[3], it[4]) }
        return rows
    }

    private fun heartRows(recordsByType: Map<String, List<Record>>, zoneId: ZoneId): List<List<String>> {
        val rows = mutableListOf(listOf("date", "metric", "avg", "min", "max", "samples"))
        val heartSamples = recordsByType["HeartRateRecord"].orEmpty()
            .flatMap { record ->
                (record.callGetter("getSamples") as? Collection<*>).orEmpty().mapNotNull { sample ->
                    val bpm = sample?.callGetter("getBeatsPerMinute")?.toString()?.toDoubleOrNull()
                    val time = sample?.instant("getTime")
                    if (bpm == null || time == null) null else time.atZone(zoneId).toLocalDate() to bpm
                }
            }
            .groupBy({ it.first }, { it.second })
        rows += heartSamples.toSortedMap().map { (date, values) ->
            listOf(date.toString(), "heart_rate", values.average().number(), values.minOrNull().number(), values.maxOrNull().number(), values.size.toString())
        }
        rows += recordsByType["RestingHeartRateRecord"].orEmpty().map { record ->
            val date = record.instant("getTime")?.atZone(zoneId)?.toLocalDate()?.toString().orEmpty()
            listOf(date, "resting_heart_rate", record.text("getBeatsPerMinute"), "", "", "1")
        }
        rows += recordsByType["OxygenSaturationRecord"].orEmpty().map { record ->
            val date = record.instant("getTime")?.atZone(zoneId)?.toLocalDate()?.toString().orEmpty()
            listOf(date, "oxygen_saturation", record.measure("getPercentage", "getValue"), "", "", "1")
        }
        return rows
    }

    private fun exerciseRows(records: List<Record>, zoneId: ZoneId): List<List<String>> {
        return listOf(listOf("date", "title", "exercise_type", "start", "end", "duration_minutes")) +
            records.map { record ->
                val start = record.instant("getStartTime")
                val end = record.instant("getEndTime")
                listOf(
                    start?.atZone(zoneId)?.toLocalDate()?.toString().orEmpty(),
                    record.text("getTitle"),
                    record.text("getExerciseType"),
                    start?.atZone(zoneId)?.toLocalTime()?.toString().orEmpty(),
                    end?.atZone(zoneId)?.toLocalTime()?.toString().orEmpty(),
                    durationMinutes(start, end),
                )
            }
    }

    private fun metricRows(
        records: List<Record>,
        zoneId: ZoneId,
        metric: String,
        valueGetter: String,
        nestedGetter: String?,
        unit: String,
    ): List<List<String>> = records.map { record ->
        val time = record.instant("getTime") ?: record.instant("getStartTime")
        val value = if (nestedGetter == null) record.text(valueGetter) else record.measure(valueGetter, nestedGetter)
        listOf(
            time?.atZone(zoneId)?.toLocalDate()?.toString().orEmpty(),
            time?.atZone(zoneId)?.toLocalTime()?.toString().orEmpty(),
            metric,
            value,
            unit,
        )
    }

    private fun durationHours(start: Instant?, end: Instant?): String =
        if (start == null || end == null) "" else (Duration.between(start, end).toMinutes() / 60.0).number()

    private fun durationMinutes(start: Instant?, end: Instant?): String =
        if (start == null || end == null) "" else Duration.between(start, end).toMinutes().toString()
}

private fun Any.callGetter(name: String): Any? = runCatching {
    javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }?.invoke(this)
}.getOrNull()

private fun Any.instant(name: String): Instant? = callGetter(name) as? Instant

private fun Any.text(name: String): String = callGetter(name)?.toString().orEmpty()

private fun Any.measure(valueGetter: String, nestedGetter: String): String =
    callGetter(valueGetter)?.callGetter(nestedGetter)?.toString().orEmpty()

private fun Any.mass(valueGetter: String, nestedGetter: String): String =
    massDouble(valueGetter, nestedGetter).number()

private fun Any.massDouble(valueGetter: String, nestedGetter: String): Double =
    callGetter(valueGetter)?.callGetter(nestedGetter)?.toString()?.toDoubleOrNull() ?: 0.0

private fun Any.energy(valueGetter: String): String = energyDouble(valueGetter).number()

private fun Any.energyDouble(valueGetter: String): Double =
    callGetter(valueGetter)?.callGetter("getInKilocalories")?.toString()?.toDoubleOrNull() ?: 0.0

private fun Double?.number(): String = this?.let { if (it % 1.0 == 0.0) it.toLong().toString() else "%.2f".format(java.util.Locale.US, it) }.orEmpty()
