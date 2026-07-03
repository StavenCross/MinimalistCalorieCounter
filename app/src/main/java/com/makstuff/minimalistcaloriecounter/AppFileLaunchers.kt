package com.makstuff.minimalistcaloriecounter

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

data class AppFileLaunchers(
    val databaseImporter: ManagedActivityResultLauncher<Array<String>, Uri?>,
    val databaseExporter: ManagedActivityResultLauncher<String, Uri?>,
    val archiveImporter: ManagedActivityResultLauncher<Array<String>, Uri?>,
    val archiveExporter: ManagedActivityResultLauncher<String, Uri?>,
    val historicalMealImporter: ManagedActivityResultLauncher<Array<String>, Uri?>,
)

@Composable
fun rememberAppFileLaunchers(viewModel: AppViewModel): AppFileLaunchers {
    val context = LocalContext.current
    val databaseImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            try {
                uri?.let { context.contentResolver.openInputStream(it) }?.use { inputStream ->
                    viewModel.databaseImportCSV(context, inputStream)
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.database) + ": " + context.getString(R.string.import_successful),
                    Toast.LENGTH_LONG,
                ).show()
            } catch (e: IllegalStateException) {
                Toast.makeText(context, context.getString(R.string.import_failed) + ": " + e.message, Toast.LENGTH_LONG).show()
            }
        },
    )
    val databaseExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values"),
        onResult = { uri ->
            val folder = context.getExternalFilesDir(null) ?: context.filesDir
            uri?.let { context.contentResolver.openOutputStream(it) }?.let {
                File(folder, "database.csv").inputStream().copyTo(it)
            }
        },
    )
    val archiveImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            try {
                uri?.let { context.contentResolver.openInputStream(it) }?.use { inputStream ->
                    viewModel.archiveImportCSV(context, inputStream)
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.archive) + ": " + context.getString(R.string.import_successful),
                    Toast.LENGTH_LONG,
                ).show()
            } catch (e: IllegalStateException) {
                Toast.makeText(context, context.getString(R.string.import_failed) + ": " + e.message, Toast.LENGTH_LONG).show()
            }
        },
    )
    val archiveExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values"),
        onResult = { uri ->
            val folder = context.getExternalFilesDir(null) ?: context.filesDir
            uri?.let { context.contentResolver.openOutputStream(it) }?.let {
                File(folder, "archive.csv").inputStream().copyTo(it)
            }
        },
    )
    val historicalMealImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            try {
                val rows = uri?.let { context.contentResolver.openInputStream(it) }?.use {
                    csvReader().readAll(it)
                } ?: return@rememberLauncherForActivityResult
                viewModel.previewHistoricalMealImport(rows)
            } catch (e: Throwable) {
                Toast.makeText(context, "Historical meal import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        },
    )
    return remember(databaseImporter, databaseExporter, archiveImporter, archiveExporter, historicalMealImporter) {
        AppFileLaunchers(databaseImporter, databaseExporter, archiveImporter, archiveExporter, historicalMealImporter)
    }
}
