package com.makstuff.minimalistcaloriecounter

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
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
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
import com.makstuff.minimalistcaloriecounter.ui.settings.AppSettingsPage
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.health.DayCheckInExporter
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import java.time.LocalDateTime

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
                AppSettingsPage(
                    uiState = uiState,
                    viewModel = viewModel,
                    fileLaunchers = fileLaunchers,
                    healthConnectManager = healthConnectManager,
                    healthConnectExportPermissionLauncher = healthConnectExportPermissionLauncher,
                )
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
