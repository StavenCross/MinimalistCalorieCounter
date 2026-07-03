package com.makstuff.minimalistcaloriecounter.persistence

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.doyaaaaaken.kotlincsv.util.CSVFieldNumDifferentException
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.DatabaseEntry
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.GoalsCsv
import com.makstuff.minimalistcaloriecounter.essentials.NUTRIENT_PROPERTIES
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import java.io.File

class AppCsvStore {
    fun readDatabase(context: Context): List<DatabaseEntry> {
        return try {
            val rows = csvReader().readAll(file(context, "database.csv").inputStream())
            check(rows.isNotEmpty()) { context.getString(R.string.database) + ": " + context.getString(R.string.csv_wrong_number_fields) }
            check(rows[0].size == 11) { context.getString(R.string.database) + ": " + context.getString(R.string.csv_wrong_number_fields) }
            rows.drop(1).map { csvLine -> DatabaseEntry.fromCSV(csvLine, context) }
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException(context.getString(R.string.database) + ": " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    fun writeDatabase(context: Context, database: List<DatabaseEntry>) {
        csvWriter().open(file(context, "database.csv")) {
            writeRow(listOf("Name") + NUTRIENT_PROPERTIES.map { it.nameForCSV } + listOf("CustomWeights", "Quickselect"))
            database.forEach { writeRow(it.stringCSV) }
        }
    }

    fun readArchive(context: Context): Archive {
        return try {
            val rows = csvReader().readAll(file(context, "archive.csv").inputStream())
            check(rows.isNotEmpty()) { context.getString(R.string.archive) + ": " + context.getString(R.string.csv_wrong_number_fields) }
            check(rows[0].size == 10) { context.getString(R.string.archive) + ": " + context.getString(R.string.csv_wrong_number_fields) }
            Archive.fromCSV(rows, context)
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException(context.getString(R.string.archive) + ": " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    fun writeArchive(context: Context, archive: Archive) {
        csvWriter().open(file(context, "archive.csv")) {
            archive.getCsvString().forEach { writeRow(it) }
        }
    }

    fun readDay(context: Context): Combo {
        return try {
            Combo.fromCSV(csvReader().readAll(file(context, "day.csv").inputStream()), context)
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException(context.getString(R.string.day) + ": " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    fun writeDay(context: Context, day: Combo) {
        csvWriter().open(file(context, "day.csv")) {
            day.getCsvString().forEach { writeRow(it) }
        }
    }

    fun readGoals(context: Context): Goals? {
        return try {
            val goalsFile = file(context, "goals.csv")
            if (!goalsFile.exists()) return null
            val rows = goalsFile.readLines()
                .filter { it.isNotBlank() }
                .map { line -> line.split(",") }
            GoalsCsv.fromRows(rows)
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException("Goals: " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    fun writeGoals(context: Context, goals: Goals) {
        csvWriter().open(file(context, "goals.csv")) {
            GoalsCsv.toRows(goals).forEach { writeRow(it) }
        }
    }

    fun readOptions(context: Context): AppOptionsFile? {
        return try {
            val optionsFile = file(context, "options.csv")
            if (!optionsFile.exists()) return null
            val rows = csvReader().readAll(optionsFile.inputStream())
            if (rows.isEmpty()) return null
            AppOptionsFile(
                theme = rows[0].toTheme(),
                healthConnectSyncEnabled = rows.getOrNull(1)?.contains("true"),
                healthConnectToastsEnabled = rows.getOrNull(2)?.contains("true"),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun writeOptions(context: Context, theme: AppTheme, syncEnabled: Boolean, toastsEnabled: Boolean) {
        csvWriter().open(file(context, "options.csv")) {
            writeRow(listOf(theme.toCsvValue()))
            writeRow(listOf(syncEnabled.toString()))
            writeRow(listOf(toastsEnabled.toString()))
        }
    }

    fun resetFromRawResource(context: Context, filename: String, rawResourceId: Int, overwriteIfExists: Boolean) {
        val target = file(context, filename)
        if (!target.exists() || overwriteIfExists) {
            context.resources.openRawResource(rawResourceId).copyTo(target.outputStream())
        }
    }

    fun resetGoals(context: Context, overwriteIfExists: Boolean) {
        val target = file(context, "goals.csv")
        if (!target.exists() || overwriteIfExists) {
            writeGoals(context, Goals())
        }
    }

    fun resetOptions(context: Context, overwriteIfExists: Boolean) {
        val target = file(context, "options.csv")
        if (!target.exists() || overwriteIfExists) {
            target.writeText("dark")
        }
    }

    private fun file(context: Context, filename: String): File {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        return File(folder, filename)
    }

    private fun List<String>.toTheme(): AppTheme {
        return if (contains("dark")) {
            AppTheme.MODE_NIGHT
        } else if (contains("light")) {
            AppTheme.MODE_DAY
        } else {
            AppTheme.MODE_AUTO
        }
    }

    private fun AppTheme.toCsvValue(): String {
        return when (this) {
            AppTheme.MODE_NIGHT -> "dark"
            AppTheme.MODE_DAY -> "light"
            AppTheme.MODE_AUTO -> "auto"
        }
    }
}

data class AppOptionsFile(
    val theme: AppTheme,
    val healthConnectSyncEnabled: Boolean?,
    val healthConnectToastsEnabled: Boolean?,
)
