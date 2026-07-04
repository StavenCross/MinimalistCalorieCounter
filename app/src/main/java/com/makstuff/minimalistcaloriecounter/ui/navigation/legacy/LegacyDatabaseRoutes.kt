package com.makstuff.minimalistcaloriecounter.ui.navigation.legacy

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenDatabaseEntry
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenShowFoodAll

/**
 * Registers legacy database food maintenance routes.
 *
 * Add Meal can still write local food backups, but these screens are now troubleshooting tools.
 * Keeping them isolated prevents the old database editing flow from leaking into current routes.
 */
fun NavGraphBuilder.legacyDatabaseRoutes(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    keyboardController: SoftwareKeyboardController?,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onEditDatabaseEntry: (Int) -> Unit,
) {
    composable(AppRoutes.CREATE_HOME) {
        fun onCreateFood() {
            keyboardController?.hide()
            try {
                viewModel.databaseCreateEntryFromInput(context)
                onNavigate(AppRoutes.DAY_HOME)
            } catch (e: IllegalStateException) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
        ScreenDatabaseEntry(
            inputName = uiState.inputDatabaseEntryCreateName,
            inputNutrients = uiState.inputDatabaseEntryCreateNutrients,
            inputQuickSelectBoolean = uiState.inputDatabaseEntryCreateQuickselect,
            inputQuickSelectWeights = uiState.inputDatabaseEntryCreateCustomWeights,
            onUpdateName = { viewModel.updateDatabaseEntryCreateName(it) },
            onUpdateNutrient = { string, index -> viewModel.updateDatabaseEntryCreateNutrient(string, index) },
            onUpdateQuickSelectWeights = { viewModel.updateDatabaseEntryCreateCustomWeights(it) },
            onConfirm = { onCreateFood() },
            onToggleSwitch = { viewModel.toggleDatabaseEntryCreateQuickselect() },
            listOfTextButtons = listOf(
                Pair(stringResource(R.string.button_cancel)) {
                    viewModel.resetDatabaseEntryCreateAllInput()
                    onNavigate(AppRoutes.DAY_HOME)
                },
                Pair(stringResource(R.string.button_clear_input)) { viewModel.resetDatabaseEntryCreateAllInput() },
                Pair(stringResource(R.string.button_create)) { onCreateFood() },
            ),
            context = context,
        )
    }

    composable(AppRoutes.DATABASE_EDIT_ENTRY) {
        val index = it.arguments?.getString("index")?.toIntOrNull()
        if (index != null && index < uiState.database.size) {
            fun onConfirmEdit() {
                keyboardController?.hide()
                try {
                    viewModel.databaseEditEntryFromInput(index, context)
                    onNavigateBack()
                } catch (e: IllegalStateException) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }
            ScreenDatabaseEntry(
                inputName = uiState.inputDatabaseEntryEditName,
                inputNutrients = uiState.inputDatabaseEntryEditNutrients,
                inputQuickSelectBoolean = uiState.inputDatabaseEntryEditQuickselect,
                inputQuickSelectWeights = uiState.inputDatabaseEntryEditCustomWeights,
                onUpdateName = { viewModel.updateDatabaseEntryEditName(it) },
                onUpdateNutrient = { string, ind -> viewModel.updateDatabaseEntryEditNutrient(string, ind) },
                onUpdateQuickSelectWeights = { viewModel.updateDatabaseEntryEditCustomWeights(it) },
                onConfirm = { onConfirmEdit() },
                onToggleSwitch = { viewModel.toggleDatabaseEntryEditQuickselect() },
                listOfTextButtons = listOf(
                    Pair(stringResource(R.string.button_cancel)) { onNavigateBack() },
                    Pair(stringResource(R.string.button_delete)) { viewModel.setAlertDialogDatabaseDelete(true, index) },
                    Pair(stringResource(R.string.button_save_changes)) { onConfirmEdit() },
                ),
                context = context,
            )
        }
    }

    composable(AppRoutes.DATABASE_HOME) {
        ScreenShowFoodAll(
            database = uiState.database,
            onFoodClicked = { onEditDatabaseEntry(it) },
            onFoodLongClicked = { onEditDatabaseEntry(it) },
        )
    }
}
