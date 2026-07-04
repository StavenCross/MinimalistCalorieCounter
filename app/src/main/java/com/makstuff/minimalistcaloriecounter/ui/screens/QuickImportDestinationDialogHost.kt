package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.runtime.Composable
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.AppViewModel

/**
 * Owns the Add Meal destination dialog.
 *
 * The dialog changes local destination preferences immediately through the view model. Keeping it
 * outside the app shell lets the route host stay focused on navigation instead of feature settings.
 */
@Composable
fun QuickImportDestinationDialogHost(
    uiState: AppUiState,
    viewModel: AppViewModel,
) {
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
}
