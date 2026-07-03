package com.makstuff.minimalistcaloriecounter

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.essentials.ALPHABET
import com.makstuff.minimalistcaloriecounter.essentials.NAV_CREATE
import com.makstuff.minimalistcaloriecounter.essentials.NAV_DATABASE
import com.makstuff.minimalistcaloriecounter.essentials.GENERAL_WEIGHTS
import com.makstuff.minimalistcaloriecounter.essentials.NavControllerListener
import com.makstuff.minimalistcaloriecounter.essentials.NavButton
import com.makstuff.minimalistcaloriecounter.essentials.toBodyWeight
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppBottomBar
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppMainDrawer
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import com.makstuff.minimalistcaloriecounter.ui.navigation.navigateApp
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonGrid
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonText
import com.makstuff.minimalistcaloriecounter.ui.reused.DropdownMenu
import com.makstuff.minimalistcaloriecounter.ui.reused.Grid
import com.makstuff.minimalistcaloriecounter.ui.reused.ScrollColumn
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetNote
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetTitle
import com.makstuff.minimalistcaloriecounter.ui.reused.TextField
import com.makstuff.minimalistcaloriecounter.ui.reused.TileArchive
import com.makstuff.minimalistcaloriecounter.ui.reused.TileIngredient
import com.makstuff.minimalistcaloriecounter.ui.reused.TileLegendArchive
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenDatabaseEntry
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenGoals
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenHealthConnectNutrition
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenEnterWeightOfFood
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenInputOrEditArchive
import com.makstuff.minimalistcaloriecounter.ui.screens.DestinationDialog
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenQuickImport
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenShowFoodAll
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenShowFoodSelection
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenWithHoverCard
import com.makstuff.minimalistcaloriecounter.ui.settings.OptionsItem
import com.makstuff.minimalistcaloriecounter.ui.settings.OptionsSectionHeader
import com.makstuff.minimalistcaloriecounter.ui.settings.SelectableOptionsItem
import com.makstuff.minimalistcaloriecounter.ui.settings.SettingsHubCard
import com.makstuff.minimalistcaloriecounter.ui.settings.SettingsSheet
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupMode
import com.makstuff.minimalistcaloriecounter.health.DayCheckInExporter
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val AccentMenu = Color(0xFF90CAF9)
private val AccentSettings = Color(0xFFFFD166)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    viewModel: AppViewModel,
    uiState: AppUiState,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    context.findActivity() // Use the proper unwrap function!
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    var mainMenuExpanded by remember { mutableStateOf(false) }
    val fileLaunchers = rememberAppFileLaunchers(viewModel)

    val healthConnectRequestPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { _ ->
            viewModel.updateHealthConnectPermissionsStatus()
        }
    )
    val healthConnectExportPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { _ ->
            viewModel.updateHealthConnectPermissionsStatus()
        }
    )

    fun navTo(route: String) {
        keyboardController?.hide()
        navController.navigateApp(route)
    }

    LaunchedEffect(uiState.automationRouteRequest) {
        val route = uiState.automationRouteRequest ?: return@LaunchedEffect
        navTo(route)
        viewModel.clearNavigationRequest(route)
    }

    fun navBack() {
        keyboardController?.hide()
        navController.popBackStack()
    }

    fun editDatabaseEntry(index: Int) {
        viewModel.updateDatabaseEntryEditName(uiState.database[index].name)
        viewModel.updateDatabaseEntryEditAllNutrients(
            uiState.database[index].nutrients.stringValues(
                true
            ).toMutableStateList()
        )
        viewModel.updateDatabaseEntryEditCustomWeights(uiState.database[index].customWeights.inputString)
        viewModel.updateDatabaseEntryEditQuickselect(uiState.database[index].quickselect)
        navTo(AppRoutes.databaseEditEntry(index))
    }

    fun setNav(string: String, button: NavButton) {
        viewModel.updateTopBarTitle(string)
        viewModel.updateNavigationBarHighlight(button)
    }
    //lambda argument moved out of parenthesis because good practice and whatnot
    NavControllerListener(
        nameFoodDayAdd = uiState.nameFoodDayAdd,
        nameFoodDayEdit = uiState.nameFoodDayEdit,
        navController = navController,
        context = context,
        setNav = { string, button -> setNav(string, button) }
    )

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateHealthConnectPermissionsStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    LaunchedEffect(Unit) {
        viewModel.updateHealthConnectPermissionsStatus()
        viewModel.databaseResetCSV(false, context)
        viewModel.archiveResetCSV(false, context)
        viewModel.dayResetCSV(false, context)
        viewModel.optionsResetFile(false, context)
        viewModel.goalsResetCSV(false, context)

        try {
            viewModel.optionsUpdateFromFile(context)
        } catch (e: IllegalStateException) {
            Toast.makeText(
                context, context.getString(R.string.options) + " CSV: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
        try {
            viewModel.archiveUpdateFromCSV(context)
        } catch (e: IllegalStateException) {
            Toast.makeText(
                context, context.getString(R.string.archive) + " CSV: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
        try {
            viewModel.databaseUpdateFromCSV(context)
        } catch (e: IllegalStateException) {
            Toast.makeText(
                context, context.getString(R.string.database)  + " CSV: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
        try {
            viewModel.dayUpdateFromCSV(context)
        } catch (e: IllegalStateException) {
            Toast.makeText(
                context, context.getString(R.string.day) + " CSV: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
        try {
            viewModel.goalsUpdateFromCSV(context)
        } catch (e: IllegalStateException) {
            Toast.makeText(
                context, "Goals CSV: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
        try {
            viewModel.quickImportOutboxUpdateFromCSV(context)
        } catch (e: IllegalStateException) {
            Toast.makeText(
                context, "Add Meal outbox CSV: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
        delay(1000.milliseconds)//1000 seems enough to prevent glitches from dark mode override loading
        viewModel.setLoadingToFalse()
    }
    @Composable
    fun SettingsPageContent() {
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
                if (uiState.healthConnectPermissionsGranted) {
                    onSuccess()
                } else {
                    viewModel.setAlertDialogHealthConnectPermissions(true)
                }
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
                    ButtonText(
                        text = stringResource(R.string.button_cancel),
                        onClick = { historicalCleanupConfirmVisible = false },
                    )
                },
                title = { Text(stringResource(R.string.confirmation)) },
                text = {
                    Text(
                        "Remove ${cleanupPreview?.total ?: 0} ${uiState.healthConnectNutritionCleanupMode.label.lowercase()} records from ${cleanupStartDate} through ${cleanupEndDate}?"
                    )
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
                        SettingsSheet.HealthData -> {
                            SheetTitle("Manage Health Connect data", "Sync, import, and clean up nutrition records written by this app.")
                            OptionsItem(stringResource(R.string.dropdown_export_archive_health_connect)) {
                                viewModel.updateActiveSettingsSheet(null)
                                handleHCInteraction { viewModel.setAlertDialogHealthConnectSync(true) }
                            }
                            OptionsSectionHeader("Export from Health Connect")
                            OptionsItem("Export start", trailingText = exportStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                                exportStartPicker.show()
                            }
                            OptionsItem("Export end", trailingText = exportEndDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                                exportEndPicker.show()
                            }
                            OptionsItem("Export mode", trailingText = uiState.healthConnectExportMode.label) {
                                val modes = HealthConnectExportMode.entries
                                val current = modes.indexOf(uiState.healthConnectExportMode).coerceAtLeast(0)
                                viewModel.updateHealthConnectExportMode(modes[(current + 1) % modes.size])
                            }
                            OptionsItem(
                                "Redacted for ChatGPT",
                                trailingText = if (uiState.healthConnectExportRedacted) "On" else "Off",
                            ) {
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
                                    healthConnectExportPermissionLauncher.launch(
                                        healthConnectManager.exportPermissionsFor(uiState.healthConnectExportMode)
                                    )
                                }
                            }
                            uiState.healthConnectExportMessage?.let { message ->
                                SheetNote(message, isError = message.contains("failed", ignoreCase = true) || message.contains("missing", ignoreCase = true))
                            }
                            OptionsSectionHeader("Remove Health Connect meals")
                            OptionsItem("Start date", trailingText = cleanupStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                                cleanupStartPicker.show()
                            }
                            OptionsItem("End date", trailingText = cleanupEndDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                                cleanupEndPicker.show()
                            }
                            OptionsItem("Cleanup mode", trailingText = uiState.healthConnectNutritionCleanupMode.label) {
                                val modes = HealthConnectCleanupMode.entries
                                val current = modes.indexOf(uiState.healthConnectNutritionCleanupMode).coerceAtLeast(0)
                                viewModel.updateHealthConnectNutritionCleanupMode(modes[(current + 1) % modes.size])
                            }
                            OptionsItem(
                                text = if (uiState.historicalMealImportInProgress) "Previewing records..." else "Preview records to remove",
                            ) {
                                handleHCInteraction { viewModel.previewHealthConnectNutritionRange() }
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
                                if (cleanupPreview != null) {
                                    historicalCleanupConfirmVisible = true
                                } else {
                                    viewModel.previewHealthConnectNutritionRange()
                                }
                            }
                            uiState.historicalMealImportMessage?.let { message ->
                                SheetNote(message, isError = false)
                            }
                        }
                        SettingsSheet.ImportTools -> {
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
                                    SheetNote(
                                        preview.issues.take(3).joinToString("\n") { "Row ${it.rowNumber}: ${it.message}" },
                                        isError = true,
                                    )
                                }
                                OptionsItem(
                                    text = if (uiState.historicalMealImportInProgress) "Writing historical meals..." else "Write historical meals to Health Connect",
                                ) {
                                    handleHCInteraction { viewModel.writeHistoricalMealImport() }
                                }
                            }
                        }
                        SettingsSheet.Theme -> {
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
                        SettingsSheet.Maintenance -> {
                            SheetTitle("Troubleshooting tools", "Database and archive utilities live here so they stay out of the daily workflow.")
                            OptionsSectionHeader("Database tools")
                            OptionsItem(stringResource(R.string.dropdown_import_database) + " (*.csv)") {
                                viewModel.setAlertDialogDatabaseImport(true)
                            }
                            OptionsItem(stringResource(R.string.dropdown_backup_database) + " (*.csv)") {
                                fileLaunchers.databaseExporter.launch("database_backup.csv")
                            }
                            OptionsItem(stringResource(R.string.dropdown_reset_database)) {
                                viewModel.setAlertDialogDatabaseReset(true)
                            }
                            OptionsSectionHeader("Archive tools")
                            OptionsItem(stringResource(R.string.dropdown_import_archive) + " (*.csv)") {
                                viewModel.setAlertDialogArchiveImport(true)
                            }
                            OptionsItem(stringResource(R.string.dropdown_backup_archive) + " (*.csv)") {
                                fileLaunchers.archiveExporter.launch("archive_backup.csv")
                            }
                            OptionsItem(stringResource(R.string.dropdown_clear_archive)) {
                                viewModel.setAlertDialogArchiveReset(true)
                            }
                        }
                    }
                }
            }
        }

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
                        onClick = { handleHCInteraction { viewModel.toggleHealthConnectSyncEnabled(context) } },
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
                        onClick = { handleHCInteraction { viewModel.toggleHealthConnectToastsEnabled(context) } },
                    )
                    OptionsItem("Manage Health Connect data") {
                        viewModel.updateActiveSettingsSheet(SettingsSheet.HealthData)
                    }
                }
            }
            item {
                SettingsHubCard(
                    title = "Import tools",
                    subtitle = "Bulk historical meals and Health Connect writes.",
                    meta = uiState.historicalMealImportPreview?.let { "${it.validRows} foods ready" } ?: "No CSV loaded",
                ) {
                    OptionsItem("Open import tools") {
                        viewModel.updateActiveSettingsSheet(SettingsSheet.ImportTools)
                    }
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
                    OptionsItem("Database and archive tools") {
                        viewModel.updateActiveSettingsSheet(SettingsSheet.Maintenance)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    if (uiState.quickImportSettingsVisible) {
        DestinationDialog(
            addDatabase = uiState.quickImportAddFoodsToDatabase,
            addDay = uiState.quickImportAddFoodsToDay,
            writeHealthConnect = uiState.quickImportWriteHealthConnect,
            onToggleAddDatabase = { viewModel.toggleQuickImportAddFoodsToDatabase() },
            onToggleAddDay = { viewModel.toggleQuickImportAddFoodsToDay() },
            onToggleHealthConnect = { viewModel.toggleQuickImportWriteHealthConnect() },
            onDismiss = { viewModel.updateQuickImportSettingsVisible(false) },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.topBarTitle) },
                navigationIcon = {
                    IconButton(onClick = { mainMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation menu",
                            tint = AccentMenu,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    if (currentRoute == AppRoutes.QUICK_IMPORT) {
                        IconButton(onClick = { viewModel.updateQuickImportSettingsVisible(true) }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Add Meal settings",
                                tint = AccentSettings,
                            )
                        }
                    }
                    if (currentRoute == AppRoutes.GOALS_HOME) {
                        IconButton(onClick = { viewModel.updateGoalsSettingsVisible(true) }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Goal settings",
                                tint = AccentSettings,
                            )
                        }
                    }
                    AppConfirmationDialogs(
                        uiState = uiState,
                        viewModel = viewModel,
                        context = context,
                        fileLaunchers = fileLaunchers,
                        healthConnectManager = healthConnectManager,
                        healthConnectRequestPermissionLauncher = healthConnectRequestPermissionLauncher,
                        onNavigate = { navTo(it) },
                        onNavigateBack = { navBack() },
                    )

                }
            )
        },
        bottomBar = {
            AppBottomBar(
                navigationBarHighlight = uiState.navigationBarHighlight,
                onNavigate = { navTo(it) },
            )
        },
    ) { innerPadding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(4.dp),
            navController = navController,
            startDestination = AppRoutes.QUICK_IMPORT,
        ) {
            composable(AppRoutes.DAY_CONTENT) {
                ScreenWithHoverCard(
                    contentAbove = {},
                    nutrients = uiState.day.overallNutrients,
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_reset_day)) { viewModel.setAlertDialogDayReset(true) },
                        Pair(stringResource(R.string.button_add_food)) { navTo(AppRoutes.DAY_HOME) },
                        Pair(stringResource(R.string.button_turn_to_archive_entry)) {
                            try {
                                viewModel.updateArchiveEntryDate(
                                    LocalDateTime.now().minusHours(12).toLocalDate()
                                )
                                viewModel.updateArchiveEntryBodyWeight("")
                                viewModel.updateArchiveEntryAllNutrients(
                                    uiState.day.overallNutrients.stringValues(
                                        true
                                    ).toMutableStateList()
                                )
                                navTo(AppRoutes.ARCHIVE_CREATE_ENTRY_FROM_DAY)
                            } catch (e: IllegalStateException) {
                                Toast.makeText(
                                    context, e.message, Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                    ),
                    content = {
                        ScrollColumn(
                            items = uiState.day.components.mapIndexed { index: Int, component ->
                                {
                                    TileIngredient(
                                        component = component,
                                        onClick = {
                                            viewModel.updateCurrentComboComponentWeight(
                                                component.first.toFormattedString(
                                                    true
                                                )
                                            )
                                            viewModel.setNameFoodDayEdit(component.second.name)
                                            navTo(AppRoutes.dayEditWeight(index))
                                        },
                                    )
                                }
                            },
                        )
                    },
                    context=context
                )
            }
            composable(AppRoutes.ARCHIVE_HOME) {
                    ScreenWithHoverCard(
                        nutrients = uiState.archive.averageNutrients,
                        contentAbove = { },
                        listOfTextButtons = listOf(
                            Pair(stringResource(R.string.button_create_entry_manually)) {
                                viewModel.resetArchiveEntryAllInput()
                                navTo(AppRoutes.ARCHIVE_CREATE_ENTRY_MANUALLY)},
                        ),
                        content = {
                            Column {
                                TileLegendArchive()
                                ScrollColumn(items = uiState.archive.entries.mapIndexed { index: Int, archiveEntry ->
                                    {
                                        TileArchive(
                                            archiveEntry = archiveEntry,
                                            onClick = {
                                                viewModel.updateArchiveEntryDate(uiState.archive.entries[index].first)
                                                viewModel.updateArchiveEntryBodyWeight(uiState.archive.entries[index].second.toBodyWeight())
                                                viewModel.updateArchiveEntryAllNutrients(
                                                    uiState.archive.entries[index].third.stringValues(
                                                        true
                                                    ).toMutableList()
                                                )
                                                navTo(AppRoutes.archiveEditEntry(index))
                                            },
                                        )
                                    }
                                })
                            }
                        },
                        context=context
                    )
                }

            composable(AppRoutes.HEALTH_CONNECT_NUTRITION) {
                ScreenHealthConnectNutrition(
                    uiState = uiState,
                    onDateChange = { viewModel.updateHealthConnectViewerDate(it) },
                    onRefresh = { viewModel.readHealthConnectNutritionMeals() },
                    onDeleteMeal = { viewModel.deleteHealthConnectNutritionMeal(it) },
                    onDeleteMealGroup = { viewModel.deleteHealthConnectNutritionMeals(it) },
                    onRepeatMealGroup = {
                        viewModel.prepareQuickImportRepeat(it)
                        navController.navigate(AppRoutes.QUICK_IMPORT) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onExportDaySummary = { date, summary ->
                        runCatching { DayCheckInExporter(context).export(date, summary) }
                            .onSuccess { Toast.makeText(context, "Exported check-in to $it", Toast.LENGTH_LONG).show() }
                            .onFailure { Toast.makeText(context, "Check-in export failed: ${it.message}", Toast.LENGTH_LONG).show() }
                    },
                )
            }

            composable(AppRoutes.GOALS_HOME) {
                ScreenGoals(
                    uiState = uiState,
                    onSettingsDismiss = { viewModel.updateGoalsSettingsVisible(false) },
                    onRefreshHealthConnect = { viewModel.refreshGoalsFromHealthConnect() },
                    onRecalculate = { viewModel.recalculateGoalRecommendation() },
                    onApplyRecommendation = { viewModel.applyGoalRecommendation() },
                    onDismissRecommendation = { viewModel.dismissGoalRecommendation() },
                    onBirthdayChange = { viewModel.updateGoalBirthday(it) },
                    onSexChange = { viewModel.updateGoalSex(it) },
                    onActivityLevelChange = { viewModel.updateGoalActivityLevel(it) },
                    onWeightLossTargetChange = { viewModel.updateGoalWeightLossTarget(it) },
                    onMeasurementChange = { field, value -> viewModel.updateGoalMeasurement(field, value) },
                    onMeasurementLockToggle = { viewModel.toggleGoalMeasurementLock(it) },
                    onMacroChange = { macro, value -> viewModel.updateGoalMacro(macro, value) },
                    onMacroLockToggle = { viewModel.toggleGoalMacroLock(it) },
                )
            }

            composable(AppRoutes.SETTINGS_HOME) {
                SettingsPageContent()
            }
            


            composable(AppRoutes.ARCHIVE_CREATE_ENTRY_MANUALLY) {
                fun onConfirm() {
                    keyboardController?.hide()
                    try {
                        viewModel.archiveAddEntry(
                            date = uiState.inputArchiveEntryDate,
                            bodyWeight = uiState.inputArchiveEntryBodyWeight,
                            nutrients = Nutrients.fromStrings(uiState.inputArchiveEntryNutrients,context),
                            context = context
                        )
                        navTo(AppRoutes.ARCHIVE_HOME)
                    } catch (e: IllegalStateException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                }
                ScreenInputOrEditArchive(
                    inputBodyWeight = uiState.inputArchiveEntryBodyWeight,
                    onUpdateBodyWeight = { string ->
                        viewModel.updateArchiveEntryBodyWeight(string)
                    },
                    inputNutrients = uiState.inputArchiveEntryNutrients,
                    onUpdateNutrient = { string, int ->
                        viewModel.updateArchiveEntryNutrient(string, int)
                    },
                    inputDate = uiState.inputArchiveEntryDate,
                    onUpdateDate = { date ->
                        viewModel.updateArchiveEntryDate(date)
                    },
                    onConfirm = { onConfirm() },
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_cancel)) { navTo(AppRoutes.ARCHIVE_HOME) },
                        Pair(stringResource(R.string.button_create_new_archive_entry)) { onConfirm() }
                    )
                )
            }

            composable(AppRoutes.ARCHIVE_CREATE_ENTRY_FROM_DAY) {
                fun onConfirm() {
                    keyboardController?.hide()
                    try {
                        viewModel.archiveAddEntry(
                            date = uiState.inputArchiveEntryDate,
                            bodyWeight = uiState.inputArchiveEntryBodyWeight,
                            nutrients = Nutrients.fromStrings(uiState.inputArchiveEntryNutrients,context),
                            context = context
                        )
                        viewModel.dayReset(context)
                        navTo(AppRoutes.ARCHIVE_HOME)
                    } catch (e: IllegalStateException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                }
                ScreenInputOrEditArchive(
                    inputBodyWeight = uiState.inputArchiveEntryBodyWeight,
                    onUpdateBodyWeight = { string ->
                        viewModel.updateArchiveEntryBodyWeight(string)
                    },
                    inputNutrients = uiState.inputArchiveEntryNutrients,
                    onUpdateNutrient = { string, int ->
                        viewModel.updateArchiveEntryNutrient(string, int)
                    },
                    inputDate = uiState.inputArchiveEntryDate,
                    onUpdateDate = { date ->
                        viewModel.updateArchiveEntryDate(date)
                    },
                    onConfirm = { onConfirm() },
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_cancel)) { navTo(AppRoutes.DAY_HOME) },
                        Pair(stringResource(R.string.button_turn_day_to_archive_entry)) { onConfirm() }
                    )
                )
            }



            composable(AppRoutes.ARCHIVE_EDIT_ENTRY) {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.archive.entries.size){
                        fun onConfirm() {
                            keyboardController?.hide()
                            try {
                                viewModel.archiveEditEntry(
                                    index = index,
                                    date = uiState.inputArchiveEntryDate,
                                    bodyWeight = uiState.inputArchiveEntryBodyWeight,
                                    nutrients = Nutrients.fromStrings(uiState.inputArchiveEntryNutrients,context),
                                    context = context
                                )
                                navTo(AppRoutes.ARCHIVE_HOME)
                            } catch (e: IllegalStateException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                        ScreenInputOrEditArchive(
                            inputBodyWeight = uiState.inputArchiveEntryBodyWeight,
                            onUpdateBodyWeight = { string ->
                                viewModel.updateArchiveEntryBodyWeight(string)
                            },
                            inputNutrients = uiState.inputArchiveEntryNutrients,
                            onUpdateNutrient = { string, int ->
                                viewModel.updateArchiveEntryNutrient(string, int)
                            },
                            inputDate = uiState.inputArchiveEntryDate,
                            onUpdateDate = { date ->
                                viewModel.updateArchiveEntryDate(date)
                            },
                            onConfirm = { onConfirm() },
                            listOfTextButtons = listOf(
                                Pair(stringResource(R.string.button_cancel)) {
                                    navTo(AppRoutes.ARCHIVE_HOME)
                                },
                                Pair(stringResource(R.string.button_delete)) {
                                    viewModel.setAlertDialogArchiveDelete(true, index)
                                },
                                Pair(stringResource(R.string.button_save_changes)) { onConfirm() }
                            )
                        )
                    }
                }}

            composable(AppRoutes.CREATE_HOME) {
                fun onCreateFood() {
                    keyboardController?.hide()
                    try {
                        viewModel.databaseCreateEntryFromInput(context)
                        navTo(AppRoutes.DAY_HOME)
                    } catch (e: IllegalStateException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                }
                ScreenDatabaseEntry(
                    inputName = uiState.inputDatabaseEntryCreateName,
                    inputNutrients = uiState.inputDatabaseEntryCreateNutrients,
                    inputQuickSelectBoolean = uiState.inputDatabaseEntryCreateQuickselect,
                    inputQuickSelectWeights = uiState.inputDatabaseEntryCreateCustomWeights,
                    onUpdateName = { string ->
                        viewModel.updateDatabaseEntryCreateName(string)
                    },
                    onUpdateNutrient = { string, index ->
                        viewModel.updateDatabaseEntryCreateNutrient(string, index)
                    },
                    onUpdateQuickSelectWeights = { string ->
                        viewModel.updateDatabaseEntryCreateCustomWeights(string)
                    },
                    onConfirm = { onCreateFood() },
                    onToggleSwitch = { viewModel.toggleDatabaseEntryCreateQuickselect() },
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_cancel)) {
                            viewModel.resetDatabaseEntryCreateAllInput()
                            navTo(AppRoutes.DAY_HOME)
                        },
                        Pair(stringResource(R.string.button_clear_input)) { viewModel.resetDatabaseEntryCreateAllInput() },
                        Pair(stringResource(R.string.button_create)) { onCreateFood() }
                    ),
                    context=context
                )
            }

            composable(AppRoutes.DATABASE_EDIT_ENTRY) {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.database.size){
                        fun onConfirmEdit() {
                            keyboardController?.hide()
                            try {
                                viewModel.databaseEditEntryFromInput(index, context)
                                navBack()
                            } catch (e: IllegalStateException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                        ScreenDatabaseEntry(
                            inputName = uiState.inputDatabaseEntryEditName,
                            inputNutrients = uiState.inputDatabaseEntryEditNutrients,
                            inputQuickSelectBoolean = uiState.inputDatabaseEntryEditQuickselect,
                            inputQuickSelectWeights = uiState.inputDatabaseEntryEditCustomWeights,
                            onUpdateName = { string ->
                                viewModel.updateDatabaseEntryEditName(string)
                            },
                            onUpdateNutrient = { string, ind ->
                                viewModel.updateDatabaseEntryEditNutrient(string, ind)
                            },
                            onUpdateQuickSelectWeights = { string ->
                                viewModel.updateDatabaseEntryEditCustomWeights(string)
                            },
                            onConfirm = { onConfirmEdit() },
                            onToggleSwitch = {
                                viewModel.toggleDatabaseEntryEditQuickselect()
                            },
                            listOfTextButtons = listOf(
                                Pair(stringResource(R.string.button_cancel)) { navBack() },
                                Pair(stringResource(R.string.button_delete)) {
                                    viewModel.setAlertDialogDatabaseDelete(true, index)
                                },
                                Pair(stringResource(R.string.button_save_changes)) {
                                    onConfirmEdit()
                                }
                            ),
                            context=context
                        )
                    }
                }}

            composable(AppRoutes.DAY_ADD_FOOD) {
                ScreenShowFoodSelection(
                    indexList = uiState.databaseLetter,
                    database = uiState.database,
                    onFoodClicked = { index ->
                        viewModel.updateCurrentComboComponentWeight("")
                        viewModel.setNameFoodDayAdd(uiState.database[index].name)
                        navTo(AppRoutes.dayAddWeight(index))
                    },
                    onFoodLongClicked = { index -> editDatabaseEntry(index) },
                    onBack = {navTo(AppRoutes.DAY_HOME)}
                )
            }


            composable(AppRoutes.DATABASE_HOME) {
                ScreenShowFoodAll(
                    database = uiState.database,
                    onFoodClicked = { index ->
                        editDatabaseEntry(index)
                    },
                    onFoodLongClicked = { index -> editDatabaseEntry(index) }
                )
            }

            composable(AppRoutes.DAY_HOME) {
                ScreenWithHoverCard(
                    contentAbove = {},
                    nutrients = uiState.day.overallNutrients,
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_reset_day)) { viewModel.setAlertDialogDayReset(true) },
                        Pair(stringResource(R.string.button_edit)) { navTo(AppRoutes.DAY_CONTENT) },
                        Pair(stringResource(R.string.button_turn_to_archive_entry)) {
                            viewModel.updateArchiveEntryDate(
                                LocalDateTime.now().minusHours(12).toLocalDate()
                            )
                            viewModel.updateArchiveEntryBodyWeight("")
                            viewModel.updateArchiveEntryAllNutrients(
                                uiState.day.overallNutrients.stringValues(
                                    true
                                ).toMutableStateList()
                            )
                            navTo(AppRoutes.ARCHIVE_CREATE_ENTRY_FROM_DAY)
                        },
                    ),
                    content = {
                        Grid(
                            modifier = Modifier.fillMaxHeight(),
                            columns = 8,
                            reverseUpDown = true,
                            reverseLeftRight = true,
                            items = ALPHABET.map {
                                Pair<Int, @Composable () -> Unit>(1) {
                                    ButtonGrid(
                                        text = it.toString(),
                                        onClick = {
                                            viewModel.databaseLetterFilter(it)
                                            navTo(AppRoutes.DAY_ADD_FOOD) }
                                    )
                                }
                            }.reversed() + uiState.databaseQuickselect.map {
                                Pair<Int, @Composable () -> Unit>(2) {
                                    ButtonGrid(
                                        text = it.second.name,
                                        onClick = {
                                            viewModel.setNameFoodDayAdd(it.second.name)
                                            viewModel.updateCurrentComboComponentWeight("")
                                            navTo(AppRoutes.dayAddWeight(it.first))
                                        },
                                        onLongClick = {
                                            editDatabaseEntry(it.first)
                                        }
                                    )
                                }
                            }.reversed()
                        )
                    },
                    context=context
                )
            }

            composable(AppRoutes.QUICK_IMPORT) {
                ScreenQuickImport(
                    uiState = uiState,
                    onTextChange = { viewModel.updateQuickImportText(it) },
                    onToggleAddDatabase = { viewModel.toggleQuickImportAddFoodsToDatabase() },
                    onToggleAddDay = { viewModel.toggleQuickImportAddFoodsToDay() },
                    onToggleHealthConnect = { viewModel.toggleQuickImportWriteHealthConnect() },
                    onRefreshDateTime = { viewModel.refreshQuickImportDateTime() },
                    onDateTimeChange = { viewModel.updateQuickImportDateTime(it) },
                    onMealTypeChange = { viewModel.updateQuickImportMealTypeOverride(it) },
                    onImport = { viewModel.quickImportCommit(context) },
                    onClear = { viewModel.resetQuickImport() },
                    onRetryOutbox = { viewModel.quickImportRetryHealthConnect(context, it) },
                )
            }

            composable(AppRoutes.DAY_ADD_WEIGHT) {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.database.size){
                        fun onConfirm() {
                            keyboardController?.hide()
                            try {
                                viewModel.dayAddFood(
                                    uiState.inputCurrentComboComponentWeight,
                                    uiState.database[index],
                                    context
                                )
                                navTo(AppRoutes.DAY_HOME)
                            } catch (e: IllegalStateException) {
                                Toast.makeText(
                                    context, e.message, Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        ScreenEnterWeightOfFood(
                            currentWeight = uiState.inputCurrentComboComponentWeight,
                            onWeightChange = { string ->
                                viewModel.updateCurrentComboComponentWeight(string)
                            },
                            onConfirm = { onConfirm() },
                            listOfTextButtons = listOf(
                                Pair(stringResource(R.string.button_cancel)) { navTo(AppRoutes.DAY_HOME) },
                                Pair(stringResource(R.string.button_add_to_day)) {
                                    onConfirm()
                                }
                            ),
                            listOfItems = GENERAL_WEIGHTS.map { list ->
                                Pair<Int, @Composable () -> Unit>(1) {
                                    ButtonGrid(
                                        text = list.second,
                                        onClick = {
                                            keyboardController?.hide()
                                            viewModel.dayAddFood(
                                                list.first,
                                                uiState.database[index],
                                                context
                                            )
                                            navTo(AppRoutes.DAY_HOME)
                                        },
                                    )
                                }
                            },
                            listOfQSItems = uiState.database[index].customWeights.listOfStrings.map { list ->
                                Pair<Int, @Composable () -> Unit>(1) {
                                    ButtonGrid(
                                        text = list.second,
                                        onClick = {
                                            keyboardController?.hide()
                                            viewModel.dayAddFood(
                                                list.first,
                                                uiState.database[index],
                                                context
                                            )
                                            navTo(AppRoutes.DAY_HOME)
                                        }
                                    )
                                }
                            }.reversed()
                        )
                    }
                }}

            composable(AppRoutes.DAY_EDIT_WEIGHT) {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.day.components.size){
                        fun onConfirm() {
                            keyboardController?.hide()
                            try {
                                viewModel.dayEditFoodWeight(
                                    uiState.inputCurrentComboComponentWeight,
                                    index,
                                    context
                                )
                                navTo(AppRoutes.DAY_CONTENT)
                            } catch (e: IllegalStateException) {
                                Toast.makeText(
                                    context, e.message, Toast.LENGTH_LONG
                                ).show()
                            }

                        }
                        ScreenEnterWeightOfFood(
                            currentWeight = uiState.inputCurrentComboComponentWeight,
                            onWeightChange = { string ->
                                viewModel.updateCurrentComboComponentWeight(string)
                            },
                            onConfirm = { onConfirm() },
                            listOfTextButtons = listOf(
                                Pair(stringResource(R.string.button_cancel)) { navTo(AppRoutes.DAY_CONTENT) },
                                Pair(stringResource(R.string.button_delete)) {
                                    viewModel.dayDeleteFood(index, context)
                                    navTo(AppRoutes.DAY_CONTENT)
                                },
                                Pair(stringResource(R.string.button_save_new_weight)) {
                                    onConfirm()
                                }
                            ),
                            listOfItems = remember {
                                GENERAL_WEIGHTS.map { list ->
                                    Pair<Int, @Composable () -> Unit>(1) {
                                        ButtonGrid(
                                            text = list.second,
                                            onClick = {
                                                keyboardController?.hide()
                                                viewModel.dayEditFoodWeight(list.first, index, context)
                                                navTo(AppRoutes.DAY_HOME)
                                            },
                                        )
                                    }
                                }
                            },
                            listOfQSItems = remember {
                                uiState.day.components[index].second.customWeights.listOfStrings.map { list ->
                                    Pair<Int, @Composable () -> Unit>(1) {
                                        ButtonGrid(
                                            text = list.second,
                                            onClick = {
                                                keyboardController?.hide()
                                                viewModel.dayEditFoodWeight(
                                                    list.first,
                                                    index,
                                                    context
                                                )
                                                navTo(AppRoutes.DAY_HOME)
                                            }
                                        )
                                    }
                                }.reversed()
                            }
                        )
                    }
                }
            }

        }
    }

        if (mainMenuExpanded) AppMainDrawer(onDismiss = { mainMenuExpanded = false }, onNavigate = { navTo(it) })
    }

    HealthConnectSyncDialogs(
        uiState = uiState,
        onFinish = { viewModel.finishHealthConnectSync() },
        onCancel = { viewModel.cancelHealthConnectSync() },
        onDismissError = { viewModel.dismissHealthConnectSyncError() },
    )
}
