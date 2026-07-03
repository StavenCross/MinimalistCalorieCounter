package com.makstuff.minimalistcaloriecounter.ui.settings

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import com.makstuff.minimalistcaloriecounter.AppFileLaunchers
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonText
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetNote
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetTitle
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsPage(
    uiState: AppUiState,
    viewModel: AppViewModel,
    fileLaunchers: AppFileLaunchers,
    healthConnectManager: HealthConnectManager,
    healthConnectExportPermissionLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var historicalCleanupConfirmVisible by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cleanupStartDate = uiState.healthConnectNutritionCleanupStartDate
    val cleanupEndDate = uiState.healthConnectNutritionCleanupEndDate
    val cleanupPreview = uiState.healthConnectNutritionCleanupPreview
    val exportStartDate = uiState.healthConnectExportStartDate
    val exportEndDate = uiState.healthConnectExportEndDate
    val cleanupStartPicker = remember(cleanupStartDate) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                viewModel.updateHealthConnectNutritionCleanupStartDate(java.time.LocalDate.of(year, month + 1, day))
            },
            cleanupStartDate.year,
            cleanupStartDate.monthValue - 1,
            cleanupStartDate.dayOfMonth,
        )
    }
    val cleanupEndPicker = remember(cleanupEndDate) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                viewModel.updateHealthConnectNutritionCleanupEndDate(java.time.LocalDate.of(year, month + 1, day))
            },
            cleanupEndDate.year,
            cleanupEndDate.monthValue - 1,
            cleanupEndDate.dayOfMonth,
        )
    }
    val exportStartPicker = remember(exportStartDate) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                viewModel.updateHealthConnectExportStartDate(java.time.LocalDate.of(year, month + 1, day))
            },
            exportStartDate.year,
            exportStartDate.monthValue - 1,
            exportStartDate.dayOfMonth,
        )
    }
    val exportEndPicker = remember(exportEndDate) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                viewModel.updateHealthConnectExportEndDate(java.time.LocalDate.of(year, month + 1, day))
            },
            exportEndDate.year,
            exportEndDate.monthValue - 1,
            exportEndDate.dayOfMonth,
        )
    }

    fun handleHCInteraction(onSuccess: () -> Unit) {
        val availabilityStatus = try {
            HealthConnectClient.getSdkStatus(context)
        } catch (_: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }

        if (android.os.Build.VERSION.SDK_INT < 28) {
            Toast.makeText(context, context.getString(R.string.toast_hc_not_available), Toast.LENGTH_LONG).show()
        } else if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            Toast.makeText(context, context.getString(R.string.toast_hc_not_available), Toast.LENGTH_LONG).show()
        } else if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            val uriString = "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.android.vending")
                    data = uriString.toUri()
                    putExtra("overlay", true)
                    putExtra("callerId", context.packageName)
                })
            } catch (_: Exception) {
                try {
                    uriHandler.openUri("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                } catch (_: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_hc_not_available), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            if (uiState.healthConnectPermissionsGranted) onSuccess() else viewModel.setAlertDialogHealthConnectPermissions(true)
        }
    }

    if (historicalCleanupConfirmVisible) {
        AlertDialog(
            onDismissRequest = { historicalCleanupConfirmVisible = false },
            confirmButton = {
                ButtonText(
                    text = stringResource(R.string.button_continue),
                    onClick = {
                        historicalCleanupConfirmVisible = false
                        handleHCInteraction { viewModel.removeHealthConnectNutritionRange() }
                    },
                )
            },
            dismissButton = {
                ButtonText(text = stringResource(R.string.button_cancel), onClick = { historicalCleanupConfirmVisible = false })
            },
            title = { Text(stringResource(R.string.confirmation)) },
            text = {
                Text("Remove ${cleanupPreview?.total ?: 0} ${uiState.healthConnectNutritionCleanupMode.label.lowercase()} records from ${cleanupStartDate} through ${cleanupEndDate}?")
            },
        )
    }

    val currentThemeLabel = when (uiState.themeUserSetting) {
        AppTheme.MODE_AUTO -> stringArrayResource(R.array.dark_mode_options)[0]
        AppTheme.MODE_DAY -> stringArrayResource(R.array.dark_mode_options)[1]
        AppTheme.MODE_NIGHT -> stringArrayResource(R.array.dark_mode_options)[2]
    }
    val healthStatus = if (uiState.healthConnectPermissionsGranted) "Connected" else "Needs permissions"

    uiState.activeSettingsSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.updateActiveSettingsSheet(null) },
            sheetState = settingsSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (sheet) {
                    SettingsSheet.HealthData -> HealthDataSheetContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        healthConnectManager = healthConnectManager,
                        healthConnectExportPermissionLauncher = healthConnectExportPermissionLauncher,
                        exportStartPicker = exportStartPicker,
                        exportEndPicker = exportEndPicker,
                        cleanupStartPicker = cleanupStartPicker,
                        cleanupEndPicker = cleanupEndPicker,
                        onConfirmCleanup = { historicalCleanupConfirmVisible = true },
                        onHealthConnectAction = { handleHCInteraction(it) },
                    )
                    SettingsSheet.ImportTools -> ImportToolsSheetContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        fileLaunchers = fileLaunchers,
                        onHealthConnectAction = { handleHCInteraction(it) },
                    )
                    SettingsSheet.Theme -> ThemeSheetContent(uiState, viewModel)
                    SettingsSheet.Maintenance -> MaintenanceSheetContent(viewModel, fileLaunchers)
                }
            }
        }
    }

    SettingsCards(
        uiState = uiState,
        viewModel = viewModel,
        currentThemeLabel = currentThemeLabel,
        healthStatus = healthStatus,
        onHealthConnectAction = { handleHCInteraction(it) },
    )
}

