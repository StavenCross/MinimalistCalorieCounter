package com.makstuff.minimalistcaloriecounter

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.makstuff.minimalistcaloriecounter.essentials.NAV_CREATE
import com.makstuff.minimalistcaloriecounter.essentials.NAV_DATABASE
import com.makstuff.minimalistcaloriecounter.essentials.NavControllerListener
import com.makstuff.minimalistcaloriecounter.essentials.NavButton
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppBottomBar
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppMainDrawer
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRouteHost
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppTopBar
import com.makstuff.minimalistcaloriecounter.ui.navigation.navigateApp
import com.makstuff.minimalistcaloriecounter.ui.model.mealsDaySummaryText
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonText
import com.makstuff.minimalistcaloriecounter.ui.reused.DropdownMenu
import com.makstuff.minimalistcaloriecounter.ui.reused.TextField
import com.makstuff.minimalistcaloriecounter.health.DayCheckInExporter
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    viewModel: AppViewModel,
    uiState: AppUiState,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    context.findActivity() // Use the proper unwrap function!
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    var mainMenuExpanded by remember { mutableStateOf(false) }
    var mealsDayCopied by remember { mutableStateOf(false) }
    val fileLaunchers = rememberAppFileLaunchers(viewModel)
    val mealsDaySummary = mealsDaySummaryText(
        date = uiState.healthConnectViewerDate,
        meals = uiState.healthConnectViewerMeals,
        targets = uiState.goals.activeTargetsFor(uiState.healthConnectViewerDate),
    )

    LaunchedEffect(currentRoute, mealsDaySummary) {
        mealsDayCopied = false
    }

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
    // NavControllerListener preserves the legacy title/highlight behavior for old routes.
    NavControllerListener(
        nameFoodDayAdd = uiState.nameFoodDayAdd,
        nameFoodDayEdit = uiState.nameFoodDayEdit,
        navController = navController,
        context = context,
        setNav = { string, button -> setNav(string, button) }
    )

    AppStartupEffects(uiState = uiState, viewModel = viewModel, onNavigate = { navTo(it) })
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                AppTopBar(
                    title = uiState.topBarTitle,
                    currentRoute = currentRoute,
                    onOpenMenu = { mainMenuExpanded = true },
                    onOpenGoalsSettings = { viewModel.updateGoalsSettingsVisible(true) },
                    mealsDayCopied = mealsDayCopied,
                    onCopyMealsDay = {
                        clipboard.setText(AnnotatedString(mealsDaySummary))
                        mealsDayCopied = true
                    },
                    onExportMealsDay = {
                        runCatching { DayCheckInExporter(context).export(uiState.healthConnectViewerDate, mealsDaySummary) }
                            .onSuccess { Toast.makeText(context, "Exported check-in to $it", Toast.LENGTH_LONG).show() }
                            .onFailure { Toast.makeText(context, "Check-in export failed: ${it.message}", Toast.LENGTH_LONG).show() }
                    },
                )
            },
            bottomBar = {
                AppBottomBar(
                    navigationBarHighlight = uiState.navigationBarHighlight,
                    onNavigate = { navTo(it) },
                )
            },
        ) { innerPadding ->
            AppRouteHost(
                uiState = uiState,
                viewModel = viewModel,
                navController = navController,
                fileLaunchers = fileLaunchers,
                healthConnectManager = healthConnectManager,
                healthConnectExportPermissionLauncher = healthConnectExportPermissionLauncher,
                keyboardController = keyboardController,
                onNavigate = { navTo(it) },
                onNavigateBack = { navBack() },
                onEditDatabaseEntry = { editDatabaseEntry(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(4.dp),
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

    if (mainMenuExpanded) AppMainDrawer(onDismiss = { mainMenuExpanded = false }, onNavigate = { navTo(it) })

    HealthConnectSyncDialogs(
        uiState = uiState,
        onFinish = { viewModel.finishHealthConnectSync() },
        onCancel = { viewModel.cancelHealthConnectSync() },
        onDismissError = { viewModel.dismissHealthConnectSyncError() },
    )
}
