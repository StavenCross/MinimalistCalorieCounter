package com.makstuff.minimalistcaloriecounter

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import com.makstuff.minimalistcaloriecounter.classes.CustomWeights
import com.makstuff.minimalistcaloriecounter.classes.DatabaseEntry
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import kotlinx.coroutines.flow.update

internal class AppViewModelDatabaseActions(private val env: AppViewModelEnvironment) {
    fun createEntryFromInput(context: Context) {
        DatabaseEntry.checkName(env.uiState.inputDatabaseEntryCreateName, context)
        addEntry(
            context = context,
            updateDependencies = true,
            databaseEntry = DatabaseEntry(
                name = env.uiState.inputDatabaseEntryCreateName,
                nutrients = Nutrients.fromStrings(env.uiState.inputDatabaseEntryCreateNutrients, context = context),
                customWeights = CustomWeights(env.uiState.inputDatabaseEntryCreateCustomWeights, context = context),
                quickselect = env.uiState.inputDatabaseEntryCreateQuickselect,
                context = context,
            ),
        )
    }

    fun addEntry(context: Context, updateDependencies: Boolean, databaseEntry: DatabaseEntry) {
        env.uiState.database.add(databaseEntry)
        if (updateDependencies) {
            sortByName()
            writeDatabase(context)
            updateQuickselect()
        }
    }

    fun updateCreateName(name: String) {
        env.state.update { currentState ->
            currentState.copy(inputDatabaseEntryCreateName = name)
        }
    }

    fun resetCreateInput() {
        env.state.update { currentState ->
            currentState.copy(
                inputDatabaseEntryCreateName = "",
                inputDatabaseEntryCreateCustomWeights = "",
                inputDatabaseEntryCreateNutrients = mutableStateListOf("", "", "", "", "", "", "", ""),
                inputDatabaseEntryCreateQuickselect = false,
            )
        }
    }

    fun toggleCreateQuickselect() {
        env.state.update { currentState ->
            currentState.copy(inputDatabaseEntryCreateQuickselect = !env.uiState.inputDatabaseEntryCreateQuickselect)
        }
    }

    fun updateCreateCustomWeights(value: String) {
        env.state.update { currentState ->
            currentState.copy(inputDatabaseEntryCreateCustomWeights = value)
        }
    }

    fun updateCreateNutrient(value: String, index: Int) {
        env.uiState.inputDatabaseEntryCreateNutrients[index] = value
    }

    fun editEntryFromInput(indexToDelete: Int, context: Context) {
        DatabaseEntry.checkName(env.uiState.inputDatabaseEntryEditName, context)
        deleteEntry(indexToDelete, false, context)
        addEntry(
            context,
            true,
            DatabaseEntry(
                name = env.uiState.inputDatabaseEntryEditName,
                nutrients = Nutrients.fromStrings(env.uiState.inputDatabaseEntryEditNutrients, context = context),
                customWeights = CustomWeights(env.uiState.inputDatabaseEntryEditCustomWeights, context = context),
                quickselect = env.uiState.inputDatabaseEntryEditQuickselect,
                context = context,
            ),
        )
    }

    fun deleteEntry(indexToDelete: Int, updateDependencies: Boolean, context: Context) {
        env.uiState.database.removeAt(indexToDelete)
        if (updateDependencies) {
            writeDatabase(context)
            updateQuickselect()
            resetLetterFilter()
        }
    }

    fun updateEditName(name: String) {
        env.state.update { currentState ->
            currentState.copy(inputDatabaseEntryEditName = name)
        }
    }

    fun toggleEditQuickselect() {
        env.state.update { currentState ->
            currentState.copy(inputDatabaseEntryEditQuickselect = !env.uiState.inputDatabaseEntryEditQuickselect)
        }
    }

    fun updateEditQuickselect(enabled: Boolean) {
        env.state.update { currentState ->
            currentState.copy(inputDatabaseEntryEditQuickselect = enabled)
        }
    }

    fun updateEditCustomWeights(value: String) {
        env.state.update { currentState ->
            currentState.copy(inputDatabaseEntryEditCustomWeights = value)
        }
    }

    fun updateEditAllNutrients(values: MutableList<String>) {
        env.state.update { currentState ->
            currentState.copy(inputDatabaseEntryEditNutrients = values.toMutableStateList())
        }
    }

    fun updateEditNutrient(value: String, index: Int) {
        env.uiState.inputDatabaseEntryEditNutrients[index] = value
    }

    fun deleteAll(context: Context, updateDependencies: Boolean = true) {
        env.uiState.database.clear()
        if (updateDependencies) {
            writeDatabase(context)
            updateQuickselect()
            resetLetterFilter()
        }
    }

    fun sortByName() {
        env.uiState.database.sortBy { it.name }
    }

    fun filterByLetter(char: Char) {
        val matches = mutableListOf<Int>()
        env.uiState.database.forEachIndexed { index, food ->
            if (food.name[0] == char) matches.add(index)
        }
        env.state.update { currentState ->
            currentState.copy(databaseLetter = matches.toMutableStateList())
        }
    }

    fun resetLetterFilter() {
        env.state.update { currentState ->
            currentState.copy(databaseLetter = mutableStateListOf())
        }
    }

    fun updateQuickselect() {
        val quickselect = mutableListOf<Pair<Int, DatabaseEntry>>()
        env.uiState.database.forEachIndexed { index, food ->
            if (food.quickselect) {
                quickselect.add(Pair(index, food))
            }
            env.state.update { currentState ->
                currentState.copy(databaseQuickselect = quickselect.toMutableStateList())
            }
        }
    }

    private fun writeDatabase(context: Context) {
        env.csvStore.writeDatabase(context, env.uiState.database)
    }
}
