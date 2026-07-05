package com.makstuff.minimalistcaloriecounter

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.DatabaseEntry
import com.makstuff.minimalistcaloriecounter.classes.GoalFieldKey
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.NutritionFoodEditDraft
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportFood
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupMode
import com.makstuff.minimalistcaloriecounter.essentials.NavButton
import com.makstuff.minimalistcaloriecounter.persistence.AppCsvStore
import com.makstuff.minimalistcaloriecounter.ui.settings.SettingsSheet
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(AppUiState(archive = Archive(context = app.applicationContext), day = Combo(context = app.applicationContext)))
    val uiState = _uiState.asStateFlow()

    private val healthConnectManager = HealthConnectManager(getApplication<Application>().applicationContext)
    private val csvStore = AppCsvStore()
    private val environment = AppViewModelEnvironment(
        application = app,
        state = _uiState,
        scope = viewModelScope,
        healthConnectManager = healthConnectManager,
        csvStore = csvStore,
    )
    private val goalsActions = AppViewModelGoalsActions(environment)
    private val quickImportActions = AppViewModelQuickImportActions(environment, this)
    private val quickImportRetryActions = AppViewModelQuickImportRetryActions(environment, this)
    private val quickImportRepeatActions = AppViewModelQuickImportRepeatActions(environment)
    private val persistenceActions = AppViewModelPersistenceActions(environment, this)
    private val healthConnectActions = AppViewModelHealthConnectActions(environment, this)
    private val healthConnectMealActions = AppViewModelHealthConnectMealActions(environment, this)
    private val healthConnectExportActions = AppViewModelHealthConnectExportActions(environment, this)
    private val uiActions = AppViewModelUiActions(environment, this)
    private val databaseActions = AppViewModelDatabaseActions(environment)
    private val archiveDayActions = AppViewModelArchiveDayActions(environment)

    fun updateHealthConnectPermissionsStatus() = healthConnectActions.updatePermissionsStatus()

    fun toggleHealthConnectSyncEnabled(context: Context) = healthConnectActions.toggleSyncEnabled()

    fun toggleHealthConnectToastsEnabled(context: Context) = healthConnectActions.toggleToastsEnabled()

    fun syncHealthConnect() = healthConnectActions.syncArchive()

    fun cancelHealthConnectSync() = healthConnectActions.cancelSync()

    fun finishHealthConnectSync() = healthConnectActions.finishSync()

    fun dismissHealthConnectSyncError() = healthConnectActions.dismissSyncError()

    fun updateHealthConnectViewerDate(date: LocalDate) = healthConnectActions.updateViewerDate(date)

    fun requestNavigation(route: String) = uiActions.requestNavigation(route)

    fun clearNavigationRequest(route: String? = null) = uiActions.clearNavigation(route)

    fun updateActiveSettingsSheet(sheet: SettingsSheet?) = uiActions.updateActiveSettingsSheet(sheet)

    fun openSettingsSheet(sheet: SettingsSheet?) = uiActions.openSettingsSheet(sheet)

    fun updateQuickImportSettingsVisible(visible: Boolean) = uiActions.updateQuickImportSettingsVisible(visible)

    fun updateGoalsSettingsVisible(visible: Boolean) = goalsActions.updateSettingsVisible(visible)

    fun updateGoalBirthday(date: LocalDate?) = goalsActions.updateBirthday(date)

    fun updateGoalSex(sex: GoalSex) = goalsActions.updateSex(sex)

    fun updateGoalActivityLevel(activityLevel: ActivityLevel) = goalsActions.updateActivityLevel(activityLevel)

    fun updateGoalWeightLossTarget(target: WeeklyWeightLossTarget) = goalsActions.updateWeightLossTarget(target)

    fun updateGoalMeasurement(field: GoalFieldKey, value: Double?) = goalsActions.updateMeasurement(field, value)

    fun toggleGoalMeasurementLock(field: GoalFieldKey) = goalsActions.toggleMeasurementLock(field)

    fun updateGoalMacro(macro: GoalMacro, value: Double?) = goalsActions.updateMacro(macro, value)

    fun toggleGoalMacroLock(macro: GoalMacro) = goalsActions.toggleMacroLock(macro)

    fun recalculateGoalRecommendation(date: LocalDate = LocalDate.now()) = goalsActions.recalculateRecommendation(date)

    fun applyGoalRecommendation(date: LocalDate = LocalDate.now()) = goalsActions.applyRecommendation(date)

    fun dismissGoalRecommendation() = goalsActions.dismissRecommendation()

    fun refreshGoalsFromHealthConnect() = goalsActions.refreshFromHealthConnect()

    fun readHealthConnectNutritionMeals() = healthConnectMealActions.readMeals()

    fun deleteHealthConnectNutritionMeal(recordId: String) = healthConnectMealActions.deleteMeal(recordId)

    fun deleteHealthConnectNutritionMeals(recordIds: List<String>) = healthConnectMealActions.deleteMeals(recordIds)

    fun addHealthConnectNutritionServing(food: HealthConnectNutritionMeal) = healthConnectMealActions.addServing(food)

    fun updateHealthConnectNutritionServingGroup(
        foods: List<HealthConnectNutritionMeal>,
        draft: NutritionFoodEditDraft,
    ) = healthConnectMealActions.updateServingGroup(foods, draft)

    fun previewHistoricalMealImport(rows: List<List<String>>) = healthConnectMealActions.previewHistoricalImport(rows)

    fun writeHistoricalMealImport() = healthConnectMealActions.writeHistoricalImport()

    fun cleanupHistoricalMealImport() = healthConnectMealActions.cleanupHistoricalImport()
    fun updateHealthConnectNutritionCleanupStartDate(date: LocalDate) = healthConnectExportActions.updateCleanupStartDate(date)
    fun updateHealthConnectNutritionCleanupEndDate(date: LocalDate) = healthConnectExportActions.updateCleanupEndDate(date)
    fun updateHealthConnectNutritionCleanupMode(mode: HealthConnectCleanupMode) = healthConnectExportActions.updateCleanupMode(mode)
    fun previewHealthConnectNutritionRange() = healthConnectExportActions.previewNutritionRange()
    fun updateHealthConnectExportStartDate(date: LocalDate) = healthConnectExportActions.updateExportStartDate(date)
    fun updateHealthConnectExportEndDate(date: LocalDate) = healthConnectExportActions.updateExportEndDate(date)
    fun updateHealthConnectExportMode(mode: HealthConnectExportMode) = healthConnectExportActions.updateExportMode(mode)
    fun updateHealthConnectExportRedacted(redacted: Boolean) = healthConnectExportActions.updateExportRedacted(redacted)
    fun exportHealthConnectRange() = healthConnectExportActions.exportRange()
    fun removeHealthConnectNutritionRange() = healthConnectExportActions.removeNutritionRange()

    fun setAlertDialogHealthConnectActivation(bool: Boolean) = uiActions.setHealthConnectActivationDialog(bool)

    fun setAlertDialogHealthConnectToasts(bool: Boolean) = uiActions.setHealthConnectToastsDialog(bool)

    fun setAlertDialogHealthConnectPermissions(bool: Boolean) = uiActions.setHealthConnectPermissionsDialog(bool)

    fun setLoadingToFalse() = uiActions.setLoadingToFalse()

    fun updateTopBarTitle(string: String) = uiActions.updateTopBarTitle(string)

    fun setTheme(theme: AppTheme, context: Context) = uiActions.setTheme(theme, context)

    fun databaseCreateEntryFromInput(context: Context) = databaseActions.createEntryFromInput(context)

    fun databaseAddEntry(context: Context, updateDependencies: Boolean, databaseEntry: DatabaseEntry) =
        databaseActions.addEntry(context, updateDependencies, databaseEntry)

    fun updateDatabaseEntryCreateName(string: String) = databaseActions.updateCreateName(string)

    fun resetDatabaseEntryCreateAllInput() = databaseActions.resetCreateInput()

    fun toggleDatabaseEntryCreateQuickselect() = databaseActions.toggleCreateQuickselect()

    fun updateDatabaseEntryCreateCustomWeights(string: String) = databaseActions.updateCreateCustomWeights(string)

    fun updateDatabaseEntryCreateNutrient(string: String, index: Int) = databaseActions.updateCreateNutrient(string, index)

    fun databaseEditEntryFromInput(indexToDelete: Int, context: Context) = databaseActions.editEntryFromInput(indexToDelete, context)

    fun databaseDeleteEntry(indexToDelete: Int, updateDependencies: Boolean, context: Context) =
        databaseActions.deleteEntry(indexToDelete, updateDependencies, context)

    fun updateDatabaseEntryEditName(string: String) = databaseActions.updateEditName(string)


    fun updateArchiveEntryDate(date: LocalDate) = archiveDayActions.updateArchiveDate(date)

    fun updateArchiveEntryBodyWeight(string: String) = archiveDayActions.updateArchiveBodyWeight(string)

    fun updateArchiveEntryNutrient(string: String, index: Int) = archiveDayActions.updateArchiveNutrient(string, index)

    fun updateArchiveEntryAllNutrients(list: MutableList<String>) = archiveDayActions.updateArchiveAllNutrients(list)

    fun archiveDeleteEntry(index: Int, context: Context) = archiveDayActions.deleteArchiveEntry(index, context)

    fun resetArchiveEntryAllInput() = archiveDayActions.resetArchiveInput()

    fun toggleDatabaseEntryEditQuickselect() = databaseActions.toggleEditQuickselect()

    fun updateDatabaseEntryEditQuickselect(boolean: Boolean) = databaseActions.updateEditQuickselect(boolean)

    fun updateDatabaseEntryEditCustomWeights(string: String) = databaseActions.updateEditCustomWeights(string)

    fun updateDatabaseEntryEditAllNutrients(list: MutableList<String>) = databaseActions.updateEditAllNutrients(list)

    fun updateDatabaseEntryEditNutrient(string: String, index: Int) = databaseActions.updateEditNutrient(string, index)

    fun dayReset(context: Context) = archiveDayActions.resetDay(context)

    fun databaseDeleteAll(context: Context, updateDependencies: Boolean = true) = databaseActions.deleteAll(context, updateDependencies)

    fun updateNavigationBarHighlight(button: NavButton) = uiActions.updateNavigationBarHighlight(button)

    fun dayAddFood(weight: String, databaseEntry: DatabaseEntry, context: Context) = archiveDayActions.addDayFood(weight, databaseEntry, context)

    fun updateQuickImportText(text: String) = quickImportActions.updateText(text)

    fun updateQuickImportParsedFood(foodIndex: Int, food: QuickImportFood) = quickImportActions.updateParsedFood(foodIndex, food)

    fun resetQuickImport() = quickImportActions.reset()

    fun refreshQuickImportDateTime() = quickImportActions.refreshDateTime()

    fun updateQuickImportDateTime(dateTime: LocalDateTime) = quickImportActions.updateDateTime(dateTime)

    fun toggleQuickImportSnackOverride() = quickImportActions.toggleSnackOverride()

    fun updateQuickImportSnackOverride(enabled: Boolean) = quickImportActions.updateSnackOverride(enabled)

    fun updateQuickImportMealTypeOverride(mealType: QuickImportMealType) = quickImportActions.updateMealTypeOverride(mealType)

    fun prepareQuickImportRepeat(foods: List<HealthConnectNutritionMeal>) = quickImportRepeatActions.prepare(foods)

    fun toggleQuickImportAddFoodsToDatabase() = quickImportActions.toggleAddFoodsToDatabase()

    fun updateQuickImportAddFoodsToDatabase(enabled: Boolean) = quickImportActions.updateAddFoodsToDatabase(enabled)

    fun toggleQuickImportAddFoodsToDay() = quickImportActions.toggleAddFoodsToDay()

    fun updateQuickImportAddFoodsToDay(enabled: Boolean) = quickImportActions.updateAddFoodsToDay(enabled)

    fun toggleQuickImportWriteHealthConnect() = quickImportActions.toggleWriteHealthConnect()

    fun updateQuickImportWriteHealthConnect(enabled: Boolean) = quickImportActions.updateWriteHealthConnect(enabled)

    fun quickImportCommit(context: Context) = quickImportActions.commit(context)

    fun quickImportRetryHealthConnect(context: Context, outboxId: String) = quickImportRetryActions.retry(context, outboxId)

    fun quickImportClearOutbox(context: Context, outboxId: String?, attentionOnly: Boolean) =
        quickImportRetryActions.clear(context, outboxId, attentionOnly)

    fun archiveAddEntry(date: LocalDate, bodyWeight: String, nutrients: Nutrients, context: Context) =
        archiveDayActions.addArchiveEntry(date, bodyWeight, nutrients, context)

    fun archiveEditEntry(index: Int, date: LocalDate, bodyWeight: String, nutrients: Nutrients, context: Context) =
        archiveDayActions.editArchiveEntry(index, date, bodyWeight, nutrients, context)

    fun dayEditFoodWeight(weightString: String, index: Int, context: Context) =
        archiveDayActions.editDayFoodWeight(weightString, index, context)

    fun dayDeleteFood(index: Int, context: Context) = archiveDayActions.deleteDayFood(index, context)

    fun databaseSortByName() = databaseActions.sortByName()

    fun setAlertDialogArchiveReset(bool: Boolean) = uiActions.setArchiveResetDialog(bool)

    fun setAlertDialogDayReset(bool: Boolean) = uiActions.setDayResetDialog(bool)

    fun setAlertDialogDatabaseReset(bool: Boolean) = uiActions.setDatabaseResetDialog(bool)

    fun setAlertDialogArchiveImport(bool: Boolean) = uiActions.setArchiveImportDialog(bool)

    fun setAlertDialogDatabaseImport(bool: Boolean) = uiActions.setDatabaseImportDialog(bool)

    fun setAlertDialogHealthConnectSync(bool: Boolean) = uiActions.setHealthConnectSyncDialog(bool)

    fun setAlertDialogArchiveDelete(bool: Boolean, index: Int = -1) = uiActions.setArchiveDeleteDialog(bool, index)

    fun setAlertDialogDatabaseDelete(bool: Boolean, index: Int = -1) = uiActions.setDatabaseDeleteDialog(bool, index)

    fun updateCurrentComboComponentWeight(string: String) = uiActions.updateCurrentComboComponentWeight(string)


    fun databaseLetterFilter(char: Char) = databaseActions.filterByLetter(char)

    fun databaseLetterReset() = databaseActions.resetLetterFilter()

    fun databaseQuickselectUpdate() = databaseActions.updateQuickselect()


    fun databaseUpdateFromCSV(context: Context) = persistenceActions.updateDatabaseFromCsv(context)

    fun databaseImportCSV(context: Context, inputStream: InputStream) = persistenceActions.importDatabaseCsv(context, inputStream)

    fun databaseResetCSV(overwriteIfExists: Boolean, context: Context) = persistenceActions.resetDatabaseCsv(overwriteIfExists, context)

    fun archiveResetCSV(overwriteIfExists: Boolean, context: Context) = persistenceActions.resetArchiveCsv(overwriteIfExists, context)

    fun dayResetCSV(overwriteIfExists: Boolean, context: Context) = persistenceActions.resetDayCsv(overwriteIfExists, context)

    fun goalsResetCSV(overwriteIfExists: Boolean, context: Context) = persistenceActions.resetGoalsCsv(overwriteIfExists, context)

    fun dayUpdateFromCSV(context: Context) = persistenceActions.updateDayFromCsv(context)

    fun goalsUpdateFromCSV(context: Context) = persistenceActions.updateGoalsFromCsv(context)

    fun quickImportOutboxUpdateFromCSV(context: Context) = persistenceActions.updateQuickImportOutboxFromCsv(context)

    fun optionsUpdateFromFile(context: Context) = persistenceActions.updateOptionsFromFile(context)

    fun optionsWriteToFile(context: Context) = persistenceActions.writeOptions(context)

    fun optionsResetFile(overwriteIfExists: Boolean, context: Context) = persistenceActions.resetOptions(overwriteIfExists, context)

    fun archiveUpdateFromCSV(context: Context) = persistenceActions.updateArchiveFromCsv(context)

    fun archiveImportCSV(context: Context, inputStream: InputStream) = persistenceActions.importArchiveCsv(context, inputStream)

    fun setNameFoodDayAdd(string: String) = uiActions.setNameFoodDayAdd(string)

    fun setNameFoodDayEdit(string: String) = uiActions.setNameFoodDayEdit(string)
}
