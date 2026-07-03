package com.makstuff.minimalistcaloriecounter

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.DatabaseEntry
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.essentials.checkValidNumber
import com.makstuff.minimalistcaloriecounter.essentials.toBodyWeight
import com.makstuff.minimalistcaloriecounter.essentials.toFormattedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

internal class AppViewModelArchiveDayActions(private val env: AppViewModelEnvironment) {
    fun updateArchiveDate(date: LocalDate) {
        env.state.update { currentState ->
            currentState.copy(inputArchiveEntryDate = date)
        }
    }

    fun updateArchiveBodyWeight(weight: String) {
        env.state.update { currentState ->
            currentState.copy(inputArchiveEntryBodyWeight = weight)
        }
    }

    fun updateArchiveNutrient(value: String, index: Int) {
        env.uiState.inputArchiveEntryNutrients[index] = value
    }

    fun updateArchiveAllNutrients(values: MutableList<String>) {
        env.state.update { currentState ->
            currentState.copy(inputArchiveEntryNutrients = values.toMutableStateList())
        }
    }

    fun deleteArchiveEntry(index: Int, context: Context) {
        val entry = env.uiState.archive.entries[index]
        env.uiState.archive.deleteEntry(index)
        writeArchive(context)
        if (env.uiState.healthConnectSyncEnabled) {
            env.scope.launch {
                env.healthConnectManager.deleteSingleEntry(entry.first)
                if (env.uiState.healthConnectToastsEnabled) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.toast_hc_entry_deleted), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun resetArchiveInput() {
        env.state.update { currentState ->
            currentState.copy(
                inputArchiveEntryDate = LocalDateTime.now().minusHours(12).toLocalDate(),
                inputArchiveEntryBodyWeight = "",
                inputArchiveEntryNutrients = mutableStateListOf("", "", "", "", "", "", "", ""),
            )
        }
    }

    fun resetDay(context: Context) {
        env.state.update { currentState ->
            currentState.copy(day = Combo(context = context))
        }
        writeDay(context)
    }

    fun addDayFood(weight: String, databaseEntry: DatabaseEntry, context: Context) {
        checkValidNumber(weight, context.getString(R.string.weight), context)
        env.uiState.day.addComponent(weight.toDouble(), databaseEntry)
        writeDay(context)
    }

    fun addArchiveEntry(
        date: LocalDate,
        bodyWeight: String,
        nutrients: Nutrients,
        context: Context,
    ) {
        if (date.isAfter(LocalDate.now())) {
            throw IllegalStateException(context.getString(R.string.error_archive_date_future))
        }
        if (env.uiState.archive.entries.any { it.first == date }) {
            throw IllegalStateException(context.getString(R.string.error_archive_date_exists))
        }
        env.uiState.archive.addEntry(date, bodyWeight, nutrients)
        writeArchive(context)
        if (env.uiState.healthConnectSyncEnabled) {
            env.scope.launch {
                env.healthConnectManager.syncSingleEntry(date, bodyWeight.toDoubleOrNull() ?: 0.0, nutrients)
                if (env.uiState.healthConnectToastsEnabled) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.toast_hc_entry_added), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun editArchiveEntry(
        index: Int,
        date: LocalDate,
        bodyWeight: String,
        nutrients: Nutrients,
        context: Context,
    ) {
        if (date.isAfter(LocalDate.now())) {
            throw IllegalStateException(context.getString(R.string.error_archive_date_future))
        }
        val oldEntry = env.uiState.archive.entries[index]
        env.uiState.archive.deleteEntry(index)

        if (env.uiState.archive.entries.any { it.first == date }) {
            env.uiState.archive.addEntry(oldEntry.first, oldEntry.second.toBodyWeight(), oldEntry.third)
            throw IllegalStateException(context.getString(R.string.error_archive_date_exists))
        }

        env.uiState.archive.addEntry(date, bodyWeight, nutrients)
        writeArchive(context)

        if (env.uiState.healthConnectSyncEnabled) {
            env.scope.launch {
                if (oldEntry.first != date) {
                    env.healthConnectManager.deleteSingleEntry(oldEntry.first)
                }
                env.healthConnectManager.syncSingleEntry(date, bodyWeight.toDoubleOrNull() ?: 0.0, nutrients)
                if (env.uiState.healthConnectToastsEnabled) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.toast_hc_entry_edited), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun editDayFoodWeight(weight: String, index: Int, context: Context) {
        checkValidNumber(weight, context.getString(R.string.weight), context)
        env.uiState.day.editComponentWeight(
            weight.toDouble().toFormattedString(true).toDouble(),
            index,
        )
        writeDay(context)
    }

    fun deleteDayFood(index: Int, context: Context) {
        env.uiState.day.deleteComponent(index)
        writeDay(context)
    }

    private fun writeArchive(context: Context) {
        env.csvStore.writeArchive(context, env.uiState.archive)
    }

    private fun writeDay(context: Context) {
        env.csvStore.writeDay(context, env.uiState.day)
    }
}