@Composable
private fun HealthDataSheetContent(
    uiState: AppUiState,
    viewModel: AppViewModel,
    healthConnectManager: HealthConnectManager,
    healthConnectExportPermissionLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>,
    exportStartPicker: DatePickerDialog,
    exportEndPicker: DatePickerDialog,
    cleanupStartPicker: DatePickerDialog,
    cleanupEndPicker: DatePickerDialog,
    onConfirmCleanup: () -> Unit,
    onHealthConnectAction: (() -> Unit) -> Unit,
) {
    val cleanupStartDate = uiState.healthConnectNutritionCleanupStartDate
    val cleanupEndDate = uiState.healthConnectNutritionCleanupEndDate
    val cleanupPreview = uiState.healthConnectNutritionCleanupPreview
    val exportStartDate = uiState.healthConnectExportStartDate
    val exportEndDate = uiState.healthConnectExportEndDate
    SheetTitle("Manage Health Connect data", "Sync, import, and clean up nutrition records written by this app.")
    OptionsItem(stringResource(R.string.dropdown_export_archive_health_connect)) {
        viewModel.updateActiveSettingsSheet(null)
        onHealthConnectAction { viewModel.setAlertDialogHealthConnectSync(true) }
    }
    OptionsSectionHeader("Export from Health Connect")
    OptionsItem("Export start", trailingText = exportStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) { exportStartPicker.show() }
    OptionsItem("Export end", trailingText = exportEndDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) { exportEndPicker.show() }
    OptionsItem("Export mode", trailingText = uiState.healthConnectExportMode.label) {
        val modes = HealthConnectExportMode.entries
        val current = modes.indexOf(uiState.healthConnectExportMode).coerceAtLeast(0)
        viewModel.updateHealthConnectExportMode(modes[(current + 1) % modes.size])
    }
    OptionsItem("Redacted for ChatGPT", trailingText = if (uiState.healthConnectExportRedacted) "On" else "Off") {
        viewModel.updateHealthConnectExportRedacted(!uiState.healthConnectExportRedacted)
    }
    OptionsItem(
        text = when {
            uiState.healthConnectExportInProgress -> "Exporting Health Connect CSV..."
            !uiState.healthConnectExportPermissionsGranted -> "Grant export read permissions"
            uiState.healthConnectExportRedacted -> "Export redacted ${uiState.healthConnectExportMode.label.lowercase()} CSV"
            else -> "Export raw ${uiState.healthConnectExportMode.label.lowercase()} CSV"
        },
    ) {
        if (uiState.healthConnectExportPermissionsGranted) {
            viewModel.exportHealthConnectRange()
        } else {
            healthConnectExportPermissionLauncher.launch(healthConnectManager.exportPermissionsFor(uiState.healthConnectExportMode))
        }
    }
    uiState.healthConnectExportMessage?.let { message ->
        SheetNote(message, isError = message.contains("failed", ignoreCase = true) || message.contains("missing", ignoreCase = true))
    }
    OptionsSectionHeader("Remove Health Connect meals")
    OptionsItem("Start date", trailingText = cleanupStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) { cleanupStartPicker.show() }
    OptionsItem("End date", trailingText = cleanupEndDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) { cleanupEndPicker.show() }
    OptionsItem("Cleanup mode", trailingText = uiState.healthConnectNutritionCleanupMode.label) {
        val modes = HealthConnectCleanupMode.entries
        val current = modes.indexOf(uiState.healthConnectNutritionCleanupMode).coerceAtLeast(0)
        viewModel.updateHealthConnectNutritionCleanupMode(modes[(current + 1) % modes.size])
    }
    OptionsItem(text = if (uiState.historicalMealImportInProgress) "Previewing records..." else "Preview records to remove") {
        onHealthConnectAction { viewModel.previewHealthConnectNutritionRange() }
    }
    cleanupPreview?.let { preview ->
        SheetNote(
            "Preview: ${preview.total} total. Historical ${preview.historicalImports}, Add Meal ${preview.addMeal}, legacy daily totals ${preview.legacyDailyTotals}.",
            isError = false,
        )
    }
    OptionsItem(
        text = if (uiState.historicalMealImportInProgress) {
            "Removal in progress..."
        } else if (cleanupPreview == null) {
            "Preview before removing"
        } else {
            "Remove ${cleanupPreview.total} previewed records"
        },
    ) {
        if (cleanupPreview != null) onConfirmCleanup() else viewModel.previewHealthConnectNutritionRange()
    }
    uiState.historicalMealImportMessage?.let { message -> SheetNote(message, isError = false) }
}

