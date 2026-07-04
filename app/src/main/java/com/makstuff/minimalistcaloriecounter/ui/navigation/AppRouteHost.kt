package com.makstuff.minimalistcaloriecounter.ui.navigation

import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.makstuff.minimalistcaloriecounter.AppFileLaunchers
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.health.DayCheckInExporter
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.ui.navigation.legacy.legacyArchiveRoutes
import com.makstuff.minimalistcaloriecounter.ui.navigation.legacy.legacyDatabaseRoutes
import com.makstuff.minimalistcaloriecounter.ui.navigation.legacy.legacyDayRoutes
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenGoals
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenHealthConnectNutrition
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenQuickImport
import com.makstuff.minimalistcaloriecounter.ui.settings.AppSettingsPage

/**
 * Registers the app's navigation graph.
 *
 * The host keeps the modern Add Meal, Meals, Goals, and Settings routes together while delegating
 * legacy archive/database/day-builder routes to isolated modules. Route callbacks intentionally pass
 * through the AppViewModel facade until the remaining feature coordinators are split further.
 */
@Composable
fun AppRouteHost(
    uiState: AppUiState,
    viewModel: AppViewModel,
    navController: NavHostController,
    fileLaunchers: AppFileLaunchers,
    healthConnectManager: HealthConnectManager,
    healthConnectExportPermissionLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>,
    keyboardController: SoftwareKeyboardController?,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onEditDatabaseEntry: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = AppRoutes.QUICK_IMPORT,
    ) {
        legacyDayRoutes(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            keyboardController = keyboardController,
            onNavigate = onNavigate,
            onEditDatabaseEntry = onEditDatabaseEntry,
        )
        legacyArchiveRoutes(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            keyboardController = keyboardController,
            onNavigate = onNavigate,
        )
        legacyDatabaseRoutes(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            keyboardController = keyboardController,
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
            onEditDatabaseEntry = onEditDatabaseEntry,
        )
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
    }
}
