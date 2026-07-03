package com.makstuff.minimalistcaloriecounter.health

internal object HealthConnectExportCsv {
    private val rawHeader = listOf(
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

    private val sensitiveColumns = setOf(
        "record_id",
        "client_record_id",
        "client_record_version",
        "data_origin_package",
        "recording_method",
        "last_modified_time",
        "raw_record",
    )

    fun build(rows: List<List<String>>, redacted: Boolean = false): String {
        val includedIndexes = rawHeader.indices.filter { index ->
            !redacted || rawHeader[index] !in sensitiveColumns
        }
        val outputRows = listOf(rawHeader.project(includedIndexes)) + rows.map { it.project(includedIndexes) }
        return outputRows.joinToString("\n") { row ->
            row.joinToString(",") { csvEscape(it) }
        } + "\n"
    }

    private fun List<String>.project(indexes: List<Int>): List<String> {
        return indexes.map { index -> getOrElse(index) { "" } }
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
