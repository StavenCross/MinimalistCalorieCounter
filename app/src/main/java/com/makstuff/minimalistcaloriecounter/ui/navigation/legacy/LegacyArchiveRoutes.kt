package com.makstuff.minimalistcaloriecounter.ui.navigation.legacy

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.essentials.toBodyWeight
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import com.makstuff.minimalistcaloriecounter.ui.reused.ScrollColumn
import com.makstuff.minimalistcaloriecounter.ui.reused.TileArchive
import com.makstuff.minimalistcaloriecounter.ui.reused.TileLegendArchive
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenInputOrEditArchive
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenWithHoverCard

/**
 * Registers the legacy archive maintenance routes.
 *
 * These routes preserve the original archive CSV/database troubleshooting surface while the
 * primary workflow moves to Add Meal, Meals, Goals, and Health Connect. Keep this file isolated so
 * future removal of the archive UI does not disturb the modern route host.
 */
fun NavGraphBuilder.legacyArchiveRoutes(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    keyboardController: SoftwareKeyboardController?,
    onNavigate: (String) -> Unit,
) {
    composable(AppRoutes.ARCHIVE_HOME) {
        ScreenWithHoverCard(
            nutrients = uiState.archive.averageNutrients,
            contentAbove = {},
            listOfTextButtons = listOf(
                Pair(stringResource(R.string.button_create_entry_manually)) {
                    viewModel.resetArchiveEntryAllInput()
                    onNavigate(AppRoutes.ARCHIVE_CREATE_ENTRY_MANUALLY)
                },
            ),
            content = {
                Column {
                    TileLegendArchive()
                    ScrollColumn(items = uiState.archive.entries.mapIndexed { index, archiveEntry ->
                        {
                            TileArchive(
                                archiveEntry = archiveEntry,
                                onClick = {
                                    viewModel.updateArchiveEntryDate(uiState.archive.entries[index].first)
                                    viewModel.updateArchiveEntryBodyWeight(uiState.archive.entries[index].second.toBodyWeight())
                                    viewModel.updateArchiveEntryAllNutrients(
                                        uiState.archive.entries[index].third.stringValues(true).toMutableList()
                                    )
                                    onNavigate(AppRoutes.archiveEditEntry(index))
                                },
                            )
                        }
                    })
                }
            },
            context = context,
        )
    }

    composable(AppRoutes.ARCHIVE_CREATE_ENTRY_MANUALLY) {
        ArchiveEntryEditor(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            keyboardController = keyboardController,
            onNavigate = onNavigate,
            onConfirmSuccess = { onNavigate(AppRoutes.ARCHIVE_HOME) },
            buttons = { onConfirm ->
                listOf(
                    Pair(stringResource(R.string.button_cancel)) { onNavigate(AppRoutes.ARCHIVE_HOME) },
                    Pair(stringResource(R.string.button_create_new_archive_entry)) { onConfirm() },
                )
            },
        )
    }

    composable(AppRoutes.ARCHIVE_CREATE_ENTRY_FROM_DAY) {
        ArchiveEntryEditor(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            keyboardController = keyboardController,
            onNavigate = onNavigate,
            onConfirmSuccess = {
                viewModel.dayReset(context)
                onNavigate(AppRoutes.ARCHIVE_HOME)
            },
            buttons = { onConfirm ->
                listOf(
                    Pair(stringResource(R.string.button_cancel)) { onNavigate(AppRoutes.DAY_HOME) },
                    Pair(stringResource(R.string.button_turn_day_to_archive_entry)) { onConfirm() },
                )
            },
        )
    }

    composable(AppRoutes.ARCHIVE_EDIT_ENTRY) {
        val index = it.arguments?.getString("index")?.toIntOrNull()
        if (index != null && index < uiState.archive.entries.size) {
            ArchiveEntryEditor(
                uiState = uiState,
                viewModel = viewModel,
                context = context,
                keyboardController = keyboardController,
                onNavigate = onNavigate,
                editIndex = index,
                onConfirmSuccess = { onNavigate(AppRoutes.ARCHIVE_HOME) },
                buttons = { onConfirm ->
                    listOf(
                        Pair(stringResource(R.string.button_cancel)) { onNavigate(AppRoutes.ARCHIVE_HOME) },
                        Pair(stringResource(R.string.button_delete)) {
                            viewModel.setAlertDialogArchiveDelete(true, index)
                        },
                        Pair(stringResource(R.string.button_save_changes)) { onConfirm() },
                    )
                },
            )
        }
    }
}

@Composable
private fun ArchiveEntryEditor(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    keyboardController: SoftwareKeyboardController?,
    onNavigate: (String) -> Unit,
    onConfirmSuccess: () -> Unit,
    buttons: @Composable ((() -> Unit) -> List<Pair<String, () -> Unit>>),
    editIndex: Int? = null,
) {
    fun onConfirm() {
        keyboardController?.hide()
        try {
            if (editIndex == null) {
                viewModel.archiveAddEntry(
                    date = uiState.inputArchiveEntryDate,
                    bodyWeight = uiState.inputArchiveEntryBodyWeight,
                    nutrients = Nutrients.fromStrings(uiState.inputArchiveEntryNutrients, context),
                    context = context,
                )
            } else {
                viewModel.archiveEditEntry(
                    index = editIndex,
                    date = uiState.inputArchiveEntryDate,
                    bodyWeight = uiState.inputArchiveEntryBodyWeight,
                    nutrients = Nutrients.fromStrings(uiState.inputArchiveEntryNutrients, context),
                    context = context,
                )
            }
            onConfirmSuccess()
        } catch (e: IllegalStateException) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }
    ScreenInputOrEditArchive(
        inputBodyWeight = uiState.inputArchiveEntryBodyWeight,
        onUpdateBodyWeight = { viewModel.updateArchiveEntryBodyWeight(it) },
        inputNutrients = uiState.inputArchiveEntryNutrients,
        onUpdateNutrient = { string, index -> viewModel.updateArchiveEntryNutrient(string, index) },
        inputDate = uiState.inputArchiveEntryDate,
        onUpdateDate = { viewModel.updateArchiveEntryDate(it) },
        onConfirm = { onConfirm() },
        listOfTextButtons = buttons { onConfirm() },
    )
}
