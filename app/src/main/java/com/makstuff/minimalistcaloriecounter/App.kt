package com.makstuff.minimalistcaloriecounter

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.essentials.ALPHABET
import com.makstuff.minimalistcaloriecounter.essentials.NAV_ARCHIVE
import com.makstuff.minimalistcaloriecounter.essentials.NAV_COMBINE
import com.makstuff.minimalistcaloriecounter.essentials.NAV_CREATE
import com.makstuff.minimalistcaloriecounter.essentials.NAV_DATABASE
import com.makstuff.minimalistcaloriecounter.essentials.NAV_DAY
import com.makstuff.minimalistcaloriecounter.essentials.GENERAL_WEIGHTS
import com.makstuff.minimalistcaloriecounter.essentials.NavControllerListener
import com.makstuff.minimalistcaloriecounter.essentials.NavButton
import com.makstuff.minimalistcaloriecounter.essentials.toBodyWeight
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonGrid
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonText
import com.makstuff.minimalistcaloriecounter.ui.reused.DropdownMenu
import com.makstuff.minimalistcaloriecounter.ui.reused.DropdownMenuItemData
import com.makstuff.minimalistcaloriecounter.ui.reused.Grid
import com.makstuff.minimalistcaloriecounter.ui.reused.NavigationBar
import com.makstuff.minimalistcaloriecounter.ui.reused.NavigationBarItem
import com.makstuff.minimalistcaloriecounter.ui.reused.NavigationBarItemData
import com.makstuff.minimalistcaloriecounter.ui.reused.ScrollColumn
import com.makstuff.minimalistcaloriecounter.ui.reused.TextField
import com.makstuff.minimalistcaloriecounter.ui.reused.TileArchive
import com.makstuff.minimalistcaloriecounter.ui.reused.TileIngredient
import com.makstuff.minimalistcaloriecounter.ui.reused.TileLegendArchive
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenDatabaseEntry
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenEnterWeightOfFood
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenInputOrEditArchive
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenShowFoodAll
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenShowFoodSelection
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenWithHoverCard
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDateTime

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
    val healthConnectManager = remember { HealthConnectManager(context) }

    val healthConnectRequestPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { _ ->
            viewModel.updateHealthConnectPermissionsStatus()
        }
    )

    fun navTo(route: String) {
        navController.navigate(route)
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
        navTo("database_edit_entry/$index")
    }

    fun setNav(string: String, button: NavButton) {
        viewModel.updateTopBarTitle(string)
        viewModel.updateNavigationBarHighlight(button)
    }
    //lambda argument moved out of parenthesis because good practice and whatnot
    NavControllerListener(
        nameFoodCombineAdd = uiState.nameFoodCombineAdd,
        nameFoodCombineEdit = uiState.nameFoodCombineEdit,
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
        viewModel.currentComboResetCSV(false, context)
        viewModel.archiveResetCSV(false, context)
        viewModel.dayResetCSV(false, context)
        viewModel.optionsResetFile(false, context)

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
            viewModel.currentComboUpdateFromCSV(context)
        } catch (e: IllegalStateException) {
            Toast.makeText(
                context, context.getString(R.string.recipe) + " CSV: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
        try {
            viewModel.dayUpdateFromCSV(context)
        } catch (e: IllegalStateException) {
            Toast.makeText(
                context, context.getString(R.string.day) + " CSV: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
        delay(1000.milliseconds)//1000 seems enough to prevent glitches from dark mode override loading
        viewModel.setLoadingToFalse()
    }
    val databaseImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            try {
                uri?.let { context.contentResolver.openInputStream(it) }?.copyTo(
                    File(context.getExternalFilesDir(null), "database.csv")
                        .outputStream()
                )
                viewModel.databaseUpdateFromCSV(context)
                Toast.makeText(
                    context, context.getString(R.string.database) + ": " + context.getString(R.string.import_successful), Toast.LENGTH_LONG
                ).show()
            } catch (e: IllegalStateException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.import_failed) + ": " + e.message, Toast.LENGTH_LONG
                ).show()
            }
        }
    )
    val databaseExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values"),
        onResult = { uri ->
            uri?.let { context.contentResolver.openOutputStream(it) }?.let {
                File(context.getExternalFilesDir(null), "database.csv")
                    .inputStream().copyTo(it)
            }
        }
    )
    val archiveImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            try {
                uri?.let { context.contentResolver.openInputStream(it) }?.copyTo(
                    File(context.getExternalFilesDir(null), "archive.csv")
                        .outputStream()
                )
                viewModel.archiveUpdateFromCSV(context)
                Toast.makeText(
                    context, context.getString(R.string.archive) + ": " + context.getString(R.string.import_successful), Toast.LENGTH_LONG
                ).show()
            } catch (e: IllegalStateException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.import_failed) + ": " + e.message, Toast.LENGTH_LONG
                ).show()
            }
        }
    )
    val archiveExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values"),
        onResult = { uri ->
            uri?.let { context.contentResolver.openOutputStream(it) }?.let {
                File(context.getExternalFilesDir(null), "archive.csv")
                    .inputStream().copyTo(it)
            }
        }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.topBarTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    IconButton(onClick = { viewModel.updateOptionsSheetVisible(true) }) {
                        Icon(painterResource(id = R.drawable.options), stringResource(R.string.options))
                    }
                    when {
                        uiState.alertDialogArchiveReset -> {
                            AlertDialog(
                                onDismissRequest = {viewModel.setAlertDialogArchiveReset(false)},
                                confirmButton = { ButtonText(text = stringResource(R.string.button_continue), onClick = {
                                    viewModel.archiveResetCSV(true, context)
                                    viewModel.archiveUpdateFromCSV(context)
                                    viewModel.setAlertDialogArchiveReset(false)
                                })},
                                dismissButton = { ButtonText(text = stringResource(R.string.button_cancel), onClick =  {viewModel.setAlertDialogArchiveReset(false)})},
                                text = { Text(stringResource(R.string.dialog_archive_clear))},
                                title = {Text(stringResource(R.string.confirmation))})
                        }}
                    when {
                        uiState.alertDialogDatabaseReset -> {
                            AlertDialog(
                                onDismissRequest = {viewModel.setAlertDialogDatabaseReset(false)},
                                confirmButton = { ButtonText(text = stringResource(R.string.button_continue),onClick= {
                                    viewModel.databaseResetCSV(true, context)
                                    viewModel.databaseUpdateFromCSV(context)
                                    viewModel.setAlertDialogDatabaseReset(false)
                                })},
                                dismissButton = { ButtonText(text = stringResource(R.string.button_cancel), onClick= {viewModel.setAlertDialogDatabaseReset(false)})},
                                text = { Text(stringResource(R.string.dialog_database_reset))},
                                title = {Text(stringResource(R.string.confirmation))})
                        }}

                    when {
                        uiState.alertDialogArchiveImport -> {
                            AlertDialog(
                                onDismissRequest = {viewModel.setAlertDialogArchiveImport(false)},
                                confirmButton = { ButtonText(text = stringResource(R.string.button_continue), onClick = {
                                    archiveImporter.launch(arrayOf("text/comma-separated-values"))
                                    viewModel.setAlertDialogArchiveImport(false)
                                })},
                                dismissButton = { ButtonText(text = stringResource(R.string.button_cancel), onClick =  {viewModel.setAlertDialogArchiveImport(false)})},
                                text = { Text(stringResource(R.string.dialog_archive_import))},
                                title = {Text(stringResource(R.string.confirmation))})
                        }}
                    when {
                        uiState.alertDialogDatabaseImport -> {
                            AlertDialog(
                                onDismissRequest = {viewModel.setAlertDialogDatabaseImport(false)},
                                confirmButton = { ButtonText(text = stringResource(R.string.button_continue),onClick= {
                                    databaseImporter.launch(arrayOf("text/comma-separated-values"))
                                    viewModel.setAlertDialogDatabaseImport(false)
                                })},
                                dismissButton = { ButtonText(text = stringResource(R.string.button_cancel), onClick= {viewModel.setAlertDialogDatabaseImport(false)})},
                                text = { Text(stringResource(R.string.dialog_database_import))},
                                title = {Text(stringResource(R.string.confirmation))})
                        }}
                    when {
                        uiState.alertDialogHealthConnectSync -> {
                            AlertDialog(
                                onDismissRequest = {viewModel.setAlertDialogHealthConnectSync(false)},
                                confirmButton = { ButtonText(text = stringResource(R.string.button_continue),onClick= {
                                    viewModel.syncHealthConnect()
                                    viewModel.setAlertDialogHealthConnectSync(false)
                                })},
                                dismissButton = { ButtonText(text = stringResource(R.string.button_cancel), onClick= {viewModel.setAlertDialogHealthConnectSync(false)})},
                                text = { Text(stringResource(R.string.dialog_health_connect_sync))},
                                title = {Text(stringResource(R.string.confirmation))})
                        }}
                    when {
                        uiState.alertDialogHealthConnectPermissions -> {
                            AlertDialog(
                                onDismissRequest = { viewModel.setAlertDialogHealthConnectPermissions(false) },
                                confirmButton = {
                                    ButtonText(text = stringResource(R.string.button_continue), onClick = {
                                        if (uiState.healthConnectAnyPermissionsGranted) {
                                            // Some granted: Take directly to app's permission settings in Health Connect
                                            try {
                                                context.startActivity(Intent("androidx.health.ACTION_MANAGE_HEALTH_PERMISSIONS").apply {
                                                    putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                                                })
                                            } catch (_: Exception) {
                                                context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
                                            }
                                        } else {
                                            // None granted: Use the system prompt
                                            healthConnectRequestPermissionLauncher.launch(healthConnectManager.permissions)
                                        }
                                        viewModel.setAlertDialogHealthConnectPermissions(false)
                                    })
                                },
                                dismissButton = {
                                    ButtonText(text = stringResource(R.string.button_cancel), onClick = {
                                        viewModel.setAlertDialogHealthConnectPermissions(false)
                                    })
                                },
                                text = { Text(stringResource(R.string.dialog_health_connect_disclosure)) },
                                title = { Text(stringResource(R.string.confirmation)) })
                        }
                    }
                    when {
                        uiState.alertDialogHealthConnectActivation -> {
                            AlertDialog(
                                onDismissRequest = { viewModel.setAlertDialogHealthConnectActivation(false) },
                                confirmButton = {
                                    ButtonText(text = stringResource(R.string.button_understood), onClick = {
                                        viewModel.setAlertDialogHealthConnectActivation(false)
                                    })
                                },
                                text = { Text(stringResource(R.string.dialog_health_connect_activation)) },
                                title = { Text(stringResource(R.string.information)) })
                        }
                    }
                    when {
                        uiState.dialogLanguageInfo -> {
                            AlertDialog(
                                onDismissRequest = {viewModel.setDialogLanguageInfo(false)},
                                properties = DialogProperties(
                                    dismissOnClickOutside = false,
                                    dismissOnBackPress = false
                                ),
                                confirmButton = { ButtonText(text = stringResource(R.string.button_understood),onClick= {
                                    viewModel.setDialogLanguageInfo(false)
                                })},
                                text = { Text(stringResource(R.string.dialog_language,stringResource(R.string.dropdown_reset_database)))},
                                title = {Text(stringResource(R.string.information))})
                        }}
                    when {
                        uiState.alertDialogDayReset -> {
                            AlertDialog(
                                onDismissRequest = {viewModel.setAlertDialogDayReset(false)},
                                confirmButton = { ButtonText(text = stringResource(R.string.button_continue),onClick= {
                                    viewModel.dayReset(context)
                                    viewModel.setAlertDialogDayReset(false)
                                })},
                                dismissButton = { ButtonText(text = stringResource(R.string.button_cancel), onClick= {viewModel.setAlertDialogDayReset(false)})},
                                text = { Text(stringResource(R.string.dialog_reset_day))},
                                title = {Text(stringResource(R.string.confirmation))})
                        }}
                    when {
                        uiState.alertDialogRecipeReset -> {
                            AlertDialog(
                                onDismissRequest = {viewModel.setAlertDialogRecipeReset(false)},
                                confirmButton = { ButtonText(text = stringResource(R.string.button_continue),onClick= {
                                    viewModel.currentComboReset(context)
                                    viewModel.setAlertDialogRecipeReset(false)
                                })},
                                dismissButton = { ButtonText(text = stringResource(R.string.button_cancel), onClick= {viewModel.setAlertDialogRecipeReset(false)})},
                                text = { Text(stringResource(R.string.dialog_reset_recipe))},
                                title = {Text(stringResource(R.string.confirmation))})
                        }
                    }
                    when {
                        uiState.alertDialogArchiveDelete -> {
                            AlertDialog(
                                onDismissRequest = { viewModel.setAlertDialogArchiveDelete(false) },
                                confirmButton = {
                                    ButtonText(text = stringResource(R.string.button_continue), onClick = {
                                        if (uiState.indexArchiveDelete != -1) {
                                            viewModel.archiveDeleteEntry(uiState.indexArchiveDelete, context)
                                            navTo("archive_home")
                                        }
                                        viewModel.setAlertDialogArchiveDelete(false)
                                    })
                                },
                                dismissButton = {
                                    ButtonText(text = stringResource(R.string.button_cancel), onClick = { viewModel.setAlertDialogArchiveDelete(false) })
                                },
                                text = { Text(stringResource(R.string.dialog_archive_delete)) },
                                title = { Text(stringResource(R.string.confirmation)) })
                        }
                    }
                    when {
                        uiState.alertDialogDatabaseDelete -> {
                            AlertDialog(
                                onDismissRequest = { viewModel.setAlertDialogDatabaseDelete(false) },
                                confirmButton = {
                                    ButtonText(text = stringResource(R.string.button_continue), onClick = {
                                        if (uiState.indexDatabaseDelete != -1) {
                                            viewModel.databaseDeleteEntry(uiState.indexDatabaseDelete, true, context)
                                            navController.popBackStack()
                                        }
                                        viewModel.setAlertDialogDatabaseDelete(false)
                                    })
                                },
                                dismissButton = {
                                    ButtonText(text = stringResource(R.string.button_cancel), onClick = { viewModel.setAlertDialogDatabaseDelete(false) })
                                },
                                text = { Text(stringResource(R.string.dialog_database_delete)) },
                                title = { Text(stringResource(R.string.confirmation)) })
                        }
                    }

                    if (uiState.optionsSheetVisible) {
                        var languageMenuExpanded by remember { mutableStateOf(false) }
                        var themeMenuExpanded by remember { mutableStateOf(false) }
                        var databaseExpanded by remember { mutableStateOf(false) }
                        var archiveExpanded by remember { mutableStateOf(false) }
                        var supportExpanded by remember { mutableStateOf(false) }

                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                        val listState = rememberLazyListState()

                        ModalBottomSheet(
                            onDismissRequest = { viewModel.updateOptionsSheetVisible(false) },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            scrimColor = Color.Black.copy(alpha = 0.5f),
                            dragHandle = { BottomSheetDefaults.DragHandle() }
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .padding(bottom = WindowInsets.navigationBars
                                        .asPaddingValues()
                                        .calculateBottomPadding())
                            ) {
                                // --- Section: Settings (Always Visible) ---

                                item {
                                    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                                    val currentLanguageLabel = when {
                                        currentLocale.contains("en") -> stringResource(R.string.always_english)
                                        currentLocale.contains("de") -> stringResource(R.string.always_german)
                                        currentLocale.contains("fr") -> stringResource(R.string.always_french)
                                        currentLocale.contains("it") -> stringResource(R.string.always_italian)
                                        currentLocale.contains("es") -> stringResource(R.string.always_spanish)
                                        else -> stringResource(R.string.system_default)
                                    }

                                    OptionsItem(
                                        text = stringResource(R.string.choose_language),
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box {
                                                    DropdownMenu(
                                                        expanded = languageMenuExpanded,
                                                        onDismissRequest = { languageMenuExpanded = false },
                                                        items = listOf(
                                                            DropdownMenuItemData(stringResource(R.string.always_english)) {
                                                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                                                                viewModel.setDialogLanguageInfo(bool = true)
                                                            },
                                                            DropdownMenuItemData(stringResource(R.string.always_german)) {
                                                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("de"))
                                                                viewModel.setDialogLanguageInfo(bool = true)
                                                            },
                                                            DropdownMenuItemData(stringResource(R.string.always_french)) {
                                                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr"))
                                                                viewModel.setDialogLanguageInfo(bool = true)
                                                            },
                                                            DropdownMenuItemData(stringResource(R.string.always_italian)) {
                                                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("it"))
                                                                viewModel.setDialogLanguageInfo(bool = true)
                                                            },
                                                            DropdownMenuItemData(stringResource(R.string.always_spanish)) {
                                                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
                                                                viewModel.setDialogLanguageInfo(bool = true)
                                                            },
                                                            DropdownMenuItemData(stringResource(R.string.system_default)) {
                                                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                                                                viewModel.setDialogLanguageInfo(bool = true)
                                                            }
                                                        )
                                                    )
                                                }
                                                Text(
                                                    text = currentLanguageLabel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        },
                                        onClick = { languageMenuExpanded = true }
                                    )
                                }

                                item {
                                    val currentThemeLabel = when (uiState.themeUserSetting) {
                                        AppTheme.MODE_AUTO -> stringArrayResource(R.array.dark_mode_options)[0]
                                        AppTheme.MODE_DAY -> stringArrayResource(R.array.dark_mode_options)[1]
                                        AppTheme.MODE_NIGHT -> stringArrayResource(R.array.dark_mode_options)[2]
                                    }
                                    OptionsItem(
                                        text = stringResource(R.string.dark_mode),
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box {
                                                    DropdownMenu(
                                                        expanded = themeMenuExpanded,
                                                        onDismissRequest = { themeMenuExpanded = false },
                                                        items = listOf(
                                                            DropdownMenuItemData(stringArrayResource(R.array.dark_mode_options)[0]) {
                                                                viewModel.setTheme(AppTheme.MODE_AUTO, context)
                                                                themeMenuExpanded = false
                                                            },
                                                            DropdownMenuItemData(stringArrayResource(R.array.dark_mode_options)[1]) {
                                                                viewModel.setTheme(AppTheme.MODE_DAY, context)
                                                                themeMenuExpanded = false
                                                            },
                                                            DropdownMenuItemData(stringArrayResource(R.array.dark_mode_options)[2]) {
                                                                viewModel.setTheme(AppTheme.MODE_NIGHT, context)
                                                                themeMenuExpanded = false
                                                            }
                                                        )
                                                    )
                                                }
                                                Text(
                                                    text = currentThemeLabel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        },
                                        onClick = { themeMenuExpanded = true }
                                    )
                                }

                                item {
                                    OptionsItem(
                                        text = stringResource(R.string.dropdown_sync_health_connect),
                                        trailingContent = {
                                            Switch(
                                                checked = uiState.healthConnectSyncEnabled && uiState.healthConnectPermissionsGranted,
                                                enabled = uiState.healthConnectPermissionsGranted,
                                                onCheckedChange = null // Click handled by OptionsItem
                                            )
                                        },
                                        onClick = {
                                            val availabilityStatus = HealthConnectClient.getSdkStatus(context)
                                            if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
                                                Toast.makeText(context, "Health Connect is not available on this device", Toast.LENGTH_LONG).show()
                                            } else if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                                                val uriString = "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
                                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                                    setPackage("com.android.vending")
                                                    data = uriString.toUri()
                                                    putExtra("overlay", true)
                                                    putExtra("callerId", context.packageName)
                                                })
                                            } else {
                                                if (uiState.healthConnectPermissionsGranted) {
                                                    viewModel.toggleHealthConnectSyncEnabled(context)
                                                } else {
                                                    viewModel.setAlertDialogHealthConnectPermissions(true)
                                                }
                                            }
                                        }
                                    )
                                }

                                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

                                // --- Section: Database ---
                                item {
                                    OptionsSectionHeader(
                                        text = stringResource(R.string.database),
                                        isExpanded = databaseExpanded,
                                        onToggle = { databaseExpanded = !databaseExpanded }
                                    )
                                }
                                if (databaseExpanded) {
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_import_database) + " (*.csv)") {
                                            viewModel.setAlertDialogDatabaseImport(true)
                                        }
                                    }
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_backup_database) + " (*.csv)") {
                                            databaseExporter.launch("database_backup.csv")
                                        }
                                    }
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_reset_database)) {
                                            viewModel.setAlertDialogDatabaseReset(true)
                                        }
                                    }
                                }

                                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

                                // --- Section: Archive ---
                                item {
                                    OptionsSectionHeader(
                                        text = stringResource(R.string.archive),
                                        isExpanded = archiveExpanded,
                                        onToggle = { archiveExpanded = !archiveExpanded }
                                    )
                                }
                                if (archiveExpanded) {
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_import_archive) + " (*.csv)") {
                                            viewModel.setAlertDialogArchiveImport(true)
                                        }
                                    }
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_backup_archive) + " (*.csv)") {
                                            archiveExporter.launch("archive_backup.csv")
                                        }
                                    }
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_export_archive_health_connect)) {
                                            if (uiState.healthConnectPermissionsGranted) {
                                                viewModel.setAlertDialogHealthConnectSync(true)
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.health_connect_permissions_missing), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_clear_archive)) {
                                            viewModel.setAlertDialogArchiveReset(true)
                                        }
                                    }
                                }

                                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

                                // --- Section: Support ---
                                item {
                                    OptionsSectionHeader(
                                        text = stringResource(R.string.support),
                                        isExpanded = supportExpanded,
                                        onToggle = { supportExpanded = !supportExpanded }
                                    )
                                }
                                if (supportExpanded) {
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_github)) {
                                            uriHandler.openUri("https://github.com/Makstuff/MinimalistCalorieCounter")
                                        }
                                    }
                                    item {
                                        OptionsItem(stringResource(R.string.privacy_policy)) {
                                            uriHandler.openUri("https://github.com/Makstuff/MinimalistCalorieCounter/blob/master/PRIVACY_POLICY.md")
                                        }
                                    }
                                    item {
                                        OptionsItem(stringResource(R.string.report_problem)) {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                val uriString = "mailto:message.makstuff@outlook.com?subject=Minimalist Calorie Counter&body=🐈"
                                                data = uriString.replace(" ", "%20").toUri()
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    item {
                                        OptionsItem(stringResource(R.string.dropdown_rate)) {
                                            val appId = "com.makstuff.minimalistcaloriecounter"
                                            val intent = Intent(Intent.ACTION_VIEW).apply { data = "market://details?id=$appId".toUri() }
                                            try { context.startActivity(intent) } catch (_: Exception) {
                                                uriHandler.openUri("https://play.google.com/store/apps/details?id=$appId")
                                            }
                                        }
                                    }
                                }
                                item { Spacer(Modifier.height(16.dp)) }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                items = listOf(
                    NavigationBarItemData(
                        stringResource(R.string.database_navbar), R.drawable.list, uiState.navigationBarHighlight == NAV_DATABASE
                    ) { navTo("database_home") },
                    NavigationBarItemData(
                        stringResource(R.string.archive), R.drawable.archive, uiState.navigationBarHighlight == NAV_ARCHIVE
                    ) { navTo("archive_home") },
                    NavigationBarItemData(
                        stringResource(R.string.day), R.drawable.today, uiState.navigationBarHighlight == NAV_DAY
                    ) {
                        if (navController.currentBackStackEntry?.destination?.route == "day_home") {
                            navTo("day_content")
                        } else {
                            navTo("day_home")
                        }
                    },
                    NavigationBarItemData(
                        stringResource(R.string.food), R.drawable.food, uiState.navigationBarHighlight == NAV_CREATE
                    ) { 
                        viewModel.resetDatabaseEntryCreateAllInput()
                        navTo("create_home") 
                    },
                    NavigationBarItemData(
                        stringResource(R.string.combine), R.drawable.dish, uiState.navigationBarHighlight == NAV_COMBINE
                    ) {
                        viewModel.currentComboReset(context)
                        navTo("combine_home") },
                ).map {
                    {
                        NavigationBarItem(
                            name = it.name,
                            iconId = it.iconId,
                            isSelected = it.isSelected,
                            onClick = it.onClick
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(4.dp),
            navController = navController,
            startDestination = "day_home",
        ) {
            composable("day_content") {
                ScreenWithHoverCard(
                    contentAbove = {},
                    nutrients = uiState.day.overallNutrients,
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_reset_day)) { viewModel.setAlertDialogDayReset(true) },
                        Pair(stringResource(R.string.button_add_food)) { navTo("day_home") },
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
                                navTo("archive_create_entry_from_day")
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
                                            navTo("day_edit_weight/$index")
                                        },
                                    )
                                }
                            },
                        )
                    },
                    context=context
                )
            }
            composable("combine_content") {
                ScreenWithHoverCard(
                    contentAbove = {},
                    nutrients = uiState.currentCombo.overallNutrients,
                    content = {
                        ScrollColumn(
                            items = uiState.currentCombo.components.mapIndexed { index: Int, component ->
                                {
                                    TileIngredient(
                                        component = component,
                                        onClick = {
                                            viewModel.updateCurrentComboComponentWeight(
                                                component.first.toFormattedString(
                                                    true
                                                )
                                            )
                                            viewModel.setNameFoodCombineEdit(component.second.name)
                                            navTo("combine_edit_weight/$index")
                                        },
                                    )
                                }
                            }
                        )
                    },
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_clear_recipe)) { viewModel.setAlertDialogRecipeReset(true) },
                        Pair(stringResource(R.string.button_add_ingredient)) { navTo("combine_home") },
                        Pair(stringResource(R.string.button_turn_to_database_entry)) {
                            try {
                                val food = uiState.currentCombo.toDatabaseEntry()
                                viewModel.databaseAddEntry(context, true, food)
                                viewModel.currentComboReset(context)
                                navTo("database_home")
                            } catch (e: IllegalStateException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    ),
                    context=context
                )
            }

            composable("archive_home") {
                    ScreenWithHoverCard(
                        nutrients = uiState.archive.averageNutrients,
                        contentAbove = { },
                        listOfTextButtons = listOf(
                            Pair(stringResource(R.string.button_create_entry_manually)) {
                                viewModel.resetArchiveEntryAllInput()
                                navTo("archive_create_entry_manually")},
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
                                                navTo("archive_edit_entry/$index")
                                            },
                                        )
                                    }
                                })
                            }
                        },
                        context=context
                    )
                }
            


            composable("archive_create_entry_manually") {
                fun onConfirm() {
                    try {
                        viewModel.archiveAddEntry(
                            date = uiState.inputArchiveEntryDate,
                            bodyWeight = uiState.inputArchiveEntryBodyWeight,
                            nutrients = Nutrients.fromStrings(uiState.inputArchiveEntryNutrients,context),
                            context = context
                        )
                        navTo("archive_home")
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
                        Pair(stringResource(R.string.button_cancel)) { navTo("archive_home") },
                        Pair(stringResource(R.string.button_create_new_archive_entry)) { onConfirm() }
                    )
                )
            }

            composable("archive_create_entry_from_day") {
                fun onConfirm() {
                    try {
                        viewModel.archiveAddEntry(
                            date = uiState.inputArchiveEntryDate,
                            bodyWeight = uiState.inputArchiveEntryBodyWeight,
                            nutrients = Nutrients.fromStrings(uiState.inputArchiveEntryNutrients,context),
                            context = context
                        )
                        viewModel.dayReset(context)
                        navTo("archive_home")
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
                        Pair(stringResource(R.string.button_cancel)) { navTo("day_home") },
                        Pair(stringResource(R.string.button_turn_day_to_archive_entry)) { onConfirm() }
                    )
                )
            }



            composable("archive_edit_entry/{index}") {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.archive.entries.size){
                        fun onConfirm() {
                            try {
                                viewModel.archiveEditEntry(
                                    index = index,
                                    date = uiState.inputArchiveEntryDate,
                                    bodyWeight = uiState.inputArchiveEntryBodyWeight,
                                    nutrients = Nutrients.fromStrings(uiState.inputArchiveEntryNutrients,context),
                                    context = context
                                )
                                navTo("archive_home")
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
                                    navTo("archive_home")
                                },
                                Pair(stringResource(R.string.button_delete)) {
                                    viewModel.setAlertDialogArchiveDelete(true, index)
                                },
                                Pair(stringResource(R.string.button_save_changes)) { onConfirm() }
                            )
                        )
                    }
                }}

            composable("create_home") {
                fun onCreateFood() {
                    try {
                        viewModel.databaseCreateEntryFromInput(context)
                        navTo("day_home")
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
                            navTo("day_home")
                        },
                        Pair(stringResource(R.string.button_clear_input)) { viewModel.resetDatabaseEntryCreateAllInput() },
                        Pair(stringResource(R.string.button_create)) { onCreateFood() }
                    ),
                    context=context
                )
            }

            composable("database_edit_entry/{index}") {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.database.size){
                        fun onConfirmEdit() {
                            try {
                                viewModel.databaseEditEntryFromInput(index, context)
                                navController.popBackStack()
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
                                Pair(stringResource(R.string.button_cancel)) { navController.popBackStack() },
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

            composable("combine_add_component") {
                ScreenShowFoodSelection(
                    indexList = uiState.databaseLetter,
                    database = uiState.database,
                    onFoodClicked = { index ->
                        viewModel.updateCurrentComboComponentWeight("")
                        viewModel.setNameFoodCombineAdd(uiState.database[index].name)
                        navTo("combine_add_weight/$index")
                    },
                    onFoodLongClicked = { index -> editDatabaseEntry(index) },
                    onBack = {navTo("combine_home")}
                )

            }

            composable("day_add_food") {
                ScreenShowFoodSelection(
                    indexList = uiState.databaseLetter,
                    database = uiState.database,
                    onFoodClicked = { index ->
                        viewModel.updateCurrentComboComponentWeight("")
                        viewModel.setNameFoodDayAdd(uiState.database[index].name)
                        navTo("day_add_weight/$index")
                    },
                    onFoodLongClicked = { index -> editDatabaseEntry(index) },
                    onBack = {navTo("day_home")}
                )
            }


            composable("database_home") {
                ScreenShowFoodAll(
                    database = uiState.database,
                    onFoodClicked = { index ->
                        editDatabaseEntry(index)
                    },
                    onFoodLongClicked = { index -> editDatabaseEntry(index) }
                )
            }

            composable("day_home") {
                ScreenWithHoverCard(
                    contentAbove = {},
                    nutrients = uiState.day.overallNutrients,
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_reset_day)) { viewModel.setAlertDialogDayReset(true) },
                        Pair(stringResource(R.string.button_edit)) { navTo("day_content") },
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
                            navTo("archive_create_entry_from_day")
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
                                            navTo("day_add_food") }
                                    )
                                }
                            }.reversed() + uiState.databaseQuickselect.map {
                                Pair<Int, @Composable () -> Unit>(2) {
                                    ButtonGrid(
                                        text = it.second.name,
                                        onClick = {
                                            viewModel.setNameFoodDayAdd(it.second.name)
                                            viewModel.updateCurrentComboComponentWeight("")
                                            navTo("day_add_weight/${it.first}")
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

            composable("combine_home") {
                ScreenWithHoverCard(
                    contentAbove = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val focusManager = LocalFocusManager.current
                            TextField(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                value = uiState.currentCombo.name,
                                onValueChange = {viewModel.currentComboUpdateName(it.replaceFirstChar { it -> it.uppercase() }) },
                                label = stringResource(R.string.recipename),
                                placeholder = stringResource(R.string.example_recipe_name),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                )
                            )
                            TextField(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                value = uiState.currentCombo.inputOverallWeight,
                                onValueChange = {
                                    viewModel.currentComboUpdateOverallWeight(
                                        it
                                    )
                                },
                                label = stringResource(R.string.overall_weight),
                                placeholder = "g ("+ stringResource(R.string.sum_if_empty) +")",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                )
                            )
                        }
                    },
                    nutrients = uiState.currentCombo.overallNutrients,
                    listOfTextButtons = listOf(
                        Pair(stringResource(R.string.button_clear_recipe)) { viewModel.setAlertDialogRecipeReset(true) },
                        Pair(stringResource(R.string.button_edit)) { navTo("combine_content") },
                        Pair(stringResource(R.string.button_turn_to_database_entry)) {
                            try {
                                val food = uiState.currentCombo.toDatabaseEntry()
                                viewModel.databaseAddEntry(context, true, food)
                                navTo("day_home")
                            } catch (e: IllegalStateException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }),
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
                                            navTo("combine_add_component") }
                                    )
                                }
                            }.reversed() + uiState.databaseQuickselect.map {
                                Pair<Int, @Composable () -> Unit>(2) {
                                    ButtonGrid(
                                        text = it.second.name,
                                        onClick = {
                                            viewModel.updateCurrentComboComponentWeight("")
                                            viewModel.setNameFoodCombineAdd(it.second.name)
                                            navTo("combine_add_weight/${it.first}")
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


            composable("day_add_weight/{index}") {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.database.size){
                        fun onConfirm() {
                            try {
                                viewModel.dayAddFood(
                                    uiState.inputCurrentComboComponentWeight,
                                    uiState.database[index],
                                    context
                                )
                                navTo("day_home")
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
                                Pair(stringResource(R.string.button_cancel)) { navTo("day_home") },
                                Pair(stringResource(R.string.button_add_to_day)) {
                                    onConfirm()
                                }
                            ),
                            listOfItems = GENERAL_WEIGHTS.map { list ->
                                Pair<Int, @Composable () -> Unit>(1) {
                                    ButtonGrid(
                                        text = list.second,
                                        onClick = {
                                            viewModel.dayAddFood(
                                                list.first,
                                                uiState.database[index],
                                                context
                                            )
                                            navTo("day_home")
                                        },
                                    )
                                }
                            },
                            listOfQSItems = uiState.database[index].customWeights.listOfStrings.map { list ->
                                Pair<Int, @Composable () -> Unit>(1) {
                                    ButtonGrid(
                                        text = list.second,
                                        onClick = {
                                            viewModel.dayAddFood(
                                                list.first,
                                                uiState.database[index],
                                                context
                                            )
                                            navTo("day_home")
                                        }
                                    )
                                }
                            }.reversed()
                        )
                    }
                }}

            composable("day_edit_weight/{index}") {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.day.components.size){
                        fun onConfirm() {
                            try {
                                viewModel.dayEditFoodWeight(
                                    uiState.inputCurrentComboComponentWeight,
                                    index,
                                    context
                                )
                                navTo("day_content")
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
                                Pair(stringResource(R.string.button_cancel)) { navTo("day_content") },
                                Pair(stringResource(R.string.button_delete)) {
                                    viewModel.dayDeleteFood(index, context)
                                    navTo("day_content")
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
                                                viewModel.dayEditFoodWeight(list.first, index, context)
                                                navTo("day_home")
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
                                                viewModel.dayEditFoodWeight(
                                                    list.first,
                                                    index,
                                                    context
                                                )
                                                navTo("day_home")
                                            }
                                        )
                                    }
                                }.reversed()
                            }
                        )
                    }
                }
            }

            composable("combine_add_weight/{index}") {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.database.size){
                        fun onConfirm() {
                            try {
                                viewModel.currentComboAddComponent(
                                    uiState.inputCurrentComboComponentWeight,
                                    uiState.database[index],
                                    context
                                )
                                navTo("combine_home")
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
                                Pair(stringResource(R.string.button_cancel)) { navTo("combine_home") },
                                Pair(stringResource(R.string.button_add_to_recipe)) {
                                    onConfirm()
                                }
                            ),
                            listOfItems = GENERAL_WEIGHTS.map { list ->
                                Pair<Int, @Composable () -> Unit>(1) {
                                    ButtonGrid(
                                        text = list.second,
                                        onClick = {
                                            viewModel.currentComboAddComponent(
                                                list.first,
                                                uiState.database[index],
                                                context
                                            )
                                            navTo("combine_home")
                                        }
                                    )
                                }
                            },
                            listOfQSItems = uiState.database[index].customWeights.listOfStrings.map { list ->
                                Pair<Int, @Composable () -> Unit>(1) {
                                    ButtonGrid(
                                        text = list.second,
                                        onClick = {
                                            viewModel.currentComboAddComponent(
                                                list.first,
                                                uiState.database[index],
                                                context
                                            )
                                            navTo("combine_home")
                                        }
                                    )
                                }
                            }.reversed()
                        )
                    }
                }
            }

            composable("combine_edit_weight/{index}") {
                val index = it.arguments?.getString("index")?.toIntOrNull()
                if (index != null) {
                    if(index < uiState.currentCombo.components.size){
                        fun onConfirm() {
                            try {
                                viewModel.currentComboEditComponentWeight(
                                    uiState.inputCurrentComboComponentWeight,
                                    index,
                                    context
                                )
                                navTo("combine_content")
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
                                Pair(stringResource(R.string.button_cancel)) { navTo("combine_content") },
                                Pair(stringResource(R.string.button_delete)) {
                                    viewModel.currentComboDeleteComponent(index, context)
                                    navTo("combine_content")
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
                                                viewModel.currentComboEditComponentWeight(
                                                    list.first,
                                                    index,
                                                    context
                                                )
                                                navTo("combine_content")
                                            }
                                        )
                                    }
                                }},
                            listOfQSItems = remember { uiState.currentCombo.components[index].second.customWeights.listOfStrings.map { list ->
                                    Pair<Int, @Composable () -> Unit>(1) {
                                        ButtonGrid(
                                            text = list.second,
                                            onClick = {
                                                viewModel.currentComboEditComponentWeight(
                                                    list.first,
                                                    index,
                                                    context
                                                )
                                                navTo("combine_content")
                                            }
                                        )
                                    }
                                }.reversed()
                            }
                        )
                    }
                }}
        }
    }
}

@Composable
fun OptionsSectionHeader(text: String, isExpanded: Boolean? = null, onToggle: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (isExpanded != null) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun OptionsItem(
    text: String, 
    trailingText: String? = null, 
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text)
        },
        trailingContent = trailingContent ?: trailingText?.let { {
            Text(
                text = it, 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            ) 
        } },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )
}
