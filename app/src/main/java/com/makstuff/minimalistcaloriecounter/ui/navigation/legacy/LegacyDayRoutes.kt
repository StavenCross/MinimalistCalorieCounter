package com.makstuff.minimalistcaloriecounter.ui.navigation.legacy

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.essentials.ALPHABET
import com.makstuff.minimalistcaloriecounter.essentials.GENERAL_WEIGHTS
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonGrid
import com.makstuff.minimalistcaloriecounter.ui.reused.Grid
import com.makstuff.minimalistcaloriecounter.ui.reused.ScrollColumn
import com.makstuff.minimalistcaloriecounter.ui.reused.TileIngredient
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenEnterWeightOfFood
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenShowFoodSelection
import com.makstuff.minimalistcaloriecounter.ui.screens.ScreenWithHoverCard
import java.time.LocalDateTime

/**
 * Registers the old day-builder routes.
 *
 * The modern Add Meal flow is the primary product path. These routes stay available for local
 * database troubleshooting and are intentionally isolated from the current Health Connect workflow.
 */
fun NavGraphBuilder.legacyDayRoutes(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    keyboardController: SoftwareKeyboardController?,
    onNavigate: (String) -> Unit,
    onEditDatabaseEntry: (Int) -> Unit,
) {
    composable(AppRoutes.DAY_CONTENT) {
        ScreenWithHoverCard(
            contentAbove = {},
            nutrients = uiState.day.overallNutrients,
            listOfTextButtons = listOf(
                Pair(stringResource(R.string.button_reset_day)) { viewModel.setAlertDialogDayReset(true) },
                Pair(stringResource(R.string.button_add_food)) { onNavigate(AppRoutes.DAY_HOME) },
                Pair(stringResource(R.string.button_turn_to_archive_entry)) {
                    prepareArchiveFromDay(uiState, viewModel)
                    onNavigate(AppRoutes.ARCHIVE_CREATE_ENTRY_FROM_DAY)
                },
            ),
            content = {
                ScrollColumn(
                    items = uiState.day.components.mapIndexed { index, component ->
                        {
                            TileIngredient(
                                component = component,
                                onClick = {
                                    viewModel.updateCurrentComboComponentWeight(component.first.toFormattedString(true))
                                    viewModel.setNameFoodDayEdit(component.second.name)
                                    onNavigate(AppRoutes.dayEditWeight(index))
                                },
                            )
                        }
                    },
                )
            },
            context = context,
        )
    }

    composable(AppRoutes.DAY_ADD_FOOD) {
        ScreenShowFoodSelection(
            indexList = uiState.databaseLetter,
            database = uiState.database,
            onFoodClicked = { index ->
                viewModel.updateCurrentComboComponentWeight("")
                viewModel.setNameFoodDayAdd(uiState.database[index].name)
                onNavigate(AppRoutes.dayAddWeight(index))
            },
            onFoodLongClicked = { onEditDatabaseEntry(it) },
            onBack = { onNavigate(AppRoutes.DAY_HOME) },
        )
    }

    composable(AppRoutes.DAY_HOME) {
        ScreenWithHoverCard(
            contentAbove = {},
            nutrients = uiState.day.overallNutrients,
            listOfTextButtons = listOf(
                Pair(stringResource(R.string.button_reset_day)) { viewModel.setAlertDialogDayReset(true) },
                Pair(stringResource(R.string.button_edit)) { onNavigate(AppRoutes.DAY_CONTENT) },
                Pair(stringResource(R.string.button_turn_to_archive_entry)) {
                    prepareArchiveFromDay(uiState, viewModel)
                    onNavigate(AppRoutes.ARCHIVE_CREATE_ENTRY_FROM_DAY)
                },
            ),
            content = {
                Grid(
                    modifier = Modifier.fillMaxHeight(),
                    columns = 8,
                    reverseUpDown = true,
                    reverseLeftRight = true,
                    items = ALPHABET.map {
                        Pair<Int, @Composable () -> Unit>(1) {
                            ButtonGrid(
                                text = it.toString(),
                                onClick = {
                                    viewModel.databaseLetterFilter(it)
                                    onNavigate(AppRoutes.DAY_ADD_FOOD)
                                },
                            )
                        }
                    }.reversed() + uiState.databaseQuickselect.map {
                        Pair<Int, @Composable () -> Unit>(2) {
                            ButtonGrid(
                                text = it.second.name,
                                onClick = {
                                    viewModel.setNameFoodDayAdd(it.second.name)
                                    viewModel.updateCurrentComboComponentWeight("")
                                    onNavigate(AppRoutes.dayAddWeight(it.first))
                                },
                                onLongClick = { onEditDatabaseEntry(it.first) },
                            )
                        }
                    }.reversed(),
                )
            },
            context = context,
        )
    }

    composable(AppRoutes.DAY_ADD_WEIGHT) {
        val index = it.arguments?.getString("index")?.toIntOrNull()
        if (index != null && index < uiState.database.size) {
            ScreenEnterWeightOfFood(
                currentWeight = uiState.inputCurrentComboComponentWeight,
                onWeightChange = { viewModel.updateCurrentComboComponentWeight(it) },
                onConfirm = { addFoodToDay(uiState, viewModel, context, keyboardController, index, onNavigate) },
                listOfTextButtons = listOf(
                    Pair(stringResource(R.string.button_cancel)) { onNavigate(AppRoutes.DAY_HOME) },
                    Pair(stringResource(R.string.button_add_to_day)) {
                        addFoodToDay(uiState, viewModel, context, keyboardController, index, onNavigate)
                    },
                ),
                listOfItems = GENERAL_WEIGHTS.map { weight ->
                    Pair<Int, @Composable () -> Unit>(1) {
                        ButtonGrid(
                            text = weight.second,
                            onClick = {
                                keyboardController?.hide()
                                viewModel.dayAddFood(weight.first, uiState.database[index], context)
                                onNavigate(AppRoutes.DAY_HOME)
                            },
                        )
                    }
                },
                listOfQSItems = uiState.database[index].customWeights.listOfStrings.map { weight ->
                    Pair<Int, @Composable () -> Unit>(1) {
                        ButtonGrid(
                            text = weight.second,
                            onClick = {
                                keyboardController?.hide()
                                viewModel.dayAddFood(weight.first, uiState.database[index], context)
                                onNavigate(AppRoutes.DAY_HOME)
                            },
                        )
                    }
                }.reversed(),
            )
        }
    }

    composable(AppRoutes.DAY_EDIT_WEIGHT) {
        val index = it.arguments?.getString("index")?.toIntOrNull()
        if (index != null && index < uiState.day.components.size) {
            ScreenEnterWeightOfFood(
                currentWeight = uiState.inputCurrentComboComponentWeight,
                onWeightChange = { viewModel.updateCurrentComboComponentWeight(it) },
                onConfirm = { editDayFoodWeight(uiState, viewModel, context, keyboardController, index, onNavigate) },
                listOfTextButtons = listOf(
                    Pair(stringResource(R.string.button_cancel)) { onNavigate(AppRoutes.DAY_CONTENT) },
                    Pair(stringResource(R.string.button_delete)) {
                        viewModel.dayDeleteFood(index, context)
                        onNavigate(AppRoutes.DAY_CONTENT)
                    },
                    Pair(stringResource(R.string.button_save_new_weight)) {
                        editDayFoodWeight(uiState, viewModel, context, keyboardController, index, onNavigate)
                    },
                ),
                listOfItems = remember {
                    GENERAL_WEIGHTS.map { weight ->
                        Pair<Int, @Composable () -> Unit>(1) {
                            ButtonGrid(
                                text = weight.second,
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.dayEditFoodWeight(weight.first, index, context)
                                    onNavigate(AppRoutes.DAY_HOME)
                                },
                            )
                        }
                    }
                },
                listOfQSItems = remember {
                    uiState.day.components[index].second.customWeights.listOfStrings.map { weight ->
                        Pair<Int, @Composable () -> Unit>(1) {
                            ButtonGrid(
                                text = weight.second,
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.dayEditFoodWeight(weight.first, index, context)
                                    onNavigate(AppRoutes.DAY_HOME)
                                },
                            )
                        }
                    }.reversed()
                },
            )
        }
    }
}

private fun prepareArchiveFromDay(uiState: AppUiState, viewModel: AppViewModel) {
    viewModel.updateArchiveEntryDate(LocalDateTime.now().minusHours(12).toLocalDate())
    viewModel.updateArchiveEntryBodyWeight("")
    viewModel.updateArchiveEntryAllNutrients(uiState.day.overallNutrients.stringValues(true).toMutableStateList())
}

private fun addFoodToDay(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    keyboardController: SoftwareKeyboardController?,
    index: Int,
    onNavigate: (String) -> Unit,
) {
    keyboardController?.hide()
    try {
        viewModel.dayAddFood(uiState.inputCurrentComboComponentWeight, uiState.database[index], context)
        onNavigate(AppRoutes.DAY_HOME)
    } catch (e: IllegalStateException) {
        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
    }
}

private fun editDayFoodWeight(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    keyboardController: SoftwareKeyboardController?,
    index: Int,
    onNavigate: (String) -> Unit,
) {
    keyboardController?.hide()
    try {
        viewModel.dayEditFoodWeight(uiState.inputCurrentComboComponentWeight, index, context)
        onNavigate(AppRoutes.DAY_CONTENT)
    } catch (e: IllegalStateException) {
        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
    }
}