@Composable
private fun ImportToolsSheetContent(
    uiState: AppUiState,
    viewModel: AppViewModel,
    fileLaunchers: AppFileLaunchers,
    onHealthConnectAction: (() -> Unit) -> Unit,
) {
    SheetTitle("Import tools", "Bring historical meal rows into Health Connect when you need the bigger hammer.")
    OptionsItem("Preview historical meal CSV") {
        fileLaunchers.historicalMealImporter.launch(arrayOf("text/*", "text/comma-separated-values"))
    }
    uiState.historicalMealImportPreview?.let { preview ->
        SheetNote(
            listOfNotNull(
                "${preview.validRows} foods",
                "${preview.mealCount} meals",
                "${preview.skippedRows} skipped",
                preview.startDate?.let { start -> preview.endDate?.let { end -> "$start to $end" } },
            ).joinToString(" | "),
        )
        if (preview.issues.isNotEmpty()) {
            SheetNote(preview.issues.take(3).joinToString("\n") { "Row ${it.rowNumber}: ${it.message}" }, isError = true)
        }
        OptionsItem(text = if (uiState.historicalMealImportInProgress) "Writing historical meals..." else "Write historical meals to Health Connect") {
            onHealthConnectAction { viewModel.writeHistoricalMealImport() }
        }
    }
}

@Composable
private fun ThemeSheetContent(uiState: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    SheetTitle("Appearance", "Pick the theme that feels best for daily logging.")
    SelectableOptionsItem(
        text = stringArrayResource(R.array.dark_mode_options)[0],
        selected = uiState.themeUserSetting == AppTheme.MODE_AUTO,
    ) {
        viewModel.setTheme(AppTheme.MODE_AUTO, context)
        viewModel.updateActiveSettingsSheet(null)
    }
    SelectableOptionsItem(
        text = stringArrayResource(R.array.dark_mode_options)[1],
        selected = uiState.themeUserSetting == AppTheme.MODE_DAY,
    ) {
        viewModel.setTheme(AppTheme.MODE_DAY, context)
        viewModel.updateActiveSettingsSheet(null)
    }
    SelectableOptionsItem(
        text = stringArrayResource(R.array.dark_mode_options)[2],
        selected = uiState.themeUserSetting == AppTheme.MODE_NIGHT,
    ) {
        viewModel.setTheme(AppTheme.MODE_NIGHT, context)
        viewModel.updateActiveSettingsSheet(null)
    }
}

