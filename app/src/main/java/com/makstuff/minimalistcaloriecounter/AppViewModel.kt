package com.makstuff.minimalistcaloriecounter

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.doyaaaaaken.kotlincsv.util.CSVFieldNumDifferentException
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.CustomWeights
import com.makstuff.minimalistcaloriecounter.classes.DatabaseEntry
import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.GoalCalculator
import com.makstuff.minimalistcaloriecounter.classes.GoalFieldKey
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.GoalRecommendation
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.GoalsCsv
import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImporter
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportCommitOptions
import com.makstuff.minimalistcaloriecounter.classes.QuickImportDatabaseEntryDraft
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportParser
import com.makstuff.minimalistcaloriecounter.classes.QuickImportPlanner
import com.makstuff.minimalistcaloriecounter.classes.QuickImportResult
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.health.HealthConnectDeleteResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectGoalProfileReadResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionReadResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.health.HistoricalMealHealthConnectResult
import com.makstuff.minimalistcaloriecounter.essentials.NUTRIENT_PROPERTIES
import com.makstuff.minimalistcaloriecounter.essentials.NavButton
import com.makstuff.minimalistcaloriecounter.essentials.checkValidNumber
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.essentials.toBodyWeight
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(AppUiState(archive = Archive(context = app.applicationContext), day = Combo(context = app.applicationContext)))
    val uiState = _uiState.asStateFlow()

    private val healthConnectManager = HealthConnectManager(getApplication<Application>().applicationContext)

    fun updateHealthConnectPermissionsStatus() {
        viewModelScope.launch {
            val allGranted = healthConnectManager.hasAllPermissions()
            val anyGranted = healthConnectManager.hasAnyPermissions()
            var needsSave = false
            _uiState.update { currentState ->
                val newSyncStatus = if (!allGranted) false else currentState.healthConnectSyncEnabled
                if (newSyncStatus != currentState.healthConnectSyncEnabled) {
                    needsSave = true
                }
                val newToastsStatus = if (!allGranted) false else currentState.healthConnectToastsEnabled
                if (newToastsStatus != currentState.healthConnectToastsEnabled) {
                    needsSave = true
                }
                currentState.copy(
                    healthConnectPermissionsGranted = allGranted,
                    healthConnectAnyPermissionsGranted = anyGranted,
                    healthConnectSyncEnabled = newSyncStatus,
                    healthConnectToastsEnabled = newToastsStatus,
                )
            }
            if (needsSave) {
                optionsWriteToFile(getApplication<Application>().applicationContext)
            }
        }
    }

    fun toggleHealthConnectSyncEnabled(context: Context) {
        _uiState.update { currentState ->
            val newState = !currentState.healthConnectSyncEnabled
            // Use launch to handle the side effects since we are inside a functional update
            if (newState) {
                viewModelScope.launch { setAlertDialogHealthConnectActivation(bool = true) }
            }
            currentState.copy(healthConnectSyncEnabled = newState)
        }
        optionsWriteToFile(context)
    }

    fun toggleHealthConnectToastsEnabled(context: Context) {
        _uiState.update { currentState ->
            val newState = !currentState.healthConnectToastsEnabled
            if (newState) {
                viewModelScope.launch { setAlertDialogHealthConnectToasts(bool = true) }
            }
            currentState.copy(healthConnectToastsEnabled = newState)
        }
        optionsWriteToFile(context)
    }

    private var healthConnectSyncJob: kotlinx.coroutines.Job? = null

    fun syncHealthConnect() {
        healthConnectSyncJob?.cancel()
        healthConnectSyncJob = viewModelScope.launch {
            if (healthConnectManager.hasAllPermissions()) {
                healthConnectManager.syncArchive(
                    archive = uiState.value.archive,
                    onProgress = { progress, current, total ->
                        _uiState.update { it.copy(
                            healthConnectSyncProgress = progress,
                            healthConnectSyncCurrentCount = current,
                            healthConnectSyncTotalCount = total
                        ) }
                    },
                    onError = { error ->
                        _uiState.update { it.copy(healthConnectSyncMessage = error) }
                    }
                )
            } else {
                Toast.makeText(getApplication<Application>().applicationContext, getApplication<Application>().getString(R.string.health_connect_permissions_missing), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cancelHealthConnectSync() {
        healthConnectSyncJob?.cancel()
        healthConnectSyncJob = null
        _uiState.update { it.copy(healthConnectSyncProgress = null, healthConnectSyncMessage = null) }
    }

    fun finishHealthConnectSync() {
        _uiState.update { it.copy(healthConnectSyncProgress = null) }
    }

    fun dismissHealthConnectSyncError() {
        _uiState.update { it.copy(healthConnectSyncProgress = null, healthConnectSyncMessage = null) }
    }

    fun updateHealthConnectViewerDate(date: LocalDate) {
        _uiState.update { currentState ->
            currentState.copy(
                healthConnectViewerDate = date,
                healthConnectViewerMessage = null,
            )
        }
        readHealthConnectNutritionMeals()
    }

    fun requestNavigation(route: String) {
        _uiState.update { currentState ->
            currentState.copy(automationRouteRequest = route)
        }
    }

    fun clearNavigationRequest(route: String? = null) {
        _uiState.update { currentState ->
            if (route == null || currentState.automationRouteRequest == route) {
                currentState.copy(automationRouteRequest = null)
            } else {
                currentState
            }
        }
    }

    fun updateActiveSettingsSheet(sheet: String?) {
        _uiState.update { currentState ->
            currentState.copy(activeSettingsSheet = sheet)
        }
    }

    fun updateQuickImportSettingsVisible(visible: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(quickImportSettingsVisible = visible)
        }
    }

    fun updateGoalsSettingsVisible(visible: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(settingsVisible = visible))
        }
    }

    fun updateGoalBirthday(date: LocalDate?) {
        _uiState.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(profile = currentState.goals.profile.copy(birthday = date)))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun updateGoalSex(sex: GoalSex) {
        _uiState.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(profile = currentState.goals.profile.copy(sex = sex)))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun updateGoalActivityLevel(activityLevel: ActivityLevel) {
        _uiState.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(profile = currentState.goals.profile.copy(activityLevel = activityLevel)))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun updateGoalWeightLossTarget(target: WeeklyWeightLossTarget) {
        _uiState.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(profile = currentState.goals.profile.copy(weightLossTarget = target)))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun updateGoalMeasurement(field: GoalFieldKey, value: Double?) {
        _uiState.update { currentState ->
            val profile = currentState.goals.profile
            val updated = when (field) {
                GoalFieldKey.HeightCm -> profile.copy(heightCm = profile.heightCm.setManual(value))
                GoalFieldKey.WeightKg -> profile.copy(weightKg = profile.weightKg.setManual(value))
                GoalFieldKey.BodyFatPercent -> profile.copy(bodyFatPercent = profile.bodyFatPercent.setManual(value))
                GoalFieldKey.LeanMassKg -> profile.copy(leanMassKg = profile.leanMassKg.setManual(value))
                else -> profile
            }
            currentState.copy(goals = currentState.goals.copy(profile = updated))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun toggleGoalMeasurementLock(field: GoalFieldKey) {
        _uiState.update { currentState ->
            val profile = currentState.goals.profile
            fun GoalMeasurement.toggled(): GoalMeasurement = copy(locked = !locked)
            val updated = when (field) {
                GoalFieldKey.HeightCm -> profile.copy(heightCm = profile.heightCm.toggled())
                GoalFieldKey.WeightKg -> profile.copy(weightKg = profile.weightKg.toggled())
                GoalFieldKey.BodyFatPercent -> profile.copy(bodyFatPercent = profile.bodyFatPercent.toggled())
                GoalFieldKey.LeanMassKg -> profile.copy(leanMassKg = profile.leanMassKg.toggled())
                else -> profile
            }
            currentState.copy(goals = currentState.goals.copy(profile = updated))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun updateGoalMacro(macro: GoalMacro, value: Double?) {
        _uiState.update { currentState ->
            val targets = currentState.goals.currentTargets.withValue(macro, value, lock = true)
            currentState.copy(goals = currentState.goals.copy(currentTargets = targets))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun toggleGoalMacroLock(macro: GoalMacro) {
        _uiState.update { currentState ->
            val targets = currentState.goals.currentTargets
            val updated = if (macro in targets.lockedMacros) targets.unlocked(macro) else targets.withValue(macro, targets.valueFor(macro), lock = true)
            currentState.copy(goals = currentState.goals.copy(currentTargets = updated))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun recalculateGoalRecommendation(date: LocalDate = LocalDate.now()) {
        _uiState.update { currentState ->
            val recommendation = GoalCalculator.recommendTargets(
                profile = currentState.goals.profile,
                existingTargets = currentState.goals.currentTargets,
                date = date,
            )
            currentState.copy(
                goals = currentState.goals.copy(
                    recommendation = recommendation,
                    message = if (recommendation == null) {
                        val missing = currentState.goals.profile.missingRequiredFields(date)
                        if (missing.isEmpty()) {
                            "Complete required profile fields and lean mass/body fat to calculate goals."
                        } else {
                            "Missing: ${missing.joinToString(", ")}."
                        }
                    } else {
                        null
                    },
                )
            )
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun applyGoalRecommendation(date: LocalDate = LocalDate.now()) {
        _uiState.update { currentState ->
            val recommendation = currentState.goals.recommendation ?: return@update currentState
            val history = (currentState.goals.history + GoalHistoryEntry(date, recommendation.targets, "recommended")).sortedBy { it.effectiveDate }
            currentState.copy(
                goals = currentState.goals.copy(
                    currentTargets = recommendation.targets,
                    history = history,
                    recommendation = null,
                    message = "Applied new goal recommendation.",
                )
            )
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun dismissGoalRecommendation() {
        _uiState.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(recommendation = null))
        }
        goalsWriteToCSV(getApplication<Application>().applicationContext)
    }

    fun refreshGoalsFromHealthConnect() {
        _uiState.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(message = "Reading Health Connect profile data..."))
        }
        viewModelScope.launch {
            when (val result = healthConnectManager.readGoalProfileSnapshot()) {
                is HealthConnectGoalProfileReadResult.Success -> {
                    _uiState.update { currentState ->
                        val profile = GoalCalculator.applyHealthSnapshot(currentState.goals.profile, result.snapshot)
                        val recommendation = GoalCalculator.recommendTargets(
                            profile = profile,
                            existingTargets = currentState.goals.currentTargets,
                            date = LocalDate.now(),
                        )
                        currentState.copy(
                            goals = currentState.goals.copy(
                                profile = profile,
                                recommendation = recommendation,
                                message = if (recommendation == null) {
                                    val missing = profile.missingRequiredFields(LocalDate.now())
                                    "Updated unlocked fields. Missing: ${missing.joinToString(", ")}."
                                } else {
                                    "Updated unlocked fields from Health Connect."
                                },
                            )
                        )
                    }
                    goalsWriteToCSV(getApplication<Application>().applicationContext)
                }
                HealthConnectGoalProfileReadResult.HealthConnectUnavailable -> {
                    _uiState.update { currentState ->
                        currentState.copy(goals = currentState.goals.copy(message = "Health Connect is unavailable."))
                    }
                }
                HealthConnectGoalProfileReadResult.PermissionsMissing -> {
                    _uiState.update { currentState ->
                        currentState.copy(goals = currentState.goals.copy(message = "Health Connect profile permissions are missing."))
                    }
                }
                is HealthConnectGoalProfileReadResult.Failed -> {
                    _uiState.update { currentState ->
                        currentState.copy(goals = currentState.goals.copy(message = "Health Connect profile read failed: ${result.message}"))
                    }
                }
            }
        }
    }

    fun readHealthConnectNutritionMeals() {
        _uiState.update { currentState ->
            currentState.copy(
                healthConnectViewerLoading = true,
                healthConnectViewerMessage = null,
            )
        }
        viewModelScope.launch {
            when (val result = healthConnectManager.readNutritionMeals(uiState.value.healthConnectViewerDate)) {
                is HealthConnectNutritionReadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMeals = result.meals,
                            healthConnectViewerMessage = if (result.meals.isEmpty()) {
                                "No Health Connect nutrition records found for this app on this date."
                            } else {
                                null
                            },
                        )
                    }
                }
                HealthConnectNutritionReadResult.HealthConnectUnavailable -> {
                    _uiState.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMeals = emptyList(),
                            healthConnectViewerMessage = getApplication<Application>().getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HealthConnectNutritionReadResult.PermissionsMissing -> {
                    _uiState.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMeals = emptyList(),
                            healthConnectViewerMessage = getApplication<Application>().getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HealthConnectNutritionReadResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMeals = emptyList(),
                            healthConnectViewerMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun deleteHealthConnectNutritionMeal(recordId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                healthConnectViewerLoading = true,
                healthConnectViewerMessage = null,
            )
        }
        viewModelScope.launch {
            when (val result = healthConnectManager.deleteNutritionMeal(recordId)) {
                HealthConnectDeleteResult.Success -> {
                    readHealthConnectNutritionMeals()
                }
                HealthConnectDeleteResult.HealthConnectUnavailable -> {
                    _uiState.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = getApplication<Application>().getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HealthConnectDeleteResult.PermissionsMissing -> {
                    _uiState.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = getApplication<Application>().getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HealthConnectDeleteResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            healthConnectViewerLoading = false,
                            healthConnectViewerMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun previewHistoricalMealImport(rows: List<List<String>>) {
        val preview = HistoricalMealImporter.parseCsv(rows)
        _uiState.update {
            it.copy(
                historicalMealImportPreview = preview,
                historicalMealImportMessage = "Preview: ${preview.validRows} foods, ${preview.mealCount} meals, ${preview.skippedRows} skipped rows.",
            )
        }
    }

    fun writeHistoricalMealImport() {
        val preview = uiState.value.historicalMealImportPreview ?: return
        if (preview.foods.isEmpty()) {
            _uiState.update { it.copy(historicalMealImportMessage = "No valid historical foods to write.") }
            return
        }
        _uiState.update {
            it.copy(
                historicalMealImportInProgress = true,
                historicalMealImportMessage = "Writing historical meals to Health Connect.",
                healthConnectSyncProgress = 0f,
                healthConnectSyncCurrentCount = 0,
                healthConnectSyncTotalCount = preview.foods.size,
            )
        }
        viewModelScope.launch {
            when (val result = healthConnectManager.writeHistoricalMealFoods(
                foods = preview.foods,
                onProgress = { progress, current, total ->
                    _uiState.update {
                        it.copy(
                            healthConnectSyncProgress = progress,
                            healthConnectSyncCurrentCount = current,
                            healthConnectSyncTotalCount = total,
                        )
                    }
                },
            )) {
                is HistoricalMealHealthConnectResult.Success -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = "Historical import complete: ${result.written} written, ${result.skippedDuplicates} duplicates skipped.",
                        )
                    }
                    readHealthConnectNutritionMeals()
                }
                HistoricalMealHealthConnectResult.HealthConnectUnavailable -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = getApplication<Application>().getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HistoricalMealHealthConnectResult.PermissionsMissing -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = getApplication<Application>().getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HistoricalMealHealthConnectResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun cleanupHistoricalMealImport() {
        val preview = uiState.value.historicalMealImportPreview ?: run {
            _uiState.update { it.copy(historicalMealImportMessage = "Import a historical meal CSV first so cleanup knows the date range.") }
            return
        }
        _uiState.update {
            it.copy(
                historicalMealImportInProgress = true,
                historicalMealImportMessage = "Removing historical import and legacy Daily Total rows.",
            )
        }
        viewModelScope.launch {
            when (val result = healthConnectManager.cleanupHistoricalMealRecords(preview.dates)) {
                is HistoricalMealHealthConnectResult.Success -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            historicalMealImportMessage = "Cleanup complete: ${result.deleted} Health Connect records removed.",
                        )
                    }
                    readHealthConnectNutritionMeals()
                }
                HistoricalMealHealthConnectResult.HealthConnectUnavailable -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            historicalMealImportMessage = getApplication<Application>().getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HistoricalMealHealthConnectResult.PermissionsMissing -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            historicalMealImportMessage = getApplication<Application>().getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HistoricalMealHealthConnectResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            historicalMealImportMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun updateHealthConnectNutritionCleanupStartDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                healthConnectNutritionCleanupStartDate = date,
                historicalMealImportMessage = null,
            )
        }
    }

    fun updateHealthConnectNutritionCleanupEndDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                healthConnectNutritionCleanupEndDate = date,
                historicalMealImportMessage = null,
            )
        }
    }

    fun removeHealthConnectNutritionRange() {
        val state = uiState.value
        _uiState.update {
            it.copy(
                historicalMealImportInProgress = true,
                historicalMealImportMessage = "Removing Health Connect meals and nutrition.",
                healthConnectSyncProgress = 0f,
                healthConnectSyncCurrentCount = 0,
                healthConnectSyncTotalCount = 0,
            )
        }
        viewModelScope.launch {
            when (val result = healthConnectManager.deleteNutritionRecordsInRange(
                startDate = state.healthConnectNutritionCleanupStartDate,
                endDate = state.healthConnectNutritionCleanupEndDate,
                onProgress = { progress, current, total ->
                    _uiState.update {
                        it.copy(
                            healthConnectSyncProgress = progress,
                            healthConnectSyncCurrentCount = current,
                            healthConnectSyncTotalCount = total,
                        )
                    }
                },
            )) {
                is HistoricalMealHealthConnectResult.Success -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = "Removed ${result.deleted} Health Connect meal/nutrition records.",
                        )
                    }
                    readHealthConnectNutritionMeals()
                }
                HistoricalMealHealthConnectResult.HealthConnectUnavailable -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = getApplication<Application>().getString(R.string.toast_hc_not_available),
                        )
                    }
                }
                HistoricalMealHealthConnectResult.PermissionsMissing -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = getApplication<Application>().getString(R.string.health_connect_permissions_missing),
                        )
                    }
                }
                is HistoricalMealHealthConnectResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            historicalMealImportInProgress = false,
                            healthConnectSyncProgress = null,
                            historicalMealImportMessage = result.message,
                        )
                    }
                }
            }
        }
    }


    fun setAlertDialogHealthConnectActivation(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogHealthConnectActivation = bool,
            )
        }
    }

    fun setAlertDialogHealthConnectToasts(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogHealthConnectToasts = bool,
            )
        }
    }

    fun setAlertDialogHealthConnectPermissions(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogHealthConnectPermissions = bool
            )
        }
    }

    fun setLoadingToFalse() {
        _uiState.update { currentState ->
            currentState.copy(
                loading = false
            )
        }
    }

    fun updateTopBarTitle(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                topBarTitle = string
            )
        }
    }


    fun setTheme(theme: AppTheme, context: Context) {
        _uiState.update { currentState ->
            currentState.copy(
                themeUserSetting = theme
            )
        }
        optionsWriteToFile(context)
    }

    fun databaseCreateEntryFromInput(context: Context) {
        // Prioritize name check
        DatabaseEntry.checkName(uiState.value.inputDatabaseEntryCreateName, context)
        
        databaseAddEntry(
            context = context,
            updateDependencies = true,
            databaseEntry = DatabaseEntry(
                name = uiState.value.inputDatabaseEntryCreateName,
                nutrients = Nutrients.fromStrings(uiState.value.inputDatabaseEntryCreateNutrients,context = context),
                customWeights = CustomWeights(uiState.value.inputDatabaseEntryCreateCustomWeights,context = context),
                quickselect = uiState.value.inputDatabaseEntryCreateQuickselect,
                context = context
            )
        )
    }

    fun databaseAddEntry(
        context: Context,
        updateDependencies: Boolean,
        databaseEntry: DatabaseEntry,
    ) {
        _uiState.value.database.add(databaseEntry)
        if (updateDependencies) {
            databaseSortByName()
            databaseQuickselectUpdate()
            databaseWriteToCSV(context)
        }
    }

    fun updateDatabaseEntryCreateName(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryCreateName = string
            )
        }
    }

    fun resetDatabaseEntryCreateAllInput() {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryCreateName = "",
                inputDatabaseEntryCreateCustomWeights = "",
                inputDatabaseEntryCreateNutrients = mutableStateListOf(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
                ),
                inputDatabaseEntryCreateQuickselect = false,
            )
        }
    }

    fun toggleDatabaseEntryCreateQuickselect() {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryCreateQuickselect = !uiState.value.inputDatabaseEntryCreateQuickselect
            )
        }
    }

    fun updateDatabaseEntryCreateCustomWeights(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryCreateCustomWeights = string
            )
        }
    }

    fun updateDatabaseEntryCreateNutrient(string: String, index: Int) {
        _uiState.value.inputDatabaseEntryCreateNutrients[index] = string
    }


    fun databaseEditEntryFromInput(indexToDelete: Int, context: Context) {
        // Prioritize name check
        DatabaseEntry.checkName(uiState.value.inputDatabaseEntryEditName, context)
        
        databaseDeleteEntry(indexToDelete, false, context)
        databaseAddEntry(
            context,
            true,
            DatabaseEntry(
                name = uiState.value.inputDatabaseEntryEditName,
                nutrients = Nutrients.fromStrings(uiState.value.inputDatabaseEntryEditNutrients,context = context),
                customWeights = CustomWeights(uiState.value.inputDatabaseEntryEditCustomWeights,context = context),
                quickselect = uiState.value.inputDatabaseEntryEditQuickselect,
                context = context
            )
        )
    }

    fun databaseDeleteEntry(indexToDelete: Int, updateDependencies: Boolean, context: Context) {
        _uiState.value.database.removeAt(indexToDelete)
        if (updateDependencies) {
            databaseWriteToCSV(context)
            databaseQuickselectUpdate()
            databaseLetterReset()
        }
    }

    fun updateDatabaseEntryEditName(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryEditName = string
            )
        }
    }


    fun updateArchiveEntryDate(date: LocalDate) {
        _uiState.update { currentState ->
            currentState.copy(
                inputArchiveEntryDate = date
            )
        }
    }

    fun updateArchiveEntryBodyWeight(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                inputArchiveEntryBodyWeight = string
            )
        }
    }

    fun updateArchiveEntryNutrient(string: String, index: Int) {
        _uiState.value.inputArchiveEntryNutrients[index] = string
    }

    fun updateArchiveEntryAllNutrients(list: MutableList<String>) {
        _uiState.update { currentState ->
            currentState.copy(
                inputArchiveEntryNutrients = list.toMutableStateList()
            )
        }
    }

    fun archiveDeleteEntry(index: Int, context: Context) {
        val entry = _uiState.value.archive.entries[index]
        _uiState.value.archive.deleteEntry(index)
        archiveWriteToCSV(context)
        if (uiState.value.healthConnectSyncEnabled) {
            viewModelScope.launch {
                healthConnectManager.deleteSingleEntry(entry.first)
                if (uiState.value.healthConnectToastsEnabled) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.toast_hc_entry_deleted), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun resetArchiveEntryAllInput() {
        _uiState.update { currentState ->
            currentState.copy(
                inputArchiveEntryDate = LocalDateTime.now().minusHours(12).toLocalDate(),
                inputArchiveEntryBodyWeight = "",
                inputArchiveEntryNutrients = mutableStateListOf(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
                ),
            )
        }
    }


    fun toggleDatabaseEntryEditQuickselect() {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryEditQuickselect = !uiState.value.inputDatabaseEntryEditQuickselect
            )
        }
    }

    fun updateDatabaseEntryEditQuickselect(boolean: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryEditQuickselect = boolean
            )
        }
    }

    fun updateDatabaseEntryEditCustomWeights(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryEditCustomWeights = string
            )
        }
    }

    fun updateDatabaseEntryEditAllNutrients(list: MutableList<String>) {
        _uiState.update { currentState ->
            currentState.copy(
                inputDatabaseEntryEditNutrients = list.toMutableStateList()
            )
        }
    }

    fun updateDatabaseEntryEditNutrient(string: String, index: Int) {
        _uiState.value.inputDatabaseEntryEditNutrients[index] = string
    }


    fun dayReset(context: Context) {
        _uiState.update { currentState ->
            currentState.copy(
                day = Combo(context = context)
            )
        }
        dayWriteToCSV(context)
    }

    fun databaseDeleteAll(context: Context, updateDependencies: Boolean = true) {
        _uiState.value.database.clear()
        if (updateDependencies) {
            databaseWriteToCSV(context)
            databaseQuickselectUpdate()
            databaseLetterReset()
        }
    }

    fun updateNavigationBarHighlight(button: NavButton) {
        _uiState.update { currentState ->
            currentState.copy(
                navigationBarHighlight = button
            )
        }
    }

    fun dayAddFood(weight: String, databaseEntry: DatabaseEntry, context: Context) {
        checkValidNumber(weight, context.getString(R.string.weight), context)
        _uiState.value.day.addComponent(weight.toDouble(), databaseEntry)
        dayWriteToCSV(context)
    }

    fun updateQuickImportText(text: String) {
        val parsed = runCatching { QuickImportParser.parse(text) }
        _uiState.update { currentState ->
            currentState.copy(
                inputQuickImportText = text,
                quickImportMeal = parsed.getOrNull(),
                quickImportError = if (text.isBlank()) null else parsed.exceptionOrNull()?.message,
                quickImportResult = null,
            )
        }
    }

    fun resetQuickImport() {
        _uiState.update { currentState ->
            currentState.copy(
                inputQuickImportText = "",
                inputQuickImportDateTime = LocalDateTime.now(),
                quickImportSnackOverride = false,
                quickImportMealTypeOverride = null,
                quickImportMeal = null,
                quickImportError = null,
                quickImportResult = null,
                quickImportAddFoodsToDatabase = true,
                quickImportAddFoodsToDay = true,
                quickImportWriteHealthConnect = true,
                quickImportInProgress = false,
            )
        }
    }

    fun refreshQuickImportDateTime() {
        _uiState.update { currentState ->
            currentState.copy(
                inputQuickImportDateTime = LocalDateTime.now(),
                quickImportSnackOverride = false,
                quickImportMealTypeOverride = null,
                quickImportResult = null,
            )
        }
    }

    fun updateQuickImportDateTime(dateTime: LocalDateTime) {
        _uiState.update { currentState ->
            currentState.copy(
                inputQuickImportDateTime = dateTime,
                quickImportSnackOverride = false,
                quickImportMealTypeOverride = null,
                quickImportResult = null,
            )
        }
    }

    fun toggleQuickImportSnackOverride() {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportSnackOverride = !currentState.quickImportSnackOverride,
                quickImportMealTypeOverride = if (!currentState.quickImportSnackOverride) {
                    QuickImportMealType.Snack
                } else {
                    null
                },
                quickImportResult = null,
            )
        }
    }

    fun updateQuickImportSnackOverride(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportSnackOverride = enabled,
                quickImportMealTypeOverride = if (enabled) {
                    QuickImportMealType.Snack
                } else {
                    null
                },
                quickImportResult = null,
            )
        }
    }

    fun updateQuickImportMealTypeOverride(mealType: QuickImportMealType) {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportMealTypeOverride = mealType,
                quickImportSnackOverride = mealType == QuickImportMealType.Snack,
                quickImportResult = null,
            )
        }
    }

    fun toggleQuickImportAddFoodsToDatabase() {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportAddFoodsToDatabase = !currentState.quickImportAddFoodsToDatabase,
                quickImportResult = null,
            )
        }
    }

    fun updateQuickImportAddFoodsToDatabase(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportAddFoodsToDatabase = enabled,
                quickImportResult = null,
            )
        }
    }

    fun toggleQuickImportAddFoodsToDay() {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportAddFoodsToDay = !currentState.quickImportAddFoodsToDay,
                quickImportResult = null,
            )
        }
    }

    fun updateQuickImportAddFoodsToDay(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportAddFoodsToDay = enabled,
                quickImportResult = null,
            )
        }
    }

    fun toggleQuickImportWriteHealthConnect() {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportWriteHealthConnect = !currentState.quickImportWriteHealthConnect,
                quickImportResult = null,
            )
        }
    }

    fun updateQuickImportWriteHealthConnect(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                quickImportWriteHealthConnect = enabled,
                quickImportResult = null,
            )
        }
    }

    fun quickImportCommit(context: Context) {
        val state = uiState.value
        val meal = state.quickImportMeal ?: run {
            updateQuickImportText(state.inputQuickImportText)
            uiState.value.quickImportMeal ?: return
        }
        val options = QuickImportCommitOptions(
            addFoodsToDatabase = state.quickImportAddFoodsToDatabase,
            addFoodsToDay = state.quickImportAddFoodsToDay,
            writeHealthConnect = state.quickImportWriteHealthConnect,
        )

        val plan = try {
            QuickImportPlanner.build(
                meal = meal,
                options = options,
                dateTime = state.inputQuickImportDateTime,
                mealType = state.quickImportMealType,
                existingDatabaseNames = state.database.map { it.name }.toSet(),
            )
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(quickImportError = e.message, quickImportResult = null) }
            return
        }

        val databaseEntries = try {
            plan.foodDrafts.map { it.toDatabaseEntry(context) }
        } catch (e: IllegalStateException) {
            _uiState.update { it.copy(quickImportError = e.message, quickImportResult = null) }
            return
        }

        _uiState.update { it.copy(quickImportInProgress = true, quickImportError = null, quickImportResult = null) }
        viewModelScope.launch {
            var databaseEntriesAdded = 0
            var dayFoodsAdded = 0

            try {
                if (options.addFoodsToDatabase) {
                    databaseEntries.forEach {
                        databaseAddEntry(context, false, it)
                    }
                    databaseSortByName()
                    databaseQuickselectUpdate()
                    databaseLetterReset()
                    databaseWriteToCSV(context)
                    databaseEntriesAdded = databaseEntries.size
                }

                if (options.addFoodsToDay) {
                    databaseEntries.zip(plan.foodDrafts).forEach { (entry, draft) ->
                        _uiState.value.day.addComponent(draft.grams, entry)
                    }
                    dayWriteToCSV(context)
                    dayFoodsAdded = databaseEntries.size
                }

                val healthResult = if (options.writeHealthConnect) {
                    healthConnectManager.insertQuickMealNutrition(plan.healthPayloads)
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        quickImportInProgress = false,
                        quickImportResult = QuickImportResult(
                            databaseEntriesAdded = databaseEntriesAdded,
                            dayFoodsAdded = dayFoodsAdded,
                            healthWriteResult = healthResult,
                        ),
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(quickImportInProgress = false) }
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        quickImportInProgress = false,
                        quickImportError = e.message ?: "Quick Import failed.",
                    )
                }
            }
        }
    }

    private fun QuickImportDatabaseEntryDraft.toDatabaseEntry(context: Context): DatabaseEntry {
        return DatabaseEntry(
            name = name,
            nutrients = Nutrients(nutrientsPer100g.toAppValues().toMutableStateList(), context = context),
            customWeights = CustomWeights(context = context),
            quickselect = true,
            context = context,
        )
    }

    fun archiveAddEntry(
        date: LocalDate,
        bodyWeight: String,
        nutrients: Nutrients,
        context: Context
    ) {
        if (date.isAfter(LocalDate.now())) {
            throw IllegalStateException(context.getString(R.string.error_archive_date_future))
        }
        if (_uiState.value.archive.entries.any { it.first == date }) {
            throw IllegalStateException(context.getString(R.string.error_archive_date_exists))
        }
        _uiState.value.archive.addEntry(date, bodyWeight, nutrients)
        archiveWriteToCSV(context)
        if (uiState.value.healthConnectSyncEnabled) {
            viewModelScope.launch {
                healthConnectManager.syncSingleEntry(date, bodyWeight.toDoubleOrNull() ?: 0.0, nutrients)
                if (uiState.value.healthConnectToastsEnabled) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.toast_hc_entry_added), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun archiveEditEntry(
        index: Int,
        date: LocalDate,
        bodyWeight: String,
        nutrients: Nutrients,
        context: Context
    ) {
        if (date.isAfter(LocalDate.now())) {
            throw IllegalStateException(context.getString(R.string.error_archive_date_future))
        }
        // Delete old entry silently (regarding toasts)
        val oldEntry = _uiState.value.archive.entries[index]
        _uiState.value.archive.deleteEntry(index)
        
        // Check if new date collides with any REMAINING entries
        if (_uiState.value.archive.entries.any { it.first == date }) {
            // Restore if validation fails (to keep state consistent)
            _uiState.value.archive.addEntry(oldEntry.first, oldEntry.second.toBodyWeight(), oldEntry.third)
            throw IllegalStateException(context.getString(R.string.error_archive_date_exists))
        }

        // Add new entry
        _uiState.value.archive.addEntry(date, bodyWeight, nutrients)
        archiveWriteToCSV(context)
        
        if (uiState.value.healthConnectSyncEnabled) {
            viewModelScope.launch {
                // Remove old date from Health Connect if it changed
                if (oldEntry.first != date) {
                    healthConnectManager.deleteSingleEntry(oldEntry.first)
                }
                // Sync new/updated entry
                healthConnectManager.syncSingleEntry(date, bodyWeight.toDoubleOrNull() ?: 0.0, nutrients)
                if (uiState.value.healthConnectToastsEnabled) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.toast_hc_entry_edited), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun dayEditFoodWeight(weightString: String, index: Int, context: Context) {
        checkValidNumber(weightString, context.getString(R.string.weight), context)
        _uiState.value.day.editComponentWeight(
            weightString.toDouble().toFormattedString(true).toDouble(), index
        )
        dayWriteToCSV(context)
    }

    fun dayDeleteFood(index: Int, context: Context) {
        _uiState.value.day.deleteComponent(index)
        dayWriteToCSV(context)
    }

    fun databaseSortByName() {
        _uiState.value.database.sortBy { it.name }
    }

    fun setAlertDialogArchiveReset(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogArchiveReset = bool
            )
        }
    }
    fun setAlertDialogDayReset(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogDayReset = bool
            )
        }
    }
    fun setDialogLanguageInfo(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                dialogLanguageInfo = bool
            )
        }
    }
    fun setAlertDialogDatabaseReset(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogDatabaseReset = bool
            )
        }
    }

    fun setAlertDialogArchiveImport(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogArchiveImport = bool
            )
        }
    }
    fun setAlertDialogDatabaseImport(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogDatabaseImport = bool
            )
        }
    }


    fun setAlertDialogHealthConnectSync(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogHealthConnectSync = bool
            )
        }
    }

    fun setAlertDialogArchiveDelete(bool: Boolean, index: Int = -1){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogArchiveDelete = bool,
                indexArchiveDelete = index
            )
        }
    }

    fun setAlertDialogDatabaseDelete(bool: Boolean, index: Int = -1){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogDatabaseDelete = bool,
                indexDatabaseDelete = index
            )
        }
    }

    fun updateCurrentComboComponentWeight(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                inputCurrentComboComponentWeight = string
            )
        }
    }


    fun databaseLetterFilter(char: Char) {
        val list = mutableListOf<Int>()
        uiState.value.database.forEachIndexed { index, food ->
            if (food.name[0] == char) list.add(index)
        }
        _uiState.update { currentState ->
            currentState.copy(
                databaseLetter = list.toMutableStateList()
            )
        }
    }
    fun databaseLetterReset() {
        _uiState.update { currentState ->
            currentState.copy(
                databaseLetter = mutableStateListOf()
            )
        }
    }


    fun databaseQuickselectUpdate() {
        val temp: MutableList<Pair<Int, DatabaseEntry>> = mutableListOf()
        uiState.value.database.forEachIndexed { index, food ->
            if (food.quickselect) {
                temp.add(Pair(index, food))
            }
            _uiState.update { currentState ->
                currentState.copy(
                    databaseQuickselect = temp.toMutableStateList()
                )
            }
        }
    }


    fun updateOptionsSheetVisible(boolean: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                optionsSheetVisible = boolean,
                optionsSheetPage = "main" // Reset to main when opening/closing
            )
        }
    }



    fun databaseUpdateFromCSV(context: Context) {
        try {
            val folder = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(folder, "database.csv")
            val rows: List<List<String>> = csvReader().readAll(file.inputStream())
            check(rows.isNotEmpty()) { context.getString(R.string.database) + ": " + context.getString(R.string.csv_wrong_number_fields) }
            check(rows[0].size==11){ context.getString(R.string.database) + ": " + context.getString(R.string.csv_wrong_number_fields)}
            val parsedEntries = rows.drop(1).map { csvLine -> DatabaseEntry.fromCSV(csvLine, context) }
            _uiState.value.database.clear()
            _uiState.value.database.addAll(parsedEntries)
            databaseSortByName()
            databaseQuickselectUpdate()
            databaseLetterReset()
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException(context.getString(R.string.database) + ": " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    fun databaseResetCSV(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "database.csv")
        if (!file.exists() || overwriteIfExists) {
            context.resources.openRawResource(R.raw.database).copyTo(file.outputStream())
        }
    }

    fun archiveResetCSV(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "archive.csv")
        if (!file.exists() || overwriteIfExists) {
            context.resources.openRawResource(R.raw.archive).copyTo(file.outputStream())
        }
    }

    fun dayResetCSV(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "day.csv")
        if (!file.exists() || overwriteIfExists) {
            context.resources.openRawResource(R.raw.day).copyTo(file.outputStream())
        }
    }

    fun goalsResetCSV(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "goals.csv")
        if (!file.exists() || overwriteIfExists) {
            csvWriter().open(file) {
                GoalsCsv.defaultRows().forEach { writeRow(it) }
            }
        }
    }

    private fun databaseWriteToCSV(context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "database.csv")
        csvWriter().open(file) {
            writeRow(
                listOf("Name") + NUTRIENT_PROPERTIES.map { it.nameForCSV } + listOf(
                    "CustomWeights",
                    "Quickselect"
                )
            )
            uiState.value.database.forEach {
                writeRow(it.stringCSV)
            }
        }
    }

    private fun dayWriteToCSV(context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "day.csv")
        csvWriter().open(file) {
            uiState.value.day.getCsvString().forEach {
                writeRow(it)
            }
        }
    }


    fun dayUpdateFromCSV(context: Context) {
        try {
            val folder = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(folder, "day.csv")
            val rows: List<List<String>> = csvReader().readAll(file.inputStream())
            _uiState.update { currentState ->
                currentState.copy(
                    day = Combo.fromCSV(rows,context)
                )
            }
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException(context.getString(R.string.day)+ ": " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    fun goalsUpdateFromCSV(context: Context) {
        try {
            val folder = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(folder, "goals.csv")
            if (!file.exists()) return
            val rows: List<List<String>> = file.readLines()
                .filter { it.isNotBlank() }
                .map { line -> line.split(",") }
            _uiState.update { currentState ->
                currentState.copy(goals = GoalsCsv.fromRows(rows))
            }
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException("Goals: " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    private fun goalsWriteToCSV(context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "goals.csv")
        csvWriter().open(file) {
            GoalsCsv.toRows(uiState.value.goals).forEach { writeRow(it) }
        }
    }

    fun optionsUpdateFromFile(context: Context) {
        try {
            val folder = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(folder, "options.csv")
            if (!file.exists()) return
            val rows: List<List<String>> = csvReader().readAll(file.inputStream())
            if (rows.isNotEmpty()) {
                val themeRow = rows[0]
                _uiState.update { currentState ->
                    currentState.copy(
                        themeUserSetting = if (themeRow.contains("dark")) AppTheme.MODE_NIGHT else if (themeRow.contains("light")) AppTheme.MODE_DAY else AppTheme.MODE_AUTO
                    )
                }

                viewModelScope.launch {
                    val granted = healthConnectManager.hasAllPermissions()
                    _uiState.update { currentState ->
                        var newState = currentState
                        if (rows.size > 1) {
                            val hcRow = rows[1]
                            val savedSyncEnabled = hcRow.contains("true")
                            newState = newState.copy(
                                healthConnectSyncEnabled = savedSyncEnabled && granted
                            )
                        }
                        if (rows.size > 2) {
                            val toastRow = rows[2]
                            val savedToastsEnabled = toastRow.contains("true")
                            newState = newState.copy(
                                healthConnectToastsEnabled = savedToastsEnabled && granted
                            )
                        }
                        newState
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to default if file is old or corrupted
        }
    }

    fun optionsWriteToFile(context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "options.csv")
        csvWriter().open(file) {
            val theme = when (uiState.value.themeUserSetting) {
                AppTheme.MODE_NIGHT -> "dark"
                AppTheme.MODE_DAY -> "light"
                AppTheme.MODE_AUTO -> "auto"
            }
            writeRow(listOf(theme))
            writeRow(listOf(uiState.value.healthConnectSyncEnabled.toString()))
            writeRow(listOf(uiState.value.healthConnectToastsEnabled.toString()))
        }
    }

    fun optionsResetFile(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "options.csv")
        if (!file.exists() || overwriteIfExists) {
            file.writeText("dark")
        }
    }

    private fun archiveWriteToCSV(context: Context) {
        val folder = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(folder, "archive.csv")
        csvWriter().open(file) {
            uiState.value.archive.getCsvString().forEach {
                writeRow(it)
            }
        }
    }

    fun archiveUpdateFromCSV(context: Context) {
        try {
            val folder = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(folder, "archive.csv")
            val rows: List<List<String>> = csvReader().readAll(file.inputStream())
            check(rows.isNotEmpty()) { context.getString(R.string.archive)+ ": " + context.getString(R.string.csv_wrong_number_fields) }
            check(rows[0].size==10){context.getString(R.string.archive)+ ": " + context.getString(R.string.csv_wrong_number_fields)}
            _uiState.update { currentState ->
                currentState.copy(
                    archive = Archive.fromCSV(rows,context)
                )
            }
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException(context.getString(R.string.archive)+ ": " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    fun setNameFoodDayAdd(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                nameFoodDayAdd = string
            )
        }
    }

    fun setNameFoodDayEdit(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                nameFoodDayEdit = string
            )
        }
    }
}
