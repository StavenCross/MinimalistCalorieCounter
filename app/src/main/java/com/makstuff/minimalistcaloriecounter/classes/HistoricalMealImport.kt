package com.makstuff.minimalistcaloriecounter.classes

import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToLong

data class HistoricalMealImportIssue(
    val rowNumber: Int,
    val message: String,
)

data class HistoricalMealFood(
    val sourceRowNumber: Int,
    val logId: String,
    val dateTime: LocalDateTime,
    val mealName: String,
    val itemDescription: String,
    val mealType: QuickImportMealType,
    val nutrients: QuickImportNutrients,
    val clientRecordId: String,
    val fingerprint: String,
) {
    fun toHealthPayload(): QuickImportHealthPayload {
        return QuickImportHealthPayload(
            dateTime = dateTime,
            mealType = mealType.healthConnectValue,
            energy = nutrients.energy,
            energyFromFat = nutrients.fat * 9.0,
            totalCarbohydrate = nutrients.carbohydrate,
            sugar = nutrients.sugar,
            protein = nutrients.protein,
            totalFat = nutrients.fat,
            saturatedFat = nutrients.saturatedFat,
            dietaryFiber = nutrients.fiber,
            name = itemDescription.ifBlank { mealName.ifBlank { "Historical meal food" } },
            clientRecordId = clientRecordId,
        )
    }
}

data class HistoricalMealImportPreview(
    val foods: List<HistoricalMealFood>,
    val issues: List<HistoricalMealImportIssue>,
    val totalRows: Int,
) {
    val validRows: Int
        get() = foods.size
    val skippedRows: Int
        get() = issues.size
    val mealCount: Int
        get() = foods.map { it.dateTime to it.mealName }.distinct().size
    val dates: Set<LocalDate>
        get() = foods.map { it.dateTime.toLocalDate() }.toSet()
    val startDate: LocalDate?
        get() = dates.minOrNull()
    val endDate: LocalDate?
        get() = dates.maxOrNull()
}

object HistoricalMealImporter {
    const val CLIENT_RECORD_ID_PREFIX = "mcc-historical-meal:"

    fun parseCsv(rows: List<List<String>>): HistoricalMealImportPreview {
        if (rows.isEmpty()) {
            return HistoricalMealImportPreview(emptyList(), listOf(HistoricalMealImportIssue(1, "CSV is empty.")), 0)
        }

        val header = rows.first().map { it.trim() }
        val index = header.withIndex().associate { it.value to it.index }
        val issues = mutableListOf<HistoricalMealImportIssue>()
        val foods = mutableListOf<HistoricalMealFood>()
        val requiredColumns = listOf(
            "date_time",
            "meal_name",
            "item_description",
            "calories",
            "fat_g",
            "sat_fat_g",
            "carbs_g",
            "fiber_g",
            "sugar_g",
            "protein_g",
        )
        val missingColumns = requiredColumns.filter { it !in index }
        if (missingColumns.isNotEmpty()) {
            return HistoricalMealImportPreview(
                emptyList(),
                listOf(HistoricalMealImportIssue(1, "Missing required columns: ${missingColumns.joinToString(", ")}.")),
                (rows.size - 1).coerceAtLeast(0),
            )
        }

        val mealCounters = mutableMapOf<String, Int>()
        rows.drop(1).forEachIndexed { rowOffset, row ->
            val rowNumber = rowOffset + 2
            if (row.all { it.isBlank() }) {
                return@forEachIndexed
            }
            fun value(column: String): String = row.getOrNull(index.getValue(column)).orEmpty().trim()

            val dateTimeText = value("date_time")
            val mealName = value("meal_name")
            val itemDescription = value("item_description")
            val parsedDateTime = parseDateTime(dateTimeText, mealName)
            val nutrients = runCatching {
                QuickImportNutrients(
                    energy = requiredDouble(value("calories"), "calories"),
                    carbohydrate = requiredDouble(value("carbs_g"), "carbs_g"),
                    sugar = requiredDouble(value("sugar_g"), "sugar_g"),
                    protein = requiredDouble(value("protein_g"), "protein_g"),
                    fat = requiredDouble(value("fat_g"), "fat_g"),
                    saturatedFat = requiredDouble(value("sat_fat_g"), "sat_fat_g"),
                    fiber = requiredDouble(value("fiber_g"), "fiber_g"),
                )
            }

            if (parsedDateTime == null) {
                issues.add(HistoricalMealImportIssue(rowNumber, "Could not parse date_time '$dateTimeText'."))
                return@forEachIndexed
            }
            val nutrientsValue = nutrients.getOrElse {
                issues.add(HistoricalMealImportIssue(rowNumber, it.message ?: "Invalid nutrition values."))
                return@forEachIndexed
            }

            val mealType = explicitMealType(dateTimeText, mealName) ?: QuickImportMealType.inferFrom(parsedDateTime)
            val mealKey = "${parsedDateTime.toLocalDate()}|${parsedDateTime.toLocalTime()}|$mealName"
            val offset = mealCounters.getOrDefault(mealKey, 0)
            mealCounters[mealKey] = offset + 1
            val recordDateTime = parsedDateTime.plusSeconds(offset.toLong())
            val fingerprint = fingerprint(
                dateTime = recordDateTime,
                mealType = mealType,
                name = itemDescription,
                nutrients = nutrientsValue,
            )
            val logId = value("log_id").ifBlank { "row-$rowNumber" }
            foods.add(
                HistoricalMealFood(
                    sourceRowNumber = rowNumber,
                    logId = logId,
                    dateTime = recordDateTime,
                    mealName = mealName,
                    itemDescription = itemDescription,
                    mealType = mealType,
                    nutrients = nutrientsValue,
                    clientRecordId = "$CLIENT_RECORD_ID_PREFIX${stableId(logId, fingerprint)}",
                    fingerprint = fingerprint,
                )
            )
        }

        return HistoricalMealImportPreview(foods, issues, (rows.size - 1).coerceAtLeast(0))
    }