@Composable
private fun MaintenanceSheetContent(viewModel: AppViewModel, fileLaunchers: AppFileLaunchers) {
    SheetTitle("Troubleshooting tools", "Database and archive utilities live here so they stay out of the daily workflow.")
    OptionsSectionHeader("Database tools")
    OptionsItem(stringResource(R.string.dropdown_import_database) + " (*.csv)") { viewModel.setAlertDialogDatabaseImport(true) }
    OptionsItem(stringResource(R.string.dropdown_backup_database) + " (*.csv)") { fileLaunchers.databaseExporter.launch("database_backup.csv") }
    OptionsItem(stringResource(R.string.dropdown_reset_database)) { viewModel.setAlertDialogDatabaseReset(true) }
    OptionsSectionHeader("Archive tools")
    OptionsItem(stringResource(R.string.dropdown_import_archive) + " (*.csv)") { viewModel.setAlertDialogArchiveImport(true) }
    OptionsItem(stringResource(R.string.dropdown_backup_archive) + " (*.csv)") { fileLaunchers.archiveExporter.launch("archive_backup.csv") }
    OptionsItem(stringResource(R.string.dropdown_clear_archive)) { viewModel.setAlertDialogArchiveReset(true) }
}

@Composable
private fun SettingsCards(
    uiState: AppUiState,
    viewModel: AppViewModel,
    currentThemeLabel: String,
    healthStatus: String,
    onHealthConnectAction: (() -> Unit) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            )
        }
        item {
            SettingsHubCard(
                title = stringResource(R.string.health_connect),
                subtitle = "Daily writes, historical imports, and cleanup.",
                meta = healthStatus,
                emphasized = true,
            ) {
                OptionsItem(
                    text = stringResource(R.string.dropdown_sync_health_connect),
                    trailingContent = {
                        Switch(
                            checked = uiState.healthConnectSyncEnabled && uiState.healthConnectPermissionsGranted,
                            enabled = uiState.healthConnectPermissionsGranted,
                            onCheckedChange = null,
                        )
                    },
                    onClick = { onHealthConnectAction { viewModel.toggleHealthConnectSyncEnabled(context) } },
                )
                OptionsItem(
                    text = stringResource(R.string.health_connect_notifications),
                    trailingContent = {
                        Switch(
                            checked = uiState.healthConnectToastsEnabled && uiState.healthConnectPermissionsGranted,
                            enabled = uiState.healthConnectPermissionsGranted,
                            onCheckedChange = null,
                        )
                    },
                    onClick = { onHealthConnectAction { viewModel.toggleHealthConnectToastsEnabled(context) } },
                )
                OptionsItem("Manage Health Connect data") { viewModel.updateActiveSettingsSheet(SettingsSheet.HealthData) }
            }
        }
        item {
            SettingsHubCard(
                title = "Import tools",
                subtitle = "Bulk historical meals and Health Connect writes.",
                meta = uiState.historicalMealImportPreview?.let { "${it.validRows} foods ready" } ?: "No CSV loaded",
            ) {
                OptionsItem("Open import tools") { viewModel.updateActiveSettingsSheet(SettingsSheet.ImportTools) }
            }
        }
        item {
            SettingsHubCard(
                title = "App preferences",
                subtitle = "Keep the everyday app comfortable.",
                meta = currentThemeLabel,
            ) {
                OptionsItem(
                    text = stringResource(R.string.dark_mode),
                    trailingText = currentThemeLabel,
                    onClick = { viewModel.updateActiveSettingsSheet(SettingsSheet.Theme) },
                )
            }
        }
        item {
            SettingsHubCard(
                title = "Troubleshooting",
                subtitle = "Database, archive, and project links.",
                meta = "Advanced",
            ) {
                OptionsItem("Database and archive tools") { viewModel.updateActiveSettingsSheet(SettingsSheet.Maintenance) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
