package com.makstuff.minimalistcaloriecounter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.DatabaseEntry
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.HistoricalMealImportPreview
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
import com.makstuff.minimalistcaloriecounter.essentials.NAV_DAY
import com.makstuff.minimalistcaloriecounter.essentials.NavButton
import com.makstuff.minimalistcaloriecounter.ui.settings.SettingsSheet
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalDateTime


fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
data class AppUiState(
    //Data collections
    val database: MutableList<DatabaseEntry> = mutableStateListOf(),
    val databaseLetter: SnapshotStateList<Int> = mutableStateListOf(),
    val databaseQuickselect: MutableList<Pair<Int, DatabaseEntry>> = mutableStateListOf(),
    val archive: Archive,

    val day: Combo,

    val navigationBarHighlight: NavButton = NAV_DAY,
    val topBarTitle: String = "",
    val automationRouteRequest: String? = null,
    val activeSettingsSheet: SettingsSheet? = null,
    val quickImportSettingsVisible: Boolean = false,
    /*
    CAREFUL! The list below MUST contain the correct number of empty nutrient value strings.
    Yes, this is indeed the case. I don't know if there is a better solution.
     */
    val inputDatabaseEntryCreateName: String = "",
    val inputDatabaseEntryCreateNutrients: MutableList<String> = mutableStateListOf(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        ""
    ),
    val inputDatabaseEntryCreateQuickselect: Boolean = false,
    val inputDatabaseEntryCreateCustomWeights: String = "",

    val inputDatabaseEntryEditName: String = "",
    val inputDatabaseEntryEditNutrients: MutableList<String> = mutableStateListOf(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        ""
    ),
    val inputDatabaseEntryEditQuickselect: Boolean = false,
    val inputDatabaseEntryEditCustomWeights: String = "",

    val inputArchiveEntryBodyWeight: String = "",
    val inputArchiveEntryNutrients: MutableList<String> = mutableStateListOf(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        ""
    ),
    val inputArchiveEntryDate: LocalDate = LocalDateTime.now().minusHours(12).toLocalDate(),

    val inputCurrentComboComponentWeight: String = "",

    val inputQuickImportText: String = "",
    val inputQuickImportDateTime: LocalDateTime = LocalDateTime.now(),
    val quickImportSnackOverride: Boolean = false,
    val quickImportMealTypeOverride: QuickImportMealType? = null,
    val quickImportMeal: QuickImportMeal? = null,
    val quickImportError: String? = null,
    val quickImportResult: QuickImportResult? = null,
    val quickImportSuccessMessage: String? = null,
    val quickImportSuccessToken: Long = 0L,
    val quickImportAddFoodsToDatabase: Boolean = true,
    val quickImportAddFoodsToDay: Boolean = true,
    val quickImportWriteHealthConnect: Boolean = true,
    val quickImportInProgress: Boolean = false,
    val goals: Goals = Goals(),

    val nameFoodDayAdd: String = "",
    val nameFoodDayEdit: String = "",

    val themeUserSetting: AppTheme = AppTheme.MODE_AUTO,
    val healthConnectPermissionsGranted: Boolean = false,
    val healthConnectExportPermissionsGranted: Boolean = false,
    val healthConnectAnyPermissionsGranted: Boolean = false,
    val healthConnectSyncEnabled: Boolean = false,
    val healthConnectToastsEnabled: Boolean = false,
    val healthConnectSyncProgress: Float? = null,
    val healthConnectSyncCurrentCount: Int = 0,
    val healthConnectSyncTotalCount: Int = 0,
    val healthConnectSyncMessage: String? = null,
    val healthConnectViewerDate: LocalDate = LocalDateTime.now().minusHours(12).toLocalDate(),
    val healthConnectViewerMeals: List<HealthConnectNutritionMeal> = emptyList(),
    val healthConnectViewerLoading: Boolean = false,
    val healthConnectViewerMessage: String? = null,
    val historicalMealImportPreview: HistoricalMealImportPreview? = null,
    val historicalMealImportMessage: String? = null,
    val historicalMealImportInProgress: Boolean = false,
    val healthConnectExportStartDate: LocalDate = LocalDateTime.now().minusHours(12).toLocalDate(),
    val healthConnectExportEndDate: LocalDate = LocalDateTime.now().minusHours(12).toLocalDate(),
    val healthConnectExportMessage: String? = null,
    val healthConnectExportInProgress: Boolean = false,
    val healthConnectNutritionCleanupStartDate: LocalDate = LocalDateTime.now().minusHours(12).toLocalDate(),
    val healthConnectNutritionCleanupEndDate: LocalDate = LocalDateTime.now().minusHours(12).toLocalDate(),
    val loading: Boolean = true,

    val alertDialogArchiveReset: Boolean = false,
    val alertDialogDatabaseReset: Boolean = false,
    val alertDialogArchiveImport: Boolean = false,
    val alertDialogDatabaseImport: Boolean = false,
    val alertDialogHealthConnectSync: Boolean = false,
    val alertDialogHealthConnectActivation: Boolean = false,
    val alertDialogHealthConnectToasts: Boolean = false,
    val alertDialogHealthConnectPermissions: Boolean = false,
    val alertDialogDayReset: Boolean = false,

    val alertDialogArchiveDelete: Boolean = false,
    val indexArchiveDelete: Int = -1,
    val alertDialogDatabaseDelete: Boolean = false,
    val indexDatabaseDelete: Int = -1,
) {
    val quickImportMealType: QuickImportMealType
        get() = quickImportMealTypeOverride
            ?: if (quickImportSnackOverride) {
                QuickImportMealType.Snack
            } else {
                QuickImportMealType.inferFrom(inputQuickImportDateTime)
            }
}