    fun parseDateTime(text: String, mealName: String): LocalDateTime? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        DATE_TIME_FORMATS.forEach { formatter ->
            try {
                return LocalDateTime.parse(trimmed.uppercase(Locale.US), formatter)
            } catch (_: DateTimeParseException) {
            }
        }
        val date = parseLeadingDate(trimmed) ?: return null
        val mealType = explicitMealType(trimmed, mealName)
        return date.atTime(fallbackTime(mealType))
    }

    fun fingerprint(
        dateTime: LocalDateTime,
        mealType: QuickImportMealType,
        name: String,
        nutrients: QuickImportNutrients,
    ): String {
        return listOf(
            dateTime.toString(),
            mealType.name,
            normalizeName(name),
            nutrients.energy.fingerprintNumber(),
            nutrients.carbohydrate.fingerprintNumber(),
            nutrients.fiber.fingerprintNumber(),
            nutrients.sugar.fingerprintNumber(),
            nutrients.protein.fingerprintNumber(),
            nutrients.fat.fingerprintNumber(),
            nutrients.saturatedFat.fingerprintNumber(),
        ).joinToString("|")
    }

    private fun requiredDouble(value: String, label: String): Double {
        require(value.isNotBlank()) { "Missing $label." }
        return value.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid $label '$value'.")
    }

    private fun explicitMealType(vararg values: String): QuickImportMealType? {
        val joined = values.joinToString(" ").lowercase(Locale.US)
        return when {
            "breakfast" in joined -> QuickImportMealType.Breakfast
            "lunch" in joined -> QuickImportMealType.Lunch
            "dinner" in joined -> QuickImportMealType.Dinner
            "snack" in joined -> QuickImportMealType.Snack
            "afternoon" in joined -> QuickImportMealType.Snack
            else -> null
        }
    }

    private fun fallbackTime(mealType: QuickImportMealType?): LocalTime {
        return when (mealType) {
            QuickImportMealType.Breakfast -> LocalTime.of(8, 0)
            QuickImportMealType.Lunch -> LocalTime.of(12, 0)
            QuickImportMealType.Dinner -> LocalTime.of(18, 0)
            QuickImportMealType.Snack -> LocalTime.of(15, 30)
            null -> LocalTime.of(12, 0)
        }
    }

    private fun parseLeadingDate(text: String): LocalDate? {
        val match = Regex("""^(\d{4}-\d{2}-\d{2})""").find(text) ?: return null
        return runCatching { LocalDate.parse(match.groupValues[1]) }.getOrNull()
    }

    private fun stableId(logId: String, fingerprint: String): String {
        return sanitizeId(logId).ifBlank { sha256(fingerprint).take(24) }
    }

    private fun sanitizeId(value: String): String {
        return value.lowercase(Locale.US)
            .replace(Regex("""[^a-z0-9._:-]+"""), "-")
            .trim('-')
            .take(120)
    }

    private fun normalizeName(value: String): String = value.trim().replace(Regex("""\s+"""), " ").lowercase(Locale.US)

    private fun Double.fingerprintNumber(): String = (this * 1000.0).roundToLong().toString()

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private val DATE_TIME_FORMATS = listOf(
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd h:mm a").toFormatter(Locale.US),
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd hh:mm a").toFormatter(Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
    )
}
