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
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
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
    private val _uiState = MutableStateFlow(AppUiState(archive = Archive(context = app.applicationContext),day = Combo(context = app.applicationContext), currentCombo = Combo(context = app.applicationContext)))
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
                currentState.copy(
                    healthConnectPermissionsGranted = allGranted,
                    healthConnectAnyPermissionsGranted = anyGranted,
                    healthConnectSyncEnabled = newSyncStatus,
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

    fun syncHealthConnect() {
        viewModelScope.launch {
            if (healthConnectManager.hasAllPermissions()) {
                healthConnectManager.syncArchive(uiState.value.archive)
            } else {
                Toast.makeText(getApplication<Application>().applicationContext, getApplication<Application>().getString(R.string.health_connect_permissions_missing), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun restoreArchiveFromHealthConnect(context: Context) {
        viewModelScope.launch {
            if (healthConnectManager.hasAllPermissions()) {
                val hcData = healthConnectManager.readArchiveFromHealthConnect()
                if (hcData.isNotEmpty()) {
                    _uiState.update { currentState ->
                        val newArchive = currentState.archive.copy(entries = hcData.toMutableStateList())
                        newArchive.updateAverageNutrients()
                        newArchive.sortByDate()
                        currentState.copy(archive = newArchive)
                    }
                    archiveWriteToCSV(context)
                    Toast.makeText(context, context.getString(R.string.toast_hc_full_archive_restored), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No data found in Health Connect", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, context.getString(R.string.health_connect_permissions_missing), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setAlertDialogHealthConnectActivation(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogHealthConnectActivation = bool
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
            context,
            true,
            DatabaseEntry(
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

    fun archiveDeleteEntry(index: Int, context: Context, showToast: Boolean = true) {
        val entry = _uiState.value.archive.entries[index]
        _uiState.value.archive.deleteEntry(index)
        archiveWriteToCSV(context)
        if (uiState.value.healthConnectSyncEnabled) {
            viewModelScope.launch {
                healthConnectManager.deleteSingleEntry(entry.first)
                if (showToast) {
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

    fun currentComboUpdateOverallWeight(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                currentCombo = uiState.value.currentCombo.copy(inputOverallWeight = string)
            )
        }
    }

    fun currentComboUpdateName(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                currentCombo = uiState.value.currentCombo.copy(name = string)
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

    fun currentComboReset(context: Context) {
        _uiState.update { currentState ->
            currentState.copy(
                currentCombo = Combo(context = context)
            )
        }
        currentComboWriteToCSV(context)
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

    fun currentComboAddComponent(weight: String, databaseEntry: DatabaseEntry, context: Context) {
        checkValidNumber(weight, context.getString(R.string.weight), context)
        _uiState.value.currentCombo.addComponent(weight.toDouble(), databaseEntry)
        currentComboWriteToCSV(context)
    }

    fun dayAddFood(weight: String, databaseEntry: DatabaseEntry, context: Context) {
        checkValidNumber(weight, context.getString(R.string.weight), context)
        _uiState.value.day.addComponent(weight.toDouble(), databaseEntry)
        dayWriteToCSV(context)
    }

    fun archiveAddEntry(
        date: LocalDate,
        bodyWeight: String,
        nutrients: Nutrients,
        context: Context,
        showToast: Boolean = true
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
                if (showToast) {
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
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.toast_hc_entry_edited), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun currentComboDeleteComponent(index: Int, context: Context) {
        _uiState.value.currentCombo.deleteComponent(index)
        currentComboWriteToCSV(context)
    }


    fun currentComboEditComponentWeight(weightString: String, index: Int, context: Context) {
        checkValidNumber(weightString, context.getString(R.string.weight), context)
        _uiState.value.currentCombo.editComponentWeight(
            weightString.toDouble().toFormattedString(true).toDouble(), index
        )
        currentComboWriteToCSV(context)
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
    fun setAlertDialogRecipeReset(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogRecipeReset = bool
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

    fun setAlertDialogHealthConnectRestore(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                alertDialogHealthConnectRestore = bool
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
            val file = File(context.getExternalFilesDir(null), "database.csv")
            val rows: List<List<String>> = csvReader().readAll(file.inputStream())
            check(rows[0].size==11){ context.getString(R.string.database) + ": " + context.getString(R.string.csv_wrong_number_fields)}
            databaseDeleteAll(context, false)
            rows.forEachIndexed { index, csvLine ->
                if (index > 0) databaseAddEntry(context, false, DatabaseEntry.fromCSV(csvLine,context))
            }
            databaseSortByName()
            databaseQuickselectUpdate()
            databaseLetterReset()
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException(context.getString(R.string.database) + ": " + context.getString(R.string.csv_wrong_number_fields))
        }
    }

    fun databaseResetCSV(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null)
        val file = File(folder, "database.csv")
        if (!file.exists() || overwriteIfExists) {
            context.resources.openRawResource(R.raw.database).copyTo(file.outputStream())
        }
    }

    fun currentComboResetCSV(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null)
        val file = File(folder, "currentrecipe.csv")
        if (!file.exists() || overwriteIfExists) {
            context.resources.openRawResource(R.raw.currentrecipe).copyTo(file.outputStream())
        }
    }

    fun archiveResetCSV(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null)
        val file = File(folder, "archive.csv")
        if (!file.exists() || overwriteIfExists) {
            context.resources.openRawResource(R.raw.archive).copyTo(file.outputStream())
        }
    }

    fun dayResetCSV(overwriteIfExists: Boolean, context: Context) {
        val folder = context.getExternalFilesDir(null)
        val file = File(folder, "day.csv")
        if (!file.exists() || overwriteIfExists) {
            context.resources.openRawResource(R.raw.day).copyTo(file.outputStream())
        }
    }

    private fun databaseWriteToCSV(context: Context) {
        val file = File(context.getExternalFilesDir(null), "database.csv")
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

    private fun currentComboWriteToCSV(context: Context) {
        val file = File(context.getExternalFilesDir(null), "currentrecipe.csv")
        csvWriter().open(file) {
            uiState.value.currentCombo.getCsvString().forEach {
                writeRow(it)
            }
        }
    }

    private fun dayWriteToCSV(context: Context) {
        val file = File(context.getExternalFilesDir(null), "day.csv")
        csvWriter().open(file) {
            uiState.value.day.getCsvString().forEach {
                writeRow(it)
            }
        }
    }

    fun currentComboUpdateFromCSV(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(null), "currentrecipe.csv")
            val rows: List<List<String>> = csvReader().readAll(file.inputStream())
            _uiState.update { currentState ->
                currentState.copy(
                    currentCombo = Combo.fromCSV(rows,context)
                )
            }
        } catch (_: CSVFieldNumDifferentException) {
            throw IllegalStateException(context.getString(R.string.recipe) + ": " + context.getString(R.string.csv_wrong_number_fields))
        }
    }


    fun dayUpdateFromCSV(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(null), "day.csv")
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

    fun optionsUpdateFromFile(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(null), "options.csv")
            if (!file.exists()) return
            val rows: List<List<String>> = csvReader().readAll(file.inputStream())
            if (rows.isNotEmpty()) {
                val themeRow = rows[0]
                _uiState.update { currentState ->
                    currentState.copy(
                        themeUserSetting = if (themeRow.contains("dark")) AppTheme.MODE_NIGHT else if (themeRow.contains("light")) AppTheme.MODE_DAY else AppTheme.MODE_AUTO
                    )
                }
                if (rows.size > 1) {
                    val hcRow = rows[1]
                    val savedSyncEnabled = hcRow.contains("true")
                    viewModelScope.launch {
                        val granted = healthConnectManager.hasAllPermissions()
                        _uiState.update { currentState ->
                            currentState.copy(
                                healthConnectSyncEnabled = savedSyncEnabled && granted
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to default if file is old or corrupted
        }
    }

    fun optionsWriteToFile(context: Context) {
        val file = File(context.getExternalFilesDir(null), "options.csv")
        csvWriter().open(file) {
            val theme = when (uiState.value.themeUserSetting) {
                AppTheme.MODE_NIGHT -> "dark"
                AppTheme.MODE_DAY -> "light"
                AppTheme.MODE_AUTO -> "auto"
            }
            writeRow(listOf(theme))
            writeRow(listOf(uiState.value.healthConnectSyncEnabled.toString()))
        }
    }

    fun optionsResetFile(overwriteIfExists: Boolean, context: Context) {
        val file = File(context.getExternalFilesDir(null), "options.csv")
        if (!file.exists() || overwriteIfExists) {
            File(context.getExternalFilesDir(null), "options.csv").writeText("dark")
        }
    }

    private fun archiveWriteToCSV(context: Context) {
        val file = File(context.getExternalFilesDir(null), "archive.csv")
        csvWriter().open(file) {
            uiState.value.archive.getCsvString().forEach {
                writeRow(it)
            }
        }
    }

    fun archiveUpdateFromCSV(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(null), "archive.csv")
            val rows: List<List<String>> = csvReader().readAll(file.inputStream())
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

    fun setNameFoodCombineAdd(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                nameFoodCombineAdd = string
            )
        }
    }

    fun setNameFoodCombineEdit(string: String) {
        _uiState.update { currentState ->
            currentState.copy(
                nameFoodCombineEdit = string
            )
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
