package com.makstuff.minimalistcaloriecounter

import android.content.Context
import com.makstuff.minimalistcaloriecounter.essentials.NavButton
import com.makstuff.minimalistcaloriecounter.ui.settings.SettingsSheet
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import kotlinx.coroutines.flow.update

internal class AppViewModelUiActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    fun requestNavigation(route: String) {
        env.state.update { currentState ->
            currentState.copy(automationRouteRequest = route)
        }
    }

    fun clearNavigation(route: String? = null) {
        env.state.update { currentState ->
            if (route == null || currentState.automationRouteRequest == route) {
                currentState.copy(automationRouteRequest = null)
            } else {
                currentState
            }
        }
    }

    fun updateActiveSettingsSheet(sheet: SettingsSheet?) {
        env.state.update { currentState ->
            currentState.copy(activeSettingsSheet = sheet)
        }
    }

    fun updateQuickImportSettingsVisible(visible: Boolean) {
        env.state.update { currentState ->
            currentState.copy(quickImportSettingsVisible = visible)
        }
    }

    fun setHealthConnectActivationDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogHealthConnectActivation = visible) }
    }

    fun setHealthConnectToastsDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogHealthConnectToasts = visible) }
    }

    fun setHealthConnectPermissionsDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogHealthConnectPermissions = visible) }
    }

    fun setLoadingToFalse() {
        env.state.update { currentState ->
            currentState.copy(loading = false)
        }
    }

    fun updateTopBarTitle(title: String) {
        env.state.update { currentState ->
            currentState.copy(topBarTitle = title)
        }
    }

    fun setTheme(theme: AppTheme, context: Context) {
        env.state.update { currentState ->
            currentState.copy(themeUserSetting = theme)
        }
        viewModel.optionsWriteToFile(context)
    }

    fun updateNavigationBarHighlight(button: NavButton) {
        env.state.update { currentState ->
            currentState.copy(navigationBarHighlight = button)
        }
    }

    fun setArchiveResetDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogArchiveReset = visible) }
    }

    fun setDayResetDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogDayReset = visible) }
    }

    fun setLanguageInfoDialog(visible: Boolean) {
        env.state.update { it.copy(dialogLanguageInfo = visible) }
    }

    fun setDatabaseResetDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogDatabaseReset = visible) }
    }

    fun setArchiveImportDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogArchiveImport = visible) }
    }

    fun setDatabaseImportDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogDatabaseImport = visible) }
    }

    fun setHealthConnectSyncDialog(visible: Boolean) {
        env.state.update { it.copy(alertDialogHealthConnectSync = visible) }
    }

    fun setArchiveDeleteDialog(visible: Boolean, index: Int = -1) {
        env.state.update { currentState ->
            currentState.copy(
                alertDialogArchiveDelete = visible,
                indexArchiveDelete = index,
            )
        }
    }

    fun setDatabaseDeleteDialog(visible: Boolean, index: Int = -1) {
        env.state.update { currentState ->
            currentState.copy(
                alertDialogDatabaseDelete = visible,
                indexDatabaseDelete = index,
            )
        }
    }

    fun updateCurrentComboComponentWeight(weight: String) {
        env.state.update { currentState ->
            currentState.copy(inputCurrentComboComponentWeight = weight)
        }
    }

    fun setNameFoodDayAdd(name: String) {
        env.state.update { currentState ->
            currentState.copy(nameFoodDayAdd = name)
        }
    }

    fun setNameFoodDayEdit(name: String) {
        env.state.update { currentState ->
            currentState.copy(nameFoodDayEdit = name)
        }
    }
}
